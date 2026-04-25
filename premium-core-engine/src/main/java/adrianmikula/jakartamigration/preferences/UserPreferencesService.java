package adrianmikula.jakartamigration.preferences;

import adrianmikula.jakartamigration.storage.PluginStorageService;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing unified user preferences across the Jakarta Migration plugin.
 * Consolidates all user settings and opt-outs into a single properties file
 * stored in the user's home directory for persistence across projects.
 */
@Slf4j
public class UserPreferencesService implements AutoCloseable {
    
    private static final String ANALYTICS_USER_ID_KEY = "anonymous.user.id";
    private static final String FIRST_SEEN_KEY = "first.seen.timestamp";
    private static final String LAST_SEEN_KEY = "last.seen.timestamp";
    private static final String USAGE_METRICS_OPT_OUT_KEY = "usage.metrics.opt.out";
    private static final String ERROR_REPORTING_OPT_OUT_KEY = "error.reporting.opt.out";
    
    private final Path preferencesPath;
    private final ConcurrentHashMap<String, String> preferencesCache = new ConcurrentHashMap<>();
    private boolean cacheLoaded = false;
    
    public UserPreferencesService() {
        this.preferencesPath = PluginStorageService.getInstance().getUserPreferencesPath();
        log.info("UserPreferencesService initialized at {}", preferencesPath);
    }
    
    /**
     * Gets the anonymous user ID.
     */
    public String getAnonymousUserId() {
        return getPreference(ANALYTICS_USER_ID_KEY);
    }
    
    /**
     * Sets the anonymous user ID.
     */
    public void setAnonymousUserId(String userId) {
        setPreference(ANALYTICS_USER_ID_KEY, userId);
    }
    
    /**
     * Gets the first seen timestamp.
     */
    public long getFirstSeenTimestamp() {
        String value = getPreference(FIRST_SEEN_KEY);
        return value != null ? Long.parseLong(value) : System.currentTimeMillis();
    }
    
    /**
     * Sets the first seen timestamp.
     */
    public void setFirstSeenTimestamp(long timestamp) {
        setPreference(FIRST_SEEN_KEY, String.valueOf(timestamp));
    }
    
    /**
     * Gets the last seen timestamp.
     */
    public long getLastSeenTimestamp() {
        String value = getPreference(LAST_SEEN_KEY);
        return value != null ? Long.parseLong(value) : System.currentTimeMillis();
    }
    
    /**
     * Sets the last seen timestamp.
     */
    public void setLastSeenTimestamp(long timestamp) {
        setPreference(LAST_SEEN_KEY, String.valueOf(timestamp));
    }
    
    /**
     * Checks if usage metrics are opted out.
     */
    public boolean isUsageMetricsOptedOut() {
        return Boolean.parseBoolean(getPreference(USAGE_METRICS_OPT_OUT_KEY, "false"));
    }
    
    /**
     * Sets usage metrics opt-out preference.
     */
    public void setUsageMetricsOptedOut(boolean optedOut) {
        setPreference(USAGE_METRICS_OPT_OUT_KEY, String.valueOf(optedOut));
        log.info("Usage metrics opt-out set to: {}", optedOut);
    }
    
    /**
     * Checks if error reporting is opted out.
     */
    public boolean isErrorReportingOptedOut() {
        return Boolean.parseBoolean(getPreference(ERROR_REPORTING_OPT_OUT_KEY, "false"));
    }
    
    /**
     * Sets error reporting opt-out preference.
     */
    public void setErrorReportingOptedOut(boolean optedOut) {
        setPreference(ERROR_REPORTING_OPT_OUT_KEY, String.valueOf(optedOut));
        log.info("Error reporting opt-out set to: {}", optedOut);
    }
    
    /**
     * Gets a preference value by key.
     */
    public String getPreference(String key) {
        loadCacheIfNeeded();
        return preferencesCache.get(key);
    }
    
    /**
     * Gets a preference value by key with default.
     */
    public String getPreference(String key, String defaultValue) {
        loadCacheIfNeeded();
        String value = preferencesCache.get(key);
        return value != null ? value : defaultValue;
    }
    
    /**
     * Sets a preference value by key.
     */
    public void setPreference(String key, String value) {
        loadCacheIfNeeded();
        preferencesCache.put(key, value);
        savePreferences();
    }
    
    /**
     * Removes a preference by key.
     */
    public void removePreference(String key) {
        loadCacheIfNeeded();
        preferencesCache.remove(key);
        savePreferences();
    }
    
    /**
     * Loads preferences from disk if not already loaded.
     */
    private void loadCacheIfNeeded() {
        if (cacheLoaded) {
            return;
        }
        
        Properties props = loadPropertiesFromFile();
        for (String key : props.stringPropertyNames()) {
            preferencesCache.put(key, props.getProperty(key));
        }
        cacheLoaded = true;
        log.debug("Loaded {} user preferences", preferencesCache.size());
    }
    
    /**
     * Loads preferences from disk.
     */
    private Properties loadPropertiesFromFile() {
        Properties props = new Properties();
        if (Files.exists(preferencesPath)) {
            try (var is = Files.newInputStream(preferencesPath)) {
                props.load(is);
                log.debug("Loaded preferences from {}", preferencesPath);
            } catch (IOException e) {
                log.warn("Error loading preferences, creating new ones", e);
            }
        }
        return props;
    }
    
    /**
     * Saves preferences to disk.
     */
    private void savePreferences() {
        Properties props = new Properties();
        preferencesCache.forEach(props::setProperty);
        
        try (var os = Files.newOutputStream(preferencesPath)) {
            props.store(os, "Jakarta Migration Plugin - User Preferences");
            log.debug("Saved {} preferences to {}", preferencesCache.size(), preferencesPath);
        } catch (IOException e) {
            log.error("Failed to save preferences", e);
        }
    }
    
    /**
     * Refreshes preferences from disk.
     */
    public void refreshPreferences() {
        cacheLoaded = false;
        loadCacheIfNeeded();
    }
    
    @Override
    public void close() {
        savePreferences();
        log.debug("UserPreferencesService closed");
    }
}
