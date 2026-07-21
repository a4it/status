package org.automatize.status.models.scheduler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

/**
 * JPA {@link AttributeConverter} that serialises a {@code List<String>}
 * to a JSON array string for storage in a TEXT column, and deserialises it
 * back on read. Handles {@code null} values gracefully by returning
 * {@code null} in both directions.
 */
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> TYPE_REF = new TypeReference<>() {};

    /**
     * Serialises the given list to its JSON array string representation for persistence.
     *
     * @param attribute the list to serialise; may be {@code null}
     * @return the JSON array string, or {@code null} if the attribute is {@code null}
     * @throws IllegalArgumentException if the list cannot be serialised to JSON
     */
    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        // Null list serialises to a null column value.
        if (attribute == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error converting List to JSON string", e);
        }
    }

    /**
     * Deserialises the stored JSON array string back into a list.
     *
     * @param dbData the JSON array string read from the database; may be {@code null} or blank
     * @return the reconstructed list, or {@code null} if the input is {@code null} or blank
     * @throws IllegalArgumentException if the JSON string cannot be parsed into a list
     */
    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        // Null or blank column value maps back to a null attribute.
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(dbData, TYPE_REF);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error converting JSON string to List", e);
        }
    }
}
