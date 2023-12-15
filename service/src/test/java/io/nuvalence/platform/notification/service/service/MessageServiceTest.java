package io.nuvalence.platform.notification.service.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;

import io.nuvalence.platform.notification.service.domain.EmailFormat;
import io.nuvalence.platform.notification.service.domain.EmailFormatContent;
import io.nuvalence.platform.notification.service.domain.EmailLayout;
import io.nuvalence.platform.notification.service.domain.LocalizedStringTemplate;
import io.nuvalence.platform.notification.service.domain.LocalizedStringTemplateLanguage;
import io.nuvalence.platform.notification.service.domain.Message;
import io.nuvalence.platform.notification.service.domain.MessageTemplate;
import io.nuvalence.platform.notification.service.domain.SmsFormat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MessageServiceTest {

    @Autowired private EmailLayoutService emailLayoutService;
    @Autowired private TemplateService templateService;
    @Autowired private MessageService service;
    @MockBean private PubSubService mockPubSubService;

    private MessageTemplate createdTemplate;

    @BeforeAll
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

        createdTemplate = templateService.createOrUpdateTemplate(templateKey, template);
    }

    @Test
    void testSave() {
        Message message = new Message();
        message.setUserId(UUID.randomUUID().toString());
        message.setMessageTemplateKey(createdTemplate.getKey());
        message.setStatus("QUEUED");
        message.setParameters(
                Map.of(
                        "parameter-key-1",
                        "parameter-value-1",
                        "parameter-key-2",
                        "parameter-value-2"));

        Message savedMessage = service.save(message);
        assertNotNull(savedMessage);
        assertNotNull(savedMessage.getId());
        assertNotNull(savedMessage.getRequestedTimestamp());

        Mockito.verify(mockPubSubService).publish(any());
    }
}
