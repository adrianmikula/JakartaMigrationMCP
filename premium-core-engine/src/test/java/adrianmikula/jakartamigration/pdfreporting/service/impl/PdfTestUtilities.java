package adrianmikula.jakartamigration.pdfreporting.service.impl;

import adrianmikula.jakartamigration.pdfreporting.service.PdfReportService;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults;
import adrianmikula.jakartamigration.platforms.model.PlatformScanResult;
import adrianmikula.jakartamigration.risk.RiskScoringService;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Test utilities for PDF reporting tests to reduce duplication and provide common test data.
 */
public class PdfTestUtilities {

    /**
     * Creates a mock dependency graph with sample data for testing.
     */
    public static DependencyGraph createMockDependencyGraph() {
        Set<Artifact> artifacts = new HashSet<>();
        artifacts.add(new Artifact("javax.servlet", "javax.servlet-api", "3.1.0", "compile", false));
        artifacts.add(new Artifact("jakarta.servlet", "jakarta.servlet-api", "5.0.0", "compile", true));
        artifacts.add(new Artifact("org.springframework", "spring-core", "5.3.0", "compile", true));
        
        return new DependencyGraph(artifacts, new HashSet<>());
    }

    /**
     * Creates mock scan results with sample data for testing.
     */
    public static ComprehensiveScanResults createMockScanResults() {
        // For now, return null since constructor is complex and tests handle null gracefully
        return null;
    }

    /**
     * Creates mock platform scan results for testing.
     */
    public static PlatformScanResult createMockPlatformScanResult() {
        // For now, return null since constructor is complex and tests handle null gracefully
        return null;
    }

    /**
     * Creates mock risk score for testing.
     */
    public static RiskScoringService.RiskScore createMockRiskScore() {
        // For now, return null since constructor is complex and tests handle null gracefully
        return null;
    }

    /**
     * Creates a basic risk analysis report request with minimal data.
     */
    public static PdfReportService.RiskAnalysisReportRequest createBasicRiskAnalysisRequest(Path outputPath) {
        return new PdfReportService.RiskAnalysisReportRequest(
            outputPath,
            "Test Project",
            "Test Risk Analysis Report",
            null, // dependencyGraph
            null, // analysisReport
            null, // scanResults
            null, // platformScanResults
            null, // riskScore
            "Incremental",
            Map.of("displayName", "Incremental", "description", "Test strategy"),
            Map.of("unitTestCoverage", 70, "overallConfidence", 75),
            List.of(), // topBlockers
            List.of("Test recommendation 1", "Test recommendation 2"),
            Map.of("phase1", Map.of("name", "Preparation", "description", "Test preparation")),
            Map.of("generatedBy", "Test Suite")
        );
    }

    /**
     * Creates a comprehensive risk analysis report request with full data.
     */
    public static PdfReportService.RiskAnalysisReportRequest createComprehensiveRiskAnalysisRequest(Path outputPath) {
        return new PdfReportService.RiskAnalysisReportRequest(
            outputPath,
            "Comprehensive Test Project",
            "Comprehensive Risk Analysis Report",
            createMockDependencyGraph(),
            null, // analysisReport
            createMockScanResults(),
            createMockPlatformScanResult(),
            createMockRiskScore(),
            "Transform",
            Map.of("displayName", "Transform", "description", "Comprehensive transformation strategy"),
            Map.of("unitTestCoverage", 60, "overallConfidence", 65),
            List.of(
                Map.of("name", "High dependency complexity", "severity", "HIGH", "impact", "Major refactoring required"),
                Map.of("name", "Legacy code patterns", "severity", "MEDIUM", "impact", "Modernization needed")
            ),
            List.of("Reduce complexity gradually", "Plan carefully", "Test thoroughly"),
            Map.of(
                "phase1", Map.of("name", "Preparation", "description", "Setup and analysis"),
                "phase2", Map.of("name", "Implementation", "description", "Execute migration"),
                "phase3", Map.of("name", "Validation", "description", "Test and verify")
            ),
            Map.of("generatedBy", "Comprehensive Test Suite")
        );
    }

    /**
     * Creates a basic refactoring action report request with minimal data.
     */
    public static PdfReportService.RefactoringActionReportRequest createBasicRefactoringRequest(Path outputPath) {
        return new PdfReportService.RefactoringActionReportRequest(
            outputPath,
            "Test Project",
            "Test Refactoring Action Report",
            null, // dependencyGraph
            null, // scanResults
            List.of(), // recipeRecommendations
            List.of(), // javaxReferences
            List.of(), // openRewriteRecipes
            Map.of("automationReady", 50), // refactoringReadiness
            Map.of("highPriority", 0, "mediumPriority", 0, "lowPriority", 0), // priorityRanking
            Map.of("generatedBy", "Test Suite") // customData
        );
    }

    /**
     * Creates a comprehensive refactoring action report request with full data.
     */
    public static PdfReportService.RefactoringActionReportRequest createComprehensiveRefactoringRequest(Path outputPath) {
        List<Map<String, Object>> javaxReferences = List.of(
            Map.of("file", "SampleController.java", "line", "15", "reference", "javax.servlet.http.HttpServlet", "priority", "high", "recipeAvailable", true),
            Map.of("file", "SampleEntity.java", "line", "8", "reference", "javax.persistence.Entity", "priority", "medium", "recipeAvailable", true),
            Map.of("file", "SampleService.java", "line", "22", "reference", "javax.inject.Inject", "priority", "low", "recipeAvailable", false)
        );

        List<Map<String, Object>> openRewriteRecipes = List.of(
            Map.of("name", "Jakarta EE 9 to 10 Migration", "category", "Namespace", "description", "Updates javax imports to jakarta", "automated", true),
            Map.of("name", "JPA Entity Annotations", "category", "JPA", "description", "Updates JPA annotations to jakarta namespace", "automated", true),
            Map.of("name", "Servlet API Migration", "category", "Servlet", "description", "Updates servlet references to jakarta", "automated", false)
        );

        return new PdfReportService.RefactoringActionReportRequest(
            outputPath,
            "Comprehensive Test Project",
            "Comprehensive Refactoring Action Report",
            createMockDependencyGraph(),
            createMockScanResults(),
            List.of(), // recipeRecommendations
            javaxReferences,
            openRewriteRecipes,
            Map.of("automationReady", 80), // refactoringReadiness
            Map.of("highPriority", 2, "mediumPriority", 1, "lowPriority", 0), // priorityRanking
            Map.of("generatedBy", "Comprehensive Test Suite") // customData
        );
    }

    /**
     * Creates a consolidated report request with full data.
     */
    public static PdfReportService.ConsolidatedReportRequest createConsolidatedReportRequest(Path outputPath) {
        return new PdfReportService.ConsolidatedReportRequest(
            outputPath,
            "Consolidated Test Project",
            "Consolidated Test Report",
            createMockDependencyGraph(),
            null, // analysisReport
            createMockScanResults(),
            createMockPlatformScanResult(),
            createMockRiskScore(),
            "Incremental",
            Map.of("displayName", "Incremental", "description", "Test strategy"),
            Map.of("unitTestCoverage", 70, "overallConfidence", 75),
            List.of(
                Map.of("name", "javax.servlet:servlet-api", "severity", "HIGH", "impact", "Test impact")
            ),
            List.of("Test recommendation 1", "Test recommendation 2"),
            Map.of("phase1", Map.of("name", "Preparation", "description", "Test preparation")),
            Map.of("generatedBy", "Test Suite")
        );
    }

    /**
     * Utility method to clean up test files safely.
     */
    public static void cleanupTestFiles(Path... paths) {
        for (Path path : paths) {
            try {
                Files.deleteIfExists(path);
                // Also clean up HTML fallback files
                Path htmlFile = path.resolveSibling(path.getFileName().toString().replace(".pdf", ".html"));
                Files.deleteIfExists(htmlFile);
            } catch (Exception e) {
                // Log but don't fail tests for cleanup issues
                System.err.println("Warning: Could not clean up test file: " + path);
            }
        }
    }

    /**
     * Utility method to assert basic PDF report generation results.
     */
    public static void assertBasicReportGeneration(Path result, Path expectedOutputPath) {
        assertAll(
            () -> assertNotNull(result, "Generated report path should not be null"),
            () -> assertTrue(Files.exists(result), "Report file should exist"),
            () -> assertTrue(Files.size(result) > 0, "Report file should have content"),
            () -> assertEquals(expectedOutputPath, result, "Should return the expected output path")
        );
    }

    /**
     * Utility method to assert HTML fallback content contains expected sections.
     */
    public static void assertHtmlContentContains(String htmlContent, String... expectedSections) {
        for (String section : expectedSections) {
            assertTrue(htmlContent.contains(section), "HTML content should contain: " + section);
        }
    }
}
