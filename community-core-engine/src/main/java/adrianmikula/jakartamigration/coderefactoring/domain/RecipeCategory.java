package adrianmikula.jakartamigration.coderefactoring.domain;

/**
 * Categories of migration recipes as required by REFACTOR.md.
 */
public enum RecipeCategory {
    ANNOTATIONS("Annotations"),
    CONFIGURATION("Configuration"),
    DATABASE("Database"),
    CDI("CDI"),
    APIS("APIs"),
    WEB("Web"),
    SECURITY("Security"),
    OTHER("Other");

    private final String displayName;

    RecipeCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
