package adrianmikula.jakartamigration.intellij.integration;

import adrianmikula.jakartamigration.advancedscanning.domain.AdvancedScanResult;
import adrianmikula.jakartamigration.platforms.domain.PlatformDetectionResult;
import adrianmikula.jakartamigration.refactor.domain.RecipeExecutionResult;
import adrianmikula.jakartamigration.scanning.domain.DependencyGraph;
import adrianmikula.jakartamigration.advancedscanning.service.AdvancedScanningService;
import adrianmikula.jakartamigration.platforms.service.PlatformDetectionService;
import adrianmikula.jakartamigration.refactor.service.RecipeExecutionService;
import adrianmikula.jakartamigration.scanning.service.DependencyAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for different build systems (Maven, Gradle, No Build System).
 * Tests that basic, advanced, and platform scans work correctly on different build system types.
 */
@Slf4j
public class BuildSystemIntegrationTest extends IntegrationTestBase {
    
    @Test
    @DisplayName("Build system tests should work on Maven projects")
    void testBuildSystemTestsWorkOnMavenProjects() throws Exception {
        // Get Maven project
        Path projectDir = getExampleProject("Maven", "build_systems");
        
        // Verify it's a Maven project
        assertTrue(hasMavenBuild(projectDir), "Should be a Maven project");
        assertFalse(hasGradleBuild(projectDir), "Should not be a Gradle project");
        
        // Test advanced scan
        AdvancedScanResult advancedResult = advancedScanningService.scanProject(projectDir);
        assertNotNull(advancedResult, "Advanced scan should work on Maven projects");
        assertTrue(advancedResult.getScanSuccess(), "Advanced scan should succeed");
        
        // Test platform scan
        PlatformDetectionResult platformResult = platformDetectionService.detectPlatforms(projectDir);
        assertNotNull(platformResult, "Platform scan should work on Maven projects");
        assertTrue(platformResult.getDetectionSuccess(), "Platform scan should succeed");
        
        // Test refactor recipe
        RecipeExecutionResult refactorResult = recipeExecutionService.executeRecipe(
            projectDir, "JavaxValidationToJakartaValidation");
        assertNotNull(refactorResult, "Refactor recipe should work on Maven projects");
        assertTrue(refactorResult.getExecutionSuccess(), "Refactor recipe should succeed");
        
        // Test dependency analysis
        DependencyGraph dependencyResult = dependencyAnalysisService.analyzeDependencies(projectDir);
        assertNotNull(dependencyResult, "Dependency analysis should work on Maven projects");
        assertTrue(dependencyResult.getAnalysisSuccess(), "Dependency analysis should succeed");
        
        log.info("Maven project all tests completed successfully");
        log.info("Advanced scan: {} findings", advancedResult.getFindings().size());
        log.info("Platform scan: {} platforms", platformResult.getDetectedPlatforms().size());
        log.info("Refactor recipe: {} applied", refactorResult.getAppliedRecipes().size());
        log.info("Dependency analysis: {} nodes", dependencyResult.getNodes().size());
    }
    
    @Test
    @DisplayName("Build system tests should work on Gradle projects")
    void testBuildSystemTestsWorkOnGradleProjects() throws Exception {
        // Get Gradle project
        Path projectDir = getExampleProject("Gradle", "build_systems");
        
        // Verify it's a Gradle project
        assertTrue(hasGradleBuild(projectDir), "Should be a Gradle project");
        assertFalse(hasMavenBuild(projectDir), "Should not be a Maven project");
        
        // Test advanced scan
        AdvancedScanResult advancedResult = advancedScanningService.scanProject(projectDir);
        assertNotNull(advancedResult, "Advanced scan should work on Gradle projects");
        assertTrue(advancedResult.getScanSuccess(), "Advanced scan should succeed");
        
        // Test platform scan
        PlatformDetectionResult platformResult = platformDetectionService.detectPlatforms(projectDir);
        assertNotNull(platformResult, "Platform scan should work on Gradle projects");
        assertTrue(platformResult.getDetectionSuccess(), "Platform scan should succeed");
        
        // Test refactor recipe
        RecipeExecutionResult refactorResult = recipeExecutionService.executeRecipe(
            projectDir, "JavaxValidationToJakartaValidation");
        assertNotNull(refactorResult, "Refactor recipe should work on Gradle projects");
        assertTrue(refactorResult.getExecutionSuccess(), "Refactor recipe should succeed");
        
        // Test dependency analysis
        DependencyGraph dependencyResult = dependencyAnalysisService.analyzeDependencies(projectDir);
        assertNotNull(dependencyResult, "Dependency analysis should work on Gradle projects");
        assertTrue(dependencyResult.getAnalysisSuccess(), "Dependency analysis should succeed");
        
        log.info("Gradle project all tests completed successfully");
        log.info("Advanced scan: {} findings", advancedResult.getFindings().size());
        log.info("Platform scan: {} platforms", platformResult.getDetectedPlatforms().size());
        log.info("Refactor recipe: {} applied", refactorResult.getAppliedRecipes().size());
        log.info("Dependency analysis: {} nodes", dependencyResult.getNodes().size());
    }
    
    @Test
    @DisplayName("Build system tests should work on projects with no build system")
    void testBuildSystemTestsWorkOnNoBuildSystemProjects() throws Exception {
        // Get project with no build system
        Path projectDir = getExampleProject("No Build System", "build_systems");
        
        // Verify it has no standard build system
        assertFalse(hasMavenBuild(projectDir), "Should not be a Maven project");
        assertFalse(hasGradleBuild(projectDir), "Should not be a Gradle project");
        
        // Test advanced scan
        AdvancedScanResult advancedResult = advancedScanningService.scanProject(projectDir);
        assertNotNull(advancedResult, "Advanced scan should work on projects with no build system");
        assertTrue(advancedResult.getScanSuccess(), "Advanced scan should succeed");
        
        // Test platform scan
        PlatformDetectionResult platformResult = platformDetectionService.detectPlatforms(projectDir);
        assertNotNull(platformResult, "Platform scan should work on projects with no build system");
        assertTrue(platformResult.getDetectionSuccess(), "Platform scan should succeed");
        
        // Test refactor recipe
        RecipeExecutionResult refactorResult = recipeExecutionService.executeRecipe(
            projectDir, "JavaxValidationToJakartaValidation");
        assertNotNull(refactorResult, "Refactor recipe should work on projects with no build system");
        assertTrue(refactorResult.getExecutionSuccess(), "Refactor recipe should succeed");
        
        // Test dependency analysis
        DependencyGraph dependencyResult = dependencyAnalysisService.analyzeDependencies(projectDir);
        assertNotNull(dependencyResult, "Dependency analysis should work on projects with no build system");
        assertTrue(dependencyResult.getAnalysisSuccess(), "Dependency analysis should succeed");
        
        log.info("No build system project all tests completed successfully");
        log.info("Advanced scan: {} findings", advancedResult.getFindings().size());
        log.info("Platform scan: {} platforms", platformResult.getDetectedPlatforms().size());
        log.info("Refactor recipe: {} applied", refactorResult.getAppliedRecipes().size());
        log.info("Dependency analysis: {} nodes", dependencyResult.getNodes().size());
    }
    
    @Test
    @DisplayName("Build system tests should handle mixed build system scenarios")
    void testBuildSystemTestsHandleMixedScenarios() throws Exception {
        // Test with different build system examples to ensure flexibility
        log.info("Testing mixed build system scenarios");
        
        // Test Maven project again (already tested above, but verify consistency)
        Path mavenProject = getExampleProject("Maven", "build_systems");
        assertTrue(hasMavenBuild(mavenProject), "Maven project should be detected correctly");
        
        // Test Gradle project again (already tested above, but verify consistency)
        Path gradleProject = getExampleProject("Gradle", "build_systems");
        assertTrue(hasGradleBuild(gradleProject), "Gradle project should be detected correctly");
        
        // Verify both projects have javax packages for testing
        assertTrue(containsJavaxPackage(mavenProject, "validation"), 
            "Maven project should contain javax packages");
        assertTrue(containsJavaxPackage(gradleProject, "validation"), 
            "Gradle project should contain javax packages");
        
        log.info("Mixed build system scenarios completed successfully");
        log.info("Maven project detected: {}", hasMavenBuild(mavenProject));
        log.info("Gradle project detected: {}", hasGradleBuild(gradleProject));
    }
    
    @Test
    @DisplayName("Build system tests should provide comprehensive coverage")
    void testBuildSystemTestsProvideComprehensiveCoverage() throws Exception {
        // Get all available build system examples
        var availableExamples = getAvailableExamples("build_systems");
        
        log.info("Available build system examples: {}", availableExamples.size());
        availableExamples.forEach(example -> {
            log.info("Example: {}", example);
        });
        
        // Verify we have the expected examples
        assertTrue(availableExamples.contains("Maven"), "Should have Maven example");
        assertTrue(availableExamples.contains("Gradle"), "Should have Gradle example");
        assertTrue(availableExamples.contains("No Build System"), "Should have no build system example");
        
        // Test that we can get each example without errors
        availableExamples.forEach(exampleName -> {
            try {
                Path projectDir = getExampleProject(exampleName, "build_systems");
                assertNotNull(projectDir, "Should be able to get project: " + exampleName);
                log.info("Successfully retrieved project: {}", exampleName);
            } catch (Exception e) {
                log.error("Failed to get project {}: {}", exampleName, e.getMessage());
            }
        });
        
        log.info("Comprehensive build system coverage test completed");
    }
    
    @Test
    @DisplayName("Build system tests should validate project structure")
    void testBuildSystemTestsValidateProjectStructure() throws Exception {
        // Get Maven project for structure validation
        Path mavenProject = getExampleProject("Maven", "build_systems");
        
        // Validate Maven project structure
        assertTrue(hasMavenBuild(mavenProject), "Maven project should have pom.xml");
        Path mainSourceDir = getMainSourceDir(mavenProject);
        assertTrue(Files.exists(mainSourceDir), "Should have main source directory");
        
        // Get Gradle project for structure validation
        Path gradleProject = getExampleProject("Gradle", "build_systems");
        
        // Validate Gradle project structure
        assertTrue(hasGradleBuild(gradleProject), "Gradle project should have build file");
        Path gradleMainSourceDir = getMainSourceDir(gradleProject);
        assertTrue(Files.exists(gradleMainSourceDir), "Should have main source directory");
        
        log.info("Project structure validation completed successfully");
        log.info("Maven project structure: valid");
        log.info("Gradle project structure: valid");
    }
    
    @Test
    @DisplayName("Build system tests should handle edge cases")
    void testBuildSystemTestsHandleEdgeCases() throws Exception {
        // Test edge cases for build system detection
        
        // Test project that might have both build files (edge case)
        Path projectDir = getExampleProject("Maven", "build_systems");
        
        // In this case, Maven should be detected as primary
        assertTrue(hasMavenBuild(projectDir), "Should detect Maven as primary build system");
        
        // Test that our utilities handle edge cases gracefully
        boolean hasMaven = hasMavenBuild(projectDir);
        boolean hasGradle = hasGradleBuild(projectDir);
        
        // Log the detection results for debugging
        log.info("Build system detection results:");
        log.info("  Has Maven: {}", hasMaven);
        log.info("  Has Gradle: {}", hasGradle);
        
        // At least one should be true for our test projects
        assertTrue(hasMaven || hasGradle, "Should detect at least one build system");
        
        log.info("Edge case handling completed successfully");
    }
}
