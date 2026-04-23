package adrianmikula.jakartamigration.analytics.config;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
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
        Properties props = loadProperties();
        
        this.supabaseUrl = props.getProperty("supabase.url", "");
        this.supabaseAnonKey = props.getProperty("supabase.anon.key", "");
        this.analyticsEnabled = Boolean.parseBoolean(props.getProperty("supabase.analytics.enabled", "true"));
        this.errorReportingEnabled = Boolean.parseBoolean(props.getProperty("supabase.error.reporting.enabled", "true"));
        this.analyticsBatchSize = Integer.parseInt(props.getProperty("supabase.analytics.batch.size", "10"));
        this.analyticsFlushIntervalSeconds = Integer.parseInt(props.getProperty("supabase.analytics.flush.interval.seconds", "30"));
        
        log.info("SupabaseConfig initialized - Analytics: {}, Error Reporting: {}", 
            analyticsEnabled, errorReportingEnabled);
    }
    
    private Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
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
