package io.nuvalence.platform.notification.service.mapper;

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
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mapper for template models.
 */
@Component
public class TemplateMapper {

    /**
     * Maps a template request model to a message response model.
     *
     * @param template template request model
     * @return message template response model
     */
    public TemplateResponseModel templateToTemplateResponseModel(MessageTemplate template) {
        Map<String, String> emailSubjectTemplates = new HashMap<>();
        Map<String, Map<String, String>> emailContentTemplates = new HashMap<>();
        Map<String, String> smsTemplates = new HashMap<>();

        template.getSmsFormat()
                .getLocalizedStringTemplate()
                .getLocalizedTemplateStrings()
                .forEach(
                        localizedStringTemplateLanguage ->
                                smsTemplates.put(
                                        localizedStringTemplateLanguage.getLanguage(),
                                        localizedStringTemplateLanguage.getTemplate()));

        template.getEmailFormat()
                .getLocalizedSubjectStringTemplate()
                .getLocalizedTemplateStrings()
                .forEach(
                        localizedStringTemplateLanguage ->
                                emailSubjectTemplates.put(
                                        localizedStringTemplateLanguage.getLanguage(),
                                        localizedStringTemplateLanguage.getTemplate()));

        template.getEmailFormat()
                .getEmailFormatContents()
                .forEach(
                        emailFormatContent -> {
                            Map<String, String> localizedStringTemplates = new HashMap<>();
                            for (LocalizedStringTemplateLanguage localizedStringTemplate :
                                    emailFormatContent
                                            .getLocalizedStringTemplate()
                                            .getLocalizedTemplateStrings()) {
                                localizedStringTemplates.put(
                                        localizedStringTemplate.getLanguage(),
                                        localizedStringTemplate.getTemplate());
                            }
                            if (emailContentTemplates.containsKey(
                                    emailFormatContent.getEmailLayoutInput())) {
                                emailContentTemplates
                                        .get(emailFormatContent.getEmailLayoutInput())
                                        .putAll(localizedStringTemplates);
                            } else {
                                emailContentTemplates.put(
                                        emailFormatContent.getEmailLayoutInput(),
                                        localizedStringTemplates);
                            }
                        });

        LocalizedTemplateModel subject = new LocalizedTemplateModel(emailSubjectTemplates);
        Map<String, LocalizedTemplateModel> content = new HashMap<>();
        emailContentTemplates.forEach(
                (key, value) -> content.put(key, new LocalizedTemplateModel(value)));

        LocalizedTemplateModel message = new LocalizedTemplateModel(smsTemplates);
        TemplateRequestModelSmsFormat smsFormat = new TemplateRequestModelSmsFormat();
        smsFormat.setMessage(message);

        return new TemplateResponseModel(
                template.getId(),
                template.getKey(),
                template.getVersion(),
                template.getStatus(),
                template.getName(),
                template.getDescription(),
                template.getParameters(),
                new EmailFormatModel(template.getEmailLayoutKey(), subject, content),
                smsFormat,
                template.getCreatedBy());
    }

    /**
     * Maps a template request model to a message template.
     *
     * @param templateRequestModel template request model
     * @return message template
     */
    public MessageTemplate templateRequestModelToTemplate(
            TemplateRequestModel templateRequestModel) {
        Map<String, LocalizedStringTemplateLanguage> emailSubjectLocalized = new HashMap<>();
        Map<String, LocalizedStringTemplateLanguage> smsMessageLocalized = new HashMap<>();
        Map<String, EmailFormatContent> contentLocalized;

        templateRequestModel
                .getEmailFormat()
                .getSubject()
                .getTemplates()
                .forEach(
                        (locale, localizedValue) ->
                                emailSubjectLocalized.put(
                                        locale,
                                        LocalizedStringTemplateLanguage.builder()
                                                .language(locale)
                                                .template(localizedValue)
                                                .build()));

        contentLocalized = buildContentLocalized(templateRequestModel);

        templateRequestModel
                .getSmsFormat()
                .getMessage()
                .getTemplates()
                .forEach(
                        (locale, localizedValue) ->
                                smsMessageLocalized.put(
                                        locale,
                                        LocalizedStringTemplateLanguage.builder()
                                                .language(locale)
                                                .template(localizedValue)
                                                .build()));

        MessageTemplate messageTemplate =
                MessageTemplate.builder()
                        .name(templateRequestModel.getName())
                        .description(templateRequestModel.getDescription())
                        .parameters(templateRequestModel.getParameters())
                        .emailLayoutKey(templateRequestModel.getEmailFormat().getLayoutKey())
                        .build();

        SmsFormat smsFormat = buildSmsFormat(smsMessageLocalized);

        EmailFormat emailFormat =
                buildEmailFormat(emailSubjectLocalized, new ArrayList<>(contentLocalized.values()));

        messageTemplate.setSmsFormat(smsFormat);
        messageTemplate.setEmailFormat(emailFormat);
        return messageTemplate;
    }

    private SmsFormat buildSmsFormat(
            Map<String, LocalizedStringTemplateLanguage> smsMessageLocalized) {
        LocalizedStringTemplate smslocalizedStringTemplate =
                LocalizedStringTemplate.builder()
                        .localizedTemplateStrings(new ArrayList<>(smsMessageLocalized.values()))
                        .build();
        smslocalizedStringTemplate
                .getLocalizedTemplateStrings()
                .forEach(l -> l.setLocalizedStringTemplate(smslocalizedStringTemplate));
        return SmsFormat.builder()
                // .messageTemplate()
                .localizedStringTemplate(smslocalizedStringTemplate)
                .build();
    }

    private EmailFormat buildEmailFormat(
            Map<String, LocalizedStringTemplateLanguage> emailSubjectLocalized,
            List<EmailFormatContent> contentLocalized) {
        LocalizedStringTemplate emailSubjectlocalizedStringTemplate =
                LocalizedStringTemplate.builder()
                        .localizedTemplateStrings(new ArrayList<>(emailSubjectLocalized.values()))
                        .build();
        emailSubjectlocalizedStringTemplate
                .getLocalizedTemplateStrings()
                .forEach(l -> l.setLocalizedStringTemplate(emailSubjectlocalizedStringTemplate));
        EmailFormat emailFormat =
                EmailFormat.builder()
                        // .messageTemplate()
                        .localizedSubjectStringTemplate(emailSubjectlocalizedStringTemplate)
                        .build();
        contentLocalized.forEach(
                c -> {
                    c.setEmailFormat(emailFormat);
                    c.getLocalizedStringTemplate()
                            .getLocalizedTemplateStrings()
                            .forEach(
                                    l ->
                                            l.setLocalizedStringTemplate(
                                                    c.getLocalizedStringTemplate()));
                });
        emailFormat.setEmailFormatContents(contentLocalized);

        return emailFormat;
    }

    private Map<String, EmailFormatContent> buildContentLocalized(
            TemplateRequestModel templateRequestModel) {
        Map<String, EmailFormatContent> contentLocalized = new HashMap<>();

        templateRequestModel
                .getEmailFormat()
                .getContent()
                .forEach(
                        (emailLayoutInput, templates) ->
                                templates
                                        .getTemplates()
                                        .forEach(
                                                (locale, localizedValue) -> {
                                                    if (contentLocalized.containsKey(
                                                            emailLayoutInput)) {
                                                        contentLocalized
                                                                .get(emailLayoutInput)
                                                                .getLocalizedStringTemplate()
                                                                .getLocalizedTemplateStrings()
                                                                .add(
                                                                        LocalizedStringTemplateLanguage
                                                                                .builder()
                                                                                .language(locale)
                                                                                .template(
                                                                                        localizedValue)
                                                                                .build());
                                                    } else {
                                                        List<LocalizedStringTemplateLanguage>
                                                                localizedStringTemplateLanguages =
                                                                        new ArrayList<>();
                                                        localizedStringTemplateLanguages.add(
                                                                LocalizedStringTemplateLanguage
                                                                        .builder()
                                                                        .language(locale)
                                                                        .template(localizedValue)
                                                                        .build());
                                                        EmailFormatContent emailFormatContent =
                                                                buildEmailFormatContent(
                                                                        emailLayoutInput,
                                                                        localizedStringTemplateLanguages);
                                                        contentLocalized.put(
                                                                emailLayoutInput,
                                                                emailFormatContent);
                                                    }
                                                }));

        return contentLocalized;
    }

    private EmailFormatContent buildEmailFormatContent(
            String emailLayoutInput,
            List<LocalizedStringTemplateLanguage> localizedStringTemplateLanguages) {
        return EmailFormatContent.builder()
                .emailLayoutInput(emailLayoutInput)
                .localizedStringTemplate(
                        LocalizedStringTemplate.builder()
                                .localizedTemplateStrings(localizedStringTemplateLanguages)
                                .build())
                .build();
    }
}
