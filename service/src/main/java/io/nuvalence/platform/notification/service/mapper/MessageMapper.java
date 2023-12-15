package io.nuvalence.platform.notification.service.mapper;

import io.nuvalence.events.event.NotificationEvent;
import io.nuvalence.platform.notification.service.domain.Message;
import io.nuvalence.platform.notification.service.generated.models.MessageRequestModel;
import io.nuvalence.platform.notification.service.generated.models.MessageResponseModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper for messages.
 */
@Mapper(componentModel = "spring")
public interface MessageMapper {
    /**
     * Map a message request model to a message.
     *
     * @param messageRequestModel message request model
     * @return message
     */
    @Mapping(source = "templateKey", target = "messageTemplateKey")
    Message messageRequestModelToMessage(MessageRequestModel messageRequestModel);

    @Mapping(source = "templateKey", target = "messageTemplateKey")
    Message notificationEventToMessage(NotificationEvent notificationEvent);

    /**
     * Map a message to a message request model.
     *
     * @param message message
     * @return message request model
     */
    @Mapping(source = "messageTemplateKey", target = "templateKey")
    MessageResponseModel messageToMessageResponseModel(Message message);
}
