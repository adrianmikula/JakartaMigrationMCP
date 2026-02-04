package adrianmikula.jakartamigration.analysis.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

/**
 * Helper class for JSON serialization/deserialization.
 */
@Slf4j
public class ObjectMapperService {

    private final ObjectMapper objectMapper;

    public ObjectMapperService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * Serializes an object to a JSON string.
     *
     * @param obj The object to serialize
     * @return JSON string representation
     */
    public String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object to JSON", e);
            throw new RuntimeException("JSON serialization failed", e);
        }
    }

    /**
     * Deserializes a JSON string to an object of the specified type.
     *
     * @param json  The JSON string to deserialize
     * @param clazz The class type to deserialize to
     * @param <T>   The type parameter
     * @return The deserialized object
     */
    public <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize JSON to {}", clazz.getSimpleName(), e);
            throw new RuntimeException("JSON deserialization failed", e);
        }
    }

    /**
     * Tries to deserialize a JSON string, returning null on failure.
     *
     * @param json  The JSON string to deserialize
     * @param clazz The class type to deserialize to
     * @param <T>   The type parameter
     * @return The deserialized object, or null if deserialization failed
     */
    public <T> T fromJsonOrNull(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize JSON to {}: {}", clazz.getSimpleName(), e.getMessage());
            return null;
        }
    }
}
