package adrianmikula.jakartamigration.intellij.service;

import adrianmikula.jakartamigration.coderefactoring.domain.Recipe;
import adrianmikula.jakartamigration.coderefactoring.domain.RefactoringChanges;
import adrianmikula.jakartamigration.coderefactoring.domain.SafetyLevel;
import adrianmikula.jakartamigration.coderefactoring.service.RecipeConfigLoader;
import adrianmikula.jakartamigration.coderefactoring.service.RecipeLibrary;
import adrianmikula.jakartamigration.coderefactoring.service.RefactoringEngine;
import adrianmikula.jakartamigration.coderefactoring.util.RecipeMapper;
import adrianmikula.jakartamigration.dependencyanalysis.domain.*;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyAnalysisModule;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphBuilder;
import adrianmikula.jakartamigration.dependencyanalysis.service.JakartaMappingService;
import adrianmikula.jakartamigration.dependencyanalysis.service.NamespaceClassifier;
import adrianmikula.jakartamigration.dependencyanalysis.service.impl.JakartaMappingServiceImpl;
import adrianmikula.jakartamigration.dependencyanalysis.service.impl.MavenDependencyGraphBuilder;
import adrianmikula.jakartamigration.dependencyanalysis.service.impl.SimpleNamespaceClassifier;
import com.intellij.openapi.diagnostic.Logger;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
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
    private final RecipeMapper recipeMapper;
    private final RefactoringEngine xmlRefactoringEngine;

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
                jakartaMappingService);

        // Initialize recipe library
        this.recipeLibrary = new RecipeLibrary();
        loadRecipesFromYaml();

        // RecipeMapper handles Java files via OpenRewrite directly
        this.recipeMapper = new RecipeMapper();

        // Simple replacement engine used only for XML files
        this.xmlRefactoringEngine = new RefactoringEngine();

        LOG.info("MigrationAnalysisService initialized with core library");
    }

    private void loadRecipesFromYaml() {
        try {
            RecipeConfigLoader loader = RecipeConfigLoader.getInstance();
            Map<String, RecipeConfigLoader.RecipeConfig> configs = loader.getAllRecipeConfigs();

            if (configs.isEmpty()) {
                LOG.warn("MigrationAnalysisService: No recipes loaded from YAML");
                return;
            }

            for (RecipeConfigLoader.RecipeConfig config : configs.values()) {
                Recipe recipe = new Recipe(
                        config.getName(),
                        config.getDescription(),
                        config.getPattern(),
                        SafetyLevel.valueOf(config.getSafety() != null ? config.getSafety() : "MEDIUM"),
                        config.getReversible() != null ? config.getReversible() : true);
                recipeLibrary.registerRecipe(recipe);
            }
            LOG.info("MigrationAnalysisService: Successfully loaded " + configs.size() + " recipes from YAML");
        } catch (Exception e) {
            LOG.error("MigrationAnalysisService: Error loading recipes from YAML", e);
        }
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
            String errorMessage) {
        public static RecipeApplicationResult success(int processed, int changed, List<String> paths) {
            return new RecipeApplicationResult(true, processed, changed, paths, null);
        }

        public static RecipeApplicationResult failure(String error) {
            return new RecipeApplicationResult(false, 0, 0, null, error);
        }
    }

    /**
     * Applies a specific recipe to a project using OpenRewrite for Java files and
     * simple string replacement for XML/other files.
     *
     * OpenRewrite requires all Java source files to be parsed as a full project
     * (not file-by-file) so that type resolution across files works correctly.
     *
     * @param recipeName  The name of the recipe to apply
     * @param projectPath The project path
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
        List<String> changedFiles = new ArrayList<>();
        int filesProcessed = 0;

        try {
            List<Path> allSourceFiles = findSourceFiles(projectPath);
            LOG.info("Found " + allSourceFiles.size() + " source files to process");

            // --- OpenRewrite batch execution for Java source files ---
            List<Path> javaFiles = allSourceFiles.stream()
                    .filter(p -> p.toString().endsWith(".java"))
                    .collect(Collectors.toList());

            org.openrewrite.Recipe orRecipe = recipeMapper.mapToOpenRewriteRecipe(List.of(recipe));
            if (orRecipe != null && !javaFiles.isEmpty()) {
                LOG.info("Running OpenRewrite recipe '" + orRecipe.getDisplayName() + "' on " + javaFiles.size()
                        + " Java files");
                filesProcessed += javaFiles.size();
                try {
                    InMemoryExecutionContext ctx = new InMemoryExecutionContext(
                            t -> LOG.warn("OpenRewrite execution warning: " + t.getMessage()));

                    // Parse all Java files together (project-level) for correct type resolution
                    JavaParser javaParser = JavaParser.fromJavaVersion()
                            .logCompilationWarningsAndErrors(false)
                            .build();

                    List<SourceFile> parsedFiles = javaParser
                            .parse(javaFiles, projectPath, ctx)
                            .collect(Collectors.toList());

                    LOG.info("OpenRewrite parsed " + parsedFiles.size() + " of " + javaFiles.size() + " Java files");

                    if (!parsedFiles.isEmpty()) {
                        List<Result> results = orRecipe
                                .run(new InMemoryLargeSourceSet(parsedFiles), ctx)
                                .getChangeset().getAllResults();

                        LOG.info("OpenRewrite produced " + results.size() + " changed file(s)");

                        for (Result result : results) {
                            if (result.getAfter() == null)
                                continue;

                            // Resolve the real path for this result
                            Path resultPath = result.getAfter().getSourcePath();
                            // getSourcePath() is relative to the parser's base dir (projectPath)
                            Path absolutePath = projectPath.resolve(resultPath);
                            if (!Files.exists(absolutePath)) {
                                // Try to find matching entry in javaFiles by filename
                                String resultName = resultPath.getFileName().toString();
                                absolutePath = javaFiles.stream()
                                        .filter(p -> p.getFileName().toString().equals(resultName))
                                        .findFirst()
                                        .orElse(null);
                            }

                            if (absolutePath != null && Files.exists(absolutePath)) {
                                String modifiedContent = result.getAfter().printAll();
                                Path backupPath = Path.of(absolutePath + ".bak");
                                Files.copy(absolutePath, backupPath, StandardCopyOption.REPLACE_EXISTING);
                                Files.writeString(absolutePath, modifiedContent);
                                changedFiles.add(absolutePath.toString());
                                LOG.info("OpenRewrite modified: " + absolutePath);
                            }
                        }
                    }
                } catch (Exception e) {
                    LOG.error("OpenRewrite execution failed, falling back to simple replacement for Java files: "
                            + e.getMessage(), e);
                    // Fall through to simple replacement below for the Java files
                    filesProcessed -= javaFiles.size(); // recount below
                    for (Path filePath : javaFiles) {
                        try {
                            RefactoringChanges changes = xmlRefactoringEngine.refactorFile(filePath, List.of(recipe));
                            if (changes.hasChanges()) {
                                Path backupPath = Path.of(filePath + ".bak");
                                Files.copy(filePath, backupPath, StandardCopyOption.REPLACE_EXISTING);
                                Files.writeString(filePath, changes.refactoredContent());
                                changedFiles.add(filePath.toString());
                            }
                            filesProcessed++;
                        } catch (IOException ioEx) {
                            LOG.warn("Fallback replacement failed for " + filePath + ": " + ioEx.getMessage());
                        }
                    }
                }
            }

            // --- Simple string replacement for XML and other non-Java files ---
            List<Path> nonJavaFiles = allSourceFiles.stream()
                    .filter(p -> !p.toString().endsWith(".java"))
                    .collect(Collectors.toList());

            for (Path filePath : nonJavaFiles) {
                try {
                    RefactoringChanges changes = xmlRefactoringEngine.refactorFile(filePath, List.of(recipe));
                    if (changes.hasChanges()) {
                        Path backupPath = Path.of(filePath + ".bak");
                        Files.copy(filePath, backupPath, StandardCopyOption.REPLACE_EXISTING);
                        Files.writeString(filePath, changes.refactoredContent());
                        changedFiles.add(filePath.toString());
                        LOG.info("Simple replacement modified: " + filePath);
                    }
                    filesProcessed++;
                } catch (IOException e) {
                    LOG.warn("Failed to process file " + filePath + ": " + e.getMessage());
                }
            }

            LOG.info("Recipe '" + recipeName + "' applied. Files processed: " + filesProcessed + ", changed: "
                    + changedFiles.size());
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

        String[] sourceDirs = { "src/main/java", "src/test/java", "src", "src/main/resources" };

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
