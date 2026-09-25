package com.pulsewallet.pulsewallet.exception;

/**
 * Thrown when a requested resource does not exist, or exists but belongs to a
 * different user.
 *
 * <p>Mapped to HTTP 404 by {@link GlobalExceptionHandler}.
 *
 * <p>Deliberately used for "not yours" as well as "not there": replying 403 to a
 * transaction ID owned by someone else confirms that the ID exists, which lets
 * an attacker enumerate other users' records. 404 reveals nothing.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * @param resource entity name, e.g. {@code "Transaction"}
     * @param id       identifier that was not found
     */
    public ResourceNotFoundException(String resource, Object id) {
        super("%s not found with id %s".formatted(resource, id));
    }
}
