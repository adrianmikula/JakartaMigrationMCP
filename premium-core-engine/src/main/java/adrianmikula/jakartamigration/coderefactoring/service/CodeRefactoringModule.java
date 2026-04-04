package adrianmikula.jakartamigration.coderefactoring.service;

import adrianmikula.jakartamigration.analysis.persistence.CentralMigrationAnalysisStore;
import adrianmikula.jakartamigration.analysis.persistence.SqliteMigrationAnalysisStore;
import adrianmikula.jakartamigration.coderefactoring.service.impl.RecipeServiceImpl;
import adrianmikula.jakartamigration.advancedscanning.service.AdvancedScanningModule;

/**
 * Module that provides access to all refactoring and recipe services.
 * This is the main entry point for the refactoring features in the premium
 * engine.
 */
public class CodeRefactoringModule {

    private final RecipeService recipeService;
    private final AdvancedScanningModule advancedScanningModule;

    public CodeRefactoringModule(CentralMigrationAnalysisStore centralStore,
            SqliteMigrationAnalysisStore projectStore) {
        this.recipeService = new RecipeServiceImpl(centralStore, projectStore);
        // Seed default recipes into central store
        adrianmikula.jakartamigration.coderefactoring.service.util.RecipeSeeder.seedDefaultRecipes(centralStore);
        // Seed upgrade recommendations from recipes
        adrianmikula.jakartamigration.coderefactoring.service.util.RecipeSeeder.seedUpgradeRecommendations(centralStore);
        
        // Initialize advanced scanning module with recipe service
        this.advancedScanningModule = new AdvancedScanningModule(recipeService);
    }

    /**
     * Gets the Recipe Service.
     */
    public RecipeService getRecipeService() {
        return recipeService;
    }

    /**
     * Gets the Advanced Scanning Module.
     */
    public AdvancedScanningModule getAdvancedScanningModule() {
        return advancedScanningModule;
    }
}
