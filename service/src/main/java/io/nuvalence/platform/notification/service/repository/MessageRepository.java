package io.nuvalence.platform.notification.service.repository;

import io.nuvalence.platform.notification.service.domain.Message;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

/**
 * Message repository.
 */
public interface MessageRepository extends CrudRepository<Message, UUID> {
    @Modifying
    @Query("UPDATE Message m SET m.status = :status WHERE m.id = :id")
    void updateMessageStatus(@Param("id") UUID id, @Param("status") String status);
}
