package com.pulsewallet.pulsewallet.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Uniform shape for every error the API returns.
 *
 * <p>Produced exclusively by
 * {@code com.pulsewallet.pulsewallet.exception.GlobalExceptionHandler}.
 *
 * <p>Example of a validation failure:
 * <pre>
 * {
 *   "timestamp": "2026-08-21T10:15:30Z",
 *   "status": 400,
 *   "error": "Bad Request",
 *   "message": "Validation failed",
 *   "path": "/api/transactions",
 *   "fieldErrors": { "amount": "must be greater than 0" }
 * }
 * </pre>
 *
 * @param timestamp   when the failure was handled
 * @param status      HTTP status code
 * @param error       HTTP reason phrase
 * @param message     human-readable summary, safe to show a user
 * @param path        request URI that failed
 * @param fieldErrors per-field messages for validation failures, otherwise
 *                    {@code null} and omitted from the JSON
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors) {
}
