package adrianmikula.jakartamigration.coderefactoring.service.util;

import adrianmikula.jakartamigration.analysis.persistence.CentralMigrationAnalysisStore;
import adrianmikula.jakartamigration.coderefactoring.domain.RecipeCategory;
import adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition;
import adrianmikula.jakartamigration.coderefactoring.domain.RecipeType;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility to seed default recipes and upgrade recommendations into the central store.
 * All recipes are loaded from recipes.yaml to keep configuration centralized.
 */
@Slf4j
public class RecipeSeeder {

        // Mapping from YAML recipe names to OpenRewrite recipe class names
        // Only includes recipes that actually exist in OpenRewrite
        private static final Map<String, String> RECIPE_NAME_TO_OPENREWRITE = new HashMap<>();
        static {
                // Jakarta EE namespace migrations - verified to exist in OpenRewrite
                RECIPE_NAME_TO_OPENREWRITE.put("MigrateServlet", "org.openrewrite.java.migrate.jakarta.JavaxServletToJakartaServlet");
                RECIPE_NAME_TO_OPENREWRITE.put("MigrateServletApi", "org.openrewrite.java.migrate.jakarta.JavaxServletToJakartaServlet");
                RECIPE_NAME_TO_OPENREWRITE.put("MigrateJPA", "org.openrewrite.java.migrate.jakarta.JavaxPersistenceToJakartaPersistence");
                // NOTE: JavaxValidationToJakartaValidation doesn't exist - using JavaxValidationMigrationToJakartaValidation
                RECIPE_NAME_TO_OPENREWRITE.put("MigrateBeanValidation", "org.openrewrite.java.migrate.jakarta.JavaxValidationMigrationToJakartaValidation");
                RECIPE_NAME_TO_OPENREWRITE.put("JavaxValidationToJakartaValidation", "org.openrewrite.java.migrate.jakarta.JavaxValidationMigrationToJakartaValidation");
                // CDI - use JavaxEnterpriseToJakartaEnterprise which exists
                RECIPE_NAME_TO_OPENREWRITE.put("MigrateCDI", "org.openrewrite.java.migrate.jakarta.JavaxEnterpriseToJakartaEnterprise");
                RECIPE_NAME_TO_OPENREWRITE.put("MigrateREST", "org.openrewrite.java.migrate.jakarta.JavaxWsToJakartaWs");
                RECIPE_NAME_TO_OPENREWRITE.put("MigrateSOAP", "org.openrewrite.java.migrate.jakarta.JavaxXmlSoapToJakartaXmlSoap");
                RECIPE_NAME_TO_OPENREWRITE.put("MigrateJMS", "org.openrewrite.java.migrate.jakarta.JavaxJmsToJakartaJms");
                RECIPE_NAME_TO_OPENREWRITE.put("MigrateBatch", "org.openrewrite.java.migrate.jakarta.JavaxBatchMigrationToJakartaBatch");
                RECIPE_NAME_TO_OPENREWRITE.put("MigrateMail", "org.openrewrite.java.migrate.jakarta.JavaxMailToJakartaMail");
                RECIPE_NAME_TO_OPENREWRITE.put("MigrateJTA", "org.openrewrite.java.migrate.jakarta.JavaxTransactionMigrationToJakartaTransaction");
                RECIPE_NAME_TO_OPENREWRITE.put("MigrateEJB", "org.openrewrite.java.migrate.jakarta.JavaxEjbToJakartaEjb");
                RECIPE_NAME_TO_OPENREWRITE.put("MigrateJSF", "org.openrewrite.java.migrate.jakarta.JavaxFacesToJakartaFaces");
                // WebSocket - use JavaxWebsocketToJakartaWebsocket (correct spelling)
                RECIPE_NAME_TO_OPENREWRITE.put("MigrateWebSocket", "org.openrewrite.java.migrate.jakarta.JavaxWebsocketToJakartaWebsocket");
                // JSONP - use JavaxJsonToJakartaJson
                RECIPE_NAME_TO_OPENREWRITE.put("MigrateJSONP", "org.openrewrite.java.migrate.jakarta.JavaxJsonToJakartaJson");
                // JSONB - doesn't exist as separate recipe, use JavaxJsonToJakartaJson
                RECIPE_NAME_TO_OPENREWRITE.put("MigrateJSONB", "org.openrewrite.java.migrate.jakarta.JavaxJsonToJakartaJson");
                RECIPE_NAME_TO_OPENREWRITE.put("MigrateSecurity", "org.openrewrite.java.migrate.jakarta.JavaxSecurityToJakartaSecurity");
                // Concurrency - doesn't exist, skip
                // JCA - doesn't exist, skip
                RECIPE_NAME_TO_OPENREWRITE.put("MigrateJAXB", "org.openrewrite.java.migrate.jakarta.JavaxXmlBindMigrationToJakartaXmlBind");
                // JAXRPC - use JavaxXmlWsMigrationToJakartaXmlWs
                RECIPE_NAME_TO_OPENREWRITE.put("MigrateJAXRPC", "org.openrewrite.java.migrate.jakarta.JavaxXmlWsMigrationToJakartaXmlWs");
                // JASPIC - doesn't exist, skip
                RECIPE_NAME_TO_OPENREWRITE.put("MigrateAnnotation", "org.openrewrite.java.migrate.jakarta.JavaxAnnotationMigrationToJakartaAnnotation");
                RECIPE_NAME_TO_OPENREWRITE.put("MigrateActivation", "org.openrewrite.java.migrate.jakarta.JavaxActivationMigrationToJakartaActivation");
                RECIPE_NAME_TO_OPENREWRITE.put("MigrateEL", "org.openrewrite.java.migrate.jakarta.JavaxElToJakartaEl");
                RECIPE_NAME_TO_OPENREWRITE.put("MigrateInterceptor", "org.openrewrite.java.migrate.jakarta.JavaxInterceptorToJakartaInterceptor");
                RECIPE_NAME_TO_OPENREWRITE.put("MigrateResource", "org.openrewrite.java.migrate.jakarta.JavaxResourceToJakartaResource");
                RECIPE_NAME_TO_OPENREWRITE.put("MigrateSaaj", "org.openrewrite.java.migrate.jakarta.JavaxXmlSoapToJakartaXmlSoap");
                RECIPE_NAME_TO_OPENREWRITE.put("MigrateAuthorization", "org.openrewrite.java.migrate.jakarta.JavaxAuthorizationMigrationToJakartaAuthorization");
        }

        public static void seedDefaultRecipes(CentralMigrationAnalysisStore store) {
                log.info("Seeding default migration recipes from recipes.yaml...");

                // Load recipes from YAML
                List<Map<String, Object>> yamlRecipes = loadRecipesFromYaml();
                log.info("Loaded {} recipes from YAML", yamlRecipes != null ? yamlRecipes.size() : 0);
                
                if (yamlRecipes == null || yamlRecipes.isEmpty()) {
                        log.error("CRITICAL: No recipes found in recipes.yaml! Falling back to legacy seeding.");
                        seedLegacyRecipes(store);
                        return;
                }

                // Delete all existing recipes to ensure DB matches YAML exactly
                // This handles cases where recipes are removed from YAML
                store.deleteAllRecipes();
                log.info("Cleared existing recipes from database");

                // Seed each recipe from YAML
                int seededCount = 0;
                for (Map<String, Object> yamlRecipe : yamlRecipes) {
                        String name = (String) yamlRecipe.get("name");
                        log.info("Seeding recipe from YAML: name={}", name);
                        String description = (String) yamlRecipe.get("description");
                        String pattern = (String) yamlRecipe.get("pattern");
                        String safety = (String) yamlRecipe.get("safety");
                        Boolean reversible = (Boolean) yamlRecipe.get("reversible");
                        String fileFilter = (String) yamlRecipe.get("fileFilter");
                        @SuppressWarnings("unchecked")
                        List<Map<String, String>> replacements = (List<Map<String, String>>) yamlRecipe.get("replacements");

                        RecipeDefinition recipe = RecipeDefinition.builder()
                                .name(name)
                                .description(description)
                                .category(determineCategory(name, fileFilter))
                                .reversible(reversible != null ? reversible : true)
                                .build();

                        // Check if this recipe has an OpenRewrite mapping
                        String openRewriteRecipeName = RECIPE_NAME_TO_OPENREWRITE.get(name);
                        log.info("Recipe '{}' - OpenRewrite mapping: {}", name, openRewriteRecipeName);
                        if (openRewriteRecipeName != null) {
                                recipe.setRecipeType(RecipeType.OPENREWRITE);
                                recipe.setOpenRewriteRecipeName(openRewriteRecipeName);
                                log.info("Recipe '{}' - Set to OPENREWRITE with class: {}", name, openRewriteRecipeName);
                        } else if (fileFilter != null && !fileFilter.isEmpty()) {
                                // XML recipes with file filters use REGEX
                                recipe.setRecipeType(RecipeType.REGEX);
                                recipe.setFilePattern(fileFilter);
                                if (replacements != null && !replacements.isEmpty()) {
                                        // Use first replacement as pattern/replacement
                                        recipe.setPattern(replacements.get(0).get("from"));
                                        recipe.setReplacement(replacements.get(0).get("to"));
                                }
                        } else if (replacements != null && !replacements.isEmpty()) {
                                // Default to REGEX if has replacements
                                recipe.setRecipeType(RecipeType.REGEX);
                                recipe.setPattern(replacements.get(0).get("from"));
                                recipe.setReplacement(replacements.get(0).get("to"));
                        } else {
                                // Default to OPENREWRITE if no other info
                                recipe.setRecipeType(RecipeType.OPENREWRITE);
                        }

                        store.saveRecipe(recipe);
                        seededCount++;
                }

                log.info("Finished seeding {} recipes from recipes.yaml.", seededCount);
        }

        /**
         * Determines the category based on recipe name and file filter.
         */
        private static RecipeCategory determineCategory(String name, String fileFilter) {
                if (fileFilter != null) {
                        if (fileFilter.contains("web.xml") || fileFilter.contains("persistence.xml")) {
                                return RecipeCategory.XML;
                        }
                        if (fileFilter.contains("pom.xml") || fileFilter.contains("build.gradle")) {
                                return RecipeCategory.BUILD_DEPENDENCIES;
                        }
                }

                String lowerName = name.toLowerCase();
                if (lowerName.contains("annotation") || lowerName.contains("@")) {
                        return RecipeCategory.ANNOTATIONS;
                }
                if (lowerName.contains("xml") || lowerName.contains("web.xml") || lowerName.contains("persistence")) {
                        return RecipeCategory.XML;
                }
                if (lowerName.contains("dependency") || lowerName.contains("maven") || lowerName.contains("hibernate") || lowerName.contains("gradle")) {
                        return RecipeCategory.BUILD_DEPENDENCIES;
                }
                
                return RecipeCategory.JAVA;
        }

        /**
         * Fallback method for legacy hardcoded recipes if YAML loading fails.
         */
        private static void seedLegacyRecipes(CentralMigrationAnalysisStore store) {
                log.info("Seeding legacy recipes (fallback)...");

                // Java Source Recipes
                store.saveRecipe(RecipeDefinition.builder()
                        .name("Migrate javax.servlet to jakarta.servlet")
                        .description("Replaces javax.servlet imports with jakarta.servlet equivalents.")
                        .category(RecipeCategory.JAVA)
                        .recipeType(RecipeType.OPENREWRITE)
                        .openRewriteRecipeName("org.openrewrite.java.migrate.jakarta.JavaxServletToJakartaServlet")
                        .reversible(true)
                        .build());

                store.saveRecipe(RecipeDefinition.builder()
                        .name("Migrate JSF to Jakarta Faces")
                        .description("Replaces javax.faces imports with jakarta.faces equivalents.")
                        .category(RecipeCategory.JAVA)
                        .recipeType(RecipeType.OPENREWRITE)
                        .openRewriteRecipeName("org.openrewrite.java.migrate.jakarta.JavaxFacesToJakartaFaces")
                        .reversible(true)
                        .build());

                // XML Recipes
                store.saveRecipe(RecipeDefinition.builder()
                        .name("Update web.xml to Jakarta EE 9+")
                        .description("Updates web.xml namespaces and version to Jakarta EE equivalents.")
                        .category(RecipeCategory.XML)
                        .recipeType(RecipeType.REGEX)
                        .pattern("xmlns=\"http://xmlns\\.jcp\\.org/xml/ns/javaee\"")
                        .replacement("xmlns=\"https://jakarta.ee/xml/ns/jakartaee\"")
                        .filePattern("**/web.xml")
                        .reversible(true)
                        .build());

                log.info("Finished seeding legacy recipes.");
        }

        /**
         * Seeds upgrade recommendations from recipes.yaml configuration.
         */
        public static void seedUpgradeRecommendations(CentralMigrationAnalysisStore store) {
                log.info("Seeding upgrade recommendations from recipes.yaml...");

                List<RecipesYamlConfig.UpgradeRecommendationConfig> recommendations = loadUpgradeRecommendationsFromYaml();
                for (RecipesYamlConfig.UpgradeRecommendationConfig rec : recommendations) {
                        store.saveUpgradeRecommendation(
                                rec.getCurrentGroupId(),
                                rec.getCurrentArtifactId(),
                                rec.getRecommendedGroupId(),
                                rec.getRecommendedArtifactId(),
                                rec.getRecommendedVersion(),
                                rec.getAssociatedRecipeName());
                }

                log.info("Finished seeding {} upgrade recommendations.", recommendations.size());
        }

        @SuppressWarnings("unchecked")
        private static List<Map<String, Object>> loadRecipesFromYaml() {
                try (InputStream is = RecipeSeeder.class.getClassLoader().getResourceAsStream("recipes.yaml")) {
                        if (is == null) {
                                log.warn("recipes.yaml not found on classpath");
                                return Collections.emptyList();
                        }
                        Yaml yaml = new Yaml(new Constructor(RecipesYamlConfig.class, new LoaderOptions()));
                        RecipesYamlConfig config = yaml.load(is);
                        if (config == null || config.getRecipes() == null) {
                                return Collections.emptyList();
                        }
                        return config.getRecipes();
                } catch (Exception e) {
                        log.error("Failed to load recipes from recipes.yaml", e);
                        return Collections.emptyList();
                }
        }

        private static List<RecipesYamlConfig.UpgradeRecommendationConfig> loadUpgradeRecommendationsFromYaml() {
                try (InputStream is = RecipeSeeder.class.getClassLoader().getResourceAsStream("recipes.yaml")) {
                        if (is == null) {
                                log.warn("recipes.yaml not found on classpath, no upgrade recommendations loaded");
                                return Collections.emptyList();
                        }
                        Yaml yaml = new Yaml(new Constructor(RecipesYamlConfig.class, new LoaderOptions()));
                        RecipesYamlConfig config = yaml.load(is);
                        if (config == null || config.getUpgradeRecommendations() == null) {
                                return Collections.emptyList();
                        }
                        return config.getUpgradeRecommendations();
                } catch (Exception e) {
                        log.error("Failed to load upgrade recommendations from recipes.yaml", e);
                        return Collections.emptyList();
                }
        }
}
