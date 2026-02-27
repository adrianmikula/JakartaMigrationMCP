package adrianmikula.jakartamigration.coderefactoring.domain;

/**
 * Safety level for refactoring recipes.
 */
public enum SafetyLevel {
    HIGH,      // Safe, well-tested recipe
    MEDIUM,    // Generally safe but may have edge cases
    LOW        // Risky, requires careful review
}

