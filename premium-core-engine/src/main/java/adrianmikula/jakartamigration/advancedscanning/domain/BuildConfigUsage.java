package adrianmikula.jakartamigration.advancedscanning.domain;

/**
 * Represents a javax.* dependency found in build configuration (pom.xml, build.gradle).
 */
public record BuildConfigUsage(
    String groupId,
    String artifactId,
    String currentVersion,
    String jakartaGroupId,
    String jakartaArtifactId,
    String recommendedVersion,
    int lineNumber
) {
    public BuildConfigUsage {
        if (groupId == null || groupId.isBlank()) {
            throw new IllegalArgumentException("groupId cannot be null or blank");
        }
        if (artifactId == null || artifactId.isBlank()) {
            throw new IllegalArgumentException("artifactId cannot be null or blank");
        }
    }

    public boolean hasJakartaEquivalent() {
        return jakartaGroupId != null && jakartaArtifactId != null;
    }
}
