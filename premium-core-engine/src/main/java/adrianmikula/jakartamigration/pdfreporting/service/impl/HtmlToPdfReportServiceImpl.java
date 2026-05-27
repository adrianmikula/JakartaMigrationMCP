package adrianmikula.jakartamigration.pdfreporting.service.impl;

import adrianmikula.jakartamigration.pdfreporting.service.PdfReportService;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.risk.RiskScoringService;
import adrianmikula.jakartamigration.pdfreporting.snippet.RiskAnalysisSnippetFactory;
import adrianmikula.jakartamigration.pdfreporting.snippet.RefactoringSnippetFactory;
import adrianmikula.jakartamigration.pdfreporting.snippet.ReportAssembler;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * HTML report generation service.
 * PDF generation disabled due to memory issues - provides HTML-only reports.
 * See docs/techdebt/pdf-memory-issues.md for details.
 */
@Slf4j
public class HtmlToPdfReportServiceImpl implements PdfReportService {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final CssCacheManager cssCacheManager;
    private final HtmlUtils htmlUtils;
    private final MigrationStrategyProvider migrationStrategyProvider;
    private final ReportCssGenerator cssGenerator;
    
    public HtmlToPdfReportServiceImpl() {
        log.info("Initializing HTML Report Service (PDF generation disabled due to memory issues)");
        this.cssCacheManager = new CssCacheManager();
        this.htmlUtils = new HtmlUtils();
        this.migrationStrategyProvider = new MigrationStrategyProvider();
        this.cssGenerator = new ReportCssGenerator();
    }
    
    @Override
    public Path generateRiskAnalysisReport(RiskAnalysisReportRequest request) {
        log.info("Generating Risk Analysis HTML report: {}", request.outputPath());
        
        try {
            // Generate HTML content using snippet-based architecture
            String htmlContent = generateRiskAnalysisHtmlWithSnippets(request);
            
            // Log HTML content size for memory monitoring
            log.info("HTML content size: {} bytes ({} MB)", htmlContent.length(), htmlContent.length() / (1024.0 * 1024.0));
            
            // Save HTML directly (PDF generation disabled due to memory issues)
            Path htmlPath = saveHtmlReport(htmlContent, request.outputPath());
            
            return htmlPath;
        } catch (Exception e) {
            log.error("Error generating risk analysis report", e);
            throw new RuntimeException("Failed to generate risk analysis report", e);
        }
    }
    
    @Override
    public Path generateRefactoringActionReport(RefactoringActionReportRequest request) {
        log.info("Generating Refactoring Action HTML report: {}", request.outputPath());

        try {
            // Generate HTML content using new snippet-based architecture with real scan data
            String htmlContent = generateRefactoringActionHtmlWithSnippets(request);

            // Log HTML content size for memory monitoring
            log.info("HTML content size: {} bytes ({} MB)", htmlContent.length(), htmlContent.length() / (1024.0 * 1024.0));

            // Save HTML directly (PDF generation disabled due to memory issues)
            Path htmlPath = saveHtmlReport(htmlContent, request.outputPath());

            return htmlPath;
        } catch (Exception e) {
            log.error("Error generating refactoring action report", e);
            throw new RuntimeException("Failed to generate refactoring action report", e);
        }
    }
    
    @Override
    public Path generateConsolidatedReport(ConsolidatedReportRequest request) {
        log.info("Generating Consolidated HTML report: {}", request.outputPath());
        
        try {
            // Generate HTML content with modern professional styling
            String htmlContent = generateConsolidatedHtml(request);
            
            // Log HTML content size for memory monitoring
            log.info("HTML content size: {} bytes ({} MB)", htmlContent.length(), htmlContent.length() / (1024.0 * 1024.0));
            
            // Save HTML directly (PDF generation disabled due to memory issues)
            Path htmlPath = saveHtmlReport(htmlContent, request.outputPath());
            
            return htmlPath;
        } catch (Exception e) {
            log.error("Error generating consolidated report", e);
            throw new RuntimeException("Failed to generate consolidated report", e);
        }
    }
    
    @Override
    public ValidationResult validateReportRequest(GeneratePdfReportRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        
        if (request.outputPath() == null) {
            errors.add(new ValidationError("outputPath", "Output path cannot be null", "Provide a valid output path"));
        }
        
        if (request.template() == null) {
            errors.add(new ValidationError("template", "Template cannot be null", "Select a valid template"));
        }
        
        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(errors);
    }
    
    @Override
    public ReportTemplate getDefaultTemplate() {
        List<ReportSection> sections = Arrays.asList(
            new ReportSection("executiveSummary", "Executive Summary", "High-level overview", true, Map.of()),
            new ReportSection("riskAssessment", "Risk Assessment", "Risk analysis and scoring", true, Map.of()),
            new ReportSection("topBlockers", "Top Blockers", "Critical issues blocking migration", true, Map.of()),
            new ReportSection("recommendations", "Recommendations", "Action items for migration", true, Map.of()),
            new ReportSection("dependencyAnalysis", "Dependency Analysis", "Dependencies requiring updates", true, Map.of()),
            new ReportSection("scanResults", "Scan Results", "Detailed scan findings", true, Map.of()),
            new ReportSection("detailedFindings", "Detailed Findings", "Technical analysis details", true, Map.of())
        );
        
        return new ReportTemplate(
            "Professional HTML-to-PDF",
            "Professional HTML-to-PDF report template using Thymeleaf and OpenHTMLtoPDF",
            sections,
            Map.of("version", "3.0", "created", LocalDateTime.now().toString(), "engine", "OpenHTMLtoPDF + Thymeleaf", 
                   "format", "html-to-pdf", "professional", true)
        );
    }
    
    @Override
    public ReportTemplate createCustomTemplate(List<ReportSection> sections) {
        return new ReportTemplate(
            "Custom HTML-to-PDF",
            "Custom HTML-to-PDF report template",
            sections,
            Map.of("version", "3.0", "created", LocalDateTime.now().toString(), "engine", "OpenHTMLtoPDF + Thymeleaf", 
                   "format", "html-to-pdf", "custom", true)
        );
    }
    
    /**
     * Public method for HTML escaping (for backward compatibility with tests).
     * Delegates to HtmlUtils.
     */
    public String escapeHtml(String text) {
        return htmlUtils.escapeHtml(text);
    }
    
    /**
     * Public method for getting plugin icon SVG (for backward compatibility with tests).
     * Delegates to HtmlUtils.
     */
    public String getPluginIconSvg() {
        return htmlUtils.getPluginIconSvg();
    }
    
    /**
     * Public method for getting shared header styles (for backward compatibility with tests).
     * Delegates to ReportCssGenerator.
     */
    public String getSharedHeaderStyles() {
        return cssGenerator.getSharedHeaderStyles();
    }
    
    /**
     * Public method for getting shared footer styles (for backward compatibility with tests).
     * Delegates to ReportCssGenerator.
     */
    public String getSharedFooterStyles() {
        return cssGenerator.getSharedFooterStyles();
    }
    
    /**
     * Public method for getting inline common styles (for backward compatibility with tests).
     * Delegates to ReportCssGenerator.
     */
    public String getInlineCommonStyles() {
        return cssGenerator.getInlineCommonStyles();
    }
    
    /**
     * Public method for getting inline refactoring styles (for backward compatibility with tests).
     * Delegates to ReportCssGenerator.
     */
    public String getInlineRefactoringStyles() {
        return cssGenerator.getInlineRefactoringStyles();
    }
    
    /**
     * Generate HTML content using the new snippet-based architecture.
     * This method replaces the monolithic template approach.
     */
    private String generateRiskAnalysisHtmlWithSnippets(RiskAnalysisReportRequest request) {
        log.info("Generating HTML using snippet-based architecture");
        
        try {
            // Calculate risk score if not provided
            RiskScoringService.RiskScore calculatedRiskScore = request.riskScore();
            if (calculatedRiskScore == null) {
                log.info("Risk score not provided, calculating from scan results and dependency graph");
                RiskScoringService riskScoringService = RiskScoringService.getInstance();
                
                // Calculate simple metrics for risk scoring
                int totalIssues = request.scanResults() != null ? request.scanResults().totalIssuesFound() : 0;
                int nonCompatibleDeps = 0;
                int blockers = 0;
                
                if (request.dependencyGraph() != null && request.dependencyGraph().getEdges() != null) {
                    for (var dep : request.dependencyGraph().getEdges()) {
                        if (!dep.to().isJakartaCompatible()) {
                            nonCompatibleDeps++;
                        }
                    }
                }
                
                // Use the simpler calculation method that returns a double
                double rawScore = riskScoringService.calculateRiskScore(
                    blockers,
                    nonCompatibleDeps,
                    totalIssues,
                    0 // critical issues count from scan results summary
                );
                
                // Create a RiskScore object from the calculated double
                String category = determineRiskCategory(rawScore);
                String categoryLabel = getCategoryLabel(category);
                String categoryColor = getCategoryColor(category);
                
                calculatedRiskScore = new RiskScoringService.RiskScore(
                    rawScore,
                    category,
                    categoryLabel,
                    categoryColor,
                    Map.of(
                        "dependencyIssues", (int) Math.min(rawScore * 0.4, 100),
                        "codeComplexity", (int) Math.min(rawScore * 0.2, 100),
                        "platformRisk", (int) Math.min(rawScore * 0.2, 100),
                        "validationConfidence", (int) Math.min(rawScore * 0.2, 100)
                    ),
                    Collections.emptyList()
                );
                log.info("Calculated risk score: {}", calculatedRiskScore.totalScore());
            }
            
            // Create request with calculated risk score
            RiskAnalysisReportRequest requestWithScore = new RiskAnalysisReportRequest(
                request.outputPath(),
                request.projectName(),
                request.reportTitle(),
                request.dependencyGraph(),
                request.analysisReport(),
                request.scanResults(),
                request.platformScanResults(),
                calculatedRiskScore,
                request.recommendedStrategy(),
                request.strategyDetails(),
                request.validationMetrics(),
                request.topBlockers(),
                request.recommendations(),
                request.implementationPhases(),
                request.customData()
            );
            
            // Create snippets using the factory
            RiskAnalysisSnippetFactory factory = new RiskAnalysisSnippetFactory();
            List<adrianmikula.jakartamigration.pdfreporting.snippet.HtmlSnippet> snippets = factory.createSnippets(requestWithScore);
            
            // Assemble the complete HTML report
            ReportAssembler assembler = new ReportAssembler();
            String htmlContent = assembler.assembleReport(snippets, request.reportTitle());
            
            log.info("Successfully generated HTML with {} snippets", snippets.size());
            return htmlContent;
            
        } catch (Exception e) {
            log.error("Failed to generate HTML with snippets", e);
            // Fallback to simplified template if snippet generation fails
            return generateFallbackHtml(request);
        }
    }
    
    private String determineRiskCategory(double score) {
        if (score < 25) return "low";
        if (score < 50) return "medium";
        if (score < 75) return "high";
        return "extreme";
    }
    
    private String getCategoryLabel(String category) {
        return switch (category) {
            case "low" -> "Low Risk";
            case "medium" -> "Medium Risk";
            case "high" -> "High Risk";
            case "extreme" -> "Extreme Risk";
            default -> "Unknown";
        };
    }
    
    private String getCategoryColor(String category) {
        return switch (category) {
            case "low" -> "#27ae60";
            case "medium" -> "#f39c12";
            case "high" -> "#e74c3c";
            case "extreme" -> "#8e44ad";
            default -> "#95a5a6";
        };
    }
    
    /**
     * Fallback HTML generation for critical errors.
     * Provides a minimal report when snippet generation fails completely.
     */
    private String generateFallbackHtml(RiskAnalysisReportRequest request) {
        String projectName = request.projectName() != null ? request.projectName() : "Unknown Project";
        String reportTitle = request.reportTitle() != null ? request.reportTitle() : "Risk Analysis Report";
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <title>%s</title>
                <meta charset="UTF-8" />
                <style>
                    body { font-family: Arial, sans-serif; margin: 40px; }
                    .container { max-width: 800px; margin: 0 auto; }
                    .error { background: #ffebee; padding: 20px; border-radius: 4px; margin: 20px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>%s</h1>
                    <div class="error">
                        <h2>Report Generation Error</h2>
                        <p>Unable to generate the full report due to a technical error.</p>
                        <p>Project: %s</p>
                        <p>Please check the logs for more details.</p>
                    </div>
                </div>
            </body>
            </html>
            """, reportTitle, reportTitle, projectName);
    }
    
    private String generateSimplifiedRiskAnalysisHtml(RiskAnalysisReportRequest request, String projectName, String reportTitle) {
        int totalIssues = request.scanResults() != null ? request.scanResults().totalIssuesFound() : 0;
        double riskScore = request.riskScore() != null ? request.riskScore().totalScore() : 50.0;
        String riskLevel = determineRiskLevel(riskScore);
        int readinessScore = 100 - (int) riskScore;
        
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>%s</title>
                <meta charset="UTF-8" />
                <style>
                    body {
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                        margin: 0;
                        padding: 40px;
                        line-height: 1.6;
                        color: #333;
                        background: #f8f9fa;
                    }
                    .container {
                        max-width: 1200px;
                        margin: 0 auto;
                        background: white;
                        padding: 40px;
                        border-radius: 8px;
                        box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
                    }
                    .header {
                        text-align: center;
                        margin-bottom: 40px;
                        border-bottom: 2px solid #e9ecef;
                        padding-bottom: 20px;
                    }
                    .header h1 {
                        color: #2c3e50;
                        margin: 0;
                        font-size: 2.5em;
                        font-weight: 300;
                    }
                    .header h2 {
                        color: #34495e;
                        font-size: 1.8em;
                        margin: 10px 0;
                        font-weight: 400;
                    }
                    .section {
                        margin: 30px 0;
                    }
                    .section h2 {
                        color: #2c3e50;
                        border-bottom: 2px solid #3498db;
                        padding-bottom: 10px;
                    }
                    .warning-box {
                        background: #fff3cd;
                        border: 1px solid #ffeaa7;
                        border-radius: 4px;
                        padding: 15px;
                        margin: 20px 0;
                    }
                    .warning-box h3 {
                        color: #856404;
                        margin-top: 0;
                    }
                    .metrics-grid {
                        display: grid;
                        grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                        gap: 20px;
                        margin: 20px 0;
                    }
                    .metric-card {
                        background: #f8f9fa;
                        padding: 20px;
                        border-radius: 8px;
                        text-align: center;
                        border-left: 4px solid #3498db;
                    }
                    .metric-value {
                        font-size: 2em;
                        font-weight: bold;
                        color: #2c3e50;
                    }
                    .metric-label {
                        color: #7f8c8d;
                        margin-top: 5px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>%s</h1>
                        <h2>Risk Analysis Report</h2>
                        <p>Generated for Eclipse-based project</p>
                    </div>
                    
                    <div class="warning-box">
                        <h3>⚠️ Eclipse Project Detected</h3>
                        <p>This appears to be an Eclipse-based project without Maven or Gradle build files. 
                        Dependency analysis requires pom.xml, build.gradle, or build.gradle.kts files. 
                        The report below includes available scan data only.</p>
                    </div>
                    
                    <div class="section">
                        <h2>Available Analysis Results</h2>
                        <div class="metrics-grid">
                            <div class="metric-card">
                                <div class="metric-value">%d</div>
                                <div class="metric-label">Issues Found</div>
                            </div>
                            <div class="metric-card">
                                <div class="metric-value">%s</div>
                                <div class="metric-label">Risk Level</div>
                            </div>
                            <div class="metric-card">
                                <div class="metric-value">%d%%</div>
                                <div class="metric-label">Readiness Score</div>
                            </div>
                        </div>
                    </div>
                    
                    <div class="section">
                        <h2>Recommendations</h2>
                        <ul>
                            <li>Consider migrating to Maven or Gradle for comprehensive dependency analysis</li>
                            <li>Add pom.xml or build.gradle files to enable full dependency scanning</li>
                            <li>Run a complete project analysis after build system migration</li>
                        </ul>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                reportTitle,
                projectName,
                reportTitle,
                totalIssues,
                riskLevel,
                readinessScore
            );
    }
    
    private String generateRefactoringActionHtml(RefactoringActionReportRequest request) {
        // Extract data from request
        String projectName = request.projectName() != null ? request.projectName() : "Unknown Project";
        String reportTitle = request.reportTitle() != null ? request.reportTitle() : "Jakarta Migration Refactoring Action Report";
        
        // Calculate metrics
        int totalFiles = request.javaxReferences() != null ? request.javaxReferences().size() : 0;
        int availableRecipes = request.openRewriteRecipes() != null ? request.openRewriteRecipes().size() : 0;
        int readyForAutomation = calculateAutomationReady(request.refactoringReadiness());
        
        // Use cached CSS if available, otherwise inline
        String commonStyles = cssCacheManager.getCachedCommonStyles() != null && !cssCacheManager.getCachedCommonStyles().isEmpty() ? cssCacheManager.getCachedCommonStyles() : cssGenerator.getInlineCommonStyles();
        String headerStyles = cssGenerator.getSharedHeaderStyles();
        String footerStyles = cssGenerator.getSharedFooterStyles();
        String refactoringStyles = cssCacheManager.getCachedRefactoringStyles() != null && !cssCacheManager.getCachedRefactoringStyles().isEmpty() ? cssCacheManager.getCachedRefactoringStyles() : cssGenerator.getInlineRefactoringStyles();
        
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>%s</title>
                <meta charset="UTF-8" />
                <style>
                    %s
                    %s
                    %s
                    %s
                </style>
            </head>
            <body>
                <div class="container">
                    %s
                    
                    <div class="section">
                        <h2>Refactoring Summary</h2>
                        <p>This report provides a comprehensive action plan for migrating javax.* references to jakarta.* equivalents. Each identified issue includes available OpenRewrite recipes for automated refactoring.</p>
                        
                        <div class="metrics-grid">
                            <div class="metric-card">
                                <div class="metric-value">%d</div>
                                <div class="metric-label">Files Requiring Refactoring</div>
                            </div>
                            <div class="metric-card">
                                <div class="metric-value">%d</div>
                                <div class="metric-label">OpenRewrite Recipes Available</div>
                            </div>
                            <div class="metric-card">
                                <div class="metric-value">%d%%</div>
                                <div class="metric-label">Ready for Automation</div>
                            </div>
                            <div class="metric-card">
                                <div class="metric-value">%d</div>
                                <div class="metric-label">Priority Actions</div>
                            </div>
                        </div>
                    </div>
                    
                    <div class="section">
                        <h2>File-by-File Refactoring Actions</h2>
                        <p>Detailed breakdown of each file requiring javax to jakarta migration, with specific refactoring recommendations:</p>
                        
                        %s
                    </div>
                    
                    <div class="section">
                        <h2>OpenRewrite Recipe Recommendations</h2>
                        <p>Available OpenRewrite recipes for automated refactoring of detected issues:</p>
                        
                        %s
                    </div>
                    
                    <div class="section">
                        <h2>Refactoring Validation Checklist</h2>
                        <p>Follow these steps to validate successful refactoring:</p>
                        
                        <div class="action-steps">
                            <ol>
                                <li>Apply OpenRewrite recipes where available</li>
                                <li>Manually update remaining javax imports to jakarta</li>
                                <li>Run comprehensive tests to verify functionality</li>
                                <li>Update configuration files and properties</li>
                                <li>Validate application startup with Jakarta EE runtime</li>
                                <li>Perform integration testing with external systems</li>
                                <li>Update documentation and deployment scripts</li>
                            </ol>
                        </div>
                    </div>
                    
                    %s
                </div>
            </body>
            </html>
            """.formatted(
                reportTitle,
                commonStyles,
                headerStyles,
                footerStyles,
                refactoringStyles,
                generateSharedHeader(reportTitle, projectName, "Refactoring Action Report"),
                totalFiles,
                availableRecipes,
                readyForAutomation,
                calculatePriorityActions(request.priorityRanking()),
                generateRefactorTableHtml(request.javaxReferences()),
                generateRecipeTableHtml(request.openRewriteRecipes()),
                generateSharedFooter("Refactoring Action Report", 1, 1)
            );
    }

    /**
     * Generate refactoring action HTML using snippet-based architecture with real scan data.
     * Replaces the monolithic template approach with data from actual scanners.
     */
    private String generateRefactoringActionHtmlWithSnippets(RefactoringActionReportRequest request) {
        log.info("Generating refactoring report using snippet-based architecture with real scan data");

        try {
            // Create snippets using the new factory with real scan data
            RefactoringSnippetFactory factory = new RefactoringSnippetFactory();
            List<adrianmikula.jakartamigration.pdfreporting.snippet.HtmlSnippet> snippets = factory.createSnippets(request);

            // Assemble the complete HTML report
            ReportAssembler assembler = new ReportAssembler();
            String htmlContent = assembler.assembleReport(snippets, request.reportTitle());

            log.info("Refactoring report generated with {} snippets using real scan data", snippets.size());
            return htmlContent;

        } catch (Exception e) {
            log.error("Error generating refactoring report with snippets, falling back to legacy method", e);
            // Fallback to legacy method if snippet generation fails
            return generateRefactoringActionHtml(request);
        }
    }

    // Helper methods for Risk Analysis Report
    private int calculateJakartaCompatible(DependencyGraph dependencyGraph) {
        if (dependencyGraph == null || dependencyGraph.getNodes().isEmpty()) {
            return 0;
        }
        
        // Count dependencies that are already Jakarta compatible
        return (int) dependencyGraph.getNodes().stream()
                .filter(node -> node.artifactId() != null && 
                        (node.artifactId().contains("jakarta") || 
                         node.version() != null && node.version().startsWith("3.")))
                .count();
    }
    
    private int calculateReadinessScore(RiskAnalysisReportRequest request) {
        if (request.riskScore() == null) {
            return 50; // Default readiness
        }
        
        double riskScore = request.riskScore().totalScore();
        // Inverse relationship: higher risk = lower readiness
        return Math.max(0, Math.min(100, 100 - (int) riskScore));
    }
    
    private String getRiskDescription(String riskLevel) {
        return switch (riskLevel.toLowerCase()) {
            case "low" -> "Low complexity migration with minimal dependencies requiring updates. Most components are already Jakarta EE compatible.";
            case "medium" -> "Moderate complexity migration with several dependencies requiring updates. Some manual intervention may be needed.";
            case "high" -> "High complexity migration with significant dependency changes required. Comprehensive testing recommended.";
            case "critical" -> "Critical complexity migration requiring major architectural changes. Extended timeline and resources needed.";
            default -> "Unknown risk level. Further analysis required.";
        };
    }
    
    private String generateRiskScoringHtml(RiskScoringService.RiskScore riskScore) {
        if (riskScore == null) {
            return "<p>Risk scoring data not available.</p>";
        }
        
        StringBuilder html = new StringBuilder();
        html.append("<div class='metrics-grid'>");
        
        // Component scores
        if (riskScore.componentScores() != null) {
            riskScore.componentScores().forEach((component, score) -> {
                html.append("<div class='metric-card'>");
                html.append("<div class='metric-value'>").append(score).append("</div>");
                html.append("<div class='metric-label'>").append(component).append("</div>");
                html.append("</div>");
            });
        }
        
        html.append("</div>");
        return html.toString();
    }
    
    private String generateDependencyTableHtml(DependencyGraph dependencyGraph) {
        if (dependencyGraph == null || dependencyGraph.getNodes().isEmpty()) {
            return "<p>No dependency data available.</p>";
        }
        
        StringBuilder html = new StringBuilder();
        html.append("<table class='dependency-table'>");
        html.append("<thead><tr>");
        html.append("<th>Dependency</th>");
        html.append("<th>Version</th>");
        html.append("<th>Status</th>");
        html.append("<th>Notes</th>");
        html.append("</tr></thead><tbody>");
        
        dependencyGraph.getNodes().forEach(node -> {
            boolean isCompatible = node.artifactId() != null && 
                    (node.artifactId().contains("jakarta") || 
                     node.version() != null && node.version().startsWith("3."));
            
            html.append("<tr>");
            html.append("<td>").append(htmlUtils.escapeHtml(node.artifactId() != null ? node.artifactId() : "Unknown")).append("</td>");
            html.append("<td>").append(htmlUtils.escapeHtml(node.version() != null ? node.version() : "Unknown")).append("</td>");
            html.append("<td class='").append(isCompatible ? "compatible" : "incompatible").append("'>");
            html.append(isCompatible ? "Jakarta Compatible" : "Requires Update");
            html.append("</td>");
            html.append("<td>").append(htmlUtils.escapeHtml(isCompatible ? "No action needed" : "Update to Jakarta EE equivalent")).append("</td>");
            html.append("</tr>");
        });
        
        html.append("</tbody></table>");
        return html.toString();
    }
    
    private String generateBlockersHtml(List<Map<String, Object>> blockers) {
        if (blockers == null || blockers.isEmpty()) {
            return "<p>No critical blockers identified.</p>";
        }
        
        StringBuilder html = new StringBuilder();
        html.append("<div class='blocker-list'>");
        
        blockers.forEach(blocker -> {
            String severity = (String) blocker.getOrDefault("severity", "MEDIUM");
            String name = (String) blocker.getOrDefault("name", "Unknown Blocker");
            String impact = (String) blocker.getOrDefault("impact", "Unknown Impact");
            String remediation = (String) blocker.getOrDefault("remediation", "No remediation available");
            
            html.append("<div class='blocker-item blocker-").append(severity.toLowerCase()).append("'>");
            html.append("<strong>").append(htmlUtils.escapeHtml(name)).append("</strong><br>");
            html.append("<em>Severity: ").append(htmlUtils.escapeHtml(severity)).append("</em><br>");
            html.append("<strong>Impact:</strong> ").append(htmlUtils.escapeHtml(impact)).append("<br>");
            html.append("<strong>Remediation:</strong> ").append(htmlUtils.escapeHtml(remediation));
            html.append("</div>");
        });
        
        html.append("</div>");
        return html.toString();
    }
    
    private String generateTimelineHtml(Map<String, Object> phases) {
        if (phases == null || phases.isEmpty()) {
            return "<p>No implementation timeline available.</p>";
        }
        
        StringBuilder html = new StringBuilder();
        html.append("<div class='timeline'>");
        
        phases.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("phase"))
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> phaseData = (Map<String, Object>) entry.getValue();
                    String phaseName = (String) phaseData.getOrDefault("name", "Unknown Phase");
                    String description = (String) phaseData.getOrDefault("description", "No description");
                    
                    html.append("<div class='timeline-item'>");
                    html.append("<div class='timeline-phase'>").append(htmlUtils.escapeHtml(phaseName)).append("</div>");
                    html.append("<div class='timeline-duration'>").append(htmlUtils.escapeHtml(description)).append("</div>");
                    html.append("</div>");
                });
        
        html.append("</div>");
        return html.toString();
    }
    
    // Helper methods for Refactoring Action Report
    private int calculateAutomationReady(Map<String, Object> refactoringReadiness) {
        if (refactoringReadiness == null) {
            return 50; // Default estimate
        }
        
        // Calculate percentage of files ready for automated refactoring
        Object readyValue = refactoringReadiness.getOrDefault("automationReady", 0.5);
        if (readyValue instanceof Number) {
            return ((Number) readyValue).intValue();
        }
        return 50;
    }
    
    private int calculatePriorityActions(Map<String, Object> priorityRanking) {
        if (priorityRanking == null) {
            return 0;
        }
        
        // Extract high and medium priority counts from simple integer values
        int highPriorityCount = 0;
        int mediumPriorityCount = 0;
        
        Object highPriority = priorityRanking.get("highPriority");
        if (highPriority instanceof Number) {
            highPriorityCount = ((Number) highPriority).intValue();
        }
        
        Object mediumPriority = priorityRanking.get("mediumPriority");
        if (mediumPriority instanceof Number) {
            mediumPriorityCount = ((Number) mediumPriority).intValue();
        }
        
        return highPriorityCount + mediumPriorityCount;
    }
    
    private String generateRefactorTableHtml(List<Map<String, Object>> javaxReferences) {
        if (javaxReferences == null || javaxReferences.isEmpty()) {
            return "<p>No javax references found requiring refactoring.</p>";
        }
        
        StringBuilder html = new StringBuilder();
        html.append("<table class='refactor-table'>");
        html.append("<thead><tr>");
        html.append("<th>File</th>");
        html.append("<th>Line</th>");
        html.append("<th>Javax Reference</th>");
        html.append("<th>Priority</th>");
        html.append("<th>Recipe Available</th>");
        html.append("</tr></thead><tbody>");
        
        javaxReferences.forEach(ref -> {
            String file = (String) ref.getOrDefault("file", "Unknown");
            String line = String.valueOf(ref.getOrDefault("line", "0"));
            String reference = (String) ref.getOrDefault("reference", "Unknown");
            String priority = (String) ref.getOrDefault("priority", "medium");
            boolean recipeAvailable = (Boolean) ref.getOrDefault("recipeAvailable", false);
            
            html.append("<tr class='priority-").append(priority.toLowerCase()).append("'>");
            html.append("<td>").append(htmlUtils.escapeHtml(file)).append("</td>");
            html.append("<td>").append(htmlUtils.escapeHtml(line)).append("</td>");
            html.append("<td>").append(htmlUtils.escapeHtml(reference)).append("</td>");
            html.append("<td>").append(htmlUtils.escapeHtml(priority.toUpperCase())).append("</td>");
            html.append("<td class='").append(recipeAvailable ? "recipe-available" : "recipe-unavailable").append("'>");
            html.append(recipeAvailable ? "Available" : "Not Available");
            html.append("</td>");
            html.append("</tr>");
        });
        
        html.append("</tbody></table>");
        return html.toString();
    }
    
    private String generateRecipeTableHtml(List<Map<String, Object>> recipes) {
        if (recipes == null || recipes.isEmpty()) {
            return "<p>No OpenRewrite recipes available for detected issues.</p>";
        }
        
        StringBuilder html = new StringBuilder();
        html.append("<table class='refactor-table'>");
        html.append("<thead><tr>");
        html.append("<th>Recipe Name</th>");
        html.append("<th>Category</th>");
        html.append("<th>Description</th>");
        html.append("<th>Automated</th>");
        html.append("</tr></thead><tbody>");
        
        recipes.forEach(recipe -> {
            String name = (String) recipe.getOrDefault("name", "Unknown Recipe");
            String category = (String) recipe.getOrDefault("category", "General");
            String description = (String) recipe.getOrDefault("description", "No description");
            boolean automated = (Boolean) recipe.getOrDefault("automated", false);
            
            html.append("<tr>");
            html.append("<td>").append(htmlUtils.escapeHtml(name)).append("</td>");
            html.append("<td>").append(htmlUtils.escapeHtml(category)).append("</td>");
            html.append("<td>").append(htmlUtils.escapeHtml(description)).append("</td>");
            html.append("<td class='").append(automated ? "recipe-available" : "recipe-unavailable").append("'>");
            html.append(automated ? "Fully Automated" : "Manual Steps Required");
            html.append("</td>");
            html.append("</tr>");
        });
        
        html.append("</tbody></table>");
        return html.toString();
    }
    
    private String determineRiskLevel(double score) {
        if (score < 25) {
            return "LOW";
        } else if (score < 50) {
            return "MEDIUM";
        } else if (score < 75) {
            return "HIGH";
        } else {
            return "CRITICAL";
        }
    }
    
    // Shared header and footer generation methods
    private static final String PLUGIN_NAME = "Jakarta Migration IntelliJ Plugin";
    private static final String PLUGIN_VERSION = "v3.0";

    private String generateSharedHeader(String reportTitle, String projectName, String reportType) {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        String pluginIconSvg = htmlUtils.getPluginIconSvg();

        return """
            <div class="report-header">
                <div class="header-left">
                    <div class="plugin-icon">
                        %s
                    </div>
                    <div class="plugin-name">%s</div>
                </div>
                <div class="header-center">
                    <h1 class="report-title">%s</h1>
                    <div class="project-name">%s</div>
                    <div class="report-type">%s</div>
                </div>
                <div class="header-right">
                    <div class="timestamp">Generated: %s</div>
                </div>
            </div>
            """.formatted(
                pluginIconSvg,
                PLUGIN_NAME,
                htmlUtils.escapeHtml(reportTitle),
                htmlUtils.escapeHtml(projectName),
                htmlUtils.escapeHtml(reportType),
                timestamp
            );
    }
    
    private String generateSharedFooter(String reportType, int pageNumber, int totalPages) {
        String pluginIconSvg = htmlUtils.getPluginIconSvg();
        return """
            <div class="report-footer">
                <div class="footer-left">
                    <div class="plugin-icon-footer">
                        %s
                    </div>
                    <div class="plugin-info">%s %s</div>
                </div>
                <div class="footer-center">
                    <div class="page-info">Page %d of %d</div>
                </div>
                <div class="footer-right">
                    <div class="report-type-footer">%s</div>
                </div>
            </div>
            """.formatted(
                pluginIconSvg,
                PLUGIN_NAME,
                PLUGIN_VERSION,
                pageNumber,
                totalPages,
                htmlUtils.escapeHtml(reportType)
            );
    }
    
    /**
     * Save HTML report directly to file (PDF generation disabled due to memory issues).
     */
    private Path saveHtmlReport(String htmlContent, Path outputPath) throws IOException {
        // Ensure output directory exists
        Files.createDirectories(outputPath.getParent());
        
        // Change extension from .pdf to .html
        Path htmlPath = outputPath.resolveSibling(outputPath.getFileName().toString().replace(".pdf", ".html"));
        
        // Write HTML content to file
        Files.writeString(htmlPath, htmlContent);
        
        log.info("HTML report saved successfully: {}", htmlPath.toAbsolutePath());
        log.info("HTML file size: {} bytes", Files.size(htmlPath));
        
        return htmlPath;
    }
    
    /**
     * Generates HTML content for consolidated reports.
     */
    private String generateConsolidatedHtml(ConsolidatedReportRequest request) {
        StringBuilder html = new StringBuilder();
        
        html.append("<html><head><title>")
           .append(htmlUtils.escapeHtml(request.reportTitle()))
           .append("</title></head><body>")
           .append("<h1>")
           .append(htmlUtils.escapeHtml(request.reportTitle()))
           .append("</h1>")
           .append("<h2>Project: ")
           .append(htmlUtils.escapeHtml(request.projectName()))
           .append("</h2>")
           .append("<p>Generated on: ")
           .append(LocalDateTime.now().format(DATE_FORMATTER))
           .append("</p>");
        
        if (request.dependencyGraph() != null) {
            html.append("<h3>Dependencies: ")
               .append(request.dependencyGraph().getNodes().size())
               .append("</h3>");
        }
        
        if (request.scanResults() != null) {
            html.append("<h3>Issues Found: ")
               .append(request.scanResults().totalIssuesFound())
               .append("</h3>");
        }
        
        if (request.recommendations() != null && !request.recommendations().isEmpty()) {
            html.append("<h3>Recommendations:</h3><ul>");
            for (String recommendation : request.recommendations()) {
                html.append("<li>").append(htmlUtils.escapeHtml(recommendation)).append("</li>");
            }
            html.append("</ul>");
        }
        
        html.append("<p><em>Generated by Jakarta Migration Tool v3.0</em></p>")
           .append("</body></html>");
        
        return html.toString();
    }
}
