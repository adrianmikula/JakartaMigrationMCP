/*
 * Copyright 2024 Adrian Kozak
 * Copyright 2024 Prairie Trail Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package adrianmikula.jakartamigration.coderefactoring.service;

import adrianmikula.jakartamigration.coderefactoring.domain.Recipe;

import java.util.List;

/**
 * Library of available refactoring recipes.
 * 
 * NOTE: This is a community stub. Full implementation with OpenRewrite recipes
 * is available in the premium edition.
 */
public class RecipeLibrary {
    
    /**
     * Gets all available recipes.
     * 
     * @return List of available recipes (empty in community edition)
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
