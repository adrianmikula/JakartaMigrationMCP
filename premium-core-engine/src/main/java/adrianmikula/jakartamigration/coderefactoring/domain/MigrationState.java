package adrianmikula.jakartamigration.coderefactoring.domain;

/**
 * Represents the current state of the migration process.
 */
public enum MigrationState {
    NOT_STARTED,
    IN_PROGRESS,
    PHASE_1_COMPLETE,
    PHASE_2_COMPLETE,
    PHASE_3_COMPLETE,
    PHASE_4_COMPLETE,
    VERIFIED,
    COMPLETE,
    FAILED,
    ROLLED_BACK
}

