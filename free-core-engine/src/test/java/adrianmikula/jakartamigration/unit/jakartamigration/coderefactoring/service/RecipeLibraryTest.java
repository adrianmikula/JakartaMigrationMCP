package unit.jakartamigration.coderefactoring.service;

import adrianmikula.jakartamigration.coderefactoring.domain.Recipe;
import adrianmikula.jakartamigration.coderefactoring.domain.SafetyLevel;
import adrianmikula.jakartamigration.coderefactoring.service.RecipeLibrary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RecipeLibrary Tests")
class RecipeLibraryTest {
    
    private final RecipeLibrary recipeLibrary = new RecipeLibrary();
    
    @Test
    @DisplayName("Should get Jakarta namespace recipe by name")
    void shouldGetRecipeByName() {
        // When
        Optional<Recipe> recipe = recipeLibrary.getRecipe("AddJakartaNamespace");
        
        // Then
        assertThat(recipe).isPresent();
        assertThat(recipe.get().name()).isEqualTo("AddJakartaNamespace");
        assertThat(recipe.get().safety()).isEqualTo(SafetyLevel.HIGH);
    }
    
    @Test
    @DisplayName("Should return empty when recipe not found")
    void shouldReturnEmptyWhenRecipeNotFound() {
        // When
        Optional<Recipe> recipe = recipeLibrary.getRecipe("NonExistentRecipe");
        
        // Then
        assertThat(recipe).isEmpty();
    }
    
    @Test
    @DisplayName("Should get all Jakarta migration recipes")
    void shouldGetJakartaRecipes() {
        // When
        List<Recipe> recipes = recipeLibrary.getJakartaRecipes();
        
        // Then
        assertThat(recipes).isNotEmpty();
        assertThat(recipes).anyMatch(r -> r.name().equals("AddJakartaNamespace"));
        assertThat(recipes).anyMatch(r -> r.name().equals("UpdatePersistenceXml"));
        assertThat(recipes).anyMatch(r -> r.name().equals("UpdateWebXml"));
    }
    
    @Test
    @DisplayName("Should register custom recipe")
    void shouldRegisterCustomRecipe() {
        // Given
        Recipe customRecipe = new Recipe(
            "CustomRecipe",
            "Custom description",
            "Custom pattern",
            SafetyLevel.MEDIUM,
            false
        );
        
        // When
        recipeLibrary.registerRecipe(customRecipe);
        Optional<Recipe> retrieved = recipeLibrary.getRecipe("CustomRecipe");
        
        // Then
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get()).isEqualTo(customRecipe);
    }

    @Test
    @DisplayName("Should throw exception when registering null recipe")
    void shouldThrowExceptionWhenRegisteringNullRecipe() {
        // Given & When & Then
        assertThatThrownBy(() -> recipeLibrary.registerRecipe(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cannot be null");
    }

    @Test
    @DisplayName("Should return all registered recipes")
    void shouldGetAllRecipes() {
        // When
        List<Recipe> allRecipes = recipeLibrary.getAllRecipes();
        
        // Then
        assertThat(allRecipes).isNotEmpty();
        assertThat(allRecipes.size()).isGreaterThanOrEqualTo(3); // Default recipes
        assertThat(allRecipes).anyMatch(r -> r.name().equals("AddJakartaNamespace"));
        assertThat(allRecipes).anyMatch(r -> r.name().equals("UpdatePersistenceXml"));
        assertThat(allRecipes).anyMatch(r -> r.name().equals("UpdateWebXml"));
    }

    @Test
    @DisplayName("Should check if recipe exists")
    void shouldCheckIfRecipeExists() {
        // Given
        String existingRecipe = "AddJakartaNamespace";
        String nonExistentRecipe = "NonExistentRecipe";
        
        // When & Then
        assertThat(recipeLibrary.hasRecipe(existingRecipe)).isTrue();
        assertThat(recipeLibrary.hasRecipe(nonExistentRecipe)).isFalse();
    }
}

