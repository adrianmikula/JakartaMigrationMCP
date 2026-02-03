package adrianmikula.jakartamigration.dependencyanalysis.domain;

import java.util.Objects;

/**
 * Represents a Maven/Gradle artifact with coordinates and metadata.
 */
public record Artifact(
    String groupId,
    String artifactId,
    String version,
    String scope,
    boolean transitive
) {
    public Artifact {
        Objects.requireNonNull(groupId, "groupId cannot be null");
        Objects.requireNonNull(artifactId, "artifactId cannot be null");
        Objects.requireNonNull(version, "version cannot be null");
        Objects.requireNonNull(scope, "scope cannot be null");
    }
    
    /**
     * Returns the Maven coordinate string (groupId:artifactId:version).
     */
    public String toCoordinate() {
        return String.format("%s:%s:%s", groupId, artifactId, version);
    }
    
    /**
     * Returns the artifact identifier (groupId:artifactId).
     */
    public String toIdentifier() {
        return String.format("%s:%s", groupId, artifactId);
    }
}

