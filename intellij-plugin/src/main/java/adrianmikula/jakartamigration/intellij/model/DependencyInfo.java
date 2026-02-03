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

    @JsonProperty("isBlocker")
    private boolean isBlocker;

    @JsonProperty("riskLevel")
    private RiskLevel riskLevel;

    @JsonProperty("migrationImpact")
    private String migrationImpact;

    public DependencyInfo() {
    }

    public DependencyInfo(String groupId, String artifactId, String currentVersion,
                          String recommendedVersion, DependencyMigrationStatus migrationStatus,
                          boolean isBlocker, RiskLevel riskLevel, String migrationImpact) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.currentVersion = currentVersion;
        this.recommendedVersion = recommendedVersion;
        this.migrationStatus = migrationStatus;
        this.isBlocker = isBlocker;
        this.riskLevel = riskLevel;
        this.migrationImpact = migrationImpact;
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

    public boolean isBlocker() {
        return isBlocker;
    }

    public void setBlocker(boolean blocker) {
        isBlocker = blocker;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getMigrationImpact() {
        return migrationImpact;
    }

    public void setMigrationImpact(String migrationImpact) {
        this.migrationImpact = migrationImpact;
    }

    public String getDisplayName() {
        return groupId + ":" + artifactId;
    }
}
