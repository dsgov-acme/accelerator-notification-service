package io.nuvalence.platform.notification.service.service;

import com.sendgrid.SendGrid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration beans for SendGrid connection.
 */
@Configuration
public class SendGridConfiguration {
    @Value("${sendgrid.apiKey}")
    private String sendGridApiKey;

    @Bean
    public SendGrid sendGrid() {
        return new SendGrid(sendGridApiKey);
    }
}
