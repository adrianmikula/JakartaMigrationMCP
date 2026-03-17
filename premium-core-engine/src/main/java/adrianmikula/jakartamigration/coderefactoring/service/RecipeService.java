package adrianmikula.jakartamigration.coderefactoring.service;

import adrianmikula.jakartamigration.coderefactoring.domain.RecipeCategory;
import adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition;
import adrianmikula.jakartamigration.coderefactoring.domain.RecipeExecutionHistory;
import adrianmikula.jakartamigration.coderefactoring.domain.RecipeExecutionResult;

import java.nio.file.Path;
import java.util.List;

/**
 * Service for managing and executing migration recipes.
 */
public interface RecipeService {

    /**
     * Gets all available recipes.
     */
    List<RecipeDefinition> getRecipes();

    /**
     * Gets recipes by category.
     */
    List<RecipeDefinition> getRecipesByCategory(RecipeCategory category);

    /**
     * Applies a recipe to the project.
     */
    RecipeExecutionResult applyRecipe(String recipeName, Path projectPath);

    /**
     * Undoes a previous recipe execution.
     */
    RecipeExecutionResult undoRecipe(Long executionId, Path projectPath);

    /**
     * Gets execution history for a project.
     */
    List<RecipeExecutionHistory> getHistory(Path projectPath);
}
