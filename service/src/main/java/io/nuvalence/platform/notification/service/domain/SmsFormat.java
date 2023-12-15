package io.nuvalence.platform.notification.service.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;

import java.util.UUID;

/**
 * Entity representing an SMS format.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(exclude = "messageTemplate")
@ToString(exclude = "messageTemplate")
@Entity
@Table(name = "sms_format")
public class SmsFormat {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", length = 36, updatable = false, nullable = false)
    private UUID id;

    @OneToOne(mappedBy = "smsFormat")
    private MessageTemplate messageTemplate;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "message", referencedColumnName = "id")
    private LocalizedStringTemplate localizedStringTemplate;
}
