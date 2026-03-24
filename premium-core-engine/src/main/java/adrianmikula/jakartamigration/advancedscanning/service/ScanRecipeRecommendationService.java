package adrianmikula.jakartamigration.advancedscanning.service;

import adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Service for recommending refactor recipes based on advanced scan results.
 * Links scan findings to applicable migration recipes.
 */
public interface ScanRecipeRecommendationService {

    /**
     * Gets recipe recommendations for files found by advanced scans.
     *
     * @param projectPath Path to the project
     * @param scanResults Map of scanner type to scan results
     * @return List of recommended recipes with confidence scores
     */
    List<RecipeRecommendation> getRecipeRecommendations(Path projectPath, 
            Map<String, Object> scanResults);

    /**
     * Gets recipe recommendations for a specific file.
     *
     * @param filePath Path to the file
     * @param scanType Type of scan that found issues in the file
     * @return List of recommended recipes for this file
     */
    List<RecipeRecommendation> getRecipeRecommendationsForFile(Path filePath, 
            String scanType);

    /**
     * Gets all available recipes organized by scan type.
     *
     * @return Map of scan type to list of applicable recipes
     */
    Map<String, List<RecipeDefinition>> getRecipesByScanType();

    /**
     * Represents a recipe recommendation with confidence score.
     */
    record RecipeRecommendation(
            RecipeDefinition recipe,
            double confidenceScore,
            String reason,
            List<String> affectedFiles
    ) {
        public RecipeRecommendation {
            if (confidenceScore < 0.0 || confidenceScore > 1.0) {
                throw new IllegalArgumentException("Confidence score must be between 0.0 and 1.0");
            }
        }
    }
}
