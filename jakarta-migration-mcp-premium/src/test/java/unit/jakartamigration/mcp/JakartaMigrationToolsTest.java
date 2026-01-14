package unit.jakartamigration.mcp;

import adrianmikula.jakartamigration.dependencyanalysis.domain.*;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyAnalysisModule;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphBuilder;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphException;
import adrianmikula.jakartamigration.mcp.JakartaMigrationTools;
import adrianmikula.jakartamigration.sourcecodescanning.domain.SourceCodeAnalysisResult;
import adrianmikula.jakartamigration.sourcecodescanning.service.SourceCodeScanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JakartaMigrationTools.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JakartaMigrationTools Unit Tests")
class JakartaMigrationToolsTest {

    @Mock
    private DependencyAnalysisModule dependencyAnalysisModule;

    @Mock
    private DependencyGraphBuilder dependencyGraphBuilder;

    @Mock
    private adrianmikula.jakartamigration.sourcecodescanning.service.SourceCodeScanner sourceCodeScanner;

    @InjectMocks
    private JakartaMigrationTools tools;

    @TempDir
    Path tempDir;
    
    private Path testProjectPath;
    private DependencyAnalysisReport mockReport;
    private DependencyGraph mockGraph;

    @BeforeEach
    void setUp() throws IOException {
        testProjectPath = tempDir.resolve("project");
        Files.createDirectories(testProjectPath);
        
        // Create mock dependency graph
        mockGraph = new DependencyGraph(
            new java.util.HashSet<>(List.of(
                new Artifact("com.example", "test-app", "1.0.0", "compile", false),
                new Artifact("javax.servlet", "javax.servlet-api", "4.0.1", "compile", true)
            )),
            new java.util.HashSet<>()
        );
        
        // Create mock analysis report
        mockReport = new DependencyAnalysisReport(
            mockGraph,
            new NamespaceCompatibilityMap(java.util.Map.of()),
            List.of(),
            List.of(),
            new RiskAssessment(0.3, List.of("Low risk"), List.of()),
            new MigrationReadinessScore(0.8, "Ready for migration")
        );
    }

    @Test
    @DisplayName("Should analyze Jakarta readiness successfully")
    void shouldAnalyzeJakartaReadinessSuccessfully() throws Exception {
        // Given
        when(dependencyAnalysisModule.analyzeProject(any(Path.class))).thenReturn(mockReport);

        // When
        String result = tools.analyzeJakartaReadiness(testProjectPath.toString());

        // Then
        assertThat(result).contains("\"status\": \"success\"");
        assertThat(result).contains("\"readinessScore\": 0.8");
        assertThat(result).contains("\"totalDependencies\": 2");
        verify(dependencyAnalysisModule, times(1)).analyzeProject(any(Path.class));
    }

    @Test
    @DisplayName("Should return error when project path does not exist")
    void shouldReturnErrorWhenProjectPathDoesNotExist() {
        // Given
        String nonExistentPath = "/non/existent/path";

        // When
        String result = tools.analyzeJakartaReadiness(nonExistentPath);

        // Then
        assertThat(result).contains("\"status\": \"error\"");
        assertThat(result).contains("does not exist");
        verify(dependencyAnalysisModule, never()).analyzeProject(any());
    }

    @Test
    @DisplayName("Should handle DependencyGraphException gracefully")
    void shouldHandleDependencyGraphExceptionGracefully() {
        // Given - path exists, but analysis throws exception
        when(dependencyAnalysisModule.analyzeProject(any(Path.class)))
            .thenThrow(new DependencyGraphException("Failed to parse pom.xml"));

        // When
        String result = tools.analyzeJakartaReadiness(testProjectPath.toString());

        // Then
        assertThat(result).contains("\"status\": \"error\"");
        assertThat(result).contains("Failed to analyze project");
    }

    @Test
    @DisplayName("Should detect blockers successfully")
    void shouldDetectBlockersSuccessfully() throws Exception {
        // Given
        List<Blocker> blockers = List.of(
            new Blocker(
                new Artifact("com.legacy", "legacy-lib", "1.0.0", "compile", false),
                BlockerType.NO_JAKARTA_EQUIVALENT,
                "No Jakarta equivalent found",
                List.of("Find alternative library"),
                0.9
            )
        );
        
        when(dependencyGraphBuilder.buildFromProject(any(Path.class))).thenReturn(mockGraph);
        when(dependencyAnalysisModule.detectBlockers(any(DependencyGraph.class))).thenReturn(blockers);

        // When
        String result = tools.detectBlockers(testProjectPath.toString());

        // Then
        assertThat(result).contains("\"status\": \"success\"");
        assertThat(result).contains("\"blockerCount\": 1");
        assertThat(result).contains("NO_JAKARTA_EQUIVALENT");
        verify(dependencyGraphBuilder, times(1)).buildFromProject(any(Path.class));
        verify(dependencyAnalysisModule, times(1)).detectBlockers(any(DependencyGraph.class));
    }

    @Test
    @DisplayName("Should return empty blockers list when no blockers found")
    void shouldReturnEmptyBlockersListWhenNoBlockersFound() throws Exception {
        // Given
        when(dependencyGraphBuilder.buildFromProject(any(Path.class))).thenReturn(mockGraph);
        when(dependencyAnalysisModule.detectBlockers(any(DependencyGraph.class))).thenReturn(List.of());

        // When
        String result = tools.detectBlockers(testProjectPath.toString());

        // Then
        assertThat(result).contains("\"status\": \"success\"");
        assertThat(result).contains("\"blockerCount\": 0");
    }

    @Test
    @DisplayName("Should recommend versions successfully")
    void shouldRecommendVersionsSuccessfully() throws Exception {
        // Given
        List<VersionRecommendation> recommendations = List.of(
            new VersionRecommendation(
                new Artifact("javax.servlet", "javax.servlet-api", "4.0.1", "compile", true),
                new Artifact("jakarta.servlet", "jakarta.servlet-api", "6.0.0", "compile", true),
                "Migrate to Jakarta namespace",
                List.of("Update imports"),
                0.95
            )
        );
        
        when(dependencyGraphBuilder.buildFromProject(any(Path.class))).thenReturn(mockGraph);
        when(dependencyAnalysisModule.recommendVersions(any())).thenReturn(recommendations);

        // When
        String result = tools.recommendVersions(testProjectPath.toString());

        // Then
        assertThat(result).contains("\"status\": \"success\"");
        assertThat(result).contains("\"recommendationCount\": 1");
        assertThat(result).contains("jakarta.servlet");
        verify(dependencyAnalysisModule, times(1)).recommendVersions(any());
    }

    @Test
    @DisplayName("Should analyze migration impact successfully")
    void shouldAnalyzeMigrationImpactSuccessfully() throws Exception {
        // Given
        SourceCodeAnalysisResult mockScanResult = new SourceCodeAnalysisResult(
            10,
            5,
            15,
            List.of()
        );
        
        when(dependencyAnalysisModule.analyzeProject(any(Path.class))).thenReturn(mockReport);
        when(sourceCodeScanner.scanProject(any(Path.class))).thenReturn(mockScanResult);

        // When
        String result = tools.analyzeMigrationImpact(testProjectPath.toString());

        // Then
        assertThat(result).contains("\"status\": \"success\"");
        assertThat(result).contains("\"totalFilesScanned\": 10");
        assertThat(result).contains("\"totalJavaxImports\": 15");
        verify(dependencyAnalysisModule, times(1)).analyzeProject(any(Path.class));
        verify(sourceCodeScanner, times(1)).scanProject(any(Path.class));
    }

    @Test
    @DisplayName("Should escape JSON special characters correctly")
    void shouldEscapeJsonSpecialCharactersCorrectly() {
        // Given
        DependencyAnalysisReport reportWithSpecialChars = new DependencyAnalysisReport(
            mockGraph,
            new NamespaceCompatibilityMap(java.util.Map.of()),
            List.of(),
            List.of(),
            new RiskAssessment(0.3, List.of("Risk with \"quotes\" and\nnewlines"), List.of()),
            new MigrationReadinessScore(0.8, "Message with \"quotes\"")
        );
        
        when(dependencyAnalysisModule.analyzeProject(any(Path.class))).thenReturn(reportWithSpecialChars);

        // When
        String result = tools.analyzeJakartaReadiness(testProjectPath.toString());

        // Then
        assertThat(result).contains("\"status\": \"success\"");
        // Check that quotes in content are escaped (not the JSON structure quotes)
        assertThat(result).contains("\\\"quotes\\\""); // Quotes should be escaped in content
        // Check that newlines in content are escaped (the JSON structure can have newlines for formatting)
        assertThat(result).contains("\\n"); // Newlines in content should be escaped
    }
}

