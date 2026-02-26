package adrianmikula.jakartamigration.advancedscanning.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Represents a third-party library that hasn't been migrated to Jakarta EE.
 * This is used by the ThirdPartyLibScanner to track dependency migration
 * issues.
 */
@Getter
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class ThirdPartyLibUsage {
    private final String libraryName;
    private final String groupId;
    private final String artifactId;
    private final String currentVersion;
    private final String issueType;
    private final String suggestedReplacement;

    /**
     * Returns the Maven/Gradle coordinate for the library.
     */
    public String getCoordinates() {
        return groupId + ":" + artifactId + ":" + currentVersion;
    }

    /**
     * Returns migration complexity level.
     */
    public MigrationComplexity getComplexity() {
        return switch (issueType) {
            case "javax-only" -> MigrationComplexity.HIGH;
            case "partial-migration" -> MigrationComplexity.MEDIUM;
            case "outdated" -> MigrationComplexity.LOW;
            default -> MigrationComplexity.UNKNOWN;
        };
    }

    public enum MigrationComplexity {
        LOW, MEDIUM, HIGH, UNKNOWN
    }
}
