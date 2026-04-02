package adrianmikula.jakartamigration.intellij.integration;

import adrianmikula.jakartamigration.advancedscanning.service.AdvancedScanningService;
import adrianmikula.jakartamigration.platforms.service.PlatformDetectionService;
import adrianmikula.jakartamigration.refactor.service.RecipeExecutionService;
import adrianmikula.jakartamigration.scanning.service.DependencyAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Base class for integration tests that handles downloading and extracting example projects.
 */
@Slf4j
public abstract class IntegrationTestBase {
    
    @TempDir
    protected Path tempDir;
    
    protected ExampleProjectManager projectManager;
    protected AdvancedScanningService advancedScanningService;
    protected PlatformDetectionService platformDetectionService;
    protected RecipeExecutionService recipeExecutionService;
    protected DependencyAnalysisService dependencyAnalysisService;
    
    @BeforeEach
    void setUp() throws IOException {
        // Initialize project manager
        projectManager = new ExampleProjectManager(tempDir);
        
        // Initialize services
        advancedScanningService = new AdvancedScanningService();
        platformDetectionService = new PlatformDetectionService();
        recipeExecutionService = new RecipeExecutionService();
        dependencyAnalysisService = new DependencyAnalysisService();
    }
    
    @AfterEach
    void tearDown() throws IOException {
        // Cleanup extracted projects but retain cached ZIP files
        if (projectManager != null) {
            projectManager.cleanupAllExtractedProjects();
        }
    }
    
    /**
     * Gets an example project by name and type, downloading if necessary.
     * 
     * @param exampleName The name of the example from examples.yaml
     * @param exampleType The type category (application_servers, javax_packages, etc.)
     * @return Path to the extracted project directory
     * @throws IOException If download/extraction fails
     */
    protected Path getExampleProject(String exampleName, String exampleType) throws IOException {
        return projectManager.getExampleProject(exampleName, exampleType);
    }
    
    /**
     * Cleans up a specific extracted project.
     * 
     * @param projectPath The path to the extracted project to clean up
     * @throws IOException If cleanup fails
     */
    protected void cleanupProject(Path projectPath) throws IOException {
        projectManager.cleanupExtractedProject(projectPath);
    }
    
    /**
     * Gets a list of available examples for testing.
     */
    protected List<String> getAvailableExamples(String type) {
        var available = projectManager.getAvailableExamples();
        var examples = available.get(type);
        if (examples != null) {
            return examples.stream()
                .map(example -> (String) example.get("name"))
                .toList();
        }
        return List.of();
    }
    
    /**
     * Logs cache statistics for debugging.
     */
    protected void logCacheStats() {
        var stats = projectManager.getCacheStats();
        log.info("Cache stats: {}", stats);
    }
    
    /**
     * Checks if a project contains specific javax packages.
     */
    protected boolean containsJavaxPackage(Path projectDir, String packageName) throws IOException {
        return Files.walk(projectDir)
            .filter(path -> path.toString().endsWith(".java"))
            .anyMatch(path -> {
                try {
                    String content = Files.readString(path);
                    return content.contains("javax." + packageName);
                } catch (IOException e) {
                    return false;
                }
            });
    }
    
    /**
     * Checks if a project has Maven build file.
     */
    protected boolean hasMavenBuild(Path projectDir) {
        return Files.exists(projectDir.resolve("pom.xml"));
    }
    
    /**
     * Checks if a project has Gradle build file.
     */
    protected boolean hasGradleBuild(Path projectDir) {
        return Files.exists(projectDir.resolve("build.gradle")) || 
               Files.exists(projectDir.resolve("build.gradle.kts"));
    }
    
    /**
     * Gets the main source directory for the project.
     */
    protected Path getMainSourceDir(Path projectDir) {
        // Try common source directory structures
        Path[] possibleDirs = {
            projectDir.resolve("src/main/java"),
            projectDir.resolve("src"),
            projectDir.resolve("java"),
            projectDir
        };
        
        for (Path dir : possibleDirs) {
            if (Files.exists(dir) && Files.isDirectory(dir)) {
                return dir;
            }
        }
        
        return projectDir;
    }
}
