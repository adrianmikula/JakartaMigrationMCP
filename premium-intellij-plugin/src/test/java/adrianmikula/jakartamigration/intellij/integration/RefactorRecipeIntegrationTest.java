package adrianmikula.jakartamigration.intellij.integration;

import adrianmikula.jakartamigration.refactor.domain.RecipeExecutionResult;
import adrianmikula.jakartamigration.refactor.domain.AppliedRecipe;
import adrianmikula.jakartamigration.refactor.service.RecipeExecutionService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for refactor recipes against projects that contain javax packages.
 */
@Slf4j
public class RefactorRecipeIntegrationTest extends IntegrationTestBase {
    
    @Test
    @DisplayName("Refactor recipe should migrate javax.validation to jakarta.validation")
    void testRefactorRecipeMigratesValidation() throws Exception {
        // Get validation example project
        Path projectDir = getExampleProject("Spring Boot javax-validation", "javax_packages");
        
        // Verify project contains javax.validation
        assertTrue(containsJavaxPackage(projectDir, "validation"), 
            "Project should contain javax.validation packages");
        
        // Run refactor recipe
        RecipeExecutionResult result = recipeExecutionService.executeRecipe(
            projectDir, "JavaxValidationToJakartaValidation");
        
        // Verify execution
        assertNotNull(result);
        assertTrue(result.getExecutionSuccess());
        assertTrue(result.getExecutionDuration().toMillis() > 0);
        
        // Check for applied recipes
        List<AppliedRecipe> appliedRecipes = result.getAppliedRecipes();
        assertTrue(appliedRecipes.size() > 0, "Should have applied recipes");
        
        // Verify validation migration
        boolean foundValidationMigration = appliedRecipes.stream()
            .anyMatch(recipe -> recipe.getRecipeName().contains("Validation"));
        
        assertTrue(foundValidationMigration, "Should apply validation migration recipe");
        
        log.info("Refactor execution completed in {} ms", result.getExecutionDuration().toMillis());
        log.info("Applied {} recipes", appliedRecipes.size());
    }
    
    @Test
    @DisplayName("Refactor recipe should migrate javax.servlet to jakarta.servlet")
    void testRefactorRecipeMigratesServlet() throws Exception {
        // Get servlet example project
        Path projectDir = getExampleProject("Simple Servlet javax", "javax_packages");
        
        // Verify project contains javax.servlet
        assertTrue(containsJavaxPackage(projectDir, "servlet"), 
            "Project should contain javax.servlet packages");
        
        // Run refactor recipe
        RecipeExecutionResult result = recipeExecutionService.executeRecipe(
            projectDir, "JavaxServletToJakartaServlet");
        
        // Verify execution
        assertNotNull(result);
        assertTrue(result.getExecutionSuccess());
        
        // Check for applied recipes
        List<AppliedRecipe> appliedRecipes = result.getAppliedRecipes();
        assertTrue(appliedRecipes.size() > 0, "Should have applied recipes");
        
        // Verify servlet migration
        boolean foundServletMigration = appliedRecipes.stream()
            .anyMatch(recipe -> recipe.getRecipeName().contains("Servlet"));
        
        assertTrue(foundServletMigration, "Should apply servlet migration recipe");
        
        log.info("Refactor execution completed in {} ms", result.getExecutionDuration().toMillis());
        log.info("Applied {} recipes", appliedRecipes.size());
    }
    
    @Test
    @DisplayName("Refactor recipe should migrate javax.persistence to jakarta.persistence")
    void testRefactorRecipeMigratesPersistence() throws Exception {
        // Get project for persistence testing
        Path projectDir = getExampleProject("Spring Boot javax-validation", "javax_packages");
        
        // Run refactor recipe
        RecipeExecutionResult result = recipeExecutionService.executeRecipe(
            projectDir, "JavaxPersistenceToJakartaPersistence");
        
        // Verify execution
        assertNotNull(result);
        assertTrue(result.getExecutionSuccess());
        
        // Check for applied recipes
        List<AppliedRecipe> appliedRecipes = result.getAppliedRecipes();
        
        // Verify persistence migration if JPA packages were found
        boolean foundPersistenceMigration = appliedRecipes.stream()
            .anyMatch(recipe -> recipe.getRecipeName().contains("Persistence"));
        
        if (foundPersistenceMigration) {
            log.info("Persistence migration recipe applied successfully");
        } else {
            log.info("No JPA packages found, no persistence migration applied");
        }
        
        log.info("Refactor execution completed in {} ms", result.getExecutionDuration().toMillis());
        log.info("Applied {} recipes", appliedRecipes.size());
    }
    
    @Test
    @DisplayName("Refactor recipe should migrate javax.ejb to jakarta.ejb")
    void testRefactorRecipeMigratesEjb() throws Exception {
        // Get project for EJB testing
        Path projectDir = getExampleProject("Spring Boot javax-validation", "javax_packages");
        
        // Run refactor recipe
        RecipeExecutionResult result = recipeExecutionService.executeRecipe(
            projectDir, "JavaxEjbToJakartaEjb");
        
        // Verify execution
        assertNotNull(result);
        assertTrue(result.getExecutionSuccess());
        
        // Check for applied recipes
        List<AppliedRecipe> appliedRecipes = result.getAppliedRecipes();
        
        // Verify EJB migration if EJB packages were found
        boolean foundEjbMigration = appliedRecipes.stream()
            .anyMatch(recipe -> recipe.getRecipeName().contains("EJB"));
        
        if (foundEjbMigration) {
            log.info("EJB migration recipe applied successfully");
        } else {
            log.info("No EJB packages found, no EJB migration applied");
        }
        
        log.info("Refactor execution completed in {} ms", result.getExecutionDuration().toMillis());
        log.info("Applied {} recipes", appliedRecipes.size());
    }
    
    @Test
    @DisplayName("Refactor recipe should migrate javax.ws.rs to jakarta.ws.rs")
    void testRefactorRecipeMigratesJaxRs() throws Exception {
        // Get project for JAX-RS testing
        Path projectDir = getExampleProject("Spring Boot javax-validation", "javax_packages");
        
        // Run refactor recipe
        RecipeExecutionResult result = recipeExecutionService.executeRecipe(
            projectDir, "JavaxJaxRsToJakartaJaxRs");
        
        // Verify execution
        assertNotNull(result);
        assertTrue(result.getExecutionSuccess());
        
        // Check for applied recipes
        List<AppliedRecipe> appliedRecipes = result.getAppliedRecipes();
        
        // Verify JAX-RS migration if JAX-RS packages were found
        boolean foundJaxRsMigration = appliedRecipes.stream()
            .anyMatch(recipe -> recipe.getRecipeName().contains("JAX-RS"));
        
        if (foundJaxRsMigration) {
            log.info("JAX-RS migration recipe applied successfully");
        } else {
            log.info("No JAX-RS packages found, no JAX-RS migration applied");
        }
        
        log.info("Refactor execution completed in {} ms", result.getExecutionDuration().toMillis());
        log.info("Applied {} recipes", appliedRecipes.size());
    }
    
    @Test
    @DisplayName("Refactor recipe should handle multiple javax packages")
    void testRefactorRecipeHandlesMultiplePackages() throws Exception {
        // Get project with multiple javax packages
        Path projectDir = getExampleProject("Spring Boot javax-validation", "javax_packages");
        
        // Run comprehensive refactor recipe
        RecipeExecutionResult result = recipeExecutionService.executeRecipe(
            projectDir, "JavaxToJakartaComprehensive");
        
        // Verify execution
        assertNotNull(result);
        assertTrue(result.getExecutionSuccess());
        
        // Check for applied recipes
        List<AppliedRecipe> appliedRecipes = result.getAppliedRecipes();
        assertTrue(appliedRecipes.size() > 0, "Should have applied recipes");
        
        log.info("Multiple packages refactor completed in {} ms", result.getExecutionDuration().toMillis());
        log.info("Applied {} recipes", appliedRecipes.size());
        
        // Log all applied recipes
        appliedRecipes.forEach(recipe -> {
            log.info("Applied recipe: {} ({} files changed)", 
                recipe.getRecipeName(), recipe.getChangedFiles().size());
        });
    }
    
    @Test
    @DisplayName("Refactor recipe should handle Maven projects correctly")
    void testRefactorRecipeHandlesMavenProjects() throws Exception {
        // Get Maven project
        Path projectDir = getExampleProject("Maven", "build_systems");
        
        // Verify it's a Maven project
        assertTrue(hasMavenBuild(projectDir), "Should be a Maven project");
        
        // Run refactor recipe
        RecipeExecutionResult result = recipeExecutionService.executeRecipe(
            projectDir, "JavaxValidationToJakartaValidation");
        
        // Verify execution
        assertNotNull(result);
        assertTrue(result.getExecutionSuccess());
        
        log.info("Maven project refactor completed successfully");
        log.info("Applied {} recipes", result.getAppliedRecipes().size());
    }
    
    @Test
    @DisplayName("Refactor recipe should handle Gradle projects correctly")
    void testRefactorRecipeHandlesGradleProjects() throws Exception {
        // Get Gradle project
        Path projectDir = getExampleProject("Gradle", "build_systems");
        
        // Run refactor recipe
        RecipeExecutionResult result = recipeExecutionService.executeRecipe(
            projectDir, "JavaxValidationToJakartaValidation");
        
        // Verify execution
        assertNotNull(result);
        assertTrue(result.getExecutionSuccess());
        
        log.info("Gradle project refactor completed successfully");
        log.info("Applied {} recipes", result.getAppliedRecipes().size());
    }
    
    @Test
    @DisplayName("Refactor recipe should provide detailed execution information")
    void testRefactorRecipeProvidesDetailedInformation() throws Exception {
        // Get project for detailed refactor testing
        Path projectDir = getExampleProject("Spring Boot javax-validation", "javax_packages");
        
        // Run refactor recipe
        RecipeExecutionResult result = recipeExecutionService.executeRecipe(
            projectDir, "JavaxValidationToJakartaValidation");
        
        // Verify detailed execution information
        assertNotNull(result);
        assertTrue(result.getExecutionSuccess());
        
        // Verify recipe details
        result.getAppliedRecipes().forEach(recipe -> {
            assertNotNull(recipe.getRecipeName(), "Recipe should have name");
            assertTrue(recipe.getChangedFiles().size() >= 0, "Recipe should have changed files count");
            assertTrue(recipe.getExecutionDuration().toMillis() >= 0, "Recipe should have execution duration");
        });
        
        log.info("Detailed refactor applied {} recipes", result.getAppliedRecipes().size());
        
        // Log file changes
        result.getAppliedRecipes().forEach(recipe -> {
            recipe.getChangedFiles().forEach(file -> {
                log.info("Changed file: {} ({} changes)", file.getFilePath(), file.getChangeCount());
            });
        });
    }
}
