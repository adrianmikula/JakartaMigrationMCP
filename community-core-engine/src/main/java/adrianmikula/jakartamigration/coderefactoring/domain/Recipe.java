package adrianmikula.jakartamigration.coderefactoring.domain;

/**
 * Represents a refactoring recipe (e.g., OpenRewrite recipe).
 */
public record Recipe(
        String name,
        String description,
        String pattern,
        SafetyLevel safety,
        boolean reversible) {
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
                true);
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
                true);
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
                true);
    }

    /**
     * Creates a recipe for JPA migration.
     */
    public static Recipe jpaRecipe() {
        return new Recipe(
                "MigrateJPA",
                "Converts javax.persistence.* to jakarta.persistence.*",
                "javax.persistence.* → jakarta.persistence.*",
                SafetyLevel.HIGH,
                true);
    }

    /**
     * Creates a recipe for Bean Validation migration.
     */
    public static Recipe beanValidationRecipe() {
        return new Recipe(
                "MigrateBeanValidation",
                "Converts javax.validation.* to jakarta.validation.*",
                "javax.validation.* → jakarta.validation.*",
                SafetyLevel.HIGH,
                true);
    }

    /**
     * Creates a recipe for Servlet migration.
     */
    public static Recipe servletRecipe() {
        return new Recipe(
                "MigrateServlet",
                "Converts javax.servlet.* to jakarta.servlet.*",
                "javax.servlet.* → jakarta.servlet.*",
                SafetyLevel.HIGH,
                true);
    }

    /**
     * Creates a recipe for CDI migration.
     */
    public static Recipe cdiRecipe() {
        return new Recipe(
                "MigrateCDI",
                "Converts javax.enterprise.* and javax.inject.* to jakarta equivalents",
                "javax.enterprise.* → jakarta.enterprise.*",
                SafetyLevel.HIGH,
                true);
    }

    /**
     * Creates a recipe for JAX-RS (REST) migration.
     */
    public static Recipe restRecipe() {
        return new Recipe(
                "MigrateREST",
                "Converts javax.ws.rs.* to jakarta.ws.rs.*",
                "javax.ws.rs.* → jakarta.ws.rs.*",
                SafetyLevel.HIGH,
                true);
    }

    /**
     * Creates a recipe for JAX-WS (SOAP) migration.
     */
    public static Recipe soapRecipe() {
        return new Recipe(
                "MigrateSOAP",
                "Converts javax.xml.ws.* to jakarta.xml.ws.*",
                "javax.xml.ws.* → jakarta.xml.ws.*",
                SafetyLevel.HIGH,
                true);
    }
}
