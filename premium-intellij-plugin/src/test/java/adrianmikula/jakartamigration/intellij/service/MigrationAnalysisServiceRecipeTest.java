package adrianmikula.jakartamigration.intellij.service;

import adrianmikula.jakartamigration.coderefactoring.domain.Recipe;
import adrianmikula.jakartamigration.coderefactoring.service.RecipeLibrary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for MigrationAnalysisService - specifically for recipe loading.
 * 
 * These tests verify:
 * 1. MigrationAnalysisService loads recipes correctly
 * 2. At least 20 recipes are available (for Jakarta migration)
 * 3. Key recipes (AddJakartaNamespace, UpdatePersistenceXml, etc.) are present
 */
@DisplayName("MigrationAnalysisService Recipe Loading Tests")
class MigrationAnalysisServiceRecipeTest {

    @Test
    @DisplayName("Should load at least 20 recipes for Jakarta migration")
    void shouldLoadAtLeast20Recipes() {
        // Given
        MigrationAnalysisService service = new MigrationAnalysisService();
        
        // When
        List<Recipe> recipes = service.getAvailableRecipes();
        
        // Then
        assertThat(recipes)
            .as("Should have at least 20 recipes for Jakarta migration")
            .hasSizeGreaterThanOrEqualTo(20);
    }
    
    @Test
    @DisplayName("Should include AddJakartaNamespace recipe")
    void shouldIncludeAddJakartaNamespaceRecipe() {
        // Given
        MigrationAnalysisService service = new MigrationAnalysisService();
        
        // When
        List<Recipe> recipes = service.getAvailableRecipes();
        
        // Then
        assertThat(recipes)
            .as("Should include AddJakartaNamespace recipe")
            .anyMatch(r -> r.name().equals("AddJakartaNamespace"));
    }
    
    @Test
    @DisplayName("Should include UpdatePersistenceXml recipe")
    void shouldIncludeUpdatePersistenceXmlRecipe() {
        // Given
        MigrationAnalysisService service = new MigrationAnalysisService();
        
        // When
        List<Recipe> recipes = service.getAvailableRecipes();
        
        // Then
        assertThat(recipes)
            .as("Should include UpdatePersistenceXml recipe")
            .anyMatch(r -> r.name().equals("UpdatePersistenceXml"));
    }
    
    @Test
    @DisplayName("Should include UpdateWebXml recipe")
    void shouldIncludeUpdateWebXmlRecipe() {
        // Given
        MigrationAnalysisService service = new MigrationAnalysisService();
        
        // When
        List<Recipe> recipes = service.getAvailableRecipes();
        
        // Then
        assertThat(recipes)
            .as("Should include UpdateWebXml recipe")
            .anyMatch(r -> r.name().equals("UpdateWebXml"));
    }
    
    @Test
    @DisplayName("Should include MigrateServletApi recipe")
    void shouldIncludeMigrateServletApiRecipe() {
        // Given
        MigrationAnalysisService service = new MigrationAnalysisService();
        
        // When
        List<Recipe> recipes = service.getAvailableRecipes();
        
        // Then
        assertThat(recipes)
            .as("Should include MigrateServletApi recipe")
            .anyMatch(r -> r.name().equals("MigrateServletApi"));
    }
    
    @Test
    @DisplayName("Should include MigrateJpa recipe")
    void shouldIncludeMigrateJpaRecipe() {
        // Given
        MigrationAnalysisService service = new MigrationAnalysisService();
        
        // When
        List<Recipe> recipes = service.getAvailableRecipes();
        
        // Then
        assertThat(recipes)
            .as("Should include MigrateJpa recipe")
            .anyMatch(r -> r.name().equals("MigrateJpa"));
    }
    
    @Test
    @DisplayName("Should include MigrateCdi recipe")
    void shouldIncludeMigrateCdiRecipe() {
        // Given
        MigrationAnalysisService service = new MigrationAnalysisService();
        
        // When
        List<Recipe> recipes = service.getAvailableRecipes();
        
        // Then
        assertThat(recipes)
            .as("Should include MigrateCdi recipe")
            .anyMatch(r -> r.name().equals("MigrateCdi"));
    }
    
    @Test
    @DisplayName("Should include all expected Jakarta migration recipes")
    void shouldIncludeAllExpectedRecipes() {
        // Given
        MigrationAnalysisService service = new MigrationAnalysisService();
        
        // When
        List<Recipe> recipes = service.getAvailableRecipes();
        
        // Then - verify all expected recipes exist
        assertThat(recipes)
            .as("Should include AddJakartaNamespace")
            .anyMatch(r -> r.name().equals("AddJakartaNamespace"));
        assertThat(recipes)
            .as("Should include UpdatePersistenceXml")
            .anyMatch(r -> r.name().equals("UpdatePersistenceXml"));
        assertThat(recipes)
            .as("Should include UpdateWebXml")
            .anyMatch(r -> r.name().equals("UpdateWebXml"));
        assertThat(recipes)
            .as("Should include MigrateServletApi")
            .anyMatch(r -> r.name().equals("MigrateServletApi"));
        assertThat(recipes)
            .as("Should include MigrateJpa")
            .anyMatch(r -> r.name().equals("MigrateJpa"));
        assertThat(recipes)
            .as("Should include MigrateCdi")
            .anyMatch(r -> r.name().equals("MigrateCdi"));
        assertThat(recipes)
            .as("Should include MigrateJaxb")
            .anyMatch(r -> r.name().equals("MigrateJaxb"));
        assertThat(recipes)
            .as("Should include MigrateValidator")
            .anyMatch(r -> r.name().equals("MigrateValidator"));
        assertThat(recipes)
            .as("Should include MigrateEjb")
            .anyMatch(r -> r.name().equals("MigrateEjb"));
        assertThat(recipes)
            .as("Should include MigrateJms")
            .anyMatch(r -> r.name().equals("MigrateJms"));
    }
}
