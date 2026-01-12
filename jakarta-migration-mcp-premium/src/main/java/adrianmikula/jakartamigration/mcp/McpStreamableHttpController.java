package adrianmikula.jakartamigration.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Streamable HTTP endpoint controller for MCP protocol.
 * 
 * This implements the newer Streamable HTTP transport, which is simpler than SSE:
 * - Single POST endpoint (no SSE complexity)
 * - Standard HTTP requests/responses
 * - No keepalive messages needed
 * - Better proxy compatibility
 * 
 * Reference: https://modelcontextprotocol.io/docs/specification/transport
 * Note: SSE transport is deprecated in favor of Streamable HTTP (as of MCP spec 2025-03-26)
 */
@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
@Slf4j
public class McpStreamableHttpController {

    private final JakartaMigrationTools jakartaMigrationTools;
    private final SentinelTools sentinelTools;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${spring.ai.mcp.server.name:jakarta-migration-mcp}")
    private String serverName;
    
    @Value("${spring.ai.mcp.server.version:1.0.0-SNAPSHOT}")
    private String serverVersion;

    /**
     * Streamable HTTP endpoint for MCP protocol.
     * 
     * This is a single POST endpoint that handles all MCP protocol methods.
     * Much simpler than SSE - no connection management, keepalive, or bidirectional communication needed.
     * 
     * Supports:
     * - Authentication via Authorization header (Bearer token)
     * - Tool filtering via ?tools=tool1,tool2 query parameter
     * - Session management via ?session=<session-id> query parameter (optional)
     * 
     * Reference: https://modelcontextprotocol.io/docs/specification/transport
     */
    @PostMapping(
        value = "/streamable-http",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> handleMcpRequest(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "session", required = false) String sessionId,
            @RequestParam(required = false) String tools,
            @RequestBody Map<String, Object> request) {
        
        String method = (String) request.get("method");
        log.info("Received MCP Streamable HTTP request: {} (session: {})", method, sessionId);
        
        // Validate authentication if provided
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            log.debug("Request authenticated with token (length: {})", token.length());
            // TODO: Validate token if needed
        }
        
        // Parse tool filter if provided
        Set<String> enabledTools = parseToolsParameter(tools);
        
        // Process the MCP request (reuse logic from SSE controller)
        Map<String, Object> response = processMcpRequest(request, enabledTools);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Parse tools query parameter (format: ?tools=tool1,tool2,tool3).
     * Returns set of enabled tool names, or empty set if all tools should be enabled.
     */
    private Set<String> parseToolsParameter(String tools) {
        if (tools == null || tools.isEmpty()) {
            return Collections.emptySet(); // Empty means all tools enabled
        }
        
        Set<String> enabledTools = new HashSet<>();
        for (String tool : tools.split(",")) {
            String trimmed = tool.trim();
            if (!trimmed.isEmpty()) {
                enabledTools.add(trimmed);
            }
        }
        
        log.info("Tool filter enabled: {}", enabledTools);
        return enabledTools;
    }

    private Map<String, Object> handleInitialize(Map<String, Object> request) {
        log.info("Handling initialize request");
        
        Map<String, Object> result = new HashMap<>();
        result.put("protocolVersion", "2024-11-05");
        result.put("capabilities", getServerCapabilities());
        result.put("serverInfo", Map.of(
            "name", serverName,
            "version", serverVersion
        ));
        
        return result;
    }
    
    private Map<String, Object> handleToolsList(Set<String> enabledTools) {
        log.info("Handling tools/list request (filter: {})", enabledTools.isEmpty() ? "all" : enabledTools);
        
        List<Map<String, Object>> tools = new ArrayList<>();
        
        // Get tools from JakartaMigrationTools
        tools.addAll(getToolsFromClass(jakartaMigrationTools, enabledTools));
        
        // Get tools from SentinelTools
        tools.addAll(getToolsFromClass(sentinelTools, enabledTools));
        
        return Map.of("tools", tools);
    }
    
    private List<Map<String, Object>> getToolsFromClass(Object toolClass, Set<String> enabledTools) {
        List<Map<String, Object>> tools = new ArrayList<>();
        
        for (java.lang.reflect.Method method : toolClass.getClass().getDeclaredMethods()) {
            org.springaicommunity.mcp.annotation.McpTool annotation = 
                method.getAnnotation(org.springaicommunity.mcp.annotation.McpTool.class);
            
            if (annotation != null) {
                // Filter tools if enabledTools is specified
                if (!enabledTools.isEmpty() && !enabledTools.contains(annotation.name())) {
                    continue; // Skip this tool if not in enabled list
                }
                Map<String, Object> toolMap = new HashMap<>();
                toolMap.put("name", annotation.name());
                toolMap.put("description", annotation.description());
                
                // Build input schema from method parameters
                Map<String, Object> inputSchema = new HashMap<>();
                inputSchema.put("type", "object");
                Map<String, Object> properties = new HashMap<>();
                List<String> required = new ArrayList<>();
                
                for (int i = 0; i < method.getParameterCount(); i++) {
                    java.lang.reflect.Parameter param = method.getParameters()[i];
                    org.springaicommunity.mcp.annotation.McpToolParam paramAnnotation = 
                        param.getAnnotation(org.springaicommunity.mcp.annotation.McpToolParam.class);
                    
                    if (paramAnnotation != null) {
                        Map<String, Object> prop = new HashMap<>();
                        prop.put("type", getJsonType(param.getType()));
                        prop.put("description", paramAnnotation.description());
                        properties.put(param.getName(), prop);
                        
                        // Add to required list if parameter is required
                        if (paramAnnotation.required()) {
                            required.add(param.getName());
                        }
                    }
                }
                
                inputSchema.put("properties", properties);
                if (!required.isEmpty()) {
                    inputSchema.put("required", required);
                }
                toolMap.put("inputSchema", inputSchema);
                
                tools.add(toolMap);
            }
        }
        
        return tools;
    }
    
    private String getJsonType(Class<?> type) {
        if (type == String.class) return "string";
        if (type == Integer.class || type == int.class) return "integer";
        if (type == Boolean.class || type == boolean.class) return "boolean";
        if (type == Double.class || type == double.class || type == Float.class || type == float.class) return "number";
        return "string"; // default
    }

    private Map<String, Object> handleToolCall(Map<String, Object> request) {
        log.info("Handling tools/call request");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) request.get("params");
        String toolName = (String) params.get("name");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");
        
        try {
            String result = executeTool(toolName, arguments);
            return Map.of(
                "content", List.of(Map.of(
                    "type", "text",
                    "text", result
                ))
            );
        } catch (IllegalArgumentException e) {
            log.warn("Tool not found: " + toolName);
            throw e; // Let processMcpRequest handle it as a JSON-RPC error
        } catch (ReflectiveOperationException e) {
            log.error("Error executing tool: " + toolName, e);
            throw new McpToolExecutionException("Tool execution failed: " + e.getMessage(), e);
        }
    }
    
    private String executeTool(String toolName, Map<String, Object> arguments) throws IllegalArgumentException, ReflectiveOperationException {
        // Try JakartaMigrationTools first
        for (java.lang.reflect.Method method : jakartaMigrationTools.getClass().getDeclaredMethods()) {
            org.springaicommunity.mcp.annotation.McpTool annotation = 
                method.getAnnotation(org.springaicommunity.mcp.annotation.McpTool.class);
            
            if (annotation != null && annotation.name().equals(toolName)) {
                return invokeTool(method, jakartaMigrationTools, arguments);
            }
        }
        
        // Try SentinelTools
        for (java.lang.reflect.Method method : sentinelTools.getClass().getDeclaredMethods()) {
            org.springaicommunity.mcp.annotation.McpTool annotation = 
                method.getAnnotation(org.springaicommunity.mcp.annotation.McpTool.class);
            
            if (annotation != null && annotation.name().equals(toolName)) {
                return invokeTool(method, sentinelTools, arguments);
            }
        }
        
        throw new IllegalArgumentException("Tool not found: " + toolName);
    }
    
    private String invokeTool(java.lang.reflect.Method method, Object instance, Map<String, Object> arguments) throws ReflectiveOperationException, IllegalArgumentException {
        Object[] params = new Object[method.getParameterCount()];
        java.lang.reflect.Parameter[] methodParams = method.getParameters();
        
        for (int i = 0; i < methodParams.length; i++) {
            String paramName = methodParams[i].getName();
            Object value = arguments.get(paramName);
            
            // Convert to appropriate type
            Class<?> paramType = methodParams[i].getType();
            if (value == null) {
                params[i] = null;
            } else if (paramType == String.class) {
                params[i] = value.toString();
            } else if (paramType == Integer.class || paramType == int.class) {
                params[i] = value instanceof Number ? ((Number) value).intValue() : Integer.parseInt(value.toString());
            } else {
                params[i] = value;
            }
        }
        
        Object result = method.invoke(instance, params);
        return result != null ? result.toString() : "";
    }

    private Map<String, Object> getServerCapabilities() {
        Map<String, Object> capabilities = new HashMap<>();
        // Enable tools capability
        Map<String, Object> toolsCapability = new HashMap<>();
        toolsCapability.put("listChanged", false); // We don't support notifications yet
        capabilities.put("tools", toolsCapability);
        
        // Enable other capabilities that might be expected
        Map<String, Object> promptsCapability = new HashMap<>();
        promptsCapability.put("listChanged", false);
        capabilities.put("prompts", promptsCapability);
        
        Map<String, Object> resourcesCapability = new HashMap<>();
        resourcesCapability.put("subscribe", false);
        resourcesCapability.put("listChanged", false);
        capabilities.put("resources", resourcesCapability);
        
        return capabilities;
    }
    
    /**
     * Process MCP request and return response with tool filtering.
     */
    private Map<String, Object> processMcpRequest(Map<String, Object> request, Set<String> enabledTools) {
        log.debug("Processing MCP request: {}", request);
        
        String method = (String) request.get("method");
        Object id = request.get("id");
        
        Map<String, Object> response = new HashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        
        try {
            switch (method) {
                case "initialize":
                    response.put("result", handleInitialize(request));
                    break;
                case "tools/list":
                    response.put("result", handleToolsList(enabledTools));
                    break;
                case "tools/call":
                    response.put("result", handleToolCall(request));
                    break;
                case "ping":
                    response.put("result", Map.of("status", "pong"));
                    break;
                default:
                    response.put("error", Map.of(
                        "code", -32601,
                        "message", "Method not found: " + method
                    ));
            }
        } catch (IllegalArgumentException e) {
            // Tool not found or invalid argument - return JSON-RPC error
            response.put("error", Map.of(
                "code", -32602,
                "message", e.getMessage()
            ));
        } catch (RuntimeException e) {
            // Other runtime errors - return JSON-RPC error
            response.put("error", Map.of(
                "code", -32603,
                "message", "Internal error: " + e.getMessage()
            ));
        }
        
        return response;
    }
}

