package io.nuvalence.platform.notification.service.controller;

import io.nuvalence.auth.access.AuthorizationHandler;
import io.nuvalence.platform.notification.service.domain.Message;
import io.nuvalence.platform.notification.service.generated.controllers.SendNotificationApiDelegate;
import io.nuvalence.platform.notification.service.generated.models.MessageRequestModel;
import io.nuvalence.platform.notification.service.generated.models.MessageResponseModel;
import io.nuvalence.platform.notification.service.mapper.MessageMapperImpl;
import io.nuvalence.platform.notification.service.service.MessageService;
import jakarta.ws.rs.ForbiddenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Implementation of SendNotificationApiDelegate.
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class SendNotificationApiDelegateImpl implements SendNotificationApiDelegate {

    private final MessageMapperImpl messageMapperImpl;
    private final MessageService messageService;
    private final AuthorizationHandler authorizationHandler;

    @Override
    public ResponseEntity<MessageResponseModel> sendMessage(
            MessageRequestModel messageRequestModel) {
        if (!authorizationHandler.isAllowed("send", Message.class)) {
            throw new ForbiddenException();
        }
        Message message =
                messageService.save(
                        messageMapperImpl.messageRequestModelToMessage(messageRequestModel));
        return ResponseEntity.ok(messageMapperImpl.messageToMessageResponseModel(message));
    }

    @Override
    public ResponseEntity<MessageResponseModel> getMessageById(String id) {
        if (!authorizationHandler.isAllowed("view", Message.class)) {
            throw new ForbiddenException();
        }
        return messageService
                .findBy(UUID.fromString(id))
                .filter(message -> authorizationHandler.isAllowedForInstance("view", message))
                .map(
                        message ->
                                ResponseEntity.ok(
                                        messageMapperImpl.messageToMessageResponseModel(message)))
                .orElse(ResponseEntity.notFound().build());
    }
}
