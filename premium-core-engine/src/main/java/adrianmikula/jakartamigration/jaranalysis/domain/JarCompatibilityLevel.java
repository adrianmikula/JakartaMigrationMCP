package adrianmikula.jakartamigration.jaranalysis.domain;

/**
 * JAR compatibility classification result.
 * Corresponds to TypeSpec: JarCompatibilityLevel enum
 */
public enum JarCompatibilityLevel {
    /**
     * Fully Jakarta EE compatible (jakarta.* only).
     */
    JAKARTA,

    /**
     * Legacy Java EE only (javax.* only).
     */
    JAVAX,

    /**
     * Mixed javax and jakarta usage, requires review.
     */
    MIXED,

    /**
     * Insufficient signal to determine compatibility.
     */
    UNKNOWN,

    /**
     * Supports both javax and jakarta at runtime (dual-ported library).
     */
    DUAL_COMPATIBLE;

    /**
     * Converts this level to the community Namespace type.
     * JAKARTA → JAKARTA, JAVAX → JAVAX, others → MIXED.
     */
    public adrianmikula.jakartamigration.dependencyanalysis.domain.Namespace toNamespace() {
        return switch (this) {
            case JAKARTA -> adrianmikula.jakartamigration.dependencyanalysis.domain.Namespace.JAKARTA;
            case JAVAX -> adrianmikula.jakartamigration.dependencyanalysis.domain.Namespace.JAVAX;
            case MIXED, UNKNOWN, DUAL_COMPATIBLE -> adrianmikula.jakartamigration.dependencyanalysis.domain.Namespace.MIXED;
        };
    }
}
