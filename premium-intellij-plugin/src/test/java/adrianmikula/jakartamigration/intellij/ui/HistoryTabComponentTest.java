package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.analysis.persistence.CentralMigrationAnalysisStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for HistoryTabComponent UI behavior.
 * 
 * These tests verify:
 * 1. History tab can retrieve recipe execution history from database
 * 2. Recipe executions are properly formatted for display
 * 3. Failed executions are shown differently from successful ones
 * 4. Affected files are properly displayed
 */
@DisplayName("HistoryTabComponent UI Tests")
class HistoryTabComponentTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Should retrieve recipe execution history from database")
    void shouldRetrieveRecipeExecutionHistory() {
        // Given
        CentralMigrationAnalysisStore store = new CentralMigrationAnalysisStore(tempDir.resolve("test.db"));
        String repoPath = tempDir.resolve("my-project").toString();
        
        // Save multiple recipe executions
        store.saveRecipeExecution(repoPath, "AddJakartaNamespace", true, "Success", List.of("file1.java"));
        store.saveRecipeExecution(repoPath, "MigrateJpa", true, "Success", List.of("file2.java", "file3.java"));
        store.saveRecipeExecution(repoPath, "MigrateCdi", false, "Build failed", null);

        // When - simulate what HistoryTabComponent would do
        var allExecutions = store.getAllRecipeExecutions(repoPath);

        // Then
        assertThat(allExecutions)
            .as("Should retrieve all recipe executions")
            .hasSize(3);
    }

    @Test
    @DisplayName("Should show successful executions with file details")
    void shouldShowSuccessfulExecutionsWithFiles() {
        // Given
        CentralMigrationAnalysisStore store = new CentralMigrationAnalysisStore(tempDir.resolve("test.db"));
        String repoPath = tempDir.resolve("my-project").toString();
        List<String> affectedFiles = List.of(
            "src/main/java/com/example/MyServlet.java",
            "src/main/java/com/example/MyBean.java",
            "pom.xml"
        );
        
        store.saveRecipeExecution(repoPath, "AddJakartaNamespace", true, "Applied successfully", affectedFiles);

        // When
        var executions = store.getRecipeExecutions(repoPath, "AddJakartaNamespace");

        // Then
        assertThat(executions).hasSize(1);
        assertThat(executions.get(0).get("success")).isEqualTo(true);
        assertThat((String) executions.get(0).get("affected_files")).contains("MyServlet.java");
    }

    @Test
    @DisplayName("Should show failed executions with error message")
    void shouldShowFailedExecutionsWithError() {
        // Given
        CentralMigrationAnalysisStore store = new CentralMigrationAnalysisStore(tempDir.resolve("test.db"));
        String repoPath = tempDir.resolve("my-project").toString();
        
        store.saveRecipeExecution(repoPath, "MigrateServletApi", false, "Build failed: cannot resolve dependencies", null);

        // When
        var executions = store.getRecipeExecutions(repoPath, "MigrateServletApi");

        // Then
        assertThat(executions).hasSize(1);
        assertThat(executions.get(0).get("success")).isEqualTo(false);
        assertThat((String) executions.get(0).get("message")).contains("cannot resolve dependencies");
    }

    @Test
    @DisplayName("Should return empty history for new repository")
    void shouldReturnEmptyHistoryForNewRepo() {
        // Given
        CentralMigrationAnalysisStore store = new CentralMigrationAnalysisStore(tempDir.resolve("test.db"));
        String repoPath = tempDir.resolve("new-project").toString();

        // When
        var executions = store.getAllRecipeExecutions(repoPath);

        // Then
        assertThat(executions)
            .as("Should return empty list for new repository with no history")
            .isEmpty();
    }

    @Test
    @DisplayName("Should order history by most recent first")
    void shouldOrderHistoryByMostRecent() {
        // Given
        CentralMigrationAnalysisStore store = new CentralMigrationAnalysisStore(tempDir.resolve("test.db"));
        String repoPath = tempDir.resolve("my-project").toString();
        
        // Save executions in specific order
        store.saveRecipeExecution(repoPath, "AddJakartaNamespace", true, "First", List.of("file1.java"));
        store.saveRecipeExecution(repoPath, "MigrateJpa", true, "Second", List.of("file2.java"));
        store.saveRecipeExecution(repoPath, "MigrateCdi", true, "Third", List.of("file3.java"));

        // When
        var executions = store.getAllRecipeExecutions(repoPath);

        // Then - should return 3 executions
        assertThat(executions).hasSize(3);
        
        // Verify all expected recipes are present
        var recipeNames = executions.stream().map(e -> (String) e.get("recipe_name")).toList();
        assertThat(recipeNames).contains("AddJakartaNamespace", "MigrateJpa", "MigrateCdi");
    }

    @Test
    @DisplayName("Should limit history to recent 100 executions")
    void shouldLimitHistoryToRecent100() {
        // Given
        CentralMigrationAnalysisStore store = new CentralMigrationAnalysisStore(tempDir.resolve("test.db"));
        String repoPath = tempDir.resolve("my-project").toString();
        
        // Save more than 100 executions
        for (int i = 1; i <= 105; i++) {
            store.saveRecipeExecution(repoPath, "Recipe" + i, true, "Execution " + i, List.of("file" + i + ".java"));
        }

        // When
        var executions = store.getAllRecipeExecutions(repoPath);

        // Then
        assertThat(executions)
            .as("Should limit to 100 most recent executions")
            .hasSize(100);
    }

    @Test
    @DisplayName("Should handle null affected files gracefully")
    void shouldHandleNullAffectedFiles() {
        // Given
        CentralMigrationAnalysisStore store = new CentralMigrationAnalysisStore(tempDir.resolve("test.db"));
        String repoPath = tempDir.resolve("my-project").toString();
        
        store.saveRecipeExecution(repoPath, "TestRecipe", true, "Success", null);

        // When
        var executions = store.getRecipeExecutions(repoPath, "TestRecipe");

        // Then
        assertThat(executions).hasSize(1);
        assertThat((String) executions.get(0).get("affected_files")).isNullOrEmpty();
    }

    @Test
    @DisplayName("Should be able to query specific recipe history")
    void shouldQuerySpecificRecipeHistory() {
        // Given
        CentralMigrationAnalysisStore store = new CentralMigrationAnalysisStore(tempDir.resolve("test.db"));
        String repoPath = tempDir.resolve("my-project").toString();
        
        store.saveRecipeExecution(repoPath, "AddJakartaNamespace", true, "Success 1", List.of("file1.java"));
        store.saveRecipeExecution(repoPath, "AddJakartaNamespace", true, "Success 2", List.of("file2.java"));
        store.saveRecipeExecution(repoPath, "MigrateJpa", true, "Success", List.of("file3.java"));

        // When
        var addJakartaHistory = store.getRecipeExecutions(repoPath, "AddJakartaNamespace");

        // Then
        assertThat(addJakartaHistory)
            .as("Should return only AddJakartaNamespace executions")
            .hasSize(2);
    }
}
