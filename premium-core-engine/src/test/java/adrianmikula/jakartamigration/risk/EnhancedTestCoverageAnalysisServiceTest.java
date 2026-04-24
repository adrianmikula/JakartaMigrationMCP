package adrianmikula.jakartamigration.risk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for EnhancedTestCoverageAnalysisService
 */
class EnhancedTestCoverageAnalysisServiceTest {

    private EnhancedTestCoverageAnalysisService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        service = EnhancedTestCoverageAnalysisService.getInstance();
    }

    @Test
    void testAnalyzeTestCoverage_EmptyProject() throws IOException {
        // Given: empty project
        Map<String, List<String>> migrationIssues = Collections.emptyMap();

        // When
        var result = service.analyzeTestCoverage(tempDir.toString(), migrationIssues);

        // Then
        assertNotNull(result);
        assertEquals(0.0f, result.validationConfidenceScore);
        assertEquals(0.0f, result.unitTestCoverage);
        assertEquals(0.0f, result.integrationTestCoverage);
        assertTrue(result.criticalRiskZones.isEmpty());
    }

    @Test
    void testAnalyzeTestCoverage_WithUnitTests() throws IOException {
        // Given: project with some unit tests
        createSourceFile("src/main/java/Example.java", "public class Example {}");
        createSourceFile("src/test/java/ExampleTest.java", "public class ExampleTest {}");

        Map<String, List<String>> migrationIssues = Collections.emptyMap();

        // When
        var result = service.analyzeTestCoverage(tempDir.toString(), migrationIssues);

        // Then
        assertNotNull(result);
        assertTrue(result.validationConfidenceScore > 0);
        assertTrue(result.unitTestCoverage > 0);
        assertFalse(result.detectedFrameworks.isEmpty());
    }

    @Test
    void testAnalyzeTestCoverage_WithIntegrationTests() throws IOException {
        // Given: project with integration tests (more valuable for migration)
        createSourceFile("src/main/java/Example.java", "public class Example {}");
        createSourceFile("src/test/java/ExampleTest.java", "public class ExampleTest {}");
        createSourceFile("src/it/java/ExampleIT.java", "@SpringBootTest public class ExampleIT {}");

        Map<String, List<String>> migrationIssues = Collections.emptyMap();

        // When
        var result = service.analyzeTestCoverage(tempDir.toString(), migrationIssues);

        // Then
        assertNotNull(result);
        assertTrue(result.integrationTestCoverage > 0);
        // Integration tests should contribute more to validation confidence
        assertTrue(result.detailedMetrics.migrationRiskCoverage > result.detailedMetrics.unitTestCoverage);
    }

    @Test
    void testAnalyzeTestCoverage_WithMockedTests() throws IOException {
        // Given: project with mocked unit tests (less valuable for migration)
        createSourceFile("src/main/java/Example.java", "public class Example {}");
        createSourceFile("src/test/java/ExampleTest.java",
            "import org.mockito.Mock; public class ExampleTest { @Mock private Example example; }");

        Map<String, List<String>> migrationIssues = Collections.emptyMap();

        // When
        var result = service.analyzeTestCoverage(tempDir.toString(), migrationIssues);

        // Then
        assertNotNull(result);
        assertTrue(result.detailedMetrics.mockedTestCoverage > 0);
        // Validation confidence should account for mocked tests being less valuable
    }

    @Test
    void testAnalyzeTestCoverage_CriticalRiskZone() throws IOException {
        // Given: project with migration issues and low test coverage
        createSourceFile("src/main/java/Example.java", "import javax.servlet.http.HttpServlet; public class Example {}");
        // Only one test file for many source files = low coverage
        createSourceFile("src/test/java/ExampleTest.java", "public class ExampleTest {}");

        Map<String, List<String>> migrationIssues = new HashMap<>();
        migrationIssues.put("main", Arrays.asList("javax.servlet usage", "legacy API calls"));

        // When
        var result = service.analyzeTestCoverage(tempDir.toString(), migrationIssues);

        // Then
        assertNotNull(result);
        assertFalse(result.criticalRiskZones.isEmpty());
        assertTrue(result.coverageRiskAssessment.riskLevel != EnhancedTestCoverageAnalysisService.RiskLevel.Low);
        assertFalse(result.recommendations.isEmpty());
    }

    @Test
    void testAnalyzeTestCoverage_FrameworkDetection() throws IOException {
        // Given: project using JUnit and Mockito
        createSourceFile("src/test/java/ExampleTest.java",
            "import org.junit.Test; import org.mockito.Mock; public class ExampleTest {}");

        Map<String, List<String>> migrationIssues = Collections.emptyMap();

        // When
        var result = service.analyzeTestCoverage(tempDir.toString(), migrationIssues);

        // Then
        assertNotNull(result);
        assertFalse(result.detectedFrameworks.isEmpty());
        boolean hasJUnit = result.detectedFrameworks.stream()
            .anyMatch(fw -> "JUnit".equals(fw.name));
        boolean hasMockito = result.detectedFrameworks.stream()
            .anyMatch(fw -> "Mockito".equals(fw.name));
        assertTrue(hasJUnit || hasMockito); // At least one should be detected
    }

    @Test
    void testMigrationAwareConfidenceScore() {
        // Test that integration tests contribute more to confidence than mocked unit tests

        // Case 1: High integration test coverage
        var metrics1 = new EnhancedTestCoverageAnalysisService.TestCoverageMetrics(
            50.0f,  // unit test coverage
            20.0f,  // mocked test coverage
            80.0f,  // integration test coverage
            0.0f,   // component test coverage
            0.0f,   // e2e test coverage
            80.0f,  // migration risk coverage
            60.0f   // overall coverage
        );

        // Case 2: High mocked unit test coverage
        var metrics2 = new EnhancedTestCoverageAnalysisService.TestCoverageMetrics(
            80.0f,  // unit test coverage
            70.0f,  // mocked test coverage (most unit tests use mocks)
            10.0f,  // integration test coverage
            0.0f,   // component test coverage
            0.0f,   // e2e test coverage
            10.0f,  // migration risk coverage
            60.0f   // overall coverage
        );

        float confidence1 = calculateMigrationAwareConfidenceScore(metrics1);
        float confidence2 = calculateMigrationAwareConfidenceScore(metrics2);

        // Integration-heavy testing should give higher confidence
        assertTrue(confidence1 > confidence2,
            "Integration tests should contribute more to migration confidence than mocked unit tests");
    }

    // Helper method to access private method for testing
    private float calculateMigrationAwareConfidenceScore(EnhancedTestCoverageAnalysisService.TestCoverageMetrics metrics) {
        // This would need to be made accessible for testing, or we test through public methods
        // For now, we'll test the overall behavior through the public API
        return 0.0f; // Placeholder
    }

    private void createSourceFile(String relativePath, String content) throws IOException {
        Path filePath = tempDir.resolve(relativePath);
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, content);
    }
}