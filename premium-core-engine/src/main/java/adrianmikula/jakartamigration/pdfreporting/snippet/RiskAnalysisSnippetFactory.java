package adrianmikula.jakartamigration.pdfreporting.snippet;

import adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.pdfreporting.service.PdfReportService;
import adrianmikula.jakartamigration.risk.RiskScoringService;

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
        
        // Metrics summary - always included, adapts to available data
        snippets.add(new MetricsSummarySnippet(
            request.dependencyGraph(),
            request.scanResults(),
            request.riskScore()
        ));
        
        // Enhanced snippets for comprehensive analysis
        snippets.add(new DependencyMatrixSnippet(request.dependencyGraph()));
        snippets.add(new CodeExamplesSnippet());
        snippets.add(new RiskHeatMapSnippet(
            request.dependencyGraph(), 
            request.scanResults(), 
            request.riskScore()
        ));
        snippets.add(new ImplementationRoadmapSnippet());
        
        return snippets;
    }
}
