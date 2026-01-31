package adrianmikula.jakartamigration.mcp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration to provide MCP server info for GetInstructions/ListOfferings actions.
 * 
 * This is a workaround for Spring AI MCP 1.1.2 issue where server info is not properly
 * stored, causing GetInstructions/ListOfferings to fail and timeout Cursor.
 * 
 * Note: This may not fully resolve the issue if Spring AI MCP doesn't use this bean,
 * but it's an attempt to provide server metadata that might be needed.
 */
@Configuration
@Slf4j
public class McpServerInfoConfiguration {
    
    /**
     * Server info properties from application.yml.
     * This ensures server name, version, and description are available.
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.ai.mcp.server")
    public McpServerInfoProperties mcpServerInfoProperties() {
        log.info("Configuring MCP server info properties");
        return new McpServerInfoProperties();
    }
    
    /**
     * Properties class to hold MCP server info.
     */
    public static class McpServerInfoProperties {
        private String name;
        private String version;
        private String description;
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getVersion() {
            return version;
        }
        
        public void setVersion(String version) {
            this.version = version;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
    }
}

