package adrianmikula.jakartamigration.intellij.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Individual dependency information for the dependencies table from TypeSpec:
 * intellij-plugin-ui.tsp
 */
public class DependencyInfo {
    @JsonProperty("groupId")
    private String groupId;

    @JsonProperty("artifactId")
    private String artifactId;

    @JsonProperty("currentVersion")
    private String currentVersion;

    @JsonProperty("recommendedGroupId")
    private String recommendedGroupId;

    @JsonProperty("recommendedArtifactId")
    private String recommendedArtifactId;

    @JsonProperty("recommendedVersion")
    private String recommendedVersion;

    @JsonProperty("jakartaCompatibilityStatus")
    private String jakartaCompatibilityStatus;

    @JsonProperty("associatedRecipeName")
    private String associatedRecipeName;

    @JsonProperty("migrationStatus")
    private DependencyMigrationStatus migrationStatus;

    @JsonProperty("isTransitive")
    private boolean isTransitive;

    @JsonProperty("isOrganizational")
    private boolean isOrganizational;

    @JsonProperty("depth")
    private int depth;  // 0 = direct dependency, 1+ = transitive level

    @JsonProperty("scope")
    private String scope;  // compile, test, provided, runtime

    // Tree structure fields for hierarchical dependency view
    private String parentDependencyId;  // ID of parent dependency (groupId:artifactId:version)
    private List<DependencyInfo> children;  // Nested dependencies
    @JsonProperty("scanReason")
    private String scanReason;

    @JsonProperty("detailMessage")
    private String detailMessage;

    @JsonProperty("confidence")
    private double confidence;

    @JsonProperty("incompatibilityFromTransitive")
    private boolean incompatibilityFromTransitive;

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
            String recommendedGroupId, String recommendedArtifactId, String recommendedVersion,
            String jakartaCompatibilityStatus, String associatedRecipeName,
            DependencyMigrationStatus migrationStatus,
            boolean isTransitive, boolean isOrganizational) {
        this(groupId, artifactId, currentVersion, recommendedGroupId, recommendedArtifactId,
            recommendedVersion, jakartaCompatibilityStatus, associatedRecipeName, migrationStatus,
            isTransitive, isOrganizational, 0, "compile");
    }

    public DependencyInfo(String groupId, String artifactId, String currentVersion,
            String recommendedGroupId, String recommendedArtifactId, String recommendedVersion,
            String jakartaCompatibilityStatus, String associatedRecipeName,
            DependencyMigrationStatus migrationStatus,
            boolean isTransitive, boolean isOrganizational,
            int depth, String scope) {
        this(groupId, artifactId, currentVersion, recommendedGroupId, recommendedArtifactId,
            recommendedVersion, jakartaCompatibilityStatus, associatedRecipeName, migrationStatus,
            isTransitive, isOrganizational, depth, scope, null, null, 0.0, false);
    }

    public DependencyInfo(String groupId, String artifactId, String currentVersion,
            String recommendedGroupId, String recommendedArtifactId, String recommendedVersion,
            String jakartaCompatibilityStatus, String associatedRecipeName,
            DependencyMigrationStatus migrationStatus,
            boolean isTransitive, boolean isOrganizational,
            int depth, String scope, String scanReason, String detailMessage,
            double confidence, boolean incompatibilityFromTransitive) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.currentVersion = currentVersion;
        this.recommendedGroupId = recommendedGroupId;
        this.recommendedArtifactId = recommendedArtifactId;
        this.recommendedVersion = recommendedVersion;
        this.jakartaCompatibilityStatus = jakartaCompatibilityStatus;
        this.associatedRecipeName = associatedRecipeName;
        this.migrationStatus = migrationStatus;
        this.isTransitive = isTransitive;
        this.isOrganizational = isOrganizational;
        this.depth = depth;
        this.scope = scope;
        this.scanReason = scanReason;
        this.detailMessage = detailMessage;
        this.confidence = confidence;
        this.incompatibilityFromTransitive = incompatibilityFromTransitive;
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

    public String getRecommendedGroupId() {
        return recommendedGroupId;
    }

    public void setRecommendedGroupId(String recommendedGroupId) {
        this.recommendedGroupId = recommendedGroupId;
    }

    public String getRecommendedArtifactId() {
        return recommendedArtifactId;
    }

    public void setRecommendedArtifactId(String recommendedArtifactId) {
        this.recommendedArtifactId = recommendedArtifactId;
    }

    public String getRecommendedVersion() {
        return recommendedVersion;
    }

    public void setRecommendedVersion(String recommendedVersion) {
        this.recommendedVersion = recommendedVersion;
    }

    public String getJakartaCompatibilityStatus() {
        return jakartaCompatibilityStatus;
    }

    public void setJakartaCompatibilityStatus(String jakartaCompatibilityStatus) {
        this.jakartaCompatibilityStatus = jakartaCompatibilityStatus;
    }

    public String getAssociatedRecipeName() {
        return associatedRecipeName;
    }

    public void setAssociatedRecipeName(String associatedRecipeName) {
        this.associatedRecipeName = associatedRecipeName;
    }

    public DependencyMigrationStatus getMigrationStatus() {
        return migrationStatus;
    }

    public void setMigrationStatus(DependencyMigrationStatus migrationStatus) {
        this.migrationStatus = migrationStatus;
    }

    /**
     * Blocker flag is no longer used - migration status determines blocking
     * behavior.
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

    public boolean isOrganizational() {
        return isOrganizational;
    }

    public void setOrganizational(boolean organizational) {
        isOrganizational = organizational;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getScanReason() {
        return scanReason;
    }

    public void setScanReason(String scanReason) {
        this.scanReason = scanReason;
    }

    public String getDetailMessage() {
        return detailMessage;
    }

    public void setDetailMessage(String detailMessage) {
        this.detailMessage = detailMessage;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public boolean isIncompatibilityFromTransitive() {
        return incompatibilityFromTransitive;
    }

    public void setIncompatibilityFromTransitive(boolean incompatibilityFromTransitive) {
        this.incompatibilityFromTransitive = incompatibilityFromTransitive;
    }

    public String getDisplayName() {
        return groupId + ":" + artifactId;
    }

    public String getRecommendedArtifactCoordinates() {
        if (recommendedGroupId != null && recommendedArtifactId != null) {
            return recommendedGroupId + ":" + recommendedArtifactId + 
                   (recommendedVersion != null ? ":" + recommendedVersion : "");
        }
        return recommendedVersion != null ? recommendedVersion : "";
    }

    public void setRecommendedArtifactCoordinates(String coordinates) {
        if (coordinates != null && coordinates.contains(":")) {
            String[] parts = coordinates.split(":");
            if (parts.length >= 2) {
                this.recommendedGroupId = parts[0];
                this.recommendedArtifactId = parts[1];
                if (parts.length >= 3) {
                    this.recommendedVersion = parts[2];
                }
            }
        }
    }

    public DependencyType getDependencyType() {
        return isTransitive ? DependencyType.TRANSITIVE : DependencyType.DIRECT;
    }

    public String getParentDependencyId() {
        return parentDependencyId;
    }

    public void setParentDependencyId(String parentDependencyId) {
        this.parentDependencyId = parentDependencyId;
    }

    public List<DependencyInfo> getChildren() {
        return children;
    }

    public void setChildren(List<DependencyInfo> children) {
        this.children = children;
    }

    /**
     * Get unique identifier for this dependency.
     * Used for tree building and duplicate detection.
     */
    public String getDependencyId() {
        return groupId + ":" + artifactId + ":" + currentVersion;
    }
}
