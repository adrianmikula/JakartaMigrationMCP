package adrianmikula.jakartamigration.intellij.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

/**
 * Feature flags configuration for the Jakarta Migration plugin.
 * Controls which features are enabled/disabled including beta features.
 */
public class FeatureFlags {
    
    private static FeatureFlags instance;
    private final Map<String, Boolean> flags;
    private final Map<String, FeatureConfig> featureConfigs;
    
    /**
     * Configuration for a single feature.
     */
    public static class FeatureConfig {
        private boolean enabled;
        private String name;
        private String description;
        private boolean beta;
        
        public boolean isEnabled() { return enabled; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public boolean isBeta() { return beta; }
    }
    
    private FeatureFlags() {
        flags = new HashMap<>();
        featureConfigs = new HashMap<>();
        loadDefaults();
        loadFromConfig();
    }
    
    public static synchronized FeatureFlags getInstance() {
        if (instance == null) {
            instance = new FeatureFlags();
        }
        return instance;
    }
    
    private void loadDefaults() {
        // Default feature flags
        flags.put("runtimeTab", false);
        flags.put("mcpServerTab", true);
        flags.put("advancedScans", false);
        
        // Feature configurations
        FeatureConfig runtimeConfig = new FeatureConfig();
        runtimeConfig.enabled = false;
        runtimeConfig.name = "Runtime Error Diagnosis";
        runtimeConfig.description = "Analyze runtime errors and provide remediation steps";
        runtimeConfig.beta = true;
        featureConfigs.put("runtimeTab", runtimeConfig);
        
        FeatureConfig mcpConfig = new FeatureConfig();
        mcpConfig.enabled = true;
        mcpConfig.name = "MCP Server";
        mcpConfig.description = "MCP server for AI Assistant integration";
        mcpConfig.beta = false;
        featureConfigs.put("mcpServerTab", mcpConfig);
        
        FeatureConfig advancedScansConfig = new FeatureConfig();
        advancedScansConfig.enabled = false;
        advancedScansConfig.name = "Advanced Scans";
        advancedScansConfig.description = "Deep scanning for JPA, CDI, Servlet and other frameworks";
        advancedScansConfig.beta = false;
        featureConfigs.put("advancedScans", advancedScansConfig);
    }
    
    private void loadFromConfig() {
        try {
            InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("feature-flags.yaml");
            
            if (inputStream == null) {
                // Try loading from working directory
                File configFile = new File("config/feature-flags.yaml");
                if (configFile.exists()) {
                    inputStream = new FileInputStream(configFile);
                } else {
                    return;
                }
            }
            
            Yaml yaml = new Yaml();
            Map<String, Object> config = yaml.load(inputStream);
            
            if (config != null && config.containsKey("features")) {
                Map<String, Object> features = (Map<String, Object>) config.get("features");
                for (Map.Entry<String, Object> entry : features.entrySet()) {
                    Map<String, Object> feature = (Map<String, Object>) entry.getValue();
                    Boolean enabled = (Boolean) feature.get("enabled");
                    flags.put(entry.getKey(), enabled != null && enabled);
                }
            }
            
        } catch (Exception e) {
            // Use defaults if config loading fails
        }
    }
    
    /**
     * Checks if a feature is enabled.
     */
    public boolean isEnabled(String featureKey) {
        return flags.getOrDefault(featureKey, false);
    }
    
    /**
     * Checks if beta features are enabled (for user preference).
     */
    public boolean isBetaFeaturesEnabled() {
        return "true".equals(System.getProperty("jakarta.migration.beta_features", "false"));
    }
    
    /**
     * Checks if experimental features are enabled (for user preference).
     */
    public boolean isExperimentalFeaturesEnabled() {
        return "true".equals(System.getProperty("jakarta.migration.experimental_features", "false"));
    }
    
    /**
     * Enables or disables beta features.
     */
    public void setBetaFeaturesEnabled(boolean enabled) {
        System.setProperty("jakarta.migration.beta_features", String.valueOf(enabled));
        // Update runtime tab flag
        flags.put("runtimeTab", enabled);
    }
    
    /**
     * Gets the feature configuration.
     */
    public FeatureConfig getFeatureConfig(String featureKey) {
        return featureConfigs.get(featureKey);
    }
    
    /**
     * Gets all feature configurations.
     */
    public Map<String, FeatureConfig> getAllFeatureConfigs() {
        return new HashMap<>(featureConfigs);
    }
}
