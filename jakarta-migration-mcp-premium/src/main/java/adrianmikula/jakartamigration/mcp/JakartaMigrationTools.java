package adrianmikula.jakartamigration.mcp;

import adrianmikula.jakartamigration.coderefactoring.domain.MigrationPlan;
import adrianmikula.jakartamigration.coderefactoring.service.MigrationPlanner;
import adrianmikula.jakartamigration.coderefactoring.service.RecipeLibrary;
import adrianmikula.jakartamigration.dependencyanalysis.domain.*;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyAnalysisModule;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphBuilder;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphException;
import adrianmikula.jakartamigration.api.service.StripePaymentLinkService;
import adrianmikula.jakartamigration.config.ApifyBillingService;
import adrianmikula.jakartamigration.config.FeatureFlag;
import adrianmikula.jakartamigration.config.FeatureFlagsService;
import adrianmikula.jakartamigration.config.FeatureFlagsProperties;
import adrianmikula.jakartamigration.runtimeverification.domain.VerificationOptions;
import adrianmikula.jakartamigration.runtimeverification.domain.VerificationResult;
import adrianmikula.jakartamigration.runtimeverification.service.RuntimeVerificationModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// CORRECTED: The annotations are in org.springaicommunity.mcp.annotation (SINGULAR, not plural)
// Verified by inspecting the JAR: org/springaicommunity/mcp/annotation/McpTool.class
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MCP Tools for Jakarta Migration.
 * Exposes Jakarta migration functionality as MCP tools that can be called by AI assistants.
 * 
 * Uses Spring AI 1.0.0 MCP Server annotations to expose tools via the MCP protocol.
 */
@Component
@Slf4j
public class JakartaMigrationTools {
    
    private final DependencyAnalysisModule dependencyAnalysisModule;
    private final DependencyGraphBuilder dependencyGraphBuilder;
    private final MigrationPlanner migrationPlanner;
    private final RecipeLibrary recipeLibrary;
    private final RuntimeVerificationModule runtimeVerificationModule;
    private final FeatureFlagsService featureFlags;
    @org.springframework.lang.Nullable
    private final ApifyBillingService apifyBillingService;
    @org.springframework.lang.Nullable
    private final StripePaymentLinkService paymentLinkService;
    private final adrianmikula.jakartamigration.sourcecodescanning.service.SourceCodeScanner sourceCodeScanner;
    private final adrianmikula.jakartamigration.coderefactoring.service.RefactoringEngine refactoringEngine;
    
    public JakartaMigrationTools(
            DependencyAnalysisModule dependencyAnalysisModule,
            DependencyGraphBuilder dependencyGraphBuilder,
            MigrationPlanner migrationPlanner,
            RecipeLibrary recipeLibrary,
            RuntimeVerificationModule runtimeVerificationModule,
            FeatureFlagsService featureFlags,
            @org.springframework.beans.factory.annotation.Autowired(required = false) 
            @org.springframework.lang.Nullable ApifyBillingService apifyBillingService,
            @org.springframework.beans.factory.annotation.Autowired(required = false) 
            @org.springframework.lang.Nullable StripePaymentLinkService paymentLinkService,
            adrianmikula.jakartamigration.sourcecodescanning.service.SourceCodeScanner sourceCodeScanner,
            adrianmikula.jakartamigration.coderefactoring.service.RefactoringEngine refactoringEngine) {
        this.dependencyAnalysisModule = dependencyAnalysisModule;
        this.dependencyGraphBuilder = dependencyGraphBuilder;
        this.migrationPlanner = migrationPlanner;
        this.recipeLibrary = recipeLibrary;
        this.runtimeVerificationModule = runtimeVerificationModule;
        this.featureFlags = featureFlags;
        this.apifyBillingService = apifyBillingService;
        this.paymentLinkService = paymentLinkService;
        this.sourceCodeScanner = sourceCodeScanner;
        this.refactoringEngine = refactoringEngine;
    }
    
    /**
     * Analyzes a Java project for Jakarta migration readiness.
     * 
     * @param projectPath Path to the project root directory
     * @return JSON string containing analysis report
     */
    @McpTool(
        name = "analyzeJakartaReadiness",
        description = "Analyzes a Java project for Jakarta migration readiness. Returns a JSON report with readiness score, blockers, and recommendations."
    )
    public String analyzeJakartaReadiness(
            @McpToolParam(description = "Path to the project root directory", required = true) String projectPath) {
        try {
            log.info("Analyzing Jakarta readiness for project: {}", projectPath);
            
            Path project = Paths.get(projectPath);
            if (!Files.exists(project) || !Files.isDirectory(project)) {
                return createErrorResponse("Project path does not exist or is not a directory: " + projectPath);
            }
            
            // Analyze project dependencies
            DependencyAnalysisReport report = dependencyAnalysisModule.analyzeProject(project);
            
            // Build response
            return buildReadinessResponse(report);
            
        } catch (DependencyGraphException e) {
            log.error("Failed to analyze project: {}", e.getMessage(), e);
            return createErrorResponse("Failed to analyze project: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during analysis", e);
            return createErrorResponse("Unexpected error: " + e.getMessage());
        }
    }
    
    /**
     * Detects blockers that prevent Jakarta migration.
     * 
     * @param projectPath Path to the project root directory
     * @return JSON string containing blockers list
     */
    @McpTool(
        name = "detectBlockers",
        description = "Detects blockers that prevent Jakarta migration. Returns a JSON list of blockers with types, reasons, and mitigation strategies."
    )
    public String detectBlockers(
            @McpToolParam(description = "Path to the project root directory", required = true) String projectPath) {
        try {
            log.info("Detecting blockers for project: {}", projectPath);
            
            Path project = Paths.get(projectPath);
            if (!Files.exists(project) || !Files.isDirectory(project)) {
                return createErrorResponse("Project path does not exist or is not a directory: " + projectPath);
            }
            
            // Build dependency graph
            DependencyGraph graph = dependencyGraphBuilder.buildFromProject(project);
            
            // Detect blockers
            List<Blocker> blockers = dependencyAnalysisModule.detectBlockers(graph);
            
            // Build response
            return buildBlockersResponse(blockers);
            
        } catch (DependencyGraphException e) {
            log.error("Failed to detect blockers: {}", e.getMessage(), e);
            return createErrorResponse("Failed to detect blockers: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during blocker detection", e);
            return createErrorResponse("Unexpected error: " + e.getMessage());
        }
    }
    
    /**
     * Recommends Jakarta-compatible versions for dependencies.
     * 
     * @param projectPath Path to the project root directory
     * @return JSON string containing version recommendations
     */
    @McpTool(
        name = "recommendVersions",
        description = "Recommends Jakarta-compatible versions for project dependencies. Returns a JSON list of version recommendations with migration paths and compatibility scores."
    )
    public String recommendVersions(
            @McpToolParam(description = "Path to the project root directory", required = true) String projectPath) {
        try {
            log.info("Recommending versions for project: {}", projectPath);
            
            Path project = Paths.get(projectPath);
            if (!Files.exists(project) || !Files.isDirectory(project)) {
                return createErrorResponse("Project path does not exist or is not a directory: " + projectPath);
            }
            
            // Build dependency graph
            DependencyGraph graph = dependencyGraphBuilder.buildFromProject(project);
            
            // Get all artifacts
            List<Artifact> artifacts = graph.getNodes().stream().collect(Collectors.toList());
            
            // Get recommendations
            List<VersionRecommendation> recommendations = dependencyAnalysisModule.recommendVersions(artifacts);
            
            // Build response
            return buildRecommendationsResponse(recommendations);
            
        } catch (DependencyGraphException e) {
            log.error("Failed to recommend versions: {}", e.getMessage(), e);
            return createErrorResponse("Failed to recommend versions: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during version recommendation", e);
            return createErrorResponse("Unexpected error: " + e.getMessage());
        }
    }
    
    /**
     * Creates a migration plan for Jakarta migration.
     * 
     * @param projectPath Path to the project root directory
     * @return JSON string containing migration plan
     */
    @McpTool(
        name = "createMigrationPlan",
        description = "Creates a comprehensive migration plan for Jakarta migration. Returns a JSON plan with phases, estimated duration, and risk assessment. Requires PREMIUM license - automated planning tool."
    )
    public String createMigrationPlan(
            @McpToolParam(description = "Path to the project root directory", required = true) String projectPath) {
        try {
            log.info("Creating migration plan for project: {}", projectPath);
            
            // Check if user has required tier (PREMIUM or ENTERPRISE)
            if (!featureFlags.hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM)) {
                return createUpgradeRequiredResponse(
                    FeatureFlag.ONE_CLICK_REFACTOR,
                    "The 'createMigrationPlan' tool requires a PREMIUM license. This tool creates comprehensive migration plans with detailed phases and risk assessment."
                );
            }
            
            Path project = Paths.get(projectPath);
            if (!Files.exists(project) || !Files.isDirectory(project)) {
                return createErrorResponse("Project path does not exist or is not a directory: " + projectPath);
            }
            
            // Analyze project first
            DependencyAnalysisReport report = dependencyAnalysisModule.analyzeProject(project);
            
            // Create migration plan
            MigrationPlan plan = migrationPlanner.createPlan(projectPath, report);
            
            // Build response
            return buildMigrationPlanResponse(plan);
            
        } catch (DependencyGraphException e) {
            log.error("Failed to create migration plan: {}", e.getMessage(), e);
            return createErrorResponse("Failed to create migration plan: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during migration plan creation", e);
            return createErrorResponse("Unexpected error: " + e.getMessage());
        }
    }
    
    /**
     * Analyzes full migration impact combining dependency analysis and source code scanning.
     * 
     * @param projectPath Path to the project root directory
     * @return JSON string containing comprehensive migration impact summary
     */
    @McpTool(
        name = "analyzeMigrationImpact",
        description = "Analyzes full migration impact combining dependency analysis and source code scanning. Returns a comprehensive summary with file counts, import counts, blockers, and estimated effort. FREE tool - analysis only."
    )
    public String analyzeMigrationImpact(
            @McpToolParam(description = "Path to the project root directory", required = true) String projectPath) {
        try {
            log.info("Analyzing migration impact for project: {}", projectPath);
            
            // FREE tool - no license check required (analysis only)
            
            Path project = Paths.get(projectPath);
            if (!Files.exists(project) || !Files.isDirectory(project)) {
                return createErrorResponse("Project path does not exist or is not a directory: " + projectPath);
            }
            
            // Run dependency analysis
            DependencyAnalysisReport depReport = dependencyAnalysisModule.analyzeProject(project);
            
            // Run source code scan
            adrianmikula.jakartamigration.sourcecodescanning.domain.SourceCodeAnalysisResult scanResult = 
                sourceCodeScanner.scanProject(project);
            
            // Create impact summary
            adrianmikula.jakartamigration.coderefactoring.domain.MigrationImpactSummary summary = 
                adrianmikula.jakartamigration.coderefactoring.domain.MigrationImpactSummary.from(depReport, scanResult);
            
            // Build response
            return buildImpactSummaryResponse(summary);
            
        } catch (Exception e) {
            log.error("Unexpected error during migration impact analysis", e);
            return createErrorResponse("Unexpected error: " + e.getMessage());
        }
    }
    
    /**
     * Refactors Java source files from javax.* to jakarta.* namespace.
     * This is an automated refactoring tool that modifies source code files.
     * 
     * @param projectPath Path to the project root directory
     * @param filePaths Optional list of specific file paths to refactor (relative to project root). If not provided, all Java and XML files will be refactored.
     * @return JSON string containing refactoring result
     */
    @McpTool(
        name = "refactorProject",
        description = "Automatically refactors Java source files from javax.* to jakarta.* namespace. Modifies source code files directly. Requires PREMIUM license - automated refactoring tool."
    )
    public String refactorProject(
            @McpToolParam(description = "Path to the project root directory", required = true) String projectPath,
            @McpToolParam(description = "Optional list of specific file paths to refactor (relative to project root). If not provided, all Java and XML files will be refactored.", required = false) List<String> filePaths) {
        try {
            log.info("Refactoring project: {}", projectPath);
            
            // Check if user has required tier (PREMIUM or ENTERPRISE)
            if (!featureFlags.hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM)) {
                return createUpgradeRequiredResponse(
                    FeatureFlag.ONE_CLICK_REFACTOR,
                    "The 'refactorProject' tool requires a PREMIUM license. This tool automatically refactors source code files from javax.* to jakarta.* namespace."
                );
            }
            
            Path project = Paths.get(projectPath);
            if (!Files.exists(project) || !Files.isDirectory(project)) {
                return createErrorResponse("Project path does not exist or is not a directory: " + projectPath);
            }
            
            // Get default recipes
            List<adrianmikula.jakartamigration.coderefactoring.domain.Recipe> recipes = List.of(
                adrianmikula.jakartamigration.coderefactoring.domain.Recipe.jakartaNamespaceRecipe(),
                adrianmikula.jakartamigration.coderefactoring.domain.Recipe.persistenceXmlRecipe(),
                adrianmikula.jakartamigration.coderefactoring.domain.Recipe.webXmlRecipe()
            );
            
            // Discover files if not provided
            List<Path> filesToRefactor;
            if (filePaths == null || filePaths.isEmpty()) {
                filesToRefactor = discoverRefactorableFiles(project);
            } else {
                filesToRefactor = filePaths.stream()
                    .map(project::resolve)
                    .filter(Files::exists)
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toList());
            }
            
            if (filesToRefactor.isEmpty()) {
                return createErrorResponse("No files found to refactor in project: " + projectPath);
            }
            
            // Refactor files
            List<String> refactoredFiles = new ArrayList<>();
            List<String> failedFiles = new ArrayList<>();
            int totalChanges = 0;
            
            for (Path filePath : filesToRefactor) {
                try {
                    adrianmikula.jakartamigration.coderefactoring.domain.RefactoringChanges changes = 
                        refactoringEngine.refactorFile(filePath, recipes);
                    
                    if (changes.hasChanges()) {
                        // Write refactored content back to file
                        Files.writeString(filePath, changes.refactoredContent());
                        refactoredFiles.add(project.relativize(filePath).toString());
                        totalChanges += changes.changeCount();
                        log.info("Refactored file: {} ({} changes)", filePath, changes.changeCount());
                    } else {
                        refactoredFiles.add(project.relativize(filePath).toString());
                        log.debug("No changes needed for file: {}", filePath);
                    }
                } catch (Exception e) {
                    log.error("Failed to refactor file: {}", filePath, e);
                    failedFiles.add(project.relativize(filePath).toString());
                }
            }
            
            // Build response
            return buildRefactoringResponse(refactoredFiles, failedFiles, totalChanges, filesToRefactor.size());
            
        } catch (Exception e) {
            log.error("Unexpected error during refactoring", e);
            return createErrorResponse("Unexpected error: " + e.getMessage());
        }
    }
    
    /**
     * Discovers all refactorable files (Java and XML) in the project.
     */
    private List<Path> discoverRefactorableFiles(Path projectPath) {
        List<Path> files = new ArrayList<>();
        
        try (java.util.stream.Stream<Path> paths = Files.walk(projectPath)) {
            paths
                .filter(Files::isRegularFile)
                .filter(p -> {
                    String path = p.toString().replace('\\', '/');
                    // Include Java and XML files
                    boolean isRelevant = p.toString().endsWith(".java") ||
                                       p.toString().endsWith(".xml");
                    // Exclude build directories
                    boolean notExcluded = !path.contains("/target/") &&
                                         !path.contains("/build/") &&
                                         !path.contains("/.git/") &&
                                         !path.contains("/node_modules/") &&
                                         !path.contains("/.gradle/") &&
                                         !path.contains("/.mvn/") &&
                                         !path.contains("/.idea/") &&
                                         !path.contains("/.vscode/") &&
                                         !path.contains("/out/") &&
                                         !path.contains("/bin/");
                    return isRelevant && notExcluded;
                })
                .forEach(files::add);
        } catch (IOException e) {
            log.error("Error discovering files in: {}", projectPath, e);
        }
        
        return files;
    }
    
    /**
     * Verifies runtime execution of a migrated application.
     * 
     * @param jarPath Path to the JAR file to execute
     * @param timeoutSeconds Optional timeout in seconds (default: 30)
     * @return JSON string containing verification result
     */
    @McpTool(
        name = "verifyRuntime",
        description = "Verifies runtime execution of a migrated Jakarta application. Returns a JSON result with execution status, errors, and metrics. Requires PREMIUM license - automated verification tool."
    )
    public String verifyRuntime(
            @McpToolParam(description = "Path to the JAR file to execute", required = true) String jarPath,
            @McpToolParam(description = "Optional timeout in seconds (default: 30)", required = false) Integer timeoutSeconds) {
        try {
            log.info("Verifying runtime for JAR: {}", jarPath);
            
            // Check if user has required tier (PREMIUM or ENTERPRISE)
            if (!featureFlags.hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM)) {
                return createUpgradeRequiredResponse(
                    FeatureFlag.BINARY_FIXES,
                    "The 'verifyRuntime' tool requires a PREMIUM license. This tool verifies runtime execution of migrated Jakarta applications."
                );
            }
            
            Path jar = Paths.get(jarPath);
            if (!Files.exists(jar) || !Files.isRegularFile(jar)) {
                return createErrorResponse("JAR file does not exist or is not a file: " + jarPath);
            }
            
            // Create verification options
            VerificationOptions options = timeoutSeconds != null 
                ? new VerificationOptions(
                    java.time.Duration.ofSeconds(timeoutSeconds),
                    VerificationOptions.defaults().maxMemoryBytes(),
                    VerificationOptions.defaults().captureStdout(),
                    VerificationOptions.defaults().captureStderr(),
                    VerificationOptions.defaults().jvmArgs()
                )
                : VerificationOptions.defaults();
            
            // Verify runtime
            VerificationResult result = runtimeVerificationModule.verifyRuntime(jar, options);
            
            // Build response
            return buildVerificationResponse(result);
            
        } catch (Exception e) {
            log.error("Unexpected error during runtime verification", e);
            return createErrorResponse("Unexpected error: " + e.getMessage());
        }
    }
    
    // Helper methods to build JSON responses
    
    private String buildReadinessResponse(DependencyAnalysisReport report) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"status\": \"success\",\n");
        json.append("  \"readinessScore\": ").append(report.readinessScore().score()).append(",\n");
        json.append("  \"readinessMessage\": \"").append(escapeJson(report.readinessScore().explanation())).append("\",\n");
        json.append("  \"totalDependencies\": ").append(report.dependencyGraph().nodeCount()).append(",\n");
        json.append("  \"blockers\": ").append(report.blockers().size()).append(",\n");
        json.append("  \"recommendations\": ").append(report.recommendations().size()).append(",\n");
        json.append("  \"riskScore\": ").append(report.riskAssessment().riskScore()).append(",\n");
        json.append("  \"riskFactors\": ").append(buildStringArray(report.riskAssessment().riskFactors())).append("\n");
        json.append("}");
        return json.toString();
    }
    
    private String buildBlockersResponse(List<Blocker> blockers) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"status\": \"success\",\n");
        json.append("  \"blockerCount\": ").append(blockers.size()).append(",\n");
        json.append("  \"blockers\": [\n");
        for (int i = 0; i < blockers.size(); i++) {
            Blocker blocker = blockers.get(i);
            json.append("    {\n");
            json.append("      \"artifact\": \"").append(escapeJson(blocker.artifact().toString())).append("\",\n");
            json.append("      \"type\": \"").append(blocker.type()).append("\",\n");
            json.append("      \"reason\": \"").append(escapeJson(blocker.reason())).append("\",\n");
            json.append("      \"confidence\": ").append(blocker.confidence()).append(",\n");
            json.append("      \"mitigationStrategies\": ").append(buildStringArray(blocker.mitigationStrategies())).append("\n");
            json.append("    }");
            if (i < blockers.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        json.append("  ]");
        
        // Add premium feature recommendation if blockers found
        if (blockers.size() > 0) {
            json.append(",\n");
            json.append("  \"premiumFeatures\": {\n");
            json.append("    \"recommended\": true,\n");
            json.append("    \"message\": \"Premium Automated Refactoring can automatically resolve many blockers without manual intervention.\",\n");
            json.append("    \"features\": [\n");
            json.append("      \"Automated Refactoring - Automatically fix detected blockers and migrate code\",\n");
            json.append("      \"Runtime Verification - Verify migrated applications work correctly\",\n");
            json.append("      \"Batch Operations - Process multiple projects simultaneously\"\n");
            json.append("    ]");
            String premiumLink = getPremiumPaymentLink();
            if (premiumLink != null) {
                json.append(",\n");
                json.append("    \"pricingUrl\": \"").append(escapeJson(premiumLink)).append("\"");
            }
            json.append("\n  }");
        }
        
        json.append("\n}");
        return json.toString();
    }
    
    private String buildRecommendationsResponse(List<VersionRecommendation> recommendations) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"status\": \"success\",\n");
        json.append("  \"recommendationCount\": ").append(recommendations.size()).append(",\n");
        json.append("  \"recommendations\": [\n");
        for (int i = 0; i < recommendations.size(); i++) {
            VersionRecommendation rec = recommendations.get(i);
            json.append("    {\n");
            json.append("      \"current\": \"").append(escapeJson(rec.currentArtifact().toString())).append("\",\n");
            json.append("      \"recommended\": \"").append(escapeJson(rec.recommendedArtifact().toString())).append("\",\n");
            json.append("      \"migrationPath\": \"").append(escapeJson(rec.migrationPath())).append("\",\n");
            json.append("      \"compatibilityScore\": ").append(rec.compatibilityScore()).append(",\n");
            json.append("      \"breakingChanges\": ").append(buildStringArray(rec.breakingChanges())).append("\n");
            json.append("    }");
            if (i < recommendations.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        json.append("  ]");
        
        // Add premium feature recommendation if many recommendations or complex migrations
        boolean hasBreakingChanges = recommendations.stream()
            .anyMatch(rec -> !rec.breakingChanges().isEmpty());
        boolean shouldRecommend = recommendations.size() > 5 || hasBreakingChanges;
        
        if (shouldRecommend) {
            json.append(",\n");
            json.append("  \"premiumFeatures\": {\n");
            json.append("    \"recommended\": true,\n");
            json.append("    \"message\": \"Premium Automated Refactoring can automatically apply these ").append(recommendations.size()).append(" version recommendations and handle breaking changes.\",\n");
            json.append("    \"features\": [\n");
            json.append("      \"Automated Refactoring - Automatically apply version recommendations and migrate code\",\n");
            json.append("      \"Runtime Verification - Verify migrated applications work correctly\",\n");
            json.append("      \"Batch Operations - Process multiple projects simultaneously\"\n");
            json.append("    ]");
            String premiumLink = getPremiumPaymentLink();
            if (premiumLink != null) {
                json.append(",\n");
                json.append("    \"pricingUrl\": \"").append(escapeJson(premiumLink)).append("\"");
            }
            json.append("\n  }");
        }
        
        json.append("\n}");
        return json.toString();
    }
    
    private String buildMigrationPlanResponse(MigrationPlan plan) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"status\": \"success\",\n");
        json.append("  \"phaseCount\": ").append(plan.phases().size()).append(",\n");
        json.append("  \"estimatedDuration\": \"").append(plan.estimatedDuration().toMinutes()).append(" minutes\",\n");
        json.append("  \"riskScore\": ").append(plan.overallRisk().riskScore()).append(",\n");
        json.append("  \"riskFactors\": ").append(buildStringArray(plan.overallRisk().riskFactors())).append(",\n");
        json.append("  \"prerequisites\": ").append(buildStringArray(plan.prerequisites())).append(",\n");
        json.append("  \"phases\": [\n");
        for (int i = 0; i < plan.phases().size(); i++) {
            var phase = plan.phases().get(i);
            json.append("    {\n");
            json.append("      \"number\": ").append(phase.phaseNumber()).append(",\n");
            json.append("      \"description\": \"").append(escapeJson(phase.description())).append("\",\n");
            json.append("      \"fileCount\": ").append(phase.files().size()).append(",\n");
            json.append("      \"estimatedDuration\": \"").append(phase.estimatedDuration().toMinutes()).append(" minutes\"\n");
            json.append("    }");
            if (i < plan.phases().size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        json.append("  ]");
        
        // Add premium feature recommendation for migration plans
        long totalMinutes = plan.estimatedDuration().toMinutes();
        boolean shouldRecommend = totalMinutes > 30 || plan.phases().size() > 5 || plan.overallRisk().riskScore() > 0.3;
        
        if (shouldRecommend) {
            json.append(",\n");
            json.append("  \"premiumFeatures\": {\n");
            json.append("    \"recommended\": true,\n");
            json.append("    \"message\": \"Premium Automated Refactoring can execute this entire migration plan automatically, saving you ").append(totalMinutes).append(" minutes of manual work.\",\n");
            json.append("    \"features\": [\n");
            json.append("      \"Automated Refactoring - Execute complete migration automatically\",\n");
            json.append("      \"Runtime Verification - Verify migrated applications work correctly\",\n");
            json.append("      \"Batch Operations - Process multiple projects simultaneously\"\n");
            json.append("    ]");
            String premiumLink = getPremiumPaymentLink();
            if (premiumLink != null) {
                json.append(",\n");
                json.append("    \"pricingUrl\": \"").append(escapeJson(premiumLink)).append("\"");
            }
            json.append(",\n");
            json.append("    \"estimatedSavings\": \"").append(totalMinutes).append(" minutes of manual work\"\n");
            json.append("  }");
        }
        
        json.append("\n}");
        return json.toString();
    }
    
    private String buildVerificationResponse(VerificationResult result) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"status\": \"").append(result.status()).append("\",\n");
        json.append("  \"errorCount\": ").append(result.errors().size()).append(",\n");
        json.append("  \"warningCount\": ").append(result.warnings().size()).append(",\n");
        json.append("  \"executionTime\": \"").append(result.metrics().executionTime().toSeconds()).append(" seconds\",\n");
        json.append("  \"exitCode\": ").append(result.metrics().exitCode()).append(",\n");
        if (result.errors().isEmpty()) {
            json.append("  \"message\": \"Runtime verification passed\"\n");
        } else {
            json.append("  \"message\": \"Runtime verification found issues\",\n");
            json.append("  \"errors\": [\n");
            for (int i = 0; i < Math.min(result.errors().size(), 5); i++) {
                var error = result.errors().get(i);
                json.append("    \"").append(escapeJson(error.message())).append("\"");
                if (i < Math.min(result.errors().size(), 5) - 1) {
                    json.append(",");
                }
                json.append("\n");
            }
            json.append("  ]\n");
        }
        json.append("}");
        return json.toString();
    }
    
    private String buildSourceCodeResponse(adrianmikula.jakartamigration.sourcecodescanning.domain.SourceCodeAnalysisResult result) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"status\": \"success\",\n");
        json.append("  \"totalFilesScanned\": ").append(result.totalFilesScanned()).append(",\n");
        json.append("  \"totalFilesWithJavaxUsage\": ").append(result.totalFilesWithJavaxUsage()).append(",\n");
        json.append("  \"totalJavaxImports\": ").append(result.totalJavaxImports()).append(",\n");
        json.append("  \"filesWithJavaxUsage\": [\n");
        
        for (int i = 0; i < result.filesWithJavaxUsage().size(); i++) {
            adrianmikula.jakartamigration.sourcecodescanning.domain.FileUsage fileUsage = result.filesWithJavaxUsage().get(i);
            json.append("    {\n");
            json.append("      \"filePath\": \"").append(escapeJson(fileUsage.filePath().toString())).append("\",\n");
            json.append("      \"lineCount\": ").append(fileUsage.lineCount()).append(",\n");
            json.append("      \"javaxImportCount\": ").append(fileUsage.getJavaxImportCount()).append(",\n");
            json.append("      \"imports\": [\n");
            
            for (int j = 0; j < fileUsage.javaxImports().size(); j++) {
                adrianmikula.jakartamigration.sourcecodescanning.domain.ImportStatement imp = fileUsage.javaxImports().get(j);
                json.append("        {\n");
                json.append("          \"fullImport\": \"").append(escapeJson(imp.fullImport())).append("\",\n");
                json.append("          \"javaxPackage\": \"").append(escapeJson(imp.javaxPackage())).append("\",\n");
                json.append("          \"jakartaEquivalent\": \"").append(escapeJson(imp.jakartaEquivalent())).append("\",\n");
                json.append("          \"lineNumber\": ").append(imp.lineNumber()).append("\n");
                json.append("        }");
                if (j < fileUsage.javaxImports().size() - 1) {
                    json.append(",");
                }
                json.append("\n");
            }
            
            json.append("      ]\n");
            json.append("    }");
            if (i < result.filesWithJavaxUsage().size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        
        json.append("  ]\n");
        json.append("}");
        return json.toString();
    }
    
    private String buildImpactSummaryResponse(adrianmikula.jakartamigration.coderefactoring.domain.MigrationImpactSummary summary) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"status\": \"success\",\n");
        json.append("  \"totalFilesToMigrate\": ").append(summary.totalFilesToMigrate()).append(",\n");
        json.append("  \"totalJavaxImports\": ").append(summary.totalJavaxImports()).append(",\n");
        json.append("  \"totalBlockers\": ").append(summary.totalBlockers()).append(",\n");
        json.append("  \"totalRecommendations\": ").append(summary.totalRecommendations()).append(",\n");
        json.append("  \"estimatedEffortMinutes\": ").append(summary.estimatedEffort().toMinutes()).append(",\n");
        json.append("  \"complexity\": \"").append(summary.complexity()).append("\",\n");
        json.append("  \"riskScore\": ").append(summary.overallRisk().riskScore()).append(",\n");
        json.append("  \"riskFactors\": ").append(buildStringArray(summary.overallRisk().riskFactors())).append(",\n");
        json.append("  \"readinessScore\": ").append(summary.dependencyAnalysis().readinessScore().score()).append(",\n");
        json.append("  \"readinessMessage\": \"").append(escapeJson(summary.dependencyAnalysis().readinessScore().explanation())).append("\",\n");
        json.append("  \"totalFilesScanned\": ").append(summary.sourceCodeAnalysis().totalFilesScanned()).append(",\n");
        json.append("  \"totalDependencies\": ").append(summary.dependencyAnalysis().dependencyGraph().nodeCount());
        
        // Add premium feature recommendation based on complexity/effort
        long effortMinutes = summary.estimatedEffort().toMinutes();
        boolean shouldRecommend = summary.complexity() == adrianmikula.jakartamigration.coderefactoring.domain.MigrationImpactSummary.MigrationComplexity.HIGH ||
                                  effortMinutes > 60 ||
                                  summary.totalBlockers() > 3 ||
                                  summary.totalFilesToMigrate() > 20;
        
        if (shouldRecommend) {
            json.append(",\n");
            json.append("  \"premiumFeatures\": {\n");
            json.append("    \"recommended\": true,\n");
            json.append("    \"message\": \"This migration has ").append(summary.complexity()).append(" complexity and will take approximately ").append(effortMinutes).append(" minutes. Premium Automated Refactoring can automate most of this work.\",\n");
            json.append("    \"features\": [\n");
            json.append("      \"Automated Refactoring - Execute complete migration automatically\",\n");
            json.append("      \"Runtime Verification - Verify migrated applications work correctly\",\n");
            json.append("      \"Batch Operations - Process multiple projects simultaneously\"\n");
            json.append("    ]");
            String premiumLink = getPremiumPaymentLink();
            if (premiumLink != null) {
                json.append(",\n");
                json.append("    \"pricingUrl\": \"").append(escapeJson(premiumLink)).append("\"");
            }
            json.append(",\n");
            json.append("    \"estimatedSavings\": \"").append(effortMinutes).append(" minutes of manual work\"\n");
            json.append("  }");
        }
        
        json.append("\n}");
        return json.toString();
    }
    
    private String buildRefactoringResponse(List<String> refactoredFiles, List<String> failedFiles, int totalChanges, int totalFiles) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"status\": \"").append(failedFiles.isEmpty() ? "success" : "partial_success").append("\",\n");
        json.append("  \"totalFiles\": ").append(totalFiles).append(",\n");
        json.append("  \"refactoredFiles\": ").append(refactoredFiles.size()).append(",\n");
        json.append("  \"failedFiles\": ").append(failedFiles.size()).append(",\n");
        json.append("  \"totalChanges\": ").append(totalChanges).append(",\n");
        json.append("  \"refactoredFileList\": ").append(buildStringArray(refactoredFiles));
        
        if (!failedFiles.isEmpty()) {
            json.append(",\n");
            json.append("  \"failedFileList\": ").append(buildStringArray(failedFiles));
        }
        
        json.append("\n}");
        return json.toString();
    }
    
    private String createErrorResponse(String message) {
        return "{\n" +
               "  \"status\": \"error\",\n" +
               "  \"message\": \"" + escapeJson(message) + "\"\n" +
               "}";
    }
    
    private String createUpgradeRequiredResponse(FeatureFlag flag, String message) {
        FeatureFlagsService.UpgradeInfo upgradeInfo = featureFlags.getUpgradeInfo(flag);
        FeatureFlagsProperties.LicenseTier currentTier = featureFlags.getCurrentTier();
        // Defensive: use flag fields when upgradeInfo is null (e.g. in tests with mocked FeatureFlagsService)
        String featureName = upgradeInfo != null ? upgradeInfo.getFeatureName() : flag.getName();
        String featureDescription = upgradeInfo != null ? upgradeInfo.getFeatureDescription() : flag.getDescription();
        FeatureFlagsProperties.LicenseTier requiredTier = upgradeInfo != null ? upgradeInfo.getRequiredTier() : flag.getRequiredTier();
        String paymentLink = upgradeInfo != null ? upgradeInfo.getPaymentLink() : null;
        String upgradeMessage = upgradeInfo != null ? upgradeInfo.getMessage() : message;

        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"status\": \"upgrade_required\",\n");
        json.append("  \"message\": \"").append(escapeJson(message)).append("\",\n");
        json.append("  \"featureName\": \"").append(escapeJson(featureName)).append("\",\n");
        json.append("  \"featureDescription\": \"").append(escapeJson(featureDescription)).append("\",\n");
        json.append("  \"currentTier\": \"").append(currentTier).append("\",\n");
        json.append("  \"requiredTier\": \"").append(requiredTier).append("\",\n");

        if (paymentLink != null && !paymentLink.isBlank()) {
            json.append("  \"paymentLink\": \"").append(escapeJson(paymentLink)).append("\",\n");
        }

        // Get all available payment links
        if (paymentLinkService != null) {
            java.util.Map<String, String> allPaymentLinks = paymentLinkService.getAllPaymentLinks();
            if (!allPaymentLinks.isEmpty()) {
                json.append("  \"availablePlans\": {\n");
                boolean first = true;
                for (java.util.Map.Entry<String, String> entry : allPaymentLinks.entrySet()) {
                    if (!first) {
                        json.append(",\n");
                    }
                    json.append("    \"").append(escapeJson(entry.getKey())).append("\": \"")
                        .append(escapeJson(entry.getValue())).append("\"");
                    first = false;
                }
                json.append("\n  },\n");
            }
        }

        json.append("  \"upgradeMessage\": \"").append(escapeJson(upgradeMessage)).append("\"\n");
        json.append("}");

        return json.toString();
    }
    
    private String buildStringArray(List<String> list) {
        if (list.isEmpty()) {
            return "[]";
        }
        return "[" + list.stream()
            .map(s -> "\"" + escapeJson(s) + "\"")
            .collect(Collectors.joining(", ")) + "]";
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
    
    /**
     * Gets the premium payment link from Stripe service, if available.
     * Falls back to null if service is not configured.
     */
    private String getPremiumPaymentLink() {
        if (paymentLinkService != null) {
            // Try "premium" first, then "professional" as fallback
            String link = paymentLinkService.getPaymentLink("premium");
            if (link == null || link.isBlank()) {
                link = paymentLinkService.getPaymentLink("professional");
            }
            return link;
        }
        return null;
    }
}

