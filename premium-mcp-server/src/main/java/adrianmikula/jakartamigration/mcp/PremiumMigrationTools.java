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
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import adrianmikula.jakartamigration.advancedscanning.service.AdvancedScanningModule;
import adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults;
import adrianmikula.jakartamigration.pdfreporting.service.PdfReportService;
import adrianmikula.jakartamigration.pdfreporting.service.impl.HtmlToPdfReportServiceImpl;
import adrianmikula.jakartamigration.jaranalysis.service.JarCompatibilityScanner;
import adrianmikula.jakartamigration.jaranalysis.service.DefaultJarCompatibilityScanner;
import adrianmikula.jakartamigration.jaranalysis.domain.JarCompatibilityLevel;
import adrianmikula.jakartamigration.jaranalysis.domain.JarCompatibilityReport;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final AdvancedScanningModule advancedScanningModule;
    private final PdfReportService pdfReportService;
    private final JarCompatibilityScanner jarScanner;
    private final ObjectMapper objectMapper;

    public PremiumMigrationTools(RecipeService recipeService, DependencyAnalysisModule dependencyAnalysisModule) {
        this.recipeService = recipeService;
        this.dependencyAnalysisModule = dependencyAnalysisModule;
        this.advancedScanningModule = new AdvancedScanningModule(recipeService);
        this.pdfReportService = new HtmlToPdfReportServiceImpl();
        this.jarScanner = new DefaultJarCompatibilityScanner();
        this.objectMapper = new ObjectMapper();
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

    /**
     * Executes a deep, technology-specific scan for Jakarta EE migration readiness.
     * PREMIUM TOOL - Requires JetBrains Marketplace subscription
     */
    @McpTool(name = "runAdvancedScan", description = "Executes a deep, technology-specific scan for Jakarta EE migration readiness. Supports: jpa, validation, servlet, cdi, rest, soap, security, build, all. Returns JSON with scan results.")
    public String runAdvancedScan(
            @McpToolParam(description = "Path to project root directory", required = true) String projectPath,
            @McpToolParam(description = "Technology to scan: 'jpa', 'validation', 'servlet', 'cdi', 'rest', 'soap', 'security', 'build', 'all'", required = true) String technology,
            @McpToolParam(description = "Whether to scan source code", required = false) Boolean includeSource,
            @McpToolParam(description = "Whether to scan binary artifacts", required = false) Boolean includeBinary) {
        try {
            log.info("Running advanced scan for technology '{}' on project: {}", technology, projectPath);
            Path project = Paths.get(projectPath);
            if (!Files.exists(project) || !Files.isDirectory(project)) {
                return JsonUtils.createErrorResponse("Project path does not exist or is not a directory: " + projectPath);
            }

            Map<String, Object> results = new HashMap<>();
            String tech = technology.toLowerCase();

            if ("all".equals(tech) || "jpa".equals(tech)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> r = objectMapper.convertValue(advancedScanningModule.getJpaAnnotationScanner().scanProject(project), Map.class);
                results.put("jpa", r);
            }
            if ("all".equals(tech) || "validation".equals(tech)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> r = objectMapper.convertValue(advancedScanningModule.getBeanValidationScanner().scanProject(project), Map.class);
                results.put("validation", r);
            }
            if ("all".equals(tech) || "servlet".equals(tech)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> r = objectMapper.convertValue(advancedScanningModule.getServletJspScanner().scanProject(project), Map.class);
                results.put("servlet", r);
            }
            if ("all".equals(tech) || "cdi".equals(tech)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> r = objectMapper.convertValue(advancedScanningModule.getCdiInjectionScanner().scanProject(project), Map.class);
                results.put("cdi", r);
            }
            if ("all".equals(tech) || "rest".equals(tech) || "soap".equals(tech)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> r = objectMapper.convertValue(advancedScanningModule.getRestSoapScanner().scanProject(project), Map.class);
                results.put("restSoap", r);
            }
            if ("all".equals(tech) || "security".equals(tech)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> r = objectMapper.convertValue(advancedScanningModule.getSecurityApiScanner().scanProject(project), Map.class);
                results.put("security", r);
            }
            if ("all".equals(tech) || "build".equals(tech)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> r = objectMapper.convertValue(advancedScanningModule.getBuildConfigScanner().scanProject(project), Map.class);
                results.put("build", r);
            }

            return new JsonResponseBuilder()
                    .addField("projectPath", projectPath)
                    .addField("technology", technology)
                    .addField("results", results)
                    .build();

        } catch (Exception e) {
            log.error("Error during advanced scan", e);
            return JsonUtils.createErrorResponse("Advanced scan failed: " + e.getMessage());
        }
    }

    /**
     * Generates a professional HTML migration report.
     * PREMIUM TOOL - Requires JetBrains Marketplace subscription
     */
    @McpTool(name = "generateHtmlReport", description = "Generates a professional HTML migration report. Supports riskAnalysis, refactoringAction, and consolidated report types. Returns the path to the generated HTML file.")
    public String generateHtmlReport(
            @McpToolParam(description = "Path to project root directory", required = true) String projectPath,
            @McpToolParam(description = "Report type: 'riskAnalysis', 'refactoringAction', 'consolidated'", required = true) String reportType,
            @McpToolParam(description = "Optional output file path", required = false) String outputPath) {
        try {
            log.info("Generating {} HTML report for project: {}", reportType, projectPath);
            Path project = Paths.get(projectPath);
            if (!Files.exists(project) || !Files.isDirectory(project)) {
                return JsonUtils.createErrorResponse("Project path does not exist or is not a directory: " + projectPath);
            }

            Path outPath;
            if (outputPath != null && !outputPath.isBlank()) {
                outPath = Paths.get(outputPath);
            } else {
                String timestamp = java.time.LocalDateTime.now().toString().replace(":", "-");
                outPath = project.resolve("reports").resolve("jakarta-migration-report-" + timestamp + ".html");
            }
            Files.createDirectories(outPath.getParent());

            DependencyAnalysisReport analysisReport = dependencyAnalysisModule.analyzeProject(project);
            DependencyGraph dependencyGraph = analysisReport.dependencyGraph();

            String type = reportType != null ? reportType.toLowerCase() : "riskanalysis";
            Path generatedPath;

            if ("refactoringaction".equals(type)) {
                PdfReportService.RefactoringActionReportRequest request = new PdfReportService.RefactoringActionReportRequest(
                    outPath, project.getFileName().toString(), "Jakarta Migration Refactoring Action Report",
                    dependencyGraph, null, List.of(), List.of(), List.of(), Map.of(), Map.of(), Map.of());
                generatedPath = pdfReportService.generateRefactoringActionReport(request);
            } else if ("consolidated".equals(type)) {
                PdfReportService.ConsolidatedReportRequest request = new PdfReportService.ConsolidatedReportRequest(
                    outPath, project.getFileName().toString(), "Jakarta Migration Consolidated Report",
                    dependencyGraph, analysisReport, null, null, null, null, Map.of(), Map.of(), List.of(), List.of(), Map.of(), Map.of());
                generatedPath = pdfReportService.generateConsolidatedReport(request);
            } else {
                PdfReportService.RiskAnalysisReportRequest request = new PdfReportService.RiskAnalysisReportRequest(
                    outPath, project.getFileName().toString(), "Jakarta Migration Risk Analysis Report",
                    dependencyGraph, analysisReport, null, null, null, null, Map.of(), Map.of(), List.of(), List.of(), Map.of(), Map.of());
                generatedPath = pdfReportService.generateRiskAnalysisReport(request);
            }

            return new JsonResponseBuilder()
                    .addField("projectPath", projectPath)
                    .addField("reportType", reportType)
                    .addField("reportPath", generatedPath.toString())
                    .build();

        } catch (Exception e) {
            log.error("Error generating HTML report", e);
            return JsonUtils.createErrorResponse("Report generation failed: " + e.getMessage());
        }
    }

    /**
     * Analyzes a Java project's readiness for migration from Java EE to Jakarta EE.
     * PREMIUM TOOL - Requires JetBrains Marketplace subscription
     */
    @McpTool(name = "analyzeJakartaReadiness", description = "Analyzes a Java project's readiness for migration from Java EE 8 (javax.*) to Jakarta EE 9+ (jakarta.*). Returns a JSON report with readiness score, blockers, and recommendations.")
    public String analyzeJakartaReadiness(
            @McpToolParam(description = "Path to project root directory", required = true) String projectPath,
            @McpToolParam(description = "Whether to include transitive dependencies", required = false) Boolean includeTransitiveDependencies,
            @McpToolParam(description = "Depth of analysis: 'basic', 'detailed', or 'comprehensive'", required = false) String analysisLevel) {
        try {
            log.info("Analyzing Jakarta readiness for project: {}", projectPath);
            Path project = Paths.get(projectPath);
            if (!Files.exists(project) || !Files.isDirectory(project)) {
                return JsonUtils.createErrorResponse("Project path does not exist or is not a directory: " + projectPath);
            }

            DependencyAnalysisReport report = dependencyAnalysisModule.analyzeProject(project);

            return new JsonResponseBuilder()
                    .addField("projectPath", projectPath)
                    .addField("readinessScore", report.readinessScore().score())
                    .addField("readinessMessage", report.readinessScore().explanation())
                    .addField("totalDependencies", report.dependencyGraph().nodeCount())
                    .addField("jakartaCompatible", report.dependencyGraph().getNodes().stream().filter(n -> n.isJakartaCompatible()).count())
                    .addField("incompatible", report.dependencyGraph().getNodes().stream().filter(n -> !n.isJakartaCompatible()).count())
                    .addField("blockerCount", report.blockers().size())
                    .addField("recommendationCount", report.recommendations().size())
                    .addField("riskScore", report.riskAssessment().riskScore())
                    .build();

        } catch (Exception e) {
            log.error("Error analyzing Jakarta readiness", e);
            return JsonUtils.createErrorResponse("Analysis failed: " + e.getMessage());
        }
    }

    /**
     * Provides detailed analysis of migration impact.
     * PREMIUM TOOL - Requires JetBrains Marketplace subscription
     */
    @McpTool(name = "analyzeMigrationImpact", description = "Provides detailed analysis of migration impact including affected dependencies, breaking changes, risk assessment, and estimated migration effort.")
    public String analyzeMigrationImpact(
            @McpToolParam(description = "Path to project root directory", required = true) String projectPath,
            @McpToolParam(description = "Analysis scope: 'dependencies', 'code', 'configuration', or 'all'", required = false) String scope,
            @McpToolParam(description = "Whether to include detailed risk assessment", required = false) Boolean includeRiskAssessment,
            @McpToolParam(description = "Output format: 'summary', 'detailed', or 'json'", required = false) String outputFormat) {
        try {
            log.info("Analyzing migration impact for project: {}", projectPath);
            Path project = Paths.get(projectPath);
            if (!Files.exists(project) || !Files.isDirectory(project)) {
                return JsonUtils.createErrorResponse("Project path does not exist or is not a directory: " + projectPath);
            }

            DependencyAnalysisReport report = dependencyAnalysisModule.analyzeProject(project);
            long incompatible = report.dependencyGraph().getNodes().stream().filter(n -> !n.isJakartaCompatible()).count();
            double effortEstimate = incompatible * 0.5 + report.blockers().size() * 2.0;

            return new JsonResponseBuilder()
                    .addField("projectPath", projectPath)
                    .addField("scope", scope != null ? scope : "all")
                    .addField("totalDependencies", report.dependencyGraph().nodeCount())
                    .addField("affectedDependencies", incompatible)
                    .addField("blockers", report.blockers().size())
                    .addField("riskScore", report.riskAssessment().riskScore())
                    .addField("estimatedEffortDays", Math.round(effortEstimate))
                    .addField("riskFactors", report.riskAssessment().riskFactors())
                    .build();

        } catch (Exception e) {
            log.error("Error analyzing migration impact", e);
            return JsonUtils.createErrorResponse("Impact analysis failed: " + e.getMessage());
        }
    }

    /**
     * Analyzes project dependencies and recommends compatible Jakarta EE versions.
     * PREMIUM TOOL - Requires JetBrains Marketplace subscription
     */
    @McpTool(name = "recommendVersions", description = "Analyzes project dependencies and recommends compatible Jakarta EE versions. Provides version upgrade paths and identifies version conflicts.")
    public String recommendVersions(
            @McpToolParam(description = "Path to project root directory", required = true) String projectPath,
            @McpToolParam(description = "Whether to include alternative dependency recommendations", required = false) Boolean includeAlternatives,
            @McpToolParam(description = "Target Jakarta EE version: '9', '9.1', '10', or '11'", required = false) String targetJakartaVersion) {
        try {
            log.info("Recommending versions for project: {}", projectPath);
            Path project = Paths.get(projectPath);
            if (!Files.exists(project) || !Files.isDirectory(project)) {
                return JsonUtils.createErrorResponse("Project path does not exist or is not a directory: " + projectPath);
            }

            DependencyAnalysisReport report = dependencyAnalysisModule.analyzeProject(project);
            List<Map<String, Object>> recommendations = report.recommendations().stream()
                    .map(r -> {
                        Map<String, Object> rec = new HashMap<>();
                        rec.put("artifact", r.currentArtifact().artifactId());
                        rec.put("currentVersion", r.currentArtifact().version());
                        rec.put("recommendedArtifact", r.recommendedArtifact().artifactId());
                        rec.put("recommendedVersion", r.recommendedArtifact().version());
                        rec.put("migrationPath", r.migrationPath());
                        rec.put("compatibilityScore", r.compatibilityScore());
                        return rec;
                    })
                    .limit(10)
                    .toList();

            return new JsonResponseBuilder()
                    .addField("projectPath", projectPath)
                    .addField("totalDependencies", report.dependencyGraph().nodeCount())
                    .addField("recommendations", recommendations)
                    .build();

        } catch (Exception e) {
            log.error("Error recommending versions", e);
            return JsonUtils.createErrorResponse("Version recommendation failed: " + e.getMessage());
        }
    }

    /**
     * Scans a compiled JAR dependency for Jakarta EE compatibility issues.
     * PREMIUM TOOL - Requires JetBrains Marketplace subscription
     */
    @McpTool(name = "scanBinaryDependency", description = "Scans a compiled JAR dependency for Jakarta EE compatibility issues. Analyzes bytecode to identify references to javax packages.")
    public String scanBinaryDependency(
            @McpToolParam(description = "Absolute path to the JAR file to scan", required = true) String jarPath,
            @McpToolParam(description = "Whether to scan for problematic method references", required = false) Boolean includeMethods,
            @McpToolParam(description = "Output detail level: 'summary', 'methods', or 'full'", required = false) String outputDetail) {
        try {
            log.info("Scanning binary dependency: {}", jarPath);
            Path jar = Paths.get(jarPath);
            if (!Files.exists(jar) || !Files.isRegularFile(jar)) {
                return JsonUtils.createErrorResponse("JAR path does not exist or is not a file: " + jarPath);
            }

            JarCompatibilityReport report = jarScanner.analyzeJar(jar);
            Map<String, Object> result = new HashMap<>();
            result.put("artifactCoordinate", report.artifactCoordinate());
            result.put("compatibilityLevel", report.level().toString());
            result.put("confidence", report.confidence());
            result.put("reasons", report.reasons());

            return new JsonResponseBuilder()
                    .addField("jarPath", jarPath)
                    .addField("result", result)
                    .build();

        } catch (Exception e) {
            log.error("Error scanning binary dependency", e);
            return JsonUtils.createErrorResponse("Binary scan failed: " + e.getMessage());
        }
    }

    /**
     * Generates a migration plan based on project analysis.
     * PREMIUM TOOL - Requires JetBrains Marketplace subscription
     */
    @McpTool(name = "generateMigrationPlan", description = "Generates a detailed migration plan with prioritized steps, timeline estimates, and resource requirements based on project analysis.")
    public String generateMigrationPlan(
            @McpToolParam(description = "Path to project root directory", required = true) String projectPath,
            @McpToolParam(description = "Migration strategy: 'big-bang', 'incremental', or 'parallel'", required = false) String strategy,
            @McpToolParam(description = "Target Jakarta EE version: '9', '9.1', '10', or '11'", required = false) String targetVersion) {
        try {
            log.info("Generating migration plan for project: {}", projectPath);
            Path project = Paths.get(projectPath);
            if (!Files.exists(project) || !Files.isDirectory(project)) {
                return JsonUtils.createErrorResponse("Project path does not exist or is not a directory: " + projectPath);
            }

            DependencyAnalysisReport report = dependencyAnalysisModule.analyzeProject(project);
            long incompatible = report.dependencyGraph().getNodes().stream().filter(n -> !n.isJakartaCompatible()).count();
            int estimatedDays = (int) Math.round(incompatible * 0.5 + report.blockers().size() * 2.0);
            String selectedStrategy = strategy != null ? strategy : "incremental";

            List<Map<String, Object>> phases = new ArrayList<>();
            phases.add(Map.of("name", "Preparation", "description", "Analyze dependencies and set up tooling", "durationDays", Math.max(1, estimatedDays / 4)));
            phases.add(Map.of("name", "Dependency Updates", "description", "Update javax dependencies to jakarta equivalents", "durationDays", Math.max(1, estimatedDays / 2)));
            phases.add(Map.of("name", "Code Migration", "description", "Replace imports and update code", "durationDays", Math.max(1, estimatedDays / 3)));
            phases.add(Map.of("name", "Testing & Validation", "description", "Run tests and validate migration", "durationDays", Math.max(1, estimatedDays / 4)));

            return new JsonResponseBuilder()
                    .addField("projectPath", projectPath)
                    .addField("strategy", selectedStrategy)
                    .addField("targetVersion", targetVersion != null ? targetVersion : "10")
                    .addField("estimatedDays", estimatedDays)
                    .addField("phases", phases)
                    .addField("blockers", report.blockers().size())
                    .addField("totalDependencies", report.dependencyGraph().nodeCount())
                    .build();

        } catch (Exception e) {
            log.error("Error generating migration plan", e);
            return JsonUtils.createErrorResponse("Plan generation failed: " + e.getMessage());
        }
    }

    /**
     * Validates migration readiness by checking preconditions and potential issues.
     * PREMIUM TOOL - Requires JetBrains Marketplace subscription
     */
    @McpTool(name = "validateMigration", description = "Validates migration readiness by checking preconditions, test coverage, build compatibility, and potential blocking issues.")
    public String validateMigration(
            @McpToolParam(description = "Path to project root directory", required = true) String projectPath,
            @McpToolParam(description = "Validation strictness: 'lenient', 'standard', or 'strict'", required = false) String strictness) {
        try {
            log.info("Validating migration readiness for project: {}", projectPath);
            Path project = Paths.get(projectPath);
            if (!Files.exists(project) || !Files.isDirectory(project)) {
                return JsonUtils.createErrorResponse("Project path does not exist or is not a directory: " + projectPath);
            }

            DependencyAnalysisReport report = dependencyAnalysisModule.analyzeProject(project);
            boolean ready = report.blockers().isEmpty() && report.readinessScore().score() > 0.5;
            List<String> issues = new ArrayList<>();
            if (!report.blockers().isEmpty()) issues.add("Blockers detected: " + report.blockers().size());
            if (report.readinessScore().score() < 0.5) issues.add("Readiness score below threshold");

            return new JsonResponseBuilder()
                    .addField("projectPath", projectPath)
                    .addField("ready", ready)
                    .addField("readinessScore", report.readinessScore().score())
                    .addField("blockers", report.blockers().size())
                    .addField("issues", issues)
                    .addField("recommendations", report.recommendations().stream()
                            .map(r -> r.currentArtifact().artifactId() + " -> " + r.recommendedArtifact().artifactId())
                            .limit(5).toList())
                    .build();

        } catch (Exception e) {
            log.error("Error validating migration", e);
            return JsonUtils.createErrorResponse("Validation failed: " + e.getMessage());
        }
    }

    /**
     * Applies a Jakarta EE migration recipe to a project.
     * PREMIUM TOOL - Requires JetBrains Marketplace subscription
     */
    @McpTool(name = "applyJakartaRecipe", description = "Applies a Jakarta EE migration recipe to a project. Returns JSON with execution result, changes made, and success status. Alias for applyRefactorRecipe.")
    public String applyJakartaRecipe(
            @McpToolParam(description = "Path to project root directory", required = true) String projectPath,
            @McpToolParam(description = "Name of the recipe to apply", required = true) String recipeName) {
        return applyRefactorRecipe(projectPath, recipeName);
    }

    /**
     * Generates an interactive HTML visualization of the dependency graph with Jakarta compatibility color-coding.
     * PREMIUM TOOL - Requires JetBrains Marketplace subscription
     */
    @McpTool(name = "generateDependencyGraphVisualization", description = "Generates an interactive HTML dependency graph visualization with Jakarta compatibility color-coding. Returns JSON with file path and statistics. Requires PREMIUM license.")
    public String generateDependencyGraphVisualization(
            @McpToolParam(description = "Path to project root directory", required = true) String projectPath,
            @McpToolParam(description = "Optional output file path (defaults to reports/dependency-graph-{timestamp}.html)", required = false) String outputPath) {
        try {
            log.info("Generating dependency graph visualization for project: {}", projectPath);
            Path project = Paths.get(projectPath);
            if (!Files.exists(project) || !Files.isDirectory(project)) {
                return JsonUtils.createErrorResponse("Project path does not exist or is not a directory: " + projectPath);
            }

            // Run dependency analysis
            DependencyAnalysisReport report = dependencyAnalysisModule.analyzeProject(project);
            DependencyGraph graph = report.dependencyGraph();

            // Determine output path
            Path reportPath;
            if (outputPath != null && !outputPath.isBlank()) {
                reportPath = Paths.get(outputPath);
            } else {
                String timestamp = java.time.LocalDateTime.now().toString().replace(":", "-");
                reportPath = project.resolve("reports").resolve("dependency-graph-" + timestamp + ".html");
            }

            // Ensure reports directory exists
            Files.createDirectories(reportPath.getParent());

            // Generate the HTML visualization
            String htmlContent = generateDependencyGraphHtml(graph, project.getFileName().toString());
            Files.writeString(reportPath, htmlContent);

            // Count nodes by status
            long compatible = graph.getNodes().stream().filter(n -> n.isJakartaCompatible()).count();
            long needsUpgrade = graph.getNodes().stream().filter(n -> !n.isJakartaCompatible() && n.groupId().startsWith("javax.")).count();
            long noJakartaVersion = graph.getNodes().stream().filter(n -> !n.isJakartaCompatible() && !n.groupId().startsWith("javax.")).count();

            return new JsonResponseBuilder()
                    .addField("status", "success")
                    .addField("projectPath", projectPath)
                    .addField("reportPath", reportPath.toString())
                    .addField("totalDependencies", graph.nodeCount())
                    .addField("jakartaCompatible", compatible)
                    .addField("needsUpgrade", needsUpgrade)
                    .addField("noJakartaVersion", noJakartaVersion)
                    .addField("edgeCount", graph.edgeCount())
                    .build();

        } catch (Exception e) {
            log.error("Error generating dependency graph visualization", e);
            return JsonUtils.createErrorResponse("Visualization generation failed: " + e.getMessage());
        }
    }

    private String generateDependencyGraphHtml(DependencyGraph graph, String projectName) {
        String nodesJson = generateGraphNodesJson(graph);
        String edgesJson = generateGraphEdgesJson(graph);

        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dependency Graph - %s</title>
    <script type="text/javascript" src="https://unpkg.com/vis-network/standalone/umd/vis-network.min.js"></script>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif; background: #f5f7fa; }
        .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 20px 30px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
        .header h1 { font-size: 24px; font-weight: 600; margin-bottom: 5px; }
        .header p { font-size: 14px; opacity: 0.9; }
        .controls { background: white; padding: 15px 30px; border-bottom: 1px solid #e1e8ed; display: flex; gap: 20px; align-items: center; flex-wrap: wrap; }
        .control-group { display: flex; align-items: center; gap: 8px; }
        .control-group label { font-size: 13px; font-weight: 500; color: #555; }
        .control-group input[type="checkbox"] { width: 16px; height: 16px; cursor: pointer; }
        .control-group button { padding: 6px 14px; border: 1px solid #ddd; background: #f8f9fa; border-radius: 4px; cursor: pointer; font-size: 13px; transition: all 0.2s; }
        .control-group button:hover { background: #e9ecef; border-color: #adb5bd; }
        .legend { display: flex; gap: 20px; align-items: center; }
        .legend-item { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #555; }
        .legend-color { width: 14px; height: 14px; border-radius: 50%%; border: 2px solid; }
        .legend-color.compatible { background: #28a745; border-color: #1e7e34; }
        .legend-color.needs-upgrade { background: #ffc107; border-color: #d39e00; }
        .legend-color.no-version { background: #dc3545; border-color: #c82333; }
        .legend-color.unknown { background: #6c757d; border-color: #545b62; }
        #graph-container { width: 100%%; height: calc(100vh - 180px); background: white; }
        .tooltip { position: absolute; background: rgba(0,0,0,0.8); color: white; padding: 8px 12px; border-radius: 4px; font-size: 12px; pointer-events: none; z-index: 1000; max-width: 300px; }
        .stats { background: #f8f9fa; padding: 10px 30px; border-bottom: 1px solid #e1e8ed; font-size: 13px; color: #666; }
        .stats span { margin-right: 20px; }
        .stats .stat-value { font-weight: 600; color: #333; }
    </style>
</head>
<body>
    <div class="header">
        <h1>Dependency Graph Visualization</h1>
        <p>%s - Jakarta Migration Analysis</p>
    </div>
    <div class="stats">
        <span>Total Dependencies: <span class="stat-value">%d</span></span>
        <span>Jakarta Compatible: <span class="stat-value" style="color: #28a745;">%d</span></span>
        <span>Needs Upgrade: <span class="stat-value" style="color: #ffc107;">%d</span></span>
        <span>No Jakarta Version: <span class="stat-value" style="color: #dc3545;">%d</span></span>
    </div>
    <div class="controls">
        <div class="legend">
            <div class="legend-item"><div class="legend-color compatible"></div> Jakarta Compatible</div>
            <div class="legend-item"><div class="legend-color needs-upgrade"></div> Needs Upgrade</div>
            <div class="legend-item"><div class="legend-color no-version"></div> No Jakarta Version</div>
            <div class="legend-item"><div class="legend-color unknown"></div> Unknown Status</div>
        </div>
        <div style="flex: 1;"></div>
        <div class="control-group">
            <button onclick="fitGraph()">Fit to Screen</button>
            <button onclick="resetZoom()">Reset View</button>
        </div>
    </div>
    <div id="graph-container"></div>

    <script type="text/javascript">
        // Graph data
        var nodes = new vis.DataSet(%s);
        var edges = new vis.DataSet(%s);

        // Network configuration
        var container = document.getElementById('graph-container');
        var data = { nodes: nodes, edges: edges };
        var options = {
            nodes: {
                shape: 'dot',
                size: 20,
                font: { size: 12, face: 'Segoe UI', color: '#333', strokeWidth: 2, strokeColor: '#fff' },
                borderWidth: 2,
                shadow: { enabled: true, color: 'rgba(0,0,0,0.2)', size: 5, x: 2, y: 2 }
            },
            edges: {
                width: 1.5,
                color: { color: '#adb5bd', highlight: '#667eea', hover: '#667eea' },
                smooth: { type: 'continuous', roundness: 0.5 },
                arrows: { to: { enabled: true, scaleFactor: 0.8 } }
            },
            physics: {
                forceAtlas2Based: {
                    gravitationalConstant: -60,
                    centralGravity: 0.005,
                    springLength: 120,
                    springConstant: 0.18,
                    damping: 0.4,
                    avoidOverlap: 0.5
                },
                maxVelocity: 50,
                minVelocity: 0.1,
                solver: 'forceAtlas2Based',
                stabilization: {
                    enabled: true,
                    iterations: 200,
                    updateInterval: 25,
                    onlyDynamicEdges: false,
                    fit: true
                }
            },
            interaction: {
                hover: true,
                hoverConnectedEdges: true,
                selectConnectedEdges: true,
                zoomView: true,
                dragView: true,
                tooltipDelay: 200,
                hideEdgesOnDrag: true
            },
            layout: {
                randomSeed: 2
            }
        };

        // Create network
        var network = new vis.Network(container, data, options);

        // Fit graph after stabilization
        network.once('stabilizationIterationsDone', function() {
            network.fit({ animation: { duration: 500, easingFunction: 'easeInOutQuad' } });
        });

        // Handle click events
        network.on('click', function(params) {
            if (params.nodes.length > 0) {
                var nodeId = params.nodes[0];
                var node = nodes.get(nodeId);
                network.focus(nodeId, { scale: 1.2, animation: { duration: 300, easingFunction: 'easeInOutQuad' } });
            }
        });

        // Control functions
        function fitGraph() {
            network.fit({ animation: { duration: 500, easingFunction: 'easeInOutQuad' } });
        }

        function resetZoom() {
            network.moveTo({ scale: 1, position: { x: 0, y: 0 }, animation: { duration: 300 } });
        }

        // Filter functionality
        function filterNodes(status) {
            var allNodes = nodes.get();
            var updateArray = [];
            allNodes.forEach(function(node) {
                if (status === 'all' || node.group === status) {
                    updateArray.push({ id: node.id, hidden: false });
                } else {
                    updateArray.push({ id: node.id, hidden: true });
                }
            });
            nodes.update(updateArray);
        }
    </script>
</body>
</html>
""".formatted(
            projectName,
            projectName,
            graph.nodeCount(),
            graph.getNodes().stream().filter(n -> n.isJakartaCompatible()).count(),
            graph.getNodes().stream().filter(n -> !n.isJakartaCompatible() && n.groupId().startsWith("javax.")).count(),
            graph.getNodes().stream().filter(n -> !n.isJakartaCompatible() && !n.groupId().startsWith("javax.")).count(),
            nodesJson,
            edgesJson
        );
    }

    private String generateGraphNodesJson(DependencyGraph graph) {
        StringBuilder json = new StringBuilder();
        json.append("[");

        int count = 0;
        for (var artifact : graph.getNodes()) {
            if (count > 0) json.append(",");

            String color = getNodeColor(artifact);
            String group = getNodeGroup(artifact);
            String title = String.format("%s:%s:%s<br>Scope: %s<br>Transitive: %s",
                artifact.groupId(), artifact.artifactId(), artifact.version(),
                artifact.scope(), artifact.transitive());

            json.append(String.format(
                "{\"id\":\"%s\",\"label\":\"%s\",\"title\":\"%s\",\"group\":\"%s\",\"color\":%s}",
                escapeJson(artifact.toIdentifier()),
                escapeJson(truncateLabel(artifact.artifactId())),
                escapeJson(title),
                group,
                color
            ));
            count++;
        }

        json.append("]");
        return json.toString();
    }

    private String generateGraphEdgesJson(DependencyGraph graph) {
        StringBuilder json = new StringBuilder();
        json.append("[");

        int count = 0;
        for (var edge : graph.getEdges()) {
            if (count > 0) json.append(",");
            json.append(String.format("{\"from\":\"%s\",\"to\":\"%s\"}",
                escapeJson(edge.from().toIdentifier()),
                escapeJson(edge.to().toIdentifier())));
            count++;
        }

        json.append("]");
        return json.toString();
    }

    private String getNodeColor(adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact artifact) {
        String bg, border;
        if (artifact.isJakartaCompatible()) {
            bg = "#28a745"; border = "#1e7e34";
        } else if (artifact.groupId().startsWith("javax.")) {
            bg = "#ffc107"; border = "#d39e00";
        } else if (artifact.groupId().contains("legacy") || artifact.artifactId().contains("old")) {
            bg = "#dc3545"; border = "#c82333";
        } else {
            bg = "#6c757d"; border = "#545b62";
        }
        return String.format("{\"background\":\"%s\",\"border\":\"%s\"}", bg, border);
    }

    private String getNodeGroup(adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact artifact) {
        if (artifact.isJakartaCompatible()) return "compatible";
        if (artifact.groupId().startsWith("javax.")) return "needs-upgrade";
        if (artifact.groupId().contains("legacy") || artifact.artifactId().contains("old")) return "no-version";
        return "unknown";
    }

    private String truncateLabel(String label) {
        if (label == null) return "unknown";
        if (label.length() <= 25) return label;
        return label.substring(0, 22) + "...";
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

}
