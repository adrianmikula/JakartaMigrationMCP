package adrianmikula.jakartamigration.coderefactoring.util;

import adrianmikula.jakartamigration.coderefactoring.domain.Recipe;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Maps domain-specific recipes to OpenRewrite recipes.
 *
 * <p>
 * Recipes are instantiated directly by class name rather than through
 * {@code Environment.builder().scanRuntimeClasspath()}, which causes a
 * {@link ClassCastException} in IntelliJ plugin environments due to classloader
 * isolation between the IDE's parent classloader and the plugin's classloader.
 */
@Slf4j
public class RecipeMapper {

    /**
     * Maps a list of domain recipes to a single composite OpenRewrite recipe.
     *
     * @param domainRecipes List of domain recipes
     * @return A composite OpenRewrite recipe, or {@code null} if none could be
     *         mapped
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

        // Return a composite recipe that delegates to all mapped recipes
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

        switch (normalizedName) {
            case "addjakartanamespace":
                return instantiateRecipe("org.openrewrite.java.migrate.jakarta.JavaxMigrationToJakarta");
            case "migratejpa":
                return instantiateRecipe("org.openrewrite.java.migrate.jakarta.JavaxPersistenceToJakartaPersistence");
            case "migratejaxrs":
            case "migraterest":
                return instantiateRecipe("org.openrewrite.java.migrate.jakarta.JavaxMigrationToJakarta");
            case "migrateejb":
                return instantiateRecipe("org.openrewrite.java.migrate.jakarta.JavaxEjbToJakartaEjb");
            case "migratejms":
                return instantiateRecipe("org.openrewrite.java.migrate.jakarta.JavaxJmsToJakartaJms");
            case "migratejta":
                return instantiateRecipe(
                        "org.openrewrite.java.migrate.jakarta.JavaxTransactionMigrationToJakartaTransaction");
            case "migratevalidator":
            case "migratebeanvalidation":
                return instantiateRecipe(
                        "org.openrewrite.java.migrate.jakarta.JavaxValidationMigrationToJakartaValidation");
            case "migratejaxb":
                return instantiateRecipe("org.openrewrite.java.migrate.jakarta.JavaxXmlBindMigrationToJakartaXmlBind");
            case "migrateservlet":
            case "migrateservletapi":
                return instantiateRecipe("org.openrewrite.java.migrate.jakarta.JavaxServletToJakartaServlet");
            case "migratecdi":
                return instantiateRecipe("org.openrewrite.java.migrate.jakarta.JavaxInjectMigrationToJakartaInject");
            case "migratejavamail":
            case "migratemail":
                return instantiateRecipe("org.openrewrite.java.migrate.jakarta.JavaxMailToJakartaMail");
            case "migratebatch":
                return instantiateRecipe("org.openrewrite.java.migrate.jakarta.JavaxBatchMigrationToJakartaBatch");
            case "migratejsf":
                return instantiateRecipe("org.openrewrite.java.migrate.jakarta.JavaxFacesToJakartaFaces");
            case "migratewebsocket":
                return instantiateRecipe("org.openrewrite.java.migrate.jakarta.JavaxWebsocketToJakartaWebsocket");
            case "migratejsonp":
                return instantiateRecipe("org.openrewrite.java.migrate.jakarta.JavaxJsonToJakartaJson");
            case "migratejsonb":
                return instantiateRecipe("org.openrewrite.java.migrate.jakarta.JavaxMigrationToJakarta");
            case "migrateactivation":
                return instantiateRecipe(
                        "org.openrewrite.java.migrate.jakarta.JavaxActivationMigrationToJakartaActivation");
            case "migrateel":
                return instantiateRecipe("org.openrewrite.java.migrate.jakarta.JavaxElToJakartaEl");
            case "migratejaxws":
            case "migratesoap":
                return instantiateRecipe("org.openrewrite.java.migrate.jakarta.JavaxXmlWsMigrationToJakartaXmlWs");
            case "migratesecurity":
                return instantiateRecipe("org.openrewrite.java.migrate.jakarta.JavaxSecurityToJakartaSecurity");
            case "migrateconcurrency":
                return instantiateRecipe("org.openrewrite.java.migrate.jakarta.JavaxMigrationToJakarta");
            case "migratejca":
                return instantiateRecipe("org.openrewrite.java.migrate.jakarta.JavaxResourceToJakartaResource");
            case "migratejaspic":
                return instantiateRecipe("org.openrewrite.java.migrate.jakarta.JavaxMigrationToJakarta");
            case "migratejaxrpc":
                return instantiateRecipe("org.openrewrite.java.migrate.jakarta.JavaxMigrationToJakarta");
            case "migrateinterceptor":
                return instantiateRecipe("org.openrewrite.java.migrate.jakarta.JavaxInterceptorToJakartaInterceptor");
            case "migrateresource":
                return instantiateRecipe(
                        "org.openrewrite.java.migrate.jakarta.JavaxAnnotationMigrationToJakartaAnnotation");
            case "migratesaaj":
                return instantiateRecipe("org.openrewrite.java.migrate.jakarta.JavaxXmlSoapToJakartaXmlSoap");
            case "migrateauthorization":
                return instantiateRecipe("org.openrewrite.java.migrate.jakarta.JavaxSecurityToJakartaSecurity");
            case "migrateannotation":
                return instantiateRecipe(
                        "org.openrewrite.java.migrate.jakarta.JavaxAnnotationMigrationToJakartaAnnotation");
            default:
                log.warn(
                        "No specific OpenRewrite mapping found for recipe: '{}'. Using generic JavaxMigrationToJakarta.",
                        recipeName);
                if (recipeName.startsWith("Migrate")) {
                    return instantiateRecipe("org.openrewrite.java.migrate.jakarta.JavaxMigrationToJakarta");
                }
                return null;
        }
    }

    /**
     * Instantiates an OpenRewrite recipe class by its fully-qualified class name.
     *
     * <p>
     * This method uses the current thread's context classloader (the plugin
     * classloader
     * inside IntelliJ) to resolve the class, avoiding the classloader mismatch that
     * would
     * occur with {@code Environment.builder().scanRuntimeClasspath()}.
     *
     * @param className Fully qualified name of the OpenRewrite recipe class
     * @return The instantiated recipe, or {@code null} if instantiation fails
     */
    private org.openrewrite.Recipe instantiateRecipe(String className) {
        try {
            // Use thread context classloader to respect IntelliJ's plugin classloader
            // isolation.
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl == null) {
                cl = RecipeMapper.class.getClassLoader();
            }
            Class<?> clazz = Class.forName(className, true, cl);
            Object instance = clazz.getDeclaredConstructor().newInstance();
            if (instance instanceof org.openrewrite.Recipe) {
                log.debug("Instantiated OpenRewrite recipe: {}", className);
                return (org.openrewrite.Recipe) instance;
            } else {
                log.error("Class '{}' is not an OpenRewrite Recipe", className);
                return null;
            }
        } catch (ClassNotFoundException e) {
            log.warn(
                    "OpenRewrite recipe class not found on classpath: {}. Check that rewrite-migrate-java is a dependency.",
                    className);
            return null;
        } catch (Exception e) {
            log.error("Failed to instantiate OpenRewrite recipe '{}': {}", className, e.getMessage(), e);
            return null;
        }
    }
}
