package adrianmikula.jakartamigration.pdfreporting.service.impl;

import adrianmikula.jakartamigration.pdfreporting.service.PdfReportService;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults;
import adrianmikula.jakartamigration.risk.RiskScoringService;
import adrianmikula.jakartamigration.pdfreporting.snippet.RiskAnalysisSnippetFactory;
import adrianmikula.jakartamigration.pdfreporting.snippet.ReportAssembler;
import adrianmikula.jakartamigration.pdfreporting.util.HtmlValidator;
import lombok.extern.slf4j.Slf4j;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Simplified HTML-to-PDF report generation service.
 * Provides basic HTML generation with fallback for testing when dependencies are not available.
 */
@Slf4j
public class HtmlToPdfReportServiceImpl implements PdfReportService {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public HtmlToPdfReportServiceImpl() {
        log.info("Initializing HTML-to-PDF Report Service (simplified version for testing)");
    }
    
    @Override
    public Path generateRiskAnalysisReport(RiskAnalysisReportRequest request) {
        log.info("Generating Risk Analysis HTML-to-PDF report: {}", request.outputPath());
        
        try {
            // Generate HTML content using snippet-based architecture
            String htmlContent = generateRiskAnalysisHtmlWithSnippets(request);
            
            // Convert HTML to PDF
            convertHtmlToPdf(htmlContent, request.outputPath());
            
            return request.outputPath();
        } catch (Exception e) {
            log.error("Error generating risk analysis report", e);
            throw new RuntimeException("Failed to generate risk analysis report", e);
        }
    }
    
    @Override
    public Path generateRefactoringActionReport(RefactoringActionReportRequest request) {
        log.info("Generating Refactoring Action HTML-to-PDF report: {}", request.outputPath());
        
        try {
            // Generate HTML content with modern professional styling
            String htmlContent = generateRefactoringActionHtml(request);
            
            // Convert HTML to PDF
            convertHtmlToPdf(htmlContent, request.outputPath());
            
            return request.outputPath();
        } catch (Exception e) {
            log.error("Error generating refactoring action report", e);
            throw new RuntimeException("Failed to generate refactoring action report", e);
        }
    }
    
    @Override
    public Path generateConsolidatedReport(ConsolidatedReportRequest request) {
        log.info("Generating Consolidated HTML-to-PDF report: {}", request.outputPath());
        
        try {
            // Generate HTML content with modern professional styling
            String htmlContent = generateConsolidatedHtml(request);
            
            // Convert HTML to PDF
            convertHtmlToPdf(htmlContent, request.outputPath());
            
            return request.outputPath();
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
    
    private String generateComprehensiveHtml(GeneratePdfReportRequest request) {
        String projectName = request.customData() != null ? 
            (String) request.customData().getOrDefault("projectName", "Unknown Project") : "Unknown Project";
        String description = request.customData() != null ? 
            (String) request.customData().getOrDefault("description", "Jakarta Migration Analysis") : "Jakarta Migration Analysis";
        
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Jakarta Migration Risk Analysis Report</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 40px; line-height: 1.6; }
                    .header { text-align: center; border-bottom: 2px solid #333; padding-bottom: 20px; margin-bottom: 30px; }
                    .section { margin: 30px 0; }
                    .risk-high { color: #d32f2f; font-weight: bold; }
                    .risk-medium { color: #f57c00; font-weight: bold; }
                    .risk-low { color: #388e3c; font-weight: bold; }
                    h1 { color: #333; }
                    h2 { color: #555; border-bottom: 1px solid #eee; padding-bottom: 10px; }
                    h3 { color: #666; }
                    ul { margin: 15px 0; }
                    li { margin: 8px 0; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>Jakarta Migration Risk Analysis Report</h1>
                    <h2>%s</h2>
                    <p>%s</p>
                    <p><strong>Generated:</strong> %s</p>
                </div>
                
                <div class="section">
                    <h2>Executive Summary</h2>
                    <p>This report provides a comprehensive analysis of the Jakarta EE migration requirements for the project. The analysis identifies dependencies that need to be updated from javax.* to jakarta.* namespaces and provides recommendations for a smooth migration path.</p>
                    <p><strong>Risk Level:</strong> <span class="risk-medium">Medium</span></p>
                    <p><strong>Estimated Migration Time:</strong> 2-4 weeks</p>
                </div>
                
                <div class="section">
                    <h2>Risk Assessment</h2>
                    <p>The migration risk has been assessed based on the following factors:</p>
                    <ul>
                        <li>Dependency complexity and version compatibility</li>
                        <li>Codebase size and structure</li>
                        <li>Third-party library compatibility</li>
                        <li>Testing requirements</li>
                    </ul>
                    <p>Overall risk assessment indicates a medium complexity migration that can be managed with proper planning.</p>
                </div>
                
                <div class="section">
                    <h2>Top Blockers</h2>
                    <ul>
                        <li><strong>Dependency Updates:</strong> Multiple javax.* dependencies require updates</li>
                        <li><strong>Configuration Changes:</strong> Spring Boot configuration may need updates</li>
                        <li><strong>Testing:</strong> Comprehensive testing required after migration</li>
                    </ul>
                </div>
                
                <div class="section">
                    <h2>Recommendations</h2>
                    <ul>
                        <li>Update javax dependencies to jakarta equivalents</li>
                        <li>Test compatibility with new versions</li>
                        <li>Review and update configuration files</li>
                        <li>Implement comprehensive testing strategy</li>
                        <li>Plan for rollback strategy</li>
                    </ul>
                </div>
                
                <div class="section">
                    <h2>Next Steps</h2>
                    <p>Proceed with the migration in phases, starting with dependency updates and followed by comprehensive testing. Ensure proper backup and rollback procedures are in place.</p>
                </div>
            </body>
            </html>
            """.formatted(
                projectName,
                description,
                LocalDateTime.now().format(DATE_FORMATTER)
            );
    }
    
    private String generateDependencyHtml(DependencyGraph dependencyGraph) {
        int totalDeps = dependencyGraph != null ? dependencyGraph.getNodes().size() : 0;
        
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Dependency Analysis Report</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 40px; line-height: 1.6; }
                    .header { text-align: center; border-bottom: 2px solid #333; padding-bottom: 20px; margin-bottom: 30px; }
                    .section { margin: 30px 0; }
                    .summary { background: #f5f5f5; padding: 20px; border-radius: 5px; }
                    h1 { color: #333; }
                    h2 { color: #555; border-bottom: 1px solid #eee; padding-bottom: 10px; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>Dependency Analysis Report</h1>
                    <p><strong>Generated:</strong> %s</p>
                </div>
                
                <div class="section">
                    <h2>Summary</h2>
                    <div class="summary">
                        <p><strong>Total Dependencies:</strong> %d</p>
                        <p><strong>Dependencies Requiring Update:</strong> %d</p>
                        <p><strong>Jakarta Compatible:</strong> %d</p>
                    </div>
                </div>
                
                <div class="section">
                    <h2>Analysis Details</h2>
                    <p>The dependency analysis identified all external dependencies that may be affected by the Jakarta EE migration. Dependencies using javax.* namespaces will need to be updated to their jakarta.* equivalents.</p>
                </div>
            </body>
            </html>
            """.formatted(
                LocalDateTime.now().format(DATE_FORMATTER),
                totalDeps,
                Math.max(1, totalDeps / 3), // Estimate 1/3 need updates
                Math.max(0, totalDeps - totalDeps / 3)
            );
    }
    
    private String generateScanResultsHtml(ComprehensiveScanResults scanResults) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Scan Results Report</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 40px; line-height: 1.6; }
                    .header { text-align: center; border-bottom: 2px solid #333; padding-bottom: 20px; margin-bottom: 30px; }
                    .section { margin: 30px 0; }
                    .findings { background: #f5f5f5; padding: 20px; border-radius: 5px; }
                    h1 { color: #333; }
                    h2 { color: #555; border-bottom: 1px solid #eee; padding-bottom: 10px; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>Scan Results Report</h1>
                    <p><strong>Generated:</strong> %s</p>
                </div>
                
                <div class="section">
                    <h2>Scan Summary</h2>
                    <div class="findings">
                        <p><strong>Scan Status:</strong> Completed</p>
                        <p><strong>Issues Found:</strong> Multiple items requiring attention</p>
                        <p><strong>Recommendations:</strong> Available for review</p>
                    </div>
                </div>
                
                <div class="section">
                    <h2>Key Findings</h2>
                    <p>The comprehensive scan identified several areas that require attention for successful Jakarta EE migration. Detailed findings are available in the full report.</p>
                </div>
            </body>
            </html>
            """.formatted(LocalDateTime.now().format(DATE_FORMATTER));
    }
    
    private String generateRiskAnalysisHtml(RiskAnalysisReportRequest request) {
        // Extract data from request
        String projectName = request.projectName() != null ? request.projectName() : "Unknown Project";
        String reportTitle = request.reportTitle() != null ? request.reportTitle() : "Jakarta Migration Risk Analysis Report";
        
        // Check if this is an Eclipse project (no dependency graph)
        if (request.dependencyGraph() == null) {
            return generateSimplifiedRiskAnalysisHtml(request, projectName, reportTitle);
        }
        
        // Calculate metrics from real data
        int totalDependencies = request.dependencyGraph().getNodes().size();
        int jakartaCompatible = calculateJakartaCompatible(request.dependencyGraph());
        int totalIssues = request.scanResults() != null ? request.scanResults().totalIssuesFound() : 0;
        
        // Risk assessment data
        double riskScore = request.riskScore() != null ? request.riskScore().totalScore() : 50.0;
        String riskLevel = determineRiskLevel(riskScore);
        
        // Build HTML with modern professional styling
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>%s</title>
                <meta charset="UTF-8">
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
                    %s
                    %s
                    .header h2 {
                        color: #34495e;
                        font-size: 1.8em;
                        margin: 10px 0;
                        font-weight: 400;
                    }
                    .risk-dial {
                        display: inline-block;
                        width: 120px;
                        height: 120px;
                        border-radius: 50%%;
                        text-align: center;
                        line-height: 120px;
                        font-size: 1.5em;
                        font-weight: bold;
                        margin: 20px;
                        color: white;
                    }
                    .risk-low { background: #27ae60; }
                    .risk-medium { background: #f39c12; }
                    .risk-high { background: #e74c3c; }
                    .risk-critical { background: #8e44ad; }
                    .section {
                        margin: 40px 0;
                        padding: 30px;
                        border: 1px solid #e1e8ed;
                        border-radius: 8px;
                        background: #ffffff;
                    }
                    .section h2 {
                        color: #2c3e50;
                        border-bottom: 2px solid #3498db;
                        padding-bottom: 15px;
                        margin-top: 0;
                        font-size: 1.8em;
                    }
                    .metrics-grid {
                        display: grid;
                        grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
                        gap: 20px;
                        margin: 30px 0;
                    }
                    .metric-card {
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                        color: white;
                        padding: 25px;
                        border-radius: 12px;
                        text-align: center;
                        box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
                    }
                    .metric-value {
                        font-size: 2.5em;
                        font-weight: bold;
                        margin-bottom: 10px;
                    }
                    .metric-label {
                        font-size: 1.1em;
                        opacity: 0.9;
                    }
                    .dependency-table {
                        width: 100%%;
                        border-collapse: collapse;
                        margin: 20px 0;
                        font-size: 0.9em;
                    }
                    .dependency-table th,
                    .dependency-table td {
                        padding: 12px 15px;
                        text-align: left;
                        border-bottom: 1px solid #ddd;
                    }
                    .dependency-table th {
                        background-color: #34495e;
                        color: white;
                        font-weight: bold;
                    }
                    .dependency-table tr:hover {
                        background-color: #f5f5f5;
                    }
                    .compatible { color: #27ae60; font-weight: bold; }
                    .incompatible { color: #e74c3c; font-weight: bold; }
                    .blocker-list {
                        list-style: none;
                        padding: 0;
                    }
                    .blocker-item {
                        background: #fff3cd;
                        border: 1px solid #ffeaa7;
                        border-left: 4px solid #e17055;
                        padding: 15px;
                        margin: 10px 0;
                        border-radius: 4px;
                    }
                    .blocker-high { border-left-color: #e74c3c; background: #fdf2f2; }
                    .blocker-medium { border-left-color: #f39c12; background: #fef9e7; }
                    .blocker-low { border-left-color: #27ae60; background: #e8f8f5; }
                    .timeline {
                        position: relative;
                        padding: 20px 0;
                    }
                    .timeline-item {
                        padding: 20px;
                        margin: 20px 0;
                        background: #f8f9fa;
                        border-radius: 8px;
                        border-left: 4px solid #3498db;
                    }
                    .timeline-phase {
                        font-weight: bold;
                        color: #2c3e50;
                        font-size: 1.2em;
                        margin-bottom: 10px;
                    }
                    .timeline-duration {
                        color: #7f8c8d;
                        font-style: italic;
                        margin-bottom: 10px;
                    }
                    .footer {
                        margin-top: 60px;
                        padding-top: 30px;
                        border-top: 1px solid #e1e8ed;
                        text-align: center;
                        color: #7f8c8d;
                        font-size: 0.9em;
                    }
                    .strategy-table-container {
                        margin: 20px 0;
                        overflow-x: auto;
                    }
                    .strategy-comparison-table {
                        width: 100%%;
                        border-collapse: collapse;
                        font-size: 0.85em;
                        background: white;
                        border-radius: 8px;
                        overflow: hidden;
                        box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
                    }
                    .strategy-comparison-table th {
                        background: linear-gradient(135deg, #2c3e50 0%%, #34495e 100%%);
                        color: white;
                        font-weight: bold;
                        padding: 15px 12px;
                        text-align: left;
                        border-bottom: 2px solid #3498db;
                        font-size: 0.9em;
                    }
                    .strategy-comparison-table td {
                        padding: 12px;
                        border-bottom: 1px solid #e1e8ed;
                        vertical-align: top;
                        line-height: 1.4;
                    }
                    .strategy-row:hover {
                        background-color: #f8f9fa;
                    }
                    .strategy-row:nth-child(even) {
                        background-color: #fafbfc;
                    }
                    .strategy-row:nth-child(even):hover {
                        background-color: #f1f3f4;
                    }
                    .strategy-name {
                        font-weight: bold;
                        min-width: 120px;
                    }
                    .strategy-indicator {
                        width: 12px;
                        height: 12px;
                        border-radius: 50%%;
                        display: inline-block;
                        margin-right: 8px;
                        border: 1px solid rgba(0, 0, 0, 0.2);
                        vertical-align: middle;
                    }
                    .strategy-description {
                        min-width: 150px;
                        font-style: italic;
                        color: #555;
                    }
                    .strategy-benefits {
                        color: #27ae60;
                        font-size: 0.9em;
                    }
                    .strategy-risks {
                        color: #e74c3c;
                        font-size: 0.9em;
                    }
                    .strategy-phases {
                        font-size: 0.8em;
                        color: #666;
                    }
                    .strategy-use-case {
                        font-weight: 500;
                        color: #2c3e50;
                        font-size: 0.9em;
                    }
                    @media print {
                        body { padding: 20px; }
                        .container { box-shadow: none; }
                        .strategy-comparison-table { font-size: 0.75em; }
                        .strategy-comparison-table th { padding: 10px 8px; }
                        .strategy-comparison-table td { padding: 8px; }
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    %s
                    <div class="risk-summary">
                        <div style="margin: 30px 0;">
                            <span class="risk-dial risk-%s">%.0f</span>
                            <div style="display: inline-block; vertical-align: top; margin-left: 20px;">
                                <h3 style="margin: 0;">Risk Level: %s</h3>
                                <p style="margin: 5px 0;">Migration Readiness Score: %d%%</p>
                            </div>
                        </div>
                    </div>
                    
                    <div class="section">
                        <h2>Executive Summary</h2>
                        <p>This comprehensive risk analysis provides detailed assessment of the Jakarta EE migration requirements. The analysis evaluates dependency compatibility, platform readiness, and potential blockers to ensure a successful migration strategy.</p>
                        
                        <div class="metrics-grid">
                            <div class="metric-card">
                                <div class="metric-value">%d</div>
                                <div class="metric-label">Total Dependencies</div>
                            </div>
                            <div class="metric-card">
                                <div class="metric-value">%d</div>
                                <div class="metric-label">Jakarta Compatible</div>
                            </div>
                            <div class="metric-card">
                                <div class="metric-value">%d</div>
                                <div class="metric-label">Issues Found</div>
                            </div>
                            <div class="metric-card">
                                <div class="metric-value">%d%%</div>
                                <div class="metric-label">Readiness Score</div>
                            </div>
                        </div>
                    </div>
                    
                    <div class="section">
                        <h2>Risk Assessment</h2>
                        <p>The migration risk has been assessed based on dependency complexity, platform compatibility, and codebase analysis. Current risk score indicates %s level of complexity.</p>
                        
                        %s
                    </div>
                    
                    <div class="section">
                        <h2>Dependency Analysis</h2>
                        <p>Complete dependency analysis identified %d total dependencies with %d requiring updates to Jakarta EE equivalents.</p>
                        
                        %s
                    </div>
                    
                    <div class="section">
                        <h2>Top Blockers</h2>
                        <p>The following critical issues may block or complicate the migration process and should be addressed first:</p>
                        
                        %s
                    </div>
                    
                    <div class="section">
                        <h2>Implementation Timeline</h2>
                        <p>Recommended phased approach to minimize risk and ensure successful migration:</p>
                        
                        %s
                    </div>
                    
                    <div class="section">
                        <h2>Migration Strategy Comparison</h2>
                        <p>The following migration strategies are available for Jakarta EE migration. Each approach has distinct benefits, risks, and implementation considerations. Select the strategy that best aligns with your project requirements, timeline, and risk tolerance.</p>
                        
                        %s
                    </div>
                    
                    %s
                </div>
            </body>
            </html>
            """.formatted(
                generateSharedHeader(reportTitle, projectName, "Risk Analysis Report"),
                riskLevel.toLowerCase(),
                riskScore,
                riskLevel,
                calculateReadinessScore(request),
                totalDependencies,
                jakartaCompatible,
                totalIssues,
                calculateReadinessScore(request),
                getRiskDescription(riskLevel),
                generateRiskScoringHtml(request.riskScore()),
                totalDependencies,
                totalDependencies - jakartaCompatible,
                generateDependencyTableHtml(request.dependencyGraph()),
                generateBlockersHtml(request.topBlockers()),
                generateTimelineHtml(request.implementationPhases()),
                generateMigrationStrategyTableHtml(),
                generateSharedFooter("Risk Analysis Report", 1, 1)
            );
    }
    
    /**
     * Generate HTML content using the new snippet-based architecture.
     * This method replaces the monolithic template approach.
     */
    private String generateRiskAnalysisHtmlWithSnippets(RiskAnalysisReportRequest request) {
        log.info("Generating HTML using snippet-based architecture");
        
        try {
            // Create snippets using the factory
            RiskAnalysisSnippetFactory factory = new RiskAnalysisSnippetFactory();
            List<adrianmikula.jakartamigration.pdfreporting.snippet.HtmlSnippet> snippets = factory.createSnippets(request);
            
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
                <meta charset="UTF-8">
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
                <meta charset="UTF-8">
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
        
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>%s</title>
                <meta charset="UTF-8">
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
                    %s
                    %s
                    .section {
                        margin: 40px 0;
                        padding: 30px;
                        border: 1px solid #e1e8ed;
                        border-radius: 8px;
                        background: #ffffff;
                    }
                    .section h2 {
                        color: #2c3e50;
                        border-bottom: 2px solid #e74c3c;
                        padding-bottom: 15px;
                        margin-top: 0;
                        font-size: 1.8em;
                    }
                    .metrics-grid {
                        display: grid;
                        grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
                        gap: 20px;
                        margin: 30px 0;
                    }
                    .metric-card {
                        background: linear-gradient(135deg, #e74c3c 0%%, #c0392b 100%%);
                        color: white;
                        padding: 25px;
                        border-radius: 12px;
                        text-align: center;
                        box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
                    }
                    .metric-value {
                        font-size: 2.5em;
                        font-weight: bold;
                        margin-bottom: 10px;
                    }
                    .metric-label {
                        font-size: 1.1em;
                        opacity: 0.9;
                    }
                    .refactor-table {
                        width: 100%%;
                        border-collapse: collapse;
                        margin: 20px 0;
                        font-size: 0.9em;
                    }
                    .refactor-table th,
                    .refactor-table td {
                        padding: 12px 15px;
                        text-align: left;
                        border-bottom: 1px solid #ddd;
                    }
                    .refactor-table th {
                        background-color: #c0392b;
                        color: white;
                        font-weight: bold;
                    }
                    .refactor-table tr:hover {
                        background-color: #f5f5f5;
                    }
                    .priority-high { background: #fdf2f2; color: #e74c3c; font-weight: bold; }
                    .priority-medium { background: #fef9e7; color: #f39c12; font-weight: bold; }
                    .priority-low { background: #e8f8f5; color: #27ae60; font-weight: bold; }
                    .recipe-available { color: #27ae60; font-weight: bold; }
                    .recipe-unavailable { color: #e74c3c; font-weight: bold; }
                    .code-example {
                        background: #2c3e50;
                        color: #ecf0f1;
                        padding: 20px;
                        border-radius: 8px;
                        font-family: 'Courier New', monospace;
                        margin: 15px 0;
                        overflow-x: auto;
                    }
                    .action-steps {
                        background: #ecf0f1;
                        padding: 20px;
                        border-radius: 8px;
                        margin: 20px 0;
                    }
                    .action-steps ol {
                        margin: 0;
                        padding-left: 20px;
                    }
                    .action-steps li {
                        margin: 10px 0;
                    }
                    .footer {
                        margin-top: 60px;
                        padding-top: 30px;
                        border-top: 1px solid #e1e8ed;
                        text-align: center;
                        color: #7f8c8d;
                        font-size: 0.9em;
                    }
                    @media print {
                        body { padding: 20px; }
                        .container { box-shadow: none; }
                    }
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
                getSharedHeaderStyles(),
                getSharedFooterStyles(),
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
    
    private String getDependencySummary(int total, int compatible) {
        int requiringUpdate = total - compatible;
        return String.format("%d total (%d compatible, %d requiring update)", total, compatible, requiringUpdate);
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
            html.append("<td>").append(escapeHtml(node.artifactId() != null ? node.artifactId() : "Unknown")).append("</td>");
            html.append("<td>").append(escapeHtml(node.version() != null ? node.version() : "Unknown")).append("</td>");
            html.append("<td class='").append(isCompatible ? "compatible" : "incompatible").append("'>");
            html.append(isCompatible ? "Jakarta Compatible" : "Requires Update");
            html.append("</td>");
            html.append("<td>").append(escapeHtml(isCompatible ? "No action needed" : "Update to Jakarta EE equivalent")).append("</td>");
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
            html.append("<strong>").append(escapeHtml(name)).append("</strong><br>");
            html.append("<em>Severity: ").append(escapeHtml(severity)).append("</em><br>");
            html.append("<strong>Impact:</strong> ").append(escapeHtml(impact)).append("<br>");
            html.append("<strong>Remediation:</strong> ").append(escapeHtml(remediation));
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
                    html.append("<div class='timeline-phase'>").append(escapeHtml(phaseName)).append("</div>");
                    html.append("<div class='timeline-duration'>").append(escapeHtml(description)).append("</div>");
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
            html.append("<td>").append(escapeHtml(file)).append("</td>");
            html.append("<td>").append(escapeHtml(line)).append("</td>");
            html.append("<td>").append(escapeHtml(reference)).append("</td>");
            html.append("<td>").append(escapeHtml(priority.toUpperCase())).append("</td>");
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
            html.append("<td>").append(escapeHtml(name)).append("</td>");
            html.append("<td>").append(escapeHtml(category)).append("</td>");
            html.append("<td>").append(escapeHtml(description)).append("</td>");
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
    private String generateSharedHeader(String reportTitle, String projectName, String reportType) {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        String pluginIconSvg = getPluginIconSvg();
        
        return """
            <div class="report-header">
                <div class="header-left">
                    <div class="plugin-icon">
                        %s
                    </div>
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
                escapeHtml(reportTitle),
                escapeHtml(projectName),
                escapeHtml(reportType),
                timestamp
            );
    }
    
    private String generateSharedFooter(String reportType, int pageNumber, int totalPages) {
        return """
            <div class="report-footer">
                <div class="footer-left">
                    <div class="plugin-info">Jakarta Migration Tool v3.0</div>
                </div>
                <div class="footer-center">
                    <div class="page-info">Page %d of %d</div>
                </div>
                <div class="footer-right">
                    <div class="report-type-footer">%s</div>
                </div>
            </div>
            """.formatted(
                pageNumber,
                totalPages,
                escapeHtml(reportType)
            );
    }
    
    private String getSharedHeaderStyles() {
        return """
            /* Shared Header Styles */
            .report-header {
                display: flex;
                justify-content: space-between;
                align-items: center;
                padding: 20px 0;
                border-bottom: 3px solid #2c3e50;
                margin-bottom: 40px;
                background: linear-gradient(135deg, #f8f9fa 0%%, #e9ecef 100%%);
                border-radius: 8px 8px 0 0;
            }
            
            .header-left {
                flex: 0 0 auto;
                display: flex;
                align-items: center;
            }
            
            .plugin-icon {
                width: 32px;
                height: 32px;
                display: flex;
                align-items: center;
                justify-content: center;
            }
            
            .plugin-icon svg {
                width: 100%%;
                height: 100%;
                max-width: 32px;
                max-height: 32px;
            }
            
            .header-center {
                flex: 1 1 auto;
                text-align: center;
                padding: 0 20px;
            }
            
            .report-title {
                color: #2c3e50;
                font-size: 2.2em;
                margin: 0;
                font-weight: 300;
                line-height: 1.2;
            }
            
            .project-name {
                color: #34495e;
                font-size: 1.4em;
                margin: 8px 0 4px 0;
                font-weight: 400;
            }
            
            .report-type {
                color: #7f8c8d;
                font-size: 1.1em;
                font-style: italic;
                margin: 0;
            }
            
            .header-right {
                flex: 0 0 auto;
                text-align: right;
            }
            
            .timestamp {
                color: #7f8c8d;
                font-size: 0.9em;
                font-weight: 500;
            }
            """;
    }
    
    private String getSharedFooterStyles() {
        return """
            /* Shared Footer Styles */
            .report-footer {
                display: flex;
                justify-content: space-between;
                align-items: center;
                padding: 20px 0;
                border-top: 2px solid #e1e8ed;
                margin-top: 60px;
                background: #f8f9fa;
                border-radius: 0 0 8px 8px;
                position: relative;
            }
            
            .footer-left {
                flex: 0 0 auto;
            }
            
            .plugin-info {
                color: #2c3e50;
                font-size: 0.9em;
                font-weight: 600;
            }
            
            .footer-center {
                flex: 1 1 auto;
                text-align: center;
            }
            
            .page-info {
                color: #7f8c8d;
                font-size: 0.85em;
                font-weight: 500;
            }
            
            .footer-right {
                flex: 0 0 auto;
                text-align: right;
            }
            
            .report-type-footer {
                color: #7f8c8d;
                font-size: 0.85em;
                font-weight: 500;
                text-transform: uppercase;
                letter-spacing: 0.5px;
            }
            
            /* Print-specific styles for PDF generation */
            @media print {
                .report-header {
                    position: fixed;
                    top: 0;
                    left: 0;
                    right: 0;
                    height: auto;
                    z-index: 1000;
                }
                
                .report-footer {
                    position: fixed;
                    bottom: 0;
                    left: 0;
                    right: 0;
                    height: auto;
                    z-index: 1000;
                }
                
                body {
                    margin-top: 120px;
                    margin-bottom: 80px;
                }
            }
            """;
    }
    
    String getPluginIconSvg() {
        // Simplified SVG icon for embedding - using a clean, scalable version
        // All XML entities must be properly escaped for Flying Saucer
        return """
            <svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg" style="display: block;">
                <defs>
                    <linearGradient id="iconGrad1" x1="0%" y1="0%" x2="100%" y2="100%">
                        <stop offset="0%" style="stop-color:#3498db;stop-opacity:1" />
                        <stop offset="100%" style="stop-color:#2c3e50;stop-opacity:1" />
                    </linearGradient>
                </defs>
                <rect x="2" y="2" width="20" height="20" rx="4" fill="url(#iconGrad1)"/>
                <path d="M8 12h8M12 8v8" stroke="white" stroke-width="2" stroke-linecap="round"/>
                <circle cx="12" cy="12" r="6" stroke="white" stroke-width="1.5" fill="none" stroke-dasharray="2,2"/>
            </svg>
            """.replace("&", "&amp;")
              .replace("#", "&amp;#");
    }
    String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }
    
    private void convertHtmlToPdf(String htmlContent, Path outputPath) throws Exception {
        try {
            // Validate HTML content before PDF conversion to catch XML parsing issues early
            HtmlValidator.validateHtml(htmlContent);
            
            // Ensure output directory exists
            Files.createDirectories(outputPath.getParent());
            
            // Create PDF renderer using Flying Saucer
            org.xhtmlrenderer.pdf.ITextRenderer renderer = new org.xhtmlrenderer.pdf.ITextRenderer();
            
            // Set document content
            renderer.setDocumentFromString(htmlContent);
            
            // Layout the document
            renderer.layout();
            
            // Create output stream and generate PDF
            try (OutputStream outputStream = new FileOutputStream(outputPath.toFile())) {
                renderer.createPDF(outputStream);
            }
            
            log.info("PDF successfully generated: {}", outputPath.toAbsolutePath());
            
            // Verify PDF was created
            if (Files.exists(outputPath) && Files.size(outputPath) > 0) {
                log.info("PDF file size: {} bytes", Files.size(outputPath));
            } else {
                throw new RuntimeException("PDF file was not created properly");
            }
            
        } catch (Exception e) {
            log.error("Error converting HTML to PDF: {}", e.getMessage(), e);
            
            // Fallback: save as HTML if PDF conversion fails
            Path htmlPath = outputPath.resolveSibling(outputPath.getFileName().toString().replace(".pdf", ".html"));
            Files.writeString(htmlPath, htmlContent);
            log.warn("PDF conversion failed, saved as HTML instead: {}", htmlPath);
            
            throw new RuntimeException("PDF conversion failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Migration strategy data for PDF reports.
     * Contains the 6 migration strategies with their details.
     */
    private record MigrationStrategyData(
        String name,
        String description,
        String benefits,
        String risks,
        String phases,
        String color,
        String useCase
    ) {}
    
    private List<MigrationStrategyData> getMigrationStrategies() {
        return Arrays.asList(
            new MigrationStrategyData(
                "Big Bang",
                "Migrate everything at once",
                "- Migrate all dependencies at once\n- Single comprehensive change\n- Best for small projects",
                "- Higher risk - issues affect entire codebase\n- Longer rollback time\n- Requires comprehensive testing",
                "1. Dependency Upgrade: Update all build files\n2. Code Refactor: Replace all javax.* imports\n3. XML/Config Update: Update configuration files\n4. Global Testing: Comprehensive testing",
                "#dc3545",
                "Small projects with low complexity and comprehensive testing capabilities"
            ),
            new MigrationStrategyData(
                "Incremental",
                "One dependency at a time",
                "- Migrate dependencies incrementally\n- Update one dependency, test, proceed\n- Lower risk per change\n- Best for large projects",
                "- Longer migration timeline\n- Must maintain compatibility\n- May require dual dependencies",
                "1. Dependency Scan: Identify javax dependencies\n2. Priority Ranking: Order by risk level\n3. Step-by-Step Upgrade: One artifact at a time\n4. Continuous Testing: Test after each change",
                "#ffc107",
                "Large projects with complex dependencies and need for risk mitigation"
            ),
            new MigrationStrategyData(
                "Transform",
                "Combined build and runtime transformation",
                "- Combine build and runtime approaches\n- Use OpenRewrite for automated changes\n- Deploy runtime adapters for edge cases",
                "- Most complex implementation\n- Requires build and runtime config\n- Higher resource overhead",
                "1. Recipe Selection: Choose Rewrite recipes\n2. Batch Execution: Run transformation across codebase\n3. Diff Review: Manual inspection of changes\n4. Final Validation: Automated test verification",
                "#17a2b8",
                "Projects requiring automation and mixed build/runtime approaches"
            ),
            new MigrationStrategyData(
                "Microservices",
                "Migrate each service independently",
                "- Migrate services one at a time\n- Each service can use different strategy\n- Independent deployment and testing",
                "- Requires coordination across services\n- Handle inter-service dependencies\n- May need service mesh updates",
                "1. Service Inventory: Map microservices and dependencies\n2. Dependency Analysis: Identify shared libraries\n3. Migration Planning: Order by dependency complexity\n4. Incremental Rollout: Deploy migrated services with legacy",
                "#6c757d",
                "Microservices architectures with independent service deployment"
            ),
            new MigrationStrategyData(
                "Adapter Pattern",
                "Use adapter classes for javax/jakarta compatibility",
                "- Maintain backward compatibility\n- Gradual replacement of javax with jakarta\n- Lower risk changes\n- Easy to rollback adapters",
                "- Additional code maintenance\n- Runtime overhead for adapters\n- More complex classpath management",
                "1. Adapter Config: Setup runtime bytecode instrumentation\n2. Runtime Proxy: Intercept javax calls, redirect to jakarta\n3. Legacy Support: Link old libraries to new EE runtime\n4. Monitor: Monitor performance and errors",
                "#6f42c1",
                "Projects requiring backward compatibility and gradual migration"
            ),
            new MigrationStrategyData(
                "Strangler",
                "Migrate module by module",
                "- Migrate one module at a time\n- New features built in Jakarta EE\n- Existing features gradually migrated\n- Good for monolithic applications",
                "- Requires inter-module compatibility layers\n- Can create duplicate logic during transition\n- Managing two EE environments simultaneously",
                "1. Interface Definition: Define module boundaries\n2. Bridge Setup: Create compatibility layer for cross-module calls\n3. Vertical Slices: Migrate one functional slice at a time\n4. Decommission: Remove legacy modules once replaced",
                "#28a745",
                "Monolithic applications with clear module boundaries"
            )
        );
    }
    
    private String generateMigrationStrategyTableHtml() {
        List<MigrationStrategyData> strategies = getMigrationStrategies();
        
        StringBuilder tableHtml = new StringBuilder();
        tableHtml.append("""
            <div class="strategy-table-container">
                <table class="strategy-comparison-table">
                    <thead>
                        <tr>
                            <th>Strategy</th>
                            <th>Description</th>
                            <th>Benefits</th>
                            <th>Risks</th>
                            <th>Implementation Phases</th>
                            <th>Best For</th>
                        </tr>
                    </thead>
                    <tbody>
            """);
        
        for (MigrationStrategyData strategy : strategies) {
            tableHtml.append(String.format("""
                <tr class="strategy-row">
                    <td class="strategy-name">
                        <div class="strategy-indicator" style="background-color: %s;"></div>
                        <strong>%s</strong>
                    </td>
                    <td class="strategy-description">%s</td>
                    <td class="strategy-benefits">%s</td>
                    <td class="strategy-risks">%s</td>
                    <td class="strategy-phases">%s</td>
                    <td class="strategy-use-case">%s</td>
                </tr>
            """, 
                strategy.color(),
                escapeHtml(strategy.name()),
                escapeHtml(strategy.description()),
                escapeHtml(strategy.benefits()).replace("\n", "<br>"),
                escapeHtml(strategy.risks()).replace("\n", "<br>"),
                escapeHtml(strategy.phases()).replace("\n", "<br>"),
                escapeHtml(strategy.useCase())
            ));
        }
        
        tableHtml.append("""
                    </tbody>
                </table>
            </div>
            """);
        
        return tableHtml.toString();
    }
    
    /**
     * Generates HTML content for consolidated reports.
     */
    private String generateConsolidatedHtml(ConsolidatedReportRequest request) {
        StringBuilder html = new StringBuilder();
        
        html.append("<html><head><title>")
           .append(escapeHtml(request.reportTitle()))
           .append("</title></head><body>")
           .append("<h1>")
           .append(escapeHtml(request.reportTitle()))
           .append("</h1>")
           .append("<h2>Project: ")
           .append(escapeHtml(request.projectName()))
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
                html.append("<li>").append(escapeHtml(recommendation)).append("</li>");
            }
            html.append("</ul>");
        }
        
        html.append("<p><em>Generated by Jakarta Migration Tool v3.0</em></p>")
           .append("</body></html>");
        
        return html.toString();
    }
}
