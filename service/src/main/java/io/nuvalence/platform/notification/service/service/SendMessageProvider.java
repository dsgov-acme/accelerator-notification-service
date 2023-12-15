package io.nuvalence.platform.notification.service.service;

import io.nuvalence.platform.notification.service.domain.Message;
import io.nuvalence.platform.notification.service.domain.MessageTemplate;
import io.nuvalence.platform.notification.usermanagent.client.generated.models.UserDTO;

import java.io.IOException;

/**
 * Interface for sending messages.
 */
public interface SendMessageProvider {

    /**
     * Send a message.
     *
     * @param user     user
     * @param message  message
     * @param template template
     * @throws IOException possibly thrown by apis.
     */
    void sendMessage(UserDTO user, Message message, MessageTemplate template) throws IOException;

    /**
     * Get the supported method.
     *
     * @return supported method
     */
    String supportedMethod();
}
