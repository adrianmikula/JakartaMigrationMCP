package adrianmikula.jakartamigration.pdfreporting.domain;

import java.util.Map;

/**
 * Represents a section in a PDF report.
 */
public record ReportSection(
    String id,
    String title,
    String description,
    boolean enabled,
    Map<String, Object> properties
) {}
