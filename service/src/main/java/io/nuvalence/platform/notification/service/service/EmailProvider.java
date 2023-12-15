package io.nuvalence.platform.notification.service.service;

import java.io.IOException;

/**
 * Interface for email providers.
 */
public interface EmailProvider {

    /**
     * Sends an email.
     * @param to recipient.
     * @param subject email subject.
     * @param body email body.
     * @throws IOException possibly thrown by apis.
     */
    void sendEmail(String to, String subject, String body) throws IOException;
}
