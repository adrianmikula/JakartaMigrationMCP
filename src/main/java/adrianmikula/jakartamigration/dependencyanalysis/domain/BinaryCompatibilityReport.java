package adrianmikula.jakartamigration.dependencyanalysis.domain;

import java.util.List;

/**
 * Report of binary compatibility analysis between two JAR versions.
 * 
 * Used to compare javax version vs jakarta version of dependencies
 * to detect breaking changes.
 */
public record BinaryCompatibilityReport(
    Artifact oldVersion,
    Artifact newVersion,
    boolean isCompatible,
    List<BreakingChange> breakingChanges,
    String summary
) {
    /**
     * Creates a compatible report (no breaking changes).
     */
    public static BinaryCompatibilityReport compatible(Artifact oldVersion, Artifact newVersion) {
        return new BinaryCompatibilityReport(
            oldVersion,
            newVersion,
            true,
            List.of(),
            "No breaking changes detected between versions"
        );
    }
    
    /**
     * Creates an incompatible report with breaking changes.
     */
    public static BinaryCompatibilityReport incompatible(
        Artifact oldVersion,
        Artifact newVersion,
        List<BreakingChange> breakingChanges
    ) {
        String summary = String.format(
            "Found %d breaking change(s) between %s and %s",
            breakingChanges.size(),
            oldVersion.version(),
            newVersion.version()
        );
        
        return new BinaryCompatibilityReport(
            oldVersion,
            newVersion,
            false,
            breakingChanges,
            summary
        );
    }
}

