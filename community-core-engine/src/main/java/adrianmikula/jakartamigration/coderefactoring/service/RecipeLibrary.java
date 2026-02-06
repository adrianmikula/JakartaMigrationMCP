package adrianmikula.jakartamigration.coderefactoring.service;

import adrianmikula.jakartamigration.coderefactoring.domain.Recipe;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Library of refactoring recipes.
 */
public class RecipeLibrary {
    
    private final Map<String, Recipe> recipes = new ConcurrentHashMap<>();
    
    public RecipeLibrary() {
        // Register default Jakarta migration recipes
        // Use private method to avoid calling overridable method in constructor
        registerRecipeInternal(Recipe.jakartaNamespaceRecipe());
        registerRecipeInternal(Recipe.persistenceXmlRecipe());
        registerRecipeInternal(Recipe.webXmlRecipe());
    }
    
    /**
     * Gets a recipe by name.
     */
    public Optional<Recipe> getRecipe(String name) {
        return Optional.ofNullable(recipes.get(name));
    }
    
    /**
     * Gets all Jakarta migration recipes.
     */
    public List<Recipe> getJakartaRecipes() {
        return recipes.values().stream()
            .filter(recipe -> recipe.name().contains("Jakarta") || 
                             recipe.name().contains("Persistence") ||
                             recipe.name().contains("Web"))
            .toList();
    }
    
    /**
     * Registers a new recipe.
     */
    public final void registerRecipe(Recipe recipe) {
        registerRecipeInternal(recipe);
    }
    
    /**
     * Internal method to register a recipe (not overridable).
     */
    private void registerRecipeInternal(Recipe recipe) {
        if (recipe == null) {
            throw new IllegalArgumentException("Recipe cannot be null");
        }
        recipes.put(recipe.name(), recipe);
    }
    
    /**
     * Returns all registered recipes.
     */
    public List<Recipe> getAllRecipes() {
        return new ArrayList<>(recipes.values());
    }
    
    /**
     * Checks if a recipe exists.
     */
    public boolean hasRecipe(String name) {
        return recipes.containsKey(name);
    }
}

