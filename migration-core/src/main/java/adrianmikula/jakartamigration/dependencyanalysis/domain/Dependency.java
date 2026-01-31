package adrianmikula.jakartamigration.dependencyanalysis.domain;

import java.util.Objects;

/**
 * Represents a dependency relationship between two artifacts.
 */
public record Dependency(
    Artifact from,
    Artifact to,
    String scope,
    boolean optional
) {
    public Dependency {
        Objects.requireNonNull(from, "from artifact cannot be null");
        Objects.requireNonNull(to, "to artifact cannot be null");
        Objects.requireNonNull(scope, "scope cannot be null");
    }
}

