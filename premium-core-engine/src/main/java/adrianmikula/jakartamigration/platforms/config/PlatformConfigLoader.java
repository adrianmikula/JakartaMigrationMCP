package adrianmikula.jakartamigration.platforms.config;

import adrianmikula.jakartamigration.platforms.model.DetectionPattern;
import adrianmikula.jakartamigration.platforms.model.JakartaCompatibility;
import adrianmikula.jakartamigration.platforms.model.PlatformConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads and parses platform configurations from platforms.yaml
 */
public class PlatformConfigLoader {
    private static final String PLATFORMS_CONFIG_PATH = "/config/platforms.yaml";
    private final Map<String, PlatformConfig> platformConfigs;
    
    public PlatformConfigLoader() {
        this.platformConfigs = loadPlatformConfigs();
    }
    
    /**
     * Loads all platform configurations from YAML file
     */
    public Map<String, PlatformConfig> loadPlatformConfigs() {
        try {
            InputStream inputStream = getClass().getResourceAsStream(PLATFORMS_CONFIG_PATH);
            if (inputStream == null) {
                throw new RuntimeException("Could not find platforms.yaml at " + PLATFORMS_CONFIG_PATH);
            }
            
            ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
            PlatformsConfig config = yamlMapper.readValue(inputStream, PlatformsConfig.class);
            
            return config.platforms();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load platform configurations", e);
        }
    }
    
    /**
     * Gets configuration for a specific platform type
     */
    public PlatformConfig getPlatformConfig(String platformType) {
        return platformConfigs.get(platformType);
    }
    
    /**
     * Gets all available platform configurations
     */
    public Map<String, PlatformConfig> getAllPlatformConfigs() {
        return new HashMap<>(platformConfigs);
    }
    
    /**
     * DTO for YAML deserialization
     */
    private record PlatformsConfig(
        Map<String, PlatformConfig> platforms,
        RiskScoringConfig riskScoring
    ) {}
    
    /**
     * Risk scoring configuration from YAML
     */
    public record RiskScoringConfig(
        int majorVersionChange,
        int frameworkChange,
        int runtimeChange
    ) {}
}
