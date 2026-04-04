package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.reporting.service.ComprehensiveReportService;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.reporting.domain.ComprehensiveScanResults;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for ComprehensiveReportService functionality.
 * Covers report generation with various data combinations and edge cases.
 */
public class ComprehensiveReportServiceTest extends BasePlatformTestCase {
    
    @Mock
    private ComprehensiveReportService reportService;
    
    @BeforeEach
    void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.openMocks(this);
        reportService = new ComprehensiveReportService();
    }
    
    @Test
    @DisplayName("Should generate comprehensive report with all data")
    void testGenerateComprehensiveReport_AllData() throws Exception {
        // Arrange
        Project project = getProject();
        DependencyGraph dependencyGraph = createTestDependencyGraph();
        ComprehensiveScanResults scanResults = createTestScanResults();
        String outputPath = getTempDir().toString();
        Map<String, String> customData = createTestCustomData();
        
        // Act
        String result = reportService.generateComprehensiveReport(
            project, dependencyGraph, scanResults, outputPath, customData);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.endsWith(".pdf"));
        assertTrue(result.contains("jakarta-migration-report-"));
        
        // Verify file was actually created
        Path resultPath = Path.of(result);
        assertTrue(java.nio.file.Files.exists(resultPath));
        assertTrue(java.nio.file.Files.size(resultPath) > 0);
    }
    
    @Test
    @DisplayName("Should generate report with minimal data")
    void testGenerateComprehensiveReport_MinimalData() throws Exception {
        // Arrange
        Project project = getProject();
        String outputPath = getTempDir().toString();
        
        // Act
        String result = reportService.generateComprehensiveReport(
            project, null, null, outputPath, null);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.endsWith(".pdf"));
        assertTrue(java.nio.file.Files.exists(Path.of(result)));
    }
    
    @Test
    @DisplayName("Should handle null project gracefully")
    void testGenerateComprehensiveReport_NullProject() throws Exception {
        // Arrange
        String outputPath = getTempDir().toString();
        
        // Act & Assert
        String result = reportService.generateComprehensiveReport(
            null, null, null, outputPath, null);
        
        assertNotNull(result);
        assertTrue(result.contains("Error generating report"));
    }
    
    @Test
    @DisplayName("Should handle invalid output path")
    void testGenerateComprehensiveReport_InvalidOutputPath() throws Exception {
        // Arrange
        Project project = getProject();
        String invalidPath = "/invalid/path/that/does/not/exist";
        
        // Act & Assert
        String result = reportService.generateComprehensiveReport(
            project, null, null, invalidPath, null);
        
        assertNotNull(result);
        assertTrue(result.contains("Error generating report"));
    }
    
    @Test
    @DisplayName("Should include platform detection results in report")
    void testGenerateComprehensiveReport_WithPlatformDetection() throws Exception {
        // Arrange
        Project project = getProject();
        String outputPath = getTempDir().toString();
        Map<String, String> customData = new HashMap<>();
        customData.put("detectedPlatforms", "tomcat,wildfly,springboot");
        customData.put("platformRisks", "Low:45,Medium:60,High:75");
        
        // Act
        String result = reportService.generateComprehensiveReport(
            project, null, null, outputPath, customData);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.contains("Platform detection completed successfully"));
        assertTrue(result.contains("tomcat,wildfly,springboot"));
        assertTrue(result.contains("Low:45,Medium:60,High:75"));
    }
    
    @Test
    @DisplayName("Should include timestamp in filename")
    void testGenerateComprehensiveReport_Timestamp() throws Exception {
        // Arrange
        Project project = getProject();
        String outputPath = getTempDir().toString();
        
        // Act
        String result = reportService.generateComprehensiveReport(
            project, null, null, outputPath, null);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.matches(".*jakarta-migration-report-\\d{4}-\\d{2}-\\d{2}.*\\.pdf"));
    }
    
    @Test
    @DisplayName("Should create output directory if missing")
    void testGenerateComprehensiveReport_CreatesOutputDirectory() throws Exception {
        // Arrange
        Project project = getProject();
        String outputPath = getTempDir().resolve("new-dir").toString();
        
        // Ensure directory doesn't exist
        java.nio.file.Path outputDir = Path.of(outputPath);
        assertFalse(java.nio.file.Files.exists(outputDir));
        
        // Act
        String result = reportService.generateComprehensiveReport(
            project, null, null, outputPath, null);
        
        // Assert
        assertNotNull(result);
        assertTrue(java.nio.file.Files.exists(outputDir));
        assertTrue(result.contains("new-dir"));
    }
    
    @Test
    @DisplayName("Should handle custom data with special characters")
    void testGenerateComprehensiveReport_CustomDataWithSpecialChars() throws Exception {
        // Arrange
        Project project = getProject();
        String outputPath = getTempDir().toString();
        Map<String, String> customData = new HashMap<>();
        customData.put("projectName", "Test & Project");
        customData.put("version", "1.0.0-beta");
        customData.put("description", "Line 1\nLine 2\nSpecial: chars & symbols");
        
        // Act
        String result = reportService.generateComprehensiveReport(
            project, null, null, outputPath, customData);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.contains("Test & Project"));
        assertTrue(result.contains("1.0.0-beta"));
        assertTrue(result.contains("Line 1\nLine 2\nSpecial: chars & symbols"));
    }
    
    // Helper methods
    private DependencyGraph createTestDependencyGraph() {
        // Create a simple dependency graph for testing
        return new DependencyGraph(
            "test-project",
            Map.of("root", Map.of(
                "com.example:app:1.0.0",
                "org.springframework:spring-core:5.3.0"
            ))
        );
    }
    
    private ComprehensiveScanResults createTestScanResults() {
        return new ComprehensiveScanResults(
            "/test/project",
            LocalDateTime.now(),
            Map.of(
                "basic", Map.of(
                    "javax-dependencies", Map.of(
                        "javax.servlet:api", "4.0.0",
                        "javax.persistence:api", "2.0.0"
                    )
                )
                ),
                "advanced", Map.of(
                    "deprecated-methods", Map.of(
                        "com.example.legacy:LegacyUtil", "DEPRECATED",
                        "com.example.old:OldService", "OUTDATED"
                    )
                )
            
        );
    }
    
    private Map<String, String> createTestCustomData() {
        Map<String, String> customData = new HashMap<>();
        customData.put("projectName", "Test Project");
        customData.put("version", "1.0.0");
        customData.put("environment", "Test Environment");
        String notes = "Test notes\nMulti-line\nSpecial chars: !@#$%^&*()";
        customData.put("notes", notes);
        return customData;
    }
}
