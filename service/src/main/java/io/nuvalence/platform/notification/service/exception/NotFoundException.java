package io.nuvalence.platform.notification.service.exception;

/**
 * Exception thrown when a resource is not found.
 */
public class NotFoundException extends RuntimeException {
    private static final long serialVersionUID = 8844812225671072068L;

    /**
     * Creates a new instance of this exception.
     * @param message exception message
     */
    public NotFoundException(String message) {
        super(message);
    }
}
