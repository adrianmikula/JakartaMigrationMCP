package adrianmikula.jakartamigration.dependencyanalysis.domain;

import java.util.List;

/**
 * Report of transitive dependency conflicts.
 */
public record TransitiveConflictReport(
    List<TransitiveConflict> conflicts,
    int totalConflicts,
    String summary
) {}

