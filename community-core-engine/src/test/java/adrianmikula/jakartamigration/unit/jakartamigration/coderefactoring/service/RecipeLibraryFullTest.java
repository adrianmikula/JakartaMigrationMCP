package adrianmikula.jakartamigration.unit.jakartamigration.coderefactoring.service;

import adrianmikula.jakartamigration.coderefactoring.domain.Recipe;
import adrianmikula.jakartamigration.coderefactoring.service.RecipeLibrary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for RecipeLibrary - verifying it loads all expected Jakarta migration recipes.
 * 
 * These tests ensure the RecipeLibrary correctly initializes with 20 recipes
 * for Jakarta EE migration.
 */
@DisplayName("RecipeLibrary Full Tests")
class RecipeLibraryFullTest {

    private final RecipeLibrary recipeLibrary = new RecipeLibrary();

    @Test
    @DisplayName("Should initialize with exactly 20 recipes")
    void shouldInitializeWith20Recipes() {
        // When
        List<Recipe> allRecipes = recipeLibrary.getAllRecipes();
        
        // Then
        assertThat(allRecipes)
            .as("RecipeLibrary should initialize with exactly 20 recipes")
            .hasSize(20);
    }
    
    @Test
    @DisplayName("Should have AddJakartaNamespace recipe")
    void shouldHaveAddJakartaNamespaceRecipe() {
        // When
        List<Recipe> allRecipes = recipeLibrary.getAllRecipes();
        
        // Then
        assertThat(allRecipes)
            .as("Should have AddJakartaNamespace recipe")
            .anyMatch(r -> r.name().equals("AddJakartaNamespace"));
    }
    
    @Test
    @DisplayName("Should have UpdatePersistenceXml recipe")
    void shouldHaveUpdatePersistenceXmlRecipe() {
        // When
        List<Recipe> allRecipes = recipeLibrary.getAllRecipes();
        
        // Then
        assertThat(allRecipes)
            .as("Should have UpdatePersistenceXml recipe")
            .anyMatch(r -> r.name().equals("UpdatePersistenceXml"));
    }
    
    @Test
    @DisplayName("Should have UpdateWebXml recipe")
    void shouldHaveUpdateWebXmlRecipe() {
        // When
        List<Recipe> allRecipes = recipeLibrary.getAllRecipes();
        
        // Then
        assertThat(allRecipes)
            .as("Should have UpdateWebXml recipe")
            .anyMatch(r -> r.name().equals("UpdateWebXml"));
    }
    
    @Test
    @DisplayName("Should have MigrateServletApi recipe")
    void shouldHaveMigrateServletApiRecipe() {
        // When
        List<Recipe> allRecipes = recipeLibrary.getAllRecipes();
        
        // Then
        assertThat(allRecipes)
            .as("Should have MigrateServletApi recipe")
            .anyMatch(r -> r.name().equals("MigrateServletApi"));
    }
    
    @Test
    @DisplayName("Should have MigrateJpa recipe")
    void shouldHaveMigrateJpaRecipe() {
        // When
        List<Recipe> allRecipes = recipeLibrary.getAllRecipes();
        
        // Then
        assertThat(allRecipes)
            .as("Should have MigrateJpa recipe")
            .anyMatch(r -> r.name().equals("MigrateJpa"));
    }
    
    @Test
    @DisplayName("Should have MigrateCdi recipe")
    void shouldHaveMigrateCdiRecipe() {
        // When
        List<Recipe> allRecipes = recipeLibrary.getAllRecipes();
        
        // Then
        assertThat(allRecipes)
            .as("Should have MigrateCdi recipe")
            .anyMatch(r -> r.name().equals("MigrateCdi"));
    }
    
    @Test
    @DisplayName("Should have MigrateJaxb recipe")
    void shouldHaveMigrateJaxbRecipe() {
        // When
        List<Recipe> allRecipes = recipeLibrary.getAllRecipes();
        
        // Then
        assertThat(allRecipes)
            .as("Should have MigrateJaxb recipe")
            .anyMatch(r -> r.name().equals("MigrateJaxb"));
    }
    
    @Test
    @DisplayName("Should have MigrateValidator recipe")
    void shouldHaveMigrateValidatorRecipe() {
        // When
        List<Recipe> allRecipes = recipeLibrary.getAllRecipes();
        
        // Then
        assertThat(allRecipes)
            .as("Should have MigrateValidator recipe")
            .anyMatch(r -> r.name().equals("MigrateValidator"));
    }
    
    @Test
    @DisplayName("Should have MigrateEjb recipe")
    void shouldHaveMigrateEjbRecipe() {
        // When
        List<Recipe> allRecipes = recipeLibrary.getAllRecipes();
        
        // Then
        assertThat(allRecipes)
            .as("Should have MigrateEjb recipe")
            .anyMatch(r -> r.name().equals("MigrateEjb"));
    }
    
    @Test
    @DisplayName("Should have MigrateJms recipe")
    void shouldHaveMigrateJmsRecipe() {
        // When
        List<Recipe> allRecipes = recipeLibrary.getAllRecipes();
        
        // Then
        assertThat(allRecipes)
            .as("Should have MigrateJms recipe")
            .anyMatch(r -> r.name().equals("MigrateJms"));
    }
    
    @Test
    @DisplayName("Should have MigrateJaxrs recipe")
    void shouldHaveMigrateJaxrsRecipe() {
        // When
        List<Recipe> allRecipes = recipeLibrary.getAllRecipes();
        
        // Then
        assertThat(allRecipes)
            .as("Should have MigrateJaxrs recipe")
            .anyMatch(r -> r.name().equals("MigrateJaxrs"));
    }
    
    @Test
    @DisplayName("Should have MigrateJaxws recipe")
    void shouldHaveMigrateJaxwsRecipe() {
        // When
        List<Recipe> allRecipes = recipeLibrary.getAllRecipes();
        
        // Then
        assertThat(allRecipes)
            .as("Should have MigrateJaxws recipe")
            .anyMatch(r -> r.name().equals("MigrateJaxws"));
    }
    
    @Test
    @DisplayName("Should have MigrateJta recipe")
    void shouldHaveMigrateJtaRecipe() {
        // When
        List<Recipe> allRecipes = recipeLibrary.getAllRecipes();
        
        // Then
        assertThat(allRecipes)
            .as("Should have MigrateJta recipe")
            .anyMatch(r -> r.name().equals("MigrateJta"));
    }
    
    @Test
    @DisplayName("Should have MigrateJavaMail recipe")
    void shouldHaveMigrateJavaMailRecipe() {
        // When
        List<Recipe> allRecipes = recipeLibrary.getAllRecipes();
        
        // Then
        assertThat(allRecipes)
            .as("Should have MigrateJavaMail recipe")
            .anyMatch(r -> r.name().equals("MigrateJavaMail"));
    }
    
    @Test
    @DisplayName("Should have MigrateWebsocket recipe")
    void shouldHaveMigrateWebsocketRecipe() {
        // When
        List<Recipe> allRecipes = recipeLibrary.getAllRecipes();
        
        // Then
        assertThat(allRecipes)
            .as("Should have MigrateWebsocket recipe")
            .anyMatch(r -> r.name().equals("MigrateWebsocket"));
    }
    
    @Test
    @DisplayName("Should have MigrateJsonb recipe")
    void shouldHaveMigrateJsonbRecipe() {
        // When
        List<Recipe> allRecipes = recipeLibrary.getAllRecipes();
        
        // Then
        assertThat(allRecipes)
            .as("Should have MigrateJsonb recipe")
            .anyMatch(r -> r.name().equals("MigrateJsonb"));
    }
    
    @Test
    @DisplayName("Should have MigrateJsonp recipe")
    void shouldHaveMigrateJsonpRecipe() {
        // When
        List<Recipe> allRecipes = recipeLibrary.getAllRecipes();
        
        // Then
        assertThat(allRecipes)
            .as("Should have MigrateJsonp recipe")
            .anyMatch(r -> r.name().equals("MigrateJsonp"));
    }
    
    @Test
    @DisplayName("Should have MigrateActivation recipe")
    void shouldHaveMigrateActivationRecipe() {
        // When
        List<Recipe> allRecipes = recipeLibrary.getAllRecipes();
        
        // Then
        assertThat(allRecipes)
            .as("Should have MigrateActivation recipe")
            .anyMatch(r -> r.name().equals("MigrateActivation"));
    }
    
    @Test
    @DisplayName("Should have MigrateSoap recipe")
    void shouldHaveMigrateSoapRecipe() {
        // When
        List<Recipe> allRecipes = recipeLibrary.getAllRecipes();
        
        // Then
        assertThat(allRecipes)
            .as("Should have MigrateSoap recipe")
            .anyMatch(r -> r.name().equals("MigrateSoap"));
    }
    
    @Test
    @DisplayName("Should have MigrateSaaj recipe")
    void shouldHaveMigrateSaajRecipe() {
        // When
        List<Recipe> allRecipes = recipeLibrary.getAllRecipes();
        
        // Then
        assertThat(allRecipes)
            .as("Should have MigrateSaaj recipe")
            .anyMatch(r -> r.name().equals("MigrateSaaj"));
    }
    
    @Test
    @DisplayName("Should have MigrateAuthorization recipe")
    void shouldHaveMigrateAuthorizationRecipe() {
        // When
        List<Recipe> allRecipes = recipeLibrary.getAllRecipes();
        
        // Then
        assertThat(allRecipes)
            .as("Should have MigrateAuthorization recipe")
            .anyMatch(r -> r.name().equals("MigrateAuthorization"));
    }
    
    @Test
    @DisplayName("Should be able to get recipe by name")
    void shouldBeAbleToGetRecipeByName() {
        // When & Then
        assertThat(recipeLibrary.getRecipe("AddJakartaNamespace")).isPresent();
        assertThat(recipeLibrary.getRecipe("UpdatePersistenceXml")).isPresent();
        assertThat(recipeLibrary.getRecipe("MigrateServletApi")).isPresent();
        assertThat(recipeLibrary.getRecipe("NonExistent")).isEmpty();
    }
    
    @Test
    @DisplayName("Should correctly identify Jakarta-specific recipes")
    void shouldCorrectlyIdentifyJakartaRecipes() {
        // When
        List<Recipe> jakartaRecipes = recipeLibrary.getJakartaRecipes();
        
        // Then - should find recipes containing Jakarta, Persistence, or Web in name
        assertThat(jakartaRecipes)
            .as("Should have at least some Jakarta-specific recipes")
            .isNotEmpty();
    }
}
