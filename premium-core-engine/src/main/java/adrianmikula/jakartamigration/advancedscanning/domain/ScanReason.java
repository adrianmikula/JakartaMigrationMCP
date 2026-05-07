package adrianmikula.jakartamigration.advancedscanning.domain;

/**
 * Enum representing the reason for a dependency's classification status.
 * Used to provide structured metadata about how a dependency was classified.
 */
public enum ScanReason {
    /**
     * Dependency is in whitelist (safe/jdk) - no migration needed.
     */
    WHITELISTED,

    /**
     * Dependency is in blacklist (upgrade) - needs migration.
     */
    BLACKLISTED,

    /**
     * Bytecode scan detected javax packages only.
     */
    BYTECODE_SCAN_JAVAX,

    /**
     * Bytecode scan detected jakarta packages only.
     */
    BYTECODE_SCAN_JAKARTA,

    /**
     * Bytecode scan detected both javax and jakarta packages.
     */
    BYTECODE_SCAN_MIXED,

    /**
     * Bytecode scan was inconclusive.
     */
    BYTECODE_SCAN_UNKNOWN,

    /**
     * Maven Central lookup found Jakarta equivalent.
     */
    MAVEN_LOOKUP_FOUND,

    /**
     * Maven Central lookup found no equivalent.
     */
    MAVEN_LOOKUP_NONE,

    /**
     * Dependency marked as incompatible due to child dependency incompatibility.
     */
    TRANSITIVE_INCOMPATIBLE,

    /**
     * Dependency is context-dependent or ambiguous - requires manual review.
     */
    REVIEW_REQUIRED,

    /**
     * No information available about the dependency.
     */
    UNKNOWN
}
