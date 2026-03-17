package adrianmikula.jakartamigration.coderefactoring.service;

import adrianmikula.jakartamigration.analysis.persistence.CentralMigrationAnalysisStore;
import adrianmikula.jakartamigration.analysis.persistence.SqliteMigrationAnalysisStore;
import adrianmikula.jakartamigration.coderefactoring.service.impl.RecipeServiceImpl;

/**
 * Module that provides access to all refactoring and recipe services.
 * This is the main entry point for the refactoring features in the premium
 * engine.
 */
public class CodeRefactoringModule {

    private final RecipeService recipeService;

    public CodeRefactoringModule(CentralMigrationAnalysisStore centralStore,
            SqliteMigrationAnalysisStore projectStore) {
        this.recipeService = new RecipeServiceImpl(centralStore, projectStore);
    }

    /**
     * Gets the Recipe Service.
     */
    public RecipeService getRecipeService() {
        return recipeService;
    }
}
