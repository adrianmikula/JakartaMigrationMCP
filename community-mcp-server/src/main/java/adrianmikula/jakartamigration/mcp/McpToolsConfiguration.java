package adrianmikula.jakartamigration.mcp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for MCP Tools registration.
 * 
 * NOTE: This configuration is currently disabled because Spring AI MCP annotation
 * classes are not available in the current dependency version.
 * 
 * Once the correct Spring AI MCP dependencies are available, this will manually
 * register @McpTool annotated methods as a workaround for annotation scanner issues.
 * 
 * Reference: https://github.com/spring-projects/spring-ai/issues/4392
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class McpToolsConfiguration {
    
    private final JakartaMigrationTools jakartaMigrationTools;
    
    /**
     * Manually registers JakartaMigrationTools methods as MCP tools.
     * 
     * TODO: Uncomment once Spring AI MCP annotation classes are available.
     * The classes ToolCallbackProvider and MethodToolCallbackProvider need to be
     * available from the spring-ai-starter-mcp-server dependency.
     * 
     * @return ToolCallbackProvider that provides access to all Jakarta migration tools
     */
    // @Bean
    // public ToolCallbackProvider toolCallbackProvider() {
    //     log.info("Registering Jakarta Migration MCP tools manually via ToolCallbackProvider");
    //     
    //     try {
    //         ToolCallbackProvider provider = MethodToolCallbackProvider.builder()
    //             .toolObjects(jakartaMigrationTools)
    //             .build();
    //         
    //         log.info("Successfully registered Jakarta Migration MCP tools");
    //         return provider;
    //     } catch (Exception e) {
    //         log.error("Failed to register MCP tools manually", e);
    //         return MethodToolCallbackProvider.builder().build();
    //     }
    // }
}

