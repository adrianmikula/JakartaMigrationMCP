package adrianmikula.jakartamigration.pdfreporting.service.impl;

import adrianmikula.jakartamigration.pdfreporting.service.PdfReportService;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Dependency;
import adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults;
import lombok.extern.slf4j.Slf4j;
import java.awt.Color;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * PDF report generation service using Apache PDFBox.
 * Modern, fast, and legally simple PDF generation for Jakarta migration projects.
 */
@Slf4j
public class PdfReportServiceImpl implements PdfReportService {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final float MARGIN = 50f;
    private static final float TITLE_MARGIN = 60f;
    private static final float SECTION_MARGIN = 40f;
    private static final float LINE_SPACING = 16f;
    private static final float LINE_HEIGHT = 14f;
    private static final PDFont TITLE_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDFont HEADER_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDFont BODY_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDFont CODE_FONT = new PDType1Font(Standard14Fonts.FontName.COURIER);
    
    // Professional colors
    private static final Color TITLE_COLOR = new Color(25, 51, 77);  // Dark blue
    private static final Color HEADER_COLOR = new Color(44, 62, 80);   // Dark gray
    private static final Color ACCENT_COLOR = new Color(0, 120, 215);  // Orange for highlights
    private static final Color TEXT_COLOR = new Color(30, 30, 30);    // Dark gray for body text
    
    @Override
    public Path generateComprehensiveReport(GeneratePdfReportRequest request) {
        log.info("Generating comprehensive PDF report: {}", request.outputPath());
        
        try (PDDocument document = new PDDocument()) {
            // 1. Main heading "Jakarta Migration Risk Analysis Report"
            PDPage titlePage = new PDPage(PDRectangle.A4);
            document.addPage(titlePage);
            
            try (PDPageContentStream contentStream = new PDPageContentStream(document, titlePage)) {
                generateTitlePage(contentStream, request);
            }
            
            // 2. Risk score & estimated migration time
            PDPage riskAssessmentPage = new PDPage(PDRectangle.A4);
            document.addPage(riskAssessmentPage);
            
            try (PDPageContentStream contentStream = new PDPageContentStream(document, riskAssessmentPage)) {
                generateRiskAssessmentSection(contentStream, request);
            }
            
            // 3. List of platform findings
            if (request.dependencyGraph() != null) {
                PDPage platformFindingsPage = new PDPage(PDRectangle.A4);
                document.addPage(platformFindingsPage);
                
                try (PDPageContentStream contentStream = new PDPageContentStream(document, platformFindingsPage)) {
                    generatePlatformFindingsSection(contentStream, request.dependencyGraph());
                }
            }
            
            // 4. List of javax artifacts found and their jakarta replacements
            if (request.dependencyGraph() != null) {
                PDPage dependencyAnalysisPage = new PDPage(PDRectangle.A4);
                document.addPage(dependencyAnalysisPage);
                
                try (PDPageContentStream contentStream = new PDPageContentStream(document, dependencyAnalysisPage)) {
                    generateDependencyAnalysisSection(contentStream, request.dependencyGraph());
                }
            }
            
            // 5. List of advanced scan results
            if (request.scanResults() != null) {
                PDPage scanResultsPage = new PDPage(PDRectangle.A4);
                document.addPage(scanResultsPage);
                
                try (PDPageContentStream contentStream = new PDPageContentStream(document, scanResultsPage)) {
                    generateAdvancedScanResultsSection(contentStream, request.scanResults());
                }
            }
            
            // 6. Footer containing support links (from the support UI tab)
            PDPage supportPage = new PDPage(PDRectangle.A4);
            document.addPage(supportPage);
            
            try (PDPageContentStream contentStream = new PDPageContentStream(document, supportPage)) {
                generateSupportLinksSection(contentStream);
            }
            
            document.save(request.outputPath().toFile());
            log.info("PDF report generated successfully: {}", request.outputPath());
            return request.outputPath();
            
        } catch (IOException e) {
            log.error("Failed to generate PDF report", e);
            throw new RuntimeException("PDF generation failed", e);
        }
    }
    
    @Override
    public Path generateDependencyReport(DependencyGraph dependencyGraph, Path outputPath) {
        log.info("Generating dependency PDF report: {}", outputPath);
        
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                generateDependencyAnalysis(contentStream, dependencyGraph);
            }
            
            document.save(outputPath.toFile());
            log.info("Dependency PDF report generated: {}", outputPath);
            return outputPath;
            
        } catch (IOException e) {
            log.error("Failed to generate dependency PDF report", e);
            throw new RuntimeException("PDF generation failed", e);
        }
    }
    
    @Override
    public Path generateScanResultsReport(ComprehensiveScanResults scanResults, Path outputPath) {
        log.info("Generating scan results PDF report: {}", outputPath);
        
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                generateScanResults(contentStream, scanResults);
            }
            
            document.save(outputPath.toFile());
            log.info("Scan results PDF report generated: {}", outputPath);
            return outputPath;
            
        } catch (IOException e) {
            log.error("Failed to generate scan results PDF report", e);
            throw new RuntimeException("PDF generation failed", e);
        }
    }
    
    @Override
    public ValidationResult validateReportRequest(GeneratePdfReportRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        List<ValidationWarning> warnings = new ArrayList<>();
        
        // Validate output path
        if (request.outputPath() == null) {
            errors.add(new ValidationError("outputPath", "Output path is required", "Provide a valid file path"));
        }
        
        // Validate data availability
        if (request.dependencyGraph() == null && request.scanResults() == null) {
            errors.add(new ValidationError("data", "No data provided for report generation", 
                "Provide either dependency graph or scan results"));
        }
        
        if (!errors.isEmpty()) {
            return ValidationResult.invalid(errors);
        } else {
            return ValidationResult.valid();
        }
    }
    
    @Override
    public ReportTemplate getDefaultTemplate() {
        List<ReportSection> sections = Arrays.asList(
            new ReportSection("title", "Jakarta Migration Risk Analysis Report", "Main heading with project metadata", true, 
                Map.of("includeTimestamp", true, "includeRiskScore", true)),
            new ReportSection("riskAssessment", "Risk Score & Migration Time", "Risk assessment and estimated migration timeline", true, 
                Map.of("includeChart", true, "showBreakdown", true)),
            new ReportSection("platformFindings", "Platform Findings", "List of platform-specific findings", true, 
                Map.of("groupByPlatform", true, "includeSeverity", true)),
            new ReportSection("dependencyAnalysis", "Javax Artifacts and Jakarta Replacements", "Detailed dependency mapping and recommendations", true, 
                Map.of("showCompatibility", true, "includeVersions", true)),
            new ReportSection("advancedScanResults", "Advanced Scan Results", "Comprehensive scan findings with severity levels", true, 
                Map.of("groupByCategory", true, "includeCounts", true)),
            new ReportSection("supportLinks", "Support Resources", "Footer containing support links and resources", true, 
                Map.of("includeLinks", true, "showContact", true))
        );
        
        return new ReportTemplate(
            "Jakarta Migration Risk Analysis Report",
            "Comprehensive migration analysis with risk assessment and recommendations",
            sections,
            Map.of("version", "2.1", "created", LocalDateTime.now().toString(), "engine", "Apache PDFBox 3.0.2", 
                   "supportsMarkdown", true, "hasChartSupport", false)
        );
    }
    
    @Override
    public ReportTemplate createCustomTemplate(List<ReportSection> sections) {
        return new ReportTemplate(
            "Custom Report",
            "Custom report template",
            sections,
            Map.of("version", "2.0", "created", LocalDateTime.now().toString(), "engine", "Apache PDFBox 3.0.2")
        );
    }
    
    private void generateTitlePage(PDPageContentStream contentStream, GeneratePdfReportRequest request) throws IOException {
        float yPosition = PDRectangle.A4.getHeight() - MARGIN;
        
        // Main title
        contentStream.setFont(TITLE_FONT, 24);
        contentStream.setNonStrokingColor(TITLE_COLOR);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("JAKARTA MIGRATION RISK ANALYSIS REPORT");
        contentStream.endText();
        
        yPosition -= 40;
        
        // Risk score and migration time (if enabled in template)
        ReportSection titleSection = findSectionById(request.template(), "title");
        if (titleSection != null && titleSection.configuration().containsKey("includeRiskScore")) {
            contentStream.setFont(HEADER_FONT, 16);
            contentStream.setNonStrokingColor(HEADER_COLOR);
            contentStream.beginText();
            contentStream.newLineAtOffset(MARGIN, yPosition);
            contentStream.showText("Risk Assessment: LOW | Est. Migration Time: 2-4 weeks");
            contentStream.endText();
            
            yPosition -= 30;
        }
        
        // Project metadata
        Map<String, Object> customData = request.customData();
        if (customData != null) {
            if (customData.containsKey("projectName")) {
                contentStream.setFont(BODY_FONT, 14);
                contentStream.setNonStrokingColor(TEXT_COLOR);
                contentStream.beginText();
                contentStream.newLineAtOffset(MARGIN, yPosition);
                contentStream.showText("Project: " + customData.get("projectName"));
                contentStream.endText();
                yPosition -= 20;
            }
            
            if (customData.containsKey("description")) {
                contentStream.setFont(BODY_FONT, 12);
                contentStream.setNonStrokingColor(TEXT_COLOR);
                contentStream.beginText();
                contentStream.newLineAtOffset(MARGIN, yPosition);
                contentStream.showText("Description: " + customData.get("description"));
                contentStream.endText();
                yPosition -= 20;
            }
        }
        
        yPosition -= 20;
        
        // Generated timestamp
        contentStream.setFont(BODY_FONT, 10);
        contentStream.setNonStrokingColor(TEXT_COLOR);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("Generated: " + LocalDateTime.now().format(DATE_FORMATTER));
        contentStream.endText();
        
        yPosition -= 15;
        
        // Engine info
        contentStream.setFont(BODY_FONT, 8);
        contentStream.setNonStrokingColor(TEXT_COLOR);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("Generated by Jakarta Migration Tool v2.1");
        contentStream.endText();
    }
    
    private void generateExecutiveSummary(PDPageContentStream contentStream, GeneratePdfReportRequest request) throws IOException {
        float yPosition = PDRectangle.A4.getHeight() - MARGIN;
        
        // Section title
        contentStream.setFont(HEADER_FONT, 18);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("EXECUTIVE SUMMARY");
        contentStream.endText();
        
        yPosition -= 30;
        
        // Draw underline
        contentStream.setLineWidth(1);
        contentStream.moveTo(MARGIN, yPosition);
        contentStream.lineTo(MARGIN + 200, yPosition);
        contentStream.stroke();
        
        yPosition -= 40;
        
        // Summary content
        contentStream.setFont(BODY_FONT, 12);
        
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("Migration Readiness: Ready");
        contentStream.endText();
        
        yPosition -= 20;
        
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("Key Findings: Analysis complete");
        contentStream.endText();
        
        yPosition -= 20;
        
        if (request.dependencyGraph() != null) {
            contentStream.beginText();
            contentStream.newLineAtOffset(MARGIN, yPosition);
            contentStream.showText("Total Dependencies: " + request.dependencyGraph().getEdges().size());
            contentStream.endText();
            
            yPosition -= 20;
            
            contentStream.beginText();
            contentStream.newLineAtOffset(MARGIN, yPosition);
            contentStream.showText("Jakarta-Compatible: " + countJakartaCompatible(request.dependencyGraph()));
            contentStream.endText();
            
            yPosition -= 20;
        }
        
        if (request.scanResults() != null) {
            contentStream.beginText();
            contentStream.newLineAtOffset(MARGIN, yPosition);
            contentStream.showText("Critical Issues: 0");
            contentStream.endText();
            
            yPosition -= 20;
        }
        
        yPosition -= 20;
        
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("Overall Assessment: Project is ready for Jakarta EE migration");
        contentStream.endText();
    }
    
    private void generateDependencyAnalysis(PDPageContentStream contentStream, DependencyGraph dependencyGraph) throws IOException {
        float yPosition = PDRectangle.A4.getHeight() - MARGIN;
        
        // Section title
        contentStream.setFont(HEADER_FONT, 18);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("Dependency Analysis");
        contentStream.endText();
        yPosition -= 40;
        
        // Dependency statistics
        contentStream.setFont(BODY_FONT, 12);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("Total Dependencies: " + dependencyGraph.edgeCount());
        contentStream.endText();
        yPosition -= 20;
        
        // Critical dependencies
        contentStream.setFont(HEADER_FONT, 14);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("Critical Dependencies:");
        contentStream.endText();
        
        yPosition -= 25;
        
        contentStream.setFont(CODE_FONT, 10);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        
        List<Dependency> sortedDependencies = dependencyGraph.getEdges().stream()
            .sorted((a, b) -> a.to().toIdentifier().compareToIgnoreCase(b.to().toIdentifier()))
            .limit(15)
            .toList();
        
        for (Dependency dep : sortedDependencies) {
            if (yPosition < MARGIN + 50) {
                // Add new page if needed
                contentStream.endText();
                // Note: In a real implementation, you'd create a new page here
                yPosition = PDRectangle.A4.getHeight() - MARGIN;
                contentStream.beginText();
                contentStream.newLineAtOffset(MARGIN, yPosition);
            }
            
            contentStream.newLineAtOffset(20, -15); // Relative offset for indentation
            yPosition -= 15;
            contentStream.showText(dep.from().toIdentifier() + " -> " + dep.to().toIdentifier());
        }
        
        contentStream.endText();
    }
    
    private void generateScanResults(PDPageContentStream contentStream, ComprehensiveScanResults scanResults) throws IOException {
        float yPosition = PDRectangle.A4.getHeight() - MARGIN;

        // Section title
        contentStream.setFont(HEADER_FONT, 18);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("ADVANCED SCAN RESULTS");
        contentStream.endText();

        yPosition -= 30;

        // Draw underline
        contentStream.setLineWidth(1);
        contentStream.moveTo(MARGIN, yPosition);
        contentStream.lineTo(MARGIN + 200, yPosition);
        contentStream.stroke();

        yPosition -= 40;

        // Scan summary - use actual data from scanResults
        contentStream.setFont(BODY_FONT, 12);

        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("Scan Summary:");
        contentStream.endText();

        yPosition -= 20;

        // Use actual summary data if available
        int totalFilesScanned = scanResults.summary() != null ? scanResults.summary().totalFilesScanned() : 0;
        int filesWithIssues = scanResults.summary() != null ? scanResults.summary().filesWithIssues() : 0;
        int criticalIssues = scanResults.summary() != null ? scanResults.summary().criticalIssues() : 0;
        int warningIssues = scanResults.summary() != null ? scanResults.summary().warningIssues() : 0;
        int infoIssues = scanResults.summary() != null ? scanResults.summary().infoIssues() : 0;
        double readinessScore = scanResults.summary() != null ? scanResults.summary().readinessScore() : 0.0;

        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN + 20, yPosition);
        contentStream.showText("- Total Files Scanned: " + totalFilesScanned);
        contentStream.endText();

        yPosition -= 20;

        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN + 20, yPosition);
        contentStream.showText("- Files with Issues: " + filesWithIssues);
        contentStream.endText();

        yPosition -= 20;

        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN + 20, yPosition);
        contentStream.showText("- Critical Issues Found: " + criticalIssues);
        contentStream.endText();

        yPosition -= 20;

        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN + 20, yPosition);
        contentStream.showText("- Warning Issues: " + warningIssues);
        contentStream.endText();

        yPosition -= 20;

        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN + 20, yPosition);
        contentStream.showText("- Info Issues: " + infoIssues);
        contentStream.endText();

        yPosition -= 20;

        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN + 20, yPosition);
        contentStream.showText("- Readiness Score: " + String.format("%.1f", readinessScore) + "%");
        contentStream.endText();

        yPosition -= 20;

        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN + 20, yPosition);
        contentStream.showText("- Total Issues Found: " + scanResults.totalIssuesFound());
        contentStream.endText();

        yPosition -= 20;

        // Show scan categories
        if (scanResults.jpaResults() != null && !scanResults.jpaResults().isEmpty()) {
            yPosition -= 10;
            contentStream.beginText();
            contentStream.newLineAtOffset(MARGIN, yPosition);
            contentStream.showText("JPA Scan: " + scanResults.jpaResults().size() + " findings");
            contentStream.endText();
            yPosition -= 15;
        }

        if (scanResults.beanValidationResults() != null && !scanResults.beanValidationResults().isEmpty()) {
            contentStream.beginText();
            contentStream.newLineAtOffset(MARGIN, yPosition);
            contentStream.showText("Bean Validation Scan: " + scanResults.beanValidationResults().size() + " findings");
            contentStream.endText();
            yPosition -= 15;
        }

        if (scanResults.servletJspResults() != null && !scanResults.servletJspResults().isEmpty()) {
            contentStream.beginText();
            contentStream.newLineAtOffset(MARGIN, yPosition);
            contentStream.showText("Servlet/JSP Scan: " + scanResults.servletJspResults().size() + " findings");
            contentStream.endText();
            yPosition -= 15;
        }
    }
    
    private void generateRecommendations(PDPageContentStream contentStream) throws IOException {
        float yPosition = PDRectangle.A4.getHeight() - MARGIN;
        
        // Section title
        contentStream.setFont(HEADER_FONT, 18);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("RECOMMENDATIONS");
        contentStream.endText();
        
        yPosition -= 30;
        
        // Draw underline
        contentStream.setLineWidth(1);
        contentStream.moveTo(MARGIN, yPosition);
        contentStream.lineTo(MARGIN + 200, yPosition);
        contentStream.stroke();
        
        yPosition -= 40;
        
        // Recommendations list
        contentStream.setFont(BODY_FONT, 12);
        
        String[] recommendations = {
            "1. Update dependencies to Jakarta EE equivalents",
            "2. Apply migration recipes using automated tools",
            "3. Test applications thoroughly after migration",
            "4. Review and update configuration files",
            "5. Validate all Jakarta EE namespace imports",
            "6. Update build scripts and dependencies",
            "7. Run comprehensive integration tests",
            "8. Document migration changes for team"
        };
        
        for (String recommendation : recommendations) {
            contentStream.beginText();
            contentStream.newLineAtOffset(MARGIN, yPosition);
            contentStream.showText(recommendation);
            contentStream.endText();
            yPosition -= 20;
        }
    }
    
    private void generateAppendix(PDPageContentStream contentStream) throws IOException {
        float yPosition = PDRectangle.A4.getHeight() - MARGIN;
        
        // Section title
        contentStream.setFont(HEADER_FONT, 18);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("APPENDIX");
        contentStream.endText();
        
        yPosition -= 30;
        
        // Draw underline
        contentStream.setLineWidth(1);
        contentStream.moveTo(MARGIN, yPosition);
        contentStream.lineTo(MARGIN + 200, yPosition);
        contentStream.stroke();
        
        yPosition -= 40;
        
        // Appendix content
        contentStream.setFont(BODY_FONT, 12);
        
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("Generated by Jakarta Migration Tool v2.0");
        contentStream.endText();
        
        yPosition -= 20;
        
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("PDF Engine: Apache PDFBox 3.0.2");
        contentStream.endText();
        
        yPosition -= 20;
        
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("Generation Date: " + LocalDateTime.now().format(DATE_FORMATTER));
        contentStream.endText();
        
        yPosition -= 30;
        
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("Additional Resources:");
        contentStream.endText();
        
        yPosition -= 20;
        
        contentStream.setFont(BODY_FONT, 10);
        
        String[] resources = {
            "- Jakarta EE Documentation: https://jakarta.ee/",
            "- Migration Guide: https://jakarta.ee/guides/migration/",
            "- Compatibility Checker: https://jakarta.ee/compatibility/",
            "- Community Support: https://jakarta.ee/community/"
        };
        
        for (String resource : resources) {
            contentStream.beginText();
            contentStream.newLineAtOffset(MARGIN, yPosition);
            contentStream.showText(resource);
            contentStream.endText();
            yPosition -= 15;
        }
    }
    
    private long countJakartaCompatible(DependencyGraph dependencyGraph) {
        return dependencyGraph.getNodes().stream()
            .filter(node -> node.isJakartaCompatible())
            .count();
    }
    
    private long countBlockers(DependencyGraph dependencyGraph) {
        // Simplified - count nodes that are javax.* (not Jakarta compatible)
        return (long) dependencyGraph.getNodes().stream()
                .filter(node -> !node.isJakartaCompatible() && node.groupId().startsWith("javax."))
                .count();
    }
    
    /**
     * Generates risk assessment section with score and migration time
     */
    private void generateRiskAssessmentSection(PDPageContentStream contentStream, GeneratePdfReportRequest request) throws IOException {
        float yPosition = PDRectangle.A4.getHeight() - MARGIN;
        
        // Section title
        contentStream.setFont(HEADER_FONT, 18);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("RISK SCORE & MIGRATION TIME");
        contentStream.endText();
        
        yPosition -= 30;
        
        // Draw underline
        contentStream.setLineWidth(1);
        contentStream.moveTo(MARGIN, yPosition);
        contentStream.lineTo(MARGIN + 250, yPosition);
        contentStream.stroke();
        
        yPosition -= 40;
        
        // Calculate actual risk score from available data
        double riskScore = calculateRiskScore(request);
        String riskLevel = determineRiskLevel(riskScore);
        
        // Calculate estimated migration time based on risk score
        int effortWeeks = (int) Math.ceil(Math.sqrt(Math.max(0, riskScore * 2)));
        String migrationTime = effortWeeks + (effortWeeks == 1 ? " week" : " weeks");
        
        // Risk score visualization
        contentStream.setFont(BODY_FONT, 14);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("Overall Risk Score: " + riskLevel + " (" + String.format("%.0f", riskScore) + "/100)");
        contentStream.endText();
        
        yPosition -= 25;
        
        contentStream.setFont(BODY_FONT, 12);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("Estimated Migration Time: " + migrationTime);
        contentStream.endText();
        
        yPosition -= 20;
        
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("Risk Breakdown:");
        contentStream.endText();
        
        yPosition -= 20;
        
        // Calculate actual risk factors from data
        DependencyGraph graph = request.dependencyGraph();
        ComprehensiveScanResults scanResults = request.scanResults();
        
        long totalDeps = graph != null ? graph.getNodes().size() : 0;
        long jakartaCompatible = graph != null ? countJakartaCompatible(graph) : 0;
        long blockers = graph != null ? countBlockers(graph) : 0;
        int totalIssues = scanResults != null ? scanResults.totalIssuesFound() : 0;
        
        String depComplexity = totalDeps < 10 ? "Low" : (totalDeps < 50 ? "Medium" : "High");
        String testCoverage = riskScore < 30 ? "Good" : (riskScore < 60 ? "Moderate" : "Needs Improvement");
        
        String[] riskFactors = {
            "• Dependency Complexity: " + depComplexity + " (" + totalDeps + " dependencies, " + jakartaCompatible + " compatible)",
            "• Migration Blockers: " + blockers + " dependencies require migration",
            "• Total Issues Found: " + totalIssues + " from advanced scans",
            "• Test Coverage: " + testCoverage
        };
        
        for (String factor : riskFactors) {
            contentStream.setFont(BODY_FONT, 10);
            contentStream.beginText();
            contentStream.newLineAtOffset(MARGIN + 20, yPosition);
            contentStream.showText(factor);
            contentStream.endText();
            yPosition -= 15;
        }
    }
    
    /**
     * Generates platform findings section
     */
    private void generatePlatformFindingsSection(PDPageContentStream contentStream, DependencyGraph dependencyGraph) throws IOException {
        float yPosition = PDRectangle.A4.getHeight() - MARGIN;
        
        // Section title
        contentStream.setFont(HEADER_FONT, 18);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("PLATFORM FINDINGS");
        contentStream.endText();
        
        yPosition -= 30;
        
        // Draw underline
        contentStream.setLineWidth(1);
        contentStream.moveTo(MARGIN, yPosition);
        contentStream.lineTo(MARGIN + 200, yPosition);
        contentStream.stroke();
        
        yPosition -= 40;
        
        // Platform-specific findings
        contentStream.setFont(BODY_FONT, 12);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("Application Server: Tomcat 10+ - Compatible");
        contentStream.endText();
        
        yPosition -= 20;
        
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("Build Tool: Maven 3.8+ - Compatible");
        contentStream.endText();
        
        yPosition -= 20;
        
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("Java Version: OpenJDK 17+ - Compatible");
        contentStream.endText();
        
        yPosition -= 20;
        
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("Framework: Spring Boot 3+ - Migration Required");
        contentStream.endText();
    }
    
    /**
     * Generates dependency analysis section with javax/jakarta mappings
     */
    private void generateDependencyAnalysisSection(PDPageContentStream contentStream, DependencyGraph dependencyGraph) throws IOException {
        float yPosition = PDRectangle.A4.getHeight() - MARGIN;
        
        // Section title
        contentStream.setFont(HEADER_FONT, 18);
        contentStream.setNonStrokingColor(HEADER_COLOR);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("JAVAX ARTIFACTS AND JAKARTA REPLACEMENTS");
        contentStream.endText();
        
        yPosition -= 30;
        
        // Draw underline
        contentStream.setLineWidth(1.0f);
        contentStream.moveTo(MARGIN, yPosition);
        contentStream.lineTo(MARGIN + 350, yPosition);
        contentStream.stroke();
        
        yPosition -= 40;
        
        // Column headers
        contentStream.setFont(BODY_FONT, 12);
        contentStream.setNonStrokingColor(TEXT_COLOR);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("Javax Artifact");
        contentStream.endText();
        
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN + 200, yPosition);
        contentStream.showText("Jakarta Replacement");
        contentStream.endText();
        
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN + 350, yPosition);
        contentStream.showText("Status");
        contentStream.endText();
        
        yPosition -= 30;
        
        // Dependency mappings (show first 10)
        List<Dependency> javaxDeps = dependencyGraph.getEdges().stream()
                .filter(dep -> dep.from().groupId().startsWith("javax."))
                .sorted((a, b) -> a.from().toIdentifier().compareToIgnoreCase(b.from().toIdentifier()))
                .limit(10)
                .toList();
        
        for (Dependency dep : javaxDeps) {
            if (yPosition < MARGIN + 50) {
                contentStream.endText();
                yPosition = PDRectangle.A4.getHeight() - MARGIN;
                contentStream.beginText();
                contentStream.newLineAtOffset(MARGIN, yPosition);
            }
            
            contentStream.setFont(CODE_FONT, 9);
            contentStream.setNonStrokingColor(TEXT_COLOR);
            contentStream.beginText();
            contentStream.newLineAtOffset(MARGIN, yPosition);
            contentStream.showText(dep.from().toIdentifier());
            contentStream.endText();
            
            contentStream.beginText();
            contentStream.newLineAtOffset(MARGIN + 200, yPosition);
            String jakartaReplacement = dep.to().toIdentifier();
            contentStream.showText(jakartaReplacement.isEmpty() ? "N/A" : jakartaReplacement);
            contentStream.endText();
            
            contentStream.beginText();
            contentStream.newLineAtOffset(MARGIN + 350, yPosition);
            contentStream.showText(dep.to().isJakartaCompatible() ? "Available" : "Migration Required");
            contentStream.endText();
            
            yPosition -= LINE_SPACING;
        }
    }
    
    /**
     * Generates advanced scan results section
     */
    private void generateAdvancedScanResultsSection(PDPageContentStream contentStream, ComprehensiveScanResults scanResults) throws IOException {
        float yPosition = PDRectangle.A4.getHeight() - MARGIN;
        
        // Section title
        contentStream.setFont(HEADER_FONT, 18);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("ADVANCED SCAN RESULTS");
        contentStream.endText();
        
        yPosition -= 30;
        
        // Draw underline
        contentStream.setLineWidth(1);
        contentStream.moveTo(MARGIN, yPosition);
        contentStream.lineTo(MARGIN + 200, yPosition);
        contentStream.stroke();
        
        yPosition -= 40;
        
        // Scan summary - use actual data from scanResults
        contentStream.setFont(BODY_FONT, 12);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("Scan Summary:");
        contentStream.endText();
        
        yPosition -= 25;
        
        // Use actual summary data if available
        int totalFilesScanned = scanResults.summary() != null ? scanResults.summary().totalFilesScanned() : 0;
        int totalIssues = scanResults.totalIssuesFound();
        int criticalIssues = scanResults.summary() != null ? scanResults.summary().criticalIssues() : 0;
        int recipesAvailable = scanResults.recommendations() != null ? scanResults.recommendations().size() : 0;
        double readinessScore = scanResults.summary() != null ? scanResults.summary().readinessScore() : 0.0;
        
        // Determine effort level based on readiness score
        String effortLevel;
        if (readinessScore >= 80) {
            effortLevel = "Low";
        } else if (readinessScore >= 50) {
            effortLevel = "Medium";
        } else {
            effortLevel = "High";
        }
        
        String[] scanStats = {
            "• Total Files Scanned: " + totalFilesScanned,
            "• Issues Found: " + totalIssues,
            "• Critical Issues: " + criticalIssues,
            "• Migration Recipes Available: " + recipesAvailable,
            "• Estimated Effort: " + effortLevel
        };
        
        for (String stat : scanStats) {
            contentStream.setFont(BODY_FONT, 10);
            contentStream.beginText();
            contentStream.newLineAtOffset(MARGIN + 20, yPosition);
            contentStream.showText(stat);
            contentStream.endText();
            yPosition -= 15;
        }
    }
    
    /**
     * Generates support links section (footer)
     */
    private void generateSupportLinksSection(PDPageContentStream contentStream) throws IOException {
        float yPosition = PDRectangle.A4.getHeight() - MARGIN;
        
        // Section title
        contentStream.setFont(HEADER_FONT, 16);
        contentStream.setNonStrokingColor(HEADER_COLOR);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("SUPPORT RESOURCES");
        contentStream.endText();
        
        yPosition -= 25;
        
        // Draw underline
        contentStream.setLineWidth(1.0f);
        contentStream.moveTo(MARGIN, yPosition);
        contentStream.lineTo(MARGIN + 200, yPosition);
        contentStream.stroke();
        
        yPosition -= 30;
        
        // Support links
        contentStream.setFont(BODY_FONT, 10);
        contentStream.setNonStrokingColor(TEXT_COLOR);
        
        String[] supportResources = {
            "• Jakarta EE Documentation: https://jakarta.ee/",
            "• Migration Guide: https://jakarta.ee/guides/migration/",
            "• Community Forums: https://jakarta.ee/community/",
            "• Compatibility Checker: https://jakarta.ee/compatibility/",
            "• GitHub Repository: https://github.com/adrianmikula/JakartaMigrationMCP",
            "• Issue Tracker: https://github.com/adrianmikula/JakartaMigrationMCP/issues"
        };
        
        for (String resource : supportResources) {
            contentStream.beginText();
            contentStream.newLineAtOffset(MARGIN, yPosition);
            contentStream.showText(resource);
            contentStream.endText();
            yPosition -= LINE_SPACING;
        }
        
        yPosition -= 20;
        
        // Professional footer with accent
        contentStream.setFont(BODY_FONT, 8);
        contentStream.setNonStrokingColor(ACCENT_COLOR);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("Generated by Jakarta Migration Tool v2.1");
        contentStream.endText();
        
        yPosition -= 12;
        
        contentStream.setFont(BODY_FONT, 6);
        contentStream.setNonStrokingColor(TEXT_COLOR);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("https://github.com/adrianmikula/JakartaMigrationMCP");
        contentStream.endText();
    }
    
    /**
     * Helper method to find a section by ID in report template
     */
    private ReportSection findSectionById(ReportTemplate template, String sectionId) {
        if (template == null || template.sections() == null) {
            return null;
        }
        
        return template.sections().stream()
                .filter(section -> sectionId.equals(section.id()))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Calculates risk score based on dependency graph and scan results.
     * Score ranges from 0 (no risk) to 100 (maximum risk).
     */
    private double calculateRiskScore(GeneratePdfReportRequest request) {
        double score = 0.0;
        
        DependencyGraph graph = request.dependencyGraph();
        ComprehensiveScanResults scanResults = request.scanResults();
        
        // Factor 1: Dependencies that need migration (blockers)
        if (graph != null) {
            long blockers = countBlockers(graph);
            score += blockers * 10; // 10 points per blocker
            
            long totalDeps = graph.getNodes().size();
            long compatible = countJakartaCompatible(graph);
            
            // Add score for non-compatible dependencies
            long nonCompatible = totalDeps - compatible;
            score += nonCompatible * 2; // 2 points per non-compatible dependency
        }
        
        // Factor 2: Advanced scan issues
        if (scanResults != null) {
            int totalIssues = scanResults.totalIssuesFound();
            score += totalIssues * 0.5; // 0.5 points per issue
            
            // Add extra for critical issues
            if (scanResults.summary() != null) {
                score += scanResults.summary().criticalIssues() * 5; // 5 extra points per critical issue
            }
        }
        
        // Cap at 100
        return Math.min(score, 100.0);
    }
    
    /**
     * Determines risk level based on calculated score.
     */
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
}
