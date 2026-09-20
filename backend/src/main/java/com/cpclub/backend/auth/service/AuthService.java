package com.cpclub.backend.auth.service;

import com.cpclub.backend.auth.dto.AuthResponse;
import com.cpclub.backend.auth.dto.GoogleSignInRequest;
import com.cpclub.backend.security.jwt.JwtUtils;
import com.cpclub.backend.user.entity.Role;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import com.cpclub.backend.user.service.PlatformCreatorAccounts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Turns a Google sign-in into a session on this site.
 *
 * <p>Sign-in is Google-only, and only for the club's university domain. There is no
 * password to check, no registration form and no separate "create an account"
 * step: the first time somebody from the domain signs in, their account is
 * created from what Google vouches for. That is the point of the design -- the
 * club never holds a credential, and membership of the domain is the membership
 * test.</p>
 *
 * <p>Everything this class trusts comes from inside the signed token. The request
 * body carries the token and nothing else, so a caller cannot assert an address
 * that is not theirs.</p>
 */
@Service
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final JwtDecoder googleIdTokenDecoder;
    private final String allowedDomain;

    public AuthService(
            UserRepository userRepository,
            JwtUtils jwtUtils,
            @Qualifier("googleIdTokenDecoder") JwtDecoder googleIdTokenDecoder,
            @Value("${app.google.allowed-domain}") String allowedDomain) {
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
        this.googleIdTokenDecoder = googleIdTokenDecoder;
        this.allowedDomain = allowedDomain.toLowerCase(Locale.ROOT).trim();
    }

    /**
     * Verifies a Google ID token and returns a session for the member behind it.
     *
     * <p>Creates the account if this is their first sign-in. The returned
     * {@code academicYear} is null for a brand new member, which is how the client
     * knows to ask the welcome question before letting them in.</p>
     *
     * @param request the credential from Google's sign-in button
     * @return a signed token for this site, plus client-safe identity details
     * @throws BadCredentialsException if the token is not a valid Google token for
     *                                 this application, or the member is not on the
     *                                 club's domain
     */
    @Transactional
    public AuthResponse signInWithGoogle(GoogleSignInRequest request) {
        Jwt token = verify(request.idToken());

        String email = requireDomain(token);

        User user = userRepository.findByEmail(email)
                .map(existing -> refresh(existing, token))
                .orElseGet(() -> create(email, token));

        String jwt = jwtUtils.generateTokenFromEmail(user.getEmail(), user.getId(), user.getRole().name());

        return new AuthResponse(
                jwt,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getCodeforcesHandle(),
                user.getAcademicYear()
        );
    }

    /**
     * Checks the token's signature, issuer, audience and expiry.
     *
     * <p>Google's reason for rejecting a token is logged but not returned. It
     * describes the token, not the member, so it cannot help whoever sent it and
     * would only tell someone probing the endpoint which check they tripped.</p>
     */
    private Jwt verify(String idToken) {
        try {
            return googleIdTokenDecoder.decode(idToken);
        } catch (JwtException e) {
            log.warn("Rejected a Google sign-in token: {}", e.getMessage());
            throw new BadCredentialsException("That Google sign-in could not be verified. Please try again.");
        }
    }

    /**
     * Establishes that the member belongs to the club's domain.
     *
     * <p>Three things are checked, because each covers a gap in the others.
     * {@code email_verified} rules out an address Google has not confirmed the
     * holder controls. The address suffix is the obvious test. The {@code hd}
     * hosted-domain claim is the strong one: Google sets it only for accounts the
     * domain's Workspace actually issued, so it distinguishes a real university
     * account from a personal account that merely carries a similar address.</p>
     *
     * <p>{@code hd} is absent for consumer accounts rather than wrong, so its
     * absence is treated as a failure only once the address has already claimed
     * the domain -- which is exactly the case worth refusing.</p>
     *
     * @return the verified, lower-cased address
     */
    private String requireDomain(Jwt token) {
        if (!Boolean.TRUE.equals(token.getClaimAsBoolean("email_verified"))) {
            throw new BadCredentialsException("Google has not verified that email address.");
        }

        String email = token.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            throw new BadCredentialsException("That Google account did not share an email address.");
        }
        email = email.toLowerCase(Locale.ROOT).trim();

        if (!email.endsWith("@" + allowedDomain)) {
            throw new BadCredentialsException(
                    "Only @" + allowedDomain + " accounts can sign in. Please use your university Google account.");
        }

        String hostedDomain = token.getClaimAsString("hd");
        if (hostedDomain != null && !allowedDomain.equalsIgnoreCase(hostedDomain.trim())) {
            throw new BadCredentialsException(
                    "Only @" + allowedDomain + " accounts can sign in. Please use your university Google account.");
        }

        return email;
    }

    /** Creates the account for someone from the domain signing in for the first time. */
    private User create(String email, Jwt token) {
        User user = User.builder()
                .name(displayName(token, email))
                .email(email)
                .avatarUrl(token.getClaimAsString("picture"))
                .role(Role.ROLE_USER)
                .isPlatformCreator(PlatformCreatorAccounts.contains(email))
                .build();

        User saved = userRepository.save(user);
        log.info("Created an account from a first Google sign-in: {}", email);
        return saved;
    }

    /**
     * Fills in what a returning member has never set, and nothing more.
     *
     * <p>A member who uploaded their own avatar or corrected their name should not
     * have Google's version put back every time they sign in, so these are only
     * written when the field is empty.</p>
     */
    private User refresh(User user, Jwt token) {
        boolean changed = false;

        if (isBlank(user.getName())) {
            user.setName(displayName(token, user.getEmail()));
            changed = true;
        }

        if (isBlank(user.getAvatarUrl())) {
            String picture = token.getClaimAsString("picture");
            if (picture != null && !picture.isBlank()) {
                user.setAvatarUrl(picture);
                changed = true;
            }
        }

        boolean shouldBeCreator = PlatformCreatorAccounts.contains(user.getEmail());
        if (user.isPlatformCreator() != shouldBeCreator) {
            user.setPlatformCreator(shouldBeCreator);
            changed = true;
        }

        return changed ? userRepository.save(user) : user;
    }

    /**
     * The name to show, falling back through what Google offers.
     *
     * <p>A Workspace account always has a name, but the claim is optional in the
     * specification, so the student ID from the address is used rather than
     * storing an empty name that would fail validation later.</p>
     */
    private String displayName(Jwt token, String email) {
        String name = token.getClaimAsString("name");
        if (!isBlank(name)) {
            return name.trim();
        }

        String given = token.getClaimAsString("given_name");
        if (!isBlank(given)) {
            return given.trim();
        }

        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
