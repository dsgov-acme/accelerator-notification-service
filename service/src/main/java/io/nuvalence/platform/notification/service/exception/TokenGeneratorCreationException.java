package io.nuvalence.platform.notification.service.exception;

/**
 * Exception thrown when a token generator cannot be created.
 */
public class TokenGeneratorCreationException extends RuntimeException {
    private static final long serialVersionUID = 649497190123456789L;

    public TokenGeneratorCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
