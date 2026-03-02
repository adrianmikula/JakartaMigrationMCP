package adrianmikula.jakartamigration.intellij.service;

import adrianmikula.jakartamigration.coderefactoring.domain.Recipe;
import adrianmikula.jakartamigration.coderefactoring.domain.RefactoringChanges;
import adrianmikula.jakartamigration.coderefactoring.service.RecipeLibrary;
import adrianmikula.jakartamigration.coderefactoring.service.RefactoringEngine;
import adrianmikula.jakartamigration.dependencyanalysis.domain.*;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyAnalysisModule;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphBuilder;
import adrianmikula.jakartamigration.dependencyanalysis.service.JakartaMappingService;
import adrianmikula.jakartamigration.dependencyanalysis.service.NamespaceClassifier;
import adrianmikula.jakartamigration.dependencyanalysis.service.impl.JakartaMappingServiceImpl;
import adrianmikula.jakartamigration.dependencyanalysis.service.impl.MavenDependencyGraphBuilder;
import adrianmikula.jakartamigration.dependencyanalysis.service.impl.SimpleNamespaceClassifier;
import com.intellij.openapi.diagnostic.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Service for performing migration analysis using the core migration library.
 * This service provides direct access to the dependency analysis module
 * without requiring a running MCP server.
 */
public class MigrationAnalysisService {
    private static final Logger LOG = Logger.getInstance(MigrationAnalysisService.class);

    private final DependencyGraphBuilder dependencyGraphBuilder;
    private final NamespaceClassifier namespaceClassifier;
    private final JakartaMappingService jakartaMappingService;
    private final DependencyAnalysisModule dependencyAnalysisModule;
    private final RecipeLibrary recipeLibrary;
    private final RefactoringEngine refactoringEngine;

    public MigrationAnalysisService() {
        // Create instances of the core library components directly
        // No Spring context needed - all dependencies are simple/immutable
        this.dependencyGraphBuilder = new MavenDependencyGraphBuilder();
        this.namespaceClassifier = new SimpleNamespaceClassifier();
        this.jakartaMappingService = new JakartaMappingServiceImpl();

        // Create the analysis module with its dependencies
        this.dependencyAnalysisModule = new adrianmikula.jakartamigration.dependencyanalysis.service.impl.DependencyAnalysisModuleImpl(
            dependencyGraphBuilder,
            namespaceClassifier,
            jakartaMappingService
        );

        // Initialize recipe library
        this.recipeLibrary = new RecipeLibrary();

        // Initialize refactoring engine
        this.refactoringEngine = new RefactoringEngine();

        LOG.info("MigrationAnalysisService initialized with core library");
    }

    /**
     * Analyzes a project for Jakarta migration readiness.
     *
     * @param projectPath Path to the project root directory
     * @return DependencyAnalysisReport containing the analysis results
     */
    public DependencyAnalysisReport analyzeProject(Path projectPath) {
        LOG.info("Analyzing project at: " + projectPath);
        return dependencyAnalysisModule.analyzeProject(projectPath);
    }

    /**
     * Gets the dependency graph for a project.
     *
     * @param projectPath Path to the project root directory
     * @return DependencyGraph containing all dependencies
     */
    public DependencyGraph getDependencyGraph(Path projectPath) {
        LOG.info("Building dependency graph for: " + projectPath);
        return dependencyGraphBuilder.buildFromProject(projectPath);
    }

    /**
     * Identifies namespaces in the dependency graph.
     *
     * @param graph The dependency graph to analyze
     * @return Map of artifacts to their namespace classification
     */
    public NamespaceCompatibilityMap identifyNamespaces(DependencyGraph graph) {
        return dependencyAnalysisModule.identifyNamespaces(graph);
    }

    /**
     * Detects migration blockers in the dependency graph.
     *
     * @param projectPath Path to the project root directory
     * @return List of detected blockers
     */
    public List<Blocker> detectBlockers(Path projectPath) {
        LOG.info("Detecting blockers in project: " + projectPath);
        DependencyGraph graph = dependencyGraphBuilder.buildFromProject(projectPath);
        return dependencyAnalysisModule.detectBlockers(graph);
    }

    /**
     * Recommends version upgrades for Jakarta migration.
     *
     * @param projectPath Path to the project root directory
     * @return List of version recommendations
     */
    public List<VersionRecommendation> recommendVersions(Path projectPath) {
        LOG.info("Recommending versions for project: " + projectPath);
        DependencyGraph graph = dependencyGraphBuilder.buildFromProject(projectPath);
        List<Artifact> artifacts = graph.getNodes().stream().toList();
        return dependencyAnalysisModule.recommendVersions(artifacts);
    }

    /**
     * Gets the readiness score for a project.
     *
     * @param projectPath Path to the project root directory
     * @return MigrationReadinessScore containing the readiness assessment
     */
    public MigrationReadinessScore getReadinessScore(Path projectPath) {
        LOG.info("Calculating readiness score for: " + projectPath);
        DependencyAnalysisReport report = dependencyAnalysisModule.analyzeProject(projectPath);
        return report.readinessScore();
    }

    /**
     * Checks if a specific artifact has a Jakarta mapping.
     *
     * @param groupId    The group ID of the artifact
     * @param artifactId The artifact ID
     * @return Optional containing the Jakarta equivalent if found
     */
    public Optional<JakartaMappingService.JakartaEquivalent> findJakartaMapping(String groupId, String artifactId) {
        Artifact artifact = new Artifact(groupId, artifactId, "unknown", "compile", false);
        return jakartaMappingService.findMapping(artifact);
    }

    /**
     * Gets all available refactoring recipes.
     *
     * @return List of available recipes
     */
    public List<Recipe> getAvailableRecipes() {
        return recipeLibrary.getAllRecipes();
    }

    /**
     * Gets Jakarta-specific refactoring recipes.
     *
     * @return List of Jakarta migration recipes
     */
    public List<Recipe> getJakartaRecipes() {
        return recipeLibrary.getJakartaRecipes();
    }

    /**
     * Result of applying a recipe to a project.
     */
    public record RecipeApplicationResult(
        boolean success,
        int filesProcessed,
        int filesChanged,
        List<String> changedFilePaths,
        String errorMessage
    ) {
        public static RecipeApplicationResult success(int processed, int changed, List<String> paths) {
            return new RecipeApplicationResult(true, processed, changed, paths, null);
        }

        public static RecipeApplicationResult failure(String error) {
            return new RecipeApplicationResult(false, 0, 0, null, error);
        }
    }

    /**
     * Applies a specific recipe to a project.
     *
     * @param recipeName   The name of the recipe to apply
     * @param projectPath  The project path
     * @return RecipeApplicationResult containing details about the application
     */
    public RecipeApplicationResult applyRecipe(String recipeName, Path projectPath) {
        LOG.info("Applying recipe '" + recipeName + "' to project: " + projectPath);

        Optional<Recipe> recipeOpt = recipeLibrary.getRecipe(recipeName);
        if (recipeOpt.isEmpty()) {
            LOG.warn("Recipe not found: " + recipeName);
            return RecipeApplicationResult.failure("Recipe not found: " + recipeName);
        }

        Recipe recipe = recipeOpt.get();
        List<Recipe> recipes = List.of(recipe);
        List<String> changedFiles = new ArrayList<>();
        int filesProcessed = 0;

        try {
            List<Path> sourceFiles = findSourceFiles(projectPath);
            LOG.info("Found " + sourceFiles.size() + " source files to process");

            for (Path filePath : sourceFiles) {
                try {
                    RefactoringChanges changes = refactoringEngine.refactorFile(filePath, recipes);

                    if (changes.hasChanges()) {
                        // Create backup before modifying
                        Path backupPath = Path.of(filePath.toString() + ".bak");
                        Files.copy(filePath, backupPath, StandardCopyOption.REPLACE_EXISTING);
                        
                        // Write the refactored content
                        Files.writeString(filePath, changes.refactoredContent());
                        changedFiles.add(filePath.toString());
                        LOG.info("Applied recipe to: " + filePath);
                    }
                    filesProcessed++;
                } catch (IOException e) {
                    LOG.warn("Failed to process file " + filePath + ": " + e.getMessage());
                }
            }

            LOG.info("Recipe '" + recipeName + "' applied successfully. Files changed: " + changedFiles.size());
            return RecipeApplicationResult.success(filesProcessed, changedFiles.size(), changedFiles);

        } catch (Exception e) {
            LOG.error("Failed to apply recipe: " + e.getMessage(), e);
            return RecipeApplicationResult.failure("Error applying recipe: " + e.getMessage());
        }
    }

    /**
     * Finds all Java and XML source files in the project.
     */
    private List<Path> findSourceFiles(Path projectPath) throws IOException {
        List<Path> sourceFiles = new ArrayList<>();

        String[] sourceDirs = {"src/main/java", "src/test/java", "src", "src/main/resources"};

        for (String sourceDir : sourceDirs) {
            Path sourcePath = projectPath.resolve(sourceDir);
            if (Files.exists(sourcePath)) {
                try (Stream<Path> paths = Files.walk(sourcePath)) {
                    paths.filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".java") ||
                                     p.toString().endsWith(".xml"))
                        .forEach(sourceFiles::add);
                }
            }
        }

        return sourceFiles;
    }
}
