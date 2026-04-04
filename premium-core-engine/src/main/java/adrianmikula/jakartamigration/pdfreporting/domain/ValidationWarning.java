package adrianmikula.jakartamigration.pdfreporting.domain;

/**
 * Represents a validation warning in PDF report generation.
 */
public record ValidationWarning(
    String field,
    String message
) {}
