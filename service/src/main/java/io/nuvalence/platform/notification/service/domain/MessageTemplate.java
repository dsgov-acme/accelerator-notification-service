package io.nuvalence.platform.notification.service.domain;

import io.nuvalence.auth.access.AccessResource;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Template entity.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@ToString
@AccessResource("message_template")
@Entity
@Table(name = "message_template")
public class MessageTemplate {

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

    @Column(name = "email_layout_key")
    private String emailLayoutKey;

    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "parameter_name")
    @Column(name = "parameter_type")
    @CollectionTable(
            name = "message_template_parameter",
            joinColumns = @JoinColumn(name = "message_template_id", nullable = false))
    private Map<String, String> parameters;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "sms_format_id", referencedColumnName = "id")
    private SmsFormat smsFormat;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "email_format_id", referencedColumnName = "id")
    private EmailFormat emailFormat;

    @Column(name = "createdby", length = 64)
    private String createdBy;

    @Column(name = "created_timestamp", updatable = false)
    private OffsetDateTime createdTimestamp;

    @Column(name = "last_updated_timestamp")
    private OffsetDateTime lastUpdatedTimestamp;

    /**
     * Constructor for creating a new template.
     *
     * @param key      the template key
     * @param that  the template to base on
     * @param status   the template status
     * @param createdBy the user who created the template
     * @param time the time the template was created
     */
    public MessageTemplate(
            String key,
            MessageTemplate that,
            String status,
            String createdBy,
            OffsetDateTime time) {
        this.name = that.getName();
        this.description = that.getDescription();
        this.key = key;
        this.status = status;
        this.version = 0;
        this.parameters = that.getParameters();
        this.emailLayoutKey = that.getEmailLayoutKey();
        this.smsFormat = that.getSmsFormat();
        this.emailFormat = that.getEmailFormat();
        this.createdBy = createdBy;
        this.createdTimestamp = time;
        this.lastUpdatedTimestamp = time;
    }
}
