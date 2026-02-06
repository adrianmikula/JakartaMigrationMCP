/*
 * Copyright 2024 Adrian Kozak
 * Copyright 2024 Prairie Trail Software
 *
 * This software is proprietary and may not be used, copied, modified,
 * or distributed except under the terms of a separate commercial license agreement.
 */
package adrianmikula.jakartamigration.runtimeverification.domain;

import java.time.Instant;

/**
 * Analysis of a verification error.
 * 
 * NOTE: This is a stub. Full implementation with detailed error
 * analysis and suggestions is available in the premium edition.
 */
public record ErrorAnalysis(
    String errorMessage,
    ErrorCategory category,
    String stackTrace,
    String suggestedFix,
    Instant timestamp
) {
    public ErrorAnalysis {
        if (category == null) {
            category = ErrorCategory.UNKNOWN;
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
        if (errorMessage == null) {
            errorMessage = "Unknown error";
        }
    }
    
    public static ErrorAnalysis of(String message, ErrorCategory category) {
        return new ErrorAnalysis(message, category, null, null, Instant.now());
    }
    
    public static ErrorAnalysis unknown(String message) {
        return new ErrorAnalysis(message, ErrorCategory.UNKNOWN, null, null, Instant.now());
    }
}
