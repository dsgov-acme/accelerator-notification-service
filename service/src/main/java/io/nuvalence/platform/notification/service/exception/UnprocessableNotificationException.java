package io.nuvalence.platform.notification.service.exception;

/**
 * Exception to be thrown in case a notification request cannot be processed.
 */
public class UnprocessableNotificationException extends RuntimeException {

    private static final long serialVersionUID = 1376566802119855473L;

    public UnprocessableNotificationException(String message) {
        super(message);
    }
}
