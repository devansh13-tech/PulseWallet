package com.pulsewallet.pulsewallet.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.pulsewallet.pulsewallet.dto.ApiError;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Translates exceptions into {@link ApiError} responses so that every failure
 * from every endpoint has one predictable shape.
 *
 * <p>Without this, Spring returns its whitelabel error page for some failures
 * and a bare stack trace for others, and the frontend ends up with per-endpoint
 * error handling.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Resource missing, or owned by another user. */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        log.debug("Resource not found on {} {}: {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    /**
     * Bean Validation failure on a {@code @Valid @RequestBody}. Collects every
     * field error so the client can highlight all bad inputs at once instead of
     * one per round trip.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            // putIfAbsent: keep the first message per field rather than letting
            // a later constraint overwrite a more useful earlier one.
            fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return build(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors);
    }

    /** Argument that passed validation but failed a business rule. */
    @ExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class })
    public ResponseEntity<ApiError> handleBadRequest(
            RuntimeException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
    }

    /** A business-rule uniqueness check failed (e.g. email already registered). */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiError> handleDuplicate(
            DuplicateResourceException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    /**
     * Belt-and-braces against a unique constraint firing at the database level
     * (e.g. a race between two concurrent registrations for the same email
     * that both passed the earlier {@code existsByEmail} check). The service
     * layer should catch this first and rethrow as
     * {@link DuplicateResourceException} with a friendlier message; this
     * handler exists so an unmapped case still returns 409, not 500.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        log.debug("Data integrity violation on {} {}",
                request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.CONFLICT, "This request conflicts with existing data", request, null);
    }

    /**
     * Wrong email/password on {@code POST /api/auth/login}. Deliberately the
     * same message whether the account does not exist or the password is
     * wrong, so the response never confirms which emails are registered.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request, null);
    }

    /**
     * Any other authentication failure raised inside controller/service code
     * rather than the JWT filter chain. Most 401s are produced earlier by
     * {@code RestAuthenticationEntryPoint}; this covers the rest so nothing
     * auth-related falls through to the generic 500 handler below.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(
            AuthenticationException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Authentication failed", request, null);
    }

    /**
     * Authenticated but not permitted - e.g. a {@code @PreAuthorize} check
     * failing. Filter-chain-level access denials are handled by
     * {@code RestAccessDeniedHandler} instead; this covers denials raised
     * after the request reaches a controller.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "You do not have permission to access this resource", request, null);
    }

    /**
     * Last-resort handler. Logs the full stack trace server-side and returns a
     * generic message, because exception text can expose library versions,
     * SQL, or file paths.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}",
                request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again.", request, null);
    }

    private ResponseEntity<ApiError> build(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            Map<String, String> fieldErrors) {
        ApiError body = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                fieldErrors);
        return ResponseEntity.status(status).body(body);
    }
}
