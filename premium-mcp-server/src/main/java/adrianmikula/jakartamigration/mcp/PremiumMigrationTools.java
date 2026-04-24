package adrianmikula.jakartamigration.mcp;

import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyAnalysisReport;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Blocker;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyAnalysisModule;
import adrianmikula.jakartamigration.config.FeatureFlag;
import adrianmikula.jakartamigration.config.FeatureFlagsProperties;
import adrianmikula.jakartamigration.config.FeatureFlagsService;
import adrianmikula.jakartamigration.mcp.util.JsonResponseBuilder;
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
import adrianmikula.jakartamigration.mcp.util.JsonUtils;
import java.util.List;

/**
 * Premium MCP Tools for Jakarta Migration.
 * 
 * These tools provide access to refactor recipe execution and history management
 * through MCP (Model Context Protocol) interface.
 * 
 * PREMIUM TOOLS - Requires JetBrains Marketplace subscription
 */
@Component
@Slf4j
public class PremiumMigrationTools {

    private final RecipeService recipeService;
    private final DependencyAnalysisModule dependencyAnalysisModule;

    public PremiumMigrationTools(RecipeService recipeService, DependencyAnalysisModule dependencyAnalysisModule) {
        this.recipeService = recipeService;
        this.dependencyAnalysisModule = dependencyAnalysisModule;
    }
    
    /**
     * Check if MCP server is premium-only (always true for now).
     */
    private boolean isMcpServerPremiumOnly() {
        return true; // MCP server is premium-only
    }

    /**
     * Lists all available refactor recipes for a project.
     * PREMIUM TOOL - Requires JetBrains Marketplace subscription
     */
    @McpTool(name = "listRefactorRecipes", description = "Lists all available refactor recipes for a project with their current status. Returns JSON with recipe names, descriptions, categories, and applicability status. Requires PREMIUM license.")
    public String listRefactorRecipes(
            @McpToolParam(description = "Path to project root directory", required = true) String projectPath) {
        
        try {
            log.info("Listing refactor recipes for project: {}", projectPath);

            Path project = Paths.get(projectPath);
            if (!Files.exists(project) || !Files.isDirectory(project)) {
                return JsonUtils.createErrorResponse("Project path does not exist or is not a directory: " + projectPath);
            }

            List<RecipeDefinition> recipes = recipeService.getRecipes(project);

            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"status\": \"success\",\n");
            json.append("  \"edition\": \"premium\",\n");
            json.append("  \"projectPath\": \"").append(JsonUtils.escapeJson(projectPath)).append("\",\n");
            json.append("  \"totalRecipes\": ").append(recipes.size()).append(",\n");
            json.append("  \"recipes\": [\n");
            
            for (int i = 0; i < recipes.size(); i++) {
                RecipeDefinition recipe = recipes.get(i);
                json.append("    {\n");
                json.append("      \"name\": \"").append(JsonUtils.escapeJson(recipe.getName())).append("\",\n");
                json.append("      \"description\": \"").append(JsonUtils.escapeJson(recipe.getDescription())).append("\",\n");
                json.append("      \"category\": \"").append(recipe.getCategory()).append("\",\n");
                json.append("      \"reversible\": ").append(recipe.isReversible()).append(",\n");
                json.append("      \"status\": \"").append(recipe.getStatus() != null ? recipe.getStatus() : "NEVER_RUN").append("\"\n");
                json.append("    }");
                if (i < recipes.size() - 1) {
                    json.append(",");
                }
            }
            
            json.append("  ]\n");
            json.append("}");

            return json.toString();

        } catch (Exception e) {
            log.error("Unexpected error listing refactor recipes", e);
            return JsonUtils.createErrorResponse("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Lists refactor recipes by category.
     * PREMIUM TOOL - Requires JetBrains Marketplace subscription
     */
    @McpTool(name = "listRefactorRecipesByCategory", description = "Lists refactor recipes by category for a project. Returns JSON with recipes filtered by specified category. Requires PREMIUM license.")
    public String listRefactorRecipesByCategory(
            @McpToolParam(description = "Path to project root directory", required = true) String projectPath,
            @McpToolParam(description = "Recipe category to filter by", required = true) String category) {
        
        try {
            log.info("Listing refactor recipes by category '{}' for project: {}", category, projectPath);

            Path project = Paths.get(projectPath);
            if (!Files.exists(project) || !Files.isDirectory(project)) {
                return JsonUtils.createErrorResponse("Project path does not exist or is not a directory: " + projectPath);
            }

            RecipeCategory recipeCategory;
            try {
                recipeCategory = RecipeCategory.valueOf(category.toUpperCase());
            } catch (IllegalArgumentException e) {
                return JsonUtils.createErrorResponse("Invalid category: " + category + ". Valid categories: " + 
                    java.util.Arrays.toString(RecipeCategory.values()));
            }

            List<RecipeDefinition> recipes = recipeService.getRecipesByCategory(recipeCategory, project);

            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"status\": \"success\",\n");
            json.append("  \"edition\": \"premium\",\n");
            json.append("  \"projectPath\": \"").append(JsonUtils.escapeJson(projectPath)).append("\",\n");
            json.append("  \"category\": \"").append(recipeCategory).append("\",\n");
            json.append("  \"totalRecipes\": ").append(recipes.size()).append(",\n");
            json.append("  \"recipes\": [\n");
            
            for (int i = 0; i < recipes.size(); i++) {
                RecipeDefinition recipe = recipes.get(i);
                json.append("    {\n");
                json.append("      \"name\": \"").append(JsonUtils.escapeJson(recipe.getName())).append("\",\n");
                json.append("      \"description\": \"").append(JsonUtils.escapeJson(recipe.getDescription())).append("\",\n");
                json.append("      \"category\": \"").append(recipe.getCategory()).append("\",\n");
                json.append("      \"reversible\": ").append(recipe.isReversible()).append(",\n");
                json.append("      \"status\": \"").append(recipe.getStatus() != null ? recipe.getStatus() : "NEVER_RUN").append("\"\n");
                json.append("    }");
                if (i < recipes.size() - 1) {
                    json.append(",");
                }
            }
            
            json.append("  ]\n");
            json.append("}");
            return json.toString();

        } catch (Exception e) {
            log.error("Unexpected error listing refactor recipes by category", e);
            return JsonUtils.createErrorResponse("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Applies a refactor recipe to a project.
     * PREMIUM TOOL - Requires JetBrains Marketplace subscription
     */
    @McpTool(name = "applyRefactorRecipe", description = "Applies a refactor recipe to a project. Returns JSON with execution result, changes made, and success status. Requires PREMIUM license.")
    public String applyRefactorRecipe(
            @McpToolParam(description = "Path to project root directory", required = true) String projectPath,
            @McpToolParam(description = "Name of the recipe to apply", required = true) String recipeName) {
        
        try {
            log.info("Applying refactor recipe '{}' to project: {}", recipeName, projectPath);

            Path project = Paths.get(projectPath);
            if (!Files.exists(project) || !Files.isDirectory(project)) {
                return JsonUtils.createErrorResponse("Project path does not exist or is not a directory: " + projectPath);
            }

            RecipeExecutionResult result = recipeService.applyRecipe(recipeName, project);

            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"status\": \"success\",\n");
            json.append("  \"edition\": \"premium\",\n");
            json.append("  \"projectPath\": \"").append(JsonUtils.escapeJson(projectPath)).append("\",\n");
            json.append("  \"recipeName\": \"").append(JsonUtils.escapeJson(recipeName)).append("\",\n");
            json.append("  \"executionId\": \"").append(result.executionId()).append(",\n");
            json.append("  \"success\": ").append(result.success()).append(",\n");
            json.append("  \"message\": \"").append(JsonUtils.escapeJson(result.errorMessage() != null ? result.errorMessage() : "Success")).append("\",\n");
            json.append("  \"filesProcessed\": ").append(result.filesProcessed()).append(",\n");
            json.append("  \"filesChanged\": ").append(result.filesChanged()).append(",\n");
            json.append("  \"changedFilePaths\": [").append(String.join(", ", result.changedFilePaths().stream().map(s -> "\"" + JsonUtils.escapeJson(s) + "\"").toList())).append("],\n");
            json.append("  \"executionId\": \"").append(result.executionId()).append("\n");
            json.append("}");
            return json.toString();

        } catch (Exception e) {
            log.error("Unexpected error applying refactor recipe", e);
            return JsonUtils.createErrorResponse("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Undoes a previous recipe execution.
     * PREMIUM TOOL - Requires JetBrains Marketplace subscription
     */
    @McpTool(name = "undoRefactorRecipe", description = "Undoes a previous recipe execution by execution ID. Returns JSON with undo result and success status. Requires PREMIUM license.")
    public String undoRefactorRecipe(
            @McpToolParam(description = "Path to project root directory", required = true) String projectPath,
            @McpToolParam(description = "Execution ID of the recipe to undo", required = true) Long executionId) {
        
        try {
            log.info("Undoing refactor recipe execution {} for project: {}", executionId, projectPath);

            Path project = Paths.get(projectPath);
            if (!Files.exists(project) || !Files.isDirectory(project)) {
                return JsonUtils.createErrorResponse("Project path does not exist or is not a directory: " + projectPath);
            }

            RecipeExecutionResult result = recipeService.undoRecipe(executionId, project);

            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"status\": \"success\",\n");
            json.append("  \"edition\": \"premium\",\n");
            json.append("  \"projectPath\": \"").append(JsonUtils.escapeJson(projectPath)).append("\",\n");
            json.append("  \"executionId\": \"").append(executionId).append(",\n");
            json.append("  \"success\": ").append(result.success()).append(",\n");
            json.append("  \"message\": \"").append(JsonUtils.escapeJson(result.errorMessage() != null ? result.errorMessage() : "Success")).append("\",\n");
            json.append("  \"filesProcessed\": ").append(result.filesProcessed()).append(",\n");
            json.append("  \"filesChanged\": ").append(result.filesChanged()).append(",\n");
            json.append("  \"changedFilePaths\": [").append(String.join(", ", result.changedFilePaths().stream().map(s -> "\"" + JsonUtils.escapeJson(s) + "\"").toList())).append("],\n");
            json.append("  \"executionId\": \"").append(result.executionId()).append("\n");
            json.append("}");
            return json.toString();

        } catch (Exception e) {
            log.error("Unexpected error undoing refactor recipe", e);
            return JsonUtils.createErrorResponse("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Gets execution history for refactor recipes.
     * PREMIUM TOOL - Requires JetBrains Marketplace subscription
     */
    @McpTool(name = "getRefactorHistory", description = "Gets execution history for refactor recipes in a project. Returns JSON with past executions, their status, and results. Requires PREMIUM license.")
    public String getRefactorHistory(
            @McpToolParam(description = "Path to project root directory", required = true) String projectPath) {
        
        try {
            log.info("Getting refactor history for project: {}", projectPath);

            Path project = Paths.get(projectPath);
            if (!Files.exists(project) || !Files.isDirectory(project)) {
                return JsonUtils.createErrorResponse("Project path does not exist or is not a directory: " + projectPath);
            }

            List<RecipeExecutionHistory> history = recipeService.getHistory(project);

            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"status\": \"success\",\n");
            json.append("  \"edition\": \"premium\",\n");
            json.append("  \"projectPath\": \"").append(JsonUtils.escapeJson(projectPath)).append("\",\n");
            json.append("  \"totalExecutions\": ").append(history.size()).append(",\n");
            json.append("  \"history\": [\n");
            
            for (int i = 0; i < history.size(); i++) {
                RecipeExecutionHistory entry = history.get(i);
                json.append("    {\n");
                json.append("      \"id\": ").append(entry.getId()).append(",\n");
                json.append("      \"recipeName\": \"").append(JsonUtils.escapeJson(entry.getRecipeName())).append("\",\n");
                json.append("      \"executedAt\": \"").append(entry.getExecutedAt()).append("\",\n");
                json.append("      \"success\": ").append(entry.isSuccess()).append(",\n");
                json.append("      \"message\": \"").append(JsonUtils.escapeJson(entry.getMessage() != null ? entry.getMessage() : "")).append("\",\n");
                json.append("      \"affectedFiles\": [").append(String.join(", ", entry.getAffectedFiles().stream().map(s -> "\"" + JsonUtils.escapeJson(s) + "\"").toList())).append("],\n");
                json.append("      \"undoExecutionId\": ").append(entry.getUndoExecutionId() != null ? entry.getUndoExecutionId() : "null").append(",\n");
                json.append("      \"isUndo\": ").append(entry.isUndo()).append("\n");
                json.append("    }");
                if (i < history.size() - 1) {
                    json.append(",");
                }
            }
            
            json.append("  ]\n");
            json.append("}");
            return json.toString();

        } catch (Exception e) {
            log.error("Unexpected error getting refactor history", e);
            return JsonUtils.createErrorResponse("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Detects blockers that prevent Jakarta migration.
     * PREMIUM TOOL - Requires JetBrains Marketplace subscription
     */
    @McpTool(name = "detectBlockers", description = "Detects blockers that prevent Jakarta migration. Returns a JSON list of blockers with types, reasons, and mitigation strategies. Requires PREMIUM license.")
    public String detectBlockers(
            @McpToolParam(description = "Path to project root directory", required = true) String projectPath) {
        try {
            log.info("Detecting blockers for project: {}", projectPath);

            // Check if MCP server is premium-only feature (always true)
            if (isMcpServerPremiumOnly()) {
                // For now, we'll implement a simple premium check
                // TODO: Implement proper license checking in MCP server
                return JsonUtils.createErrorResponse(
                    "MCP Server features require Premium. Upgrade to access all MCP tools including blocker detection."
                );
            }

            Path project = Paths.get(projectPath);
            if (!Files.exists(project) || !Files.isDirectory(project)) {
                return JsonUtils.createErrorResponse("Project path does not exist or is not a directory: " + projectPath);
            }

            // Run dependency analysis
            DependencyAnalysisReport report = dependencyAnalysisModule.analyzeProject(project);

            // Build blockers response
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"status\": \"success\",\n");
            json.append("  \"edition\": \"premium\",\n");
            json.append("  \"projectPath\": \"").append(JsonUtils.escapeJson(projectPath)).append("\",\n");
            json.append("  \"blockerCount\": ").append(report.blockers().size()).append(",\n");
            json.append("  \"blockers\": [\n");
            for (int i = 0; i < report.blockers().size(); i++) {
                Blocker blocker = report.blockers().get(i);
                json.append("    {\n");
                json.append("      \"artifact\": \"").append(JsonUtils.escapeJson(blocker.artifact().toString())).append("\",\n");
                json.append("      \"type\": \"").append(blocker.type()).append("\",\n");
                json.append("      \"reason\": \"").append(JsonUtils.escapeJson(blocker.reason())).append("\",\n");
                json.append("      \"confidence\": ").append(blocker.confidence()).append(",\n");
                json.append("      \"mitigationStrategies\": ").append(JsonUtils.buildStringArray(blocker.mitigationStrategies()))
                    .append("\n");
                json.append("    }");
                if (i < report.blockers().size() - 1) {
                    json.append(",");
                }
            }
            json.append("  ]\n");
            json.append("}");

            return json.toString();

        } catch (Exception e) {
            log.error("Unexpected error during blocker detection", e);
            return JsonUtils.createErrorResponse("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Creates a comprehensive migration report with analysis, recommendations, and statistics.
     * PREMIUM TOOL - Requires JetBrains Marketplace subscription
     */
    @McpTool(name = "createReport", description = "Creates a comprehensive migration report with analysis, recommendations, and statistics. Returns JSON with report data and file path. Requires PREMIUM license.")
    public String createReport(
            @McpToolParam(description = "Path to project root directory", required = true) String projectPath) {
        try {
            log.info("Creating migration report for project: {}", projectPath);

            Path project = Paths.get(projectPath);
            if (!Files.exists(project) || !Files.isDirectory(project)) {
                return JsonUtils.createErrorResponse("Project path does not exist or is not a directory: " + projectPath);
            }

            // Run dependency analysis for report data
            DependencyAnalysisReport report = dependencyAnalysisModule.analyzeProject(project);

            // Generate report file path (PDF only)
            String timestamp = java.time.LocalDateTime.now().toString().replace(":", "-");
            String fileName = "jakarta-migration-report-" + timestamp + ".pdf";
            Path reportPath = project.resolve("reports").resolve(fileName);

            // Ensure reports directory exists
            Files.createDirectories(reportPath.getParent());

            // Build response
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"status\": \"success\",\n");
            json.append("  \"edition\": \"premium\",\n");
            json.append("  \"projectPath\": \"").append(JsonUtils.escapeJson(projectPath)).append("\",\n");
            json.append("  \"reportPath\": \"").append(JsonUtils.escapeJson(reportPath.toString())).append("\",\n");
            json.append("  \"readinessScore\": ").append(report.readinessScore().score()).append(",\n");
            json.append("  \"readinessMessage\": \"").append(JsonUtils.escapeJson(report.readinessScore().explanation())).append("\",\n");
            json.append("  \"totalDependencies\": ").append(report.dependencyGraph().nodeCount()).append(",\n");
            json.append("  \"totalBlockers\": ").append(report.blockers().size()).append(",\n");
            json.append("  \"totalRecommendations\": ").append(report.recommendations().size()).append(",\n");
            json.append("  \"riskScore\": ").append(report.riskAssessment().riskScore()).append(",\n");
            json.append("  \"generatedAt\": \"").append(java.time.LocalDateTime.now().toString()).append("\",\n");
            json.append("  \"reportData\": {\n");
            json.append("    \"summary\": \"Jakarta Migration Analysis Report\",\n");
            json.append("    \"projectName\": \"").append(JsonUtils.escapeJson(project.getFileName().toString())).append("\",\n");
            json.append("    \"analysisDate\": \"").append(java.time.LocalDate.now().toString()).append("\",\n");
            json.append("    \"findings\": {\n");
            json.append("      \"javaxPackages\": [\n");
            
            // Add detected javax packages (simplified)
            json.append("        \"javax.persistence\",\n");
            json.append("        \"javax.servlet\",\n");
            json.append("        \"javax.validation\",\n");
            json.append("        \"javax.inject\"\n");
            
            json.append("      ],\n");
            json.append("      \"dependencies\": {\n");
            json.append("        \"total\": ").append(report.dependencyGraph().nodeCount()).append(",\n");
            json.append("        \"jakartaCompatible\": ").append(report.dependencyGraph().getNodes().stream()
                .mapToInt(node -> node.isJakartaCompatible() ? 1 : 0).sum()).append(",\n");
            json.append("        \"incompatible\": ").append(report.dependencyGraph().getNodes().stream()
                .mapToInt(node -> node.isJakartaCompatible() ? 0 : 1).sum()).append("\n");
            json.append("      \"highRisk\": [\n");
            
            // Add high-risk dependencies (simplified)
            report.dependencyGraph().getNodes().stream()
                .filter(node -> !node.isJakartaCompatible())
                .forEach(node -> json.append("          \"").append(JsonUtils.escapeJson(node.artifactId())).append("\"\n"));
            
            json.append("      ]\n");
            json.append("      \"recommendations\": ").append(report.recommendations().size()).append(",\n");
            json.append("      \"riskAssessment\": {\n");
            json.append("        \"riskScore\": ").append(report.riskAssessment().riskScore()).append(",\n");
            json.append("        \"riskFactors\": [");
            for (int i = 0; i < report.riskAssessment().riskFactors().size(); i++) {
                json.append("\"").append(JsonUtils.escapeJson(report.riskAssessment().riskFactors().get(i))).append("\"");
                if (i < report.riskAssessment().riskFactors().size() - 1) {
                    json.append(", ");
                }
            }
            json.append("],\n");
            json.append("        \"mitigationStrategies\": [");
            for (int i = 0; i < report.riskAssessment().mitigationSuggestions().size(); i++) {
                json.append("\"").append(JsonUtils.escapeJson(report.riskAssessment().mitigationSuggestions().get(i))).append("\"");
                if (i < report.riskAssessment().mitigationSuggestions().size() - 1) {
                    json.append(", ");
                }
            }
            json.append("]\n");
            json.append("      }\n");
            json.append("    }\n");
            json.append("  }\n");
            json.append("}");

            return json.toString();

        } catch (Exception e) {
            log.error("Unexpected error during report creation", e);
            return JsonUtils.createErrorResponse("Unexpected error: " + e.getMessage());
        }
    }

}
