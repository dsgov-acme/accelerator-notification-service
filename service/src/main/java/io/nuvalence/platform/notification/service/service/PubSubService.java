package io.nuvalence.platform.notification.service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuvalence.platform.notification.service.config.PubSubOutboundConfig;
import io.nuvalence.platform.notification.service.domain.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

/**
 * Handle PubSub writes & callbacks.
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class PubSubService {
    private final PubSubOutboundConfig.PubSubOutboundGateway messagingGateway;
    private final ObjectMapper objectMapper;

    /**
     * Serialize and write entity to PubSub.
     *
     * @param entity Message entity to write to PubSub
     * @return original entity
     */
    public Message publish(Message entity) {
        try {
            String str = objectMapper.writeValueAsString(entity);
            MessageHeaders messageHeaders =
                    new MessageHeaders(Map.of("my-header", "my-header-value"));

            org.springframework.messaging.Message<String> msg =
                    MessageBuilder.createMessage(str, messageHeaders);
            messagingGateway.publish(msg);
        } catch (IOException | MessagingException ex) {
            log.warn("PubSub message could not be written", ex);
        }

        return entity;
    }
}
