package adrianmikula.jakartamigration.advancedscanning.domain;

import java.util.Objects;

public record DeprecatedApiUsage(
    String className,
    String methodName,
    String jakartaEquivalent,
    int lineNumber,
    String context,
    String deprecationType // removed, changed, deprecated
) {
    public DeprecatedApiUsage {
        Objects.requireNonNull(className, "className cannot be null");
    }

    public boolean hasJakartaEquivalent() {
        return jakartaEquivalent != null && !jakartaEquivalent.isBlank();
    }
}
