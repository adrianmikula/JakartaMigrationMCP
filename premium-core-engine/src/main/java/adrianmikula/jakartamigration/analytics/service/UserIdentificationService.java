package adrianmikula.jakartamigration.analytics.service;

import adrianmikula.jakartamigration.analytics.config.SupabaseConfig;
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
    
    private static final String USER_ID_FILE = "analytics-user-id.properties";
    private static final String USER_ID_KEY = "anonymous.user.id";
    private static final String FIRST_SEEN_KEY = "first.seen.timestamp";
    private static final String LAST_SEEN_KEY = "last.seen.timestamp";
    
    private final Path userIdPath;
    private final String anonymousUserId;
    private final SupabaseConfig supabaseConfig;
    
    public UserIdentificationService() {
        this(getDefaultStoragePath(), new SupabaseConfig());
    }
    
    public UserIdentificationService(Path storagePath, SupabaseConfig supabaseConfig) {
        this.supabaseConfig = supabaseConfig;
        this.userIdPath = storagePath.resolve(USER_ID_FILE);
        
        try {
            Files.createDirectories(storagePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create analytics storage directory: " + storagePath, e);
        }
        
        this.anonymousUserId = loadOrCreateUserId();
        updateLastSeen();
        
        log.info("UserIdentificationService initialized for user: {}", anonymousUserId);
    }
    
    /**
     * Gets the default storage path for user identification.
     * Uses the IntelliJ system directory or falls back to user home.
     */
    private static Path getDefaultStoragePath() {
        // Try to use IntelliJ's config directory
        String ideaConfigPath = System.getProperty("idea.config.path");
        if (ideaConfigPath != null) {
            return Path.of(ideaConfigPath, "jakarta-migration");
        }
        
        // Fallback to user home
        String userHome = System.getProperty("user.home");
        return Path.of(userHome, ".jakarta-migration", "analytics");
    }
    
    /**
     * Loads existing user ID or creates a new one.
     */
    private String loadOrCreateUserId() {
        Properties props = loadUserProperties();
        
        String userId = props.getProperty(USER_ID_KEY);
        if (userId == null || userId.trim().isEmpty()) {
            userId = UUID.randomUUID().toString();
            props.setProperty(USER_ID_KEY, userId);
            props.setProperty(FIRST_SEEN_KEY, String.valueOf(System.currentTimeMillis()));
            props.setProperty(LAST_SEEN_KEY, String.valueOf(System.currentTimeMillis()));
            saveUserProperties(props);
            log.info("Created new anonymous user ID: {}", userId);
        } else {
            log.debug("Loaded existing anonymous user ID: {}", userId);
        }
        
        return userId;
    }
    
    /**
     * Updates the last seen timestamp for the user.
     */
    private void updateLastSeen() {
        Properties props = loadUserProperties();
        props.setProperty(LAST_SEEN_KEY, String.valueOf(System.currentTimeMillis()));
        saveUserProperties(props);
    }
    
    /**
     * Loads user properties from disk.
     */
    private Properties loadUserProperties() {
        Properties props = new Properties();
        if (Files.exists(userIdPath)) {
            try {
                props.load(Files.newInputStream(userIdPath));
            } catch (IOException e) {
                log.warn("Error loading user ID properties, creating new ones", e);
            }
        }
        return props;
    }
    
    /**
     * Saves user properties to disk.
     */
    private void saveUserProperties(Properties props) {
        try {
            props.store(Files.newOutputStream(userIdPath), 
                "Jakarta Migration Analytics - Anonymous User Identification");
        } catch (IOException e) {
            log.error("Failed to save user ID properties", e);
        }
    }
    
    /**
     * Gets the anonymous user ID.
     */
    public String getAnonymousUserId() {
        return anonymousUserId;
    }
    
    /**
     * Gets the first seen timestamp.
     */
    public long getFirstSeenTimestamp() {
        Properties props = loadUserProperties();
        String firstSeen = props.getProperty(FIRST_SEEN_KEY);
        return firstSeen != null ? Long.parseLong(firstSeen) : System.currentTimeMillis();
    }
    
    /**
     * Gets the last seen timestamp.
     */
    public long getLastSeenTimestamp() {
        Properties props = loadUserProperties();
        String lastSeen = props.getProperty(LAST_SEEN_KEY);
        return lastSeen != null ? Long.parseLong(lastSeen) : System.currentTimeMillis();
    }
    
    /**
     * Checks if analytics is properly configured and enabled.
     */
    public boolean isAnalyticsEnabled() {
        return supabaseConfig.isAnalyticsEnabled() && supabaseConfig.isConfigured();
    }
    
    /**
     * Checks if error reporting is properly configured and enabled.
     */
    public boolean isErrorReportingEnabled() {
        return supabaseConfig.isErrorReportingEnabled() && supabaseConfig.isConfigured();
    }
    
    @Override
    public void close() {
        // Update last seen timestamp when closing
        updateLastSeen();
        log.debug("UserIdentificationService closed");
    }
}
