package adrianmikula.jakartamigration.advancedscanning.domain;

import java.util.Objects;

/**
 * Generic usage record that consolidates all the duplicate Usage classes.
 * Represents a javax.* API usage found in source code with its Jakarta EE equivalent.
 * Replaces: BeanValidationUsage, CdiInjectionUsage, ConfigFileUsage, DeprecatedApiUsage,
 * IntegrationPointUsage, JmsMessagingUsage, JpaAnnotationUsage, LoggingMetricsUsage,
 * ReflectionUsage, RestSoapUsage, SecurityApiUsage, SerializationCacheUsage,
 * ServletJspUsage, TestContainerUsage, ThirdPartyLibUsage, TransitiveDependencyUsage, UnitTestUsage
 */
public record JavaxUsage(
    String className,
    String jakartaEquivalent,
    int lineNumber,
    String context
) {
    public JavaxUsage {
        Objects.requireNonNull(className, "className cannot be null");
        if (className.isBlank()) {
            throw new IllegalArgumentException("className cannot be blank");
        }
    }

    /**
     * Returns true if this usage has a Jakarta EE equivalent.
     */
    public boolean hasJakartaEquivalent() {
        return jakartaEquivalent != null && !jakartaEquivalent.isBlank();
    }

    /**
     * Creates a new JavaxUsage with validation that the className starts with javax.
     */
    public static JavaxUsage of(String className, String jakartaEquivalent, int lineNumber, String context) {
        return new JavaxUsage(className, jakartaEquivalent, lineNumber, context);
    }
}
