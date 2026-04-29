package adrianmikula.jakartamigration.pdfreporting.snippet;

import adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Dependency;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.risk.RiskScoringService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RiskDialsSnippet.
 * Verifies that risk dials display actual data from scan results and risk scores.
 */
class RiskDialsSnippetTest {

    @Test
    @DisplayName("Should generate risk dials with actual risk score")
    void shouldGenerateRiskDialsWithActualData() throws SnippetGenerationException {
        // Arrange
        var riskScore = new RiskScoringService.RiskScore(
            45.0,  // totalScore
            "medium",
            "Medium Risk",
            "#f39c12",
            Map.of("dependencyIssues", 20, "codeComplexity", 15, "platformRisk", 10),
            Collections.emptyList()
        );
        
        var scanResults = createMockScanResults();
        var dependencyGraph = createMockDependencyGraph();
        
        var snippet = new RiskDialsSnippet(riskScore, scanResults, dependencyGraph);

        // Act
        String html = snippet.generate();

        // Assert
        assertTrue(html.contains("45"), "Should display actual risk score");
        assertTrue(html.contains("Migration Risk"), "Should display migration risk dial");
        assertTrue(html.contains("Migration Effort"), "Should display migration effort dial");
        assertTrue(html.contains("Confidence"), "Should display confidence dial");
        assertTrue(html.contains("migration-risk-gauge"), "Should contain migration risk gauge container");
        assertTrue(html.contains("migration-effort-gauge"), "Should contain migration effort gauge container");
        assertTrue(html.contains("confidence-gauge"), "Should contain confidence gauge container");
    }

    @Test
    @DisplayName("Should display factor breakdowns with actual values")
    void shouldDisplayFactorBreakdowns() throws SnippetGenerationException {
        // Arrange
        var riskScore = new RiskScoringService.RiskScore(
            60.0, "high", "High Risk", "#e74c3c",
            Map.of("dependencyIssues", 30, "codeComplexity", 20, "platformRisk", 10),
            Collections.emptyList()
        );
        
        var scanResults = createMockScanResults();
        var dependencyGraph = createMockDependencyGraph();
        
        var snippet = new RiskDialsSnippet(riskScore, scanResults, dependencyGraph);

        // Act
        String html = snippet.generate();

        // Assert
        assertTrue(html.contains("Direct dependencies needing upgrade"), "Should show direct deps factor");
        assertTrue(html.contains("Transitive dependency issues"), "Should show transitive deps factor");
        assertTrue(html.contains("Platforms needing upgrade"), "Should show platforms factor");
        assertTrue(html.contains("Source code issues"), "Should show source issues factor");
        assertTrue(html.contains("Config/non-source issues"), "Should show config issues factor");
        assertTrue(html.contains("Refactors with recipes"), "Should show recipes factor");
        assertTrue(html.contains("Project complexity"), "Should show project complexity factor");
        assertTrue(html.contains("Organisational dependencies"), "Should show org deps factor");
    }

    @Test
    @DisplayName("Should apply color coding based on thresholds")
    void shouldApplyColorCoding() throws SnippetGenerationException {
        // Arrange
        var riskScore = new RiskScoringService.RiskScore(
            75.0, "high", "High Risk", "#e74c3c",
            Map.of("dependencyIssues", 50, "codeComplexity", 20, "platformRisk", 5),
            Collections.emptyList()
        );
        
        var scanResults = createMockScanResults();
        var dependencyGraph = createMockDependencyGraph();
        
        var snippet = new RiskDialsSnippet(riskScore, scanResults, dependencyGraph);

        // Act
        String html = snippet.generate();

        // Assert
        assertTrue(html.contains("factor-item"), "Should contain factor items with color classes");
        // The actual color class depends on the calculated values
    }

    @Test
    @DisplayName("Should show no data message when risk score is null")
    void shouldShowNoDataMessageForNullRiskScore() throws SnippetGenerationException {
        // Arrange
        var snippet = new RiskDialsSnippet(null, null, null);

        // Act
        String html = snippet.generate();

        // Assert
        assertTrue(html.contains("No risk score data available"));
        assertTrue(html.contains("Run risk analysis to generate risk dials"));
    }

    @Test
    @DisplayName("Should be applicable when risk score exists")
    void shouldBeApplicableWithValidRiskScore() {
        // Arrange
        var riskScore = new RiskScoringService.RiskScore(
            50.0, "medium", "Medium Risk", "#f39c12",
            Map.of("dependencyIssues", 25, "codeComplexity", 15, "platformRisk", 10),
            Collections.emptyList()
        );
        
        var snippet = new RiskDialsSnippet(riskScore, null, null);

        // Act & Assert
        assertTrue(snippet.isApplicable());
    }

    @Test
    @DisplayName("Should not be applicable when risk score is null")
    void shouldNotBeApplicableWithNullRiskScore() {
        // Arrange
        var snippet = new RiskDialsSnippet(null, null, null);

        // Act & Assert
        assertFalse(snippet.isApplicable());
    }

    @Test
    @DisplayName("Should return correct order")
    void shouldReturnCorrectOrder() {
        // Arrange
        var riskScore = new RiskScoringService.RiskScore(
            50.0, "medium", "Medium Risk", "#f39c12",
            Map.of("dependencyIssues", 25, "codeComplexity", 15, "platformRisk", 10),
            Collections.emptyList()
        );
        
        var snippet = new RiskDialsSnippet(riskScore, null, null);

        // Act & Assert
        assertEquals(43, snippet.getOrder());
    }

    @Test
    @DisplayName("Should calculate factors from actual dependency data")
    void shouldCalculateFactorsFromDependencyData() throws SnippetGenerationException {
        // Arrange
        var riskScore = new RiskScoringService.RiskScore(
            40.0, "medium", "Medium Risk", "#f39c12",
            Map.of("dependencyIssues", 20, "codeComplexity", 15, "platformRisk", 5),
            Collections.emptyList()
        );
        
        var dependencyGraph = createMockDependencyGraph();
        var snippet = new RiskDialsSnippet(riskScore, null, dependencyGraph);

        // Act
        String html = snippet.generate();

        // Assert
        // Should calculate from actual dependency data
        assertTrue(html.contains("Organisational dependencies"));
    }

    @Test
    @DisplayName("Should calculate factors from actual scan data")
    void shouldCalculateFactorsFromScanData() throws SnippetGenerationException {
        // Arrange
        var riskScore = new RiskScoringService.RiskScore(
            55.0, "medium", "Medium Risk", "#f39c12",
            Map.of("dependencyIssues", 25, "codeComplexity", 20, "platformRisk", 10),
            Collections.emptyList()
        );
        
        var scanResults = createMockScanResults();
        var snippet = new RiskDialsSnippet(riskScore, scanResults, null);

        // Act
        String html = snippet.generate();

        // Assert
        // Should calculate from actual scan data
        assertTrue(html.contains("Source code issues"));
    }

    @Test
    @DisplayName("Should not contain hardcoded values")
    void shouldNotContainHardcodedValues() throws SnippetGenerationException {
        // Arrange
        var riskScore = new RiskScoringService.RiskScore(
            35.0, "low", "Low Risk", "#27ae60",
            Map.of("dependencyIssues", 15, "codeComplexity", 10, "platformRisk", 10),
            Collections.emptyList()
        );
        
        var scanResults = createMockScanResults();
        var dependencyGraph = createMockDependencyGraph();
        
        var snippet = new RiskDialsSnippet(riskScore, scanResults, dependencyGraph);

        // Act
        String html = snippet.generate();

        // Assert
        assertFalse(html.contains("Estimated"), "Should not contain estimated values");
        assertFalse(html.contains("approximately"), "Should not contain approximate values");
        assertFalse(html.contains("sample"), "Should not contain sample data references");
    }

    @Test
    @DisplayName("Should include Chart.js JavaScript initialization code")
    void shouldIncludeChartJsInitialization() throws SnippetGenerationException {
        // Arrange
        var riskScore = new RiskScoringService.RiskScore(
            50.0, "medium", "Medium Risk", "#f39c12",
            Map.of("dependencyIssues", 25, "codeComplexity", 15, "platformRisk", 10),
            Collections.emptyList()
        );
        
        var scanResults = createMockScanResults();
        var dependencyGraph = createMockDependencyGraph();
        
        var snippet = new RiskDialsSnippet(riskScore, scanResults, dependencyGraph);

        // Act
        String html = snippet.generate();

        // Assert
        assertTrue(html.contains("<script>"), "Should include script tag for Chart.js initialization");
        assertTrue(html.contains("new Chart"), "Should include Chart.js constructor");
        assertTrue(html.contains("type: 'doughnut'"), "Should use doughnut chart type for gauges");
        assertTrue(html.contains("migration-risk-gauge"), "Should reference migration risk gauge element");
        assertTrue(html.contains("migration-effort-gauge"), "Should reference migration effort gauge element");
        assertTrue(html.contains("confidence-gauge"), "Should reference confidence gauge element");
        assertTrue(html.contains("cutout: '75%'"), "Should configure doughnut cutout for gauge appearance");
        assertTrue(html.contains("DOMContentLoaded"), "Should wrap initialization in DOMContentLoaded event listener");
    }

    @Test
    @DisplayName("Should use correct gauge colors based on risk score")
    void shouldUseCorrectGaugeColors() throws SnippetGenerationException {
        // Arrange - Low risk (green)
        var lowRiskScore = new RiskScoringService.RiskScore(
            20.0, "low", "Low Risk", "#27ae60",
            Map.of("dependencyIssues", 10, "codeComplexity", 5, "platformRisk", 5),
            Collections.emptyList()
        );
        
        var snippet = new RiskDialsSnippet(lowRiskScore, null, null);

        // Act
        String html = snippet.generate();

        // Assert
        assertTrue(html.contains("#27ae60"), "Should use green color for low risk");
    }

    @Test
    @DisplayName("Should use correct gauge colors for high risk")
    void shouldUseCorrectGaugeColorsForHighRisk() throws SnippetGenerationException {
        // Arrange - High risk (red)
        var highRiskScore = new RiskScoringService.RiskScore(
            70.0, "high", "High Risk", "#e74c3c",
            Map.of("dependencyIssues", 40, "codeComplexity", 20, "platformRisk", 10),
            Collections.emptyList()
        );
        
        var snippet = new RiskDialsSnippet(highRiskScore, null, null);

        // Act
        String html = snippet.generate();

        // Assert
        assertTrue(html.contains("#e74c3c"), "Should use red color for high risk");
    }

    // Helper methods to create mock data

    private ComprehensiveScanResults createMockScanResults() {
        var summary = new ComprehensiveScanResults.ScanSummary(
            100,  // totalFilesScanned
            25,   // filesWithIssues
            5,    // criticalIssues
            10,   // warningIssues
            15,   // infoIssues
            0.75  // readinessScore
        );
        
        return new ComprehensiveScanResults(
            "/test/project",
            LocalDateTime.now(),
            Collections.emptyMap(),
            Collections.emptyMap(),
            Collections.emptyMap(),
            Collections.emptyMap(),
            Collections.emptyMap(),
            Collections.emptyMap(),
            Collections.emptyMap(),
            Collections.emptyList(),
            30,  // totalIssuesFound
            summary
        );
    }

    private DependencyGraph createMockDependencyGraph() {
        // Create a simple mock dependency graph
        Set<Artifact> nodes = new HashSet<>();
        Set<Dependency> edges = new HashSet<>();
        
        // Create artifacts
        Artifact artifact1 = new Artifact("org.example", "example-lib", "1.0.0", "compile", false);
        Artifact artifact2 = new Artifact("jakarta.example", "jakarta-lib", "2.0.0", "compile", false);
        Artifact artifact3 = new Artifact("org.test", "test-lib", "2.0.0", "compile", false);
        
        nodes.add(artifact1);
        nodes.add(artifact2);
        nodes.add(artifact3);
        
        // Add dependencies (edges)
        edges.add(new Dependency(artifact1, artifact2, "compile", false));
        edges.add(new Dependency(artifact1, artifact3, "compile", false));
        
        return new DependencyGraph(nodes, edges);
    }
}
