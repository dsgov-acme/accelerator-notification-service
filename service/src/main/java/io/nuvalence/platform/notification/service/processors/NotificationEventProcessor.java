package io.nuvalence.platform.notification.service.processors;

import io.nuvalence.events.event.NotificationEvent;
import io.nuvalence.events.subscriber.EventProcessor;
import io.nuvalence.platform.notification.service.domain.Message;
import io.nuvalence.platform.notification.service.domain.MessageTemplate;
import io.nuvalence.platform.notification.service.exception.BadDataException;
import io.nuvalence.platform.notification.service.exception.NotFoundException;
import io.nuvalence.platform.notification.service.exception.UnprocessableNotificationException;
import io.nuvalence.platform.notification.service.mapper.MessageMapperImpl;
import io.nuvalence.platform.notification.service.repository.MessageRepository;
import io.nuvalence.platform.notification.service.service.MessageService;
import io.nuvalence.platform.notification.service.service.SendMessageService;
import io.nuvalence.platform.notification.service.service.TemplateService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * This class is responsible for processing notification events.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventProcessor implements EventProcessor<NotificationEvent> {
    private static final String QUEUED_STATUS = "QUEUED";
    private static final String SENT_STATUS = "SENT";

    private static final String UNPROCESSABLE_STATUS = "UNPROCESSABLE";
    private NotificationEvent event;
    private final MessageMapperImpl messageMapperImpl;
    private final TemplateService templateService;
    private final MessageRepository messageRepository;
    private final SendMessageService sendMessageService;
    private final MessageService messageService;

    @Override
    public void setData(NotificationEvent event) {
        this.event = event;
    }

    @Override
    public NotificationEvent getData() {
        return event;
    }

    @Override
    @Transactional
    public void execute() {
        log.debug(
                "Received event {} of type {}",
                event.getMetadata().getId(),
                event.getMetadata().getType());

        Message message = messageMapperImpl.notificationEventToMessage(event);

        // verify template exists
        MessageTemplate messageTemplate =
                templateService
                        .getTemplate(message.getMessageTemplateKey())
                        .orElseThrow(() -> new NotFoundException("Template not found"));
        // verify all parameters in message are in template, ignore those which are not
        messageTemplate
                .getParameters()
                .forEach(
                        (key, parameterType) -> {
                            if (!message.getParameters().containsKey(key)) {
                                log.warn(
                                        "Parameter {} not found in template {}",
                                        key,
                                        messageTemplate.getKey());
                                throw new BadDataException("Parameter not found in template");
                            } else {
                                String parameterValue = message.getParameters().get(key);
                                if (!isCorrectType(parameterValue, parameterType)) {
                                    log.warn(
                                            "Parameter {} value {} does not correspond to"
                                                    + " type {}",
                                            key,
                                            parameterValue,
                                            parameterType);
                                    throw new BadDataException("Parameter not correct type");
                                }
                            }
                        });
        // queue message for sending
        OffsetDateTime now = OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS);

        message.setStatus(QUEUED_STATUS);
        message.setRequestedTimestamp(now);

        Message savedMessaged = messageRepository.save(message);
        try {
            sendMessageService.sendMessage(savedMessaged);
            messageService.updateMessageStatus(savedMessaged.getId(), SENT_STATUS);
        } catch (UnprocessableNotificationException e) {
            messageService.updateMessageStatus(savedMessaged.getId(), UNPROCESSABLE_STATUS);
        } catch (Exception e) {
            log.error("An error occurred processing request", e);
        }
    }

    private boolean isCorrectType(String parameterValue, String parameterType) {
        return switch (parameterType) {
            case "Number" -> isNumber(parameterValue);
            case "DateTime" -> isDateTime(parameterValue);
            case "Date" -> isDate(parameterValue);
            default -> true;
        };
    }

    private boolean isNumber(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isDate(String str) {
        try {
            LocalDate.parse(str, DateTimeFormatter.ISO_DATE);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isDateTime(String str) {
        try {
            LocalDateTime.parse(str, DateTimeFormatter.ISO_DATE_TIME);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
