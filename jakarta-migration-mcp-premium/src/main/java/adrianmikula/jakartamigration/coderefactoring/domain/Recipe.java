package adrianmikula.jakartamigration.coderefactoring.domain;

/**
 * Represents a refactoring recipe (e.g., OpenRewrite recipe).
 */
public record Recipe(
    String name,
    String description,
    String pattern,
    SafetyLevel safety,
    boolean reversible
) {
    public Recipe {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        if (description == null) {
            throw new IllegalArgumentException("Description cannot be null");
        }
        if (pattern == null) {
            throw new IllegalArgumentException("Pattern cannot be null");
        }
        if (safety == null) {
            throw new IllegalArgumentException("Safety cannot be null");
        }
    }
    
    /**
     * Creates the standard Jakarta namespace migration recipe.
     */
    public static Recipe jakartaNamespaceRecipe() {
        return new Recipe(
            "AddJakartaNamespace",
            "Converts javax.* imports and references to jakarta.*",
            "javax.* → jakarta.*",
            SafetyLevel.HIGH,
            true
        );
    }
    
    /**
     * Creates a recipe for updating persistence.xml.
     */
    public static Recipe persistenceXmlRecipe() {
        return new Recipe(
            "UpdatePersistenceXml",
            "Updates persistence.xml namespace to Jakarta",
            "xmlns:persistence='http://java.sun.com/xml/ns/persistence' → xmlns:persistence='https://jakarta.ee/xml/ns/persistence'",
            SafetyLevel.HIGH,
            true
        );
    }
    
    /**
     * Creates a recipe for updating web.xml.
     */
    public static Recipe webXmlRecipe() {
        return new Recipe(
            "UpdateWebXml",
            "Updates web.xml namespace to Jakarta",
            "http://java.sun.com/xml/ns/javaee → https://jakarta.ee/xml/ns/jakartaee",
            SafetyLevel.MEDIUM,
            true
        );
    }
}

