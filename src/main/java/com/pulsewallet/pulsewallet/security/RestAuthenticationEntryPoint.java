package com.pulsewallet.pulsewallet.security;

import java.io.IOException;
import java.time.Instant;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;
import com.pulsewallet.pulsewallet.dto.ApiError;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Runs when an unauthenticated request hits a protected endpoint, or a
 * request carries an invalid/expired JWT. Produces the same
 * {@link ApiError} shape as {@code GlobalExceptionHandler}, so the response
 * format does not depend on whether the failure happened inside the security
 * filter chain or inside a controller.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        ApiError body = new ApiError(
                Instant.now(),
                HttpServletResponse.SC_UNAUTHORIZED,
                "Unauthorized",
                "Authentication is required to access this resource",
                request.getRequestURI(),
                null);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
