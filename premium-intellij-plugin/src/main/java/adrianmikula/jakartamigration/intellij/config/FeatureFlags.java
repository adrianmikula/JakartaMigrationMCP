package adrianmikula.jakartamigration.intellij.config;

import com.intellij.openapi.diagnostic.Logger;
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
    
    private static final Logger LOG = Logger.getInstance(FeatureFlags.class);
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
        System.out.println("DEBUG: FeatureFlags constructor called");
        loadDefaults();
        System.out.println("DEBUG: About to call loadFromConfig");
        loadFromConfig();
        System.out.println("DEBUG: FeatureFlags initialization complete");
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
        flags.put("platformsTab", false); // Add platforms tab flag
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
        
        // Platforms tab configuration
        FeatureConfig platformsConfig = new FeatureConfig();
        platformsConfig.enabled = false;
        platformsConfig.name = "Platforms Detection";
        platformsConfig.description = "Detect application servers and Jakarta EE compatibility";
        platformsConfig.beta = false;
        featureConfigs.put("platformsTab", platformsConfig);
    }
    
    private void loadFromConfig() {
        System.err.println("DEBUG: loadFromConfig called");
        
        // First try to load from gradle.properties
        System.err.println("DEBUG: About to call loadFromGradleProperties");
        loadFromGradleProperties();
        
        // Then try to load from feature-flags.yaml
        InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("feature-flags.yaml");
        
        if (inputStream == null) {
            // Try loading from working directory
            File configFile = new File("config/feature-flags.yaml");
            if (configFile.exists()) {
                try {
                    inputStream = new FileInputStream(configFile);
                } catch (FileNotFoundException e) {
                    System.out.println("DEBUG: feature-flags.yaml found in resources");
                }
            } else {
                System.out.println("DEBUG: feature-flags.yaml not found, skipping");
            }
        } else {
            System.out.println("DEBUG: feature-flags.yaml found in resources");
        }
        
        if (inputStream != null) {
            try {
                Yaml yaml = new Yaml();
                Map<String, Object> config = yaml.load(inputStream);
                
                if (config != null && config.containsKey("features")) {
                    Map<String, Object> features = (Map<String, Object>) config.get("features");
                    for (Map.Entry<String, Object> entry : features.entrySet()) {
                        Map<String, Object> feature = (Map<String, Object>) entry.getValue();
                        Boolean enabled = (Boolean) feature.get("enabled");
                        if (enabled != null) {
                            flags.put(entry.getKey(), enabled);
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("DEBUG: Error loading feature-flags.yaml: " + e.getMessage());
            } finally {
                try {
                    inputStream.close();
                } catch (java.io.IOException e) {
                    // Ignore
                }
            }
        }
        
        System.err.println("DEBUG: loadFromConfig complete");
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
                System.out.println("DEBUG: Looking for gradle.properties at: " + gradlePropsFile.getAbsolutePath());
                System.out.println("DEBUG: gradle.properties exists: " + gradlePropsFile.exists());
                
                if (gradlePropsFile.exists()) {
                    Properties props = new Properties();
                    try (FileInputStream fis = new FileInputStream(gradlePropsFile)) {
                        props.load(fis);
                        
                        // Check for experimental features flag
                        String experimentalFlag = props.getProperty("jakarta.migration.experimental_features");
                        System.out.println("DEBUG: Found experimental flag in gradle.properties: " + experimentalFlag);
                        if (experimentalFlag != null) {
                            boolean enabled = "true".equalsIgnoreCase(experimentalFlag.trim());
                            flags.put("experimental_features", enabled);
                            System.out.println("DEBUG: Set experimental_features flag to: " + enabled);
                            return; // Found it, no need to check other paths
                        }
                    }
                }
            }
            
            System.out.println("DEBUG: No gradle.properties found with experimental flag");
        } catch (Exception e) {
            System.out.println("DEBUG: Error loading gradle.properties: " + e.getMessage());
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
     * Checks if platforms tab is enabled.
     */
    public boolean isPlatformsEnabled() {
        boolean enabled = flags.getOrDefault("platformsTab", false);
        System.out.println("DEBUG: FeatureFlags.isPlatformsEnabled() returning: " + enabled);
        return enabled;
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
            System.out.println("DEBUG: Experimental features from system property: " + result);
            return result;
        }
        
        // Then check environment variable
        String envVar = System.getenv("JAKARTA_MIGRATION_EXPERIMENTAL_FEATURES");
        if (envVar != null) {
            boolean result = "true".equalsIgnoreCase(envVar);
            System.out.println("DEBUG: Experimental features from environment variable: " + result);
            return result;
        }
        
        // Finally check internal flags (loaded from config files)
        boolean result = flags.getOrDefault("experimental_features", false);
        System.out.println("DEBUG: Experimental features from internal flags: " + result);
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
     * Enables or disables platforms tab features.
     */
    public void setPlatformsEnabled(boolean enabled) {
        System.out.println("DEBUG: FeatureFlags.setPlatformsEnabled() called with: " + enabled);
        flags.put("platformsTab", enabled);
        LOG.info("Platforms tab feature enabled: " + enabled);
        System.out.println("DEBUG: FeatureFlags.setPlatformsEnabled() - flags now contains: " + flags.get("platformsTab"));
    }
    
    /**
     * Enables or disables experimental features.
     */
    public void setExperimentalFeaturesEnabled(boolean enabled) {
        System.out.println("DEBUG: FeatureFlags.setExperimentalFeaturesEnabled() called with: " + enabled);
        flags.put("experimental_features", enabled);
        LOG.info("Experimental features enabled: " + enabled);
        System.out.println("DEBUG: FeatureFlags.setExperimentalFeaturesEnabled() - flags now contains: " + flags.get("experimental_features"));
    }
    
    /**
     * Enables or disables advanced scans features.
     */
    public void setAdvancedScansEnabled(boolean enabled) {
        flags.put("advancedScans", enabled);
        LOG.info("Advanced scans feature enabled: " + enabled);
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
