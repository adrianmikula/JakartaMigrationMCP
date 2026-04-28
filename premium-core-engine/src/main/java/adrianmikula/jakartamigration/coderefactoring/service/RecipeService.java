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
     * Gets all available recipes with project-specific status.
     */
    List<RecipeDefinition> getRecipes(Path projectPath);

    /**
     * Gets recipes by category with project-specific status.
     */
    List<RecipeDefinition> getRecipesByCategory(RecipeCategory category, Path projectPath);

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

    /**
     * Checks if a recipe is archived (no longer available for new executions).
     * Used by History tab to determine if undo is available.
     */
    boolean isRecipeArchived(String recipeName);
}
