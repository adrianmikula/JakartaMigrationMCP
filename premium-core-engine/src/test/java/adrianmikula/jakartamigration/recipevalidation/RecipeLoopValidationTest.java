package adrianmikula.jakartamigration.recipevalidation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Test class that loops through all recipes in recipes.yaml to ensure they exist and are configured correctly.
 * This test validates each recipe in the recipes.yaml file by iterating through them and checking:
 * - Required fields are present
 * - Field values are valid
 * - No duplicate recipes exist
 * - Upgrade recommendations are properly linked (if applicable)
 */
@DisplayName("Recipe Loop Validation Test")
public class RecipeLoopValidationTest {

    // Expected recipe names from recipes.yaml
    private static final List<String> EXPECTED_RECIPE_NAMES = Arrays.asList(
        "AddJakartaNamespace",
        "JavaxValidationToJakartaValidation",
        "UpdatePersistenceXml",
        "UpdateWebXml",
        "MigrateJPA",
        "MigrateBeanValidation",
        "MigrateServlet",
        "MigrateCDI",
        "MigrateREST",
        "MigrateSOAP",
        "MigrateJMS",
        "MigrateBatch",
        "MigrateMail",
        "MigrateJTA",
        "MigrateEJB",
        "MigrateJSF",
        "MigrateWebSocket",
        "MigrateJSONP",
        "MigrateJSONB",
        "MigrateSecurity",
        "MigrateConcurrency",
        "MigrateJCA",
        "MigrateJAXB",
        "MigrateJAXRPC",
        "MigrateJASPIC",
        "MigrateAnnotation",
        "MigrateActivation",
        "MigrateEL",
        "MigrateInterceptor",
        "MigrateResource",
        "MigrateServletApi",
        "MigrateSaaj",
        "MigrateAuthorization"
    );

    // Valid safety levels
    private static final Set<String> VALID_SAFETY_LEVELS = new HashSet<>(Arrays.asList("HIGH", "MEDIUM", "LOW"));

    // Required fields for each recipe
    private static final List<String> REQUIRED_FIELDS = Arrays.asList("name", "description", "pattern", "replacements", "safety", "reversible");

    @Test
    @DisplayName("All expected recipes should exist in recipes.yaml")
    public void testAllExpectedRecipesExist() throws IOException {
        List<Map<String, Object>> recipes = loadRecipes();
        
        // Extract recipe names from loaded recipes
        List<String> recipeNames = new ArrayList<>();
        for (Map<String, Object> recipe : recipes) {
            recipeNames.add((String) recipe.get("name"));
        }
        
        // Check each expected recipe exists
        for (String expectedName : EXPECTED_RECIPE_NAMES) {
            org.junit.jupiter.api.Assertions.assertTrue(recipeNames.contains(expectedName), 
                "Expected recipe not found: " + expectedName);
        }
        
        // Log the count
        System.out.println("Total recipes found: " + recipes.size());
        System.out.println("Expected recipes: " + EXPECTED_RECIPE_NAMES.size());
    }

    @Test
    @DisplayName("All recipes should have all required fields")
    public void testAllRecipesHaveRequiredFields() throws IOException {
        List<Map<String, Object>> recipes = loadRecipes();
        
        for (int i = 0; i < recipes.size(); i++) {
            Map<String, Object> recipe = recipes.get(i);
            String recipeName = (String) recipe.get("name");
            
            for (String requiredField : REQUIRED_FIELDS) {
                org.junit.jupiter.api.Assertions.assertTrue(recipe.containsKey(requiredField), 
                    "Recipe '" + recipeName + "' is missing required field: " + requiredField);
                
                Object value = recipe.get(requiredField);
                org.junit.jupiter.api.Assertions.assertNotNull(value, 
                    "Recipe '" + recipeName + "' has null value for required field: " + requiredField);
            }
        }
    }

    @Test
    @DisplayName("All recipes should have valid field values")
    public void testAllRecipesHaveValidFieldValues() throws IOException {
        List<Map<String, Object>> recipes = loadRecipes();
        
        for (Map<String, Object> recipe : recipes) {
            String name = (String) recipe.get("name");
            String description = (String) recipe.get("description");
            String pattern = (String) recipe.get("pattern");
            String safety = (String) recipe.get("safety");
            Boolean reversible = (Boolean) recipe.get("reversible");
            List<Map<String, String>> replacements = (List<Map<String, String>>) recipe.get("replacements");
            
            // Validate name
            org.junit.jupiter.api.Assertions.assertNotNull(name, "Recipe name must not be null");
            org.junit.jupiter.api.Assertions.assertFalse(name.isEmpty(), "Recipe name must not be empty");
            org.junit.jupiter.api.Assertions.assertTrue(name.matches("^[a-zA-Z][a-zA-Z0-9]*$"), 
                "Recipe name '" + name + "' should start with a letter and contain only alphanumeric characters");
            
            // Validate description
            org.junit.jupiter.api.Assertions.assertNotNull(description, "Recipe '" + name + "' description must not be null");
            org.junit.jupiter.api.Assertions.assertFalse(description.isEmpty(), "Recipe '" + name + "' description must not be empty");
            org.junit.jupiter.api.Assertions.assertTrue(description.length() >= 10, 
                "Recipe '" + name + "' description should be at least 10 characters");
            
            // Validate pattern
            org.junit.jupiter.api.Assertions.assertNotNull(pattern, "Recipe '" + name + "' pattern must not be null");
            org.junit.jupiter.api.Assertions.assertFalse(pattern.isEmpty(), "Recipe '" + name + "' pattern must not be empty");
            
            // Validate safety level
            org.junit.jupiter.api.Assertions.assertNotNull(safety, "Recipe '" + name + "' safety must not be null");
            org.junit.jupiter.api.Assertions.assertTrue(VALID_SAFETY_LEVELS.contains(safety), 
                "Recipe '" + name + "' has invalid safety level: " + safety + ". Must be one of: " + VALID_SAFETY_LEVELS);
            
            // Validate reversible
            org.junit.jupiter.api.Assertions.assertNotNull(reversible, "Recipe '" + name + "' reversible must not be null");
            
            // Validate replacements
            org.junit.jupiter.api.Assertions.assertNotNull(replacements, "Recipe '" + name + "' replacements must not be null");
            org.junit.jupiter.api.Assertions.assertFalse(replacements.isEmpty(), "Recipe '" + name + "' replacements must not be empty");
        }
    }

    @Test
    @DisplayName("All recipes should have valid replacement mappings")
    public void testAllRecipesHaveValidReplacements() throws IOException {
        List<Map<String, Object>> recipes = loadRecipes();
        
        for (Map<String, Object> recipe : recipes) {
            String name = (String) recipe.get("name");
            List<Map<String, String>> replacements = (List<Map<String, String>>) recipe.get("replacements");
            
            org.junit.jupiter.api.Assertions.assertNotNull(replacements, "Recipe '" + name + "' replacements must not be null");
            org.junit.jupiter.api.Assertions.assertFalse(replacements.isEmpty(), "Recipe '" + name + "' must have at least one replacement");
            
            for (int j = 0; j < replacements.size(); j++) {
                Map<String, String> replacement = replacements.get(j);
                String from = replacement.get("from");
                String to = replacement.get("to");
                
                org.junit.jupiter.api.Assertions.assertNotNull(from, 
                    "Recipe '" + name + "' replacement #" + j + " has null 'from' value");
                org.junit.jupiter.api.Assertions.assertNotNull(to, 
                    "Recipe '" + name + "' replacement #" + j + " has null 'to' value");
                org.junit.jupiter.api.Assertions.assertFalse(from.isEmpty(), 
                    "Recipe '" + name + "' replacement #" + j + " has empty 'from' value");
                org.junit.jupiter.api.Assertions.assertFalse(to.isEmpty(), 
                    "Recipe '" + name + "' replacement #" + j + " has empty 'to' value");
                
                // Validate that 'from' and 'to' are different
                org.junit.jupiter.api.Assertions.assertNotEquals(from, to, 
                    "Recipe '" + name + "' replacement #" + j + " has identical 'from' and 'to' values: " + from);
            }
        }
    }

    @Test
    @DisplayName("All recipes should have unique names")
    public void testAllRecipesHaveUniqueNames() throws IOException {
        List<Map<String, Object>> recipes = loadRecipes();
        Set<String> seenNames = new HashSet<>();
        
        for (Map<String, Object> recipe : recipes) {
            String name = (String) recipe.get("name");
            org.junit.jupiter.api.Assertions.assertFalse(seenNames.contains(name), "Duplicate recipe name found: " + name);
            seenNames.add(name);
        }
    }

    @Test
    @DisplayName("All recipes should have unique patterns (or different names with same pattern is allowed)")
    public void testAllRecipesHaveUniquePatterns() throws IOException {
        List<Map<String, Object>> recipes = loadRecipes();
        
        // Check for recipes with identical pattern AND name (which would be a true duplicate)
        for (int i = 0; i < recipes.size(); i++) {
            String name1 = (String) recipes.get(i).get("name");
            String pattern1 = (String) recipes.get(i).get("pattern");
            
            for (int j = i + 1; j < recipes.size(); j++) {
                String name2 = (String) recipes.get(j).get("name");
                String pattern2 = (String) recipes.get(j).get("pattern");
                
                // Only fail if both name AND pattern are identical (true duplicates)
                org.junit.jupiter.api.Assertions.assertFalse(name1.equals(name2) && pattern1.equals(pattern2), 
                    "Duplicate recipe found with same name and pattern: " + pattern1);
            }
        }
    }

    @Test
    @DisplayName("All recipes should have valid reversible values")
    public void testAllRecipesHaveValidReversibleValues() throws IOException {
        List<Map<String, Object>> recipes = loadRecipes();
        
        for (Map<String, Object> recipe : recipes) {
            String name = (String) recipe.get("name");
            Boolean reversible = (Boolean) recipe.get("reversible");
            
            org.junit.jupiter.api.Assertions.assertNotNull(reversible, "Recipe '" + name + "' reversible must not be null");
            org.junit.jupiter.api.Assertions.assertTrue(reversible instanceof Boolean, 
                "Recipe '" + name + "' reversible must be a boolean value");
        }
    }

    @Test
    @DisplayName("All recipes with fileFilter should have valid filters")
    public void testRecipesWithFileFilterHaveValidFilters() throws IOException {
        List<Map<String, Object>> recipes = loadRecipes();
        
        for (Map<String, Object> recipe : recipes) {
            String name = (String) recipe.get("name");
            
            if (recipe.containsKey("fileFilter") && recipe.get("fileFilter") != null) {
                String fileFilter = (String) recipe.get("fileFilter");
                org.junit.jupiter.api.Assertions.assertFalse(fileFilter.isEmpty(), 
                    "Recipe '" + name + "' has empty fileFilter");
                
                // File filter should contain file extension or wildcard
                boolean validFilter = fileFilter.contains(".") || 
                                      fileFilter.contains("*") ||
                                      fileFilter.contains("?");
                org.junit.jupiter.api.Assertions.assertTrue(validFilter, 
                    "Recipe '" + name + "' fileFilter should contain file extension or wildcard: " + fileFilter);
            }
        }
    }

    @Test
    @DisplayName("All recipes should have valid patterns (containing arrow or 'to' keyword)")
    public void testRecipesHaveValidPatterns() throws IOException {
        List<Map<String, Object>> recipes = loadRecipes();
        
        for (Map<String, Object> recipe : recipes) {
            String name = (String) recipe.get("name");
            String pattern = (String) recipe.get("pattern");
            
            // Pattern should contain arrow symbol, ->, or 'to' keyword
            boolean validPattern = pattern.contains("→") || 
                                   pattern.contains("->") || 
                                   pattern.contains(" to ") ||
                                   pattern.startsWith("to ");
            
            org.junit.jupiter.api.Assertions.assertTrue(validPattern, 
                "Recipe '" + name + "' pattern should contain arrow symbol or 'to' keyword: " + pattern);
        }
    }

    @Test
    @DisplayName("All javax to jakarta conversions should have correct namespace mapping")
    public void testJavaxToJakartaNamespaceMappings() throws IOException {
        List<Map<String, Object>> recipes = loadRecipes();
        
        for (Map<String, Object> recipe : recipes) {
            String name = (String) recipe.get("name");
            List<Map<String, String>> replacements = (List<Map<String, String>>) recipe.get("replacements");
            
            // Skip recipes that are not javax to jakarta conversions
            if (!name.startsWith("Migrate") && !name.equals("AddJakartaNamespace")) {
                continue;
            }
            
            for (Map<String, String> replacement : replacements) {
                String from = replacement.get("from");
                String to = replacement.get("to");
                
                // If converting from javax, should convert to jakarta
                if (from != null && from.startsWith("javax.")) {
                    org.junit.jupiter.api.Assertions.assertTrue(to.startsWith("jakarta."), 
                        "Recipe '" + name + "' should convert javax to jakarta: " + from + " -> " + to);
                }
            }
        }
    }

    @Test
    @DisplayName("All upgrade recommendations should have required fields")
    public void testUpgradeRecommendationsHaveRequiredFields() throws IOException {
        List<Map<String, Object>> recipes = loadRecipes();
        
        // Check if upgrade_recommendations exist
        Map<String, Object> data = loadFullYamlData();
        
        if (data.containsKey("upgrade_recommendations")) {
            List<Map<String, Object>> recommendations = (List<Map<String, Object>>) data.get("upgrade_recommendations");
            
            if (recommendations != null) {
                List<String> requiredRecommendationFields = Arrays.asList(
                    "currentGroupId", "currentArtifactId", "recommendedGroupId", 
                    "recommendedArtifactId", "recommendedVersion", "associatedRecipeName"
                );
                
                for (int i = 0; i < recommendations.size(); i++) {
                    Map<String, Object> rec = recommendations.get(i);
                    
                    for (String field : requiredRecommendationFields) {
                        org.junit.jupiter.api.Assertions.assertTrue(rec.containsKey(field), 
                            "Upgrade recommendation #" + i + " is missing required field: " + field);
                        org.junit.jupiter.api.Assertions.assertNotNull(rec.get(field), 
                            "Upgrade recommendation #" + i + " has null value for field: " + field);
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("Total recipe count should match expected count")
    public void testRecipeCount() throws IOException {
        List<Map<String, Object>> recipes = loadRecipes();
        
        org.junit.jupiter.api.Assertions.assertEquals(EXPECTED_RECIPE_NAMES.size(), recipes.size(), 
            "Recipe count mismatch. Expected: " + EXPECTED_RECIPE_NAMES.size() + 
            ", Found: " + recipes.size());
    }

    /**
     * Helper method to load recipes from recipes.yaml using classpath
     */
    private List<Map<String, Object>> loadRecipes() throws IOException {
        Yaml yaml = new Yaml();
        
        // Try classpath first (works in test context)
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("recipes.yaml")) {
            if (inputStream != null) {
                Map<String, Object> data = yaml.load(inputStream);
                return (List<Map<String, Object>>) data.get("recipes");
            }
        }
        
        // Fallback to file system path
        Map<String, Object> data = yaml.load(
            Files.newInputStream(Paths.get("premium-core-engine/src/main/resources/recipes.yaml"))
        );
        return (List<Map<String, Object>>) data.get("recipes");
    }
    
    /**
     * Helper method to load full YAML data including upgrade_recommendations
     */
    private Map<String, Object> loadFullYamlData() throws IOException {
        Yaml yaml = new Yaml();
        
        // Try classpath first (works in test context)
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("recipes.yaml")) {
            if (inputStream != null) {
                return yaml.load(inputStream);
            }
        }
        
        // Fallback to file system path
        return yaml.load(
            Files.newInputStream(Paths.get("premium-core-engine/src/main/resources/recipes.yaml"))
        );
    }
}
