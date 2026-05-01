package adrianmikula.jakartamigration.analytics.service;

import adrianmikula.jakartamigration.analytics.config.SupabaseConfig;
import adrianmikula.jakartamigration.preferences.UserPreferencesService;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;

/**
 * Service for managing anonymous user identification.
 * Generates and stores a UUID for each unique user installation.
 */
@Slf4j
public class UserIdentificationService implements AutoCloseable {
    
    private static final String USER_ID_KEY = "anonymous.user.id";
    private static final String FIRST_SEEN_KEY = "first.seen.timestamp";
    private static final String LAST_SEEN_KEY = "last.seen.timestamp";
    private static final String USAGE_METRICS_OPT_OUT_KEY = "usage.metrics.opt.out";
    private static final String ERROR_REPORTING_OPT_OUT_KEY = "error.reporting.opt.out";
    private static final String USAGE_PERMISSION_REQUESTED_KEY = "usage.permission.requested";
    
    private final UserPreferencesService preferencesService;
    private final String anonymousUserId;
    private final SupabaseConfig supabaseConfig;
    
    public UserIdentificationService() {
        this.preferencesService = new UserPreferencesService();
        this.supabaseConfig = new SupabaseConfig();
        this.anonymousUserId = loadOrCreateUserId();
        updateLastSeen();
        
        log.info("UserIdentificationService initialized for user: {}", anonymousUserId);
    }
    
    public UserIdentificationService(Path storagePath, SupabaseConfig supabaseConfig) {
        this.preferencesService = storagePath != null ? new UserPreferencesService(storagePath) : new UserPreferencesService();
        this.supabaseConfig = supabaseConfig;
        this.anonymousUserId = loadOrCreateUserId();
        updateLastSeen();
    }

    /**
     * Loads existing user ID or creates a new one.
     */
    private String loadOrCreateUserId() {
        String userId = preferencesService.getPreference(USER_ID_KEY);
        if (userId == null || userId.trim().isEmpty()) {
            userId = UUID.randomUUID().toString();
            preferencesService.setPreference(USER_ID_KEY, userId);
            preferencesService.setPreference(FIRST_SEEN_KEY, String.valueOf(System.currentTimeMillis()));
            preferencesService.setPreference(LAST_SEEN_KEY, String.valueOf(System.currentTimeMillis()));
            log.info("Created new anonymous user ID: {}", userId);
        } else {
            log.debug("Loaded existing anonymous user ID: {}", userId);
        }
        
        return userId;
    }
    
    /**
     * Updates the last seen timestamp for user.
     */
    private void updateLastSeen() {
        preferencesService.setPreference(LAST_SEEN_KEY, String.valueOf(System.currentTimeMillis()));
    }
    
    /**
     * Gets the last seen timestamp.
     */
    public long getLastSeenTimestamp() {
        return preferencesService.getLastSeenTimestamp();
    }
    
    /**
     * Gets the first seen timestamp.
     */
    public long getFirstSeenTimestamp() {
        return preferencesService.getFirstSeenTimestamp();
    }
    
    /**
     * Gets the anonymous user ID.
     */
    public String getAnonymousUserId() {
        return anonymousUserId;
    }
    
    /**
     * Checks if analytics is properly configured and enabled.
     */
    public boolean isAnalyticsEnabled() {
        return supabaseConfig.isAnalyticsEnabled() && supabaseConfig.isConfigured() && !isUsageMetricsOptedOut();
    }
    
    /**
     * Checks if error reporting is properly configured and enabled.
     */
    public boolean isErrorReportingEnabled() {
        return supabaseConfig.isErrorReportingEnabled() && supabaseConfig.isConfigured() && !isErrorReportingOptedOut();
    }
    
    /**
     * Checks if user has opted out of usage metrics.
     */
    public boolean isUsageMetricsOptedOut() {
        return Boolean.parseBoolean(preferencesService.getPreference(USAGE_METRICS_OPT_OUT_KEY, "false"));
    }
    
    /**
     * Checks if error reporting is opted out.
     */
    public boolean isErrorReportingOptedOut() {
        return Boolean.parseBoolean(preferencesService.getPreference(ERROR_REPORTING_OPT_OUT_KEY, "false"));
    }
    
    /**
     * Sets the user's preference for usage metrics.
     */
    public void setUsageMetricsEnabled(boolean enabled) {
        preferencesService.setPreference(USAGE_METRICS_OPT_OUT_KEY, String.valueOf(!enabled));
        log.info("Usage metrics enabled: {}", enabled);
    }
    
    /**
     * Sets the user's preference for error reporting.
     */
    public void setErrorReportingEnabled(boolean enabled) {
        preferencesService.setPreference(ERROR_REPORTING_OPT_OUT_KEY, String.valueOf(!enabled));
        log.info("Error reporting enabled: {}", enabled);
    }
    
    /**
     * Checks if usage permission has been requested.
     */
    public boolean isUsagePermissionRequested() {
        return Boolean.parseBoolean(preferencesService.getPreference(USAGE_PERMISSION_REQUESTED_KEY, "false"));
    }
    
    /**
     * Marks that usage permission has been requested.
     */
    public void setUsagePermissionRequested() {
        preferencesService.setPreference(USAGE_PERMISSION_REQUESTED_KEY, "true");
        log.info("Usage permission requested flag set");
    }
    
    @Override
    public void close() {
        // Update last seen timestamp when closing
        updateLastSeen();
        log.debug("UserIdentificationService closed");
    }
}
