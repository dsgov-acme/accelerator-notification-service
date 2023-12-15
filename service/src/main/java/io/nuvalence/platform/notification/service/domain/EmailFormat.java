package io.nuvalence.platform.notification.service.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;

import java.util.List;
import java.util.UUID;

/**
 * Entity representing an Email format.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(exclude = "messageTemplate")
@ToString(exclude = "messageTemplate")
@Entity
@Table(name = "email_format")
public class EmailFormat {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", length = 36, updatable = false, nullable = false)
    private UUID id;

    @OneToOne(mappedBy = "emailFormat")
    private MessageTemplate messageTemplate;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "subject", referencedColumnName = "id")
    private LocalizedStringTemplate localizedSubjectStringTemplate;

    @OneToMany(
            mappedBy = "emailFormat",
            cascade = CascadeType.ALL,
            fetch = jakarta.persistence.FetchType.EAGER)
    private List<EmailFormatContent> emailFormatContents;
}
