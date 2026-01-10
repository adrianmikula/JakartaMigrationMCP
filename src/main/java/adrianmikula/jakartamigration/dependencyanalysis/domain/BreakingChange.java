package adrianmikula.jakartamigration.dependencyanalysis.domain;

/**
 * Represents a breaking change detected between two JAR versions.
 */
public record BreakingChange(
    BreakingChangeType type,
    String className,
    String memberName,
    String description
) {
    /**
     * Types of breaking changes that can occur.
     */
    public enum BreakingChangeType {
        /** A class was removed */
        CLASS_REMOVED,
        
        /** A method was removed */
        METHOD_REMOVED,
        
        /** A method signature changed (parameters or return type) */
        METHOD_SIGNATURE_CHANGED,
        
        /** A field was removed */
        FIELD_REMOVED,
        
        /** A field type changed */
        FIELD_TYPE_CHANGED,
        
        /** An interface was removed */
        INTERFACE_REMOVED,
        
        /** A class visibility changed (public to protected, etc.) */
        VISIBILITY_CHANGED,
        
        /** Other breaking changes */
        OTHER
    }
}

