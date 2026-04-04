package adrianmikula.jakartamigration.coderefactoring.service.impl;

import adrianmikula.jakartamigration.analysis.persistence.CentralMigrationAnalysisStore;
import adrianmikula.jakartamigration.analysis.persistence.SqliteMigrationAnalysisStore;
import adrianmikula.jakartamigration.coderefactoring.domain.RecipeCategory;
import adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition;
import adrianmikula.jakartamigration.coderefactoring.domain.RecipeExecutionHistory;
import adrianmikula.jakartamigration.coderefactoring.domain.RecipeExecutionResult;
import adrianmikula.jakartamigration.coderefactoring.domain.RecipeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecipeServiceImplTest {

    @TempDir
    Path tempDir;

    private CentralMigrationAnalysisStore centralStore;
    private SqliteMigrationAnalysisStore projectStore;
    private RecipeServiceImpl recipeService;
    private Path projectPath;

    @BeforeEach
    void setUp() throws IOException {
        projectPath = tempDir.resolve("my-project");
        Files.createDirectories(projectPath);

        centralStore = new CentralMigrationAnalysisStore(tempDir.resolve("central.db"));
        projectStore = new SqliteMigrationAnalysisStore(tempDir.resolve("project.db"));
        recipeService = new RecipeServiceImpl(centralStore, projectStore);

        seedInitialRecipes();
    }

    private void seedInitialRecipes() {
        // Regex recipe
        RecipeDefinition regexRecipe = RecipeDefinition.builder()
                .name("ReplaceJavaxServletRegex")
                .description("Replaces javax.servlet with jakarta.servlet using regex")
                .category(RecipeCategory.WEB)
                .recipeType(RecipeType.REGEX)
                .pattern("import javax\\.servlet\\.")
                .replacement("import jakarta.servlet.")
                .filePattern("**/*.java")
                .reversible(true)
                .build();
        centralStore.saveRecipe(regexRecipe);

        // OpenRewrite recipe
        RecipeDefinition orRecipe = RecipeDefinition.builder()
                .name("MigrateJavaxToJakarta")
                .description("Full Jakarta EE migration using OpenRewrite")
                .category(RecipeCategory.OTHER)
                .recipeType(RecipeType.OPENREWRITE)
                .openRewriteRecipeName("org.openrewrite.java.migrate.jakarta.JavaxMigrationToJakarta")
                .reversible(true)
                .build();
        centralStore.saveRecipe(orRecipe);
    }

    @Test
    @DisplayName("Should list all seeded recipes")
    void shouldListRecipes() {
        List<RecipeDefinition> recipes = recipeService.getRecipes(projectPath);
        assertThat(recipes).hasSize(2);
        assertThat(recipes).anyMatch(r -> r.getName().equals("ReplaceJavaxServletRegex"));
    }

    @Test
    @DisplayName("Should apply regex recipe and record history")
    void shouldApplyRegexRecipe() throws IOException {
        // Given
        Path javaFile = projectPath.resolve("Test.java");
        Files.writeString(javaFile, "import javax.servlet.http.HttpServlet;\npublic class Test {}");

        // When
        RecipeExecutionResult result = recipeService.applyRecipe("ReplaceJavaxServletRegex", projectPath);

        // Then
        assertThat(result.success()).isTrue();
        assertThat(result.filesChanged()).isEqualTo(1);
        assertThat(Files.readString(javaFile)).contains("import jakarta.servlet.http.HttpServlet;");

        // Verify history
        List<RecipeExecutionHistory> history = recipeService.getHistory(projectPath);
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getRecipeName()).isEqualTo("ReplaceJavaxServletRegex");
        assertThat(history.get(0).getAffectedFiles()).contains("Test.java");
    }

    @Test
    @DisplayName("Should undo regex recipe")
    void shouldUndoRegexRecipe() throws IOException {
        // Given
        Path javaFile = projectPath.resolve("Test.java");
        String originalContent = "import javax.servlet.http.HttpServlet;\npublic class Test {}";
        Files.writeString(javaFile, originalContent);

        RecipeExecutionResult applyResult = recipeService.applyRecipe("ReplaceJavaxServletRegex", projectPath);
        assertThat(applyResult.success()).isTrue();

        // When
        RecipeExecutionResult undoResult = recipeService.undoRecipe(applyResult.executionId(), projectPath);

        // Then
        assertThat(undoResult.success()).isTrue();
        assertThat(Files.readString(javaFile)).isEqualTo(originalContent);

        // Verify history has undo record
        List<RecipeExecutionHistory> history = recipeService.getHistory(projectPath);
        assertThat(history).hasSize(2); // One for apply, one for undo
        assertThat(history.get(0).isUndo()).isTrue();
        assertThat(history.get(1).getUndoExecutionId()).isNotNull();
    }

    @Test
    @DisplayName("Should return failure if recipe not found")
    void shouldReturnFailureIfRecipeNotFound() {
        RecipeExecutionResult result = recipeService.applyRecipe("UnknownRecipe", projectPath);
        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("Recipe not found");
    }
}
