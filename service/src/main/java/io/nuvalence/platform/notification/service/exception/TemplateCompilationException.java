package io.nuvalence.platform.notification.service.exception;

/**
 * Exception thrown when a template fails to compile.
 */
public class TemplateCompilationException extends RuntimeException {
    private static final long serialVersionUID = 19491990123456789L;

    public TemplateCompilationException(String template, Throwable cause) {
        super("Error compiling template: " + template, cause);
    }
}
