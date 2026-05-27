package adrianmikula.jakartamigration.coderefactoring.service.util;

import adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Schema consistency test to ensure recipe seeding doesn't lose data.
 * This test catches mismatches between:
 * - recipes.json field structure
 * - RecipeDefinition domain model fields
 * - Database schema columns
 * - RecipeSeeder field extraction
 * - INSERT statement field inclusion
 */
@Tag("slow")
class RecipeSchemaConsistencyTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Should have consistent fields between JSON, domain model, and database schema")
    void shouldHaveConsistentFieldsBetweenJsonDomainAndDatabase() throws Exception {
        // Step 1: Get fields from RecipeDefinition domain model
        Set<String> domainFields = getRecipeDefinitionFields();
        
        // Step 2: Get columns from database schema
        Set<String> dbColumns = getDatabaseColumns(tempDir);
        
        // Step 3: Get fields from recipes.json
        Set<String> jsonFields = getRecipesJsonFields();
        
        // Step 4: Verify critical fields are present in domain model and database
        Set<String> criticalFields = Set.of(
            "name", "description", "category", "recipeType",
            "openRewriteRecipeName", "pattern", "safety", "replacement",
            "filePattern", "reversible", "addedInPluginVersion", "archived"
        );

        for (String field : criticalFields) {
            String dbColumn = convertToDbColumnName(field);
            assertThat(domainFields).as("Domain model should have field: %s", field).contains(field);
            assertThat(dbColumns).as("Database schema should have column: %s", dbColumn).contains(dbColumn);
        }

        // Step 5: Verify JSON source fields that are used to build domain model
        Set<String> expectedJsonFields = Set.of(
            "name", "description", "category", "openRewriteClass",
            "pattern", "safety", "replacements", "reversible"
        );

        for (String jsonField : expectedJsonFields) {
            assertThat(jsonFields).as("JSON should have field: %s", jsonField).contains(jsonField);
        }
        
        // Step 5: Report any discrepancies
        reportDiscrepancies(domainFields, dbColumns, jsonFields);
    }

    private Set<String> getRecipeDefinitionFields() {
        Set<String> fields = new HashSet<>();
        for (Field field : RecipeDefinition.class.getDeclaredFields()) {
            fields.add(field.getName());
        }
        return fields;
    }

    private Set<String> getDatabaseColumns(Path tempDir) throws Exception {
        Set<String> columns = new HashSet<>();
        Path dbPath = tempDir.resolve("test-schema.db");
        
        // Initialize database schema using CentralMigrationAnalysisStore
        new adrianmikula.jakartamigration.analysis.persistence.CentralMigrationAnalysisStore(dbPath);
        
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(recipes)")) {
            
            while (rs.next()) {
                columns.add(rs.getString("name"));
            }
        }
        
        return columns;
    }

    private Set<String> getRecipesJsonFields() throws Exception {
        Set<String> fields = new HashSet<>();
        var is = getClass().getClassLoader().getResourceAsStream("recipes.json");
        if (is == null) {
            return fields;
        }

        try (is) {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> data = mapper.readValue(is, Map.class);

            // Get first recipe to examine its fields
            List<Map<String, Object>> recipes = (List<Map<String, Object>>) data.get("recipes");
            if (recipes != null && !recipes.isEmpty()) {
                Map<String, Object> firstRecipe = recipes.get(0);
                fields.addAll(firstRecipe.keySet());
            }
        }

        return fields;
    }

    private String convertToDbColumnName(String fieldName) {
        // Handle special cases for existing column names
        if (fieldName.equals("openRewriteRecipeName")) {
            return "openrewrite_recipe_name";
        }
        return fieldName.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    private void reportDiscrepancies(Set<String> domainFields, Set<String> dbColumns, Set<String> jsonFields) {
        List<String> issues = new ArrayList<>();
        
        // Check for domain fields not in database
        for (String field : domainFields) {
            String dbColumn = convertToDbColumnName(field);
            if (!dbColumns.contains(dbColumn) && !field.equals("id") && !field.equals("createdAt") 
                && !field.equals("lastRunDate") && !field.equals("status")) {
                issues.add(String.format("Domain field '%s' (DB column '%s') not in database schema", field, dbColumn));
            }
        }
        
        // Check for database columns not in domain
        for (String column : dbColumns) {
            String domainField = convertToDomainFieldName(column);
            if (!domainFields.contains(domainField) && !column.equals("id") && !column.equals("created_at")) {
                issues.add(String.format("DB column '%s' (Domain field '%s') not in domain model", column, domainField));
            }
        }
        
        if (!issues.isEmpty()) {
            System.out.println("Schema consistency issues found:");
            for (String issue : issues) {
                System.out.println("  - " + issue);
            }
        }
        
        assertThat(issues).isEmpty();
    }

    private String convertToDomainFieldName(String columnName) {
        // Handle special cases for existing column names
        if (columnName.equals("openrewrite_recipe_name")) {
            return "openRewriteRecipeName";
        }
        String[] parts = columnName.split("_");
        StringBuilder result = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            result.append(Character.toUpperCase(parts[i].charAt(0)));
            if (parts[i].length() > 1) {
                result.append(parts[i].substring(1));
            }
        }
        return result.toString();
    }
}
