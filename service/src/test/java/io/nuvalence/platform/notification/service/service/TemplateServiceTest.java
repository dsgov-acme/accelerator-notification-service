package io.nuvalence.platform.notification.service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.nuvalence.platform.notification.service.domain.EmailFormat;
import io.nuvalence.platform.notification.service.domain.EmailFormatContent;
import io.nuvalence.platform.notification.service.domain.EmailLayout;
import io.nuvalence.platform.notification.service.domain.LocalizedStringTemplate;
import io.nuvalence.platform.notification.service.domain.LocalizedStringTemplateLanguage;
import io.nuvalence.platform.notification.service.domain.MessageTemplate;
import io.nuvalence.platform.notification.service.domain.SmsFormat;
import io.nuvalence.platform.notification.service.model.SearchTemplateFilter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TemplateServiceTest {

    @Autowired private EmailLayoutService emailLayoutService;
    @Autowired private TemplateService service;

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
        templateParameters.put("parameter1", "parameter-value1");
        templateParameters.put("parameter2", "parameter-value2");
        templateParameters.put("parameter3", "parameter-value3");

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

        createdTemplate = service.createOrUpdateTemplate(templateKey, template);
    }

    @Test
    void testCreateOrUpdateTemplate() {
        assertNotNull(createdTemplate);
    }

    @Test
    void testCreateOrUpdateTemplate_update() {
        String updatedDescription = "updated description";
        createdTemplate.setDescription(updatedDescription);

        // entityManager.detach(createdTemplate);

        MessageTemplate updateTemplate =
                service.createOrUpdateTemplate(createdTemplate.getKey(), createdTemplate);

        assertNotNull(createdTemplate);
        assertEquals(updatedDescription, updateTemplate.getDescription());
    }

    @Test
    void testGetTemplate() {
        Optional<MessageTemplate> foundTemplate = service.getTemplate(createdTemplate.getKey());

        assertTrue(foundTemplate.isPresent());
    }

    @Test
    void testGetTemplates() {
        SearchTemplateFilter filter =
                SearchTemplateFilter.builder().name(createdTemplate.getName()).build();
        Page<MessageTemplate> result = service.getTemplates(filter);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(createdTemplate.getId(), result.getContent().get(0).getId());
    }

    @Test
    void testGetTemplates_not_found() {
        SearchTemplateFilter filter = SearchTemplateFilter.builder().name("unknown").build();
        Page<MessageTemplate> result = service.getTemplates(filter);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
    }
}
