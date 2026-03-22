package adrianmikula.jakartamigration.coderefactoring.service.util;

import java.util.List;
import java.util.Map;

/**
 * POJO for deserializing recipes.yaml configuration.
 * Includes both recipe definitions and upgrade recommendations.
 */
public class RecipesYamlConfig {

    private List<Map<String, Object>> recipes;
    private List<UpgradeRecommendationConfig> upgradeRecommendations;

    public List<Map<String, Object>> getRecipes() {
        return recipes;
    }

    public void setRecipes(List<Map<String, Object>> recipes) {
        this.recipes = recipes;
    }

    public List<UpgradeRecommendationConfig> getUpgradeRecommendations() {
        return upgradeRecommendations;
    }

    public void setUpgradeRecommendations(List<UpgradeRecommendationConfig> upgradeRecommendations) {
        this.upgradeRecommendations = upgradeRecommendations;
    }

    /**
     * Upgrade recommendation entry from YAML config.
     * Maps a javax artifact to its jakarta equivalent.
     */
    public static class UpgradeRecommendationConfig {
        private String currentGroupId;
        private String currentArtifactId;
        private String recommendedGroupId;
        private String recommendedArtifactId;
        private String recommendedVersion;
        private String associatedRecipeName;

        public String getCurrentGroupId() { return currentGroupId; }
        public void setCurrentGroupId(String currentGroupId) { this.currentGroupId = currentGroupId; }

        public String getCurrentArtifactId() { return currentArtifactId; }
        public void setCurrentArtifactId(String currentArtifactId) { this.currentArtifactId = currentArtifactId; }

        public String getRecommendedGroupId() { return recommendedGroupId; }
        public void setRecommendedGroupId(String recommendedGroupId) { this.recommendedGroupId = recommendedGroupId; }

        public String getRecommendedArtifactId() { return recommendedArtifactId; }
        public void setRecommendedArtifactId(String recommendedArtifactId) { this.recommendedArtifactId = recommendedArtifactId; }

        public String getRecommendedVersion() { return recommendedVersion; }
        public void setRecommendedVersion(String recommendedVersion) { this.recommendedVersion = recommendedVersion; }

        public String getAssociatedRecipeName() { return associatedRecipeName; }
        public void setAssociatedRecipeName(String associatedRecipeName) { this.associatedRecipeName = associatedRecipeName; }
    }
}
