package adrianmikula.jakartamigration.credits;

import adrianmikula.jakartamigration.analytics.service.UsageService;
import adrianmikula.jakartamigration.analytics.service.UserIdentificationService;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing user credits in the freemium model.
 * Simplified to a single credit type for all premium actions.
 * Credits are stored per-user (IDE-wide) using SQLite.
 *
 * For free users:
 * - Limited action credits (default: 10, configurable via freemium.properties)
 *
 * Premium users have unlimited credits.
 */
@Slf4j
public class CreditsService implements AutoCloseable {

    private static final String DB_FILE = "credits.db";
    private static final int DB_VERSION = 2; // Bumped for schema migration

    private final Path dbPath;
    private final ThreadLocal<Connection> connectionHolder = new ThreadLocal<>();
    private final FreemiumConfig freemiumConfig;
    private final UserIdentificationService userIdentificationService;
    private final UsageService usageService;

    // In-memory cache for credits to avoid repeated DB queries
    private final ConcurrentHashMap<CreditType, Integer> creditsCache = new ConcurrentHashMap<>();
    private boolean cacheLoaded = false;

    /**
     * Creates a CreditsService with the default storage location.
     * In IntelliJ, this stores in the IDE's config directory.
     */
    public CreditsService() {
        this(getDefaultStoragePath());
    }

    /**
     * Creates a CreditsService with a custom storage path.
     * Used primarily for testing.
     *
     * @param storagePath the directory to store the credits database
     */
    public CreditsService(Path storagePath) {
        this.freemiumConfig = new FreemiumConfig();
        this.dbPath = storagePath.resolve(DB_FILE);
        
        // Initialize analytics services
        this.userIdentificationService = new UserIdentificationService();
        this.usageService = new UsageService(userIdentificationService);
        
        try {
            Files.createDirectories(storagePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create credits storage directory: " + storagePath, e);
        }
        initializeDatabase();
        log.info("CreditsService initialized at {} with credit limit: {}", dbPath, freemiumConfig.getCreditLimit());
    }

    /**
     * Gets the default storage path for credits.
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
        return Path.of(userHome, ".jakarta-migration", "credits");
    }

    /**
     * Gets the credit limit from FreemiumConfig.
     *
     * @return the credit limit for free users
     */
    public int getCreditLimit() {
        return freemiumConfig.getCreditLimit();
    }

    /**
     * Gets the FreemiumConfig for accessing truncation settings.
     *
     * @return the FreemiumConfig instance
     */
    public FreemiumConfig getFreemiumConfig() {
        return freemiumConfig;
    }

    private Connection getConnection() throws SQLException {
        Connection conn = connectionHolder.get();
        if (conn == null || conn.isClosed()) {
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            conn.setAutoCommit(false);
            connectionHolder.set(conn);
        }
        return conn;
    }

    private void initializeDatabase() {
        try (Connection conn = getConnection()) {
            createTables(conn);
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize credits database", e);
        }
    }

    private void createTables(Connection conn) throws SQLException {
        // Credits table
        String createCreditsTable = """
            CREATE TABLE IF NOT EXISTS credits (
                credit_type TEXT PRIMARY KEY,
                used_count INTEGER NOT NULL DEFAULT 0,
                last_updated INTEGER NOT NULL
            )
            """;

        // Schema version table
        String createVersionTable = """
            CREATE TABLE IF NOT EXISTS schema_version (
                version INTEGER PRIMARY KEY
            )
            """;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createCreditsTable);
            stmt.execute(createVersionTable);
        }

        // Initialize default rows if they don't exist
        for (CreditType type : CreditType.values()) {
            String insertDefault = """
                INSERT OR IGNORE INTO credits (credit_type, used_count, last_updated)
                VALUES (?, 0, ?)
                """;
            try (PreparedStatement pstmt = conn.prepareStatement(insertDefault)) {
                pstmt.setString(1, type.getKey());
                pstmt.setLong(2, System.currentTimeMillis());
                pstmt.executeUpdate();
            }
        }
    }

    /**
     * Gets the number of credits remaining for a specific credit type.
     *
     * @param type the credit type
     * @return the number of remaining credits
     */
    public int getRemainingCredits(CreditType type) {
        loadCacheIfNeeded();
        Integer used = creditsCache.getOrDefault(type, 0);
        int limit = freemiumConfig.getCreditLimit();
        return Math.max(0, limit - used);
    }

    /**
     * Gets the total credit limit for a specific credit type.
     *
     * @param type the credit type
     * @return the total credit limit
     */
    public int getCreditLimit(CreditType type) {
        return freemiumConfig.getCreditLimit();
    }

    /**
     * Gets the number of credits used for a specific credit type.
     *
     * @param type the credit type
     * @return the number of credits used
     */
    public int getUsedCredits(CreditType type) {
        loadCacheIfNeeded();
        return creditsCache.getOrDefault(type, 0);
    }

    /**
     * Checks if there are any credits remaining for a specific type.
     *
     * @param type the credit type
     * @return true if credits are available
     */
    public boolean hasCredits(CreditType type) {
        return getRemainingCredits(type) > 0;
    }

    /**
     * Uses one credit of the specified type.
     * Returns true if credit was successfully used, false if no credits remaining.
     *
     * @param type the credit type to use
     * @return true if credit was used successfully
     */
    public boolean useCredit(CreditType type) {
        int creditsBefore = getRemainingCredits(type);
        log.info("[CREDIT DEBUG] Attempting to use {} credit. Before: {}, Limit: {}",
            type, creditsBefore, freemiumConfig.getCreditLimit());

        if (!hasCredits(type)) {
            log.warn("[CREDIT DEBUG] Failed to use {} credit - none remaining (used: {}, limit: {})",
                type, creditsCache.getOrDefault(type, 0), freemiumConfig.getCreditLimit());
            return false;
        }

        try (Connection conn = getConnection()) {
            String updateSql = """
                UPDATE credits
                SET used_count = used_count + 1, last_updated = ?
                WHERE credit_type = ?
                """;

            try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                pstmt.setLong(1, System.currentTimeMillis());
                pstmt.setString(2, type.getKey());
                int updated = pstmt.executeUpdate();

                if (updated > 0) {
                    conn.commit();
                    // Update cache
                    creditsCache.merge(type, 1, Integer::sum);
                    int creditsAfter = getRemainingCredits(type);
                    log.info("[CREDIT DEBUG] Successfully used 1 {} credit. Before: {}, After: {}, Used: {}",
                        type, creditsBefore, creditsAfter, creditsCache.getOrDefault(type, 0));
                    
                    // Track usage analytics
                    usageService.trackCreditUsage(type.getKey());
                    
                    return true;
                } else {
                    log.error("[CREDIT DEBUG] Database update failed for {} credit - no rows updated", type);
                }
            }
        } catch (SQLException e) {
            log.error("[CREDIT DEBUG] SQLException while using credit for {}: {}", type, e.getMessage(), e);
        }

        return false;
    }

    /**
     * Resets all credits to zero used (for testing or admin purposes).
     * Should not be used in production code.
     */
    public void resetAllCredits() {
        try (Connection conn = getConnection()) {
            String resetSql = """
                UPDATE credits
                SET used_count = 0, last_updated = ?
                """;

            try (PreparedStatement pstmt = conn.prepareStatement(resetSql)) {
                pstmt.setLong(1, System.currentTimeMillis());
                pstmt.executeUpdate();
                conn.commit();
            }

            // Clear cache
            creditsCache.clear();
            cacheLoaded = false;

            log.info("All credits have been reset to zero");
        } catch (SQLException e) {
            log.error("Failed to reset credits", e);
        }
    }

    /**
     * Loads the credits cache from the database if not already loaded.
     */
    private synchronized void loadCacheIfNeeded() {
        if (cacheLoaded) {
            return;
        }

        try (Connection conn = getConnection()) {
            String selectSql = "SELECT credit_type, used_count FROM credits";

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(selectSql)) {
                while (rs.next()) {
                    String typeKey = rs.getString("credit_type");
                    int usedCount = rs.getInt("used_count");

                    CreditType type = CreditType.fromKey(typeKey);
                    if (type != null) {
                        creditsCache.put(type, usedCount);
                    }
                }
            }

            cacheLoaded = true;
            log.debug("Loaded credits cache: {}", creditsCache);
        } catch (SQLException e) {
            log.error("Failed to load credits cache", e);
        }
    }

    /**
     * Refreshes the cache from the database.
     * Useful after external modifications.
     */
    public void refreshCache() {
        cacheLoaded = false;
        loadCacheIfNeeded();
    }

    @Override
    public void close() {
        try {
            Connection conn = connectionHolder.get();
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            log.warn("Error closing credits database connection", e);
        } finally {
            connectionHolder.remove();
        }
        
        // Close analytics services
        try {
            if (usageService != null) {
                usageService.close();
            }
            if (userIdentificationService != null) {
                userIdentificationService.close();
            }
        } catch (Exception e) {
            log.warn("Error closing analytics services", e);
        }
    }
}
