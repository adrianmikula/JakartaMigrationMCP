package adrianmikula.jakartamigration.intellij.mcp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JakartaMcpServerProvider.
 * Verifies MCP server provider initialization and tool registration.
 */
class JakartaMcpServerProviderTest {

    private JakartaMcpServerProvider serverProvider;

    @BeforeEach
    void setUp() {
        serverProvider = new JakartaMcpServerProvider();
    }

    @Test
    void testInitialization() {
        serverProvider.initialize();
        
        assertTrue(serverProvider.isReady(), "Server should be ready after initialization");
        assertEquals(9, serverProvider.getToolCount(), "Should have 9 tools registered");
    }

    @Test
    void testGetServerId() {
        assertEquals("jakarta-migration-mcp", serverProvider.getServerId());
    }

    @Test
    void testGetServerName() {
        assertEquals("Jakarta Migration MCP", serverProvider.getServerName());
    }

    @Test
    void testGetServerVersion() {
        assertEquals("1.0.0", serverProvider.getServerVersion());
    }

    @Test
    void testGetServerMetadata() {
        serverProvider.initialize();
        Map<String, Object> metadata = serverProvider.getServerMetadata();
        
        assertNotNull(metadata, "Metadata should not be null");
        assertEquals("jakarta-migration-mcp", metadata.get("name"));
        assertEquals("1.0.0", metadata.get("version"));
        assertTrue((Boolean) metadata.get("autoLoad"), "autoLoad should be true");
    }

    @Test
    void testGetTools() {
        serverProvider.initialize();
        
        var tools = serverProvider.getTools();
        assertNotNull(tools, "Tools list should not be null");
        assertFalse(tools.isEmpty(), "Tools list should not be empty");
        assertEquals(9, tools.size(), "Should have 9 tools");
    }

    @Test
    void testGetToolByName() {
        serverProvider.initialize();
        
        McpToolDefinition tool = serverProvider.getTool("analyzeJakartaReadiness");
        assertNotNull(tool, "Should find analyzeJakartaReadiness tool");
        assertEquals("analyzeJakartaReadiness", tool.getName());
        
        McpToolDefinition nonExistent = serverProvider.getTool("nonExistentTool");
        assertNull(nonExistent, "Should return null for non-existent tool");
    }

    @Test
    void testIsReadyBeforeInitialization() {
        assertFalse(serverProvider.isReady(), "Server should not be ready before initialization");
    }

    @Test
    void testDoubleInitialization() {
        serverProvider.initialize();
        serverProvider.initialize(); // Should not throw
        
        assertTrue(serverProvider.isReady(), "Server should still be ready");
        assertEquals(9, serverProvider.getToolCount(), "Tool count should remain the same");
    }

    @Test
    void testInvokeToolWithValidTool() {
        serverProvider.initialize();
        
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("projectPath", "/test/project");
        
        var result = serverProvider.invokeTool("analyzeJakartaReadiness", arguments);
        assertNotNull(result, "Result should not be null");
        assertDoesNotThrow(() -> result.get(), "Should not throw exception");
    }

    @Test
    void testInvokeToolWithUnknownTool() {
        serverProvider.initialize();
        
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("projectPath", "/test/project");
        
        var result = serverProvider.invokeTool("unknownTool", arguments);
        assertNotNull(result, "Result should not be null");
        
        assertThrows(Exception.class, () -> result.get(), 
                "Should throw exception for unknown tool");
    }

    @Test
    void testInvokeToolWithMissingRequiredArgument() {
        serverProvider.initialize();
        
        Map<String, Object> arguments = new HashMap<>();
        // Missing required "projectPath" argument
        
        var result = serverProvider.invokeTool("analyzeJakartaReadiness", arguments);
        assertNotNull(result, "Result should not be null");
        
        assertThrows(Exception.class, () -> result.get(), 
                "Should throw exception for missing required argument");
    }

    @Test
    void testServerConfigurationJson() {
        serverProvider.initialize();
        
        String json = serverProvider.getServerConfigurationJson();
        assertNotNull(json, "JSON configuration should not be null");
        assertFalse(json.isEmpty(), "JSON configuration should not be empty");
        assertTrue(json.contains("jakarta-migration-mcp"), 
                "JSON should contain server name");
        assertTrue(json.contains("analyzeJakartaReadiness"), 
                "JSON should contain tool names");
    }

    @Test
    void testDispose() {
        serverProvider.initialize();
        assertTrue(serverProvider.isReady(), "Should be ready before dispose");
        
        serverProvider.dispose();
        assertFalse(serverProvider.isReady(), "Should not be ready after dispose");
        assertEquals(0, serverProvider.getToolCount(), "Tool count should be 0 after dispose");
    }

    @Test
    void testToString() {
        serverProvider.initialize();
        
        String str = serverProvider.toString();
        assertNotNull(str, "toString should not return null");
        assertTrue(str.contains("jakarta-migration-mcp"), "Should contain server ID");
        assertTrue(str.contains("9"), "Should contain tool count");
    }

    @Test
    void testAllRequiredToolsAreRegistered() {
        serverProvider.initialize();
        
        String[] requiredTools = {
                "analyzeJakartaReadiness",
                "analyzeMigrationImpact",
                "detectBlockers",
                "recommendVersions",
                "applyOpenRewriteRefactoring",
                "scanBinaryDependency",
                "updateDependency",
                "generateMigrationPlan",
                "validateMigration"
        };
        
        for (String toolName : requiredTools) {
            assertNotNull(serverProvider.getTool(toolName), 
                    "Required tool should be registered: " + toolName);
        }
    }
}
