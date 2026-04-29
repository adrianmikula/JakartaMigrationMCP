package adrianmikula.jakartamigration.analytics.config;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Configuration for Supabase analytics and error reporting.
 * Loads configuration from freemium.properties file.
 */
@Slf4j
public class SupabaseConfig {
    
    private static final String CONFIG_FILE = "config/freemium.properties";
    
    private final String supabaseUrl;
    private final String supabaseAnonKey;
    private final boolean analyticsEnabled;
    private final boolean errorReportingEnabled;
    private final int analyticsBatchSize;
    private final int analyticsFlushIntervalSeconds;
    
    public SupabaseConfig() {
        this(loadPropertiesFromClasspath());
    }
    
    /**
     * Constructor for testing with custom Properties.
     * @param props Custom properties for testing
     */
    public SupabaseConfig(Properties props) {
        this.supabaseUrl = props.getProperty("supabase.url", "");
        this.supabaseAnonKey = props.getProperty("supabase.anon.key", "");
        this.analyticsEnabled = parseBoolean(props.getProperty("supabase.analytics.enabled", "true"));
        this.errorReportingEnabled = parseBoolean(props.getProperty("supabase.error.reporting.enabled", "true"));
        this.analyticsBatchSize = parseInteger(props.getProperty("supabase.analytics.batch.size", "10"));
        this.analyticsFlushIntervalSeconds = parseInteger(props.getProperty("supabase.analytics.flush.interval.seconds", "30"));
        
        log.info("SupabaseConfig initialized - Analytics: {}, Error Reporting: {}", 
            analyticsEnabled, errorReportingEnabled);
    }
    
    /**
     * Constructor for testing with custom config file path.
     * @param configPath Path to custom config file
     */
    public SupabaseConfig(Path configPath) {
        this(loadPropertiesFromFile(configPath));
    }
    
    private static Properties loadPropertiesFromClasspath() {
        Properties props = new Properties();
        try (InputStream is = SupabaseConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (is != null) {
                props.load(is);
            } else {
                log.warn("Could not find configuration file: {}", CONFIG_FILE);
            }
        } catch (IOException e) {
            log.error("Error loading Supabase configuration", e);
        }
        return props;
    }
    
    private static Properties loadPropertiesFromFile(Path configPath) {
        Properties props = new Properties();
        try (InputStream is = Files.newInputStream(configPath)) {
            props.load(is);
        } catch (IOException e) {
            log.error("Error loading Supabase configuration from file: {}", configPath, e);
        }
        return props;
    }
    
    private static boolean parseBoolean(String value) {
        try {
            return Boolean.parseBoolean(value);
        } catch (Exception e) {
            return true; // Default to true for invalid values
        }
    }
    
    private static int parseInteger(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 10; // Default value for invalid numbers
        }
    }
    
    public String getSupabaseUrl() {
        return supabaseUrl;
    }
    
    public String getSupabaseAnonKey() {
        return supabaseAnonKey;
    }
    
    public boolean isAnalyticsEnabled() {
        return analyticsEnabled;
    }
    
    public boolean isErrorReportingEnabled() {
        return errorReportingEnabled;
    }
    
    public int getAnalyticsBatchSize() {
        return analyticsBatchSize;
    }
    
    public int getAnalyticsFlushIntervalSeconds() {
        return analyticsFlushIntervalSeconds;
    }
    
    public boolean isConfigured() {
        return !supabaseUrl.isEmpty() && !supabaseAnonKey.isEmpty();
    }
}
