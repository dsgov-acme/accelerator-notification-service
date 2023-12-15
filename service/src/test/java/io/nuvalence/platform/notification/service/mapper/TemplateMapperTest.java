package io.nuvalence.platform.notification.service.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.nuvalence.platform.notification.service.domain.EmailFormat;
import io.nuvalence.platform.notification.service.domain.EmailFormatContent;
import io.nuvalence.platform.notification.service.domain.LocalizedStringTemplate;
import io.nuvalence.platform.notification.service.domain.LocalizedStringTemplateLanguage;
import io.nuvalence.platform.notification.service.domain.MessageTemplate;
import io.nuvalence.platform.notification.service.domain.SmsFormat;
import io.nuvalence.platform.notification.service.generated.models.EmailFormatModel;
import io.nuvalence.platform.notification.service.generated.models.LocalizedTemplateModel;
import io.nuvalence.platform.notification.service.generated.models.TemplateRequestModel;
import io.nuvalence.platform.notification.service.generated.models.TemplateRequestModelSmsFormat;
import io.nuvalence.platform.notification.service.generated.models.TemplateResponseModel;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class TemplateMapperTest {

    private final TemplateMapper templateMapper = new TemplateMapper();

    @Test
    void testTemplateToTemplateResponseModel() {
        String templateKey = "key";
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

        SmsFormat smsFormat =
                SmsFormat.builder()
                        .id(UUID.randomUUID())
                        .localizedStringTemplate(
                                LocalizedStringTemplate.builder()
                                        .localizedTemplateStrings(
                                                List.of(
                                                        localizedSmsStringTemplateLanguage1,
                                                        localizedSmsStringTemplateLanguage2))
                                        .build())
                        .build();

        EmailFormat emailFormat =
                EmailFormat.builder()
                        .id(UUID.randomUUID())
                        .localizedSubjectStringTemplate(
                                LocalizedStringTemplate.builder()
                                        .localizedTemplateStrings(
                                                List.of(
                                                        localizedSubjectStringTemplateLanguage1,
                                                        localizedSubjectStringTemplateLanguage2))
                                        .build())
                        .emailFormatContents(
                                List.of(
                                        EmailFormatContent.builder()
                                                .id(UUID.randomUUID())
                                                .emailLayoutInput("body")
                                                .localizedStringTemplate(
                                                        localizedContentStringTemplate)
                                                .build()))
                        .build();

        MessageTemplate template =
                MessageTemplate.builder()
                        .id(UUID.randomUUID())
                        .key(templateKey)
                        .name("template name")
                        .description("template description")
                        .parameters(templateParameters)
                        .emailLayoutKey("emailLayoutKey")
                        .smsFormat(smsFormat)
                        .emailFormat(emailFormat)
                        .build();

        TemplateResponseModel templateResponseModel =
                templateMapper.templateToTemplateResponseModel(template);
        assertEquals(templateResponseModel.getId(), template.getId());
        assertEquals(2, templateResponseModel.getSmsFormat().getMessage().getTemplates().size());
        assertEquals(
                template.getEmailLayoutKey(),
                templateResponseModel.getEmailFormat().getLayoutKey());
        assertEquals(2, templateResponseModel.getEmailFormat().getSubject().getTemplates().size());
        assertEquals(1, templateResponseModel.getEmailFormat().getContent().size());
    }

    @Test
    void testTemplateRequestModelToTemplate() {

        Map<String, String> templateParameters = new HashMap<>();
        templateParameters.put("parameter1", "parameter-value1");
        templateParameters.put("parameter2", "parameter-value2");
        templateParameters.put("parameter3", "parameter-value3");

        TemplateRequestModelSmsFormat smsFormat = new TemplateRequestModelSmsFormat();
        smsFormat.setMessage(
                new LocalizedTemplateModel(
                        Map.of(
                                "en", "template-sms-value1",
                                "es", "template-sms-value2")));

        LocalizedTemplateModel subjectModel =
                new LocalizedTemplateModel(
                        Map.of(
                                "en", "template-subject-value1",
                                "es", "template-subject-value2"));
        Map<String, LocalizedTemplateModel> contentModelMap = new HashMap<>();
        contentModelMap.put(
                "body",
                new LocalizedTemplateModel(
                        Map.of(
                                "en", "template-content-value1",
                                "es", "template-content-value2")));

        EmailFormatModel emailFormat =
                new EmailFormatModel("emailLayoutKey", subjectModel, contentModelMap);

        TemplateRequestModel templateRequestModel =
                new TemplateRequestModel(
                        "name", "description", templateParameters, emailFormat, smsFormat);

        MessageTemplate mappedTemplate =
                templateMapper.templateRequestModelToTemplate(templateRequestModel);

        assertNotNull(mappedTemplate);
        assertEquals(templateRequestModel.getName(), mappedTemplate.getName());
        assertEquals(templateRequestModel.getDescription(), mappedTemplate.getDescription());
        assertEquals(
                templateRequestModel.getParameters().size(), mappedTemplate.getParameters().size());
        assertEquals(
                templateRequestModel.getEmailFormat().getLayoutKey(),
                mappedTemplate.getEmailLayoutKey());
        assertEquals(
                2,
                mappedTemplate
                        .getSmsFormat()
                        .getLocalizedStringTemplate()
                        .getLocalizedTemplateStrings()
                        .size());
        assertEquals(
                2,
                mappedTemplate
                        .getEmailFormat()
                        .getLocalizedSubjectStringTemplate()
                        .getLocalizedTemplateStrings()
                        .size());
        assertEquals(1, mappedTemplate.getEmailFormat().getEmailFormatContents().size());
    }
}
