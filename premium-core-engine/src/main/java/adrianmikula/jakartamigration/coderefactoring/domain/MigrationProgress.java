package adrianmikula.jakartamigration.coderefactoring.domain;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Current progress of the migration process.
 */
public record MigrationProgress(
    MigrationState currentState,
    int currentPhase,
    ProgressStatistics statistics,
    List<Checkpoint> checkpoints,
    LocalDateTime lastUpdate
) {
    public MigrationProgress {
        if (currentState == null) {
            throw new IllegalArgumentException("CurrentState cannot be null");
        }
        if (currentPhase < 0) {
            throw new IllegalArgumentException("CurrentPhase cannot be negative");
        }
        if (statistics == null) {
            throw new IllegalArgumentException("Statistics cannot be null");
        }
        if (checkpoints == null) {
            throw new IllegalArgumentException("Checkpoints cannot be null");
        }
        if (lastUpdate == null) {
            throw new IllegalArgumentException("LastUpdate cannot be null");
        }
    }
    
    /**
     * Returns true if migration is complete.
     */
    public boolean isComplete() {
        return currentState == MigrationState.COMPLETE;
    }
    
    /**
     * Returns true if migration is in progress.
     */
    public boolean isInProgress() {
        return currentState == MigrationState.IN_PROGRESS;
    }
}

