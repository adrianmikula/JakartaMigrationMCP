package adrianmikula.jakartamigration.mcp.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for JSON operations in MCP responses.
 * Consolidates duplicate helper methods from various MCP tool classes.
 */
public final class JsonUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule());

    private JsonUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Escapes JSON special characters in strings.
     *
     * @param input The input string to escape
     * @return The escaped string
     */
    public static String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Creates a JSON error response with the given message.
     *
     * @param message Error message to include in response
     * @return JSON error response as a string
     */
    public static String createErrorResponse(String message) {
        return "{\n" +
                "  \"status\": \"error\",\n" +
                "  \"message\": \"" + escapeJson(message) + "\"\n" +
                "}";
    }

    /**
     * Builds a JSON array string from a list of strings.
     *
     * @param list List of strings to convert to JSON array
     * @return JSON array string
     */
    public static String buildStringArray(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        return "[" + list.stream()
                .map(s -> "\"" + escapeJson(s) + "\"")
                .collect(Collectors.joining(", ")) + "]";
    }

    /**
     * Converts an object to JSON string.
     *
     * @param obj Object to serialize
     * @return JSON string
     * @throws JsonProcessingException if serialization fails
     */
    public static String toJson(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize to JSON", e);
        }
    }

    /**
     * Converts a JSON string to a list of objects of the given type.
     *
     * @param json JSON array string
     * @param clazz Class of list elements
     * @return List of deserialized objects
     * @throws JsonProcessingException if deserialization fails
     */
    public static <T> List<T> fromJsonList(String json, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<List<T>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize JSON list", e);
        }
    }

    /**
     * Converts a JSON string to an object of the given type.
     *
     * @param json JSON string
     * @param clazz Class to deserialize to
     * @return Deserialized object
     * @throws JsonProcessingException if deserialization fails
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize JSON", e);
        }
    }
}
