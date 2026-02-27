package adrianmikula.jakartamigration.coderefactoring.domain;

/**
 * Represents a single change made during refactoring.
 */
public record ChangeDetail(
    int lineNumber,
    String originalLine,
    String newLine,
    String description,
    ChangeType type
) {
    public ChangeDetail {
        if (lineNumber < 1) {
            throw new IllegalArgumentException("LineNumber must be >= 1");
        }
        if (originalLine == null) {
            throw new IllegalArgumentException("OriginalLine cannot be null");
        }
        if (newLine == null) {
            throw new IllegalArgumentException("NewLine cannot be null");
        }
        if (description == null) {
            throw new IllegalArgumentException("Description cannot be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("Type cannot be null");
        }
    }
}

