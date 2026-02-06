/*
 * Copyright 2024 Adrian Kozak
 * Copyright 2024 Prairie Trail Software
 *
 * This software is proprietary and may not be used, copied, modified,
 * or distributed except under the terms of a separate commercial license agreement.
 */
package adrianmikula.jakartamigration.runtimeverification.domain;

import java.util.List;

/**
 * Result of runtime verification.
 * 
 * NOTE: This is a stub. Full implementation with bytecode analysis
 * is available in the premium edition.
 */
public record VerificationResult(
    boolean success,
    List<String> issues,
    List<String> warnings,
    List<String> info,
    int jakartaReferences,
    int javaxReferences
) {
    public VerificationResult {
        if (issues == null) {
            issues = List.of();
        }
        if (warnings == null) {
            warnings = List.of();
        }
        if (info == null) {
            info = List.of();
        }
    }
    
    public static VerificationResult empty() {
        return new VerificationResult(true, List.of(), List.of(), List.of(), 0, 0);
    }
    
    public boolean hasIssues() {
        return !issues.isEmpty();
    }
    
    /**
     * Returns the status of the verification.
     */
    public String status() {
        return success ? "VERIFIED" : "ISSUES_FOUND";
    }
    
    /**
     * Returns the list of errors (issues).
     */
    public List<String> errors() {
        return issues;
    }
}
