/*
 * Copyright 2024 Adrian Kozak
 * Copyright 2024 Prairie Trail Software
 *
 * This software is proprietary and may not be used, copied, modified,
 * or distributed except under the terms of a separate commercial license agreement.
 */
package adrianmikula.jakartamigration.coderefactoring.domain;

/**
 * Represents a refactoring recipe (e.g., OpenRewrite recipe).
 * 
 * NOTE: This is a stub. Full implementation with OpenRewrite recipes
 * is available in the premium edition.
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
            name = "Unknown";
        }
        if (description == null) {
            description = "";
        }
        if (pattern == null) {
            pattern = "";
        }
        if (safety == null) {
            safety = SafetyLevel.MEDIUM;
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
