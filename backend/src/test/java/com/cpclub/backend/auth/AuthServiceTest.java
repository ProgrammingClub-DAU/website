package com.cpclub.backend.auth;

import com.cpclub.backend.auth.dto.AuthResponse;
import com.cpclub.backend.auth.dto.GoogleSignInRequest;
import com.cpclub.backend.auth.service.AuthService;
import com.cpclub.backend.security.jwt.JwtUtils;
import com.cpclub.backend.user.entity.AcademicYear;
import com.cpclub.backend.user.entity.Role;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Signing in with a university Google account.
 *
 * <p>The decoder is mocked throughout, because its job -- checking Google's
 * signature, the issuer and the audience -- is configuration, covered where it is
 * configured. What is worth testing here is everything decided after a token is
 * known to be genuine: who is allowed in, and what happens to their account.</p>
 *
 * <p>The distinction matters most for the refusals. A token can be perfectly valid
 * and still belong to somebody's personal Gmail, and that is the case the club
 * will actually hit.</p>
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String DOMAIN = "dau.ac.in";
    private static final String MEMBER_EMAIL = "202401226@dau.ac.in";

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private JwtDecoder googleIdTokenDecoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        // Built by hand rather than with @InjectMocks: the allowed domain is a
        // configured string, not a collaborator, and it is the subject of half
        // these tests.
        authService = new AuthService(userRepository, jwtUtils, googleIdTokenDecoder, DOMAIN);
    }

    // ---------------------------------------------------------------- refusals

    @Test
    @DisplayName("A personal Gmail is turned away, however valid its token")
    void shouldRefuseAnAddressOutsideTheDomain() {
        givenGoogleReturns(token("someone@gmail.com", true, null));

        BadCredentialsException thrown = assertThrows(BadCredentialsException.class, this::signIn);

        assertTrue(thrown.getMessage().contains("@" + DOMAIN),
                "the member has to be told which account to use, was: " + thrown.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("An address Google has not verified is not proof of anything")
    void shouldRefuseAnUnverifiedEmail() {
        givenGoogleReturns(token(MEMBER_EMAIL, false, DOMAIN));

        assertThrows(BadCredentialsException.class, this::signIn);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("A personal account wearing the university address is turned away")
    void shouldRefuseAHostedDomainThatDisagreesWithTheAddress() {
        // The address ends in the right domain but Google says the account was
        // issued by a different Workspace, which is the impersonation case the
        // hd claim exists to catch.
        givenGoogleReturns(token(MEMBER_EMAIL, true, "example.com"));

        assertThrows(BadCredentialsException.class, this::signIn);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("A token Google will not vouch for is refused, and says nothing about why")
    void shouldRefuseATokenThatFailsVerification() {
        when(googleIdTokenDecoder.decode(anyString()))
                .thenThrow(new BadJwtException("signature mismatch on kid abc123"));

        BadCredentialsException thrown = assertThrows(BadCredentialsException.class, this::signIn);

        // The decoder's reason describes the token, not the member. Repeating it
        // would only help someone probing the endpoint.
        assertTrue(thrown.getMessage().toLowerCase().contains("could not be verified"));
        assertTrue(!thrown.getMessage().contains("kid abc123"),
                "the verification failure leaked to the caller: " + thrown.getMessage());
    }

    // ------------------------------------------------------------- first visit

    @Test
    @DisplayName("Signing in for the first time creates the account; there is no registration step")
    void shouldCreateAnAccountOnFirstSignIn() {
        givenGoogleReturns(tokenWithProfile(MEMBER_EMAIL, "Ravi Patel", "https://lh3.google/photo.jpg"));
        when(userRepository.findByEmail(MEMBER_EMAIL)).thenReturn(Optional.empty());
        whenSavedGetsAnId(42L);
        when(jwtUtils.generateTokenFromEmail(anyString(), anyLong(), anyString())).thenReturn("our-jwt");

        AuthResponse response = signIn();

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());

        assertEquals(MEMBER_EMAIL, saved.getValue().getEmail());
        assertEquals("Ravi Patel", saved.getValue().getName());
        assertEquals("https://lh3.google/photo.jpg", saved.getValue().getAvatarUrl());
        assertEquals(Role.ROLE_USER, saved.getValue().getRole(), "a new member is never an admin");
        assertNull(saved.getValue().getPassword(), "Google accounts have no password");

        assertEquals("our-jwt", response.token());
        assertEquals(42L, response.id());
    }

    @Test
    @DisplayName("A new member comes back with no year, which is what sends them to the welcome step")
    void shouldReportNoAcademicYearForANewMember() {
        givenGoogleReturns(tokenWithProfile(MEMBER_EMAIL, "Ravi Patel", null));
        when(userRepository.findByEmail(MEMBER_EMAIL)).thenReturn(Optional.empty());
        whenSavedGetsAnId(42L);
        when(jwtUtils.generateTokenFromEmail(anyString(), anyLong(), anyString())).thenReturn("our-jwt");

        assertNull(signIn().academicYear());
    }

    @Test
    @DisplayName("A name is invented from the student ID rather than stored empty")
    void shouldFallBackToTheStudentIdWhenGoogleSendsNoName() {
        givenGoogleReturns(tokenWithProfile(MEMBER_EMAIL, null, null));
        when(userRepository.findByEmail(MEMBER_EMAIL)).thenReturn(Optional.empty());
        whenSavedGetsAnId(42L);
        when(jwtUtils.generateTokenFromEmail(anyString(), anyLong(), anyString())).thenReturn("our-jwt");

        signIn();

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertEquals("202401226", saved.getValue().getName());
    }

    // ---------------------------------------------------------- returning member

    @Test
    @DisplayName("A returning member is matched by address, not signed up again")
    void shouldReuseTheExistingAccount() {
        User existing = existingMember();
        givenGoogleReturns(tokenWithProfile(MEMBER_EMAIL, "Ravi Patel", "https://lh3.google/photo.jpg"));
        when(userRepository.findByEmail(MEMBER_EMAIL)).thenReturn(Optional.of(existing));
        when(jwtUtils.generateTokenFromEmail(anyString(), anyLong(), anyString())).thenReturn("our-jwt");

        AuthResponse response = signIn();

        verify(userRepository, never()).save(any());
        assertEquals(7L, response.id());
        assertEquals(AcademicYear.SECOND_YEAR_ONWARDS, response.academicYear());
        assertEquals("ravi_cf", response.codeforcesHandle());
    }

    @Test
    @DisplayName("Google does not overwrite a name or avatar the member chose")
    void shouldNotOverwriteWhatTheMemberSet() {
        User existing = existingMember();
        givenGoogleReturns(tokenWithProfile(MEMBER_EMAIL, "RAVI PATEL (STUDENT)", "https://lh3.google/photo.jpg"));
        when(userRepository.findByEmail(MEMBER_EMAIL)).thenReturn(Optional.of(existing));
        when(jwtUtils.generateTokenFromEmail(anyString(), anyLong(), anyString())).thenReturn("our-jwt");

        signIn();

        assertEquals("Ravi", existing.getName(), "the member's own name was replaced");
        assertEquals("https://cdn/ravi.png", existing.getAvatarUrl(), "the member's own avatar was replaced");
    }

    @Test
    @DisplayName("A member who never set an avatar gets Google's, once")
    void shouldFillInAnAvatarThatWasNeverSet() {
        User existing = existingMember();
        existing.setAvatarUrl(null);
        givenGoogleReturns(tokenWithProfile(MEMBER_EMAIL, "Ravi Patel", "https://lh3.google/photo.jpg"));
        when(userRepository.findByEmail(MEMBER_EMAIL)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));
        when(jwtUtils.generateTokenFromEmail(anyString(), anyLong(), anyString())).thenReturn("our-jwt");

        signIn();

        assertEquals("https://lh3.google/photo.jpg", existing.getAvatarUrl());
        verify(userRepository).save(existing);
    }

    @Test
    @DisplayName("The address is matched in lower case, so capitals do not create a second account")
    void shouldNormaliseTheAddress() {
        givenGoogleReturns(tokenWithProfile("202401226@DAU.AC.IN", "Ravi Patel", null));
        when(userRepository.findByEmail(MEMBER_EMAIL)).thenReturn(Optional.of(existingMember()));
        when(jwtUtils.generateTokenFromEmail(anyString(), anyLong(), anyString())).thenReturn("our-jwt");

        assertNotNull(signIn());
        verify(userRepository).findByEmail(MEMBER_EMAIL);
    }

    // ------------------------------------------------------------------ helpers

    private AuthResponse signIn() {
        return authService.signInWithGoogle(new GoogleSignInRequest("google-id-token"));
    }

    private void givenGoogleReturns(Jwt token) {
        when(googleIdTokenDecoder.decode(anyString())).thenReturn(token);
    }

    private void whenSavedGetsAnId(long id) {
        when(userRepository.save(any(User.class))).thenAnswer(call -> {
            User user = call.getArgument(0);
            user.setId(id);
            return user;
        });
    }

    private User existingMember() {
        return User.builder()
                .id(7L)
                .name("Ravi")
                .email(MEMBER_EMAIL)
                .avatarUrl("https://cdn/ravi.png")
                .codeforcesHandle("ravi_cf")
                .academicYear(AcademicYear.SECOND_YEAR_ONWARDS)
                .role(Role.ROLE_USER)
                .build();
    }

    private Jwt token(String email, boolean verified, String hostedDomain) {
        Jwt.Builder builder = Jwt.withTokenValue("google-id-token")
                .header("alg", "RS256")
                .claim("email", email)
                .claim("email_verified", verified);

        if (hostedDomain != null) {
            builder.claim("hd", hostedDomain);
        }

        return builder.build();
    }

    private Jwt tokenWithProfile(String email, String name, String picture) {
        Jwt.Builder builder = Jwt.withTokenValue("google-id-token")
                .header("alg", "RS256")
                .claim("email", email)
                .claim("email_verified", true)
                .claim("hd", DOMAIN);

        if (name != null) {
            builder.claim("name", name);
        }
        if (picture != null) {
            builder.claim("picture", picture);
        }

        return builder.build();
    }
}
