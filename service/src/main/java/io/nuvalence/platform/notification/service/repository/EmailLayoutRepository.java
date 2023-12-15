package io.nuvalence.platform.notification.service.repository;

import io.nuvalence.platform.notification.service.domain.EmailLayout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link EmailLayout} entities.
 */
public interface EmailLayoutRepository
        extends JpaRepository<EmailLayout, UUID>, JpaSpecificationExecutor<EmailLayout> {

    /**
     * Obtain the latest version of the email layout by key.
     *
     * @param key email layout key
     * @return email layout
     */
    Optional<EmailLayout> findFirstByKeyOrderByVersionDesc(String key);
}
