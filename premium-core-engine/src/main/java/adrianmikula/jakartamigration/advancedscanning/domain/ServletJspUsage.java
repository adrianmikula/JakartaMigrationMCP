package adrianmikula.jakartamigration.advancedscanning.domain;

/**
 * Represents a javax.servlet.*, javax.servlet.jsp.*, or EL usage found in source code.
 */
public record ServletJspUsage(
    String className,
    String jakartaEquivalent,
    int lineNumber,
    String context,
    String usageType // servlet, jsp, el, listener, filter
) {
    /**
     * Creates a new Servlet/JSP usage.
     */
    public ServletJspUsage {
        if (className == null || className.isBlank()) {
            throw new IllegalArgumentException("className cannot be null or blank");
        }
    }

    /**
     * Returns true if this has a Jakarta EE equivalent.
     */
    public boolean hasJakartaEquivalent() {
        return jakartaEquivalent != null && !jakartaEquivalent.isBlank();
    }
}
