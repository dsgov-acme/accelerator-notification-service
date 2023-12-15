package io.nuvalence.platform.notification.service.utils.jpa;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * JPA converter to map UUID class to db column.
 */
@Converter(autoApply = true)
public class UuidPersistenceConverter implements AttributeConverter<UUID, String> {

    @Override
    public String convertToDatabaseColumn(UUID entityValue) {
        return (entityValue == null) ? null : entityValue.toString();
    }

    @Override
    public UUID convertToEntityAttribute(String databaseValue) {
        return StringUtils.hasLength(databaseValue) ? UUID.fromString(databaseValue.trim()) : null;
    }
}
