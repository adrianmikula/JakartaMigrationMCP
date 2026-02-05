package unit.jakartamigration.coderefactoring;

import adrianmikula.jakartamigration.coderefactoring.domain.Recipe;
import adrianmikula.jakartamigration.coderefactoring.domain.SafetyLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Recipe Tests")
class RecipeTest {
    
    @Test
    @DisplayName("Should create recipe with all fields")
    void shouldCreateRecipe() {
        // Given
        String name = "AddJakartaNamespace";
        String description = "Converts javax.* to jakarta.*";
        String pattern = "javax.* → jakarta.*";
        SafetyLevel safety = SafetyLevel.HIGH;
        boolean reversible = true;
        
        // When
        Recipe recipe = new Recipe(name, description, pattern, safety, reversible);
        
        // Then
        assertThat(recipe.name()).isEqualTo(name);
        assertThat(recipe.description()).isEqualTo(description);
        assertThat(recipe.pattern()).isEqualTo(pattern);
        assertThat(recipe.safety()).isEqualTo(safety);
        assertThat(recipe.reversible()).isTrue();
    }
    
    @Test
    @DisplayName("Should throw exception when name is null")
    void shouldThrowWhenNameIsNull() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            new Recipe(null, "Description", "Pattern", SafetyLevel.HIGH, true)
        );
    }
    
    @Test
    @DisplayName("Should throw exception when name is blank")
    void shouldThrowWhenNameIsBlank() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            new Recipe("  ", "Description", "Pattern", SafetyLevel.HIGH, true)
        );
    }
    
    @Test
    @DisplayName("Should create Jakarta namespace recipe")
    void shouldCreateJakartaNamespaceRecipe() {
        // When
        Recipe recipe = Recipe.jakartaNamespaceRecipe();
        
        // Then
        assertThat(recipe.name()).isEqualTo("AddJakartaNamespace");
        assertThat(recipe.safety()).isEqualTo(SafetyLevel.HIGH);
        assertThat(recipe.reversible()).isTrue();
    }
}

