package adrianmikula.jakartamigration.advancedscanning.domain;

/**
 * Represents a third-party library that hasn't been migrated to Jakarta EE.
 * This is used by the ThirdPartyLibScanner to track dependency migration issues.
 */
public class ThirdPartyLibUsage {
    private final String libraryName;
    private final String groupId;
    private final String artifactId;
    private final String currentVersion;
    private final String issueType;
    private final String suggestedReplacement;

    public ThirdPartyLibUsage(String libraryName, String groupId, String artifactId, 
                            String currentVersion, String issueType, String suggestedReplacement) {
        this.libraryName = libraryName;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.currentVersion = currentVersion;
        this.issueType = issueType;
        this.suggestedReplacement = suggestedReplacement;
    }

    public String getLibraryName() {
        return libraryName;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public String getIssueType() {
        return issueType;
    }

    public String getSuggestedReplacement() {
        return suggestedReplacement;
    }

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
