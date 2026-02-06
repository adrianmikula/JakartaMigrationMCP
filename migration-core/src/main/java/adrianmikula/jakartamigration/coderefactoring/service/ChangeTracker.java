/*
 * Copyright 2024 Adrian Kozak
 * Copyright 2024 Prairie Trail Software
 *
 * This software is proprietary and may not be used, copied, modified,
 * or distributed except under the terms of a separate commercial license agreement.
 */
package adrianmikula.jakartamigration.coderefactoring.service;

import java.nio.file.Path;
import java.util.List;

/**
 * Tracks changes made during refactoring.
 * 
 * NOTE: This is a stub. Full implementation with OpenRewrite-based
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
        // Premium feature - no-op in stub implementation
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
