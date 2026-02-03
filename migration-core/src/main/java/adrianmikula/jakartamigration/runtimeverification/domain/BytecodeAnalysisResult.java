package adrianmikula.jakartamigration.runtimeverification.domain;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Result of bytecode analysis for Jakarta migration verification.
 */
public record BytecodeAnalysisResult(
    Set<String> javaxClasses,
    Set<String> jakartaClasses,
    Set<String> mixedNamespaceClasses,
    List<RuntimeError> potentialErrors,
    List<Warning> warnings,
    long analysisTimeMs,
    int classesAnalyzed
) {
    public BytecodeAnalysisResult {
        Objects.requireNonNull(javaxClasses, "javaxClasses cannot be null");
        Objects.requireNonNull(jakartaClasses, "jakartaClasses cannot be null");
        Objects.requireNonNull(mixedNamespaceClasses, "mixedNamespaceClasses cannot be null");
        Objects.requireNonNull(potentialErrors, "potentialErrors cannot be null");
        Objects.requireNonNull(warnings, "warnings cannot be null");
        
        if (analysisTimeMs < 0) {
            throw new IllegalArgumentException("analysisTimeMs cannot be negative");
        }
        if (classesAnalyzed < 0) {
            throw new IllegalArgumentException("classesAnalyzed cannot be negative");
        }
    }
    
    /**
     * Returns true if any Jakarta migration issues were found.
     */
    public boolean hasIssues() {
        return !javaxClasses.isEmpty() || 
               !mixedNamespaceClasses.isEmpty() || 
               !potentialErrors.isEmpty() ||
               !warnings.isEmpty();
    }
}

