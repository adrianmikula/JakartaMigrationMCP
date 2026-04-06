package adrianmikula.jakartamigration.pdfreporting.domain;

import java.util.List;
import java.util.Map;

/**
 * Represents a report template configuration.
 */
public record ReportTemplate(
    String name,
    String description,
    List<ReportSection> sections,
    Map<String, Object> metadata
) {}
