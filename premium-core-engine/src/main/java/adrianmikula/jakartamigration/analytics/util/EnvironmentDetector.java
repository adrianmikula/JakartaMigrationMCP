package adrianmikula.jakartamigration.analytics.util;

/**
 * Utility for detecting the current environment (dev, demo, prod) based on system properties and launch mode.
 */
public class EnvironmentDetector {
    
    private static final String ENVIRONMENT = detectEnvironment();
    
    /**
     * Detects the current environment based on system properties and launch mode.
     * 
     * @return "dev", "demo", or "prod"
     */
    private static String detectEnvironment() {
        // Check for explicit environment system property
        String mode = System.getProperty("jakarta.migration.mode");
        if (mode != null) {
            return switch (mode.toLowerCase()) {
                case "dev", "development" -> "dev";
                case "demo" -> "demo";
                default -> "prod";
            };
        }
        
        // Check for dev mode indicators (from DevModeLogger logic)
        String devMode = System.getProperty("jakarta.migration.dev.mode");
        if (devMode != null && Boolean.parseBoolean(devMode)) {
            return "dev";
        }
        
        // Check if running via gradle runIdeDev
        String javaCommand = System.getProperty("sun.java.command");
        if (javaCommand != null && javaCommand.contains("runIdeDev")) {
            return "dev";
        }
        
        // Check for environment variable
        String envDevMode = System.getenv("JAKARTA_MIGRATION_DEV_MODE");
        if (envDevMode != null && Boolean.parseBoolean(envDevMode)) {
            return "dev";
        }
        
        // Check for demo mode system property (set by runIdeDemo task)
        String demoMode = System.getProperty("jakarta.migration.mode");
        if (demoMode != null && demoMode.equals("demo")) {
            return "demo";
        }
        
        // Default to production
        return "prod";
    }
    
    /**
     * Gets the current environment.
     * 
     * @return "dev", "demo", or "prod"
     */
    public static String getCurrentEnvironment() {
        return ENVIRONMENT;
    }
    
    /**
     * Checks if running in development mode.
     * 
     * @return true if environment is "dev"
     */
    public static boolean isDevMode() {
        return "dev".equals(ENVIRONMENT);
    }
    
    /**
     * Checks if running in demo mode.
     * 
     * @return true if environment is "demo"
     */
    public static boolean isDemoMode() {
        return "demo".equals(ENVIRONMENT);
    }
    
    /**
     * Checks if running in production mode.
     * 
     * @return true if environment is "prod"
     */
    public static boolean isProdMode() {
        return "prod".equals(ENVIRONMENT);
    }
}
