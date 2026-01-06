package adrianmikula.jakartamigration.runtimeverification.domain;

/**
 * Categories of errors that can occur during Jakarta migration runtime verification.
 */
public enum ErrorCategory {
    /**
     * Error related to namespace migration (javax to jakarta).
     */
    NAMESPACE_MIGRATION,
    
    /**
     * Error related to classpath issues (missing dependencies, wrong versions).
     */
    CLASSPATH_ISSUE,
    
    /**
     * Error related to binary incompatibility.
     */
    BINARY_INCOMPATIBILITY,
    
    /**
     * Error related to configuration (XML, properties, etc.).
     */
    CONFIGURATION_ERROR,
    
    /**
     * Error category is unknown or could not be determined.
     */
    UNKNOWN
}

