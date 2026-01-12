package adrianmikula.jakartamigration.mcp;

import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * Sentinel Tools - Clean implementation example for MCP tools.
 * 
 * This class demonstrates the "clean" pattern for implementing MCP tools:
 * - Uses ONLY @McpTool (not @Tool)
 * - Returns simple String types (not reactive types)
 * - Uses proper logging (not System.out.println)
 * - Is a Spring-managed @Component bean
 * 
 * This pattern ensures tools are properly registered by the MCP annotation scanner
 * when using type: SYNC in application.yml.
 */
@Component
@Slf4j
public class SentinelTools {

    /**
     * Verifies if required environment variables are present.
     * 
     * This is a simple "sentinel" tool that can be used to verify the MCP server
     * is working correctly and can access environment variables.
     * 
     * @param name The environment variable name to check
     * @return Status message indicating if the variable is defined or missing
     */
    @McpTool(
        name = "check_env",
        description = "Verifies if required env vars are present. Returns status message indicating if the variable is defined or missing."
    )
    public String checkEnv(
            @McpToolParam(description = "Variable name to check") String name) {
        // CRITICAL: NO System.out.println here! Use a proper logger if needed (to file/stderr)
        // System.out.println corrupts the MCP JSON stream and causes disconnection
        log.debug("Checking environment variable: {}", name);
        
        String val = System.getenv(name);
        return (val != null) ? "Defined: " + val : "Missing: " + name;
    }
}

