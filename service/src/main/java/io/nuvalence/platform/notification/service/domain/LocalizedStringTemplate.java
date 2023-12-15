package io.nuvalence.platform.notification.service.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.GenericGenerator;

import java.util.List;
import java.util.UUID;

/**
 * Entity representing a localized string template.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@ToString
@Entity
@Table(name = "localized_string_template")
public class LocalizedStringTemplate {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", length = 36, updatable = false, nullable = false)
    private UUID id;

    @OneToMany(
            mappedBy = "localizedStringTemplate",
            fetch = jakarta.persistence.FetchType.EAGER,
            cascade = CascadeType.ALL)
    @Fetch(value = FetchMode.SELECT)
    private List<LocalizedStringTemplateLanguage> localizedTemplateStrings;
}
