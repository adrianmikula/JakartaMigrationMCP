package adrianmikula.jakartamigration.advancedscanning.domain;

/**
 * Represents a javax.persistence.* annotation usage found in source code.
 */
public record JpaAnnotationUsage(
    String annotationName,
    String jakartaEquivalent,
    int lineNumber,
    String elementName,
    String context
) {
    /**
     * Creates a new JPA annotation usage.
     */
    public JpaAnnotationUsage {
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
