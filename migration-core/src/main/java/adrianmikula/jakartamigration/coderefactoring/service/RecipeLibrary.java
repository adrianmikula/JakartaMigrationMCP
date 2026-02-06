/*
 * Copyright 2024 Adrian Kozak
 * Copyright 2024 Prairie Trail Software
 *
 * This software is proprietary and may not be used, copied, modified,
 * or distributed except under the terms of a separate commercial license agreement.
 */
package adrianmikula.jakartamigration.coderefactoring.service;

import adrianmikula.jakartamigration.coderefactoring.domain.Recipe;

import java.util.List;

/**
 * Library of available refactoring recipes.
 * 
 * NOTE: This is a stub. Full implementation with OpenRewrite recipes
 * is available in the premium edition.
 */
public class RecipeLibrary {
    
    /**
     * Gets all available recipes.
     * 
     * @return List of available recipes (empty in stub implementation)
     */
    public List<Recipe> getAvailableRecipes() {
        return List.of();
    }
    
    /**
     * Gets a recipe by name.
     * 
     * @param name Recipe name
     * @return Recipe or null if not found
     */
    public Recipe getRecipe(String name) {
        return null;
    }
    
    /**
     * Gets the Jakarta namespace migration recipe.
     * 
     * @return Jakarta namespace recipe
     */
    public Recipe getJakartaNamespaceRecipe() {
        return Recipe.jakartaNamespaceRecipe();
    }
}
