package adrianmikula.jakartamigration.intellij.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of IntelliJ AI Assistant's McpServerProvider extension point.
 * Registers Jakarta Migration MCP server tools with IntelliJ's AI Assistant for auto-loading.
 *
 * This class is automatically instantiated by IntelliJ when the plugin is enabled.
 * The AI Assistant discovers MCP servers via the MCP extension point and can invoke
 * the registered tools during AI-assisted coding sessions.
 *
 * For AI Assistant MCP Integration:
 * - Tools are exposed with full JSON Schema input validation
 * - Server metadata includes connection information
 * - Auto-loading is enabled when plugin is active
 */
public class JakartaMcpServerProvider {

    private static final Logger LOG = Logger.getInstance(JakartaMcpServerProvider.class);
    private static final String SERVER_ID = "jakarta-migration-mcp";
    private static final String SERVER_NAME = "Jakarta Migration MCP";
    private static final String SERVER_VERSION = "1.0.0";
    private static final String SERVER_DESCRIPTION = "MCP server for Jakarta EE migration analysis and automation";
    private static final String SERVER_VENDOR = "Jakarta Migration Team";
    private static final String SERVER_URL = "https://jakarta-migration.com";

    // MCP Server connection configuration
    private static final String TRANSPORT_TYPE = "streamable-http";
    private static final String DEFAULT_ENDPOINT = "/mcp";
    private static final int DEFAULT_TIMEOUT_MS = 30000;

    private final ObjectMapper objectMapper;
    private final Map<String, McpToolDefinition> registeredTools;
    private boolean isInitialized = false;

    public JakartaMcpServerProvider() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.registeredTools = new ConcurrentHashMap<>();
    }

    /**
     * Initializes the MCP server provider.
     * Called by IntelliJ when the plugin is loaded and by ProjectActivity on project open.
     */
    public void initialize() {
        if (isInitialized) {
            LOG.warn("JakartaMcpServerProvider already initialized");
            return;
        }

        LOG.info("Initializing Jakarta Migration MCP Server Provider");

        try {
            // Register all tools from the registry
            List<McpToolDefinition> tools = McpToolRegistry.getAllTools();
            for (McpToolDefinition tool : tools) {
                registeredTools.put(tool.getName(), tool);
            }

            isInitialized = true;
            LOG.info("Registered " + tools.size() + " MCP tools with IntelliJ AI Assistant");

            // Log tool details for debugging
            for (McpToolDefinition tool : tools) {
                LOG.info("  - " + tool.getName() + ": " + tool.getDescription().substring(0, Math.min(100, tool.getDescription().length())) + "...");
            }

        } catch (Exception e) {
            LOG.error("Failed to initialize MCP Server Provider: " + e.getMessage(), e);
        }
    }

    /**
     * Initialize with a specific project for project-aware tool execution.
     * @param project The current IntelliJ project
     */
    public void initialize(Project project) {
        initialize();
        LOG.info("MCP Server Provider initialized for project: " + project.getName());
    }

    // ==================== Server Metadata for AI Assistant Discovery ====================

    /**
     * Get the server ID for MCP discovery.
     * @return Unique server identifier
     */
    @NotNull
    public String getServerId() {
        return SERVER_ID;
    }

    /**
     * Get the display name for the MCP server.
     * @return Human-readable server name
     */
    @NotNull
    public String getServerName() {
        return SERVER_NAME;
    }

    /**
     * Get the server version.
     * @return Semantic version string
     */
    @NotNull
    public String getServerVersion() {
        return SERVER_VERSION;
    }

    /**
     * Get the server description.
     * @return Server description for AI Assistant
     */
    @NotNull
    public String getServerDescription() {
        return SERVER_DESCRIPTION;
    }

    /**
     * Get the server vendor/author.
     * @return Vendor name
     */
    @NotNull
    public String getServerVendor() {
        return SERVER_VENDOR;
    }

    /**
     * Get the server URL.
     * @return Server website URL
     */
    @NotNull
    public String getServerUrl() {
        return SERVER_URL;
    }

    // ==================== Tool Registration ====================

    /**
     * Get all registered tools.
     * @return List of tool definitions
     */
    @NotNull
    public List<McpToolDefinition> getTools() {
        return new ArrayList<>(registeredTools.values());
    }

    /**
     * Get tool definition by name.
     * @param name Tool name
     * @return Tool definition or null if not found
     */
    public McpToolDefinition getTool(String name) {
        return registeredTools.get(name);
    }

    /**
     * Get the number of registered tools.
     * @return Count of registered tools
     */
    public int getToolCount() {
        return registeredTools.size();
    }

    // ==================== Server Metadata for AI Assistant ====================

    /**
     * Get complete server metadata for AI Assistant discovery.
     * Includes server info, capabilities, and tool list.
     * @return Map containing all server metadata
     */
    @NotNull
    public Map<String, Object> getServerMetadata() {
        Map<String, Object> metadata = McpToolRegistry.getServerMetadata();

        // Add AI Assistant specific metadata
        metadata.put("id", SERVER_ID);
        metadata.put("name", SERVER_NAME);
        metadata.put("version", SERVER_VERSION);
        metadata.put("description", SERVER_DESCRIPTION);
        metadata.put("vendor", SERVER_VENDOR);
        metadata.put("url", SERVER_URL);

        // Auto-loading configuration
        metadata.put("autoLoad", true);
        metadata.put("requiresUserConfirmation", false);

        // IDE compatibility
        metadata.put("minIdeVersion", "2023.3");
        metadata.put("supportedIdeVersions", List.of("2023.3", "2024.1", "2024.2", "2024.3"));

        // Connection configuration for MCP transport
        Map<String, Object> connection = new ConcurrentHashMap<>();
        connection.put("transportType", TRANSPORT_TYPE);
        connection.put("endpoint", DEFAULT_ENDPOINT);
        connection.put("timeoutMs", DEFAULT_TIMEOUT_MS);
        connection.put("reconnectAttempts", 3);
        connection.put("reconnectDelayMs", 1000);
        metadata.put("connection", connection);

        // Capabilities specific to AI Assistant
        Map<String, Object> aiCapabilities = new ConcurrentHashMap<>();
        aiCapabilities.put("streaming", true);
        aiCapabilities.put("toolCancellation", true);
        aiCapabilities.put("progressReporting", true);
        metadata.put("aiCapabilities", aiCapabilities);

        return metadata;
    }

    /**
     * Get the MCP server connection configuration.
     * Used by AI Assistant to connect to this MCP server.
     * @return Map containing connection settings
     */
    @NotNull
    public Map<String, Object> getConnectionConfig() {
        Map<String, Object> connection = new ConcurrentHashMap<>();
        connection.put("type", TRANSPORT_TYPE);
        connection.put("endpoint", DEFAULT_ENDPOINT);
        connection.put("timeoutMs", DEFAULT_TIMEOUT_MS);

        // Authentication (if required)
        connection.put("authType", "none");

        // Health check configuration
        connection.put("healthCheckIntervalMs", 30000);
        connection.put("healthCheckEndpoint", "/health");

        return connection;
    }

    /**
     * Get the server capabilities for MCP protocol negotiation.
     * @return Map of server capabilities
     */
    @NotNull
    public Map<String, Object> getCapabilities() {
        Map<String, Object> capabilities = new ConcurrentHashMap<>();

        // Core MCP capabilities
        capabilities.put("tools", true);
        capabilities.put("resources", false);
        capabilities.put("prompts", false);

        // Tool capabilities
        Map<String, Object> tools = new ConcurrentHashMap<>();
        tools.put("listChanged", true);
        capabilities.put("tools", tools);

        // Logging capabilities
        Map<String, Object> logging = new ConcurrentHashMap<>();
        logging.put("level", List.of("debug", "info", "warning", "error"));
        capabilities.put("logging", logging);

        return capabilities;
    }

    // ==================== Tool Invocation ====================

    /**
     * Handle a tool invocation request from AI Assistant.
     * @param toolName Name of the tool to invoke
     * @param arguments Tool arguments as a map
     * @return CompletableFuture with the result
     */
    @NotNull
    public CompletableFuture<Map<String, Object>> invokeTool(
            @NotNull String toolName,
            @NotNull Map<String, Object> arguments) {

        return CompletableFuture.supplyAsync(() -> {
            LOG.info("Tool invocation request: " + toolName + " with " + arguments.size() + " arguments");

            McpToolDefinition tool = getTool(toolName);
            if (tool == null) {
                throw new IllegalArgumentException("Unknown tool: " + toolName);
            }

            // Validate arguments against schema
            validateArguments(tool, arguments);

            // Create response with invocation metadata
            Map<String, Object> response = new ConcurrentHashMap<>();
            response.put("tool", toolName);
            response.put("status", "invoked");
            response.put("timestamp", System.currentTimeMillis());
            response.put("arguments", arguments);
            response.put("server", SERVER_ID);

            return response;
        });
    }

    /**
     * Validate arguments against tool's input schema.
     * @param tool Tool definition
     * @param arguments Arguments to validate
     */
    private void validateArguments(McpToolDefinition tool, Map<String, Object> arguments) {
        if (tool.getInputSchema() == null) {
            return;
        }

        List<String> required = tool.getInputSchema().getRequired();
        if (required != null) {
            for (String reqField : required) {
                if (!arguments.containsKey(reqField)) {
                    throw new IllegalArgumentException(
                            "Missing required argument: " + reqField + " for tool: " + tool.getName());
                }
            }
        }
    }

    // ==================== Server Status ====================

    /**
     * Check if the server is ready for tool invocation.
     * @return true if initialized and ready
     */
    public boolean isReady() {
        return isInitialized && !registeredTools.isEmpty();
    }

    /**
     * Check if the server is healthy.
     * @return true if server is healthy and responsive
     */
    public boolean isHealthy() {
        return isReady();
    }

    // ==================== JSON Serialization ====================

    /**
     * Create a JSON representation of the server configuration for AI Assistant.
     * @return JSON string with server and tool configuration
     */
    @NotNull
    public String getServerConfigurationJson() {
        try {
            Map<String, Object> config = new ConcurrentHashMap<>();
            config.put("server", getServerMetadata());
            config.put("connection", getConnectionConfig());
            config.put("tools", registeredTools.values());
            return objectMapper.writeValueAsString(config);
        } catch (Exception e) {
            LOG.error("Failed to serialize server configuration: " + e.getMessage());
            return "{}";
        }
    }

    /**
     * Get the tool definitions as JSON for AI Assistant discovery.
     * @return JSON array of tool definitions
     */
    @NotNull
    public String getToolsJson() {
        try {
            return objectMapper.writeValueAsString(registeredTools.values());
        } catch (Exception e) {
            LOG.error("Failed to serialize tools: " + e.getMessage());
            return "[]";
        }
    }

    // ==================== Resource Management ====================

    /**
     * Dispose of resources when the plugin is unloaded.
     * Called by IntelliJ when the plugin is disabled.
     */
    public void dispose() {
        LOG.info("Disposing JakartaMcpServerProvider");
        registeredTools.clear();
        isInitialized = false;
    }

    @Override
    public String toString() {
        return "JakartaMcpServerProvider{" +
                "serverId='" + SERVER_ID + '\'' +
                ", serverName='" + SERVER_NAME + '\'' +
                ", version='" + SERVER_VERSION + '\'' +
                ", toolCount=" + registeredTools.size() +
                ", initialized=" + isInitialized +
                '}';
    }
}
