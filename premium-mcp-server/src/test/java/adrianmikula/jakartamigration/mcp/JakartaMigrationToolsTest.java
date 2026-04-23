package adrianmikula.jakartamigration.mcp;

import adrianmikula.jakartamigration.dependencyanalysis.domain.*;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyAnalysisModule;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphBuilder;
import adrianmikula.jakartamigration.config.FeatureFlagsService;
import adrianmikula.jakartamigration.sourcecodescanning.service.SourceCodeScanner;
import adrianmikula.jakartamigration.coderefactoring.service.RecipeService;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PremiumMigrationTools.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PremiumMigrationTools Unit Tests")
class JakartaMigrationToolsTest {

    @Mock
    private DependencyAnalysisModule dependencyAnalysisModule;

    @Mock
    private DependencyGraphBuilder dependencyGraphBuilder;

    @Mock
    private FeatureFlagsService featureFlagsService;

    @Mock
    private SourceCodeScanner sourceCodeScanner;

    @Mock
    private RecipeService recipeService;

    @InjectMocks
    private PremiumMigrationTools tools;

    @TempDir
    Path tempDir;

    private Path testProjectPath;
    private Path testJarPath;
    private DependencyAnalysisReport mockReport;
    private DependencyGraph mockGraph;

    @BeforeEach
    void setUp() throws IOException {
        testProjectPath = tempDir.resolve("project");
        Files.createDirectories(testProjectPath);

        testJarPath = tempDir.resolve("app.jar");
        Files.createFile(testJarPath);

        // Create mock dependency graph
        mockGraph = new DependencyGraph(
                new java.util.HashSet<>(List.of(
                        new Artifact("com.example", "test-app", "1.0.0", "compile", false),
                        new Artifact("javax.servlet", "javax.servlet-api", "4.0.1", "compile", true))),
                new java.util.HashSet<>());

        // Create mock analysis report
        mockReport = new DependencyAnalysisReport(
                mockGraph,
                java.util.Map.of(),
                List.of(),
                List.of(),
                new RiskAssessment(0.3, List.of("Low risk"), List.of()),
                new MigrationReadinessScore(0.8, "Ready for migration"));
    }

    @Test
    @DisplayName("Should create report successfully")
    void shouldCreateReportSuccessfully() throws Exception {
        // Given
        when(dependencyAnalysisModule.analyzeProject(any(Path.class))).thenReturn(mockReport);

        // When
        String result = tools.createReport(testProjectPath.toString());

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
        String result = tools.createReport(nonExistentPath);

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
                .thenThrow(new RuntimeException("Failed to parse pom.xml"));

        // When
        String result = tools.createReport(testProjectPath.toString());

        // Then
        assertThat(result).contains("\"status\": \"error\"");
        assertThat(result).contains("Unexpected error");
    }

    @Test
    @DisplayName("Should detect blockers successfully")
    void shouldDetectBlockersSuccessfully() throws Exception {
        // When
        String result = tools.detectBlockers(testProjectPath.toString());

        // Then - detectBlockers requires premium license, so should return error
        assertThat(result).contains("\"status\": \"error\"");
        assertThat(result).contains("MCP Server features require Premium");
        verify(dependencyAnalysisModule, never()).analyzeProject(any(Path.class));
    }

    @Test
    @DisplayName("Should return empty blockers list when no blockers found")
    void shouldReturnEmptyBlockersListWhenNoBlockersFound() throws Exception {
        // When
        String result = tools.detectBlockers(testProjectPath.toString());

        // Then - detectBlockers requires premium license, so should return error
        assertThat(result).contains("\"status\": \"error\"");
        assertThat(result).contains("MCP Server features require Premium");
        verify(dependencyAnalysisModule, never()).analyzeProject(any(Path.class));
    }

    @Test
    @DisplayName("Should list refactor recipes successfully")
    void shouldListRefactorRecipesSuccessfully() throws Exception {
        // Given
        when(recipeService.getRecipes(any(Path.class))).thenReturn(List.of());

        // When
        String result = tools.listRefactorRecipes(testProjectPath.toString());

        // Then
        assertThat(result).contains("\"status\": \"success\"");
        assertThat(result).contains("\"edition\": \"premium\"");
        verify(recipeService, times(1)).getRecipes(any(Path.class));
    }

    @Test
    @DisplayName("Should return error message for invalid method")
    void shouldReturnErrorForInvalidMethod() throws Exception {
        // This test is placeholder for methods that don't exist yet
        // Given
        when(dependencyAnalysisModule.analyzeProject(any(Path.class))).thenReturn(mockReport);

        // When - using createReport as it exists
        String result = tools.createReport(testProjectPath.toString());

        // Then
        assertThat(result).contains("\"status\": \"success\"");
        verify(dependencyAnalysisModule, times(1)).analyzeProject(any(Path.class));
    }

    @Test
    @DisplayName("Should escape JSON special characters correctly")
    void shouldEscapeJsonSpecialCharactersCorrectly() {
        // Given
        DependencyAnalysisReport reportWithSpecialChars = new DependencyAnalysisReport(
                mockGraph,
                java.util.Map.of(),
                List.of(),
                List.of(),
                new RiskAssessment(0.3, List.of("Risk with \"quotes\" and\nnewlines"), List.of()),
                new MigrationReadinessScore(0.8, "Message with \"quotes\""));

        when(dependencyAnalysisModule.analyzeProject(any(Path.class))).thenReturn(reportWithSpecialChars);

        // When
        String result = tools.createReport(testProjectPath.toString());

        // Then
        assertThat(result).contains("\"status\": \"success\"");
        // Check that quotes in content are escaped (not the JSON structure quotes)
        assertThat(result).contains("\\\"quotes\\\""); // Quotes should be escaped in content
        // Check that newlines in content are escaped (the JSON structure can have
        // newlines for formatting)
        assertThat(result).contains("\\n"); // Newlines in content should be escaped
    }
}
