package adrianmikula.jakartamigration.pdfreporting.service.impl;

import adrianmikula.jakartamigration.pdfreporting.service.PdfReportService;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Dependency;
import adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults;
import adrianmikula.jakartamigration.platforms.model.PlatformScanResult;
import adrianmikula.jakartamigration.platforms.model.PlatformDetection;
import adrianmikula.jakartamigration.risk.RiskScoringService;
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
            if (request.platformScanResults() != null) {
                PDPage platformFindingsPage = new PDPage(PDRectangle.A4);
                document.addPage(platformFindingsPage);

                try (PDPageContentStream contentStream = new PDPageContentStream(document, platformFindingsPage)) {
                    generatePlatformFindingsSection(contentStream, request.platformScanResults());
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
            
            // 6. Detailed file findings section
            if (request.scanResults() != null && shouldIncludeDetailedFindings(request.template())) {
                PDPage detailedFindingsPage = new PDPage(PDRectangle.A4);
                document.addPage(detailedFindingsPage);
                
                try (PDPageContentStream contentStream = new PDPageContentStream(document, detailedFindingsPage)) {
                    generateDetailedFileFindingsSection(contentStream, request.scanResults());
                }
            }
            
            // 7. Footer containing support links (from the support UI tab)
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
            new ReportSection("detailedFileFindings", "Detailed File Findings", "Per-file scan results with migration suggestions", true, 
                Map.of("includeFilePaths", true, "includeMigrationSuggestions", true, "maxFilesPerCategory", 10)),
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

            // Calculate actual risk score and migration time from available data
            RiskScoringService riskService = RiskScoringService.getInstance();
            double riskScore = calculateRiskScoreFromRequest(request, riskService);
            String riskLevel = riskService.determineRiskLevel(riskScore);
            String migrationTime = riskService.formatMigrationTime(riskScore);

            contentStream.showText("Risk Assessment: " + riskLevel + " | Est. Migration Time: " + migrationTime);
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

        // Summary content based on actual scan data
        ComprehensiveScanResults scanResults = request.scanResults();
        double readinessScore = scanResults != null && scanResults.summary() != null
            ? scanResults.summary().readinessScore()
            : 0.0;

        // Determine migration readiness based on actual score
        String readinessStatus;
        if (readinessScore >= 80) {
            readinessStatus = "Ready";
        } else if (readinessScore >= 50) {
            readinessStatus = "Moderate Effort Required";
        } else if (readinessScore >= 25) {
            readinessStatus = "Significant Effort Required";
        } else {
            readinessStatus = "Not Started / Minimal";
        }

        contentStream.setFont(BODY_FONT, 12);

        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("Migration Readiness: " + readinessStatus);
        contentStream.endText();

        yPosition -= 20;

        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("Key Findings: Analysis complete - " + String.format("%.1f%%", readinessScore) + " readiness score");
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

        if (scanResults != null && scanResults.summary() != null) {
            int criticalIssues = scanResults.summary().criticalIssues();
            contentStream.beginText();
            contentStream.newLineAtOffset(MARGIN, yPosition);
            contentStream.showText("Critical Issues: " + criticalIssues);
            contentStream.endText();

            yPosition -= 20;
        }

        yPosition -= 20;

        // Generate dynamic assessment based on actual data
        String assessment;
        if (readinessScore >= 80) {
            assessment = "Project is ready for Jakarta EE migration";
        } else if (readinessScore >= 50) {
            assessment = "Project requires moderate effort for Jakarta EE migration";
        } else if (readinessScore >= 25) {
            assessment = "Project requires significant effort for Jakarta EE migration";
        } else {
            assessment = "Jakarta EE migration not yet started or minimal progress";
        }

        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("Overall Assessment: " + assessment);
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
    
    private void generateRecommendations(PDPageContentStream contentStream, GeneratePdfReportRequest request) throws IOException {
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

        // Build recommendations list from actual data sources
        List<String> recommendations = new ArrayList<>();

        // Use scan results recommendations if available
        if (request.scanResults() != null && request.scanResults().recommendations() != null) {
            recommendations.addAll(request.scanResults().recommendations());
        }

        // Use platform recommendations if available
        if (request.platformScanResults() != null && request.platformScanResults().recommendations() != null) {
            for (String rec : request.platformScanResults().recommendations()) {
                // Avoid duplicates
                if (!recommendations.contains(rec)) {
                    recommendations.add(rec);
                }
            }
        }

        // Fall back to generic recommendations if no specific data available
        if (recommendations.isEmpty()) {
            recommendations = Arrays.asList(
                "Update dependencies to Jakarta EE equivalents",
                "Apply migration recipes using automated tools",
                "Test applications thoroughly after migration",
                "Review and update configuration files",
                "Validate all Jakarta EE namespace imports",
                "Update build scripts and dependencies",
                "Run comprehensive integration tests",
                "Document migration changes for team"
            );
        }

        // Limit to top 8 recommendations to avoid overflow
        List<String> displayRecommendations = recommendations.stream()
            .limit(8)
            .toList();

        contentStream.setFont(BODY_FONT, 12);

        int index = 1;
        for (String recommendation : displayRecommendations) {
            if (yPosition < MARGIN + 50) {
                break; // Stop if we're running out of page space
            }
            contentStream.beginText();
            contentStream.newLineAtOffset(MARGIN, yPosition);
            contentStream.showText(index + ". " + recommendation);
            contentStream.endText();
            yPosition -= 20;
            index++;
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
        
        // Calculate actual risk score from available data using RiskScoringService
        RiskScoringService riskService = RiskScoringService.getInstance();
        double riskScore = calculateRiskScoreFromRequest(request, riskService);
        String riskLevel = riskService.determineRiskLevel(riskScore);
        String migrationTime = riskService.formatMigrationTime(riskScore);
        
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
     * Generates platform findings section using actual platform detection data
     */
    private void generatePlatformFindingsSection(PDPageContentStream contentStream, PlatformScanResult platformScanResults) throws IOException {
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

        // Display actual detected platforms
        contentStream.setFont(BODY_FONT, 12);

        if (platformScanResults == null || platformScanResults.detectedPlatforms() == null || platformScanResults.detectedPlatforms().isEmpty()) {
            contentStream.beginText();
            contentStream.newLineAtOffset(MARGIN, yPosition);
            contentStream.showText("No platforms detected in the project");
            contentStream.endText();
        } else {
            for (PlatformDetection detection : platformScanResults.detectedPlatforms()) {
                if (yPosition < MARGIN + 50) {
                    // Page would overflow, stop adding more
                    break;
                }

                String status = detection.isJakartaCompatible() ? "Compatible" : "Migration Required";
                String platformInfo = String.format("%s: %s v%s - %s",
                    detection.platformType(),
                    detection.platformName(),
                    detection.detectedVersion(),
                    status);

                contentStream.beginText();
                contentStream.newLineAtOffset(MARGIN, yPosition);
                contentStream.showText(platformInfo);
                contentStream.endText();

                // If not compatible, show required version
                if (!detection.isJakartaCompatible() && detection.minJakartaVersion() != null) {
                    yPosition -= 15;
                    contentStream.setFont(BODY_FONT, 10);
                    contentStream.beginText();
                    contentStream.newLineAtOffset(MARGIN + 20, yPosition);
                    contentStream.showText("Required for Jakarta EE: " + detection.minJakartaVersion() + "+");
                    contentStream.endText();
                    contentStream.setFont(BODY_FONT, 12);
                }

                yPosition -= 25;
            }
        }
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
     * Generates detailed file findings section showing per-file scan results
     * with file paths and migration suggestions.
     */
    private void generateDetailedFileFindingsSection(PDPageContentStream contentStream, ComprehensiveScanResults scanResults) throws IOException {
        float yPosition = PDRectangle.A4.getHeight() - MARGIN;

        // Section title
        contentStream.setFont(HEADER_FONT, 18);
        contentStream.setNonStrokingColor(HEADER_COLOR);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("DETAILED FILE FINDINGS");
        contentStream.endText();

        yPosition -= 30;

        // Draw underline
        contentStream.setLineWidth(1);
        contentStream.moveTo(MARGIN, yPosition);
        contentStream.lineTo(MARGIN + 250, yPosition);
        contentStream.stroke();

        yPosition -= 40;

        // Generate file findings for each scanner category
        yPosition = generateJpaFileFindings(contentStream, scanResults, yPosition);
        yPosition = generateBeanValidationFileFindings(contentStream, scanResults, yPosition);
        yPosition = generateServletJspFileFindings(contentStream, scanResults, yPosition);
        yPosition = generateThirdPartyLibFileFindings(contentStream, scanResults, yPosition);
        yPosition = generateTransitiveDepFileFindings(contentStream, scanResults, yPosition);
    }

    private float generateJpaFileFindings(PDPageContentStream contentStream, ComprehensiveScanResults scanResults, float yPosition) throws IOException {
        if (scanResults.jpaResults() == null || scanResults.jpaResults().isEmpty()) {
            return yPosition;
        }

        contentStream.setFont(HEADER_FONT, 14);
        contentStream.setNonStrokingColor(HEADER_COLOR);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("JPA Annotations");
        contentStream.endText();

        yPosition -= 20;

        contentStream.setFont(BODY_FONT, 10);
        contentStream.setNonStrokingColor(TEXT_COLOR);

        // Extract file results from jpaResults map
        Object jpaData = scanResults.jpaResults().get("fileResults");
        if (jpaData instanceof List<?> fileResults) {
            int count = 0;
            for (Object result : fileResults) {
                if (count >= 10) break; // Limit to 10 files per category

                if (result instanceof adrianmikula.jakartamigration.advancedscanning.domain.JpaScanResult jpaResult) {
                    if (jpaResult.hasJavaxUsage()) {
                        // File path
                        contentStream.beginText();
                        contentStream.newLineAtOffset(MARGIN, yPosition);
                        contentStream.showText(jpaResult.filePath().toString());
                        contentStream.endText();
                        yPosition -= 12;

                        // Annotations found
                        for (var annotation : jpaResult.annotations()) {
                            contentStream.beginText();
                            contentStream.newLineAtOffset(MARGIN + 20, yPosition);
                            String migration = annotation.hasJakartaEquivalent() ? " -> " + annotation.jakartaEquivalent() : "";
                            contentStream.showText("• " + annotation.annotationName() + migration);
                            contentStream.endText();
                            yPosition -= 10;
                        }
                        yPosition -= 8;
                        count++;
                    }
                }
            }
        }

        yPosition -= 15;
        return yPosition;
    }

    private float generateBeanValidationFileFindings(PDPageContentStream contentStream, ComprehensiveScanResults scanResults, float yPosition) throws IOException {
        if (scanResults.beanValidationResults() == null || scanResults.beanValidationResults().isEmpty()) {
            return yPosition;
        }

        contentStream.setFont(HEADER_FONT, 14);
        contentStream.setNonStrokingColor(HEADER_COLOR);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("Bean Validation");
        contentStream.endText();

        yPosition -= 20;

        contentStream.setFont(BODY_FONT, 10);
        contentStream.setNonStrokingColor(TEXT_COLOR);

        Object validationData = scanResults.beanValidationResults().get("fileResults");
        if (validationData instanceof List<?> fileResults) {
            int count = 0;
            for (Object result : fileResults) {
                if (count >= 10) break;

                if (result instanceof adrianmikula.jakartamigration.advancedscanning.domain.BeanValidationScanResult validationResult) {
                    if (validationResult.hasJavaxUsage()) {
                        contentStream.beginText();
                        contentStream.newLineAtOffset(MARGIN, yPosition);
                        contentStream.showText(validationResult.filePath().toString());
                        contentStream.endText();
                        yPosition -= 12;

                        for (var annotation : validationResult.annotations()) {
                            contentStream.beginText();
                            contentStream.newLineAtOffset(MARGIN + 20, yPosition);
                            String migration = annotation.hasJakartaEquivalent() ? " -> " + annotation.jakartaEquivalent() : "";
                            contentStream.showText("• " + annotation.annotationName() + migration);
                            contentStream.endText();
                            yPosition -= 10;
                        }
                        yPosition -= 8;
                        count++;
                    }
                }
            }
        }

        yPosition -= 15;
        return yPosition;
    }

    private float generateServletJspFileFindings(PDPageContentStream contentStream, ComprehensiveScanResults scanResults, float yPosition) throws IOException {
        if (scanResults.servletJspResults() == null || scanResults.servletJspResults().isEmpty()) {
            return yPosition;
        }

        contentStream.setFont(HEADER_FONT, 14);
        contentStream.setNonStrokingColor(HEADER_COLOR);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("Servlet/JSP");
        contentStream.endText();

        yPosition -= 20;

        contentStream.setFont(BODY_FONT, 10);
        contentStream.setNonStrokingColor(TEXT_COLOR);

        Object servletData = scanResults.servletJspResults().get("fileResults");
        if (servletData instanceof List<?> fileResults) {
            int count = 0;
            for (Object result : fileResults) {
                if (count >= 10) break;

                if (result instanceof adrianmikula.jakartamigration.advancedscanning.domain.ServletJspScanResult servletResult) {
                    if (servletResult.hasJavaxUsage()) {
                        contentStream.beginText();
                        contentStream.newLineAtOffset(MARGIN, yPosition);
                        contentStream.showText(servletResult.filePath().toString());
                        contentStream.endText();
                        yPosition -= 12;

                        for (var usage : servletResult.usages()) {
                            contentStream.beginText();
                            contentStream.newLineAtOffset(MARGIN + 20, yPosition);
                            String migration = usage.hasJakartaEquivalent() ? " -> " + usage.jakartaEquivalent() : "";
                            contentStream.showText("• " + usage.className() + migration);
                            contentStream.endText();
                            yPosition -= 10;
                        }
                        yPosition -= 8;
                        count++;
                    }
                }
            }
        }

        yPosition -= 15;
        return yPosition;
    }

    private float generateThirdPartyLibFileFindings(PDPageContentStream contentStream, ComprehensiveScanResults scanResults, float yPosition) throws IOException {
        if (scanResults.thirdPartyLibResults() == null || scanResults.thirdPartyLibResults().isEmpty()) {
            return yPosition;
        }

        contentStream.setFont(HEADER_FONT, 14);
        contentStream.setNonStrokingColor(HEADER_COLOR);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("Third-Party Libraries");
        contentStream.endText();

        yPosition -= 20;

        contentStream.setFont(BODY_FONT, 10);
        contentStream.setNonStrokingColor(TEXT_COLOR);

        Object libData = scanResults.thirdPartyLibResults().get("libraries");
        if (libData instanceof List<?> libraries) {
            int count = 0;
            for (Object lib : libraries) {
                if (count >= 10) break;

                if (lib instanceof adrianmikula.jakartamigration.advancedscanning.domain.ThirdPartyLibUsage libUsage) {
                    contentStream.beginText();
                    contentStream.newLineAtOffset(MARGIN, yPosition);
                    String replacement = libUsage.getSuggestedReplacement() != null ? " -> " + libUsage.getSuggestedReplacement() : "";
                    contentStream.showText("• " + libUsage.getCoordinates() + replacement);
                    contentStream.endText();
                    yPosition -= 12;
                    count++;
                }
            }
        }

        yPosition -= 15;
        return yPosition;
    }

    private float generateTransitiveDepFileFindings(PDPageContentStream contentStream, ComprehensiveScanResults scanResults, float yPosition) throws IOException {
        if (scanResults.transitiveDependencyResults() == null || scanResults.transitiveDependencyResults().isEmpty()) {
            return yPosition;
        }

        contentStream.setFont(HEADER_FONT, 14);
        contentStream.setNonStrokingColor(HEADER_COLOR);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("Transitive Dependencies");
        contentStream.endText();

        yPosition -= 20;

        contentStream.setFont(BODY_FONT, 10);
        contentStream.setNonStrokingColor(TEXT_COLOR);

        Object transData = scanResults.transitiveDependencyResults().get("fileResults");
        if (transData instanceof List<?> fileResults) {
            int count = 0;
            for (Object result : fileResults) {
                if (count >= 10) break;

                if (result instanceof adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyScanResult transResult) {
                    contentStream.beginText();
                    contentStream.newLineAtOffset(MARGIN, yPosition);
                    contentStream.showText(transResult.getFilePath().toString());
                    contentStream.endText();
                    yPosition -= 12;
                    count++;
                }
            }
        }

        yPosition -= 15;
        return yPosition;
    }

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
     * Determines if detailed file findings section should be included based on template configuration.
     */
    private boolean shouldIncludeDetailedFindings(ReportTemplate template) {
        if (template == null || template.sections() == null) {
            return false;
        }
        
        return template.sections().stream()
                .filter(section -> "detailedFileFindings".equals(section.id()))
                .findFirst()
                .map(ReportSection::enabled)
                .orElse(false);
    }
    
    /**
     * Helper method to calculate risk score from request data using RiskScoringService.
     * Extracts metrics from dependency graph and scan results.
     */
    private double calculateRiskScoreFromRequest(GeneratePdfReportRequest request, RiskScoringService riskService) {
        DependencyGraph graph = request.dependencyGraph();
        ComprehensiveScanResults scanResults = request.scanResults();

        int blockers = 0;
        int nonCompatibleDeps = 0;

        if (graph != null) {
            blockers = (int) countBlockers(graph);
            long totalDeps = graph.getNodes().size();
            long compatible = countJakartaCompatible(graph);
            nonCompatibleDeps = (int) (totalDeps - compatible);
        }

        int totalIssues = 0;
        int criticalIssues = 0;

        if (scanResults != null) {
            totalIssues = scanResults.totalIssuesFound();
            if (scanResults.summary() != null) {
                criticalIssues = scanResults.summary().criticalIssues();
            }
        }

        return riskService.calculateRiskScore(blockers, nonCompatibleDeps, totalIssues, criticalIssues);
    }
}
