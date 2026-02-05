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
package adrianmikula.jakartamigration.coderefactoring.service;

import java.nio.file.Path;
import java.util.List;

/**
 * Tracks changes made during refactoring.
 * 
 * NOTE: This is a community stub. Full implementation with OpenRewrite-based
 * change tracking is available in the premium edition.
 */
public class ChangeTracker {
    
    /**
     * Records a change to a file.
     * 
     * @param filePath Path to the changed file
     * @param description Description of the change
     */
    public void recordChange(Path filePath, String description) {
        // Premium feature - no-op in community edition
    }
    
    /**
     * Gets all recorded changes.
     * 
     * @return List of change descriptions
     */
    public java.util.List<String> getChanges() {
        return List.of();
    }
    
    /**
     * Creates a checkpoint for rollback.
     * 
     * @return Checkpoint ID
     */
    public String createCheckpoint() {
        return "";
    }
    
    /**
     * Rolls back to a checkpoint.
     * 
     * @param checkpointId Checkpoint ID to rollback to
     * @return true if rollback was successful
     */
    public boolean rollbackToCheckpoint(String checkpointId) {
        return false;
    }
}
