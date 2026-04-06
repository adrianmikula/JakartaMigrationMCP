package adrianmikula.jakartamigration.intellij.service;

import adrianmikula.jakartamigration.intellij.service.RiskScoringService;
import adrianmikula.jakartamigration.intellij.service.RiskScoringService.RiskScore;
import adrianmikula.jakartamigration.intellij.service.RiskScoringService.RiskFinding;
import adrianmikula.jakartamigration.intellij.service.RiskScoringService.RiskConfig;
import adrianmikula.jakartamigration.intellij.service.RiskScoringService.CategoryConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for RiskScoringService with weighted inputs from YAML configuration.
 * Verifies that risk score calculations work correctly with different configurations
 * and edge cases.
 */
public class RiskScoringServiceTest {

    private RiskScoringService riskScoringService;

    @BeforeEach
    void setUp() {
        // Get the singleton instance
        riskScoringService = RiskScoringService.getInstance();
    }

    @Test
    @Tag("fast")
    void testCalculateRiskScoreWithEmptyData() {
        RiskScoringService service = RiskScoringService.getInstance();
        
        var score = service.calculateRiskScore(
            Map.of(), 
            Map.of(), 
            100, // total file count
            1.0  // platform risk score
        );
        
        assertThat(score.totalScore()).isGreaterThanOrEqualTo(0);
        assertThat(score.category()).isNotNull();
    }

    @Test
    void testBasicRiskScoreCalculation() {
        // Test with minimal data
        Map<String, List<RiskFinding>> scanFindings = new HashMap<>();
        Map<String, Integer> dependencyIssues = new HashMap<>();

        RiskScore score = riskScoringService.calculateRiskScore(scanFindings, dependencyIssues);

        assertThat(score.totalScore()).isGreaterThanOrEqualTo(0);
        assertThat(score.category()).isIn("trivial", "low", "medium", "high", "extreme");
        assertThat(score.categoryLabel()).isNotNull();
        assertThat(score.categoryColor()).isNotNull();
        assertThat(score.findings()).isNotNull();
        assertThat(score.componentScores()).isNotNull();
    }

    @Test
    void testRiskScoreWithScanFindings() {
        Map<String, List<RiskFinding>> scanFindings = new HashMap<>();
        
        // Add JPA findings with appropriate scores based on risk levels
        List<RiskFinding> jpaFindings = Arrays.asList(
            new RiskFinding("jpa", "entityWithJakartaId", "Jakarta ID annotation usage", "low", 5),
            new RiskFinding("jpa", "missingVersion", "Missing version in persistence.xml", "high", 25)
        );
        scanFindings.put("jpa", jpaFindings);
        
        // Add Servlet findings
        List<RiskFinding> servletFindings = Arrays.asList(
            new RiskFinding("servlet", "javaxServletImport", "javax.servlet import - needs migration", "high", 30)
        );
        scanFindings.put("servlet", servletFindings);

        Map<String, Integer> dependencyIssues = new HashMap<>();

        RiskScore score = riskScoringService.calculateRiskScore(scanFindings, dependencyIssues);

        // Should have a higher score due to high-risk findings
        assertThat(score.totalScore()).isGreaterThan(0);
        assertThat(score.findings()).hasSize(3); // 2 JPA + 1 Servlet findings
        assertThat(score.category()).isNotEqualTo("trivial");
    }

    @Test
    void testRiskScoreWithDependencyIssues() {
        Map<String, List<RiskFinding>> scanFindings = new HashMap<>();
        Map<String, Integer> dependencyIssues = new HashMap<>();
        
        // Add dependency issues
        dependencyIssues.put("noJakartaVersion", 25);
        dependencyIssues.put("blockedDependency", 40);
        dependencyIssues.put("directDependency", 10);

        RiskScore score = riskScoringService.calculateRiskScore(scanFindings, dependencyIssues);

        // Should have a higher score due to dependency issues
        assertThat(score.totalScore()).isGreaterThan(0);
    }

    @Test
    void testRiskScoreWithCombinedFactors() {
        Map<String, List<RiskFinding>> scanFindings = new HashMap<>();
        
        // Add scan findings
        List<RiskFinding> jpaFindings = Arrays.asList(
            new RiskFinding("jpa", "entityWithJakartaId", "Jakarta ID annotation usage", "low", 5)
        );
        scanFindings.put("jpa", jpaFindings);
        
        Map<String, Integer> dependencyIssues = new HashMap<>();
        dependencyIssues.put("noJakartaVersion", 25);

        RiskScore score = riskScoringService.calculateRiskScore(scanFindings, dependencyIssues);

        // Should have a moderate score combining both factors
        assertThat(score.totalScore()).isGreaterThan(0);
        assertThat(score.findings()).hasSize(1);
    }

    @Test
    void testRiskScoreCategories() {
        // Test trivial category
        Map<String, List<RiskFinding>> scanFindings = new HashMap<>();
        Map<String, Integer> dependencyIssues = new HashMap<>();

        RiskScore trivialScore = riskScoringService.calculateRiskScore(scanFindings, dependencyIssues);
        assertThat(trivialScore.category()).isEqualTo("trivial");
        assertThat(trivialScore.categoryLabel()).isEqualTo("Trivial");

        // Test higher score categories by adding more findings
        List<RiskFinding> highRiskFindings = Arrays.asList(
            new RiskFinding("servlet", "javaxServletImport", "javax.servlet import - needs migration", "high", 50),
            new RiskFinding("jpa", "missingVersion", "Missing version in persistence.xml", "high", 50)
        );
        scanFindings.put("servlet", highRiskFindings);
        scanFindings.put("jpa", highRiskFindings);

        dependencyIssues.put("blockedDependency", 40);

        RiskScore highScore = riskScoringService.calculateRiskScore(scanFindings, dependencyIssues);
        assertThat(highScore.totalScore()).isGreaterThan(trivialScore.totalScore());
    }

    @Test
    void testBaseWeightApplication() {
        Map<String, List<RiskFinding>> scanFindings = new HashMap<>();
        
        // Add findings to a scan type with higher base weight (servlet: 20 vs jpa: 15)
        List<RiskFinding> servletFindings = Arrays.asList(
            new RiskFinding("servlet", "javaxServletImport", "javax.servlet import - needs migration", "high", 10)
        );
        scanFindings.put("servlet", servletFindings);

        Map<String, Integer> dependencyIssues = new HashMap<>();

        RiskScore score = riskScoringService.calculateRiskScore(scanFindings, dependencyIssues);

        // Score should be affected by base weight
        assertThat(score.totalScore()).isGreaterThan(0);
    }

    @Test
    void testComponentWeightsFromYaml() {
        // Test that the weights from YAML are correctly applied
        Map<String, List<RiskFinding>> scanFindings = new HashMap<>();
        List<RiskFinding> findings = Arrays.asList(
            new RiskFinding("jpa", "entityWithJakartaId", "Jakarta ID annotation usage", "low", 10)
        );
        scanFindings.put("jpa", findings);

        Map<String, Integer> dependencyIssues = new HashMap<>();
        dependencyIssues.put("directDependency", 10);

        RiskScore score = riskScoringService.calculateRiskScore(scanFindings, dependencyIssues);

        // Verify the calculation includes both scan findings and dependency issues
        // according to the weights (0.4 each) plus complexity (0.2)
        assertThat(score.totalScore()).isGreaterThan(0);
    }

    @Test
    void testRiskLevelForFinding() {
        String riskLevel = riskScoringService.getRiskLevelForFinding("jpa", "entityWithJakartaId");
        assertThat(riskLevel).isEqualTo("low");

        riskLevel = riskScoringService.getRiskLevelForFinding("jpa", "missingVersion");
        assertThat(riskLevel).isEqualTo("high");

        riskLevel = riskScoringService.getRiskLevelForFinding("servlet", "javaxServletImport");
        assertThat(riskLevel).isEqualTo("high");
    }

    @Test
    void testRiskLevelForFindingInvalidScanType() {
        assertThatThrownBy(() -> {
            riskScoringService.getRiskLevelForFinding("invalidScanType", "someFinding");
        })
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No risk configuration found for scan type: invalidScanType");
    }

    @Test
    void testRiskLevelForFindingInvalidFindingType() {
        assertThatThrownBy(() -> {
            riskScoringService.getRiskLevelForFinding("jpa", "invalidFinding");
        })
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No risk level defined for finding: invalidFinding");
    }

    @Test
    void testGetRiskConfigs() {
        Map<String, RiskConfig> configs = riskScoringService.getRiskConfigs();
        
        assertThat(configs).containsKeys("jpa", "servlet", "beanValidation", "cdi", "jms", "webservice", "serializationCache");
        
        RiskConfig jpaConfig = configs.get("jpa");
        assertThat(jpaConfig.displayName).isEqualTo("JPA/Entity");
        assertThat(jpaConfig.baseWeight).isEqualTo(15);
        assertThat(jpaConfig.findings).containsKeys("entityWithJakartaId", "missingVersion", "entityWithDeprecatedApi");
    }

    @Test
    void testGetCategoryConfig() {
        CategoryConfig trivialConfig = riskScoringService.getCategoryConfig("trivial");
        assertThat(trivialConfig.label).isEqualTo("Trivial");
        assertThat(trivialConfig.color).isEqualTo("#28a745");
        assertThat(trivialConfig.minScore).isEqualTo(0);
        assertThat(trivialConfig.maxScore).isEqualTo(24);

        CategoryConfig highConfig = riskScoringService.getCategoryConfig("high");
        assertThat(highConfig.label).isEqualTo("High");
        assertThat(highConfig.color).isEqualTo("#fd7e14");
        assertThat(highConfig.minScore).isEqualTo(100);
        assertThat(highConfig.maxScore).isEqualTo(199);
    }

    @Test
    void testRiskScoreNormalization() {
        // Test that scores are properly normalized to max 100
        Map<String, List<RiskFinding>> scanFindings = new HashMap<>();
        
        // Add very high scoring findings
        List<RiskFinding> highScoreFindings = Arrays.asList(
            new RiskFinding("servlet", "javaxServletImport", "javax.servlet import - needs migration", "high", 100),
            new RiskFinding("jpa", "missingVersion", "Missing version in persistence.xml", "high", 100)
        );
        scanFindings.put("servlet", highScoreFindings);
        scanFindings.put("jpa", highScoreFindings);

        Map<String, Integer> dependencyIssues = new HashMap<>();
        dependencyIssues.put("blockedDependency", 100);

        RiskScore score = riskScoringService.calculateRiskScore(scanFindings, dependencyIssues);

        // Should be capped at 100
        assertThat(score.totalScore()).isLessThanOrEqualTo(100);
    }

    @Test
    void testEmptyFindingsAndDependencies() {
        Map<String, List<RiskFinding>> scanFindings = new HashMap<>();
        Map<String, Integer> dependencyIssues = new HashMap<>();

        RiskScore score = riskScoringService.calculateRiskScore(scanFindings, dependencyIssues);

        // Should still return a valid score (based on complexity)
        assertThat(score.totalScore()).isGreaterThanOrEqualTo(0);
        assertThat(score.category()).isEqualTo("trivial"); // Only complexity score
        assertThat(score.findings()).isEmpty();
    }

    @Test
    void testMultipleScanTypesWithDifferentWeights() {
        Map<String, List<RiskFinding>> scanFindings = new HashMap<>();
        
        // Add findings to different scan types with different base weights
        List<RiskFinding> jpaFindings = Arrays.asList(
            new RiskFinding("jpa", "entityWithJakartaId", "Jakarta ID annotation usage", "low", 10)
        );
        scanFindings.put("jpa", jpaFindings);

        List<RiskFinding> servletFindings = Arrays.asList(
            new RiskFinding("servlet", "javaxServletImport", "javax.servlet import - needs migration", "high", 10)
        );
        scanFindings.put("servlet", servletFindings);

        Map<String, Integer> dependencyIssues = new HashMap<>();

        RiskScore score = riskScoringService.calculateRiskScore(scanFindings, dependencyIssues);

        // Should have findings from both scan types
        assertThat(score.findings()).hasSize(2);
        assertThat(score.totalScore()).isGreaterThan(0);
        
        // Verify both findings are present
        List<String> scanTypes = score.findings().stream()
            .map(RiskFinding::scanType)
            .toList();
        assertThat(scanTypes).contains("jpa", "servlet");
    }

    @Test
    void testRiskFindingRecord() {
        RiskFinding finding = new RiskFinding("jpa", "entityWithJakartaId", 
            "Jakarta ID annotation usage", "low", 5);

        assertThat(finding.scanType()).isEqualTo("jpa");
        assertThat(finding.findingType()).isEqualTo("entityWithJakartaId");
        assertThat(finding.description()).isEqualTo("Jakarta ID annotation usage");
        assertThat(finding.riskLevel()).isEqualTo("low");
        assertThat(finding.score()).isEqualTo(5);
    }

    @Test
    void testRiskScoreRecord() {
        Map<String, Integer> componentScores = new HashMap<>();
        componentScores.put("scanFindings", 20);
        componentScores.put("dependencyIssues", 15);
        
        List<RiskFinding> findings = Arrays.asList(
            new RiskFinding("jpa", "entityWithJakartaId", "Jakarta ID annotation usage", "low", 5)
        );

        RiskScore score = new RiskScore(35, "low", "Low", "#17a2b8", componentScores, findings);

        assertThat(score.totalScore()).isEqualTo(35);
        assertThat(score.category()).isEqualTo("low");
        assertThat(score.categoryLabel()).isEqualTo("Low");
        assertThat(score.categoryColor()).isEqualTo("#17a2b8");
        assertThat(score.componentScores()).isEqualTo(componentScores);
        assertThat(score.findings()).isEqualTo(findings);
    }

    @Test
    void testSerializationCacheScanType() {
        // Test the serializationCache scan type which has a lower base weight
        Map<String, List<RiskFinding>> scanFindings = new HashMap<>();
        
        List<RiskFinding> serializationFindings = Arrays.asList(
            new RiskFinding("serializationCache", "Serializable", "Java Serialization usage", "low", 5),
            new RiskFinding("serializationCache", "CacheLibrary", "Third-party cache library", "low", 3)
        );
        scanFindings.put("serializationCache", serializationFindings);

        Map<String, Integer> dependencyIssues = new HashMap<>();

        RiskScore score = riskScoringService.calculateRiskScore(scanFindings, dependencyIssues);

        // Should have findings but lower score due to base weight of 2
        assertThat(score.findings()).hasSize(2);
        assertThat(score.totalScore()).isGreaterThan(0);
    }

    @Test
    void testAllConfiguredScanTypes() {
        // Test that all configured scan types can be used
        Map<String, RiskConfig> configs = riskScoringService.getRiskConfigs();
        
        for (String scanType : configs.keySet()) {
            Map<String, List<RiskFinding>> scanFindings = new HashMap<>();
            
            // Add a low-risk finding for each scan type
            List<RiskFinding> findings = Arrays.asList(
                new RiskFinding(scanType, "testFinding", "Test finding", "low", 5)
            );
            scanFindings.put(scanType, findings);

            Map<String, Integer> dependencyIssues = new HashMap<>();

            // Should not throw exception
            RiskScore score = riskScoringService.calculateRiskScore(scanFindings, dependencyIssues);
            assertThat(score.totalScore()).isGreaterThanOrEqualTo(0);
        }
    }
}
