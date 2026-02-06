/*
 * Copyright 2024 Adrian Kozak
 * Copyright 2024 Prairie Trail Software
 *
 * This software is proprietary and may not be used, copied, modified,
 * or distributed except under the terms of a separate commercial license agreement.
 */
package adrianmikula.jakartamigration.coderefactoring.domain;

import java.util.List;

/**
 * Represents a specific action to be taken on a file during a migration phase.
 * 
 * NOTE: This is a community stub. Full implementation with migration actions
 * is available in the premium edition.
 */
public record PhaseAction(
    String filePath,
    String actionType,
    List<String> specificChanges
) {
    public PhaseAction {
        if (filePath == null) {
            filePath = "";
        }
        if (actionType == null) {
            actionType = "UNKNOWN";
        }
        if (specificChanges == null) {
            specificChanges = List.of();
        }
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
