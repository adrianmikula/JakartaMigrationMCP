package adrianmikula.jakartamigration.pdfreporting.service;

import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyAnalysisReport;
import adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults;
import adrianmikula.jakartamigration.config.JakartaMigrationConfigService;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Service for generating comprehensive PDF reports for Jakarta migration projects.
 * Provides detailed analysis, dependency information, and migration recommendations.
 */
public interface PdfReportService {
    
    /**
     * Generates a comprehensive PDF report containing:
     * - Dependency tree visualization
     * - Maven dependency lists
     * - Advanced scan results
     * - Support links and footer
     * 
     * @param request The report generation request
     * @return Path to the generated PDF file
     */
    Path generateComprehensiveReport(GeneratePdfReportRequest request);
    
    /**
     * Generates a dependency-focused PDF report.
     * 
     * @param dependencyGraph The dependency graph to include
     * @param outputPath Where to save the PDF
     * @return Path to the generated PDF file
     */
    Path generateDependencyReport(DependencyGraph dependencyGraph, Path outputPath);
    
    /**
     * Generates an advanced scanning results PDF report.
     * 
     * @param scanResults The comprehensive scan results
     * @param outputPath Where to save the PDF
     * @return Path to the generated PDF file
     */
    Path generateScanResultsReport(ComprehensiveScanResults scanResults, Path outputPath);
    
    /**
     * Validates that all required data is available for report generation.
     * 
     * @param request The report generation request
     * @return Validation result with any errors or warnings
     */
    ValidationResult validateReportRequest(GeneratePdfReportRequest request);
    
    /**
     * Gets the default report template.
     * 
     * @return The default template configuration
     */
    ReportTemplate getDefaultTemplate();
    
    /**
     * Creates a custom report template.
     * 
     * @param sections The sections to include in the template
     * @return A new report template
     */
    ReportTemplate createCustomTemplate(List<ReportSection> sections);
    
    /**
     * Data class for PDF report generation requests.
     */
    record GeneratePdfReportRequest(
        Path outputPath,
        DependencyGraph dependencyGraph,
        DependencyAnalysisReport analysisReport,
        ComprehensiveScanResults scanResults,
        ReportTemplate template,
        Map<String, Object> customData
    ) {}
    
    /**
     * Data class for validation results.
     */
    record ValidationResult(
        boolean isValid,
        List<ValidationError> errors,
        List<ValidationWarning> warnings
    ) {
        public static ValidationResult valid() {
            return new ValidationResult(true, List.of(), List.of());
        }
        
        public static ValidationResult invalid(List<ValidationError> errors) {
            return new ValidationResult(false, errors, List.of());
        }
        
        public static ValidationResult withWarnings(List<ValidationWarning> warnings) {
            return new ValidationResult(true, List.of(), warnings);
        }
    }
    
    /**
     * Data class for validation errors.
     */
    record ValidationError(
        String field,
        String message,
        String suggestion
    ) {}
    
    /**
     * Data class for validation warnings.
     */
    record ValidationWarning(
        String field,
        String message
    ) {}
    
    /**
     * Data class for report templates.
     */
    record ReportTemplate(
        String name,
        String description,
        List<ReportSection> sections,
        Map<String, Object> metadata
    ) {}
    
    /**
     * Data class for report sections.
     */
    record ReportSection(
        String id,
        String title,
        String description,
        boolean enabled,
        Map<String, Object> configuration
    ) {}
}
