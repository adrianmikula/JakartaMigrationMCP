package adrianmikula.jakartamigration.intellij.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility to configure debug logging only in development mode
 */
public class DevModeLogger {
    
    private static final boolean DEV_MODE = isDevMode();
    private static final Logger log = LoggerFactory.getLogger(DevModeLogger.class);
    
    /**
     * Check if running in development mode
     */
    private static boolean isDevMode() {
        // Check for dev mode indicators
        String devMode = System.getProperty("jakarta.migration.dev.mode");
        if (devMode != null) {
            return Boolean.parseBoolean(devMode);
        }
        
        // Check if running via gradle runIdeDev
        String javaCommand = System.getProperty("sun.java.command");
        if (javaCommand != null && javaCommand.contains("runIdeDev")) {
            return true;
        }
        
        // Check for environment variable
        String envDevMode = System.getenv("JAKARTA_MIGRATION_DEV_MODE");
        if (envDevMode != null) {
            return Boolean.parseBoolean(envDevMode);
        }
        
        return false;
    }
    
    /**
     * Configure debug logging for platform detection if in dev mode
     */
    public static void configureDevModeLogging() {
        if (DEV_MODE) {
            System.setProperty("jakarta.migration.dev.mode", "true");
            // Set log level via system property for SLF4J implementations
            System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
            System.setProperty("org.slf4j.simpleLogger.log.adrianmikula.jakartamigration.platforms", "debug");
            System.setProperty("org.slf4j.simpleLogger.log.adrianmikula.jakartamigration.util", "debug");
            System.setProperty("org.slf4j.simpleLogger.log.adrianmikula.jakartamigration.advancedscanning", "debug");
            
            log.info("🔧 Jakarta Migration Plugin: Debug logging enabled for development mode");
        }
    }
    
    /**
     * Check if debug logging is enabled
     */
    public static boolean isDebugEnabled() {
        return DEV_MODE;
    }
}
