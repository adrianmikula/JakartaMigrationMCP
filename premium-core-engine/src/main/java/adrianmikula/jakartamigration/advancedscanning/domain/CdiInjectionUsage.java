package adrianmikula.jakartamigration.advancedscanning.domain;

/**
 * Represents a javax.inject or javax.enterprise usage found in source code.
 */
public record CdiInjectionUsage(
    String className,
    String jakartaEquivalent,
    int lineNumber,
    String context,
    String usageType // annotation, import, interface
) {
    public CdiInjectionUsage {
        if (className == null || className.isBlank()) {
            throw new IllegalArgumentException("className cannot be null or blank");
        }
    }

    public boolean hasJakartaEquivalent() {
        return jakartaEquivalent != null && !jakartaEquivalent.isBlank();
    }
}
