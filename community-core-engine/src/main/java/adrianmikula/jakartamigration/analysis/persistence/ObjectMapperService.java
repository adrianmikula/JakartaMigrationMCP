package adrianmikula.jakartamigration.analysis.persistence;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Helper class for JSON serialization/deserialization.
 */
@Slf4j
public class ObjectMapperService {

    private final ObjectMapper objectMapper;

    public ObjectMapperService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.registerModule(new Jdk8Module());
        this.objectMapper.registerModule(new ParameterNamesModule());

        SimpleModule pathModule = new SimpleModule();
        pathModule.addSerializer(Path.class, new JsonSerializer<Path>() {
            @Override
            public void serialize(Path value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeString(value.toString());
            }
        });
        pathModule.addDeserializer(Path.class, new JsonDeserializer<Path>() {
            @Override
            public Path deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                return Path.of(p.getValueAsString());
            }
        });
        this.objectMapper.registerModule(pathModule);

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
     * Deserializes a JSON string to an object of the specified generic type.
     *
     * @param json         The JSON string to deserialize
     * @param typeReference The type reference to deserialize to
     * @param <T>          The type parameter
     * @return The deserialized object
     */
    public <T> T fromJson(String json, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize JSON to {}", typeReference.getType(), e);
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
