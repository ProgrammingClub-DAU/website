package com.cpclub.backend.security.config;

import com.cpclub.backend.security.jwt.AuthEntryPointJwt;
import com.cpclub.backend.security.jwt.AuthTokenFilter;
import com.cpclub.backend.security.service.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Central Spring Security configuration for stateless JWT authentication.
 *
 * <p>It defines password verification, CORS, public API routes, and the request filter
 * that reconstructs authentication from a signed token. Fine-grained admin checks stay
 * near endpoints through method-security annotations.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * Comma-separated list of allowed CORS origins.
     * Set CORS_ALLOWED_ORIGINS env var in production to include the Vercel frontend URL.
     * Example: https://cpclub.vercel.app,http://localhost:3000
     */
    @Value("${cpclub.cors.allowed-origins:http://localhost:3000}")
    private String corsAllowedOrigins;

    private final UserDetailsServiceImpl userDetailsService;
    private final AuthEntryPointJwt unauthorizedHandler;
    private final AuthTokenFilter authTokenFilter;

    // No AuthenticationManager, DaoAuthenticationProvider or PasswordEncoder.
    // Sign-in is a verified Google ID token exchanged for one of our JWTs, so
    // there is no credential for Spring Security to check and nothing to hash.
    // UserDetailsServiceImpl is still wired in below: the JWT filter uses it to
    // re-read a member's authorities from the database on every request.
    /**
     * Defines browser origins, methods, and headers permitted to call the API.
     * Origins are read from the {@code cpclub.cors.allowed-origins} property, which maps
     * to the {@code CORS_ALLOWED_ORIGINS} environment variable in production.
     *
     * @return global CORS policy for frontend-to-backend requests
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Split on comma to support multiple origins (e.g. Vercel + localhost)
        List<String> origins = Arrays.asList(corsAllowedOrigins.split(","));
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Accept"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Creates the HTTP security policy and inserts JWT authentication before password login.
     *
     * <p>Sessions and CSRF are disabled because authentication is supplied on every request
     * by a bearer token instead of a server-held browser session.</p>
     *
     * @param http mutable Spring Security DSL
     * @return immutable filter chain used for every request
     * @throws Exception if an invalid security configuration is detected
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/health").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        // Public reads are listed one by one rather than as a wildcard.
                        // /api/users/all and /api/users/profile return UserResponseDto,
                        // which carries the member's email address; they are protected
                        // by @PreAuthorize on the controller. A blanket
                        // GET /api/users/** permitAll would make that annotation the
                        // only thing standing between an anonymous request and every
                        // student's email, so deleting one line during a refactor would
                        // silently publish the roster. Listing the public paths keeps the
                        // filter chain deny-by-default: a new endpoint under /api/users
                        // requires a token until someone deliberately opens it.
                        // The site reads without an account: the members page, a member's
                        // profile and the committee list are all open. "team" is matched
                        // before the authenticated() catch-all below, and cannot collide
                        // with the numeric {id} pattern beside it.
                        .requestMatchers(HttpMethod.GET, "/api/users", "/api/users/team", "/api/users/platform-creators", "/api/users/{id:[0-9]+}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/leaderboard/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/blogs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/events/upcoming", "/api/events/completed").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/events/{id:[0-9]+}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/events/{id:[0-9]+}/photos").permitAll()
                        // /api/gallery/photos must be matched here, before the ADMIN rule for
                        // /api/gallery/** below, or the public gallery would demand a login.
                        .requestMatchers(HttpMethod.GET, "/api/gallery/members", "/api/gallery/members/batches", "/api/gallery/photos").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/hall-of-fame", "/api/hall-of-fame/{id:[0-9]+}").permitAll()
                        .requestMatchers("/api/events", "/api/events/**").hasRole("ADMIN")
                        .requestMatchers("/api/gallery/**").hasRole("ADMIN")
                        // /** also matches the bare /api/hall-of-fame, so POST to the collection
                        // is covered. GETs were let through above.
                        .requestMatchers("/api/hall-of-fame", "/api/hall-of-fame/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/users/{id:[0-9]+}/lookup").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/users/{id:[0-9]+}/club-role").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/snapshots/**").authenticated()
                        .requestMatchers("/api/users/**").authenticated()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                );

        // No authentication provider is registered: the filter below establishes
        // the principal from our own JWT, and nothing else authenticates a request.
        http.addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
