package adrianmikula.jakartamigration.advancedscanning.domain;

/**
 * Represents a javax.validation.* constraint annotation usage found in source code.
 */
public record BeanValidationUsage(
    String annotationName,
    String jakartaEquivalent,
    int lineNumber,
    String elementName,
    String context
) {
    /**
     * Creates a new Bean Validation annotation usage.
     */
    public BeanValidationUsage {
        if (annotationName == null || annotationName.isBlank()) {
            throw new IllegalArgumentException("annotationName cannot be null or blank");
        }
    }

    /**
     * Returns true if this annotation has a Jakarta EE equivalent.
     */
    public boolean hasJakartaEquivalent() {
        return jakartaEquivalent != null && !jakartaEquivalent.isBlank();
    }
}
