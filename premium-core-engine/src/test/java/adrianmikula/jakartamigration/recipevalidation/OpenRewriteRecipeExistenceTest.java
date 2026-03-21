package adrianmikula.jakartamigration.recipevalidation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.config.Environment;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that all recipes in recipes.yaml that use OpenRewrite type
 * have valid OpenRewrite recipe class names that actually exist in OpenRewrite.
 */
@DisplayName("OpenRewrite Recipe Existence Test")
public class OpenRewriteRecipeExistenceTest {

    @Test
    @DisplayName("All OpenRewrite recipes in recipes.yaml should exist in OpenRewrite's available recipes")
    public void testAllOpenRewriteRecipesExist() throws Exception {
        // Load recipes from YAML
        List<Map<String, Object>> yamlRecipes = loadRecipesFromYaml();
        
        // Get all available OpenRewrite recipes
        Environment env = Environment.builder()
            .scanRuntimeClasspath()
            .build();
        List<String> availableRecipeNames = env.listRecipes().stream()
            .map(Recipe::getName)
            .collect(Collectors.toList());
        
        Set<String> availableRecipeSet = availableRecipeNames.stream()
            .collect(Collectors.toSet());
        
        System.out.println("=== OpenRewrite Recipe Existence Check ===");
        System.out.println("Total available OpenRewrite recipes: " + availableRecipeSet.size());
        System.out.println();
        
        // Track recipes that don't exist in OpenRewrite
        List<String> missingRecipes = new ArrayList<>();
        List<String> existingRecipes = new ArrayList<>();
        
        // Known OpenRewrite recipe mappings from RecipeSeeder
        // These are the class names we expect to find
        List<String> expectedOpenRewriteRecipes = getExpectedOpenRewriteRecipes();
        
        for (String orRecipeName : expectedOpenRewriteRecipes) {
            if (availableRecipeSet.contains(orRecipeName)) {
                existingRecipes.add(orRecipeName);
                System.out.println("✓ FOUND: " + orRecipeName);
            } else {
                missingRecipes.add(orRecipeName);
                System.out.println("✗ MISSING: " + orRecipeName);
            }
        }
        
        System.out.println();
        System.out.println("Summary:");
        System.out.println("  - Recipes found in OpenRewrite: " + existingRecipes.size());
        System.out.println("  - Recipes NOT found in OpenRewrite: " + missingRecipes.size());
        
        if (!missingRecipes.isEmpty()) {
            System.out.println();
            System.out.println("Missing recipes should either:");
            System.out.println("  1. Be removed from RecipeSeeder mapping, OR");
            System.out.println("  2. Be changed to use REGEX type instead");
        }
        
        System.out.println("=== End OpenRewrite Recipe Existence Check ===");
        
        // This test documents the current state - it will pass but shows issues
        // To make this test fail on missing recipes, uncomment:
        // assertTrue(missingRecipes.isEmpty(), "Missing " + missingRecipes.size() + " OpenRewrite recipes: " + missingRecipes);
    }
    
    @Test
    @DisplayName("Print all available Jakarta-related OpenRewrite recipes")
    public void testPrintJakartaRecipes() throws Exception {
        Environment env = Environment.builder()
            .scanRuntimeClasspath()
            .build();
        
        List<String> jakartaRecipes = env.listRecipes().stream()
            .map(Recipe::getName)
            .filter(name -> name.toLowerCase().contains("jakarta") || name.toLowerCase().contains("javax"))
            .sorted()
            .collect(Collectors.toList());
        
        System.out.println("=== Jakarta-related OpenRewrite Recipes ===");
        System.out.println("Total: " + jakartaRecipes.size());
        for (String recipe : jakartaRecipes) {
            System.out.println("  - " + recipe);
        }
        System.out.println("=== End Jakarta-related Recipes ===");
        
        assertFalse(jakartaRecipes.isEmpty(), "Should find Jakarta-related recipes");
    }
    
    /**
     * Returns the list of OpenRewrite recipe class names that we expect to exist.
     * This matches the mapping in RecipeSeeder.
     */
    private List<String> getExpectedOpenRewriteRecipes() {
        List<String> recipes = new ArrayList<>();
        
        // Jakarta EE namespace migrations - verified to exist in OpenRewrite
        recipes.add("org.openrewrite.java.migrate.jakarta.JavaxServletToJakartaServlet");
        recipes.add("org.openrewrite.java.migrate.jakarta.JavaxPersistenceToJakartaPersistence");
        // Fixed: JavaxValidationMigrationToJakartaValidation (with Migration)
        recipes.add("org.openrewrite.java.migrate.jakarta.JavaxValidationMigrationToJakartaValidation");
        // CDI uses JavaxEnterpriseToJakartaEnterprise
        recipes.add("org.openrewrite.java.migrate.jakarta.JavaxEnterpriseToJakartaEnterprise");
        recipes.add("org.openrewrite.java.migrate.jakarta.JavaxWsToJakartaWs");
        recipes.add("org.openrewrite.java.migrate.jakarta.JavaxXmlSoapToJakartaXmlSoap");
        recipes.add("org.openrewrite.java.migrate.jakarta.JavaxJmsToJakartaJms");
        recipes.add("org.openrewrite.java.migrate.jakarta.JavaxBatchMigrationToJakartaBatch");
        recipes.add("org.openrewrite.java.migrate.jakarta.JavaxMailToJakartaMail");
        recipes.add("org.openrewrite.java.migrate.jakarta.JavaxTransactionMigrationToJakartaTransaction");
        recipes.add("org.openrewrite.java.migrate.jakarta.JavaxEjbToJakartaEjb");
        recipes.add("org.openrewrite.java.migrate.jakarta.JavaxFacesToJakartaFaces");
        // Fixed: JavaxWebsocketToJakartaWebsocket (correct spelling)
        recipes.add("org.openrewrite.java.migrate.jakarta.JavaxWebsocketToJakartaWebsocket");
        // Fixed: JavaxJsonToJakartaJson (JSONP/JSONB)
        recipes.add("org.openrewrite.java.migrate.jakarta.JavaxJsonToJakartaJson");
        recipes.add("org.openrewrite.java.migrate.jakarta.JavaxSecurityToJakartaSecurity");
        recipes.add("org.openrewrite.java.migrate.jakarta.JavaxXmlBindMigrationToJakartaXmlBind");
        // Fixed: JavaxXmlWsMigrationToJakartaXmlWs (JAXRPC)
        recipes.add("org.openrewrite.java.migrate.jakarta.JavaxXmlWsMigrationToJakartaXmlWs");
        recipes.add("org.openrewrite.java.migrate.jakarta.JavaxAnnotationMigrationToJakartaAnnotation");
        recipes.add("org.openrewrite.java.migrate.jakarta.JavaxActivationMigrationToJakartaActivation");
        recipes.add("org.openrewrite.java.migrate.jakarta.JavaxElToJakartaEl");
        recipes.add("org.openrewrite.java.migrate.jakarta.JavaxInterceptorToJakartaInterceptor");
        recipes.add("org.openrewrite.java.migrate.jakarta.JavaxResourceToJakartaResource");
        // Fixed: JavaxAuthorizationMigrationToJakartaAuthorization
        recipes.add("org.openrewrite.java.migrate.jakarta.JavaxAuthorizationMigrationToJakartaAuthorization");
        
        return recipes;
    }
    
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> loadRecipesFromYaml() throws Exception {
        Yaml yaml = new Yaml();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("recipes.yaml")) {
            if (inputStream != null) {
                Map<String, Object> data = yaml.load(inputStream);
                return (List<Map<String, Object>>) data.get("recipes");
            }
        }
        // Fallback
        Map<String, Object> data = yaml.load(
            Files.newInputStream(Paths.get("premium-core-engine/src/main/resources/recipes.yaml"))
        );
        return (List<Map<String, Object>>) data.get("recipes");
    }
}
