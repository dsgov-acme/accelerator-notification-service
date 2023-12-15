package io.nuvalence.platform.notification.service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.cloud.spring.pubsub.support.GcpPubSubHeaders;
import io.nuvalence.platform.notification.service.domain.Message;
import io.nuvalence.platform.notification.service.exception.MessageParsingException;
import io.nuvalence.platform.notification.service.exception.UnprocessableNotificationException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessageHandler;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Service to handle messages from the PubSub subscription, for notification processing.
 */
@Slf4j
@Service("notificationProcessingSubscriber")
public class NotificationProcessingSubscriber implements MessageHandler {

    private static final String SENT_STATUS = "SENT";

    private static final String UNPROCESSABLE_STATUS = "UNPROCESSABLE";
    private final ObjectMapper mapper;
    private final MessageService messageService;
    private final SendMessageService sendMessageService;

    /**
     * Subscriber constructor.
     *
     * @param mapper the object mapper bean
     * @param messageService service to update message status
     * @param sendMessageService service to process notifications.
     */
    public NotificationProcessingSubscriber(
            ObjectMapper mapper,
            MessageService messageService,
            SendMessageService sendMessageService) {
        this.mapper = mapper;
        this.messageService = messageService;
        this.sendMessageService = sendMessageService;
    }

    @Override
    @Transactional
    public void handleMessage(org.springframework.messaging.Message<?> message) {
        log.trace("Received message for notification processing.");

        Message messageToSend = parseSubscriptionPayload(message);

        try {
            sendMessageService.sendMessage(messageToSend);
            messageService.updateMessageStatus(messageToSend.getId(), SENT_STATUS);
            acknowledgeMessage(message);
        } catch (UnprocessableNotificationException e) {
            acknowledgeMessage(message);
            messageService.updateMessageStatus(messageToSend.getId(), UNPROCESSABLE_STATUS);
        } catch (Exception e) {
            log.error("An error occurred processing request", e);
            acknowledgeMessage(message, false);
        }
    }

    private void acknowledgeMessage(org.springframework.messaging.Message<?> message) {
        acknowledgeMessage(message, true);
    }

    private void acknowledgeMessage(
            org.springframework.messaging.Message<?> message, boolean acknowledge) {
        BasicAcknowledgeablePubsubMessage originalMessage =
                message.getHeaders()
                        .get(
                                GcpPubSubHeaders.ORIGINAL_MESSAGE,
                                BasicAcknowledgeablePubsubMessage.class);
        if (originalMessage != null) {
            if (acknowledge) {
                log.debug("Acknowledging pubsub message");
                originalMessage.ack();
            } else {
                log.debug("N-Acknowledging pubsub message");
                originalMessage.nack();
            }
        }
    }

    private Message parseSubscriptionPayload(org.springframework.messaging.Message<?> message) {
        try {
            var payload = (byte[]) message.getPayload();
            String requestWrapperString = new String(payload, StandardCharsets.UTF_8);
            return mapper.readValue(requestWrapperString, Message.class);
        } catch (IOException ex) {
            log.error("Error parsing message from PubSub", ex);
            throw new MessageParsingException("Error parsing message from PubSub", ex);
        }
    }
}
