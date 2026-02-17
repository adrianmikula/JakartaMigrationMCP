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
    
    /**
     * Creates a recipe for migrating Servlet API from javax to jakarta.
     */
    public static Recipe servletApiRecipe() {
        return new Recipe(
            "MigrateServletApi",
            "Migrates javax.servlet.* packages to jakarta.servlet.*",
            "javax.servlet → jakarta.servlet",
            SafetyLevel.HIGH,
            true
        );
    }
    
    /**
     * Creates a recipe for migrating JPA from javax to jakarta.
     */
    public static Recipe jpaRecipe() {
        return new Recipe(
            "MigrateJpa",
            "Migrates javax.persistence.* packages to jakarta.persistence.*",
            "javax.persistence → jakarta.persistence",
            SafetyLevel.HIGH,
            true
        );
    }
    
    /**
     * Creates a recipe for migrating CDI from javax to jakarta.
     */
    public static Recipe cdiRecipe() {
        return new Recipe(
            "MigrateCdi",
            "Migrates javax.inject.* and javax.enterprise.* packages to jakarta.*",
            "javax.inject/javax.enterprise → jakarta.inject/jakarta.enterprise",
            SafetyLevel.HIGH,
            true
        );
    }
    
    /**
     * Creates a recipe for migrating JAXB from javax to jakarta.
     */
    public static Recipe jaxbRecipe() {
        return new Recipe(
            "MigrateJaxb",
            "Migrates javax.xml.bind.* packages to jakarta.xml.bind.*",
            "javax.xml.bind → jakarta.xml.bind",
            SafetyLevel.MEDIUM,
            true
        );
    }
    
    /**
     * Creates a recipe for migrating Bean Validation from javax to jakarta.
     */
    public static Recipe validatorRecipe() {
        return new Recipe(
            "MigrateValidator",
            "Migrates javax.validation.* packages to jakarta.validation.*",
            "javax.validation → jakarta.validation",
            SafetyLevel.HIGH,
            true
        );
    }
}

