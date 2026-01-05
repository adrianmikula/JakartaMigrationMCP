package unit.jakartamigration.coderefactoring.service;

import com.bugbounty.jakartamigration.coderefactoring.domain.Recipe;
import com.bugbounty.jakartamigration.coderefactoring.domain.SafetyLevel;
import com.bugbounty.jakartamigration.coderefactoring.service.RecipeLibrary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

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
}

