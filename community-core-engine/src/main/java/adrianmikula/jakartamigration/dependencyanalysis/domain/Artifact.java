package adrianmikula.jakartamigration.dependencyanalysis.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Objects;

/**
 * Represents a Maven/Gradle artifact with coordinates and metadata.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Artifact(
        String groupId,
        String artifactId,
        String version,
        String scope,
        boolean transitive) {
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

    /**
     * Checks if this artifact is Jakarta EE compatible based on its groupId.
     * Returns true if the artifact is already using Jakarta namespace.
     */
    public boolean isJakartaCompatible() {
        return groupId != null && (
            groupId.startsWith("jakarta.") ||
            groupId.startsWith("jakartaee.") ||
            groupId.equals("jakarta.xml.bind") ||
            groupId.equals("jakarta.annotation")
        );
    }

    /**
     * Creates an Artifact from a Maven coordinate string
     * (groupId:artifactId:version).
     *
     * @param coordinate The coordinate string
     * @return New Artifact instance
     * @throws IllegalArgumentException if coordinate format is invalid
     */
    public static Artifact fromCoordinate(String coordinate) {
        if (coordinate == null || coordinate.isEmpty()) {
            throw new IllegalArgumentException("Coordinate cannot be null or empty");
        }
        String[] parts = coordinate.split(":");
        if (parts.length < 3) {
            throw new IllegalArgumentException(
                    "Invalid coordinate format: " + coordinate + ". Expected groupId:artifactId:version");
        }
        // Use default scope "compile" and transitive false if not specified,
        // though our current toCoordinate only includes the first 3 parts.
        return new Artifact(parts[0], parts[1], parts[2], "compile", false);
    }
}
