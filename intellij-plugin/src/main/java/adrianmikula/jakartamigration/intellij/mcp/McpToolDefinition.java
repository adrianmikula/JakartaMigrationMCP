package adrianmikula.jakartamigration.intellij.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents an MCP tool definition with metadata required by IntelliJ AI Assistant.
 * Includes name, description, and JSON Schema for input validation.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpToolDefinition {

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("inputSchema")
    private InputSchema inputSchema;

    @JsonProperty("serverName")
    private String serverName;

    @JsonProperty("version")
    private String version;

    public McpToolDefinition() {
    }

    public McpToolDefinition(String name, String description, InputSchema inputSchema, String serverName, String version) {
        this.name = name;
        this.description = description;
        this.inputSchema = inputSchema;
        this.serverName = serverName;
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public InputSchema getInputSchema() {
        return inputSchema;
    }

    public void setInputSchema(InputSchema inputSchema) {
        this.inputSchema = inputSchema;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        McpToolDefinition that = (McpToolDefinition) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    /**
     * JSON Schema definition for tool input validation.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class InputSchema {

        @JsonProperty("type")
        private String type;

        @JsonProperty("properties")
        private Map<String, PropertySchema> properties;

        @JsonProperty("required")
        private List<String> required;

        @JsonProperty("additionalProperties")
        private Boolean additionalProperties;

        public InputSchema() {
            this.type = "object";
            this.additionalProperties = false;
        }

        public InputSchema(Map<String, PropertySchema> properties, List<String> required) {
            this.type = "object";
            this.properties = properties;
            this.required = required;
            this.additionalProperties = false;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Map<String, PropertySchema> getProperties() {
            return properties;
        }

        public void setProperties(Map<String, PropertySchema> properties) {
            this.properties = properties;
        }

        public List<String> getRequired() {
            return required;
        }

        public void setRequired(List<String> required) {
            this.required = required;
        }

        public Boolean getAdditionalProperties() {
            return additionalProperties;
        }

        public void setAdditionalProperties(Boolean additionalProperties) {
            this.additionalProperties = additionalProperties;
        }
    }

    /**
     * Schema for individual properties in the input schema.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PropertySchema {

        @JsonProperty("type")
        private String type;

        @JsonProperty("description")
        private String description;

        @JsonProperty("enum")
        private List<String> enumValues;

        @JsonProperty("default")
        private Object defaultValue;

        @JsonProperty("pattern")
        private String pattern;

        @JsonProperty("minLength")
        private Integer minLength;

        @JsonProperty("maxLength")
        private Integer maxLength;

        public PropertySchema() {
        }

        public PropertySchema(String type, String description) {
            this.type = type;
            this.description = description;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public List<String> getEnumValues() {
            return enumValues;
        }

        public void setEnumValues(List<String> enumValues) {
            this.enumValues = enumValues;
        }

        public Object getDefault() {
            return defaultValue;
        }

        public void setDefault(Object defaultValue) {
            this.defaultValue = defaultValue;
        }

        public String getPattern() {
            return pattern;
        }

        public void setPattern(String pattern) {
            this.pattern = pattern;
        }

        public Integer getMinLength() {
            return minLength;
        }

        public void setMinLength(Integer minLength) {
            this.minLength = minLength;
        }

        public Integer getMaxLength() {
            return maxLength;
        }

        public void setMaxLength(Integer maxLength) {
            this.maxLength = maxLength;
        }

        public static PropertySchema string(String description) {
            return new PropertySchema("string", description);
        }

        public static PropertySchema integer(String description) {
            return new PropertySchema("integer", description);
        }

        public static PropertySchema number(String description) {
            return new PropertySchema("number", description);
        }

        public static PropertySchema booleanSchema(String description) {
            return new PropertySchema("boolean", description);
        }

        public static PropertySchema array(String description) {
            return new PropertySchema("array", description);
        }

        public static PropertySchema object(String description) {
            return new PropertySchema("object", description);
        }
    }
}
