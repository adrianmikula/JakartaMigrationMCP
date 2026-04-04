package adrianmikula.jakartamigration.intellij.model;

import adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus;

/**
 * Record representing Jakarta artifact coordinates from Maven Central
 */
public record JakartaArtifactCoordinates(
    String groupId,
    String artifactId,
    String version,
    DependencyMigrationStatus status
) {
    /**
     * Gets the artifact coordinates in Maven format
     */
    public String getCoordinates() {
        return groupId + ":" + artifactId + ":" + version;
    }
    
    /**
     * Gets the display name for UI
     */
    public String getDisplayName() {
        return artifactId + " (" + version + ")";
    }
}
