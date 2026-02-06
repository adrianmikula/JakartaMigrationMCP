package adrianmikula.jakartamigration.runtimeverification.domain;

import java.util.List;
import java.util.Objects;

/**
 * Result of class loader instrumentation analysis.
 */
public record ClassLoaderAnalysisResult(
    List<String> loadedClasses,
    List<String> failedLoads,
    List<String> namespaceConflicts,
    String summary
) {
    public ClassLoaderAnalysisResult {
        Objects.requireNonNull(loadedClasses, "loadedClasses cannot be null");
        Objects.requireNonNull(failedLoads, "failedLoads cannot be null");
        Objects.requireNonNull(namespaceConflicts, "namespaceConflicts cannot be null");
        Objects.requireNonNull(summary, "summary cannot be null");
    }
}

