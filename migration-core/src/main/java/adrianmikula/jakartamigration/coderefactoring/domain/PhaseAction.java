/*
 * Copyright 2024 Adrian Kozak
 * Copyright 2024 Prairie Trail Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
