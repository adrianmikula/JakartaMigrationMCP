package adrianmikula.jakartamigration.credits;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration for the freemium model.
 * Loads credit limits and truncation settings from properties file.
 */
@Slf4j
public class FreemiumConfig {

    private static final String CONFIG_FILE = "config/freemium.properties";

    // Default values
    private static final int DEFAULT_CREDIT_LIMIT = 10;
    private static final int DEFAULT_TRUNCATION_LIMIT = 10;
    private static final boolean DEFAULT_TRUNCATION_ENABLED = true;

    private final Properties properties;

    /**
     * Creates a FreemiumConfig loading from the default properties file.
     */
    public FreemiumConfig() {
        this.properties = loadProperties();
    }

    /**
     * Loads properties from the configuration file.
     * Falls back to defaults if file cannot be loaded.
     */
    private Properties loadProperties() {
        Properties props = new Properties();

        // Set defaults first
        props.setProperty("freemium.credits.actions.limit", String.valueOf(DEFAULT_CREDIT_LIMIT));
        props.setProperty("freemium.truncation.enabled", String.valueOf(DEFAULT_TRUNCATION_ENABLED));
        props.setProperty("freemium.truncation.dashboard.limit", String.valueOf(DEFAULT_TRUNCATION_LIMIT));
        props.setProperty("freemium.truncation.dependencies.limit", String.valueOf(DEFAULT_TRUNCATION_LIMIT));
        props.setProperty("freemium.truncation.advanced_scan.limit", String.valueOf(DEFAULT_TRUNCATION_LIMIT));

        // Try to load from configuration file
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (is != null) {
                props.load(is);
                log.info("Loaded freemium configuration from {}", CONFIG_FILE);
            } else {
                log.warn("Freemium configuration file not found: {}. Using defaults.", CONFIG_FILE);
            }
        } catch (IOException e) {
            log.warn("Failed to load freemium configuration: {}. Using defaults.", e.getMessage());
        }

        // Allow system property overrides for testing
        overrideFromSystemProperties(props);

        return props;
    }

    /**
     * Overrides properties from system properties for testing.
     */
    private void overrideFromSystemProperties(Properties props) {
        String[] keys = {
            "freemium.credits.actions.limit",
            "freemium.truncation.enabled",
            "freemium.truncation.dashboard.limit",
            "freemium.truncation.dependencies.limit",
            "freemium.truncation.advanced_scan.limit"
        };

        for (String key : keys) {
            String systemValue = System.getProperty(key);
            if (systemValue != null) {
                props.setProperty(key, systemValue);
                log.info("Overriding {} from system property: {}", key, systemValue);
            }
        }
    }

    /**
     * Gets the credit limit for actions.
     *
     * @return the maximum number of actions for free users
     */
    public int getCreditLimit() {
        return getIntProperty("freemium.credits.actions.limit", DEFAULT_CREDIT_LIMIT);
    }

    /**
     * Checks if truncation is enabled.
     *
     * @return true if truncation mode is enabled
     */
    public boolean isTruncationEnabled() {
        return getBooleanProperty("freemium.truncation.enabled", DEFAULT_TRUNCATION_ENABLED);
    }

    /**
     * Gets the truncation limit for a specific tab.
     *
     * @param tabName the name of the tab (dashboard, dependencies, advanced_scan)
     * @return the truncation limit for that tab
     */
    public int getTruncationLimit(String tabName) {
        String key = "freemium.truncation." + tabName + ".limit";
        return getIntProperty(key, DEFAULT_TRUNCATION_LIMIT);
    }

    /**
     * Gets the truncation limit for the Dashboard tab.
     *
     * @return the truncation limit
     */
    public int getDashboardTruncationLimit() {
        return getTruncationLimit("dashboard");
    }

    /**
     * Gets the truncation limit for the Dependencies tab.
     *
     * @return the truncation limit
     */
    public int getDependenciesTruncationLimit() {
        return getTruncationLimit("dependencies");
    }

    /**
     * Gets the truncation limit for the Advanced Scans tab (per sub-tab).
     *
     * @return the truncation limit per sub-tab
     */
    public int getAdvancedScanTruncationLimit() {
        return getTruncationLimit("advanced_scan");
    }

    /**
     * Helper method to get an integer property.
     */
    private int getIntProperty(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                log.warn("Invalid integer value for {}: {}. Using default: {}", key, value, defaultValue);
            }
        }
        return defaultValue;
    }

    /**
     * Helper method to get a boolean property.
     */
    private boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            return Boolean.parseBoolean(value.trim());
        }
        return defaultValue;
    }

    /**
     * Reloads the configuration from the properties file.
     * Useful for testing or when configuration changes.
     */
    public void reload() {
        Properties newProps = loadProperties();
        this.properties.clear();
        this.properties.putAll(newProps);
        log.info("Freemium configuration reloaded");
    }
}
