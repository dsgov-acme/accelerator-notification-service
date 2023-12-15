package io.nuvalence.platform.notification.service.exception;

/**
 * Exception thrown when parsing messages.
 */
public class MessageParsingException extends RuntimeException {
    private static final long serialVersionUID = 1234567890123456789L;

    public MessageParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
