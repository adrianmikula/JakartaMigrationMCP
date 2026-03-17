package adrianmikula.jakartamigration.coderefactoring.domain;

/**
 * Categories of migration recipes as required by REFACTOR.md.
 */
public enum RecipeCategory {
    JAVA("Java"),
    XML("XML"),
    ANNOTATIONS("Annotations"),
    BUILD_DEPENDENCIES("Build/Dependencies");

    private final String displayName;

    RecipeCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
