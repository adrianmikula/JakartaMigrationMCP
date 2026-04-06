package adrianmikula.jakartamigration.coderefactoring.service.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;

import adrianmikula.jakartamigration.coderefactoring.domain.RecipeCategory;
import adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition;
import adrianmikula.jakartamigration.coderefactoring.domain.RecipeType;
import adrianmikula.jakartamigration.analysis.persistence.CentralMigrationAnalysisStore;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Utility to seed default recipes and upgrade recommendations into central store.
 * All recipes are loaded from recipes.json to keep configuration centralized.
 */
@Slf4j
public class RecipeSeeder {

        public static void seedDefaultRecipes(CentralMigrationAnalysisStore store) {
                log.info("Seeding default migration recipes from recipes.json...");

                List<Map<String, Object>> jsonRecipes = loadRecipesFromJson();
                log.info("Loaded {} recipes from JSON", jsonRecipes != null ? jsonRecipes.size() : 0);
                
                if (jsonRecipes == null || jsonRecipes.isEmpty()) {
                        log.error("CRITICAL: No recipes found in recipes.json!");
                        return;
                }

                int seededCount = 0;
                for (Map<String, Object> jsonRecipe : jsonRecipes) {
                        String name = (String) jsonRecipe.get("name");
                        String description = (String) jsonRecipe.get("description");
                        String safety = (String) jsonRecipe.get("safety");
                        Boolean reversible = (Boolean) jsonRecipe.get("reversible");
                        String fileFilter = (String) jsonRecipe.get("fileFilter");
                        String openRewriteClass = (String) jsonRecipe.get("openRewriteClass");
                        @SuppressWarnings("unchecked")
                        List<Map<String, String>> replacements = (List<Map<String, String>>) jsonRecipe.get("replacements");

                        RecipeDefinition recipe = RecipeDefinition.builder()
                                .name(name)
                                .description(description)
                                .category(determineCategory(jsonRecipe))
                                .reversible(reversible != null ? reversible : true)
                                .build();

                        if (openRewriteClass == null) {
                                log.error("No OpenRewrite class defined for recipe '{}', skipping", name);
                                continue; // Skip this recipe entirely
                        } else {
                                recipe.setRecipeType(RecipeType.OPENREWRITE);
                                recipe.setOpenRewriteRecipeName(openRewriteClass);
                                
                                // For OpenRewrite recipes, we don't set regex pattern/replacement
                                // The OpenRewrite recipe class handles the transformation
                                
                                store.saveRecipe(recipe);
                                seededCount++;
                        }
                }

                log.info("Finished seeding {} recipes from recipes.json.", seededCount);
        }

        private static RecipeCategory determineCategory(Map<String, Object> recipe) {
                String categoryStr = (String) recipe.get("category");
                if (categoryStr != null) {
                        try {
                                return RecipeCategory.valueOf(categoryStr);
                        } catch (IllegalArgumentException e) {
                                log.warn("Unknown category '{}' for recipe '{}', using CONFIGURATION as fallback", categoryStr, recipe.get("name"));
                                return RecipeCategory.CONFIGURATION;
                        }
                }
                return RecipeCategory.CONFIGURATION;
        }

        @SuppressWarnings("unchecked")
        private static List<Map<String, Object>> loadRecipesFromJson() {
                try (InputStream is = RecipeSeeder.class.getClassLoader().getResourceAsStream("recipes.json")) {
                        if (is == null) {
                                log.warn("recipes.json not found on classpath");
                                return Collections.emptyList();
                        }

                        ObjectMapper mapper = new ObjectMapper();
                        Map<String, Object> data = mapper.readValue(is, new TypeReference<Map<String, Object>>() {});
                        
                        return (List<Map<String, Object>>) data.get("recipes");
                } catch (Exception e) {
                        log.error("Failed to load recipes from JSON", e);
                        return Collections.emptyList();
                }
        }

        public static void seedUpgradeRecommendations(CentralMigrationAnalysisStore store) {
                log.info("Seeding upgrade recommendations from recipes.json...");

                List<Map<String, Object>> recommendations = loadUpgradeRecommendationsFromJson();
                if (recommendations != null && !recommendations.isEmpty()) {
                        log.info("Found {} upgrade recommendations", recommendations.size());
                        
                        int seededCount = 0;
                        for (Map<String, Object> recommendation : recommendations) {
                                String description = (String) recommendation.get("description");
                                String recipeName = (String) recommendation.get("recipeName");
                                String pattern = (String) recommendation.get("pattern");
                                String safety = (String) recommendation.get("safety");
                                Boolean reversible = (Boolean) recommendation.get("reversible");

                                RecipeDefinition recipe = RecipeDefinition.builder()
                                        .name(recipeName)
                                        .description(description)
                                        .pattern(pattern)
                                        .reversible(reversible != null ? reversible : true)
                                        .recipeType(RecipeType.OPENREWRITE)
                                        .build();

                                store.saveRecipe(recipe);
                                seededCount++;
                        }

                        log.info("Finished seeding {} upgrade recommendations.", seededCount);
                } else {
                        log.info("No upgrade recommendations found in recipes.json");
                }
        }

        @SuppressWarnings("unchecked")
        private static List<Map<String, Object>> loadUpgradeRecommendationsFromJson() {
                try (InputStream is = RecipeSeeder.class.getClassLoader().getResourceAsStream("recipes.json")) {
                        if (is == null) {
                                log.warn("recipes.json not found on classpath, no upgrade recommendations loaded");
                                return Collections.emptyList();
                        }

                        ObjectMapper mapper = new ObjectMapper();
                        Map<String, Object> data = mapper.readValue(is, new TypeReference<Map<String, Object>>() {});
                        return (List<Map<String, Object>>) data.get("upgradeRecommendations");
                } catch (Exception e) {
                        log.error("Failed to load upgrade recommendations from recipes.json", e);
                        return Collections.emptyList();
                }
        }
}
