package adrianmikula.jakartamigration.coderefactoring.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Domain model for a recipe execution history record.
 * Maps to 'recipe_executions' table.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeExecutionHistory {
    private Long id;
    private String recipeName;
    private Instant executedAt;
    private boolean success;
    private String message;
    private List<String> affectedFiles;

    // For undo support
    private Long undoExecutionId;
    private boolean isUndo;
}
