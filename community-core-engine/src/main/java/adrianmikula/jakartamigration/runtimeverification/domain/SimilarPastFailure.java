package adrianmikula.jakartamigration.runtimeverification.domain;

import java.util.Objects;

/**
 * Represents a similar past failure that may provide insights.
 */
public record SimilarPastFailure(
    String errorPattern,
    String resolution,
    double similarityScore
) {
    public SimilarPastFailure {
        Objects.requireNonNull(errorPattern, "errorPattern cannot be null");
        Objects.requireNonNull(resolution, "resolution cannot be null");
        
        if (similarityScore < 0.0 || similarityScore > 1.0) {
            throw new IllegalArgumentException("similarityScore must be between 0.0 and 1.0");
        }
    }
}

