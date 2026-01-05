package com.bugbounty.jakartamigration.coderefactoring.domain;

import java.util.List;

/**
 * Result of validating refactored code.
 */
public record ValidationResult(
    boolean isValid,
    List<ValidationIssue> issues,
    String filePath,
    ValidationStatus status
) {
    public ValidationResult {
        if (issues == null) {
            throw new IllegalArgumentException("Issues cannot be null");
        }
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("FilePath cannot be null or blank");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
    }
    
    /**
     * Returns true if validation passed with no issues.
     */
    public boolean isSuccessful() {
        return isValid && issues.isEmpty();
    }
    
    /**
     * Returns true if there are any critical issues.
     */
    public boolean hasCriticalIssues() {
        return issues.stream()
            .anyMatch(issue -> issue.severity() == ValidationSeverity.CRITICAL);
    }
}

