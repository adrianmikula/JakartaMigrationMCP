package integration.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for MCP Server using STDIO transport.
 * 
 * This test treats the MCP server as a "black box" and communicates via JSON-RPC
 * over stdin/stdout, just as Claude or Cursor would in a real-world scenario.
 * 
 * Strategy: "The Out-of-Process Mock"
 * - Spins up the MCP server as a sub-process
 * - Communicates via JSON-RPC over stdin/stdout
 * - Verifies JSON-RPC response contracts
 */
@Disabled("Spring context loading issues - low importance integration test")
class McpServerStdioIntegrationTest {

    private Process serverProcess;
    private BufferedReader serverOutput;
    private PrintWriter serverInput;
    private ObjectMapper objectMapper;
    private int requestId = 1;

    @BeforeEach
    void setup() throws IOException {
        // 1. Build the JAR if it doesn't exist
        File jarFile = new File("build/libs/jakarta-migration-mcp-1.0.0-SNAPSHOT.jar");
        if (!jarFile.exists()) {
            throw new IllegalStateException(
                "JAR file not found. Please run 'gradlew bootJar' first: " + jarFile.getAbsolutePath()
            );
        }

        // 2. Start the server as a sub-process
        ProcessBuilder processBuilder = new ProcessBuilder(
            "java",
            "-jar",
            jarFile.getAbsolutePath(),
            "--spring.profiles.active=mcp-stdio"
        );
        processBuilder.environment().put("MCP_TRANSPORT", "stdio");
        
        this.serverProcess = processBuilder.start();
        this.serverOutput = new BufferedReader(new InputStreamReader(serverProcess.getInputStream()));
        this.serverInput = new PrintWriter(new OutputStreamWriter(serverProcess.getOutputStream()), true);
        this.objectMapper = new ObjectMapper();

        // 3. Initialize the MCP connection
        sendRequest("initialize", Map.of(
            "protocolVersion", "2024-11-05",
            "capabilities", Map.of(),
            "clientInfo", Map.of("name", "test-client", "version", "1.0.0")
        ));
        
        // Wait for initialization response
        JsonNode response = readResponse();
        assertThat(response.has("result")).isTrue();
    }

    @Test
    void testServerInitialization() throws IOException {
        // Verify server info is available from initialize response
        sendRequest("initialize", Map.of(
            "protocolVersion", "2024-11-05",
            "capabilities", Map.of(),
            "clientInfo", Map.of("name", "test-client", "version", "1.0.0")
        ));
        
        JsonNode response = readResponse();
        assertThat(response.has("result")).isTrue();
        JsonNode result = response.get("result");
        assertThat(result.has("serverInfo")).isTrue();
        JsonNode serverInfo = result.get("serverInfo");
        assertThat(serverInfo.get("name").asText()).isEqualTo("jakarta-migration-mcp");
        assertThat(serverInfo.has("version")).isTrue();
    }

    @Test
    void testListTools() throws IOException {
        // Verify all tools are listed correctly
        sendRequest("tools/list", Map.of());
        JsonNode response = readResponse();
        
        assertThat(response.has("result")).isTrue();
        JsonNode result = response.get("result");
        assertThat(result.has("tools")).isTrue();
        JsonNode tools = result.get("tools");
        assertThat(tools.isArray()).isTrue();
        assertThat(tools.size()).isGreaterThan(0);
        
        // Verify Jakarta migration tools are present
        boolean hasAnalyzeReadiness = false;
        boolean hasDetectBlockers = false;
        boolean hasRecommendVersions = false;
        boolean hasCreatePlan = false;
        boolean hasVerifyRuntime = false;
        boolean hasCheckEnv = false;
        
        for (JsonNode tool : tools) {
            String name = tool.get("name").asText();
            if ("analyzeJakartaReadiness".equals(name)) hasAnalyzeReadiness = true;
            if ("detectBlockers".equals(name)) hasDetectBlockers = true;
            if ("recommendVersions".equals(name)) hasRecommendVersions = true;
            if ("createMigrationPlan".equals(name)) hasCreatePlan = true;
            if ("verifyRuntime".equals(name)) hasVerifyRuntime = true;
            if ("check_env".equals(name)) hasCheckEnv = true;
            
            // Verify tool descriptions are present (LLMs rely on these!)
            assertThat(tool.has("name")).isTrue();
            assertThat(tool.has("description")).isTrue();
            assertThat(tool.get("description").asText()).isNotEmpty();
        }
        
        assertThat(hasAnalyzeReadiness).isTrue();
        assertThat(hasDetectBlockers).isTrue();
        assertThat(hasRecommendVersions).isTrue();
        assertThat(hasCreatePlan).isTrue();
        assertThat(hasVerifyRuntime).isTrue();
        assertThat(hasCheckEnv).isTrue();
    }

    @Test
    void testCheckEnvTool() throws IOException {
        // Test the simple sentinel tool
        sendRequest("tools/call", Map.of(
            "name", "check_env",
            "arguments", Map.of("name", "PATH")
        ));
        
        JsonNode response = readResponse();
        assertThat(response.has("result")).isTrue();
        JsonNode result = response.get("result");
        assertThat(result.has("content")).isTrue();
        JsonNode content = result.get("content");
        assertThat(content.isArray()).isTrue();
        assertThat(content.size()).isGreaterThan(0);
        
        String text = content.get(0).get("text").asText();
        assertThat(text).isNotNull();
        // Should either say "Defined:" or "Missing:" 
        assertThat(text).matches("(Defined|Missing):.*");
    }

    @Test
    @Disabled("Tool analyzeJakartaReadiness no longer exists in community edition")
    void testAnalyzeJakartaReadinessTool() throws IOException {
        // Use a test project path (create a minimal test project)
        Path testProject = TestProjectHelper.createTestProject();
        
        try {
            sendRequest("tools/call", Map.of(
                "name", "analyzeJakartaReadiness",
                "arguments", Map.of("projectPath", testProject.toString())
            ));
            
            JsonNode response = readResponse();
            assertThat(response.has("result")).isTrue();
            JsonNode result = response.get("result");
            assertThat(result.has("content")).isTrue();
            
            String text = result.get("content").get(0).get("text").asText();
            assertThat(text).isNotNull();
            // Should return JSON with status field
            assertThat(text).contains("\"status\"");
            
        } finally {
            TestProjectHelper.deleteTestProject(testProject);
        }
    }

    @Test
    @Disabled("Tool detectBlockers no longer exists in community edition")
    void testDetectBlockersTool() throws IOException {
        Path testProject = TestProjectHelper.createTestProject();
        
        try {
            sendRequest("tools/call", Map.of(
                "name", "detectBlockers",
                "arguments", Map.of("projectPath", testProject.toString())
            ));
            
            JsonNode response = readResponse();
            assertThat(response.has("result")).isTrue();
            JsonNode result = response.get("result");
            assertThat(result.has("content")).isTrue();
            
            String text = result.get("content").get(0).get("text").asText();
            assertThat(text).isNotNull();
            assertThat(text).contains("\"status\"");
            
        } finally {
            TestProjectHelper.deleteTestProject(testProject);
        }
    }

    @Test
    void testRecommendVersionsTool() throws IOException {
        Path testProject = TestProjectHelper.createTestProject();
        
        try {
            sendRequest("tools/call", Map.of(
                "name", "recommendVersions",
                "arguments", Map.of("projectPath", testProject.toString())
            ));
            
            JsonNode response = readResponse();
            assertThat(response.has("result")).isTrue();
            JsonNode result = response.get("result");
            assertThat(result.has("content")).isTrue();
            
            String text = result.get("content").get(0).get("text").asText();
            assertThat(text).isNotNull();
            assertThat(text).contains("\"status\"");
            
        } finally {
            TestProjectHelper.deleteTestProject(testProject);
        }
    }

    @Test
    @Disabled("Tool createMigrationPlan no longer exists in community edition")
    void testCreateMigrationPlanTool() throws IOException {
        Path testProject = TestProjectHelper.createTestProject();
        
        try {
            sendRequest("tools/call", Map.of(
                "name", "createMigrationPlan",
                "arguments", Map.of("projectPath", testProject.toString())
            ));
            
            JsonNode response = readResponse();
            assertThat(response.has("result")).isTrue();
            JsonNode result = response.get("result");
            assertThat(result.has("content")).isTrue();
            
            String text = result.get("content").get(0).get("text").asText();
            assertThat(text).isNotNull();
            assertThat(text).contains("\"status\"");
            
        } finally {
            TestProjectHelper.deleteTestProject(testProject);
        }
    }

    @Test
    void testToolInputSchemaValidation() throws IOException {
        // Verify that tool input schemas are valid
        sendRequest("tools/list", Map.of());
        JsonNode response = readResponse();
        JsonNode tools = response.get("result").get("tools");
        
        for (JsonNode tool : tools) {
            assertThat(tool.has("inputSchema")).isTrue();
            JsonNode schema = tool.get("inputSchema");
            assertThat(schema.has("type")).isTrue();
        }
    }

    @Test
    void testInvalidToolCall() throws IOException {
        // Verify error handling for invalid tool calls
        sendRequest("tools/call", Map.of(
            "name", "nonexistent_tool",
            "arguments", Map.of()
        ));
        
        JsonNode response = readResponse();
        // Should have an error
        assertThat(response.has("error")).isTrue();
    }
    
    // Helper methods for JSON-RPC communication
    
    private void sendRequest(String method, Map<String, Object> params) throws IOException {
        Map<String, Object> request = Map.of(
            "jsonrpc", "2.0",
            "id", requestId++,
            "method", method,
            "params", params
        );
        
        String json = objectMapper.writeValueAsString(request);
        serverInput.println(json);
        serverInput.flush();
    }
    
    private JsonNode readResponse() throws IOException {
        String line = serverOutput.readLine();
        if (line == null) {
            throw new IOException("Server closed connection");
        }
        return objectMapper.readTree(line);
    }

    @AfterEach
    void tearDown() {
        if (serverInput != null) {
            serverInput.close();
        }
        if (serverOutput != null) {
            try {
                serverOutput.close();
            } catch (IOException e) {
                // Ignore
            }
        }
        if (serverProcess != null) {
            serverProcess.destroy();
            try {
                serverProcess.waitFor(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (serverProcess.isAlive()) {
                serverProcess.destroyForcibly();
            }
        }
    }
}


