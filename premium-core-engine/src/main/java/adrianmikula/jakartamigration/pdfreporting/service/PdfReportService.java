package adrianmikula.jakartamigration.pdfreporting.service;

import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyAnalysisReport;
import adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults;
import adrianmikula.jakartamigration.advancedscanning.service.ScanRecipeRecommendationService;
import adrianmikula.jakartamigration.platforms.model.EnhancedPlatformScanResult;
import adrianmikula.jakartamigration.risk.RiskScoringService;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Service for generating comprehensive PDF reports for Jakarta migration projects.
 * Provides detailed analysis, dependency information, and migration recommendations.
 */
public interface PdfReportService {
    
    /**
     * Generates a Risk Analysis Report containing complete risk assessment with full details.
     * Includes executive summary, risk dials and scoring, dependency analysis with compatibility matrix,
     * dependency graphs, scan counts by category, platform compatibility analysis, top blockers,
     * and implementation timeline with risk mitigation strategies.
     * 
     * @param request The risk analysis report generation request
     * @return Path to the generated PDF file
     */
    Path generateRiskAnalysisReport(RiskAnalysisReportRequest request);
    
    /**
     * Generates a Refactoring Action Report containing detailed refactoring guide.
     * Includes complete file-by-file inventory of javax references, OpenRewrite recipe suggestions,
     * grouped recommendations by dependency type, priority-ranked actions, code examples,
     * automated refactoring readiness assessment, and validation checklists.
     * 
     * @param request The refactoring action report generation request
     * @return Path to the generated PDF file
     */
    Path generateRefactoringActionReport(RefactoringActionReportRequest request);
    
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
        EnhancedPlatformScanResult platformScanResults,
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
    
    /**
     * Data class for risk analysis report generation requests.
     */
    record RiskAnalysisReportRequest(
        Path outputPath,
        String projectName,
        String reportTitle,
        DependencyGraph dependencyGraph,
        DependencyAnalysisReport analysisReport,
        ComprehensiveScanResults scanResults,
        EnhancedPlatformScanResult platformScanResults,
        RiskScoringService.RiskScore riskScore,
        String recommendedStrategy,
        Map<String, Object> strategyDetails,
        Map<String, Object> validationMetrics,
        List<Map<String, Object>> topBlockers,
        List<String> recommendations,
        Map<String, Object> implementationPhases,
        Map<String, Object> customData
    ) {}
    
    /**
     * Data class for refactoring action report generation requests.
     * Uses real scan data from ComprehensiveScanResults and RecipeRecommendationService.
     */
    record RefactoringActionReportRequest(
        Path outputPath,
        String projectName,
        String reportTitle,
        DependencyGraph dependencyGraph,
        ComprehensiveScanResults scanResults,
        List<ScanRecipeRecommendationService.RecipeRecommendation> recipeRecommendations,
        List<Map<String, Object>> javaxReferences,
        List<Map<String, Object>> openRewriteRecipes,
        Map<String, Object> refactoringReadiness,
        Map<String, Object> priorityRanking,
        Map<String, Object> customData
    ) {}
    
    /**
     * Generates a consolidated report combining multiple analysis types.
     * 
     * @param request The consolidated report generation request
     * @return Path to the generated PDF file
     */
    Path generateConsolidatedReport(ConsolidatedReportRequest request);
    
    /**
     * Data class for consolidated report generation requests.
     */
    record ConsolidatedReportRequest(
        Path outputPath,
        String projectName,
        String reportTitle,
        DependencyGraph dependencyGraph,
        DependencyAnalysisReport analysisReport,
        ComprehensiveScanResults scanResults,
        EnhancedPlatformScanResult platformScanResults,
        RiskScoringService.RiskScore riskScore,
        String recommendedStrategy,
        Map<String, Object> strategyDetails,
        Map<String, Object> validationMetrics,
        List<Map<String, Object>> topBlockers,
        List<String> recommendations,
        Map<String, Object> implementationPhases,
        Map<String, Object> customData
    ) {}
}
