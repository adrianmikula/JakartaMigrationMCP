package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.service.ScanRecipeRecommendationService;
import adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition;
import adrianmikula.jakartamigration.coderefactoring.domain.RecipeCategory;
import adrianmikula.jakartamigration.coderefactoring.domain.RecipeExecutionResult;
import adrianmikula.jakartamigration.coderefactoring.domain.RecipeExecutionHistory;
import adrianmikula.jakartamigration.coderefactoring.service.RecipeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for ScanRecipeRecommendationServiceImpl.
 * Simplified test without Mockito for basic functionality verification.
 */
class ScanRecipeRecommendationServiceImplTest {

    private ScanRecipeRecommendationService recommendationService;
    private Path testProjectPath;

    @BeforeEach
    void setUp() {
        // Create a simple mock implementation for testing
        RecipeService mockRecipeService = new MockRecipeService();
        recommendationService = new ScanRecipeRecommendationServiceImpl(mockRecipeService);
        testProjectPath = Paths.get("/test/project");
    }

    @Test
    void testGetRecipeRecommendations_EmptyScanResults() {
        // Given
        Map<String, Object> scanResults = Map.of();

        // When
        List<ScanRecipeRecommendationService.RecipeRecommendation> recommendations = 
            recommendationService.getRecipeRecommendations(testProjectPath, scanResults);

        // Then
        assertTrue(recommendations.isEmpty());
    }

    @Test
    void testGetRecipeRecommendations_NullScanResults() {
        // Given
        Map<String, Object> scanResults = Map.of(
            "JPA_ANNOTATION_SCANNER", null
        );

        // When
        List<ScanRecipeRecommendationService.RecipeRecommendation> recommendations = 
            recommendationService.getRecipeRecommendations(testProjectPath, scanResults);

        // Then
        assertTrue(recommendations.isEmpty());
    }

    @Test
    void testRecipeRecommendationRecord_Validation() {
        // Given
        RecipeDefinition recipe = RecipeDefinition.builder()
            .name("TestRecipe")
            .build();

        // When & Then - Valid confidence scores
        assertDoesNotThrow(() -> new ScanRecipeRecommendationService.RecipeRecommendation(
            recipe, 0.0, "test", List.of()));
        assertDoesNotThrow(() -> new ScanRecipeRecommendationService.RecipeRecommendation(
            recipe, 1.0, "test", List.of()));

        // When & Then - Invalid confidence scores
        assertThrows(IllegalArgumentException.class, () -> 
            new ScanRecipeRecommendationService.RecipeRecommendation(
                recipe, -0.1, "test", List.of()));
        assertThrows(IllegalArgumentException.class, () -> 
            new ScanRecipeRecommendationService.RecipeRecommendation(
                recipe, 1.1, "test", List.of()));
    }

    @Test
    void testGetRecipesByScanType_ReturnsNonEmptyMap() {
        // When
        Map<String, List<RecipeDefinition>> recipesByScanType = 
            recommendationService.getRecipesByScanType();

        // Then
        assertNotNull(recipesByScanType);
        assertTrue(recipesByScanType.containsKey("JPA_ANNOTATION_SCANNER"));
        assertTrue(recipesByScanType.containsKey("SERVLET_JSP_SCANNER"));
    }

    @Test
    void testGetRecipeRecommendationsForFile_ReturnsEmptyForUnknownScanType() {
        // Given
        Path testFile = Paths.get("/test/project/TestFile.java");

        // When
        List<ScanRecipeRecommendationService.RecipeRecommendation> recommendations = 
            recommendationService.getRecipeRecommendationsForFile(testFile, "UNKNOWN_SCANNER");

        // Then
        assertTrue(recommendations.isEmpty());
    }

    /**
     * Simple mock RecipeService implementation for testing.
     */
    private static class MockRecipeService implements RecipeService {
        @Override
        public List<RecipeDefinition> getRecipes(Path projectPath) {
            return List.of(
                RecipeDefinition.builder()
                    .name("MigrateJPA")
                    .description("Converts javax.persistence.* to jakarta.persistence.*")
                    .category(RecipeCategory.DATABASE)
                    .build(),
                RecipeDefinition.builder()
                    .name("MigrateServlet")
                    .description("Converts javax.servlet.* to jakarta.servlet.*")
                    .category(RecipeCategory.WEB)
                    .build()
            );
        }

        @Override
        public List<RecipeDefinition> getRecipesByCategory(RecipeCategory category, Path projectPath) {
            return getRecipes(projectPath).stream()
                .filter(recipe -> recipe.getCategory() == category)
                .toList();
        }

        @Override
        public adrianmikula.jakartamigration.coderefactoring.domain.RecipeExecutionResult applyRecipe(String recipeName, Path projectPath) {
            return null; // Not implemented for this test
        }

        @Override
        public adrianmikula.jakartamigration.coderefactoring.domain.RecipeExecutionResult undoRecipe(Long executionId, Path projectPath) {
            return null; // Not implemented for this test
        }

        @Override
        public List<adrianmikula.jakartamigration.coderefactoring.domain.RecipeExecutionHistory> getHistory(Path projectPath) {
            return List.of(); // Not implemented for this test
        }
    }
}
