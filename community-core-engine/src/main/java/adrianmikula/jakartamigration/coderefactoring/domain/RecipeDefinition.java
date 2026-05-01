package adrianmikula.jakartamigration.coderefactoring.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Domain model for a migration recipe definition.
 * Maps to the 'recipes' table in the central database.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeDefinition {
    private Long id;
    private String name;
    private String description;
    private RecipeCategory category;
    private RecipeType recipeType;

    // For OPENREWRITE type
    private String openRewriteRecipeName;

    // For REGEX type
    private String pattern;
    private String replacement;
    private String filePattern;

    private boolean reversible;
    private Instant createdAt;

    // Recipe versioning and archival
    private String addedInPluginVersion;
    private boolean archived;

    // Status and History (computed or loaded from execution history)
    private Instant lastRunDate;
    private RecipeStatus status;

    public enum RecipeStatus {
        NEVER_RUN,
        RUN_SUCCESS,
        RUN_UNDONE,
        RUN_FAILED
    }
}
