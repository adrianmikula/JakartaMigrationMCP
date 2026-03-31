package adrianmikula.jakartamigration.intellij.config;

import com.intellij.openapi.diagnostic.Logger;

import java.awt.GraphicsEnvironment;
import java.util.Properties;

/**
 * Failsafe configuration for licensing system.
 * 
 * This class provides configuration options and system properties
 * that can be used to prevent IDE lockup due to licensing issues.
 * 
 * Available system properties:
 * - jakarta.migration.dev=true - Development mode (always licensed)
 * - jakarta.migration.safe=true - Safe mode (fallback behavior)
 * - jakarta.migration.license.timeout=5000 - License check timeout in ms
 * - jakarta.migration.license.async=true - Force async license checks
 */
public class LicenseFailsafeConfig {
    private static final Logger LOG = Logger.getInstance(LicenseFailsafeConfig.class);
    
    // System property keys
    public static final String DEV_MODE_PROPERTY = "jakarta.migration.dev";
    public static final String SAFE_MODE_PROPERTY = "jakarta.migration.safe";
    public static final String LICENSE_TIMEOUT_PROPERTY = "jakarta.migration.license.timeout";
    public static final String ASYNC_LICENSE_PROPERTY = "jakarta.migration.license.async";
    public static final String DISABLE_LICENSE_PROPERTY = "jakarta.migration.license.disable";
    public static final String FORCE_TRIAL_PROPERTY = "jakarta.migration.force.trial";
    
    // Default values
    public static final long DEFAULT_LICENSE_TIMEOUT_MS = 5000;
    public static final boolean DEFAULT_ASYNC_LICENSE = true;
    
    private static final Properties config = new Properties();
    
    static {
        loadConfiguration();
    }
    
    private static void loadConfiguration() {
        // Load from failsafe.properties if available
        try {
            var inputStream = LicenseFailsafeConfig.class.getClassLoader()
                    .getResourceAsStream("failsafe-config.properties");
            if (inputStream != null) {
                config.load(inputStream);
                LOG.info("LicenseFailsafeConfig: Loaded failsafe configuration");
            }
        } catch (Exception e) {
            LOG.warn("LicenseFailsafeConfig: Could not load failsafe configuration", e);
        }
        
        // Log current failsafe status
        logFailsafeStatus();
    }
    
    /**
     * Check if development mode is enabled.
     * In dev mode, all license checks are bypassed and the plugin is always licensed.
     */
    public static boolean isDevMode() {
        return Boolean.getBoolean(DEV_MODE_PROPERTY) || 
               "dev".equals(System.getProperty("jakarta.migration.mode")) ||
               config.containsKey(DEV_MODE_PROPERTY);
    }
    
    /**
     * Check if safe mode is enabled.
     * In safe mode, the plugin uses fallback behavior and avoids any potentially
     * blocking operations.
     */
    public static boolean isSafeMode() {
        return Boolean.getBoolean(SAFE_MODE_PROPERTY) || 
               config.containsKey(SAFE_MODE_PROPERTY);
    }
    
    /**
     * Check if license checks are completely disabled.
     * This is the most aggressive failsafe option.
     */
    public static boolean isLicenseDisabled() {
        return Boolean.getBoolean(DISABLE_LICENSE_PROPERTY) || 
               config.containsKey(DISABLE_LICENSE_PROPERTY);
    }
    
    /**
     * Get license check timeout in milliseconds.
     */
    public static long getLicenseTimeoutMs() {
        String timeoutProp = System.getProperty(LICENSE_TIMEOUT_PROPERTY);
        if (timeoutProp != null) {
            try {
                return Long.parseLong(timeoutProp);
            } catch (NumberFormatException e) {
                LOG.warn("LicenseFailsafeConfig: Invalid timeout value: " + timeoutProp);
            }
        }
        
        String configTimeout = config.getProperty(LICENSE_TIMEOUT_PROPERTY);
        if (configTimeout != null) {
            try {
                return Long.parseLong(configTimeout);
            } catch (NumberFormatException e) {
                LOG.warn("LicenseFailsafeConfig: Invalid config timeout: " + configTimeout);
            }
        }
        
        return DEFAULT_LICENSE_TIMEOUT_MS;
    }
    
    /**
     * Check if async license checks are forced.
     */
    public static boolean isAsyncLicenseForced() {
        return Boolean.getBoolean(ASYNC_LICENSE_PROPERTY) || 
               Boolean.parseBoolean(config.getProperty(ASYNC_LICENSE_PROPERTY)) ||
               DEFAULT_ASYNC_LICENSE;
    }
    
    /**
     * Check if trial mode is forced.
     */
    public static boolean isTrialForced() {
        return Boolean.getBoolean(FORCE_TRIAL_PROPERTY) || 
               config.containsKey(FORCE_TRIAL_PROPERTY);
    }
    
    /**
     * Get failsafe configuration summary.
     */
    public static String getFailsafeSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("License Failsafe Configuration:\n");
        sb.append("  Dev Mode: ").append(isDevMode()).append("\n");
        sb.append("  Safe Mode: ").append(isSafeMode()).append("\n");
        sb.append("  License Disabled: ").append(isLicenseDisabled()).append("\n");
        sb.append("  Timeout: ").append(getLicenseTimeoutMs()).append("ms\n");
        sb.append("  Async Forced: ").append(isAsyncLicenseForced()).append("\n");
        sb.append("  Trial Forced: ").append(isTrialForced()).append("\n");
        return sb.toString();
    }
    
    /**
     * Apply failsafe configuration to system properties.
     * This can be called during plugin initialization to ensure failsafe settings.
     */
    public static void applyFailsafeSettings() {
        if (isDevMode()) {
            System.setProperty(DEV_MODE_PROPERTY, "true");
            LOG.info("LicenseFailsafeConfig: Applied dev mode setting");
        }
        
        if (isSafeMode()) {
            System.setProperty(SAFE_MODE_PROPERTY, "true");
            LOG.info("LicenseFailsafeConfig: Applied safe mode setting");
        }
        
        if (isLicenseDisabled()) {
            System.setProperty(DISABLE_LICENSE_PROPERTY, "true");
            LOG.info("LicenseFailsafeConfig: Applied license disabled setting");
        }
        
        if (isTrialForced()) {
            System.setProperty(FORCE_TRIAL_PROPERTY, "true");
            System.setProperty("jakarta.migration.premium", "true");
            LOG.info("LicenseFailsafeConfig: Applied forced trial setting");
        }
        
        long timeout = getLicenseTimeoutMs();
        System.setProperty(LICENSE_TIMEOUT_PROPERTY, String.valueOf(timeout));
        
        boolean async = isAsyncLicenseForced();
        System.setProperty(ASYNC_LICENSE_PROPERTY, String.valueOf(async));
        
        LOG.info("LicenseFailsafeConfig: Failsafe settings applied");
    }
    
    /**
     * Check if the current environment suggests failsafe mode should be enabled.
     * This is based on heuristics like CI environments, headless mode, etc.
     */
    public static boolean shouldEnableFailsafe() {
        // CI/CD environment indicators
        boolean isCI = System.getenv("CI") != null || 
                     System.getenv("CONTINUOUS_INTEGRATION") != null ||
                     System.getenv("JENKINS_URL") != null ||
                     System.getenv("GITHUB_ACTIONS") != null ||
                     System.getenv("GITLAB_CI") != null;
        
        // Headless environment
        boolean isHeadless = GraphicsEnvironment.isHeadless() ||
                            System.getProperty("java.awt.headless", "false").equals("true");
        
        // Test environment
        boolean isTest = System.getProperty("java.class.path").contains("test") ||
                        Thread.currentThread().getStackTrace().length > 50; // Heuristic for test depth
        
        // Debug mode
        boolean isDebug = System.getProperty("java.debug", "false").equals("true") ||
                         System.getProperty("ide.debug.mode", "false").equals("true");
        
        return isCI || isHeadless || isTest || isDebug;
    }
    
    private static void logFailsafeStatus() {
        if (isDevMode() || isSafeMode() || isLicenseDisabled()) {
            LOG.info("LicenseFailsafeConfig: Failsafe mode detected\n" + getFailsafeSummary());
        }
        
        if (shouldEnableFailsafe()) {
            LOG.info("LicenseFailsafeConfig: Environment suggests failsafe mode should be enabled");
        }
    }
    
    /**
     * Create a failsafe properties file with recommended settings.
     * This can be used by users to configure failsafe behavior.
     */
    public static String getRecommendedFailsafeProperties() {
        return """
            # Jakarta Migration Plugin - License Failsafe Configuration
            # 
            # Use this file to prevent IDE lockup due to licensing issues.
            # Copy this content to failsafe-config.properties in your plugin resources.
            
            # Development mode - always licensed, useful for development
            # jakarta.migration.dev=true
            
            # Safe mode - fallback behavior, avoids blocking operations
            # jakarta.migration.safe=true
            
            # License timeout in milliseconds (default: 5000)
            jakarta.migration.license.timeout=3000
            
            # Force async license checks (default: true)
            jakarta.migration.license.async=true
            
            # Completely disable license checks (most aggressive failsafe)
            # jakarta.migration.license.disable=true
            
            # Force trial mode
            # jakarta.migration.force.trial=true
            """;
    }
}
