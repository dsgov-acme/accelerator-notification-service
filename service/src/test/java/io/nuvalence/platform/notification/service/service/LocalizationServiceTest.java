package io.nuvalence.platform.notification.service.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import io.nuvalence.platform.notification.service.exception.BadDataException;
import io.nuvalence.platform.notification.service.repository.MessageTemplateRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LocalizationServiceTest {

    private LocalizationService service;
    private MessageTemplateRepository templateRepository;

    @BeforeAll
    static void init() {}

    @BeforeEach
    void setUp() {
        templateRepository = mock(MessageTemplateRepository.class);
        service = new LocalizationService(templateRepository);
    }

    @Test
    void locateTagsValidation() {
        service.validateLocaleTag("en");
        service.validateLocaleTag("en-US");
        service.validateLocaleTag("es-US");
        service.validateLocaleTag("es-CO");
        // assert it throws a BadDataException with specific message validation
        assertThrows(BadDataException.class, () -> service.validateLocaleTag("en-US-ES"));
        assertThrows(BadDataException.class, () -> service.validateLocaleTag(""));
        assertThrows(BadDataException.class, () -> service.validateLocaleTag(null));
    }
}
