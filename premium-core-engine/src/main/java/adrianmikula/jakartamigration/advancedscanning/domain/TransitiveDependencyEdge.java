package adrianmikula.jakartamigration.advancedscanning.domain;

/**
 * Represents a directed edge in the dependency tree, linking a parent
 * artifact to a child artifact. Used for reconstructing the full dependency
 * graph during deep scans.
 */
public record TransitiveDependencyEdge(
        String parentArtifactKey,
        String childArtifactKey
) {
    public TransitiveDependencyEdge {
        if (parentArtifactKey == null || parentArtifactKey.isBlank()) {
            throw new IllegalArgumentException("parentArtifactKey cannot be null or blank");
        }
        if (childArtifactKey == null || childArtifactKey.isBlank()) {
            throw new IllegalArgumentException("childArtifactKey cannot be null or blank");
        }
    }
}
