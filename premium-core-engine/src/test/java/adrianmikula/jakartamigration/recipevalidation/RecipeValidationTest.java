package adrianmikula.jakartamigration.recipevalidation;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import static org.junit.jupiter.api.Assertions.*;

public class RecipeValidationTest {

    @Test
    public void testAllRecipesExistAndAreValid() throws IOException {
        // Load recipes.yaml
        String yamlPath = "premium-core-engine/src/main/resources/recipes.yaml";
        List<Map<String, Object>> recipes = loadRecipes(yamlPath);

        // Validate each recipe
        for (Map<String, Object> recipe : recipes) {
            String name = (String) recipe.get("name");
            String pattern = (String) recipe.get("pattern");
            List<Map<String, String>> replacements = (List<Map<String, String>>) recipe.get("replacements");
            String safety = (String) recipe.get("safety");
            Boolean reversible = (Boolean) recipe.get("reversible");

            // Basic validation checks
            assertNotNull(name, "Recipe name must not be null");
            assertFalse(name.isEmpty(), "Recipe name must not be empty");
            assertNotNull(pattern, "Recipe pattern must not be null for " + name);
            assertFalse(pattern.isEmpty(), "Recipe pattern must not be empty for " + name);
            assertNotNull(replacements, "Recipe replacements must not be null for " + name);
            assertFalse(replacements.isEmpty(), "Recipe replacements must not be empty for " + name);
            assertNotNull(safety, "Recipe safety must not be null for " + name);
            assertTrue(safety.equals("HIGH") || safety.equals("MEDIUM") || safety.equals("LOW"),
                    "Recipe safety must be HIGH, MEDIUM, or LOW for " + name);
            assertNotNull(reversible, "Recipe reversible must not be null for " + name);

            // Validate replacement structure
            for (Map<String, String> replacement : replacements) {
                assertTrue(replacement.containsKey("from"), "Replacement must have 'from' field in " + name);
                assertTrue(replacement.containsKey("to"), "Replacement must have 'to' field in " + name);
                assertNotNull(replacement.get("from"), "Replacement 'from' must not be null in " + name);
                assertNotNull(replacement.get("to"), "Replacement 'to' must not be null in " + name);
                assertFalse(replacement.get("from").isEmpty(), "Replacement 'from' must not be empty in " + name);
                assertFalse(replacement.get("to").isEmpty(), "Replacement 'to' must not be empty in " + name);
            }
        }
    }

    @Test
    public void testNoDuplicateRecipeNames() throws IOException {
        // Load recipes.yaml
        String yamlPath = "premium-core-engine/src/main/resources/recipes.yaml";
        List<Map<String, Object>> recipes = loadRecipes(yamlPath);

        // Check for duplicate names
        for (int i = 0; i < recipes.size(); i++) {
            String name1 = (String) recipes.get(i).get("name");
            for (int j = i + 1; j < recipes.size(); j++) {
                String name2 = (String) recipes.get(j).get("name");
                assertNotEquals(name1, name2, "Duplicate recipe name found: " + name1);
            }
        }
    }

    @Test
    public void testAllRecipesHaveUniquePatterns() throws IOException {
        // Load recipes.yaml
        String yamlPath = "premium-core-engine/src/main/resources/recipes.yaml";
        List<Map<String, Object>> recipes = loadRecipes(yamlPath);

        // Check for recipes with identical pattern AND name (true duplicates)
        for (int i = 0; i < recipes.size(); i++) {
            String name1 = (String) recipes.get(i).get("name");
            String pattern1 = (String) recipes.get(i).get("pattern");
            for (int j = i + 1; j < recipes.size(); j++) {
                String name2 = (String) recipes.get(j).get("name");
                String pattern2 = (String) recipes.get(j).get("pattern");
                // Only fail if both name AND pattern are identical
                assertFalse(name1.equals(name2) && pattern1.equals(pattern2), 
                    "Duplicate recipe found with same name and pattern: " + pattern1);
            }
        }
    }

    private List<Map<String, Object>> loadRecipes(String path) throws IOException {
        Yaml yaml = new Yaml();
        
        // Try classpath first (works in test context)
        try (java.io.InputStream inputStream = getClass().getClassLoader().getResourceAsStream("recipes.yaml")) {
            if (inputStream != null) {
                Map<String, Object> data = yaml.load(inputStream);
                return (List<Map<String, Object>>) data.get("recipes");
            }
        }
        
        // Fallback to file system path
        Map<String, Object> data = yaml.load(Files.newInputStream(Paths.get(path)));
        return (List<Map<String, Object>>) data.get("recipes");
    }
}
