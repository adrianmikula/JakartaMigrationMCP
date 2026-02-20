package adrianmikula.jakartamigration.advancedscanning.domain;

public record RestSoapUsage(
    String className,
    String jakartaEquivalent,
    int lineNumber,
    String context,
    String usageType // rest, soap, client
) {
    public RestSoapUsage {
        if (className == null || className.isBlank()) {
            throw new IllegalArgumentException("className cannot be null or blank");
        }
    }

    public boolean hasJakartaEquivalent() {
        return jakartaEquivalent != null && !jakartaEquivalent.isBlank();
    }
}
