package com.bugbounty.jakartamigration.coderefactoring.domain;

/**
 * Status of validation.
 */
public enum ValidationStatus {
    /**
     * Validation passed successfully
     */
    PASSED,
    
    /**
     * Validation failed with errors
     */
    FAILED,
    
    /**
     * Validation passed with warnings
     */
    PASSED_WITH_WARNINGS,
    
    /**
     * Validation could not be completed
     */
    INCOMPLETE
}

