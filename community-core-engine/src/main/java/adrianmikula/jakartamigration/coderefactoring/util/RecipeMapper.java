package adrianmikula.jakartamigration.coderefactoring.util;

import adrianmikula.jakartamigration.coderefactoring.domain.Recipe;
import lombok.extern.slf4j.Slf4j;
import org.openrewrite.config.Environment;

import java.util.ArrayList;
import java.util.List;

/**
 * Maps domain-specific recipes to OpenRewrite recipes.
 */
@Slf4j
public class RecipeMapper {

    private final Environment environment;

    public RecipeMapper() {
        this.environment = Environment.builder()
                .scanRuntimeClasspath()
                .build();
    }

    /**
     * Maps a list of domain recipes to a single composite OpenRewrite recipe.
     *
     * @param domainRecipes List of domain recipes
     * @return A composite OpenRewrite recipe
     */
    public org.openrewrite.Recipe mapToOpenRewriteRecipe(List<Recipe> domainRecipes) {
        List<org.openrewrite.Recipe> openRewriteRecipes = new ArrayList<>();

        for (Recipe domainRecipe : domainRecipes) {
            org.openrewrite.Recipe orRecipe = mapSingleRecipe(domainRecipe);
            if (orRecipe != null) {
                openRewriteRecipes.add(orRecipe);
            }
        }

        if (openRewriteRecipes.isEmpty()) {
            return null;
        }

        // Return a composite recipe
        return new org.openrewrite.Recipe() {
            @Override
            public String getDisplayName() {
                return "Jakarta Migration Composite Recipe";
            }

            @Override
            public String getDescription() {
                return "A composite recipe built from domain-specific migration tasks.";
            }

            @Override
            public List<org.openrewrite.Recipe> getRecipeList() {
                return openRewriteRecipes;
            }
        };
    }

    private org.openrewrite.Recipe mapSingleRecipe(Recipe domainRecipe) {
        String recipeName = domainRecipe.name();
        String normalizedName = recipeName.toLowerCase();

        // Map common recipes to standardized OpenRewrite migration sets
        switch (normalizedName) {
            case "addjakartanamespace":
                return activateRecipe("org.openrewrite.java.migrate.jakarta.JavaxMigrationToJakarta");
            case "migratejpa":
                return activateRecipe("org.openrewrite.java.migrate.jakarta.JavaxPersistenceToJakartaPersistence");
            case "migratejaxrs":
            case "migraterest":
                return activateRecipe("org.openrewrite.java.migrate.jakarta.JavaxMigrationToJakarta");
            case "migrateejb":
                return activateRecipe("org.openrewrite.java.migrate.jakarta.JavaxEjbToJakartaEjb");
            case "migratejms":
                return activateRecipe("org.openrewrite.java.migrate.jakarta.JavaxJmsToJakartaJms");
            case "migratejta":
                return activateRecipe(
                        "org.openrewrite.java.migrate.jakarta.JavaxTransactionMigrationToJakartaTransaction");
            case "migratevalidator":
            case "migratebeanvalidation":
                return activateRecipe(
                        "org.openrewrite.java.migrate.jakarta.JavaxValidationMigrationToJakartaValidation");
            case "migratejaxb":
                return activateRecipe("org.openrewrite.java.migrate.jakarta.JavaxXmlBindMigrationToJakartaXmlBind");
            case "migrateservlet":
            case "migrateservletapi":
                return activateRecipe("org.openrewrite.java.migrate.jakarta.JavaxServletToJakartaServlet");
            case "migratecdi":
                return activateRecipe("org.openrewrite.java.migrate.jakarta.JavaxInjectMigrationToJakartaInject");
            case "migratejavamail":
            case "migratemail":
                return activateRecipe("org.openrewrite.java.migrate.jakarta.JavaxMailToJakartaMail");
            case "migratebatch":
                return activateRecipe("org.openrewrite.java.migrate.jakarta.JavaxBatchMigrationToJakartaBatch");
            case "migratejsf":
                return activateRecipe("org.openrewrite.java.migrate.jakarta.JavaxFacesToJakartaFaces");
            case "migratewebsocket":
                return activateRecipe("org.openrewrite.java.migrate.jakarta.JavaxWebsocketToJakartaWebsocket");
            case "migratejsonp":
                return activateRecipe("org.openrewrite.java.migrate.jakarta.JavaxJsonToJakartaJson");
            case "migratejsonb":
                return activateRecipe("org.openrewrite.java.migrate.jakarta.JavaxMigrationToJakarta");
            case "migrateactivation":
                return activateRecipe(
                        "org.openrewrite.java.migrate.jakarta.JavaxActivationMigrationToJakartaActivation");
            case "migrateel":
                return activateRecipe("org.openrewrite.java.migrate.jakarta.JavaxElToJakartaEl");
            case "migratejaxws":
            case "migratesoap":
                return activateRecipe("org.openrewrite.java.migrate.jakarta.JavaxXmlWsMigrationToJakartaXmlWs");
            case "migratesecurity":
                return activateRecipe("org.openrewrite.java.migrate.jakarta.JavaxSecurityToJakartaSecurity");
            case "migrateconcurrency":
                return activateRecipe("org.openrewrite.java.migrate.jakarta.JavaxMigrationToJakarta");
            case "migratejca":
                return activateRecipe("org.openrewrite.java.migrate.jakarta.JavaxResourceToJakartaResource");
            case "migratejaspic":
                return activateRecipe("org.openrewrite.java.migrate.jakarta.JavaxMigrationToJakarta");
            case "migratejaxrpc":
                return activateRecipe("org.openrewrite.java.migrate.jakarta.JavaxMigrationToJakarta");
            case "migrateinterceptor":
                return activateRecipe("org.openrewrite.java.migrate.jakarta.JavaxInterceptorToJakartaInterceptor");
            case "migrateresource":
                return activateRecipe(
                        "org.openrewrite.java.migrate.jakarta.JavaxAnnotationMigrationToJakartaAnnotation");
            case "migratesaaj":
                return activateRecipe("org.openrewrite.java.migrate.jakarta.JavaxXmlSoapToJakartaXmlSoap");
            case "migrateauthorization":
                return activateRecipe("org.openrewrite.java.migrate.jakarta.JavaxSecurityToJakartaSecurity");
            case "migrateannotation":
                return activateRecipe(
                        "org.openrewrite.java.migrate.jakarta.JavaxAnnotationMigrationToJakartaAnnotation");
            default:
                log.warn(
                        "No specific OpenRewrite mapping found for recipe: {}. Using generic AddJakartaNamespace if appropriate.",
                        recipeName);
                if (recipeName.startsWith("Migrate"))
                    return activateRecipe("org.openrewrite.java.migrate.jakarta.JavaxMigrationToJakarta");
                return null;
        }
    }

    private org.openrewrite.Recipe activateRecipe(String recipeName) {
        try {
            org.openrewrite.Recipe recipe = environment.activateRecipes(recipeName);
            if (recipe == null) {
                log.error("Could not activate OpenRewrite recipe: {}", recipeName);
            }
            return recipe;
        } catch (Exception e) {
            log.error("Error activating OpenRewrite recipe: {}", recipeName, e);
            return null;
        }
    }
}
