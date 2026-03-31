package adrianmikula.jakartamigration.mcp;

import adrianmikula.jakartamigration.coderefactoring.domain.RecipeCategory;
import adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition;
import adrianmikula.jakartamigration.coderefactoring.domain.RecipeExecutionHistory;
import adrianmikula.jakartamigration.coderefactoring.domain.RecipeExecutionResult;
import adrianmikula.jakartamigration.coderefactoring.service.RecipeService;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Premium MCP Tools for Jakarta Migration Refactor Recipes and History Actions.
 * 
 * These tools provide access to refactor recipe execution and history management
 * through the MCP (Model Context Protocol) interface.
 * 
 * PREMIUM TOOLS - Requires JetBrains Marketplace subscription
 */
@Component
@Slf4j
public class PremiumMigrationTools {

    private final RecipeService recipeService;

    public PremiumMigrationTools(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    /**
     * Lists all available refactor recipes for a project.
     * PREMIUM TOOL - Requires JetBrains Marketplace subscription
     */
    @McpTool(name = "listRefactorRecipes", description = "Lists all available refactor recipes for a project with their current status. Returns JSON with recipe names, descriptions, categories, and applicability status. Requires PREMIUM license.")
    public String listRefactorRecipes(
            @McpToolParam(description = "Path to the project root directory", required = true) String projectPath) {
        
        try {
            log.info("Listing refactor recipes for project: {}", projectPath);

            Path project = Paths.get(projectPath);
            if (!Files.exists(project) || !Files.isDirectory(project)) {
                return createErrorResponse("Project path does not exist or is not a directory: " + projectPath);
            }

            List<RecipeDefinition> recipes = recipeService.getRecipes(project);

            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"status\": \"success\",\n");
            json.append("  \"edition\": \"premium\",\n");
            json.append("  \"projectPath\": \"").append(escapeJson(projectPath)).append("\",\n");
            json.append("  \"totalRecipes\": ").append(recipes.size()).append(",\n");
            json.append("  \"recipes\": [\n");

            for (int i = 0; i < recipes.size(); i++) {
                RecipeDefinition recipe = recipes.get(i);
                json.append("    {\n");
                json.append("      \"name\": \"").append(escapeJson(recipe.name())).append("\",\n");
                json.append("      \"displayName\": \"").append(escapeJson(recipe.displayName())).append("\",\n");
                json.append("      \"description\": \"").append(escapeJson(recipe.description())).append("\",\n");
                json.append("      \"category\": \"").append(recipe.category()).append("\",\n");
                json.append("      \"applicable\": ").append(recipe.isApplicable()).append(",\n");
                json.append("      \"estimatedTime\": \"").append(escapeJson(recipe.estimatedTime())).append("\",\n");
                json.append("      \"riskLevel\": \"").append(recipe.riskLevel()).append("\"\n");
                json.append("    }");
                if (i < recipes.size() - 1) {
                    json.append(",");
                }
                json.append("\n");
            }

            json.append("  ]\n");
            json.append("}");

            return json.toString();

        } catch (Exception e) {
            log.error("Unexpected error listing refactor recipes", e);
            return createErrorResponse("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Lists refactor recipes by category.
     * PREMIUM TOOL - Requires JetBrains Marketplace subscription
     */
    @McpTool(name = "listRefactorRecipesByCategory", description = "Lists refactor recipes by category for a project. Returns JSON with recipes filtered by the specified category. Requires PREMIUM license.")
    public String listRefactorRecipesByCategory(
            @McpToolParam(description = "Path to the project root directory", required = true) String projectPath,
            @McpToolParam(description = "Recipe category to filter by", required = true) String category) {
        
        try {
            log.info("Listing refactor recipes by category '{}' for project: {}", category, projectPath);

            Path project = Paths.get(projectPath);
            if (!Files.exists(project) || !Files.isDirectory(project)) {
                return createErrorResponse("Project path does not exist or is not a directory: " + projectPath);
            }

            RecipeCategory recipeCategory;
            try {
                recipeCategory = RecipeCategory.valueOf(category.toUpperCase());
            } catch (IllegalArgumentException e) {
                return createErrorResponse("Invalid category: " + category + ". Valid categories: " + 
                    java.util.Arrays.toString(RecipeCategory.values()));
            }

            List<RecipeDefinition> recipes = recipeService.getRecipesByCategory(recipeCategory, project);

            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"status\": \"success\",\n");
            json.append("  \"edition\": \"premium\",\n");
            json.append("  \"projectPath\": \"").append(escapeJson(projectPath)).append("\",\n");
            json.append("  \"category\": \"").append(recipeCategory).append("\",\n");
            json.append("  \"totalRecipes\": ").append(recipes.size()).append(",\n");
            json.append("  \"recipes\": [\n");

            for (int i = 0; i < recipes.size(); i++) {
                RecipeDefinition recipe = recipes.get(i);
                json.append("    {\n");
                json.append("      \"name\": \"").append(escapeJson(recipe.name())).append("\",\n");
                json.append("      \"displayName\": \"").append(escapeJson(recipe.displayName())).append("\",\n");
                json.append("      \"description\": \"").append(escapeJson(recipe.description())).append("\",\n");
                json.append("      \"applicable\": ").append(recipe.isApplicable()).append(",\n");
                json.append("      \"estimatedTime\": \"").append(escapeJson(recipe.estimatedTime())).append("\",\n");
                json.append("      \"riskLevel\": \"").append(recipe.riskLevel()).append("\"\n");
                json.append("    }");
                if (i < recipes.size() - 1) {
                    json.append(",");
                }
                json.append("\n");
            }

            json.append("  ]\n");
            json.append("}");

            return json.toString();

        } catch (Exception e) {
            log.error("Unexpected error listing refactor recipes by category", e);
            return createErrorResponse("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Applies a refactor recipe to the project.
     * PREMIUM TOOL - Requires JetBrains Marketplace subscription
     */
    @McpTool(name = "applyRefactorRecipe", description = "Applies a refactor recipe to the project. Returns JSON with execution result, changes made, and success status. Requires PREMIUM license.")
    public String applyRefactorRecipe(
            @McpToolParam(description = "Path to the project root directory", required = true) String projectPath,
            @McpToolParam(description = "Name of the recipe to apply", required = true) String recipeName) {
        
        try {
            log.info("Applying refactor recipe '{}' to project: {}", recipeName, projectPath);

            Path project = Paths.get(projectPath);
            if (!Files.exists(project) || !Files.isDirectory(project)) {
                return createErrorResponse("Project path does not exist or is not a directory: " + projectPath);
            }

            RecipeExecutionResult result = recipeService.applyRecipe(recipeName, project);

            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"status\": \"success\",\n");
            json.append("  \"edition\": \"premium\",\n");
            json.append("  \"projectPath\": \"").append(escapeJson(projectPath)).append("\",\n");
            json.append("  \"recipeName\": \"").append(escapeJson(recipeName)).append("\",\n");
            json.append("  \"executionId\": ").append(result.executionId()).append(",\n");
            json.append("  \"success\": ").append(result.success()).append(",\n");
            json.append("  \"message\": \"").append(escapeJson(result.message())).append("\",\n");
            json.append("  \"filesModified\": ").append(result.filesModified()).append(",\n");
            json.append("  \"changesMade\": ").append(result.changesMade()).append(",\n");
            json.append("  \"executionTimeMs\": ").append(result.executionTimeMs()).append(",\n");
            json.append("  \"canUndo\": ").append(result.canUndo()).append("\n");
            json.append("}");

            return json.toString();

        } catch (Exception e) {
            log.error("Unexpected error applying refactor recipe", e);
            return createErrorResponse("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Undoes a previous recipe execution.
     * PREMIUM TOOL - Requires JetBrains Marketplace subscription
     */
    @McpTool(name = "undoRefactorRecipe", description = "Undoes a previous recipe execution by execution ID. Returns JSON with undo result and success status. Requires PREMIUM license.")
    public String undoRefactorRecipe(
            @McpToolParam(description = "Path to the project root directory", required = true) String projectPath,
            @McpToolParam(description = "Execution ID of the recipe to undo", required = true) Long executionId) {
        
        try {
            log.info("Undoing refactor recipe execution {} for project: {}", executionId, projectPath);

            Path project = Paths.get(projectPath);
            if (!Files.exists(project) || !Files.isDirectory(project)) {
                return createErrorResponse("Project path does not exist or is not a directory: " + projectPath);
            }

            RecipeExecutionResult result = recipeService.undoRecipe(executionId, project);

            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"status\": \"success\",\n");
            json.append("  \"edition\": \"premium\",\n");
            json.append("  \"projectPath\": \"").append(escapeJson(projectPath)).append("\",\n");
            json.append("  \"executionId\": ").append(executionId).append(",\n");
            json.append("  \"success\": ").append(result.success()).append(",\n");
            json.append("  \"message\": \"").append(escapeJson(result.message())).append("\",\n");
            json.append("  \"filesModified\": ").append(result.filesModified()).append(",\n");
            json.append("  \"changesReverted\": ").append(result.changesMade()).append(",\n");
            json.append("  \"executionTimeMs\": ").append(result.executionTimeMs()).append("\n");
            json.append("}");

            return json.toString();

        } catch (Exception e) {
            log.error("Unexpected error undoing refactor recipe", e);
            return createErrorResponse("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Gets the execution history for refactor recipes.
     * PREMIUM TOOL - Requires JetBrains Marketplace subscription
     */
    @McpTool(name = "getRefactorHistory", description = "Gets the execution history for refactor recipes in a project. Returns JSON with past executions, their status, and results. Requires PREMIUM license.")
    public String getRefactorHistory(
            @McpToolParam(description = "Path to the project root directory", required = true) String projectPath) {
        
        try {
            log.info("Getting refactor history for project: {}", projectPath);

            Path project = Paths.get(projectPath);
            if (!Files.exists(project) || !Files.isDirectory(project)) {
                return createErrorResponse("Project path does not exist or is not a directory: " + projectPath);
            }

            List<RecipeExecutionHistory> history = recipeService.getHistory(project);

            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"status\": \"success\",\n");
            json.append("  \"edition\": \"premium\",\n");
            json.append("  \"projectPath\": \"").append(escapeJson(projectPath)).append("\",\n");
            json.append("  \"totalExecutions\": ").append(history.size()).append(",\n");
            json.append("  \"history\": [\n");

            for (int i = 0; i < history.size(); i++) {
                RecipeExecutionHistory entry = history.get(i);
                json.append("    {\n");
                json.append("      \"executionId\": ").append(entry.executionId()).append(",\n");
                json.append("      \"recipeName\": \"").append(escapeJson(entry.recipeName())).append("\",\n");
                json.append("      \"recipeDisplayName\": \"").append(escapeJson(entry.recipeDisplayName())).append("\",\n");
                json.append("      \"executionTime\": \"").append(entry.executionTime()).append("\",\n");
                json.append("      \"success\": ").append(entry.success()).append(",\n");
                json.append("      \"message\": \"").append(escapeJson(entry.message())).append("\",\n");
                json.append("      \"filesModified\": ").append(entry.filesModified()).append(",\n");
                json.append("      \"changesMade\": ").append(entry.changesMade()).append(",\n");
                json.append("      \"canUndo\": ").append(entry.canUndo()).append(",\n");
                json.append("      \"executionTimeMs\": ").append(entry.executionTimeMs()).append("\n");
                json.append("    }");
                if (i < history.size() - 1) {
                    json.append(",");
                }
                json.append("\n");
            }

            json.append("  ]\n");
            json.append("}");

            return json.toString();

        } catch (Exception e) {
            log.error("Unexpected error getting refactor history", e);
            return createErrorResponse("Unexpected error: " + e.getMessage());
        }
    }

    private String createErrorResponse(String message) {
        return "{\n" +
                "  \"status\": \"error\",\n" +
                "  \"edition\": \"premium\",\n" +
                "  \"message\": \"" + escapeJson(message) + "\"\n" +
                "}";
    }

    private String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
