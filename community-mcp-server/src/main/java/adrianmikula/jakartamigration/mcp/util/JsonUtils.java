package adrianmikula.jakartamigration.mcp.util;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for JSON operations in MCP responses.
 * Consolidates duplicate helper methods from various MCP tool classes.
 */
public final class JsonUtils {

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
}
