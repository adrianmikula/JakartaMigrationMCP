package adrianmikula.jakartamigration.coderefactoring.domain;

import java.time.LocalDateTime;

/**
 * Represents a checkpoint in the migration process for rollback purposes.
 */
public record Checkpoint(
    String checkpointId,
    String filePath,
    LocalDateTime timestamp,
    String description
) {
    public Checkpoint {
        if (checkpointId == null || checkpointId.isBlank()) {
            throw new IllegalArgumentException("CheckpointId cannot be null or blank");
        }
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("FilePath cannot be null or blank");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
        if (description == null) {
            throw new IllegalArgumentException("Description cannot be null");
        }
    }
}

