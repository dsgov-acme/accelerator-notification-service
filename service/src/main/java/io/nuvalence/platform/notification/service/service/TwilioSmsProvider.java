package io.nuvalence.platform.notification.service.service;

import com.twilio.Twilio;
import com.twilio.exception.ApiConnectionException;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

/**
 * Implementation to send sms messages through Twilio.
 */
@Slf4j
@Setter
public class TwilioSmsProvider implements SmsProvider {

    @Value("${twilio.phoneNumber}")
    private String twilioPhoneNumber;

    @Value("${twilio.accountSID}")
    private String twilioAccountSid;

    @Value("${twilio.authToken}")
    private String twilioAuthToken;

    @PostConstruct
    public void initialize() {
        Twilio.init(twilioAccountSid, twilioAuthToken);
    }

    /**
     * Sends a sms via Twilio.
     * @param to      recipient.
     * @param message message to be sent.
     */
    public void sendSms(String to, String message) {
        try {
            Message.creator(new PhoneNumber(to), new PhoneNumber(twilioPhoneNumber), message)
                    .create();
        } catch (ApiConnectionException connectionException) {
            log.warn(
                    "Network issue encountered while sending sms to {}. This operation will be"
                            + " retried. Error details: {}",
                    to,
                    connectionException.getMessage());
            throw connectionException;
        } catch (ApiException apiException) {
            log.warn(
                    "Twilio api exception encountered while sending sms to {}. This operation will"
                            + " be retried. Error details: {}",
                    to,
                    apiException.getMessage());
            throw apiException;
        } catch (Exception e) {
            log.warn(
                    "An unexpected exception has occurred while sending sms to {}. This operation"
                            + " will be retried. Error details: {}",
                    to,
                    e.getMessage());
            throw e;
        }
    }
}
