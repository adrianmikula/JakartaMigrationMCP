package adrianmikula.jakartamigration.dependencyanalysis.domain;

/**
 * Migration readiness score (0.0 to 1.0).
 */
public record MigrationReadinessScore(
    double score,
    String explanation
) {
    public MigrationReadinessScore {
        if (score < 0.0 || score > 1.0) {
            throw new IllegalArgumentException("Score must be between 0.0 and 1.0");
        }
    }
}

