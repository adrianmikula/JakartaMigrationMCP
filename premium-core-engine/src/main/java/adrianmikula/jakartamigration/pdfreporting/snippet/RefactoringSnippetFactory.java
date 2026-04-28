package adrianmikula.jakartamigration.pdfreporting.snippet;

import adrianmikula.jakartamigration.advancedscanning.service.ScanRecipeRecommendationService;
import adrianmikula.jakartamigration.pdfreporting.service.PdfReportService;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating HTML snippets for Refactoring Action reports.
 * Generates snippets using real scan data from ComprehensiveScanResults.
 */
public class RefactoringSnippetFactory {

    /**
     * Create a list of HTML snippets for a Refactoring Action report.
     * All snippets use real scan data - no estimates or hardcoded values.
     *
     * @param request The refactoring action report request with real scan data
     * @return List of HTML snippets ordered for report generation
     */
    public List<HtmlSnippet> createSnippets(PdfReportService.RefactoringActionReportRequest request) {
        List<HtmlSnippet> snippets = new ArrayList<>();

        // Header - always included
        snippets.add(new HeaderSnippet(
            request.projectName(),
            request.reportTitle(),
            "Refactoring Action Report"
        ));

        // Quick Wins - simple javax→jakarta renames to build confidence
        if (request.scanResults() != null) {
            QuickWinsSnippet quickWins = new QuickWinsSnippet(request.scanResults());
            if (quickWins.isApplicable()) {
                snippets.add(quickWins);
            }
        }

        // Code Change Suggestions - before/after examples for each finding
        if (request.scanResults() != null) {
            snippets.add(new CodeChangeSuggestionSnippet(request.scanResults()));
        }

        // Scan Coverage - real statistics from ScanSummary
        if (request.scanResults() != null) {
            snippets.add(new ScanCoverageSnippet(request.scanResults()));
        }

        // JPA Findings - real javax.persistence.* scan results
        if (request.scanResults() != null) {
            snippets.add(new JpaFindingsSnippet(request.scanResults()));
        }

        // CDI Findings - real javax.inject scan results
        if (request.scanResults() != null) {
            snippets.add(new CdiFindingsSnippet(request.scanResults()));
        }

        // Transitive Dependency Analysis - shows javax transitive deps
        if (request.scanResults() != null) {
            snippets.add(new TransitiveDependencySnippet(request.scanResults()));
        }

        // Library Migration Guide - real third-party library analysis
        if (request.scanResults() != null) {
            snippets.add(new LibraryMigrationSnippet(request.scanResults()));
        }

        // Servlet/JSP Findings - real javax.servlet.* scan results
        if (request.scanResults() != null) {
            snippets.add(new ServletJspFindingsSnippet(request.scanResults()));
        }

        // Security API Findings - real javax.security.* scan results
        if (request.scanResults() != null) {
            snippets.add(new SecurityApiFindingsSnippet(request.scanResults()));
        }

        // Build File References - pom.xml/build.gradle dependency updates
        if (request.scanResults() != null) {
            snippets.add(new BuildFileReferenceSnippet(request.scanResults()));
        }

        // REST/SOAP Services - JAX-RS and JAX-WS findings
        if (request.scanResults() != null) {
            snippets.add(new RestSoapFindingsSnippet(request.scanResults()));
        }

        // Deprecated API Findings - deprecated javax.* APIs
        if (request.scanResults() != null) {
            snippets.add(new DeprecatedApiFindingsSnippet(request.scanResults()));
        }

        // Docker & CI/CD Configuration - Java references in config files
        if (request.scanResults() != null) {
            snippets.add(new DockerCicdFindingsSnippet(request.scanResults()));
        }

        // Reflection Usage - patterns that may break during migration
        if (request.scanResults() != null) {
            snippets.add(new ReflectionUsageFindingsSnippet(request.scanResults()));
        }

        // Recipe Recommendations with Confidence - real recommendations from service
        List<ScanRecipeRecommendationService.RecipeRecommendation> recipes = request.recipeRecommendations();
        if (recipes != null && !recipes.isEmpty()) {
            snippets.add(new RecipeRecommendationsEnhancedSnippet(recipes));
        }

        return snippets;
    }
}
