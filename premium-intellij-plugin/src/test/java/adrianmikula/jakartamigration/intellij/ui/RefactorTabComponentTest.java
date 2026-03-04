package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.analysis.persistence.CentralMigrationAnalysisStore;
import adrianmikula.jakartamigration.coderefactoring.domain.Recipe;
import adrianmikula.jakartamigration.intellij.service.MigrationAnalysisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for RefactorTabComponent UI behavior.
 * 
 * These tests verify:
 * 1. MigrationAnalysisService is correctly created and initialized
 * 2. At least 21 recipes are available for the Refactor tab
 * 3. Key OpenRewrite recipes are present (AddJakartaNamespace, UpdateWebXml, etc.)
 * 4. The UI can display all expected recipes
 * 5. Recipe execution history is saved to database
 * 6. Undo state can be checked from database
 */
@DisplayName("RefactorTabComponent UI Tests")
class RefactorTabComponentTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("MigrationAnalysisService should be initialized and load recipes")
    void shouldInitializeMigrationAnalysisService() {
        // Given
        MigrationAnalysisService service = new MigrationAnalysisService();
        
        // When
        List<Recipe> recipes = service.getAvailableRecipes();
        
        // Then
        assertThat(recipes)
            .as("MigrationAnalysisService should be initialized and load recipes")
            .isNotNull();
    }
    
    @Test
    @DisplayName("Refactor tab should have at least 21 recipes available")
    void shouldHaveAtLeast21Recipes() {
        // Given
        MigrationAnalysisService service = new MigrationAnalysisService();
        
        // When
        List<Recipe> recipes = service.getAvailableRecipes();
        
        // Then
        assertThat(recipes)
            .as("Refactor tab should have at least 21 recipes for Jakarta migration")
            .hasSizeGreaterThanOrEqualTo(21);
    }
    
    @Test
    @DisplayName("Refactor tab should include key OpenRewrite recipes")
    void shouldIncludeKeyOpenRewriteRecipes() {
        // Given
        MigrationAnalysisService service = new MigrationAnalysisService();
        
        // When
        List<Recipe> recipes = service.getAvailableRecipes();
        
        // Then - verify all key recipes are present
        assertThat(recipes)
            .as("Should include AddJakartaNamespace recipe")
            .anyMatch(r -> r.name().equals("AddJakartaNamespace"));
        
        assertThat(recipes)
            .as("Should include UpdatePersistenceXml recipe")
            .anyMatch(r -> r.name().equals("UpdatePersistenceXml"));
        
        assertThat(recipes)
            .as("Should include UpdateWebXml recipe")
            .anyMatch(r -> r.name().equals("UpdateWebXml"));
        
        assertThat(recipes)
            .as("Should include MigrateServletApi recipe")
            .anyMatch(r -> r.name().equals("MigrateServletApi"));
        
        assertThat(recipes)
            .as("Should include MigrateJpa recipe")
            .anyMatch(r -> r.name().equals("MigrateJpa"));
        
        assertThat(recipes)
            .as("Should include MigrateCdi recipe")
            .anyMatch(r -> r.name().equals("MigrateCdi"));
    }
    
    @Test
    @DisplayName("All recipes should have valid names and descriptions")
    void shouldHaveValidRecipeMetadata() {
        // Given
        MigrationAnalysisService service = new MigrationAnalysisService();
        
        // When
        List<Recipe> recipes = service.getAvailableRecipes();
        
        // Then
        for (Recipe recipe : recipes) {
            assertThat(recipe.name())
                .as("Recipe name should not be blank for " + recipe)
                .isNotBlank();
            
            assertThat(recipe.description())
                .as("Recipe description should not be null for " + recipe.name())
                .isNotNull();
            
            assertThat(recipe.safety())
                .as("Recipe safety level should not be null for " + recipe.name())
                .isNotNull();
        }
    }
    
    @Test
    @DisplayName("Recipes should have appropriate safety levels")
    void shouldHaveAppropriateSafetyLevels() {
        // Given
        MigrationAnalysisService service = new MigrationAnalysisService();
        
        // When
        List<Recipe> recipes = service.getAvailableRecipes();
        
        // Then - verify key recipes have HIGH safety level (recommended for automated migration)
        assertThat(recipes)
            .as("AddJakartaNamespace should have HIGH safety level")
            .anyMatch(r -> r.name().equals("AddJakartaNamespace") && 
                          r.safety().toString().equals("HIGH"));
        
        assertThat(recipes)
            .as("UpdatePersistenceXml should have HIGH safety level")
            .anyMatch(r -> r.name().equals("UpdatePersistenceXml") && 
                          r.safety().toString().equals("HIGH"));
        
        assertThat(recipes)
            .as("MigrateServletApi should have HIGH safety level")
            .anyMatch(r -> r.name().equals("MigrateServletApi") && 
                          r.safety().toString().equals("HIGH"));
    }
    
    @Test
    @DisplayName("Should be able to get recipe by name from service")
    void shouldGetRecipeByName() {
        // Given
        MigrationAnalysisService service = new MigrationAnalysisService();
        
        // When & Then
        var addJakarta = service.getAvailableRecipes().stream()
            .filter(r -> r.name().equals("AddJakartaNamespace"))
            .findFirst();
        
        assertThat(addJakarta)
            .as("Should be able to find AddJakartaNamespace recipe")
            .isPresent();
        
        assertThat(addJakarta.get().description())
            .as("AddJakartaNamespace description should contain javax to jakarta")
            .contains("jakarta");
    }
    
    @Test
    @DisplayName("getJakartaRecipes should return Jakarta-specific recipes")
    void shouldGetJakartaRecipes() {
        // Given
        MigrationAnalysisService service = new MigrationAnalysisService();
        
        // When
        List<Recipe> jakartaRecipes = service.getJakartaRecipes();
        
        // Then
        assertThat(jakartaRecipes)
            .as("Should have at least some Jakarta-specific recipes")
            .isNotEmpty();
    }
    
    @Test
    @DisplayName("All registered recipes should be retrievable")
    void shouldRetrieveAllRegisteredRecipes() {
        // Given
        MigrationAnalysisService service = new MigrationAnalysisService();
        
        // When
        List<Recipe> allRecipes = service.getAvailableRecipes();
        int totalCount = allRecipes.size();
        
        // Then - verify we can get each recipe by name
        for (Recipe recipe : allRecipes) {
            var retrieved = service.getAvailableRecipes().stream()
                .filter(r -> r.name().equals(recipe.name()))
                .findFirst();
            
            assertThat(retrieved)
                .as("Should be able to retrieve recipe: " + recipe.name())
                .isPresent();
        }
        
        assertThat(totalCount)
            .as("Total recipe count should be at least 21")
            .isGreaterThanOrEqualTo(21);
    }

    @Test
    @DisplayName("Recipe execution should be saved to database after successful apply")
    void shouldSaveRecipeExecutionToDatabase() {
        // Given
        CentralMigrationAnalysisStore store = new CentralMigrationAnalysisStore(tempDir.resolve("test.db"));
        String repoPath = tempDir.resolve("my-project").toString();
        List<String> affectedFiles = List.of("src/main/java/com/example/MyServlet.java", "pom.xml");

        // When - simulate what happens when a recipe is applied
        store.saveRecipeExecution(repoPath, "AddJakartaNamespace", true, "Applied successfully - 2 file(s) changed", affectedFiles);

        // Then - verify execution was saved
        var executions = store.getRecipeExecutions(repoPath, "AddJakartaNamespace");
        assertThat(executions).hasSize(1);
        assertThat(executions.get(0).get("success")).isEqualTo(true);
        assertThat((String) executions.get(0).get("message")).contains("Applied successfully");
    }

    @Test
    @DisplayName("Undo state should be available when recipe has affected files in database")
    void shouldCheckUndoStateFromDatabase() {
        // Given
        CentralMigrationAnalysisStore store = new CentralMigrationAnalysisStore(tempDir.resolve("test.db"));
        String repoPath = tempDir.resolve("my-project").toString();
        
        // First, save a recipe execution with affected files
        store.saveRecipeExecution(repoPath, "MigrateServletApi", true, "Success", List.of("file1.java", "file2.java"));
        
        // When - query for undo state
        var executions = store.getRecipeExecutions(repoPath, "MigrateServletApi");
        
        // Then - verify we can determine undo is available
        boolean hasUndo = false;
        for (Map<String, Object> exec : executions) {
            Boolean success = (Boolean) exec.get("success");
            String files = (String) exec.get("affected_files");
            if (Boolean.TRUE.equals(success) && files != null && !files.isEmpty()) {
                hasUndo = true;
                break;
            }
        }
        assertThat(hasUndo).isTrue();
    }

    @Test
    @DisplayName("Undo state should not be available when recipe failed")
    void shouldNotHaveUndoStateForFailedRecipe() {
        // Given
        CentralMigrationAnalysisStore store = new CentralMigrationAnalysisStore(tempDir.resolve("test.db"));
        String repoPath = tempDir.resolve("my-project").toString();
        
        // Save a failed recipe execution
        store.saveRecipeExecution(repoPath, "MigrateJpa", false, "Build failed", null);
        
        // When
        var executions = store.getRecipeExecutions(repoPath, "MigrateJpa");
        
        // Then - verify undo is NOT available for failed recipes
        boolean hasUndo = false;
        for (Map<String, Object> exec : executions) {
            Boolean success = (Boolean) exec.get("success");
            String files = (String) exec.get("affected_files");
            if (Boolean.TRUE.equals(success) && files != null && !files.isEmpty()) {
                hasUndo = true;
                break;
            }
        }
        assertThat(hasUndo).isFalse();
    }

    @Test
    @DisplayName("Should retrieve all recipe executions for history tab")
    void shouldGetAllRecipeExecutionsForHistory() {
        // Given
        CentralMigrationAnalysisStore store = new CentralMigrationAnalysisStore(tempDir.resolve("test.db"));
        String repoPath = tempDir.resolve("my-project").toString();
        
        // Save multiple executions
        store.saveRecipeExecution(repoPath, "AddJakartaNamespace", true, "Success 1", List.of("file1.java"));
        store.saveRecipeExecution(repoPath, "MigrateJpa", true, "Success 2", List.of("file2.java"));
        store.saveRecipeExecution(repoPath, "MigrateCdi", false, "Failed", null);
        
        // When
        var allExecutions = store.getAllRecipeExecutions(repoPath);
        
        // Then
        assertThat(allExecutions).hasSize(3);
        
        // Verify all expected recipes are present
        var recipeNames = allExecutions.stream().map(e -> (String) e.get("recipe_name")).toList();
        assertThat(recipeNames).contains("AddJakartaNamespace", "MigrateJpa", "MigrateCdi");
    }
}
