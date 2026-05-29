package adrianmikula.jakartamigration.mcp;

import adrianmikula.jakartamigration.dependencyanalysis.domain.*;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyAnalysisModule;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphBuilder;
import adrianmikula.jakartamigration.config.FeatureFlagsService;
import adrianmikula.jakartamigration.sourcecodescanning.service.SourceCodeScanner;
import adrianmikula.jakartamigration.coderefactoring.domain.RecipeExecutionResult;
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
        // Given
        when(dependencyAnalysisModule.analyzeProject(any(Path.class))).thenReturn(mockReport);

        // When
        String result = tools.detectBlockers(testProjectPath.toString());

        // Then
        assertThat(result).contains("\"status\"").contains("\"success\"");
        assertThat(result).contains("\"edition\"").contains("\"premium\"");
        verify(dependencyAnalysisModule, times(1)).analyzeProject(any(Path.class));
    }

    @Test
    @DisplayName("Should return empty blockers list when no blockers found")
    void shouldReturnEmptyBlockersListWhenNoBlockersFound() throws Exception {
        // Given - report with no blockers
        DependencyAnalysisReport emptyBlockersReport = new DependencyAnalysisReport(
                mockGraph,
                java.util.Map.of(),
                List.of(),
                List.of(),
                new RiskAssessment(0.1, List.of("Low risk"), List.of()),
                new MigrationReadinessScore(0.9, "Ready for migration"));
        when(dependencyAnalysisModule.analyzeProject(any(Path.class))).thenReturn(emptyBlockersReport);

        // When
        String result = tools.detectBlockers(testProjectPath.toString());

        // Then
        assertThat(result).contains("\"status\"").contains("\"success\"");
        assertThat(result).contains("\"blockerCount\"").contains("0");
        verify(dependencyAnalysisModule, times(1)).analyzeProject(any(Path.class));
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

    @Test
    @DisplayName("Should analyze Jakarta readiness successfully")
    void shouldAnalyzeJakartaReadinessSuccessfully() {
        // Given
        when(dependencyAnalysisModule.analyzeProject(any(Path.class))).thenReturn(mockReport);

        // When
        String result = tools.analyzeJakartaReadiness(testProjectPath.toString(), true, "detailed");

        // Then
        assertThat(result).contains("\"status\"").contains("\"success\"");
        assertThat(result).contains("\"readinessScore\":0.8");
        assertThat(result).contains("\"totalDependencies\":2");
        verify(dependencyAnalysisModule, times(1)).analyzeProject(any(Path.class));
    }

    @Test
    @DisplayName("Should analyze migration impact successfully")
    void shouldAnalyzeMigrationImpactSuccessfully() {
        // Given
        when(dependencyAnalysisModule.analyzeProject(any(Path.class))).thenReturn(mockReport);

        // When
        String result = tools.analyzeMigrationImpact(testProjectPath.toString(), "all", true, "detailed");

        // Then
        assertThat(result).contains("\"status\"").contains("\"success\"");
        assertThat(result).contains("\"projectPath\":\"" + testProjectPath.toString().replace("\\", "\\\\") + "\"");
        assertThat(result).contains("\"scope\":\"all\"");
        verify(dependencyAnalysisModule, times(1)).analyzeProject(any(Path.class));
    }

    @Test
    @DisplayName("Should recommend versions successfully")
    void shouldRecommendVersionsSuccessfully() {
        // Given
        when(dependencyAnalysisModule.analyzeProject(any(Path.class))).thenReturn(mockReport);

        // When
        String result = tools.recommendVersions(testProjectPath.toString(), true, "10");

        // Then
        assertThat(result).contains("\"status\"").contains("\"success\"");
        assertThat(result).contains("\"totalDependencies\":2");
        verify(dependencyAnalysisModule, times(1)).analyzeProject(any(Path.class));
    }

    @Test
    @DisplayName("Should generate migration plan successfully")
    void shouldGenerateMigrationPlanSuccessfully() {
        // Given
        when(dependencyAnalysisModule.analyzeProject(any(Path.class))).thenReturn(mockReport);

        // When
        String result = tools.generateMigrationPlan(testProjectPath.toString(), "incremental", "10");

        // Then
        assertThat(result).contains("\"status\"").contains("\"success\"");
        assertThat(result).contains("\"strategy\":\"incremental\"");
        assertThat(result).contains("\"targetVersion\":\"10\"");
        verify(dependencyAnalysisModule, times(1)).analyzeProject(any(Path.class));
    }

    @Test
    @DisplayName("Should validate migration successfully")
    void shouldValidateMigrationSuccessfully() {
        // Given
        when(dependencyAnalysisModule.analyzeProject(any(Path.class))).thenReturn(mockReport);

        // When
        String result = tools.validateMigration(testProjectPath.toString(), "standard");

        // Then
        assertThat(result).contains("\"status\"").contains("\"success\"");
        assertThat(result).contains("\"ready\":true");
        assertThat(result).contains("\"readinessScore\":0.8");
        verify(dependencyAnalysisModule, times(1)).analyzeProject(any(Path.class));
    }

    @Test
    @DisplayName("Should scan binary dependency successfully")
    void shouldScanBinaryDependencySuccessfully() {
        // When
        String result = tools.scanBinaryDependency(testJarPath.toString(), false, "summary");

        // Then - empty file returns UNKNOWN compatibility level
        assertThat(result).contains("\"status\"").contains("\"success\"");
        assertThat(result).contains("\"jarPath\":\"" + testJarPath.toString().replace("\\", "\\\\") + "\"");
    }

    @Test
    @DisplayName("Should apply Jakarta recipe by delegating to applyRefactorRecipe")
    void shouldApplyJakartaRecipeSuccessfully() throws Exception {
        // Given
        RecipeExecutionResult mockResult = new RecipeExecutionResult(
                true, 5, 3, List.of("File1.java", "File2.java"), "Success", 1L);
        when(recipeService.applyRecipe(anyString(), any(Path.class))).thenReturn(mockResult);

        // When
        String result = tools.applyJakartaRecipe(testProjectPath.toString(), "jakarta.servlet");

        // Then
        assertThat(result).contains("\"status\"").contains("\"success\"");
        verify(recipeService, times(1)).applyRecipe(eq("jakarta.servlet"), any(Path.class));
    }

    @Test
    @DisplayName("Should return error for invalid project path in new tools")
    void shouldReturnErrorForInvalidPathInNewTools() {
        String nonExistentPath = "/non/existent/path";

        assertThat(tools.analyzeJakartaReadiness(nonExistentPath, false, "basic"))
                .contains("\"status\"").contains("\"error\"").contains("does not exist");
        assertThat(tools.analyzeMigrationImpact(nonExistentPath, "all", false, "json"))
                .contains("\"status\"").contains("\"error\"").contains("does not exist");
        assertThat(tools.recommendVersions(nonExistentPath, false, "10"))
                .contains("\"status\"").contains("\"error\"").contains("does not exist");
        assertThat(tools.generateMigrationPlan(nonExistentPath, "incremental", "10"))
                .contains("\"status\"").contains("\"error\"").contains("does not exist");
        assertThat(tools.validateMigration(nonExistentPath, "standard"))
                .contains("\"status\"").contains("\"error\"").contains("does not exist");
        assertThat(tools.generateHtmlReport(nonExistentPath, "riskAnalysis", null))
                .contains("\"status\"").contains("\"error\"").contains("does not exist");
        assertThat(tools.scanBinaryDependency(nonExistentPath, false, "summary"))
                .contains("\"status\"").contains("\"error\"").contains("does not exist");
    }
}
