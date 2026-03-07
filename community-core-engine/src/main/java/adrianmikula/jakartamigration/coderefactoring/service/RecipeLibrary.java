package adrianmikula.jakartamigration.coderefactoring.service;

import adrianmikula.jakartamigration.coderefactoring.domain.Recipe;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Library of refactoring recipes.
 */
public class RecipeLibrary {

    private static final java.util.logging.Logger LOG = java.util.logging.Logger
            .getLogger(RecipeLibrary.class.getName());

    private final Map<String, Recipe> recipes = new ConcurrentHashMap<>();

    public RecipeLibrary() {
        // By default, do not hardcode recipes.
        // They will be loaded dynamically from YAML by the service.
        LOG.info("RecipeLibrary: Initialized (empty)");
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
        return new ArrayList<>(recipes.values());
    }

    /**
     * Registers a new recipe.
     */
    public final void registerRecipe(Recipe recipe) {
        registerRecipeInternal(recipe);
    }

    /**
     * Clears all registered recipes.
     */
    public void clearRecipes() {
        recipes.clear();
        LOG.info("RecipeLibrary: All recipes cleared");
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
        LOG.info("RecipeLibrary.getAllRecipes: Returning " + recipes.size() + " recipes");
        return new ArrayList<>(recipes.values());
    }

    /**
     * Checks if a recipe exists.
     */
    public boolean hasRecipe(String name) {
        return recipes.containsKey(name);
    }
}
