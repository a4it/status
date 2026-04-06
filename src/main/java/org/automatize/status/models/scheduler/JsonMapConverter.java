package org.automatize.status.models.scheduler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Map;

/**
 * JPA {@link AttributeConverter} that serialises a {@code Map<String, String>}
 * to a JSON string for storage in a TEXT column, and deserialises it back on
 * read. Handles {@code null} values gracefully by returning {@code null} in
 * both directions.
 */
@Converter
public class JsonMapConverter implements AttributeConverter<Map<String, String>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, String>> TYPE_REF = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(Map<String, String> attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error converting Map to JSON string", e);
        }
    }

    @Override
    public Map<String, String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(dbData, TYPE_REF);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error converting JSON string to Map", e);
        }
    }
}
