/*
 * Copyright 2024 Adrian Kozak
 * Copyright 2024 Prairie Trail Software
 *
 * This software is proprietary and may not be used, copied, modified,
 * or distributed except under the terms of a separate commercial license agreement.
 */
package adrianmikula.jakartamigration.coderefactoring.domain;

/**
 * Safety level for refactoring recipes.
 * 
 * NOTE: This is a stub. Full implementation with OpenRewrite recipes
 * is available in the premium edition.
 */
public enum SafetyLevel {
    HIGH,      // Safe, well-tested recipe
    MEDIUM,    // Generally safe but may have edge cases
    LOW        // Risky, requires careful review
}
