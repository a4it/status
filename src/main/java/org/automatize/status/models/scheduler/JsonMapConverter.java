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

    /**
     * Serialises the given map to its JSON string representation for persistence.
     *
     * @param attribute the map to serialise; may be {@code null}
     * @return the JSON string, or {@code null} if the attribute is {@code null}
     * @throws IllegalArgumentException if the map cannot be serialised to JSON
     */
    @Override
    public String convertToDatabaseColumn(Map<String, String> attribute) {
        // Null map serialises to a null column value.
        if (attribute == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error converting Map to JSON string", e);
        }
    }

    /**
     * Deserialises the stored JSON string back into a map.
     *
     * @param dbData the JSON string read from the database; may be {@code null} or blank
     * @return the reconstructed map, or {@code null} if the input is {@code null} or blank
     * @throws IllegalArgumentException if the JSON string cannot be parsed into a map
     */
    @Override
    public Map<String, String> convertToEntityAttribute(String dbData) {
        // Null or blank column value maps back to a null attribute.
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
