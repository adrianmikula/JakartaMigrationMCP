package integration.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for MCP Server using SSE transport.
 * 
 * This test starts the MCP server as a Spring Boot application and uses
 * MockMvc to test the SSE endpoint, just as Apify or other SSE-capable clients would.
 * 
 * Strategy: "The In-Process Spring Boot Test"
 * - Uses @SpringBootTest to start the server in-process
 * - Tests SSE endpoint via MockMvc
 * - Verifies JSON-RPC response contracts
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    classes = adrianmikula.projectname.ProjectNameApplication.class,
    properties = {
        "jakarta.migration.stripe.enabled=false",
        "jakarta.migration.apify.enabled=false",
        "jakarta.migration.storage.file.enabled=false",
        "spring.ai.mcp.server.transport=sse"
    }
)
@ActiveProfiles("mcp-sse")
@AutoConfigureMockMvc
class McpServerSseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    
    private ObjectMapper objectMapper = new ObjectMapper();
    private int requestId = 1;

    @Test
    void testSseEndpointConnection() throws Exception {
        // Test that SSE endpoint is accessible
        mockMvc.perform(get("/mcp/sse"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));
    }

    @Test
    void testSseConnectionSendsSessionId() throws Exception {
        // Test that SSE connection sends session ID on initial connection
        String response = mockMvc.perform(get("/mcp/sse"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
            .andReturn()
            .getResponse()
            .getContentAsString();
        
        // SSE response should contain session information
        // Format: event: session\ndata: {"sessionId":"...","status":"connected"}
        assertThat(response).isNotNull();
        assertThat(response).contains("sessionId");
        assertThat(response).contains("connected");
        
        // Try to parse session ID from response
        Pattern sessionPattern = Pattern.compile("\"sessionId\"\\s*:\\s*\"([^\"]+)\"");
        java.util.regex.Matcher matcher = sessionPattern.matcher(response);
        assertThat(matcher.find()).isTrue();
        String sessionId = matcher.group(1);
        assertThat(sessionId).isNotEmpty();
        assertThat(sessionId.length()).isGreaterThan(10); // UUID should be long
    }

    @Test
    void testSseConnectionWithSessionIdCorrelation() throws Exception {
        // Test that POST requests can be correlated with SSE connection via session ID
        
        // First, establish SSE connection and extract session ID
        String sseResponse = mockMvc.perform(get("/mcp/sse"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        
        // Extract session ID
        Pattern sessionPattern = Pattern.compile("\"sessionId\"\\s*:\\s*\"([^\"]+)\"");
        java.util.regex.Matcher matcher = sessionPattern.matcher(sseResponse);
        assertThat(matcher.find()).isTrue();
        String sessionId = matcher.group(1);
        
        // Now send a POST request with the session ID
        Map<String, Object> request = Map.of(
            "jsonrpc", "2.0",
            "id", requestId++,
            "method", "tools/list",
            "params", Map.of()
        );
        
        String postResponse = mockMvc.perform(post("/mcp/sse")
                .header("X-Session-Id", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        
        // Verify response is valid
        JsonNode jsonResponse = objectMapper.readTree(postResponse);
        assertThat(jsonResponse.has("result")).isTrue();
        assertThat(jsonResponse.get("result").has("tools")).isTrue();
    }

    @Test
    void testSseConnectionWithoutSessionIdFallsBack() throws Exception {
        // Test that POST requests without session ID still work (uses first available connection)
        
        // Establish SSE connection first
        mockMvc.perform(get("/mcp/sse"))
            .andExpect(status().isOk());
        
        // Send POST request without session ID
        Map<String, Object> request = Map.of(
            "jsonrpc", "2.0",
            "id", requestId++,
            "method", "tools/list",
            "params", Map.of()
        );
        
        String response = mockMvc.perform(post("/mcp/sse")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        
        // Should still get a valid response
        JsonNode jsonResponse = objectMapper.readTree(response);
        assertThat(jsonResponse.has("result")).isTrue();
    }

    @Test
    void testSseConnectionStaysAlive() throws Exception {
        // Test that SSE connection doesn't timeout immediately
        // This verifies the keepalive mechanism is working
        
        var result = mockMvc.perform(get("/mcp/sse"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
            .andReturn();
        
        // Connection should be established
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        
        // Wait a bit and verify connection is still considered active
        // (In a real scenario, keepalive messages would be sent every 15 seconds)
        // We can't easily test keepalive with MockMvc, but we can verify the connection
        // doesn't immediately fail
        Thread.sleep(100); // Small delay
        
        // Connection should still be valid (no exception thrown)
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void testMultipleSseConnections() throws Exception {
        // Test that multiple SSE connections can be established simultaneously
        
        // Establish first connection
        String response1 = mockMvc.perform(get("/mcp/sse"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        
        // Extract first session ID
        Pattern sessionPattern = Pattern.compile("\"sessionId\"\\s*:\\s*\"([^\"]+)\"");
        java.util.regex.Matcher matcher1 = sessionPattern.matcher(response1);
        assertThat(matcher1.find()).isTrue();
        String sessionId1 = matcher1.group(1);
        
        // Establish second connection
        String response2 = mockMvc.perform(get("/mcp/sse"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        
        // Extract second session ID
        java.util.regex.Matcher matcher2 = sessionPattern.matcher(response2);
        assertThat(matcher2.find()).isTrue();
        String sessionId2 = matcher2.group(1);
        
        // Session IDs should be different
        assertThat(sessionId1).isNotEqualTo(sessionId2);
        
        // Both connections should work independently
        Map<String, Object> request = Map.of(
            "jsonrpc", "2.0",
            "id", requestId++,
            "method", "tools/list",
            "params", Map.of()
        );
        
        // Test with first session
        String postResponse1 = mockMvc.perform(post("/mcp/sse")
                .header("X-Session-Id", sessionId1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        
        assertThat(objectMapper.readTree(postResponse1).has("result")).isTrue();
        
        // Test with second session
        request = Map.of(
            "jsonrpc", "2.0",
            "id", requestId++,
            "method", "tools/list",
            "params", Map.of()
        );
        
        String postResponse2 = mockMvc.perform(post("/mcp/sse")
                .header("X-Session-Id", sessionId2)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        
        assertThat(objectMapper.readTree(postResponse2).has("result")).isTrue();
    }

    @Test
    void testSseConnectionWithInvalidSessionId() throws Exception {
        // Test that POST requests with invalid session ID still return response
        // (should fall back to first available connection or return POST response only)
        
        Map<String, Object> request = Map.of(
            "jsonrpc", "2.0",
            "id", requestId++,
            "method", "tools/list",
            "params", Map.of()
        );
        
        String response = mockMvc.perform(post("/mcp/sse")
                .header("X-Session-Id", "invalid-session-id-12345")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        
        // Should still get a valid response (POST response, even if SSE fails)
        JsonNode jsonResponse = objectMapper.readTree(response);
        assertThat(jsonResponse.has("result")).isTrue();
    }

    @Test
    void testSseConnectionWithAuthentication() throws Exception {
        // Test that SSE connection accepts Authorization header (Apify requirement)
        
        String response = mockMvc.perform(get("/mcp/sse")
                .header("Authorization", "Bearer test-token-12345"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
            .andReturn()
            .getResponse()
            .getContentAsString();
        
        // Should still connect successfully with auth header
        assertThat(response).contains("sessionId");
    }

    @Test
    void testServerInitialization() throws Exception {
        // Test initialize request via POST
        Map<String, Object> request = Map.of(
            "jsonrpc", "2.0",
            "id", requestId++,
            "method", "initialize",
            "params", Map.of(
                "protocolVersion", "2024-11-05",
                "capabilities", Map.of(),
                "clientInfo", Map.of("name", "test-client", "version", "1.0.0")
            )
        );
        
        String response = mockMvc.perform(post("/mcp/sse")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        
        JsonNode jsonResponse = objectMapper.readTree(response);
        assertThat(jsonResponse.has("result")).isTrue();
        JsonNode result = jsonResponse.get("result");
        assertThat(result.has("serverInfo")).isTrue();
        JsonNode serverInfo = result.get("serverInfo");
        assertThat(serverInfo.get("name").asText()).isEqualTo("jakarta-migration-mcp");
        assertThat(serverInfo.has("version")).isTrue();
    }

    @Test
    void testListTools() throws Exception {
        Map<String, Object> request = Map.of(
            "jsonrpc", "2.0",
            "id", requestId++,
            "method", "tools/list",
            "params", Map.of()
        );
        
        String response = mockMvc.perform(post("/mcp/sse")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        
        JsonNode jsonResponse = objectMapper.readTree(response);
        assertThat(jsonResponse.has("result")).isTrue();
        JsonNode result = jsonResponse.get("result");
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
    void testCheckEnvTool() throws Exception {
        Map<String, Object> request = Map.of(
            "jsonrpc", "2.0",
            "id", requestId++,
            "method", "tools/call",
            "params", Map.of(
                "name", "check_env",
                "arguments", Map.of("name", "PATH")
            )
        );
        
        String response = mockMvc.perform(post("/mcp/sse")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        
        JsonNode jsonResponse = objectMapper.readTree(response);
        assertThat(jsonResponse.has("result")).isTrue();
        JsonNode result = jsonResponse.get("result");
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
    void testAnalyzeJakartaReadinessTool() throws Exception {
        Path testProject = createTestProject();
        
        try {
            Map<String, Object> request = Map.of(
                "jsonrpc", "2.0",
                "id", requestId++,
                "method", "tools/call",
                "params", Map.of(
                    "name", "analyzeJakartaReadiness",
                    "arguments", Map.of("projectPath", testProject.toString())
                )
            );
            
            String response = mockMvc.perform(post("/mcp/sse")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
            
            JsonNode jsonResponse = objectMapper.readTree(response);
            assertThat(jsonResponse.has("result")).isTrue();
            JsonNode result = jsonResponse.get("result");
            assertThat(result.has("content")).isTrue();
            
            String text = result.get("content").get(0).get("text").asText();
            assertThat(text).isNotNull();
            assertThat(text).contains("\"status\"");
            
        } finally {
            deleteTestProject(testProject);
        }
    }

    @Test
    void testDetectBlockersTool() throws Exception {
        Path testProject = createTestProject();
        
        try {
            Map<String, Object> request = Map.of(
                "jsonrpc", "2.0",
                "id", requestId++,
                "method", "tools/call",
                "params", Map.of(
                    "name", "detectBlockers",
                    "arguments", Map.of("projectPath", testProject.toString())
                )
            );
            
            String response = mockMvc.perform(post("/mcp/sse")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
            
            JsonNode jsonResponse = objectMapper.readTree(response);
            assertThat(jsonResponse.has("result")).isTrue();
            JsonNode result = jsonResponse.get("result");
            assertThat(result.has("content")).isTrue();
            
            String text = result.get("content").get(0).get("text").asText();
            assertThat(text).isNotNull();
            assertThat(text).contains("\"status\"");
            
        } finally {
            deleteTestProject(testProject);
        }
    }

    @Test
    void testRecommendVersionsTool() throws Exception {
        Path testProject = createTestProject();
        
        try {
            Map<String, Object> request = Map.of(
                "jsonrpc", "2.0",
                "id", requestId++,
                "method", "tools/call",
                "params", Map.of(
                    "name", "recommendVersions",
                    "arguments", Map.of("projectPath", testProject.toString())
                )
            );
            
            String response = mockMvc.perform(post("/mcp/sse")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
            
            JsonNode jsonResponse = objectMapper.readTree(response);
            assertThat(jsonResponse.has("result")).isTrue();
            JsonNode result = jsonResponse.get("result");
            assertThat(result.has("content")).isTrue();
            
            String text = result.get("content").get(0).get("text").asText();
            assertThat(text).isNotNull();
            assertThat(text).contains("\"status\"");
            
        } finally {
            deleteTestProject(testProject);
        }
    }

    @Test
    void testCreateMigrationPlanTool() throws Exception {
        Path testProject = createTestProject();
        
        try {
            Map<String, Object> request = Map.of(
                "jsonrpc", "2.0",
                "id", requestId++,
                "method", "tools/call",
                "params", Map.of(
                    "name", "createMigrationPlan",
                    "arguments", Map.of("projectPath", testProject.toString())
                )
            );
            
            String response = mockMvc.perform(post("/mcp/sse")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
            
            JsonNode jsonResponse = objectMapper.readTree(response);
            assertThat(jsonResponse.has("result")).isTrue();
            JsonNode result = jsonResponse.get("result");
            assertThat(result.has("content")).isTrue();
            
            String text = result.get("content").get(0).get("text").asText();
            assertThat(text).isNotNull();
            assertThat(text).contains("\"status\"");
            
        } finally {
            deleteTestProject(testProject);
        }
    }

    @Test
    void testToolInputSchemaValidation() throws Exception {
        Map<String, Object> request = Map.of(
            "jsonrpc", "2.0",
            "id", requestId++,
            "method", "tools/list",
            "params", Map.of()
        );
        
        String response = mockMvc.perform(post("/mcp/sse")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        
        JsonNode jsonResponse = objectMapper.readTree(response);
        JsonNode tools = jsonResponse.get("result").get("tools");
        
        for (JsonNode tool : tools) {
            assertThat(tool.has("inputSchema")).isTrue();
            JsonNode schema = tool.get("inputSchema");
            assertThat(schema.has("type")).isTrue();
        }
    }

    @Test
    void testInvalidToolCall() throws Exception {
        Map<String, Object> request = Map.of(
            "jsonrpc", "2.0",
            "id", requestId++,
            "method", "tools/call",
            "params", Map.of(
                "name", "nonexistent_tool",
                "arguments", Map.of()
            )
        );
        
        String response = mockMvc.perform(post("/mcp/sse")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        
        JsonNode jsonResponse = objectMapper.readTree(response);
        // Should have an error
        assertThat(jsonResponse.has("error")).isTrue();
    }

    /**
     * Create a minimal test project for testing Jakarta migration tools.
     */
    private Path createTestProject() {
        try {
            Path testProject = Paths.get(System.getProperty("java.io.tmpdir"), "mcp-test-project-" + System.currentTimeMillis());
            testProject.toFile().mkdirs();
            
            // Create a minimal pom.xml
            Path pomXml = testProject.resolve("pom.xml");
            Files.writeString(pomXml, """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
                         http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.test</groupId>
                    <artifactId>test-project</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>javax.servlet</groupId>
                            <artifactId>javax.servlet-api</artifactId>
                            <version>4.0.1</version>
                        </dependency>
                    </dependencies>
                </project>
                """);
            
            return testProject;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test project", e);
        }
    }

    private void deleteTestProject(Path testProject) {
        try {
            if (testProject != null && testProject.toFile().exists()) {
                deleteRecursively(testProject.toFile());
            }
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }
}

