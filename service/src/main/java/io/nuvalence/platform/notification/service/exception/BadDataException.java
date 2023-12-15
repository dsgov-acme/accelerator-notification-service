package io.nuvalence.platform.notification.service.exception;

/**
 * Exception thrown when processing data is not valid.
 */
public class BadDataException extends RuntimeException {
    private static final long serialVersionUID = -9036738998540806492L;

    /**
     * Creates a new instance of this exception.
     * @param message exception message
     */
    public BadDataException(String message) {
        super(message);
    }
}
