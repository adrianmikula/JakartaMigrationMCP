package adrianmikula.jakartamigration.pdfreporting.snippet;

import adrianmikula.jakartamigration.pdfreporting.service.PdfReportService;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating HTML snippets for Risk Analysis reports.
 * Generates appropriate snippets based on available data.
 */
public class RiskAnalysisSnippetFactory {
    
    /**
     * Create a list of HTML snippets for a Risk Analysis report.
     * 
     * @param request The risk analysis report request
     * @return List of HTML snippets ordered for report generation
     */
    public List<HtmlSnippet> createSnippets(PdfReportService.RiskAnalysisReportRequest request) {
        List<HtmlSnippet> snippets = new ArrayList<>();
        
        // Header - always included
        snippets.add(new HeaderSnippet(
            request.projectName(), 
            request.reportTitle(), 
            "Risk Analysis Report"
        ));
        
        // Eclipse warning - only if no dependency graph
        snippets.add(new EclipseWarningSnippet(request.dependencyGraph() != null));
        
        // Executive summary for PMs/POs
        snippets.add(new ExecutiveSummarySnippet(
            request.projectName(),
            request.dependencyGraph(),
            request.scanResults(),
            request.riskScore()
        ));
        
        // Metrics summary - always included, adapts to available data
        snippets.add(new MetricsSummarySnippet(
            request.dependencyGraph(),
            request.scanResults(),
            request.riskScore()
        ));
        
        // Advanced scan summary - counts per category
        if (request.scanResults() != null) {
            AdvancedScanSummarySnippet advancedScanSummary = new AdvancedScanSummarySnippet(request.scanResults());
            if (advancedScanSummary.isApplicable()) {
                snippets.add(advancedScanSummary);
            }
        }
        
        // Risk dials - visual gauge representations with factor breakdowns
        snippets.add(new RiskDialsSnippet(
            request.riskScore(),
            request.scanResults(),
            request.dependencyGraph()
        ));
        
        // Enhanced snippets for comprehensive analysis
        snippets.add(new DependencyMatrixSnippet(request.dependencyGraph()));
        snippets.add(new PlatformDetectionSnippet(request.platformScanResults()));
        snippets.add(new RiskFindingsDetailSnippet(request.riskScore()));
        snippets.add(new ComponentScoreSnippet(request.riskScore()));
        snippets.add(new RiskHeatMapSnippet(
            request.dependencyGraph(),
            request.scanResults(),
            request.riskScore()
        ));
        
        // Migration strategies from property files
        var strategiesSnippet = new MigrationStrategiesSnippet();
        if (strategiesSnippet.isApplicable()) {
            snippets.add(strategiesSnippet);
        }
        
        // Strategy comparison table
        var comparisonSnippet = new StrategyComparisonSnippet();
        if (comparisonSnippet.isApplicable()) {
            snippets.add(comparisonSnippet);
        }
        
        return snippets;
    }
}
