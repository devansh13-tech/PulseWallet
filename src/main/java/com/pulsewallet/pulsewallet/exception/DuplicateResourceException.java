package com.pulsewallet.pulsewallet.exception;

/**
 * Thrown when a create/update would violate a uniqueness rule that belongs to
 * the business (e.g. an email already registered, a category name a user
 * already has). Mapped to HTTP 409 Conflict by {@link GlobalExceptionHandler}.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
