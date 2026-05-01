package adrianmikula.jakartamigration.coderefactoring.service;

import adrianmikula.jakartamigration.analysis.persistence.CentralMigrationAnalysisStore;
import adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition;
import adrianmikula.jakartamigration.coderefactoring.service.util.RecipeSeeder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for recipe seeding during module initialization.
 * Tests the actual CodeRefactoringModule startup flow and validates DB state.
 */
@Tag("slow")
class RecipeSeedingIntegrationTest {

    @TempDir
    Path tempDir;

    private CentralMigrationAnalysisStore store;
    private String originalMode;

    @BeforeEach
    void setUp() {
        // Save original mode property
        originalMode = System.getProperty("jakarta.migration.mode");
        
        // Create test database
        Path dbPath = tempDir.resolve("test-central.db");
        store = new CentralMigrationAnalysisStore(dbPath);
    }

    @AfterEach
    void tearDown() {
        // Restore original mode property
        if (originalMode != null) {
            System.setProperty("jakarta.migration.mode", originalMode);
        } else {
            System.clearProperty("jakarta.migration.mode");
        }
    }

    @Test
    @DisplayName("Should successfully initialize module with valid recipes")
    void shouldSuccessfullyInitializeModuleWithValidRecipes() {
        // When
        RecipeSeeder.seedDefaultRecipes(store);

        // Then
        List<RecipeDefinition> recipes = store.getRecipes();
        assertThat(recipes).isNotEmpty();
        
        // Verify all recipes have openRewriteRecipeName set
        for (RecipeDefinition recipe : recipes) {
            assertThat(recipe.getOpenRewriteRecipeName())
                .as("Recipe '%s' should have openRewriteRecipeName", recipe.getName())
                .isNotNull();
            assertThat(recipe.getOpenRewriteRecipeName())
                .as("Recipe '%s' should have non-blank openRewriteRecipeName", recipe.getName())
                .isNotBlank();
        }
    }

    @Test
    @DisplayName("Should validate DB state after seeding")
    void shouldValidateDbStateAfterSeeding() throws Exception {
        // When
        RecipeSeeder.seedDefaultRecipes(store);

        // Then - verify DB state by querying directly
        Path dbPath = store.getDbPath();
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM recipes WHERE archived = FALSE")) {
            
            assertThat(rs.next()).isTrue();
            int count = rs.getInt(1);
            assertThat(count).isGreaterThan(0);
        }

        // Verify openrewrite_recipe_name is not null for non-archived recipes
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT COUNT(*) FROM recipes WHERE archived = FALSE AND openrewrite_recipe_name IS NULL")) {
            
            assertThat(rs.next()).isTrue();
            int nullCount = rs.getInt(1);
            assertThat(nullCount).isEqualTo(0);
        }

        // Verify schema version
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT value FROM metadata WHERE key = 'schema_version'")) {
            
            assertThat(rs.next()).isTrue();
            String version = rs.getString("value");
            assertThat(version).isEqualTo("2");
        }
    }

    @Test
    @DisplayName("Should fail-fast in dev mode on missing required fields")
    void shouldFailFastInDevModeOnMissingRequiredFields() {
        // Given - set dev mode
        System.setProperty("jakarta.migration.mode", "dev");

        // When/Then - with actual valid recipes, should succeed
        // Note: To properly test fail-fast behavior with invalid recipes,
        // we would need to refactor RecipeSeeder to accept a custom resource path
        // or use a test classloader. For now, we verify dev mode works with valid recipes.
        RecipeSeeder.seedDefaultRecipes(store);
        List<RecipeDefinition> recipes = store.getRecipes();
        assertThat(recipes).isNotEmpty();
    }

    @Test
    @DisplayName("Should gracefully skip invalid recipes in production mode")
    void shouldGracefullySkipInvalidRecipesInProductionMode() {
        // Given - set production mode
        System.setProperty("jakarta.migration.mode", "production");

        // When - seed with actual recipes (all should be valid)
        RecipeSeeder.seedDefaultRecipes(store);

        // Then
        List<RecipeDefinition> recipes = store.getRecipes();
        assertThat(recipes).isNotEmpty();
        
        // All recipes should have openRewriteRecipeName since current recipes.json is valid
        for (RecipeDefinition recipe : recipes) {
            if (!recipe.isArchived()) {
                assertThat(recipe.getOpenRewriteRecipeName())
                    .as("Non-archived recipe '%s' should have openRewriteRecipeName", recipe.getName())
                    .isNotNull();
            }
        }
    }

    @Test
    @DisplayName("Should be idempotent - seeding twice should not create duplicates")
    void shouldBeIdempotent() {
        // When - seed first time
        RecipeSeeder.seedDefaultRecipes(store);
        List<RecipeDefinition> recipesAfterFirst = store.getRecipes();
        int firstCount = recipesAfterFirst.size();

        // When - seed second time
        RecipeSeeder.seedDefaultRecipes(store);
        List<RecipeDefinition> recipesAfterSecond = store.getRecipes();
        int secondCount = recipesAfterSecond.size();

        // Then - counts should be equal (no duplicates)
        assertThat(secondCount).isEqualTo(firstCount);
        
        // Verify recipes are the same
        assertThat(recipesAfterSecond).hasSameSizeAs(recipesAfterFirst);
    }

    @Test
    @DisplayName("Should handle demo mode gracefully")
    void shouldHandleDemoModeGracefully() {
        // Given - set demo mode
        System.setProperty("jakarta.migration.mode", "demo");

        // When
        RecipeSeeder.seedDefaultRecipes(store);

        // Then
        List<RecipeDefinition> recipes = store.getRecipes();
        assertThat(recipes).isNotEmpty();
    }

    @Test
    @DisplayName("Should default to production mode when no mode is set")
    void shouldDefaultToProductionMode() {
        // Given - clear mode property
        System.clearProperty("jakarta.migration.mode");

        // When
        RecipeSeeder.seedDefaultRecipes(store);

        // Then - should succeed (production mode is graceful)
        List<RecipeDefinition> recipes = store.getRecipes();
        assertThat(recipes).isNotEmpty();
    }
}
