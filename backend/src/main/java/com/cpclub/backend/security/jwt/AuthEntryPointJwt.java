package com.cpclub.backend.security.jwt;

import com.cpclub.backend.common.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Writes the platform's JSON error envelope when an unauthenticated request reaches a protected route.
 */
@Component
@Slf4j
public class AuthEntryPointJwt implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

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
