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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for MCP Server using Streamable HTTP transport.
 * 
 * Streamable HTTP is simpler than SSE:
 * - Single POST endpoint (no SSE complexity)
 * - Standard HTTP requests/responses
 * - No keepalive messages needed
 * - Better proxy compatibility
 * 
 * Strategy: "The In-Process Spring Boot Test"
 * - Uses @SpringBootTest to start the server in-process
 * - Tests Streamable HTTP endpoint via MockMvc
 * - Verifies JSON-RPC response contracts
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    classes = adrianmikula.projectname.ProjectNameApplication.class,
    properties = {
        "jakarta.migration.stripe.enabled=false",
        "jakarta.migration.apify.enabled=false",
        "jakarta.migration.storage.file.enabled=false",
        "spring.ai.mcp.server.transport=streamable-http"
    }
)
@ActiveProfiles("mcp-streamable-http")
@AutoConfigureMockMvc
class McpServerStreamableHttpIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    
    private ObjectMapper objectMapper = new ObjectMapper();
    private int requestId = 1;

    @Test
    void testStreamableHttpEndpointExists() throws Exception {
        // Test that Streamable HTTP endpoint is accessible
        Map<String, Object> request = Map.of(
            "jsonrpc", "2.0",
            "id", requestId++,
            "method", "ping",
            "params", Map.of()
        );
        
        mockMvc.perform(post("/mcp/streamable-http")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void testServerInitialization() throws Exception {
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
        
        String response = mockMvc.perform(post("/mcp/streamable-http")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
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
        assertThat(result.has("protocolVersion")).isTrue();
        assertThat(result.get("protocolVersion").asText()).isEqualTo("2024-11-05");
    }

    @Test
    void testListTools() throws Exception {
        Map<String, Object> request = Map.of(
            "jsonrpc", "2.0",
            "id", requestId++,
            "method", "tools/list",
            "params", Map.of()
        );
        
        String response = mockMvc.perform(post("/mcp/streamable-http")
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
        
        String response = mockMvc.perform(post("/mcp/streamable-http")
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
            
            String response = mockMvc.perform(post("/mcp/streamable-http")
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
            
            String response = mockMvc.perform(post("/mcp/streamable-http")
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
            
            String response = mockMvc.perform(post("/mcp/streamable-http")
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
            
            String response = mockMvc.perform(post("/mcp/streamable-http")
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
        
        String response = mockMvc.perform(post("/mcp/streamable-http")
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
        
        String response = mockMvc.perform(post("/mcp/streamable-http")
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

    @Test
    void testAuthenticationHeader() throws Exception {
        // Test that Streamable HTTP accepts Authorization header
        Map<String, Object> request = Map.of(
            "jsonrpc", "2.0",
            "id", requestId++,
            "method", "tools/list",
            "params", Map.of()
        );
        
        String response = mockMvc.perform(post("/mcp/streamable-http")
                .header("Authorization", "Bearer test-token-12345")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        
        // Should still work with auth header
        JsonNode jsonResponse = objectMapper.readTree(response);
        assertThat(jsonResponse.has("result")).isTrue();
    }

    @Test
    void testToolFiltering() throws Exception {
        // Test tool filtering via query parameter
        Map<String, Object> request = Map.of(
            "jsonrpc", "2.0",
            "id", requestId++,
            "method", "tools/list",
            "params", Map.of()
        );
        
        String response = mockMvc.perform(post("/mcp/streamable-http?tools=check_env,analyzeJakartaReadiness")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        
        JsonNode jsonResponse = objectMapper.readTree(response);
        JsonNode tools = jsonResponse.get("result").get("tools");
        
        // Should only return filtered tools
        assertThat(tools.isArray()).isTrue();
        for (JsonNode tool : tools) {
            String name = tool.get("name").asText();
            assertThat(name).isIn("check_env", "analyzeJakartaReadiness");
        }
    }

    @Test
    void testSessionParameter() throws Exception {
        // Test session parameter (optional, for session management)
        Map<String, Object> request = Map.of(
            "jsonrpc", "2.0",
            "id", requestId++,
            "method", "tools/list",
            "params", Map.of()
        );
        
        String response = mockMvc.perform(post("/mcp/streamable-http?session=test-session-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        
        // Should work with session parameter
        JsonNode jsonResponse = objectMapper.readTree(response);
        assertThat(jsonResponse.has("result")).isTrue();
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

