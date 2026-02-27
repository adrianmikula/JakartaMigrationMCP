package adrianmikula.jakartamigration.coderefactoring.domain;

import java.util.List;
import java.util.Objects;

/**
 * Represents a specific action to be taken on a file during a migration phase.
 */
public record PhaseAction(
    String filePath,
    String actionType,
    List<String> specificChanges
) {
    public PhaseAction {
        Objects.requireNonNull(filePath, "filePath cannot be null");
        Objects.requireNonNull(actionType, "actionType cannot be null");
        Objects.requireNonNull(specificChanges, "specificChanges cannot be null");
    }
    
    /**
     * Action types for migration phases.
     */
    public enum ActionType {
        UPDATE_IMPORTS,
        UPDATE_PACKAGE,
        UPDATE_XML_NAMESPACE,
        UPDATE_DEPENDENCY,
        UPDATE_CLASS_REFERENCES
    }
}

