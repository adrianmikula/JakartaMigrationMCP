package adrianmikula.jakartamigration.intellij.service;

import adrianmikula.jakartamigration.coderefactoring.domain.RecipeCategory;
import adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition;
import adrianmikula.jakartamigration.coderefactoring.service.RecipeService;
import adrianmikula.jakartamigration.experiment.domain.*;
import adrianmikula.jakartamigration.experiment.mcp.ExperimentTools;
import adrianmikula.jakartamigration.experiment.service.ExperimentRunner;
import adrianmikula.jakartamigration.experiment.service.NoOpOrchestratorFactory;
import com.intellij.openapi.diagnostic.Logger;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for integrating with the premium-experiment-engine.
 * Provides a simplified interface for the UI to interact with experiment functionality.
 */
public class ExperimentService {
    private static final Logger LOG = Logger.getInstance(ExperimentService.class);
    
    private final ExperimentTools experimentTools;
    private final RecipeService recipeService;
    private final Path projectRoot;

    public ExperimentService(Path projectRoot, RecipeService recipeService) {
        this.projectRoot = projectRoot;
        this.recipeService = recipeService;
        
        // Create ExperimentTools with default configuration
        // Note: This uses a null container factory for now - will need proper implementation
        // when actual testcontainers integration is added
        this.experimentTools = new ExperimentTools(
            projectRoot,
            new NoOpOrchestratorFactory(),
            "eclipse-temurin:17-jdk",
            300
        );
    }

    /**
     * Get available recipes for sequence building.
     */
    public List<RecipeDefinition> getAvailableRecipes() {
        return recipeService.getRecipes(projectRoot);
    }

    /**
     * Get recipes by category.
     */
    public List<RecipeDefinition> getRecipesByCategory(RecipeCategory category) {
        return recipeService.getRecipesByCategory(category, projectRoot);
    }

    /**
     * Save a migration sequence.
     */
    public MigrationSequence saveSequence(MigrationSequence sequence) throws Exception {
        return experimentTools.createMigrationSequence(
            sequence.name(),
            sequence.description(),
            sequence.steps(),
            sequence.tags()
        );
    }

    /**
     * List all migration sequences.
     */
    public List<MigrationSequence> listSequences() throws Exception {
        return experimentTools.listMigrationSequences(null);
    }

    /**
     * List migration sequences by tag.
     */
    public List<MigrationSequence> listSequencesByTag(String tag) throws Exception {
        return experimentTools.listMigrationSequences(tag);
    }

    /**
     * Get a specific migration sequence by name.
     */
    public Optional<MigrationSequence> getSequence(String name) throws Exception {
        return experimentTools.getMigrationSequence(name);
    }

    /**
     * Run an experiment for a given sequence.
     */
    public ExperimentResult runExperiment(String sequenceName, Optional<String> gitRef) throws Exception {
        return experimentTools.runMigrationExperiment(sequenceName, projectRoot, gitRef);
    }

    /**
     * Get experiment history.
     */
    public List<ExperimentResult> getHistory(Optional<String> sequenceName, Optional<ExperimentStatus> status, int limit) throws Exception {
        return experimentTools.getExperimentHistory(
            sequenceName.orElse(null),
            status.orElse(null),
            limit
        );
    }

    /**
     * Get a specific experiment result by run ID.
     */
    public Optional<ExperimentResult> getExperimentResult(String runId) throws Exception {
        return experimentTools.getExperimentResult(runId);
    }

    /**
     * Get the sequence used for a specific experiment run.
     */
    public Optional<MigrationSequence> getSequenceByResult(String runId) throws Exception {
        return Optional.of(experimentTools.getMigrationSequenceResult(runId));
    }

    /**
     * Compare two experiment runs.
     */
    public ComparisonReport compareExperiments(String runIdA, String runIdB) throws Exception {
        return experimentTools.compareExperiments(runIdA, runIdB);
    }
}
