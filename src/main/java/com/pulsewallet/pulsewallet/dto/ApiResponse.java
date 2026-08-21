package com.pulsewallet.pulsewallet.dto;

import java.time.Instant;

/**
 * Uniform envelope for successful API responses.
 *
 * <p>Every endpoint returning data wraps it in this record so the React client
 * can rely on one shape:
 * <pre>
 * { "success": true, "message": "OK", "data": { ... }, "timestamp": "..." }
 * </pre>
 *
 * <p>{@code null} fields are stripped by
 * {@code spring.jackson.default-property-inclusion=non_null}, so an error
 * response does not carry a useless {@code "data": null}.
 *
 * @param <T> payload type
 */
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        Instant timestamp) {

    /** Success with a default message. */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", data, Instant.now());
    }

    /** Success with an explicit message, e.g. "Transaction created". */
    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data, Instant.now());
    }

    /** Success with no payload, e.g. after a delete. */
    public static ApiResponse<Void> message(String message) {
        return new ApiResponse<>(true, message, null, Instant.now());
    }
}
