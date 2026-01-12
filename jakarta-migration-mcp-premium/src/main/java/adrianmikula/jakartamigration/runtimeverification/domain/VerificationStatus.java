package adrianmikula.jakartamigration.runtimeverification.domain;

/**
 * Status of a runtime verification operation.
 */
public enum VerificationStatus {
    /**
     * Verification completed successfully with no errors.
     */
    SUCCESS,
    
    /**
     * Verification failed with errors.
     */
    FAILED,
    
    /**
     * Verification completed with some warnings but no critical errors.
     */
    PARTIAL,
    
    /**
     * Verification timed out.
     */
    TIMEOUT,
    
    /**
     * Verification status is unknown or could not be determined.
     */
    UNKNOWN
}

