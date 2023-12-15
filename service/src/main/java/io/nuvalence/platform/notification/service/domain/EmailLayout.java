package io.nuvalence.platform.notification.service.domain;

import io.nuvalence.auth.access.AccessResource;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Base class for all notes.
 */
@Getter
@Setter
@ToString
@AccessResource("email_layout")
@Entity
@Table(name = "email_layout")
public class EmailLayout {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", length = 36, updatable = false, nullable = false)
    private UUID id;

    @Column(name = "key")
    private String key;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "version")
    private Integer version;

    @Column(name = "status")
    private String status;

    @Column(name = "content")
    private String content;

    @Convert(disableConversion = true)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "email_layout_input",
            joinColumns = @JoinColumn(name = "email_layout_id", nullable = false))
    @Column(name = "input")
    private List<String> inputs = new ArrayList<>();

    @Column(name = "createdby", length = 64)
    private String createdBy;

    @Column(name = "created_timestamp", updatable = false)
    private OffsetDateTime createdTimestamp;

    @Column(name = "last_updated_timestamp")
    private OffsetDateTime lastUpdatedTimestamp;
}
