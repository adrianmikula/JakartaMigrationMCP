package adrianmikula.jakartamigration.dependencyanalysis.domain;

import lombok.Builder;

/**
 * Represents a recommended upgrade from a legacy javax artifact to a Jakarta
 * artifact,
 * potentially associated with a specific migration recipe.
 */
@Builder
public record UpgradeRecommendation(
        String currentGroupId,
        String currentArtifactId,
        String recommendedGroupId,
        String recommendedArtifactId,
        String recommendedVersion,
        String associatedRecipeName) {
}
