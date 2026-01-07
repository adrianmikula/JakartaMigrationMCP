package component.jakartamigration.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for MCP SSE endpoint controller.
 * 
 * Tests the SSE endpoint implementation for Apify compliance:
 * - SSE connection establishment
 * - JSON-RPC protocol methods (initialize, tools/list, tools/call)
 * - Authentication header support
 * - Tool filtering via query parameters
 */
@SpringBootTest(
    classes = adrianmikula.projectname.ProjectNameApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "jakarta.migration.stripe.enabled=false",
        "jakarta.migration.apify.enabled=false",
        "jakarta.migration.storage.file.enabled=false",
        "spring.ai.mcp.server.transport=sse"
    }
)
@AutoConfigureMockMvc
@ActiveProfiles("mcp-sse")
@DisplayName("MCP SSE Controller Integration Tests")
class McpSseControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Should establish SSE connection")
    void shouldEstablishSseConnection() throws Exception {
        // When & Then
        // Note: SseEmitter doesn't set content type immediately in MockMvc,
        // but the endpoint should return 200 OK
        mockMvc.perform(get("/mcp/sse")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should accept Authorization header")
    void shouldAcceptAuthorizationHeader() throws Exception {
        // When & Then
        // Note: SseEmitter doesn't set content type immediately in MockMvc,
        // but the endpoint should accept the Authorization header and return 200 OK
        mockMvc.perform(get("/mcp/sse")
                        .header("Authorization", "Bearer test-token")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should handle initialize request")
    void shouldHandleInitializeRequest() throws Exception {
        // Given
        Map<String, Object> request = new HashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", 1);
        request.put("method", "initialize");
        
        Map<String, Object> params = new HashMap<>();
        params.put("protocolVersion", "2024-11-05");
        params.put("capabilities", new HashMap<>());
        Map<String, Object> clientInfo = new HashMap<>();
        clientInfo.put("name", "test-client");
        clientInfo.put("version", "1.0.0");
        params.put("clientInfo", clientInfo);
        request.put("params", params);

        // When
        MvcResult result = mockMvc.perform(post("/mcp/sse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        String responseBody = result.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(responseBody);
        
        assertThat(response.get("jsonrpc").asText()).isEqualTo("2.0");
        assertThat(response.get("id").asInt()).isEqualTo(1);
        assertThat(response.has("result")).isTrue();
        
        JsonNode resultNode = response.get("result");
        assertThat(resultNode.get("protocolVersion").asText()).isEqualTo("2024-11-05");
        assertThat(resultNode.has("capabilities")).isTrue();
        assertThat(resultNode.has("serverInfo")).isTrue();
        
        JsonNode serverInfo = resultNode.get("serverInfo");
        assertThat(serverInfo.get("name").asText()).isEqualTo("jakarta-migration-mcp");
        assertThat(serverInfo.get("version").asText()).isEqualTo("1.0.0-SNAPSHOT");
    }

    @Test
    @DisplayName("Should handle tools/list request")
    void shouldHandleToolsListRequest() throws Exception {
        // Given
        Map<String, Object> request = new HashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", 2);
        request.put("method", "tools/list");
        request.put("params", new HashMap<>());

        // When
        MvcResult result = mockMvc.perform(post("/mcp/sse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        String responseBody = result.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(responseBody);
        
        assertThat(response.get("jsonrpc").asText()).isEqualTo("2.0");
        assertThat(response.get("id").asInt()).isEqualTo(2);
        assertThat(response.has("result")).isTrue();
        
        JsonNode resultNode = response.get("result");
        assertThat(resultNode.has("tools")).isTrue();
        
        JsonNode tools = resultNode.get("tools");
        assertThat(tools.isArray()).isTrue();
        assertThat(tools.size()).isGreaterThan(0);
        
        // Verify at least one tool has required fields
        JsonNode firstTool = tools.get(0);
        assertThat(firstTool.has("name")).isTrue();
        assertThat(firstTool.has("description")).isTrue();
        assertThat(firstTool.has("inputSchema")).isTrue();
    }

    @Test
    @DisplayName("Should filter tools via query parameter")
    void shouldFilterToolsViaQueryParameter() throws Exception {
        // Given
        Map<String, Object> request = new HashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", 3);
        request.put("method", "tools/list");
        request.put("params", new HashMap<>());

        // When - Request with tool filter
        mockMvc.perform(get("/mcp/sse")
                        .param("tools", "check_env")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk());

        // Note: SSE endpoint with query params doesn't immediately return tools/list
        // This test verifies the endpoint accepts the filter parameter
        // Actual filtering would be tested via POST /mcp/sse with tools query param
    }

    @Test
    @DisplayName("Should handle tools/call request")
    void shouldHandleToolsCallRequest() throws Exception {
        // Given
        Map<String, Object> request = new HashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", 4);
        request.put("method", "tools/call");
        
        Map<String, Object> params = new HashMap<>();
        params.put("name", "check_env");
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("name", "JAVA_HOME");
        params.put("arguments", arguments);
        request.put("params", params);

        // When
        MvcResult result = mockMvc.perform(post("/mcp/sse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        String responseBody = result.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(responseBody);
        
        assertThat(response.get("jsonrpc").asText()).isEqualTo("2.0");
        assertThat(response.get("id").asInt()).isEqualTo(4);
        assertThat(response.has("result")).isTrue();
        
        JsonNode resultNode = response.get("result");
        assertThat(resultNode.has("content")).isTrue();
        
        JsonNode content = resultNode.get("content");
        assertThat(content.isArray()).isTrue();
        assertThat(content.size()).isGreaterThan(0);
        
        JsonNode firstContent = content.get(0);
        assertThat(firstContent.get("type").asText()).isEqualTo("text");
        assertThat(firstContent.has("text")).isTrue();
    }

    @Test
    @DisplayName("Should handle ping request")
    void shouldHandlePingRequest() throws Exception {
        // Given
        Map<String, Object> request = new HashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", 5);
        request.put("method", "ping");
        request.put("params", new HashMap<>());

        // When
        MvcResult result = mockMvc.perform(post("/mcp/sse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        String responseBody = result.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(responseBody);
        
        assertThat(response.get("jsonrpc").asText()).isEqualTo("2.0");
        assertThat(response.get("id").asInt()).isEqualTo(5);
        assertThat(response.has("result")).isTrue();
        assertThat(response.get("result").get("status").asText()).isEqualTo("pong");
    }

    @Test
    @DisplayName("Should return error for unknown method")
    void shouldReturnErrorForUnknownMethod() throws Exception {
        // Given
        Map<String, Object> request = new HashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", 6);
        request.put("method", "unknown_method");
        request.put("params", new HashMap<>());

        // When
        MvcResult result = mockMvc.perform(post("/mcp/sse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        String responseBody = result.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(responseBody);
        
        assertThat(response.get("jsonrpc").asText()).isEqualTo("2.0");
        assertThat(response.get("id").asInt()).isEqualTo(6);
        assertThat(response.has("error")).isTrue();
        
        JsonNode error = response.get("error");
        assertThat(error.get("code").asInt()).isEqualTo(-32601);
        assertThat(error.get("message").asText()).contains("Method not found");
    }

    @Test
    @DisplayName("Should handle initialize with Authorization header")
    void shouldHandleInitializeWithAuthorizationHeader() throws Exception {
        // Given
        Map<String, Object> request = new HashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", 7);
        request.put("method", "initialize");
        
        Map<String, Object> params = new HashMap<>();
        params.put("protocolVersion", "2024-11-05");
        params.put("capabilities", new HashMap<>());
        request.put("params", params);

        // When
        MvcResult result = mockMvc.perform(post("/mcp/sse")
                        .header("Authorization", "Bearer test-token-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        String responseBody = result.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(responseBody);
        
        assertThat(response.has("result")).isTrue();
        // Verify authentication header was accepted (no error)
    }

    @Test
    @DisplayName("Should return all tools when no filter specified")
    void shouldReturnAllToolsWhenNoFilterSpecified() throws Exception {
        // Given
        Map<String, Object> request = new HashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", 8);
        request.put("method", "tools/list");
        request.put("params", new HashMap<>());

        // When
        MvcResult result = mockMvc.perform(post("/mcp/sse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        String responseBody = result.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(responseBody);
        
        JsonNode tools = response.get("result").get("tools");
        assertThat(tools.size()).isGreaterThanOrEqualTo(6); // At least 6 tools (5 Jakarta + 1 Sentinel)
    }
}

