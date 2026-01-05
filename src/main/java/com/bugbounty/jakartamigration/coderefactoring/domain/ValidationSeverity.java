package com.bugbounty.jakartamigration.coderefactoring.domain;

/**
 * Severity level of a validation issue.
 */
public enum ValidationSeverity {
    /**
     * Critical issue that prevents compilation or execution
     */
    CRITICAL,
    
    /**
     * Error that may cause runtime issues
     */
    ERROR,
    
    /**
     * Warning that should be addressed
     */
    WARNING,
    
    /**
     * Informational message
     */
    INFO
}

