package adrianmikula.jakartamigration.intellij.mcp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for McpToolRegistry and McpToolDefinition.
 * Verifies that all MCP tools have proper metadata and JSON schemas.
 */
class McpToolRegistryTest {

    private McpToolRegistry toolRegistry;

    @BeforeEach
    void setUp() {
        // The registry is static, so we don't need to create an instance
    }

    @Test
    void testGetAllToolsReturnsNonEmptyList() {
        List<McpToolDefinition> tools = McpToolRegistry.getAllTools();
        assertNotNull(tools, "Tools list should not be null");
        assertFalse(tools.isEmpty(), "Tools list should not be empty");
        assertEquals(9, tools.size(), "Expected 9 MCP tools");
    }

    @Test
    void testAllToolsHaveRequiredFields() {
        List<McpToolDefinition> tools = McpToolRegistry.getAllTools();

        for (McpToolDefinition tool : tools) {
            assertNotNull(tool.getName(), "Tool name should not be null: " + tool);
            assertFalse(tool.getName().isEmpty(), "Tool name should not be empty");
            
            assertNotNull(tool.getDescription(), "Tool description should not be null: " + tool.getName());
            assertFalse(tool.getDescription().isEmpty(), "Tool description should not be empty");
            assertTrue(tool.getDescription().length() > 50, 
                    "Tool description should be descriptive: " + tool.getName());
            
            assertNotNull(tool.getServerName(), "Server name should not be null: " + tool.getName());
            assertNotNull(tool.getVersion(), "Version should not be null: " + tool.getName());
            assertNotNull(tool.getInputSchema(), "Input schema should not be null: " + tool.getName());
        }
    }

    @Test
    void testAllToolsHaveValidInputSchema() {
        List<McpToolDefinition> tools = McpToolRegistry.getAllTools();

        for (McpToolDefinition tool : tools) {
            McpToolDefinition.InputSchema schema = tool.getInputSchema();
            assertNotNull(schema, "Schema should not be null: " + tool.getName());
            assertEquals("object", schema.getType(), "Schema type should be 'object': " + tool.getName());
            
            // All tools should have at least one property
            assertNotNull(schema.getProperties(), "Properties should not be null: " + tool.getName());
            assertFalse(schema.getProperties().isEmpty(), 
                    "Tool should have at least one property: " + tool.getName());
            
            // All tools should have required fields
            assertNotNull(schema.getRequired(), "Required fields should not be null: " + tool.getName());
            assertFalse(schema.getRequired().isEmpty(), 
                    "Tool should have at least one required field: " + tool.getName());
        }
    }

    @Test
    void testToolNamesAreUnique() {
        List<McpToolDefinition> tools = McpToolRegistry.getAllTools();
        long uniqueCount = tools.stream()
                .map(McpToolDefinition::getName)
                .distinct()
                .count();
        assertEquals(tools.size(), uniqueCount, "All tool names should be unique");
    }

    @Test
    void testAnalyzeJakartaReadinessTool() {
        McpToolDefinition tool = McpToolRegistry.getAllTools().stream()
                .filter(t -> "analyzeJakartaReadiness".equals(t.getName()))
                .findFirst()
                .orElse(null);

        assertNotNull(tool, "analyzeJakartaReadiness tool should exist");
        assertTrue(tool.getDescription().contains("readiness"),
                "Description should mention readiness");
        
        // Verify required fields
        assertTrue(tool.getInputSchema().getRequired().contains("projectPath"),
                "projectPath should be required");
    }

    @Test
    void testAnalyzeMigrationImpactTool() {
        McpToolDefinition tool = McpToolRegistry.getAllTools().stream()
                .filter(t -> "analyzeMigrationImpact".equals(t.getName()))
                .findFirst()
                .orElse(null);

        assertNotNull(tool, "analyzeMigrationImpact tool should exist");
        assertTrue(tool.getDescription().contains("impact"),
                "Description should mention impact");
    }

    @Test
    void testApplyOpenRewriteRefactoringTool() {
        McpToolDefinition tool = McpToolRegistry.getAllTools().stream()
                .filter(t -> "applyOpenRewriteRefactoring".equals(t.getName()))
                .findFirst()
                .orElse(null);

        assertNotNull(tool, "applyOpenRewriteRefactoring tool should exist");
        
        McpToolDefinition.InputSchema schema = tool.getInputSchema();
        assertTrue(schema.getRequired().contains("filePatterns"),
                "filePatterns should be required for refactoring tool");
    }

    @Test
    void testServerMetadata() {
        Map<String, Object> metadata = McpToolRegistry.getServerMetadata();
        
        assertNotNull(metadata, "Metadata should not be null");
        assertEquals("jakarta-migration-mcp", metadata.get("name"));
        assertEquals("1.0.0", metadata.get("version"));
        
        assertNotNull(metadata.get("capabilities"), "Capabilities should be present");
        assertTrue((Boolean) ((Map<?, ?>) metadata.get("capabilities")).get("tools"),
                "Tools capability should be enabled");
    }

    @Test
    void testToolPropertySchemasHaveDescriptions() {
        List<McpToolDefinition> tools = McpToolRegistry.getAllTools();

        for (McpToolDefinition tool : tools) {
            Map<String, McpToolDefinition.PropertySchema> properties = 
                    tool.getInputSchema().getProperties();
            
            for (Map.Entry<String, McpToolDefinition.PropertySchema> entry : properties.entrySet()) {
                McpToolDefinition.PropertySchema propSchema = entry.getValue();
                assertNotNull(propSchema.getDescription(), 
                        "Property '" + entry.getKey() + "' in tool '" + tool.getName() + 
                        "' should have a description");
                assertFalse(propSchema.getDescription().isEmpty(),
                        "Property description should not be empty: " + entry.getKey());
            }
        }
    }

    @Test
    void testPropertyTypes() {
        List<McpToolDefinition> tools = McpToolRegistry.getAllTools();

        for (McpToolDefinition tool : tools) {
            Map<String, McpToolDefinition.PropertySchema> properties = 
                    tool.getInputSchema().getProperties();
            
            for (McpToolDefinition.PropertySchema propSchema : properties.values()) {
                assertNotNull(propSchema.getType(), 
                        "Property type should not be null: " + tool.getName());
                assertTrue(
                        propSchema.getType().equals("string") ||
                        propSchema.getType().equals("integer") ||
                        propSchema.getType().equals("number") ||
                        propSchema.getType().equals("boolean") ||
                        propSchema.getType().equals("array") ||
                        propSchema.getType().equals("object"),
                        "Invalid property type: " + propSchema.getType());
            }
        }
    }

    @Test
    void testProjectPathIsRequiredForAllAnalysisTools() {
        List<McpToolDefinition> tools = McpToolRegistry.getAllTools();

        for (McpToolDefinition tool : tools) {
            McpToolDefinition.InputSchema schema = tool.getInputSchema();
            if (schema.getProperties().containsKey("projectPath")) {
                assertTrue(schema.getRequired().contains("projectPath"),
                        "projectPath should be required for tool: " + tool.getName());
            }
        }
    }
}
