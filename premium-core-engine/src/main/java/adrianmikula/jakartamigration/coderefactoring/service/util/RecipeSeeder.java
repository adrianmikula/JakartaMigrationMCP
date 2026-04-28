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
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility to seed default recipes and upgrade recommendations into central store.
 * All recipes are loaded from recipes.json to keep configuration centralized.
 * Implements soft-delete semantics: recipes are never deleted, only marked archived.
 */
@Slf4j
public class RecipeSeeder {

    private static final String RECIPES_JSON = "recipes.json";
    private static final String VERSION_PROPERTIES = "version.properties";

    /**
     * Seeds default recipes from recipes.json with versioning and soft-delete support.
     * - New recipes are added with the current plugin version
     * - Existing recipes are never modified (immutable semantics)
     * - Recipes no longer in config are marked as archived (not deleted)
     *
     * @param store the central migration analysis store
     */
    public static void seedDefaultRecipes(CentralMigrationAnalysisStore store) {
        String pluginVersion = getPluginVersion();
        log.info("Seeding default migration recipes from {} (plugin version: {})...", RECIPES_JSON, pluginVersion);

        // Load current recipes from config
        List<Map<String, Object>> jsonRecipes = loadRecipesFromJson();
        log.info("Loaded {} recipes from JSON config", jsonRecipes != null ? jsonRecipes.size() : 0);

        if (jsonRecipes == null || jsonRecipes.isEmpty()) {
            log.error("CRITICAL: No recipes found in {}!", RECIPES_JSON);
            return;
        }

        // Get existing recipes from database
        List<RecipeDefinition> existingRecipes = store.getAllRecipes();
        Set<String> existingNames = existingRecipes.stream()
                .map(RecipeDefinition::getName)
                .collect(Collectors.toSet());
        log.info("Found {} existing recipes in database", existingRecipes.size());

        // Build set of current recipe names from config
        Set<String> currentNames = jsonRecipes.stream()
                .map(r -> (String) r.get("name"))
                .collect(Collectors.toSet());

        // Step 1: Mark archived any existing recipes not in current config
        int archivedCount = 0;
        for (RecipeDefinition existing : existingRecipes) {
            if (!currentNames.contains(existing.getName()) && !existing.isArchived()) {
                log.info("Recipe '{}' no longer in config, marking as archived", existing.getName());
                store.markRecipeArchived(existing.getName());
                archivedCount++;
            }
        }
        if (archivedCount > 0) {
            log.info("Marked {} recipes as archived", archivedCount);
        }

        // Step 2: Insert new recipes (never update existing)
        int insertedCount = 0;
        int skippedCount = 0;
        for (Map<String, Object> jsonRecipe : jsonRecipes) {
            String name = (String) jsonRecipe.get("name");

            // Skip if recipe already exists (immutable semantics)
            if (existingNames.contains(name)) {
                log.debug("Recipe '{}' already exists, skipping (immutable)", name);
                skippedCount++;
                continue;
            }

            RecipeDefinition recipe = buildRecipeFromJson(jsonRecipe, pluginVersion);
            if (recipe != null) {
                store.insertRecipe(recipe);
                insertedCount++;
                log.info("Inserted new recipe: {} (version: {})", name, pluginVersion);
            }
        }

        log.info("Recipe seeding complete: {} inserted, {} skipped (existing), {} archived",
                insertedCount, skippedCount, archivedCount);
    }

    /**
     * Builds a RecipeDefinition from JSON data.
     */
    private static RecipeDefinition buildRecipeFromJson(Map<String, Object> jsonRecipe, String pluginVersion) {
        String name = (String) jsonRecipe.get("name");
        String description = (String) jsonRecipe.get("description");
        String safety = (String) jsonRecipe.get("safety");
        Boolean reversible = (Boolean) jsonRecipe.get("reversible");
        String fileFilter = (String) jsonRecipe.get("fileFilter");
        String openRewriteClass = (String) jsonRecipe.get("openRewriteClass");
        String pattern = (String) jsonRecipe.get("pattern");

        @SuppressWarnings("unchecked")
        List<Map<String, String>> replacements = (List<Map<String, String>>) jsonRecipe.get("replacements");

        RecipeDefinition.RecipeDefinitionBuilder builder = RecipeDefinition.builder()
                .name(name)
                .description(description)
                .category(determineCategory(jsonRecipe))
                .reversible(reversible != null ? reversible : true)
                .pattern(pattern)
                .filePattern(fileFilter)
                .addedInPluginVersion(pluginVersion)
                .archived(false);

        // Extract replacement string from replacements list if present
        if (replacements != null && !replacements.isEmpty()) {
            StringBuilder replacementBuilder = new StringBuilder();
            for (int i = 0; i < replacements.size(); i++) {
                Map<String, String> repl = replacements.get(i);
                if (i > 0) replacementBuilder.append("; ");
                replacementBuilder.append(repl.get("from")).append(" -> ").append(repl.get("to"));
            }
            builder.replacement(replacementBuilder.toString());
        }

        if (openRewriteClass != null && !openRewriteClass.isEmpty()) {
            builder.recipeType(RecipeType.OPENREWRITE);
            builder.openRewriteRecipeName(openRewriteClass);
        } else {
            log.warn("Recipe '{}' has no openRewriteClass defined, defaulting to OPENREWRITE", name);
            builder.recipeType(RecipeType.OPENREWRITE);
        }

        return builder.build();
    }

    private static RecipeCategory determineCategory(Map<String, Object> recipe) {
        String categoryStr = (String) recipe.get("category");
        if (categoryStr != null) {
            try {
                return RecipeCategory.valueOf(categoryStr);
            } catch (IllegalArgumentException e) {
                log.warn("Unknown category '{}' for recipe '{}', using CONFIGURATION as fallback",
                        categoryStr, recipe.get("name"));
                return RecipeCategory.CONFIGURATION;
            }
        }
        return RecipeCategory.CONFIGURATION;
    }

    /**
     * Gets the current plugin version from version.properties file.
     */
    private static String getPluginVersion() {
        try (InputStream is = RecipeSeeder.class.getClassLoader().getResourceAsStream(VERSION_PROPERTIES)) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                String version = props.getProperty("version");
                if (version != null && !version.isEmpty()) {
                    return version;
                }
            }
        } catch (Exception e) {
            log.warn("Could not load version from {}: {}", VERSION_PROPERTIES, e.getMessage());
        }

        // Fallback: try to read from gradle.properties
        return "1.0.15"; // Default fallback version
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> loadRecipesFromJson() {
        try (InputStream is = RecipeSeeder.class.getClassLoader().getResourceAsStream(RECIPES_JSON)) {
            if (is == null) {
                log.warn("{} not found on classpath", RECIPES_JSON);
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
        log.info("Seeding upgrade recommendations from {}...", RECIPES_JSON);

        List<Map<String, Object>> recommendations = loadUpgradeRecommendationsFromJson();
        if (recommendations != null && !recommendations.isEmpty()) {
            log.info("Found {} upgrade recommendations", recommendations.size());

            int seededCount = 0;
            String pluginVersion = getPluginVersion();

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
                        .category(RecipeCategory.OTHER)
                        .addedInPluginVersion(pluginVersion)
                        .archived(false)
                        .build();

                store.insertRecipe(recipe);
                seededCount++;
            }

            log.info("Finished seeding {} upgrade recommendations.", seededCount);
        } else {
            log.info("No upgrade recommendations found in {}", RECIPES_JSON);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> loadUpgradeRecommendationsFromJson() {
        try (InputStream is = RecipeSeeder.class.getClassLoader().getResourceAsStream(RECIPES_JSON)) {
            if (is == null) {
                log.warn("{} not found on classpath, no upgrade recommendations loaded", RECIPES_JSON);
                return Collections.emptyList();
            }

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> data = mapper.readValue(is, new TypeReference<Map<String, Object>>() {});
            return (List<Map<String, Object>>) data.get("upgradeRecommendations");
        } catch (Exception e) {
            log.error("Failed to load upgrade recommendations from {}", RECIPES_JSON, e);
            return Collections.emptyList();
        }
    }
}
