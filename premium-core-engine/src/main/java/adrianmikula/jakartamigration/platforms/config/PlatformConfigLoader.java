package adrianmikula.jakartamigration.platforms.config;

import adrianmikula.jakartamigration.platforms.model.DetectionPattern;
import adrianmikula.jakartamigration.platforms.model.JakartaCompatibility;
import adrianmikula.jakartamigration.platforms.model.PlatformConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads and parses platform configurations from platforms.yaml
 */
public class PlatformConfigLoader {
    private static final String PLATFORMS_CONFIG_PATH = "/config/platforms.yaml";
    private final Map<String, PlatformConfig> platformConfigs;
    private final RiskScoringConfig riskScoringConfig;
    
    public PlatformConfigLoader() {
        this.platformConfigs = loadPlatformConfigs();
        this.riskScoringConfig = loadRiskScoringConfig();
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
            Map<String, Object> yamlData = yamlMapper.readValue(inputStream, Map.class);
            
            return parsePlatformConfigs(yamlData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load platform configurations", e);
        }
    }
    
    /**
     * Parse platform configurations from YAML data
     */
    private Map<String, PlatformConfig> parsePlatformConfigs(Map<String, Object> yamlData) {
        Map<String, PlatformConfig> configs = new HashMap<>();
        
        if (yamlData.containsKey("platforms")) {
            Map<String, Object> platforms = (Map<String, Object>) yamlData.get("platforms");
            
            for (Map.Entry<String, Object> entry : platforms.entrySet()) {
                String platformName = entry.getKey();
                Map<String, Object> platformData = (Map<String, Object>) entry.getValue();
                
                PlatformConfig config = parsePlatformConfig(platformName, platformData);
                configs.put(platformName, config);
            }
        }
        
        return configs;
    }
    
    /**
     * Parse individual platform configuration
     */
    private PlatformConfig parsePlatformConfig(String platformName, Map<String, Object> platformData) {
        String name = (String) platformData.getOrDefault("name", platformName);
        String description = (String) platformData.getOrDefault("description", "");
        
        // Parse patterns
        List<DetectionPattern> patterns = new ArrayList<>();
        if (platformData.containsKey("patterns")) {
            List<Map<String, Object>> patternList = (List<Map<String, Object>>) platformData.get("patterns");
            for (Map<String, Object> patternData : patternList) {
                String file = (String) patternData.get("file");
                String regex = (String) patternData.get("regex");
                int versionGroup = (Integer) patternData.getOrDefault("versionGroup", 1);
                patterns.add(new DetectionPattern(file, regex, versionGroup));
            }
        }
        
        // Parse Jakarta compatibility
        JakartaCompatibility jakartaCompatibility = null;
        if (platformData.containsKey("jakartaCompatibility")) {
            Map<String, Object> compatData = (Map<String, Object>) platformData.get("jakartaCompatibility");
            String minVersion = (String) compatData.getOrDefault("minVersion", "11");
            List<String> supportedVersions = (List<String>) compatData.getOrDefault("supportedVersions", List.of("11"));
            jakartaCompatibility = new JakartaCompatibility(minVersion, supportedVersions);
        }
        
        // Parse optional fields
        List<String> javaxVersions = new ArrayList<>();
        if (platformData.containsKey("javaxVersions")) {
            javaxVersions = (List<String>) platformData.get("javaxVersions");
        }
        
        Map<String, String> requirements = new HashMap<>();
        if (platformData.containsKey("requirements")) {
            requirements = (Map<String, String>) platformData.get("requirements");
        }
        
        List<String> commonArtifacts = new ArrayList<>();
        if (platformData.containsKey("commonArtifacts")) {
            commonArtifacts = (List<String>) platformData.get("commonArtifacts");
        }
        
        return new PlatformConfig(name, description, patterns, jakartaCompatibility, 
                               javaxVersions, requirements, commonArtifacts);
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
     * Loads risk scoring configuration from YAML file
     */
    private RiskScoringConfig loadRiskScoringConfig() {
        try {
            InputStream inputStream = getClass().getResourceAsStream(PLATFORMS_CONFIG_PATH);
            if (inputStream == null) {
                throw new RuntimeException("Could not find platforms.yaml at " + PLATFORMS_CONFIG_PATH);
            }
            
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            Map<String, Object> yamlData = mapper.readValue(inputStream, Map.class);
            
            RiskScoringConfig config = new RiskScoringConfig();
            
            // Load risk scoring section
            if (yamlData.containsKey("riskScoring")) {
                Map<String, Object> riskScoring = (Map<String, Object>) yamlData.get("riskScoring");
                
                // Load platform base risks
                if (riskScoring.containsKey("platformBaseRisks")) {
                    config.setPlatformBaseRisks((Map<String, Integer>) riskScoring.get("platformBaseRisks"));
                }
                
                // Load deployment artifact risks
                if (riskScoring.containsKey("deploymentArtifacts")) {
                    config.setDeploymentArtifacts((Map<String, Integer>) riskScoring.get("deploymentArtifacts"));
                }
            }
            
            return config;
            
        } catch (Exception e) {
            // Return default configuration if loading fails
            return new RiskScoringConfig();
        }
    }
    
    /**
     * Gets the risk scoring configuration
     */
    public RiskScoringConfig getRiskScoringConfig() {
        return riskScoringConfig;
    }
    
    /**
     * Gets all platform configurations
     */
    public Map<String, PlatformConfig> getPlatformConfigs() {
        return platformConfigs;
    }
}
