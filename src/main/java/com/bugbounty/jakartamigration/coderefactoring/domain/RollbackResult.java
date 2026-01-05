package com.bugbounty.jakartamigration.coderefactoring.domain;

/**
 * Result of a rollback operation.
 */
public record RollbackResult(
    boolean success,
    String filePath,
    String checkpointId,
    String message,
    RollbackStatus status
) {
    public RollbackResult {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("FilePath cannot be null or blank");
        }
        if (checkpointId == null || filePath.isBlank()) {
            throw new IllegalArgumentException("CheckpointId cannot be null or blank");
        }
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
    }
    
    /**
     * Creates a successful rollback result.
     */
    public static RollbackResult success(String filePath, String checkpointId) {
        return new RollbackResult(
            true,
            filePath,
            checkpointId,
            "Rollback completed successfully",
            RollbackStatus.SUCCESS
        );
    }
    
    /**
     * Creates a failed rollback result.
     */
    public static RollbackResult failure(String filePath, String checkpointId, String reason) {
        return new RollbackResult(
            false,
            filePath,
            checkpointId,
            reason,
            RollbackStatus.FAILED
        );
    }
}

