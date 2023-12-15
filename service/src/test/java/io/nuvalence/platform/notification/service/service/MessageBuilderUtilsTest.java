package io.nuvalence.platform.notification.service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import io.nuvalence.platform.notification.service.domain.LocalizedStringTemplate;
import io.nuvalence.platform.notification.service.domain.LocalizedStringTemplateLanguage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class MessageBuilderUtilsTest {

    @Mock LocalizedStringTemplate localizedStringTemplate;

    private LocalizedStringTemplateLanguage englishPlain;

    private LocalizedStringTemplateLanguage englishUs;

    private LocalizedStringTemplateLanguage spanishPlain;

    private LocalizedStringTemplateLanguage spanishSpain;

    @BeforeEach
    void setUp() {
        englishPlain = new LocalizedStringTemplateLanguage();
        englishPlain.setLanguage("en");

        englishUs = new LocalizedStringTemplateLanguage();
        englishUs.setLanguage("en-US");

        spanishPlain = new LocalizedStringTemplateLanguage();
        spanishPlain.setLanguage("es");

        spanishSpain = new LocalizedStringTemplateLanguage();
        spanishSpain.setLanguage("es-ES");
    }

    @Test
    void getLocalizedTemplateTest_LanguageAndRegionMatch() {
        when(localizedStringTemplate.getLocalizedTemplateStrings())
                .thenReturn(List.of(spanishSpain, englishUs));
        Optional<LocalizedStringTemplateLanguage> result =
                MessageBuilderUtils.getLocalizedTemplate(localizedStringTemplate, "en-US");

        assertTrue(result.isPresent());
        assertEquals(englishUs, result.get());
    }

    @Test
    void getLocalizedTemplateTest_LanguageMatchesRegionDoesNot_GeneralUserPreference() {
        when(localizedStringTemplate.getLocalizedTemplateStrings())
                .thenReturn(List.of(englishUs, spanishSpain));
        Optional<LocalizedStringTemplateLanguage> result =
                MessageBuilderUtils.getLocalizedTemplate(localizedStringTemplate, "en");

        assertTrue(result.isPresent());
        assertEquals(englishUs, result.get());
    }

    @Test
    void getLocalizedTemplateTest_LanguageAndRegionDoNotMatch_GeneralTemplateLanguage() {
        when(localizedStringTemplate.getLocalizedTemplateStrings())
                .thenReturn(List.of(englishPlain, spanishPlain));
        Optional<LocalizedStringTemplateLanguage> result =
                MessageBuilderUtils.getLocalizedTemplate(localizedStringTemplate, "en-US");

        assertTrue(result.isPresent());
        assertEquals(englishPlain, result.get());
    }

    @Test
    void getLocalizedTemplateTest_LanguageAndRegionDoNotMatch() {
        when(localizedStringTemplate.getLocalizedTemplateStrings())
                .thenReturn(List.of(englishPlain, spanishPlain));
        Optional<LocalizedStringTemplateLanguage> result =
                MessageBuilderUtils.getLocalizedTemplate(localizedStringTemplate, "fr-FR");

        assertTrue(result.isEmpty());
    }
}
