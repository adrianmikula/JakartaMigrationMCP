package adrianmikula.jakartamigration.intellij.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Individual dependency information for the dependencies table from TypeSpec: intellij-plugin-ui.tsp
 */
public class DependencyInfo {
    @JsonProperty("groupId")
    private String groupId;

    @JsonProperty("artifactId")
    private String artifactId;

    @JsonProperty("currentVersion")
    private String currentVersion;

    @JsonProperty("recommendedVersion")
    private String recommendedVersion;

    @JsonProperty("migrationStatus")
    private DependencyMigrationStatus migrationStatus;

    @JsonProperty("isTransitive")
    private boolean isTransitive;

    public enum DependencyType {
        DIRECT("Direct"),
        TRANSITIVE("Transitive");

        private final String displayName;

        DependencyType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public DependencyInfo() {
    }

    public DependencyInfo(String groupId, String artifactId, String currentVersion,
                          String recommendedVersion, DependencyMigrationStatus migrationStatus,
                          boolean isTransitive) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.currentVersion = currentVersion;
        this.recommendedVersion = recommendedVersion;
        this.migrationStatus = migrationStatus;
        this.isTransitive = isTransitive;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(String currentVersion) {
        this.currentVersion = currentVersion;
    }

    public String getRecommendedVersion() {
        return recommendedVersion;
    }

    public void setRecommendedVersion(String recommendedVersion) {
        this.recommendedVersion = recommendedVersion;
    }

    public DependencyMigrationStatus getMigrationStatus() {
        return migrationStatus;
    }

    public void setMigrationStatus(DependencyMigrationStatus migrationStatus) {
        this.migrationStatus = migrationStatus;
    }

    /**
     * Blocker flag is no longer used - migration status determines blocking behavior.
     * Always returns false.
     */
    public boolean isBlocker() {
        return false;
    }

    public void setBlocker(boolean blocker) {
        // No-op: blockers are no longer used
    }

    public boolean isTransitive() {
        return isTransitive;
    }

    public void setTransitive(boolean transitive) {
        isTransitive = transitive;
    }

    public String getDisplayName() {
        return groupId + ":" + artifactId;
    }

    public DependencyType getDependencyType() {
        return isTransitive ? DependencyType.TRANSITIVE : DependencyType.DIRECT;
    }
}
