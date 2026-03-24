package adrianmikula.jakartamigration.pdfreporting.domain;

/**
 * Represents a validation error in PDF report generation.
 */
public record ValidationError(
    String field,
    String message,
    String suggestion
) {}
