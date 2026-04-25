package adrianmikula.jakartamigration.storage;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Consolidated storage service for Jakarta Migration plugin.
 * Manages all file operations with standardized paths for project and user data.
 * 
 * Provides two main storage locations:
 * - Project level: projectPath/.jakartamigration/
 * - User home level: ~/.jakartamigration/
 */
@Slf4j
public class PluginStorageService {
    
    private static final String JAKARTA_MIGRATION_DIR = ".jakartamigration";
    private static final String PROJECT_DB_FILE = "jakarta-migration-plugin.db";
    private static final String CREDITS_DB_FILE = "credits-usage.db";
    private static final String USER_PREFERENCES_FILE = "plugin-user-preferences.properties";
    
    private static volatile PluginStorageService instance;
    
    private PluginStorageService() {}
    
    /**
     * Gets the singleton instance of the storage service.
     */
    public static PluginStorageService getInstance() {
        if (instance == null) {
            synchronized (PluginStorageService.class) {
                if (instance == null) {
                    instance = new PluginStorageService();
                }
            }
        }
        return instance;
    }
    
    /**
     * Gets the project storage directory path.
     * Creates the directory if it doesn't exist.
     * 
     * @param projectPath the project root path
     * @return path to .jakartamigration directory in project
     */
    public Path getProjectStoragePath(Path projectPath) {
        Path storageDir = projectPath.resolve(JAKARTA_MIGRATION_DIR);
        try {
            Files.createDirectories(storageDir);
            log.debug("Created/verified project storage directory: {}", storageDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create project storage directory: " + storageDir, e);
        }
        return storageDir;
    }
    
    /**
     * Gets the project database file path.
     * 
     * @param projectPath the project root path
     * @return path to jakarta-migration-plugin.db
     */
    public Path getProjectDatabasePath(Path projectPath) {
        return getProjectStoragePath(projectPath).resolve(PROJECT_DB_FILE);
    }
    
    /**
     * Gets the user home storage directory path.
     * Creates the directory if it doesn't exist.
     * 
     * @return path to ~/.jakartamigration directory
     */
    public Path getUserHomeStoragePath() {
        String userHome = System.getProperty("user.home");
        if (userHome == null) {
            throw new IllegalStateException("User home directory not found");
        }
        
        Path storageDir = Path.of(userHome, JAKARTA_MIGRATION_DIR);
        try {
            Files.createDirectories(storageDir);
            log.debug("Created/verified user home storage directory: {}", storageDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create user home storage directory: " + storageDir, e);
        }
        return storageDir;
    }
    
    /**
     * Gets the main database file path in user home.
     * 
     * @return path to ~/.jakartamigration/jakarta-migration.db
     */
    public Path getUserHomeDatabasePath() {
        return getUserHomeStoragePath().resolve("jakarta-migration.db");
    }
    
    /**
     * Gets the credits database file path in user home.
     * 
     * @return path to credits-usage.db
     */
    public Path getCreditsDatabasePath() {
        return getUserHomeStoragePath().resolve(CREDITS_DB_FILE);
    }
    
    /**
     * Gets the user preferences file path in user home.
     * 
     * @return path to plugin-user-preferences.properties
     */
    public Path getUserPreferencesPath() {
        return getUserHomeStoragePath().resolve(USER_PREFERENCES_FILE);
    }
    
    /**
     * Migrates data from legacy storage locations to new consolidated locations.
     * Detects existing files and moves data appropriately.
     */
    public void migrateExistingData() {
        log.info("Starting migration of existing plugin data to consolidated storage");
        
        try {
            migrateCreditsData();
            migrateAnalyticsData();
            migrateCentralDatabase();
            
            log.info("Data migration completed successfully");
        } catch (Exception e) {
            log.error("Data migration failed", e);
            throw new RuntimeException("Failed to migrate existing plugin data", e);
        }
    }
    
    /**
     * Migrates credits data from legacy locations.
     */
    private void migrateCreditsData() {
        // Check legacy locations for credits.db
        Path[] legacyPaths = getLegacyCreditsPaths();
        
        for (Path legacyPath : legacyPaths) {
            if (Files.exists(legacyPath)) {
                try {
                    Path newPath = getCreditsDatabasePath();
                    log.info("Migrating credits data from {} to {}", legacyPath, newPath);
                    
                    // Copy database file
                    Files.copy(legacyPath, newPath);
                    
                    // Backup old file
                    Path backupPath = legacyPath.resolveSibling(legacyPath.getFileName() + ".backup");
                    Files.move(legacyPath, backupPath);
                    
                    log.info("Credits data migrated successfully, old file backed up to {}", backupPath);
                    break; // Only migrate from first found location
                } catch (IOException e) {
                    log.error("Failed to migrate credits data from " + legacyPath, e);
                }
            }
        }
    }
    
    /**
     * Migrates analytics user ID data from legacy locations.
     */
    private void migrateAnalyticsData() {
        // Check legacy locations for analytics-user-id.properties
        Path[] legacyPaths = getLegacyAnalyticsPaths();
        
        for (Path legacyPath : legacyPaths) {
            if (Files.exists(legacyPath)) {
                try {
                    Path newPrefsPath = getUserPreferencesPath();
                    log.info("Migrating analytics data from {} to {}", legacyPath, newPrefsPath);
                    
                    // Load legacy properties
                    Properties legacyProps = new Properties();
                    try (var is = Files.newInputStream(legacyPath)) {
                        legacyProps.load(is);
                    }
                    
                    // Create new preferences file if it doesn't exist
                    Properties newProps = new Properties();
                    if (Files.exists(newPrefsPath)) {
                        try (var is = Files.newInputStream(newPrefsPath)) {
                            newProps.load(is);
                        }
                    }
                    
                    // Copy analytics-related properties
                    String[] analyticsKeys = {
                        "anonymous.user.id", "first.seen.timestamp", "last.seen.timestamp",
                        "usage.metrics.opt.out", "error.reporting.opt.out"
                    };
                    
                    for (String key : analyticsKeys) {
                        String value = legacyProps.getProperty(key);
                        if (value != null) {
                            newProps.setProperty(key, value);
                        }
                    }
                    
                    // Save new preferences
                    try (var os = Files.newOutputStream(newPrefsPath)) {
                        newProps.store(os, "Jakarta Migration Plugin - User Preferences");
                    }
                    
                    // Backup old file
                    Path backupPath = legacyPath.resolveSibling(legacyPath.getFileName() + ".backup");
                    Files.move(legacyPath, backupPath);
                    
                    log.info("Analytics data migrated successfully, old file backed up to {}", backupPath);
                    break; // Only migrate from first found location
                } catch (IOException e) {
                    log.error("Failed to migrate analytics data from " + legacyPath, e);
                }
            }
        }
    }
    
    /**
     * Migrates central database data if it exists.
     */
    private void migrateCentralDatabase() {
        // Check for central-migration-analysis.db in legacy locations
        Path[] legacyPaths = getLegacyCentralDbPaths();
        
        for (Path legacyPath : legacyPaths) {
            if (Files.exists(legacyPath)) {
                try {
                    log.info("Found legacy central database at {}, marking for manual migration", legacyPath);
                    
                    // For now, just backup the file - manual migration may be needed
                    // since central DB has different schema than project DB
                    Path backupPath = legacyPath.resolveSibling(legacyPath.getFileName() + ".backup");
                    Files.move(legacyPath, backupPath);
                    
                    log.info("Legacy central database backed up to {}. Manual migration may be required.", backupPath);
                    break;
                } catch (IOException e) {
                    log.error("Failed to backup central database from " + legacyPath, e);
                }
            }
        }
    }
    
    /**
     * Gets legacy credits database paths to check for migration.
     */
    private Path[] getLegacyCreditsPaths() {
        String userHome = System.getProperty("user.home");
        String ideaConfigPath = System.getProperty("idea.config.path");
        
        if (ideaConfigPath != null) {
            return new Path[] {
                Path.of(ideaConfigPath, "jakarta-migration", "credits.db"),
                Path.of(userHome, ".jakarta-migration", "credits", "credits.db")
            };
        } else {
            return new Path[] {
                Path.of(userHome, ".jakarta-migration", "credits", "credits.db")
            };
        }
    }
    
    /**
     * Gets legacy analytics file paths to check for migration.
     */
    private Path[] getLegacyAnalyticsPaths() {
        String userHome = System.getProperty("user.home");
        String ideaConfigPath = System.getProperty("idea.config.path");
        
        if (ideaConfigPath != null) {
            return new Path[] {
                Path.of(ideaConfigPath, "jakarta-migration", "analytics-user-id.properties"),
                Path.of(userHome, ".jakarta-migration", "analytics", "analytics-user-id.properties")
            };
        } else {
            return new Path[] {
                Path.of(userHome, ".jakarta-migration", "analytics", "analytics-user-id.properties")
            };
        }
    }
    
    /**
     * Gets legacy central database paths to check for migration.
     */
    private Path[] getLegacyCentralDbPaths() {
        String userHome = System.getProperty("user.home");
        String os = System.getProperty("os.name").toLowerCase();
        Path basePath;
        
        if (os.contains("windows")) {
            String appData = System.getenv("APPDATA");
            basePath = appData != null ? Path.of(appData) : Path.of(userHome, "AppData", "Roaming");
        } else if (os.contains("mac")) {
            basePath = Path.of(userHome, "Library", "Application Support");
        } else {
            basePath = Path.of(userHome, ".config");
        }
        
        return new Path[] {
            basePath.resolve("JakartaMigration").resolve("central-migration-analysis.db")
        };
    }
    
    /**
     * Checks if migration has been completed.
     */
    public boolean isMigrationCompleted() {
        Path migrationMarker = getUserHomeStoragePath().resolve(".migration-completed");
        return Files.exists(migrationMarker);
    }
    
    /**
     * Marks migration as completed.
     */
    public void markMigrationCompleted() {
        try {
            Path migrationMarker = getUserHomeStoragePath().resolve(".migration-completed");
            Files.createFile(migrationMarker);
            log.info("Migration marked as completed");
        } catch (IOException e) {
            log.error("Failed to mark migration as completed", e);
        }
    }
}
