package adrianmikula.jakartamigration.analysis.persistence;

/**
 * Metadata for a scanner type, containing all mapping information.
 */
public record ScannerTypeMetadata(
    String scannerType,
    String uiTab,
    String legacyNamespace,
    String targetNamespace,
    String refactorRecipe,
    String description,
    String anticipatedErrorMessages,
    String solutionHint,
    boolean isPremium
) {}
