package com.cpclub.backend.security.jwt;

import com.cpclub.backend.common.dto.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Writes the platform's JSON error envelope when an unauthenticated request reaches a protected route.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AuthEntryPointJwt implements AuthenticationEntryPoint {

    /**
     * Spring's configured mapper, injected rather than constructed.
     *
     * <p>This previously built its own {@code com.fasterxml.jackson} (Jackson 2)
     * mapper. Spring Boot 4 serializes with Jackson 3, and Jackson 2 is only on the
     * classpath transitively — so that mapper had no JSR-310 module and threw on
     * this class's own {@link java.time.LocalDateTime} timestamp. Every 401 failed
     * while writing its body, and the caller got a serialization error instead of
     * the "unauthorized" signal the frontend uses to redirect to login.</p>
     *
     * <p>It went unnoticed because no test put an unauthenticated request through
     * the real filter chain; every MockMvc test used {@code standaloneSetup}, which
     * has no security in it.</p>
     */
    private final ObjectMapper objectMapper;

    /**
     * Produces a JSON HTTP 401 response instead of the default HTML/login redirect.
     *
     * @param request rejected HTTP request
     * @param response HTTP response to populate
     * @param authException security failure that caused the rejection
     * @throws IOException if the response body cannot be written
     * @throws ServletException if the servlet container reports an error
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        log.error("Unauthorized error: {}", authException.getMessage());

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        ApiResponse<Void> apiResponse = ApiResponse.error("Unauthorized access: " + authException.getMessage());
        objectMapper.writeValue(response.getOutputStream(), apiResponse);
    }
}
