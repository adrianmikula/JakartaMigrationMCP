package adrianmikula.jakartamigration.mcp.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.HashMap;

/**
 * Utility class for creating JSON responses following MCP schema definitions.
 * Uses Jackson ObjectMapper for proper JSON structure and validation.
 */
public class JsonResponseBuilder {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Object> data = new HashMap<>();
    
    public JsonResponseBuilder() {
        // Initialize with common response structure
        data.put("status", "success");
    }
    
    public JsonResponseBuilder status(String status) {
        data.put("status", status);
        return this;
    }
    
    public JsonResponseBuilder status(String status, String message) {
        data.put("status", status);
        if (message != null) {
            data.put("message", message);
        }
        return this;
    }
    
    public JsonResponseBuilder error(String message) {
        return status("error", message);
    }
    
    public JsonResponseBuilder upgradeRequired(String featureName, String featureDescription, 
                                     String requiredTier, String currentTier, 
                                     String upgradeUrl, String upgradeMessage) {
        data.put("status", "upgrade_required");
        data.put("featureName", featureName);
        data.put("featureDescription", featureDescription);
        data.put("requiredTier", requiredTier);
        data.put("currentTier", currentTier);
        data.put("upgradeUrl", upgradeUrl);
        data.put("upgradeMessage", upgradeMessage);
        return this;
    }
    
    public JsonResponseBuilder addField(String key, Object value) {
        data.put(key, value);
        return this;
    }
    
    public JsonResponseBuilder addFields(Map<String, Object> fields) {
        data.putAll(fields);
        return this;
    }
    
    public JsonResponseBuilder addArray(String key, java.util.List<?> items) {
        data.put(key, items);
        return this;
    }
    
    public String build() {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{\"status\":\"error\",\"message\":\"Failed to build JSON response: " + e.getMessage() + "\"}";
        }
    }
}
