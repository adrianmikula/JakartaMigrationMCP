package adrianmikula.jakartamigration.intellij.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

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
        flags.put("experimental_features", false); // Add experimental features flag
        
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
        
        // Experimental features configuration
        FeatureConfig experimentalConfig = new FeatureConfig();
        experimentalConfig.enabled = false;
        experimentalConfig.name = "Experimental Features";
        experimentalConfig.description = "Cutting-edge features under development";
        experimentalConfig.beta = true;
        featureConfigs.put("experimental_features", experimentalConfig);
    }
    
    private void loadFromConfig() {
        try {
            // First try to load from gradle.properties
            loadFromGradleProperties();
            
            // Then try to load from feature-flags.yaml
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
    
    private void loadFromGradleProperties() {
        try {
            // Try multiple possible locations for gradle.properties
            String[] possiblePaths = {
                "gradle.properties",                    // Current directory
                "../gradle.properties",                 // Parent directory
                "../../gradle.properties",              // Two levels up
                "../../../gradle.properties",           // Three levels up
                System.getProperty("user.dir") + "/gradle.properties" // User working directory
            };
            
            for (String path : possiblePaths) {
                File gradlePropsFile = new File(path);
                
                if (gradlePropsFile.exists()) {
                    Properties props = new Properties();
                    try (FileInputStream fis = new FileInputStream(gradlePropsFile)) {
                        props.load(fis);
                        
                        // Check for experimental features flag
                        String experimentalFlag = props.getProperty("jakarta.migration.experimental_features");
                        if (experimentalFlag != null) {
                            boolean enabled = "true".equalsIgnoreCase(experimentalFlag.trim());
                            flags.put("experimental_features", enabled);
                            return; // Found it, no need to check other paths
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Use defaults if gradle.properties loading fails
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
     * Checks multiple sources: system property, environment variable, and internal flags.
     */
    public boolean isExperimentalFeaturesEnabled() {
        // First check system property
        String systemProperty = System.getProperty("jakarta.migration.experimental_features");
        if (systemProperty != null) {
            boolean result = "true".equalsIgnoreCase(systemProperty);
            return result;
        }
        
        // Then check environment variable
        String envVar = System.getenv("JAKARTA_MIGRATION_EXPERIMENTAL_FEATURES");
        if (envVar != null) {
            boolean result = "true".equalsIgnoreCase(envVar);
            return result;
        }
        
        // Finally check internal flags (loaded from config files)
        boolean result = flags.getOrDefault("experimental_features", false);
        return result;
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
