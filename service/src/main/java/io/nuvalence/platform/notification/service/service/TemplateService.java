package io.nuvalence.platform.notification.service.service;

import io.nuvalence.auth.token.UserToken;
import io.nuvalence.platform.notification.service.domain.MessageTemplate;
import io.nuvalence.platform.notification.service.model.SearchTemplateFilter;
import io.nuvalence.platform.notification.service.repository.MessageTemplateRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Service for managing {@link io.nuvalence.platform.notification.service.domain.MessageTemplate} entities.
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class TemplateService {

    private final MessageTemplateRepository templateRepository;

    /**
     * Create or update a template.
     *
     * @param key      the template key
     * @param template the template
     * @return the template
     */
    @Transactional
    public MessageTemplate createOrUpdateTemplate(
            final String key, final MessageTemplate template) {
        OffsetDateTime now = OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS);

        Optional<MessageTemplate> templateFound =
                templateRepository.findFirstByKeyOrderByVersionDesc(key);

        MessageTemplate templateToSave =
                new MessageTemplate(key, template, "DRAFT", getCreatedBy().orElse(null), now);

        if (templateFound.isPresent()) {
            MessageTemplate existingTemplate = templateFound.get();
            templateToSave.setId(existingTemplate.getId());
        }

        MessageTemplate resultingMessageTemplate = templateRepository.save(templateToSave);
        return resultingMessageTemplate;
    }

    /**
     * Get a template by key.
     *
     * @param key the template key
     * @return the first template found by key (latest version)
     */
    public Optional<MessageTemplate> getTemplate(final String key) {
        return templateRepository.findFirstByKeyOrderByVersionDesc(key);
    }

    /**
     * Get templates.
     *
     * @param filter the filter
     * @return the templates
     */
    public Page<MessageTemplate> getTemplates(final SearchTemplateFilter filter) {
        return templateRepository.findAll(
                filter.getTemplateSpecifications(), filter.getPageRequest());
    }

    private Optional<String> getCreatedBy() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String createdByUserId = null;
        if ((authentication instanceof UserToken)) {
            final UserToken token = (UserToken) authentication;
            createdByUserId = token.getApplicationUserId();
        }
        return Optional.ofNullable(createdByUserId);
    }
}
