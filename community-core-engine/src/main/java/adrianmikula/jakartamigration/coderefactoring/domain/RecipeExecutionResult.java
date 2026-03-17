package adrianmikula.jakartamigration.coderefactoring.domain;

import java.util.List;

/**
 * Result of a recipe execution.
 */
public record RecipeExecutionResult(
        boolean success,
        int filesProcessed,
        int filesChanged,
        List<String> changedFilePaths,
        String errorMessage,
        Long executionId) {
}
