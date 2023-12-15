package io.nuvalence.platform.notification.service.service;

import io.nuvalence.platform.notification.service.domain.EmailFormat;
import io.nuvalence.platform.notification.service.domain.EmailFormatContent;
import io.nuvalence.platform.notification.service.domain.LocalizedStringTemplate;
import io.nuvalence.platform.notification.service.domain.LocalizedStringTemplateLanguage;
import io.nuvalence.platform.notification.service.domain.MessageTemplate;
import io.nuvalence.platform.notification.service.domain.SmsFormat;
import io.nuvalence.platform.notification.service.exception.BadDataException;
import io.nuvalence.platform.notification.service.repository.MessageTemplateRepository;
import io.nuvalence.platform.notification.service.utils.XmlUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.filterwriter.XLIFFWriter;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.StartGroup;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.filters.xliff.XLIFFFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IllformedLocaleException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Service to handle localization data.
 */
@RequiredArgsConstructor
@Slf4j
@Service
@SuppressWarnings({"checkstyle:ClassFanOutComplexity", "checkstyle:ClassDataAbstractionCoupling"})
public class LocalizationService {

    private final MessageTemplateRepository templateRepository;

    @Value("${localization.default-locale}")
    private String defaultLocale;

    @PostConstruct
    public void init() {
        // validate defaultLocale on startup
        validateLocaleTag(defaultLocale);
    }

    /**
     * Validates a locale tag against IETF BCP 47 format.
     * @param localeTag a locale tag to be validated
     * @throws BadDataException when the locale tag is not valid
     */
    public void validateLocaleTag(String localeTag) throws BadDataException {

        if (localeTag == null) {
            throw new BadDataException("Locale tag should not be null");
        }

        try {
            // validating locale tag
            (new Locale.Builder()).setLanguageTag(localeTag).build();
        } catch (IllformedLocaleException e) {
            throw new BadDataException("Locale tag " + localeTag + " is not IETF valid");
        }
    }

    /**
     * Returns the localization data for a given locale tag.
     * @param localeTag an IETF BCP 47 locale tag to be used as target locale
     * @return a valid XLIFF string with the localization data
     * @throws BadDataException when the locale tag is not valid
     */
    public String getLocalizationData(String localeTag) {

        validateLocaleTag(localeTag);

        List<MessageTemplate> templates = templateRepository.findAll();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (XLIFFWriter writer = new XLIFFWriter()) {

            LocaleId sourceLocale = LocaleId.fromString(defaultLocale);
            LocaleId targetLocale = LocaleId.fromString(localeTag);

            writer.setOutput(baos);

            writer.create(
                    null,
                    null,
                    sourceLocale,
                    targetLocale,
                    "plaintext",
                    "notification-service",
                    " Please use 'resname' attribute on all 'group' and 'trans-unit' elements ");

            templates.forEach(template -> writeTemplateGroup(writer, template, targetLocale));
            writer.close();
            return XmlUtils.xmlValidateAndMinify(baos.toString(StandardCharsets.UTF_8));

        } catch (Exception e) {
            throw new BadDataException("Error generating localization data" + e.getMessage());
        }
    }

    private void writeTemplateGroup(
            XLIFFWriter writer, MessageTemplate template, LocaleId targetLocale) {

        var group = new StartGroup(null, template.getKey());
        group.setName(template.getKey());
        writer.writeStartGroup(group);

        writeSmsGroup(writer, template, targetLocale);

        writeEmailGroup(writer, template, targetLocale);

        writer.writeEndGroup();
    }

    private void writeEmailGroup(
            XLIFFWriter writer, MessageTemplate template, LocaleId targetLocale) {

        String emailResname = "email";
        var group = new StartGroup(null, emailResname);
        group.setName(emailResname);
        writer.writeStartGroup(group);

        var emailFormat = template.getEmailFormat();

        // subject
        var langStrings =
                Optional.ofNullable(emailFormat)
                        .map(EmailFormat::getLocalizedSubjectStringTemplate)
                        .map(LocalizedStringTemplate::getLocalizedTemplateStrings)
                        .orElse(List.of());

        writeTextUnit(writer, "subject", langStrings, targetLocale);

        writeEmailContents(writer, emailFormat, targetLocale);

        writer.writeEndGroup();
    }

    private void writeEmailContents(
            XLIFFWriter writer, EmailFormat emailFormat, LocaleId targetLocale) {
        // contents
        String contentResname = "content";
        var group = new StartGroup(null, contentResname);
        group.setName(contentResname);
        writer.writeStartGroup(group);

        var formatContents =
                Optional.ofNullable(emailFormat)
                        .map(EmailFormat::getEmailFormatContents)
                        .orElse(List.of());

        formatContents = formatContentsDeduplicate(formatContents);

        for (var formatContent : formatContents) {
            var resourceName = formatContent.getEmailLayoutInput();
            var langStrings =
                    Optional.of(formatContent)
                            .map(EmailFormatContent::getLocalizedStringTemplate)
                            .map(LocalizedStringTemplate::getLocalizedTemplateStrings)
                            .orElse(List.of());
            writeTextUnit(writer, resourceName, langStrings, targetLocale);
        }

        writer.writeEndGroup();
    }

    private List<EmailFormatContent> formatContentsDeduplicate(
            List<EmailFormatContent> toDeduplicate) {

        var deduplicaterMap = new HashMap<String, EmailFormatContent>();
        for (var formatContent : toDeduplicate) {
            deduplicaterMap.put(formatContent.getEmailLayoutInput(), formatContent);
        }

        return new ArrayList<>(deduplicaterMap.values());
    }

    private void writeSmsGroup(
            XLIFFWriter writer, MessageTemplate template, LocaleId targetLocale) {

        String smsResname = "sms";
        var group = new StartGroup(null, smsResname);
        group.setName(smsResname);
        writer.writeStartGroup(group);

        var langStrings =
                Optional.ofNullable(template)
                        .map(MessageTemplate::getSmsFormat)
                        .map(SmsFormat::getLocalizedStringTemplate)
                        .map(LocalizedStringTemplate::getLocalizedTemplateStrings)
                        .orElse(List.of());

        writeTextUnit(writer, "message", langStrings, targetLocale);

        writer.writeEndGroup();
    }

    private void writeTextUnit(
            XLIFFWriter writer,
            String resourceName,
            List<LocalizedStringTemplateLanguage> langStrings,
            LocaleId targetLocale) {

        String sourceValue = "";
        String targetValue = "";

        for (var lang : langStrings) {
            if (lang.getLanguage().equals(defaultLocale)) {
                sourceValue = lang.getTemplate();
            }
            if (lang.getLanguage().equals(targetLocale.toBCP47())) {
                targetValue = lang.getTemplate();
            }
        }

        ITextUnit tu = new TextUnit(resourceName);
        tu.setName(resourceName);
        tu.setSourceContent(new TextFragment(sourceValue));
        tu.setTargetContent(targetLocale, new TextFragment(targetValue));
        writer.writeTextUnit(tu);
    }

    /**
     * Parses the provided XLIFF file and updates the existing message templates for all the 
     * matching configurations.
     * Those not matching are ignored, sine this method is not intended to create new message
     * templates, but rather add language data to existing ones.
     * @param xliffFileString the XLIFF file with language support data to parse
     * @return a pair with the target locale and the list of message templates updated but pending to be persisted
     * @throws BadDataException when the provided XLIFF file is not valid
     */
    public Pair<String, List<MessageTemplate>> parseXliffToExistingMsgTemplates(
            String xliffFileString) {

        try (XLIFFFilter filter = new XLIFFFilter()) {

            XmlUtils.xmlValidateAndMinify(xliffFileString);

            ByteArrayInputStream stream =
                    new ByteArrayInputStream(xliffFileString.getBytes(StandardCharsets.UTF_8));

            var document =
                    new RawDocument(
                            stream,
                            RawDocument.UNKOWN_ENCODING,
                            LocaleId.AUTODETECT,
                            LocaleId.AUTODETECT);

            filter.open(document);

            var documentPartFound = false;
            String targetLocale = null;
            List<MessageTemplate> updatedTemplatesToPersist = new ArrayList<>();
            while (filter.hasNext()) {
                Event event = filter.next();
                switch (event.getEventType()) {
                    case DOCUMENT_PART:
                        if (documentPartFound) {
                            throw new BadDataException(
                                    "There is more than one document part in the provided"
                                            + " localization data");
                        }
                        documentPartFound = true;
                        targetLocale = filter.getCurrentTargetLocale().toBCP47();

                        if (targetLocale.equals("und")) {
                            throw new BadDataException("Target locale is not defined");
                        }

                        validateLocaleTag(targetLocale);

                        break;

                    case START_GROUP:
                        var template = parseXliffMainGroup(filter, event, targetLocale);
                        if (template != null) {
                            updatedTemplatesToPersist.add(template);
                        }

                        break;

                    default:
                        break;
                }
            }

            return Pair.of(Objects.requireNonNull(targetLocale), updatedTemplatesToPersist);

        } catch (Exception e) {
            throw new BadDataException(
                    "Error parsing provided localization data. No data was saved. "
                            + e.getMessage());
        }
    }

    private MessageTemplate parseXliffMainGroup(
            XLIFFFilter filter, Event event, String targetLocale) {

        if (targetLocale == null) {
            throw new BadDataException("Target locale was not found in the provided XLIFF file");
        }

        String groupName = getGroupName(event);
        var messageTemplate =
                templateRepository.findFirstByKeyOrderByVersionDesc(groupName).orElse(null);

        var subEvent = filter.next();
        switch (subEvent.getEventType()) {
            case START_GROUP:
                parseXliffMessageFormats(filter, subEvent, messageTemplate);
                break;
            case END_GROUP:
                break;
            default:
                unsupportedXliffStructure();
                break;
        }

        return messageTemplate;
    }

    /**
     * Parse the XLIFF structure for a message format, such as SMS or Email.
     */
    private void parseXliffMessageFormats(
            XLIFFFilter filter, Event event, MessageTemplate messageTemplate) {

        String formatName = getGroupName(event).toLowerCase(Locale.ENGLISH);
        switch (formatName) {
            case "sms":
                parseXliffSmsFormat(filter, messageTemplate);
                filter.next();
                parseXliffEmailFormat(filter, messageTemplate);
                break;
            case "email":
                parseXliffEmailFormat(filter, messageTemplate);
                filter.next();
                parseXliffSmsFormat(filter, messageTemplate);
                break;
            default:
                unsupportedXliffStructure();
                break;
        }
    }

    private void parseXliffEmailFormat(XLIFFFilter filter, MessageTemplate messageTemplate) {
        var subEvent = filter.next();
        switch (subEvent.getEventType()) {
            case END_GROUP:
                break;
            case TEXT_UNIT:
                parseEmailSubject(filter, subEvent, messageTemplate);
                subEvent = filter.next();
                parseEmailContents(filter, subEvent, messageTemplate);
                break;

            case START_GROUP:
                parseEmailContents(filter, subEvent, messageTemplate);
                subEvent = filter.next();
                parseEmailSubject(filter, subEvent, messageTemplate);
                break;

            default:
                unsupportedXliffStructure();
                break;
        }
        subEvent = filter.next();
        if (!subEvent.isEndGroup()) {
            unsupportedXliffStructure();
        }
    }

    private void parseEmailContents(
            XLIFFFilter filter, Event subEvent, MessageTemplate messageTemplate) {

        String emailSubGroupName = getGroupName(subEvent).toLowerCase(Locale.ENGLISH);

        if (!emailSubGroupName.equals("content")) {
            unsupportedXliffStructure();
        }

        for (var contentTypeEvent = filter.next();
                contentTypeEvent.getEventType() != EventType.END_GROUP;
                contentTypeEvent = filter.next()) {

            if (contentTypeEvent.getEventType() != EventType.TEXT_UNIT) {
                unsupportedXliffStructure();
            }

            var contentAndData = readTextUnitData(filter, contentTypeEvent);
            if (contentAndData.getFirst().isBlank()) {
                unsupportedXliffStructure();
            }
            if (!contentAndData.getSecond().isBlank()) {
                var formatContents =
                        (messageTemplate != null && messageTemplate.getEmailFormat() != null)
                                ? messageTemplate.getEmailFormat().getEmailFormatContents()
                                : null;

                if (formatContents != null) {

                    formatContents = formatContentsDeduplicate(formatContents);

                    messageTemplate.getEmailFormat().setEmailFormatContents(formatContents);

                    formatContents.stream()
                            .filter(
                                    formatContent ->
                                            formatContent
                                                    .getEmailLayoutInput()
                                                    .equals(contentAndData.getFirst()))
                            .findFirst()
                            .ifPresent(
                                    formatContent -> {
                                        var langStrings =
                                                Optional.of(formatContent)
                                                        .map(
                                                                EmailFormatContent
                                                                        ::getLocalizedStringTemplate)
                                                        .map(
                                                                LocalizedStringTemplate
                                                                        ::getLocalizedTemplateStrings)
                                                        .orElse(null);

                                        addDataToLangStrings(
                                                langStrings,
                                                filter.getCurrentTargetLocale(),
                                                contentAndData.getSecond());
                                    });
                }
            }
        }
    }

    private void parseEmailSubject(
            XLIFFFilter filter, Event subEvent, MessageTemplate messageTemplate) {

        var nameAndData = readTextUnitData(filter, subEvent);
        if (!nameAndData.getFirst().equalsIgnoreCase("subject")) {
            unsupportedXliffStructure();
        }
        if (!nameAndData.getSecond().isBlank()) {
            var langStrings =
                    Optional.ofNullable(messageTemplate)
                            .map(MessageTemplate::getEmailFormat)
                            .map(EmailFormat::getLocalizedSubjectStringTemplate)
                            .map(LocalizedStringTemplate::getLocalizedTemplateStrings)
                            .orElse(null);

            addDataToLangStrings(
                    langStrings, filter.getCurrentTargetLocale(), nameAndData.getSecond());
        }
    }

    private void parseXliffSmsFormat(XLIFFFilter filter, MessageTemplate messageTemplate) {

        var subEvent = filter.next();
        switch (subEvent.getEventType()) {
            case END_GROUP:
                break;
            case TEXT_UNIT:
                var nameAndData = readTextUnitData(filter, subEvent);
                if (!nameAndData.getFirst().equalsIgnoreCase("message")) {
                    unsupportedXliffStructure();
                }
                if (!nameAndData.getSecond().isBlank()) {
                    var langStrings =
                            Optional.ofNullable(messageTemplate)
                                    .map(MessageTemplate::getSmsFormat)
                                    .map(SmsFormat::getLocalizedStringTemplate)
                                    .map(LocalizedStringTemplate::getLocalizedTemplateStrings)
                                    .orElse(null);

                    addDataToLangStrings(
                            langStrings, filter.getCurrentTargetLocale(), nameAndData.getSecond());
                }
                if (!filter.next().getEventType().equals(EventType.END_GROUP)) {
                    unsupportedXliffStructure();
                }
                break;

            default:
                unsupportedXliffStructure();
                break;
        }
    }

    private void addDataToLangStrings(
            List<LocalizedStringTemplateLanguage> langStrings,
            LocaleId targetLocaleId,
            String data) {
        if (langStrings != null) {
            Optional<LocalizedStringTemplateLanguage> existingLangString =
                    langStrings.stream()
                            .filter(
                                    langString ->
                                            langString
                                                    .getLanguage()
                                                    .equals(targetLocaleId.toBCP47()))
                            .findFirst();

            if (existingLangString.isPresent()) {
                existingLangString.get().setTemplate(data);
            } else {
                langStrings.add(
                        LocalizedStringTemplateLanguage.builder()
                                .language(targetLocaleId.toBCP47())
                                .template(data)
                                .localizedStringTemplate(
                                        langStrings.get(0).getLocalizedStringTemplate())
                                .build());
            }
        }
    }

    /** Throws a common XLIFF structure validation exception. */
    private void unsupportedXliffStructure() {
        throw new BadDataException(
                "Unsupported XLIFF structure. Please get a new XLIFF file from this API to"
                        + " get the proper format.");
    }

    /**
     * Get the group resname of an event previously checked as a START_GROUP.
     */
    private String getGroupName(Event event) {
        StartGroup group = event.getStartGroup();
        if (group.getName() == null) {
            throw new BadDataException(
                    "There is at least one group missing the resname attribute needed for "
                            + " message template mapping");
        }
        return group.getName().trim();
    }

    /**
     * Get the resname and target data of an event previously checked as a TEXT_UNIT.
     */
    private Pair<String, String> readTextUnitData(XLIFFFilter filter, Event event) {
        ITextUnit tu = event.getTextUnit();
        if (tu.getName() == null) {
            throw new BadDataException(
                    "There is at least one trans-unit missing the resname attribute needed for "
                            + " message template mapping");
        }
        String targetText = "";
        if (tu.hasTarget(filter.getCurrentTargetLocale())) {
            targetText = tu.getTarget(filter.getCurrentTargetLocale()).toString();
        }
        return Pair.of(tu.getName().trim(), targetText.trim());
    }
}
