package adrianmikula.jakartamigration.coderefactoring.service.util;

import adrianmikula.jakartamigration.analysis.persistence.CentralMigrationAnalysisStore;
import adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("slow")
class RecipeSeederTest {

    @TempDir
    Path tempDir;

    private CentralMigrationAnalysisStore store;

    @BeforeEach
    void setUp() {
        store = new CentralMigrationAnalysisStore(tempDir.resolve("central.db"));
    }

    @Test
    @DisplayName("Should successfully seed recipes with valid OpenRewrite classes")
    void shouldSeedRecipesWithValidOpenRewriteClasses() {
        // When
        RecipeSeeder.seedDefaultRecipes(store);
        
        // Then - Should have loaded recipes from the actual recipes.json
        List<RecipeDefinition> recipes = store.getRecipes();
        
        // Verify that recipes with valid OpenRewrite classes were loaded
        // (This will depend on the actual recipes.json content)
        assertThat(recipes).isNotEmpty();
        
        // Verify that all loaded recipes have OpenRewrite classes set
        for (RecipeDefinition recipe : recipes) {
            assertThat(recipe.getOpenRewriteRecipeName()).isNotNull();
            assertThat(recipe.getOpenRewriteRecipeName()).isNotBlank();
        }
    }

    @Test
    @DisplayName("Should handle empty store gracefully")
    void shouldHandleEmptyStoreGracefully() {
        // When
        RecipeSeeder.seedDefaultRecipes(store);
        
        // Then - should not throw exception
        List<RecipeDefinition> recipes = store.getRecipes();
        // The actual number depends on the recipes.json content
        // The important thing is that it doesn't crash
        assertThat(recipes).isNotNull();
    }

    @Test
    @DisplayName("Should verify all loaded recipes have OpenRewrite classes")
    void shouldVerifyAllLoadedRecipesHaveOpenRewriteClasses() {
        // When
        RecipeSeeder.seedDefaultRecipes(store);
        
        // Then
        List<RecipeDefinition> recipes = store.getRecipes();
        
        // Verify that all recipes have OpenRewrite classes set
        for (RecipeDefinition recipe : recipes) {
            assertThat(recipe.getOpenRewriteRecipeName())
                .as("Recipe '%s' should have an OpenRewrite class", recipe.getName())
                .isNotNull();
            assertThat(recipe.getOpenRewriteRecipeName())
                .as("Recipe '%s' should have a non-blank OpenRewrite class", recipe.getName())
                .isNotBlank();
        }
    }

    @Test
    @DisplayName("Should successfully seed upgrade recommendations")
    void shouldSeedUpgradeRecommendations() {
        // When
        RecipeSeeder.seedUpgradeRecommendations(store);
        
        // Then
        List<RecipeDefinition> recipes = store.getRecipes();
        
        // Verify that upgrade recommendations were loaded
        assertThat(recipes).isNotEmpty();
        
        // Verify that all upgrade recommendations have valid names
        for (RecipeDefinition recipe : recipes) {
            assertThat(recipe.getName())
                .as("Upgrade recommendation should have a name")
                .isNotNull();
            assertThat(recipe.getName())
                .as("Upgrade recommendation should have a non-blank name")
                .isNotBlank();
        }
    }

    @Test
    @DisplayName("Should handle upgrade recommendations with missing associatedRecipeName gracefully")
    void shouldHandleMissingAssociatedRecipeNameGracefully() {
        // Given - Set dev mode to ensure validation is enforced
        System.setProperty("jakarta.migration.mode", "dev");
        
        try {
            // When - seed upgrade recommendations
            RecipeSeeder.seedUpgradeRecommendations(store);
            
            // Then - should not throw exception and should only insert valid recipes
            List<RecipeDefinition> recipes = store.getRecipes();
            
            // Verify all recipes have valid names
            for (RecipeDefinition recipe : recipes) {
                assertThat(recipe.getName())
                    .as("Recipe should have a non-null name")
                    .isNotNull();
                assertThat(recipe.getName())
                    .as("Recipe should have a non-blank name")
                    .isNotBlank();
            }
        } finally {
            // Cleanup - Reset system property
            System.clearProperty("jakarta.migration.mode");
        }
    }

    @Test
    @DisplayName("Should handle upgrade recommendations with empty associatedRecipeName gracefully")
    void shouldHandleEmptyAssociatedRecipeNameGracefully() {
        // Given - Set dev mode to ensure validation is enforced
        System.setProperty("jakarta.migration.mode", "dev");

        try {
            // When - seed upgrade recommendations
            RecipeSeeder.seedUpgradeRecommendations(store);

            // Then - should not throw exception and should only insert valid recipes
            List<RecipeDefinition> recipes = store.getRecipes();

            // Verify all recipes have non-blank names
            for (RecipeDefinition recipe : recipes) {
                assertThat(recipe.getName())
                    .as("Recipe should have a non-blank name")
                    .isNotBlank();
            }
        } finally {
            // Cleanup - Reset system property
            System.clearProperty("jakarta.migration.mode");
        }
    }

    @Test
    @DisplayName("Should extract safety field from JSON")
    void shouldExtractSafetyFieldFromJson() throws Exception {
        // When
        RecipeSeeder.seedDefaultRecipes(store);

        // Then - verify safety field is populated in database directly
        java.nio.file.Path dbPath = store.getDbPath();
        try (java.sql.Connection conn = java.sql.DriverManager.getConnection("jdbc:sqlite:" + dbPath);
             java.sql.Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery(
                 "SELECT COUNT(*) FROM recipes WHERE safety IS NOT NULL AND safety != ''")) {

            assertThat(rs.next()).isTrue();
            int count = rs.getInt(1);
            assertThat(count).as("At least some recipes should have safety field populated").isGreaterThan(0);
        }
    }

    @Test
    @DisplayName("Should populate all domain fields from JSON")
    void shouldPopulateAllDomainFieldsFromJson() {
        // When
        RecipeSeeder.seedDefaultRecipes(store);

        // Then - verify critical fields are populated
        List<RecipeDefinition> recipes = store.getRecipes();
        
        for (RecipeDefinition recipe : recipes) {
            assertThat(recipe.getName()).isNotNull();
            assertThat(recipe.getCategory()).isNotNull();
            assertThat(recipe.getRecipeType()).isNotNull();
            // safety can be null for some recipes, but should be populated for most
        }
    }
}
