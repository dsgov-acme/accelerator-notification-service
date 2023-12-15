package io.nuvalence.platform.notification.service.service;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.TagType;
import com.github.jknack.handlebars.Template;
import io.nuvalence.platform.notification.service.domain.LocalizedStringTemplate;
import io.nuvalence.platform.notification.service.domain.LocalizedStringTemplateLanguage;
import io.nuvalence.platform.notification.service.exception.TemplateCompilationException;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Utility class for message builders.
 */
@Slf4j
public class MessageBuilderUtils {

    private MessageBuilderUtils() {
        // private constructor
    }

    /**
     * Get the localized template for a given language.If there is not a direct match,
     * the non-country-specific language is obtained, following BCP 47 standard for language tags.
     *
     * @param localizedStringTemplate localized string template
     * @param language                language
     * @return localized template
     */
    public static Optional<LocalizedStringTemplateLanguage> getLocalizedTemplate(
            LocalizedStringTemplate localizedStringTemplate, String language) {

        String languagePart = language.contains("-") ? language.split("-")[0] : language;

        Optional<LocalizedStringTemplateLanguage> result =
                localizedStringTemplate.getLocalizedTemplateStrings().stream()
                        .filter(
                                localizedTemplateString ->
                                        localizedTemplateString.getLanguage().equals(language))
                        .findFirst();

        if (result.isEmpty()) {
            result =
                    localizedStringTemplate.getLocalizedTemplateStrings().stream()
                            .filter(
                                    localizedTemplateString ->
                                            localizedTemplateString
                                                    .getLanguage()
                                                    .split("-")[0]
                                                    .equals(languagePart))
                            .findFirst();
        }

        return result;
    }

    /**
     * Replace parameters in a template.
     *
     * @param template    template
     * @param parameters  parameters
     * @param handlebars  handlebars
     * @return template with parameters replaced
     * @throws TemplateCompilationException when an error occurs compiling the template
     */
    public static String replaceParameterInTemplate(
            String template, Map<String, String> parameters, Handlebars handlebars) {
        try {
            Template handleBarTemplate = handlebars.compileInline(template);
            return handleBarTemplate.apply(parameters);
        } catch (Exception e) {
            log.error("Error compiling template: {}", template, e);
            throw new TemplateCompilationException(template, e);
        }
    }

    /**
     * Get the variables in a template.
     *
     * @param template   template
     * @param handlebars handlebars
     * @return variables in template
     * @throws TemplateCompilationException when an error occurs compiling the template
     */
    public static Set<String> getVariablesInTemplate(String template, Handlebars handlebars) {
        try {
            Template handleBarTemplate = handlebars.compileInline(template);
            return new HashSet<>(handleBarTemplate.collect(TagType.VAR));
        } catch (Exception e) {
            log.error("Error compiling template: {}", template, e);
            throw new TemplateCompilationException(template, e);
        }
    }
}
