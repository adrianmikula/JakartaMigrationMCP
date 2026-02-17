package adrianmikula.jakartamigration.coderefactoring.service;

import adrianmikula.jakartamigration.coderefactoring.domain.Recipe;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Library of refactoring recipes.
 */
public class RecipeLibrary {
    
    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(RecipeLibrary.class.getName());
    
    private final Map<String, Recipe> recipes = new ConcurrentHashMap<>();
    
    public RecipeLibrary() {
        LOG.info("RecipeLibrary: Initializing with 20 recipes...");
        // Register default Jakarta migration recipes
        // Use private method to avoid calling overridable method in constructor
        registerRecipeInternal(Recipe.jakartaNamespaceRecipe());
        registerRecipeInternal(Recipe.persistenceXmlRecipe());
        registerRecipeInternal(Recipe.servletApiRecipe());
        registerRecipeInternal(Recipe.jpaRecipe());
        registerRecipeInternal(Recipe.cdiRecipe());
        registerRecipeInternal(Recipe.jaxbRecipe());
        registerRecipeInternal(Recipe.validatorRecipe());
        registerRecipeInternal(Recipe.ejbRecipe());
        registerRecipeInternal(Recipe.jmsRecipe());
        registerRecipeInternal(Recipe.jaxrsRecipe());
        registerRecipeInternal(Recipe.jaxwsRecipe());
        registerRecipeInternal(Recipe.jtaRecipe());
        registerRecipeInternal(Recipe.javaMailRecipe());
        registerRecipeInternal(Recipe.websocketRecipe());
        registerRecipeInternal(Recipe.jsonbRecipe());
        registerRecipeInternal(Recipe.jsonpRecipe());
        registerRecipeInternal(Recipe.activationRecipe());
        registerRecipeInternal(Recipe.soapRecipe());
        registerRecipeInternal(Recipe.saajRecipe());
        registerRecipeInternal(Recipe.authorizationRecipe());
        LOG.info("RecipeLibrary: Registered " + recipes.size() + " recipes");
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

