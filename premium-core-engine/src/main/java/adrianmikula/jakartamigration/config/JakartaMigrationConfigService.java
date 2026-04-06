package adrianmikula.jakartamigration.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

/**
 * Service for loading and providing access to Jakarta Migration configuration.
 * Centralizes all hardcoded values and provides dynamic lookup capabilities.
 */
@Slf4j
public class JakartaMigrationConfigService {
    
    private static volatile JakartaMigrationConfigService instance;
    private final ObjectMapper yamlMapper;
    private Map<String, Object> config;
    
    private JakartaMigrationConfigService() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.config = loadConfig();
    }
    
    /**
     * Gets the singleton instance of the configuration service.
     */
    public static JakartaMigrationConfigService getInstance() {
        if (instance == null) {
            synchronized (JakartaMigrationConfigService.class) {
                if (instance == null) {
                    instance = new JakartaMigrationConfigService();
                }
            }
        }
        return instance;
    }
    
    /**
     * Gets the server configuration.
     */
    public ServerConfig getServerConfig() {
        @SuppressWarnings("unchecked")
        Map<String, Object> serverConfig = (Map<String, Object>) config.get("server");
        if (serverConfig == null) {
            return new ServerConfig();
        }
        
        return new ServerConfig(
            getString(serverConfig, "name", "jakarta-migration-mcp"),
            getString(serverConfig, "display_name", "Jakarta Migration MCP"),
            getString(serverConfig, "version", "1.0.0"),
            getString(serverConfig, "description", "MCP server for Jakarta EE migration analysis and automation"),
            getString(serverConfig, "vendor", "Jakarta Migration Team"),
            getString(serverConfig, "url", "https://jakarta-migration.com")
        );
    }
    
    /**
     * Gets the database configuration.
     */
    public DatabaseConfig getDatabaseConfig() {
        @SuppressWarnings("unchecked")
        Map<String, Object> dbConfig = (Map<String, Object>) config.get("database");
        if (dbConfig == null) {
            return new DatabaseConfig();
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> centralConfig = (Map<String, Object>) dbConfig.get("central");
        @SuppressWarnings("unchecked")
        Map<String, Object> projectConfig = (Map<String, Object>) dbConfig.get("project");
        
        return new DatabaseConfig(
            new DatabaseConfig.DatabaseInfo(
                getString(centralConfig, "file", "central-migration-analysis.db"),
                getInt(centralConfig, "version", 5)
            ),
            new DatabaseConfig.DatabaseInfo(
                getString(projectConfig, "file", "jakarta-migration.db"),
                getInt(projectConfig, "version", 2)
            )
        );
    }
    
    /**
     * Gets dependency mapping for a given dependency coordinate.
     */
    public Optional<DependencyMapping> getDependencyMapping(String groupId, String artifactId) {
        String key = groupId + ":" + artifactId;
        @SuppressWarnings("unchecked")
        Map<String, Object> mappings = (Map<String, Object>) config.get("dependency_mappings");
        
        if (mappings != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> mapping = (Map<String, Object>) mappings.get(key);
            if (mapping != null) {
                String[] jakartaParts = getString(mapping, "jakarta", "").split(":");
                return Optional.of(new DependencyMapping(
                    jakartaParts.length >= 2 ? jakartaParts[0] : "",
                    jakartaParts.length >= 2 ? jakartaParts[1] : "",
                    getString(mapping, "version", ""),
                    getString(mapping, "notes", null)
                ));
            }
        }
        return Optional.empty();
    }
    
    /**
     * Gets third-party library information.
     */
    public Optional<ThirdPartyLibraryInfo> getThirdPartyLibraryInfo(String groupId, String artifactId) {
        String key = groupId + ":" + artifactId;
        @SuppressWarnings("unchecked")
        Map<String, Object> libraries = (Map<String, Object>) config.get("third_party_libraries");
        
        if (libraries != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> library = (Map<String, Object>) libraries.get(key);
            if (library != null) {
                return Optional.of(new ThirdPartyLibraryInfo(
                    getString(library, "name", ""),
                    getString(library, "jakarta_alternative", ""),
                    getString(library, "migration_status", ""),
                    getString(library, "notes", "")
                ));
            }
        }
        return Optional.empty();
    }
    
    /**
     * Gets transitive dependency mapping.
     */
    public Optional<TransitiveDependencyMapping> getTransitiveDependencyMapping(String groupId, String artifactId) {
        String key = groupId + ":" + artifactId;
        @SuppressWarnings("unchecked")
        Map<String, Object> mappings = (Map<String, Object>) config.get("transitive_dependencies");
        
        if (mappings != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> mapping = (Map<String, Object>) mappings.get(key);
            if (mapping != null) {
                return Optional.of(new TransitiveDependencyMapping(
                    getString(mapping, "jakarta", ""),
                    getString(mapping, "risk_level", ""),
                    getString(mapping, "recommendation", "")
                ));
            }
        }
        return Optional.empty();
    }
    
    /**
     * Gets support URL configuration.
     */
    public SupportConfig getSupportConfig() {
        @SuppressWarnings("unchecked")
        Map<String, Object> support = (Map<String, Object>) config.get("support");
        if (support == null) {
            return new SupportConfig();
        }
        
        return new SupportConfig(
            getString(support, "github", ""),
            getString(support, "linkedin", ""),
            getString(support, "plugin_page", ""),
            getString(support, "plugin_updates", ""),
            getString(support, "marketplace", ""),
            getString(support, "documentation", ""),
            getString(support, "issues", ""),
            getString(support, "discussions", "")
        );
    }
    
    /**
     * Reloads the configuration from the YAML file.
     */
    public void reloadConfig() {
        this.config = loadConfig();
        log.info("Configuration reloaded successfully");
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> loadConfig() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("jakarta-migration-config.yaml")) {
            if (is == null) {
                log.warn("jakarta-migration-config.yaml not found, using empty configuration");
                return Map.of();
            }
            
            Map<String, Object> loadedConfig = yamlMapper.readValue(is, Map.class);
            log.info("Successfully loaded Jakarta Migration configuration");
            return loadedConfig;
        } catch (Exception e) {
            log.error("Failed to load Jakarta Migration configuration", e);
            return Map.of();
        }
    }
    
    private String getString(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }
    
    private int getInt(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    /**
     * Server configuration data class.
     */
    public record ServerConfig(
        String name,
        String displayName,
        String version,
        String description,
        String vendor,
        String url
    ) {
        public ServerConfig() {
            this("jakarta-migration-mcp", "Jakarta Migration MCP", "1.0.0", 
                 "MCP server for Jakarta EE migration analysis and automation", 
                 "Jakarta Migration Team", "https://jakarta-migration.com");
        }
    }
    
    /**
     * Database configuration data class.
     */
    public record DatabaseConfig(
        DatabaseInfo central,
        DatabaseInfo project
    ) {
        public DatabaseConfig() {
            this(new DatabaseInfo("central-migration-analysis.db", 5),
                 new DatabaseInfo("jakarta-migration.db", 2));
        }
        
        public record DatabaseInfo(String file, int version) {}
    }
    
    /**
     * Dependency mapping data class.
     */
    public record DependencyMapping(
        String jakartaGroupId,
        String jakartaArtifactId,
        String version,
        String notes
    ) {}
    
    /**
     * Third-party library information data class.
     */
    public record ThirdPartyLibraryInfo(
        String name,
        String jakartaAlternative,
        String migrationStatus,
        String notes
    ) {}
    
    /**
     * Transitive dependency mapping data class.
     */
    public record TransitiveDependencyMapping(
        String jakarta,
        String riskLevel,
        String recommendation
    ) {}
    
    /**
     * Support configuration data class.
     */
    public record SupportConfig(
        String github,
        String linkedin,
        String pluginPage,
        String pluginUpdates,
        String marketplace,
        String documentation,
        String issues,
        String discussions
    ) {
        public SupportConfig() {
            this("", "", "", "", "", "", "", "");
        }
    }
}
