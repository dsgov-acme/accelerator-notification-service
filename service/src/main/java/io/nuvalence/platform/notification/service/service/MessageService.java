package io.nuvalence.platform.notification.service.service;

import io.nuvalence.platform.notification.service.domain.Message;
import io.nuvalence.platform.notification.service.domain.MessageTemplate;
import io.nuvalence.platform.notification.service.exception.BadDataException;
import io.nuvalence.platform.notification.service.exception.NotFoundException;
import io.nuvalence.platform.notification.service.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing {@link Message} entities.
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class MessageService {

    private static final String QUEUED_STATUS = "QUEUED";
    private final PubSubService pubSubService;
    private final TemplateService templateService;
    private final MessageRepository messageRepository;

    /**
     * Get a message by id.
     *
     * @param id the message id
     * @return the message
     */
    public Optional<Message> findBy(UUID id) {
        return messageRepository.findById(id);
    }

    /**
     * Save a message.
     *
     * @param message the message
     * @return the saved message
     */
    public Message save(Message message) {
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

        pubSubService.publish(savedMessaged);

        return savedMessaged;
    }

    public void updateMessageStatus(UUID id, String status) {
        messageRepository.updateMessageStatus(id, status);
    }

    private boolean isCorrectType(String parameterValue, String parameterType) {
        switch (parameterType) {
            case "Number":
                return isNumber(parameterValue);
            case "DateTime":
                return isDateTime(parameterValue);
            case "Date":
                return isDate(parameterValue);
            default:
                return true;
        }
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
