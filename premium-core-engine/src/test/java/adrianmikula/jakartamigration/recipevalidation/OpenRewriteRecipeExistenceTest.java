package adrianmikula.jakartamigration.recipevalidation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.config.Environment;
import com.fasterxml.jackson.databind.ObjectMapper;

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
 * Test to verify that all recipes in recipes.json that use OpenRewrite type
 * have valid OpenRewrite recipe class names that actually exist in OpenRewrite.
 */
@Tag("slow")
@DisplayName("OpenRewrite Recipe Existence Test")
public class OpenRewriteRecipeExistenceTest {

    @Test
    @DisplayName("All OpenRewrite recipes in recipes.json should exist in OpenRewrite's available recipes")
    public void testAllOpenRewriteRecipesExist() throws Exception {
        // Load recipes from JSON
        List<Map<String, Object>> jsonRecipes = loadRecipesFromJson();
        
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
        
        // Extract all openRewriteClass values from recipes.json
        List<String> recipeClasses = new ArrayList<>();
        for (Map<String, Object> recipe : jsonRecipes) {
            String openRewriteClass = (String) recipe.get("openRewriteClass");
            if (openRewriteClass != null && !openRewriteClass.isEmpty()) {
                recipeClasses.add(openRewriteClass);
            }
        }
        
        // Track recipes that don't exist in OpenRewrite
        List<String> missingRecipes = new ArrayList<>();
        List<String> existingRecipes = new ArrayList<>();
        
        for (String orRecipeName : recipeClasses) {
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
        System.out.println("  - Total recipes in recipes.json: " + recipeClasses.size());
        System.out.println("  - Recipes found in OpenRewrite: " + existingRecipes.size());
        System.out.println("  - Recipes NOT found in OpenRewrite: " + missingRecipes.size());
        
        System.out.println("=== End OpenRewrite Recipe Existence Check ===");
        
        // Fail the test if any recipes are missing
        assertTrue(missingRecipes.isEmpty(), 
            "Found " + missingRecipes.size() + " invalid OpenRewrite recipes in recipes.json: " + missingRecipes);
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
    
    
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> loadRecipesFromJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("recipes.json")) {
            if (inputStream != null) {
                Map<String, Object> data = mapper.readValue(inputStream, Map.class);
                return (List<Map<String, Object>>) data.get("recipes");
            }
        }
        // Fallback
        Map<String, Object> data = mapper.readValue(
            Files.newInputStream(Paths.get("premium-core-engine/src/main/resources/recipes.json")),
            Map.class
        );
        return (List<Map<String, Object>>) data.get("recipes");
    }
}
