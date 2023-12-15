package io.nuvalence.platform.notification.service.processors;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.nuvalence.events.event.EventMetadata;
import io.nuvalence.events.event.NotificationEvent;
import io.nuvalence.platform.notification.service.domain.EmailFormat;
import io.nuvalence.platform.notification.service.domain.EmailFormatContent;
import io.nuvalence.platform.notification.service.domain.EmailLayout;
import io.nuvalence.platform.notification.service.domain.LocalizedStringTemplate;
import io.nuvalence.platform.notification.service.domain.LocalizedStringTemplateLanguage;
import io.nuvalence.platform.notification.service.domain.Message;
import io.nuvalence.platform.notification.service.domain.MessageTemplate;
import io.nuvalence.platform.notification.service.domain.SmsFormat;
import io.nuvalence.platform.notification.service.exception.BadDataException;
import io.nuvalence.platform.notification.service.exception.NotFoundException;
import io.nuvalence.platform.notification.service.mapper.MessageMapperImpl;
import io.nuvalence.platform.notification.service.repository.MessageRepository;
import io.nuvalence.platform.notification.service.service.EmailLayoutService;
import io.nuvalence.platform.notification.service.service.MessageService;
import io.nuvalence.platform.notification.service.service.SendMessageService;
import io.nuvalence.platform.notification.service.service.TemplateService;
import io.nuvalence.platform.notification.usermanagent.client.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SpringBootTest
@ActiveProfiles("test")
class NotificationEventProcessorTest {

    @Autowired private EmailLayoutService emailLayoutService;

    @Mock private MessageMapperImpl messageMapper;

    @Mock private TemplateService templateService;

    @Mock private MessageRepository messageRepository;

    @Mock private SendMessageService sendMessageService;

    @Mock private MessageService messageService;

    @InjectMocks private NotificationEventProcessor eventProcessor;

    private MessageTemplate createdTemplate;

    @BeforeEach
    void setUp() {
        final String emailLayoutKeykey = "emailLayoutKey";
        List<String> inputs =
                new ArrayList<>() {
                    private static final long serialVersionUID = 4861793309100343408L;

                    {
                        add("input1");
                        add("input2");
                        add("input3");
                    }
                };
        EmailLayout emailLayout = new EmailLayout();
        emailLayout.setName("name");
        emailLayout.setDescription("description");
        emailLayout.setContent("content");
        emailLayout.setInputs(inputs);

        final String templateKey = "key";
        Map<String, String> templateParameters = new HashMap<>();
        templateParameters.put("parameter-key-1", "parameter-value-1");
        templateParameters.put("parameter-key-2", "parameter-value-2");

        LocalizedStringTemplateLanguage localizedSmsStringTemplateLanguage1 =
                LocalizedStringTemplateLanguage.builder()
                        .language("en")
                        .template("template-sms-value1")
                        .build();

        LocalizedStringTemplateLanguage localizedSmsStringTemplateLanguage2 =
                LocalizedStringTemplateLanguage.builder()
                        .language("es")
                        .template("template-sms-value2")
                        .build();

        LocalizedStringTemplateLanguage localizedContentStringTemplateLanguage1 =
                LocalizedStringTemplateLanguage.builder()
                        .language("en")
                        .template("template-content-value1")
                        .build();

        LocalizedStringTemplateLanguage localizedContentStringTemplateLanguage2 =
                LocalizedStringTemplateLanguage.builder()
                        .language("es")
                        .template("template-content-value2")
                        .build();

        LocalizedStringTemplate localizedContentStringTemplate =
                LocalizedStringTemplate.builder()
                        .localizedTemplateStrings(
                                List.of(
                                        localizedContentStringTemplateLanguage1,
                                        localizedContentStringTemplateLanguage2))
                        .build();
        localizedContentStringTemplateLanguage1.setLocalizedStringTemplate(
                localizedContentStringTemplate);
        localizedContentStringTemplateLanguage2.setLocalizedStringTemplate(
                localizedContentStringTemplate);
        EmailFormatContent emailFormatContent =
                EmailFormatContent.builder()
                        .emailLayoutInput("body")
                        .localizedStringTemplate(localizedContentStringTemplate)
                        .build();

        SmsFormat smsFormat =
                SmsFormat.builder()
                        .localizedStringTemplate(
                                LocalizedStringTemplate.builder()
                                        .localizedTemplateStrings(
                                                List.of(
                                                        localizedSmsStringTemplateLanguage1,
                                                        localizedSmsStringTemplateLanguage2))
                                        .build())
                        .build();
        localizedSmsStringTemplateLanguage1.setLocalizedStringTemplate(
                smsFormat.getLocalizedStringTemplate());
        localizedSmsStringTemplateLanguage2.setLocalizedStringTemplate(
                smsFormat.getLocalizedStringTemplate());

        LocalizedStringTemplateLanguage localizedSubjectStringTemplateLanguage1 =
                LocalizedStringTemplateLanguage.builder()
                        .language("en")
                        .template("template-subject-value1")
                        .build();

        LocalizedStringTemplateLanguage localizedSubjectStringTemplateLanguage2 =
                LocalizedStringTemplateLanguage.builder()
                        .language("es")
                        .template("template-subject-value2")
                        .build();

        EmailFormat emailFormat =
                EmailFormat.builder()
                        .localizedSubjectStringTemplate(
                                LocalizedStringTemplate.builder()
                                        .localizedTemplateStrings(
                                                List.of(
                                                        localizedSubjectStringTemplateLanguage1,
                                                        localizedSubjectStringTemplateLanguage2))
                                        .build())
                        .emailFormatContents(List.of(emailFormatContent))
                        .build();
        localizedSubjectStringTemplateLanguage1.setLocalizedStringTemplate(
                emailFormat.getLocalizedSubjectStringTemplate());
        localizedSubjectStringTemplateLanguage2.setLocalizedStringTemplate(
                emailFormat.getLocalizedSubjectStringTemplate());
        emailFormatContent.setEmailFormat(emailFormat);

        EmailLayout createdEmailLayout =
                emailLayoutService.createEmailLayout(emailLayoutKeykey, emailLayout);

        MessageTemplate template =
                MessageTemplate.builder()
                        .key(templateKey)
                        .name("template name")
                        .description("template description")
                        .parameters(templateParameters)
                        .emailLayoutKey(createdEmailLayout.getKey())
                        .smsFormat(smsFormat)
                        .emailFormat(emailFormat)
                        .build();

        createdTemplate = template;
    }

    @Test
    void testExecuteWithValidEvent() throws IOException, ApiException {
        final String templateKey = "key";
        Map<String, String> templateParameters = new HashMap<>();
        templateParameters.put("parameter-key-1", "parameter-value-1");
        templateParameters.put("parameter-key-2", "parameter-value-2");
        // Arrange
        NotificationEvent event =
                NotificationEvent.builder()
                        .userId(UUID.randomUUID())
                        .templateKey(templateKey)
                        .parameters(templateParameters)
                        .build();
        EventMetadata metadata = new EventMetadata();
        metadata.setId(UUID.randomUUID());
        metadata.setType("notificationEvent");
        event.setMetadata(metadata);
        Message message = new Message();
        message.setUserId(String.valueOf(event.getUserId()));
        message.setMessageTemplateKey(event.getTemplateKey());
        message.setParameters(event.getParameters());

        when(messageMapper.notificationEventToMessage(event)).thenReturn(message);
        when(templateService.getTemplate(any())).thenReturn(java.util.Optional.of(createdTemplate));
        when(messageRepository.save(any())).thenReturn(message);

        // Act
        eventProcessor.setData(event);
        eventProcessor.execute();

        // Assert
        verify(sendMessageService, times(1)).sendMessage(message);
        verify(messageService, times(1)).updateMessageStatus(message.getId(), "SENT");
    }

    @Test
    void testExecuteWithInvalidParameterType() {

        NotificationEvent event =
                NotificationEvent.builder()
                        .userId(UUID.randomUUID())
                        .templateKey("templateKey")
                        .parameters(new HashMap<>())
                        .build();
        EventMetadata metadata = new EventMetadata();
        metadata.setId(UUID.randomUUID());
        metadata.setType("notificationEvent");
        event.setMetadata(metadata);
        Message message = new Message();
        message.setUserId(String.valueOf(event.getUserId()));
        message.setMessageTemplateKey(event.getTemplateKey());
        message.setParameters(event.getParameters());

        when(messageMapper.notificationEventToMessage(event)).thenReturn(message);
        when(templateService.getTemplate(any())).thenReturn(java.util.Optional.of(createdTemplate));
        when(messageRepository.save(any())).thenReturn(message);

        // Act and Assert
        assertThrows(
                BadDataException.class,
                () -> {
                    eventProcessor.setData(event);
                    eventProcessor.execute();
                });

        // Verify that sendMessageService and messageService are not called
        verifyNoInteractions(sendMessageService);
        verifyNoInteractions(messageService);
    }

    @Test
    void testExecuteWithTemplateNotFound() {
        // Arrange
        final String templateKey = "key";
        Map<String, String> templateParameters = new HashMap<>();
        templateParameters.put("parameter-key-1", "parameter-value-1");
        templateParameters.put("parameter-key-2", "parameter-value-2");
        // Arrange
        NotificationEvent event =
                NotificationEvent.builder()
                        .userId(UUID.randomUUID())
                        .templateKey(templateKey)
                        .parameters(templateParameters)
                        .build();
        EventMetadata metadata = new EventMetadata();
        metadata.setId(UUID.randomUUID());
        metadata.setType("notificationEvent");
        event.setMetadata(metadata);
        Message message = new Message();
        message.setUserId(String.valueOf(event.getUserId()));
        message.setMessageTemplateKey(event.getTemplateKey());
        message.setParameters(event.getParameters());

        when(messageMapper.notificationEventToMessage(event)).thenReturn(message);
        when(templateService.getTemplate(any())).thenReturn(java.util.Optional.empty());

        // Act and Assert
        assertThrows(
                NotFoundException.class,
                () -> {
                    eventProcessor.setData(event);
                    eventProcessor.execute();
                });

        // Verify that sendMessageService and messageService are not called
        verifyNoInteractions(sendMessageService);
        verifyNoInteractions(messageService);
    }
}
