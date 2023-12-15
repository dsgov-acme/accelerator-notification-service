package io.nuvalence.platform.notification.service.service;

import io.nuvalence.platform.notification.service.domain.Message;
import io.nuvalence.platform.notification.service.domain.MessageTemplate;
import io.nuvalence.platform.notification.service.exception.UnprocessableNotificationException;
import io.nuvalence.platform.notification.service.service.usermanagementapi.UserManagementClientService;
import io.nuvalence.platform.notification.usermanagent.client.ApiException;
import io.nuvalence.platform.notification.usermanagent.client.generated.models.UserDTO;
import io.nuvalence.platform.notification.usermanagent.client.generated.models.UserPreferenceDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for sending messages.
 */
@Slf4j
@Service
public class SendMessageService {

    private final UserManagementClientService userManagementClientService;

    private final TemplateService templateService;

    private Map<String, SendMessageProvider> sendMessageProviderMap = new HashMap<>();

    /**
     * Constructor.
     *
     * @param userManagementClientService user management client service
     * @param sendMessageProviders        list of send message providers
     * @param templateService             template service
     */
    public SendMessageService(
            UserManagementClientService userManagementClientService,
            List<SendMessageProvider> sendMessageProviders,
            TemplateService templateService) {
        this.userManagementClientService = userManagementClientService;
        this.templateService = templateService;
        for (SendMessageProvider sendMessageProvider : sendMessageProviders) {
            sendMessageProviderMap.put(sendMessageProvider.supportedMethod(), sendMessageProvider);
        }
    }

    /**
     * Send a message.
     *
     * @param message message
     * @throws ApiException if an error occurs while querying user management service
     * @throws IOException possibly thrown by apis.
     * @throws UnprocessableNotificationException if an error occurs while sending the message
     */
    public void sendMessage(Message message) throws ApiException, IOException {
        UUID userId = UUID.fromString(message.getUserId());

        // Query user management service for user preferences
        Optional<UserDTO> user = userManagementClientService.getUser(userId);
        if (user.isEmpty()) {
            String userNotFoundMessage =
                    String.format("Message could not be sent. User not found %s", userId);
            log.error(userNotFoundMessage);
            throw new UnprocessableNotificationException(userNotFoundMessage);
        }

        UserPreferenceDTO userPreferences = user.get().getPreferences();

        if (userPreferences == null || userPreferences.getPreferredCommunicationMethod() == null) {
            String communicationPreferencesNotFound =
                    String.format(
                            "Message could not be sent. Communication preferences not found for"
                                    + " user %s",
                            userId);
            log.error(communicationPreferencesNotFound);
            throw new UnprocessableNotificationException(communicationPreferencesNotFound);
        }

        SendMessageProvider messageProvider =
                sendMessageProviderMap.get(userPreferences.getPreferredCommunicationMethod());
        if (messageProvider == null) {
            String communicationPreferencesNotAvailable =
                    String.format(
                            "Message could not be sent. Preferred communication method not"
                                    + " supported for user %s",
                            userId);
            log.error(communicationPreferencesNotAvailable);
            throw new UnprocessableNotificationException(communicationPreferencesNotAvailable);
        }

        Optional<MessageTemplate> template =
                templateService.getTemplate(message.getMessageTemplateKey());
        if (template.isEmpty()) {
            String templateNotFound =
                    String.format(
                            "Message could not be sent. Template not found for template key %s",
                            message.getMessageTemplateKey());
            log.error(templateNotFound);
            throw new UnprocessableNotificationException(templateNotFound);
        }

        messageProvider.sendMessage(user.get(), message, template.get());
    }
}
