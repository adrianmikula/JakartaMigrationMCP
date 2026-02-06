package adrianmikula.jakartamigration.dependencyanalysis.domain;

import java.util.List;

/**
 * Recommendation for a Jakarta-compatible version of an artifact.
 */
public record VersionRecommendation(
    Artifact currentArtifact,
    Artifact recommendedArtifact,
    String migrationPath,
    List<String> breakingChanges,
    double compatibilityScore
) {
    public VersionRecommendation {
        if (compatibilityScore < 0.0 || compatibilityScore > 1.0) {
            throw new IllegalArgumentException("Compatibility score must be between 0.0 and 1.0");
        }
    }
}

