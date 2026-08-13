package com.cpclub.backend.security;

import com.cpclub.backend.security.jwt.JwtUtils;
import com.cpclub.backend.security.service.UserDetailsImpl;
import com.cpclub.backend.user.entity.Role;
import com.cpclub.backend.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    /** Any sufficiently long value that is not the previously-leaked one. */
    private static final String TEST_SECRET =
            "test-only-signing-key-not-used-anywhere-outside-this-suite-0123456789";

    /** The value that leaked via this repository's git history. */
    private static final String LEAKED_SECRET =
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 3600000);
    }

    @Test
    @DisplayName("Should generate valid JWT token for authenticated user")
    void generateJwtToken_Success() {
        User user = User.builder()
                .id(1L)
                .name("Alice Doe")
                .email("alice@example.com")
                .password("password")
                .role(Role.ROLE_USER)
                .codeforcesHandle("alice_cp")
                .build();

        UserDetailsImpl userDetails = UserDetailsImpl.build(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        String token = jwtUtils.generateJwtToken(auth);

        assertNotNull(token);
        assertTrue(jwtUtils.validateJwtToken(token));
        assertEquals("alice@example.com", jwtUtils.getUserNameFromJwtToken(token));
    }

    @Test
    @DisplayName("Should reject invalid or malformed JWT token")
    void validateJwtToken_Malformed() {
        String invalidToken = "not.a.valid.jwt.token";

        boolean isValid = jwtUtils.validateJwtToken(invalidToken);

        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should reject an expired JWT token")
    void validateJwtToken_Expired() {
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", -1);
        String expiredToken = jwtUtils.generateTokenFromEmail("alice@example.com", 1L, "ROLE_USER");

        assertFalse(jwtUtils.validateJwtToken(expiredToken));
    }

    @Test
    @DisplayName("Should reject a JWT with an invalid signature")
    void validateJwtToken_InvalidSignature() {
        String validToken = jwtUtils.generateTokenFromEmail("alice@example.com", 1L, "ROLE_USER");
        // Changing a character in the middle of the signature guarantees a different byte array
        String invalidSignatureToken = validToken.substring(0, validToken.length() - 5) + "invalid";

        assertFalse(jwtUtils.validateJwtToken(invalidSignatureToken));
    }

    /**
     * Rotation is a dashboard action, so nothing in the codebase can force it.
     * This guard is the substitute: if the leaked secret is still configured, the
     * application refuses to start rather than serving traffic on a key anyone
     * can read from the git history.
     */
    @Test
    @DisplayName("Startup fails when the previously-leaked secret is configured")
    void validateSecret_RejectsKnownCompromisedSecret() {
        JwtUtils utils = new JwtUtils();
        ReflectionTestUtils.setField(utils, "jwtSecret", LEAKED_SECRET);

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> ReflectionTestUtils.invokeMethod(utils, "validateSecret"));
        assertTrue(thrown.getMessage().contains("publicly known"),
                "the message must tell the operator why it refused: " + thrown.getMessage());
    }

    @Test
    @DisplayName("Startup fails when the secret is too short to be safe")
    void validateSecret_RejectsShortSecret() {
        JwtUtils utils = new JwtUtils();
        ReflectionTestUtils.setField(utils, "jwtSecret", "too-short");

        assertThrows(IllegalStateException.class,
                () -> ReflectionTestUtils.invokeMethod(utils, "validateSecret"));
    }

    @Test
    @DisplayName("A properly generated secret is accepted")
    void validateSecret_AcceptsGoodSecret() {
        JwtUtils utils = new JwtUtils();
        ReflectionTestUtils.setField(utils, "jwtSecret", TEST_SECRET);

        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(utils, "validateSecret"));
    }
}
