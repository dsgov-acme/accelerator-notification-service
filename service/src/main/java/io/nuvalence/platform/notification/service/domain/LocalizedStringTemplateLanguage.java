package io.nuvalence.platform.notification.service.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
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
 * Entity representing a localized string template language.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(exclude = "localizedStringTemplate")
@ToString(exclude = "localizedStringTemplate")
@Entity
@Table(name = "localized_string_template_language")
public class LocalizedStringTemplateLanguage {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", length = 36, updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    private LocalizedStringTemplate localizedStringTemplate;

    @Column(name = "language")
    private String language;

    @Column(name = "template")
    private String template;
}
