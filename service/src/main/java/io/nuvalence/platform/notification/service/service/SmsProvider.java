package io.nuvalence.platform.notification.service.service;

/**
 * Interface for SMS providers.
 */
public interface SmsProvider {

    /** Send an SMS.
     *
     * @param to      recipient
     * @param message message
     */
    void sendSms(String to, String message);
}
