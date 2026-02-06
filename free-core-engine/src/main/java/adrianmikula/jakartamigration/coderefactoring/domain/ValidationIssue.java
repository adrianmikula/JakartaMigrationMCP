package adrianmikula.jakartamigration.coderefactoring.domain;

/**
 * Represents a validation issue found during code validation.
 */
public record ValidationIssue(
    int lineNumber,
    String message,
    ValidationSeverity severity,
    String suggestion
) {
    public ValidationIssue {
        if (lineNumber < 0) {
            throw new IllegalArgumentException("LineNumber cannot be negative");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message cannot be null or blank");
        }
        if (severity == null) {
            throw new IllegalArgumentException("Severity cannot be null");
        }
        if (suggestion == null) {
            throw new IllegalArgumentException("Suggestion cannot be null");
        }
    }
}

