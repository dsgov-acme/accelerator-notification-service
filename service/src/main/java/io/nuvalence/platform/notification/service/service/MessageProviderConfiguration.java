package io.nuvalence.platform.notification.service.service;

import com.sendgrid.SendGrid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for message providers.
 */
@Slf4j
@Configuration
public class MessageProviderConfiguration {

    /**
     * Create a message provider for email.
     * @param sendGrid api.
     * @return SendGridEmailProvider object.
     */
    @Bean
    public EmailProvider emailMessageProvider(SendGrid sendGrid) {
        return new SendGridEmailProvider(sendGrid);
    }

    /**
     * Create a message provider for SMS.
     *
     * @return SMS message provider
     */
    @Bean
    public SmsProvider smsMessageProvider() {
        return new TwilioSmsProvider();
    }
}
