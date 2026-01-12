package adrianmikula.jakartamigration.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Custom SSE endpoint controller for MCP protocol.
 * 
 * This is a workaround for Spring AI MCP webmvc starter not registering SSE endpoints.
 * Implements the MCP protocol over SSE for Apify deployment.
 * 
 * Reference: https://modelcontextprotocol.io/docs/specification/transport
 */
@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
@Slf4j
public class McpSseController {

    private final JakartaMigrationTools jakartaMigrationTools;
    private final SentinelTools sentinelTools;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${spring.ai.mcp.server.name:jakarta-migration-mcp}")
    private String serverName;
    
    @Value("${spring.ai.mcp.server.version:1.0.0-SNAPSHOT}")
    private String serverVersion;
    
    // Store active SSE connections by session ID (simple implementation using thread ID for now)
    // In production, use proper session management with unique IDs
    private final Map<String, SseEmitter> activeConnections = new ConcurrentHashMap<>();
    
    // Scheduled executor for keepalive messages
    private final ScheduledExecutorService keepaliveExecutor = Executors.newScheduledThreadPool(1);

    /**
     * SSE endpoint for MCP protocol.
     * 
     * Apify/Clients connect to this endpoint via EventSource.
     * Supports:
     * - Authentication via Authorization header (Bearer token)
     * - Tool filtering via ?tools=tool1,tool2 query parameter
     * - JSON-RPC messages via query parameters
     * 
     * Reference: https://docs.apify.com/platform/integrations/mcp
     */
    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sseEndpoint(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) String tools,
            @RequestParam(required = false) String message) {
        
        log.info("SSE connection established for MCP server");
        
        // Validate authentication if provided (Apify requirement)
        if (authHeader != null) {
            if (!authHeader.startsWith("Bearer ")) {
                log.warn("Invalid Authorization header format");
                // For now, we'll allow connections without auth for testing
                // In production, you might want to reject here
            } else {
                String token = authHeader.substring(7);
                log.debug("Connection authenticated with token (length: {})", token.length());
                // TODO: Validate token if needed
            }
        }
        
        // Parse tool filter if provided (Apify supports ?tools=tool1,tool2)
        Set<String> enabledTools = parseToolsParameter(tools);
        
        // Set a reasonable timeout (30 seconds) - clients should send requests quickly
        // Long.MAX_VALUE can cause issues with some proxies/servers
        SseEmitter emitter = new SseEmitter(30000L);
        
        // Generate session ID before setting up connection
        String sessionId = UUID.randomUUID().toString();
        activeConnections.put(sessionId, emitter);
        log.info("Registered SSE connection with session ID: {}", sessionId);
        
        // Send initial connection message with session ID
        // This helps clients know which session to use for subsequent POST requests
        try {
            // Send session info as initial event
            Map<String, Object> sessionInfo = new HashMap<>();
            sessionInfo.put("sessionId", sessionId);
            sessionInfo.put("status", "connected");
            sendSseMessage(emitter, "session", sessionInfo);
            log.info("Sent session info to client: sessionId={}", sessionId);
        } catch (IOException e) {
            log.error("Failed to send initial SSE message", e);
            activeConnections.remove(sessionId);
            emitter.completeWithError(e);
            return emitter;
        }
        
        // Handle incoming message if provided via query parameter
        if (message != null && !message.isEmpty()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> request = objectMapper.readValue(message, Map.class);
                handleMcpRequestOverSse(emitter, request, enabledTools);
            } catch (Exception e) {
                log.error("Failed to process message from query parameter", e);
            }
        }
        
        // Start keepalive scheduler to prevent connection timeout
        // Send keepalive every 15 seconds (SSE connections can timeout without activity)
        keepaliveExecutor.scheduleAtFixedRate(() -> {
            try {
                if (activeConnections.containsKey(sessionId)) {
                    emitter.send(SseEmitter.event().comment("keepalive"));
                    log.trace("Sent SSE keepalive for session: {}", sessionId);
                }
            } catch (IOException e) {
                log.debug("Failed to send keepalive for session: {} - connection may be closed", sessionId);
                activeConnections.remove(sessionId);
            }
        }, 15, 15, TimeUnit.SECONDS);
        
        // Handle connection lifecycle
        emitter.onCompletion(() -> {
            log.info("SSE connection completed for session: {}", sessionId);
            activeConnections.remove(sessionId);
        });
        emitter.onTimeout(() -> {
            log.warn("SSE connection timed out for session: {}", sessionId);
            activeConnections.remove(sessionId);
            emitter.complete();
        });
        emitter.onError((ex) -> {
            log.error("SSE connection error for session: {}", sessionId, ex);
            activeConnections.remove(sessionId);
        });
        
        return emitter;
    }
    
    /**
     * Parse tools query parameter (Apify format: ?tools=tool1,tool2,tool3).
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

    /**
     * Handle MCP JSON-RPC requests via POST.
     * 
     * This endpoint accepts MCP protocol messages and routes them to the appropriate handlers.
     * Supports authentication via Authorization header (Apify requirement).
     * For SSE transport, responses are sent via SSE events to the active connection.
     * Also returns the response in the POST response for compatibility.
     */
    @PostMapping(value = "/sse", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> handleMcpRequest(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @RequestBody Map<String, Object> request) {
        
        String method = (String) request.get("method");
        log.info("Received MCP request: {} (session: {}, active connections: {})", 
            method, sessionId, activeConnections.size());
        
        // Validate authentication if provided
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            log.debug("Request authenticated with token (length: {})", token.length());
            // TODO: Validate token if needed
        }
        
        Map<String, Object> response = processMcpRequest(request);
        
        // For SSE transport, always try to send response via SSE if connection exists
        // This is the primary way MCP clients receive responses over SSE
        SseEmitter emitter = null;
        if (sessionId != null) {
            emitter = activeConnections.get(sessionId);
            if (emitter == null) {
                log.warn("Session ID {} not found in active connections (available: {})", 
                    sessionId, activeConnections.keySet());
            }
        } else if (!activeConnections.isEmpty()) {
            // Use first available connection if no session ID provided
            // This works for single-client scenarios (most common)
            emitter = activeConnections.values().iterator().next();
            log.debug("Using first available SSE connection (no session ID provided, total connections: {})", 
                activeConnections.size());
        }
        
        if (emitter != null) {
            final SseEmitter finalEmitter = emitter; // Make effectively final for lambda
            try {
                sendSseMessage(emitter, "message", response);
                log.info("Sent response via SSE for request: {} (id: {})", method, request.get("id"));
            } catch (IOException e) {
                log.warn("Failed to send response via SSE for request: {} (id: {}), connection may be closed. " +
                    "Returning POST response only.", method, request.get("id"), e);
                // Remove dead connection - find session ID and remove it
                String deadSessionId = null;
                for (Map.Entry<String, SseEmitter> entry : activeConnections.entrySet()) {
                    if (entry.getValue() == finalEmitter) {
                        deadSessionId = entry.getKey();
                        break;
                    }
                }
                if (deadSessionId != null) {
                    activeConnections.remove(deadSessionId);
                    log.debug("Removed dead SSE connection: {}", deadSessionId);
                }
            }
        } else {
            log.warn("No active SSE connection found for request: {} (id: {}). " +
                "Active connections: {}. Returning POST response only.", 
                method, request.get("id"), activeConnections.size());
        }
        
        // Always return response in POST body as well for compatibility
        // Some clients may read from POST response if SSE fails
        return ResponseEntity.ok(response);
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

    private Map<String, Object> handleToolsList() {
        return handleToolsList(Collections.emptySet());
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
    
    private List<Map<String, Object>> getToolsFromClass(Object toolClass) {
        return getToolsFromClass(toolClass, Collections.emptySet());
    }
    
    private List<Map<String, Object>> getToolsFromClass(Object toolClass, Set<String> enabledTools) {
        List<Map<String, Object>> tools = new ArrayList<>();
        
        for (Method method : toolClass.getClass().getDeclaredMethods()) {
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
            // Tool not found - return error response instead of throwing
            log.warn("Tool not found: " + toolName);
            throw e; // Let processMcpRequest handle it as a JSON-RPC error
        } catch (ReflectiveOperationException e) {
            log.error("Error executing tool: " + toolName, e);
            throw new McpToolExecutionException("Tool execution failed: " + e.getMessage(), e);
        }
    }
    
    private String executeTool(String toolName, Map<String, Object> arguments) throws IllegalArgumentException, ReflectiveOperationException {
        // Try JakartaMigrationTools first
        for (Method method : jakartaMigrationTools.getClass().getDeclaredMethods()) {
            org.springaicommunity.mcp.annotation.McpTool annotation = 
                method.getAnnotation(org.springaicommunity.mcp.annotation.McpTool.class);
            
            if (annotation != null && annotation.name().equals(toolName)) {
                return invokeTool(method, jakartaMigrationTools, arguments);
            }
        }
        
        // Try SentinelTools
        for (Method method : sentinelTools.getClass().getDeclaredMethods()) {
            org.springaicommunity.mcp.annotation.McpTool annotation = 
                method.getAnnotation(org.springaicommunity.mcp.annotation.McpTool.class);
            
            if (annotation != null && annotation.name().equals(toolName)) {
                return invokeTool(method, sentinelTools, arguments);
            }
        }
        
        throw new IllegalArgumentException("Tool not found: " + toolName);
    }
    
    private String invokeTool(Method method, Object instance, Map<String, Object> arguments) throws ReflectiveOperationException, IllegalArgumentException {
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
     * Handle MCP request over SSE connection.
     * Sends response back via SSE event.
     */
    private void handleMcpRequestOverSse(SseEmitter emitter, Map<String, Object> request, Set<String> enabledTools) {
        try {
            Map<String, Object> response = processMcpRequest(request, enabledTools);
            String json = objectMapper.writeValueAsString(response);
            
            // Send response as SSE event
            emitter.send(SseEmitter.event()
                .name("message")
                .data(json));
        } catch (Exception e) {
            log.error("Error handling MCP request over SSE", e);
            try {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("jsonrpc", "2.0");
                errorResponse.put("id", request.get("id"));
                errorResponse.put("error", Map.of(
                    "code", -32603,
                    "message", "Internal error: " + e.getMessage()
                ));
                String json = objectMapper.writeValueAsString(errorResponse);
                emitter.send(SseEmitter.event()
                    .name("message")
                    .data(json));
            } catch (IOException ioException) {
                log.error("Failed to send error response", ioException);
            }
        }
    }
    
    /**
     * Process MCP request and return response.
     */
    private Map<String, Object> processMcpRequest(Map<String, Object> request) {
        return processMcpRequest(request, Collections.emptySet());
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

    private void sendSseMessage(SseEmitter emitter, String event, Object data) throws IOException {
        String json = objectMapper.writeValueAsString(data);
        emitter.send(SseEmitter.event()
            .name(event)
            .data(json));
    }
}

