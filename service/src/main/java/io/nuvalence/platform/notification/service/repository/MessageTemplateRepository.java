package io.nuvalence.platform.notification.service.repository;

import io.nuvalence.platform.notification.service.domain.MessageTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link MessageTemplate} entities.
 */
public interface MessageTemplateRepository
        extends JpaRepository<MessageTemplate, UUID>, JpaSpecificationExecutor<MessageTemplate> {

    /**
     * Obtain the latest version of the message template by key.
     *
     * @param key email layout key
     * @return message template
     */
    Optional<MessageTemplate> findFirstByKeyOrderByVersionDesc(String key);
}
