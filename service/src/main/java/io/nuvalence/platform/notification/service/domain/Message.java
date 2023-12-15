package io.nuvalence.platform.notification.service.domain;

import io.nuvalence.auth.access.AccessResource;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a message to be sent to a user.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@ToString
@AccessResource("message")
@Entity
@Table(name = "message")
public class Message implements Serializable {

    private static final long serialVersionUID = -1428351642619871288L;

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", length = 36, updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", length = 64)
    private String userId;

    @Column(name = "message_template_key")
    private String messageTemplateKey;

    @Column(name = "status")
    private String status;

    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "parameter_name")
    @Column(name = "parameter_value")
    @CollectionTable(
            name = "message_parameter",
            joinColumns = @JoinColumn(name = "message_id", nullable = false))
    private Map<String, String> parameters;

    @Column(name = "requested_timestamp", updatable = false)
    private OffsetDateTime requestedTimestamp;

    @Column(name = "sent_timestamp")
    private OffsetDateTime sentTimestamp;
}
