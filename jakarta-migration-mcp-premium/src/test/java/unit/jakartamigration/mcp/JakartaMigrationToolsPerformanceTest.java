package unit.jakartamigration.mcp;

import adrianmikula.jakartamigration.coderefactoring.service.MigrationPlanner;
import adrianmikula.jakartamigration.coderefactoring.service.RecipeLibrary;
import adrianmikula.jakartamigration.dependencyanalysis.domain.*;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyAnalysisModule;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphBuilder;
import adrianmikula.jakartamigration.mcp.JakartaMigrationTools;
import adrianmikula.jakartamigration.runtimeverification.service.RuntimeVerificationModule;
import adrianmikula.jakartamigration.config.FeatureFlagsService;
import adrianmikula.jakartamigration.config.ApifyBillingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Performance tests for JakartaMigrationTools.
 * Ensures tools are fast and responsive when processing large input data.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("JakartaMigrationTools Performance Tests")
class JakartaMigrationToolsPerformanceTest {

    @Mock
    private DependencyAnalysisModule dependencyAnalysisModule;

    @Mock
    private DependencyGraphBuilder dependencyGraphBuilder;

    @Mock
    private MigrationPlanner migrationPlanner;

    @Mock
    private RecipeLibrary recipeLibrary;

    @Mock
    private RuntimeVerificationModule runtimeVerificationModule;

    @Mock
    private FeatureFlagsService featureFlagsService;

    @Mock
    private ApifyBillingService apifyBillingService;

    @InjectMocks
    private JakartaMigrationTools tools;

    @TempDir
    Path tempDir;
    
    private Path testProjectPath;

    @BeforeEach
    void setUp() throws Exception {
        testProjectPath = tempDir.resolve("large-project");
        Files.createDirectories(testProjectPath);
    }

    @Test
    @DisplayName("Should analyze large project (1000+ dependencies) within 5 seconds")
    void shouldAnalyzeLargeProjectWithinTimeLimit() {
        // Given - Create a large dependency graph
        List<Artifact> largeArtifactList = IntStream.range(0, 1000)
            .mapToObj(i -> new Artifact(
                "com.example",
                "dependency-" + i,
                "1.0." + i,
                "compile",
                i % 2 == 0
            ))
            .toList();

        DependencyGraph largeGraph = new DependencyGraph(new java.util.HashSet<>(largeArtifactList), new java.util.HashSet<>());
        
        DependencyAnalysisReport largeReport = new DependencyAnalysisReport(
            largeGraph,
            new NamespaceCompatibilityMap(java.util.Map.of()),
            createLargeBlockersList(100),
            createLargeRecommendationsList(200),
            new RiskAssessment(0.5, createLargeStringList(50), List.of()),
            new MigrationReadinessScore(0.7, "Large project analysis")
        );

        when(dependencyAnalysisModule.analyzeProject(any(Path.class))).thenReturn(largeReport);

        // When
        long startTime = System.currentTimeMillis();
        String result = tools.analyzeJakartaReadiness(testProjectPath.toString());
        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertThat(result).contains("\"status\": \"success\"");
        assertThat(result).contains("\"totalDependencies\": 1000");
        assertThat(duration).isLessThan(5000); // Should complete within 5 seconds
    }

    @Test
    @DisplayName("Should detect blockers in large project (500+ blockers) within 3 seconds")
    void shouldDetectBlockersInLargeProjectWithinTimeLimit() {
        // Given
        DependencyGraph largeGraph = new DependencyGraph(
            new java.util.HashSet<>(IntStream.range(0, 1000)
                .mapToObj(i -> new Artifact("com.example", "dep-" + i, "1.0.0", "compile", false))
                .toList()),
            new java.util.HashSet<>()
        );

        List<Blocker> largeBlockersList = createLargeBlockersList(500);
        
        when(dependencyGraphBuilder.buildFromProject(any(Path.class))).thenReturn(largeGraph);
        when(dependencyAnalysisModule.detectBlockers(any(DependencyGraph.class))).thenReturn(largeBlockersList);

        // When
        long startTime = System.currentTimeMillis();
        String result = tools.detectBlockers(testProjectPath.toString());
        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertThat(result).contains("\"status\": \"success\"");
        assertThat(result).contains("\"blockerCount\": 500");
        assertThat(duration).isLessThan(3000); // Should complete within 3 seconds
    }

    @Test
    @DisplayName("Should recommend versions for large project (1000+ artifacts) within 4 seconds")
    void shouldRecommendVersionsForLargeProjectWithinTimeLimit() {
        // Given
        DependencyGraph largeGraph = new DependencyGraph(
            new java.util.HashSet<>(IntStream.range(0, 1000)
                .mapToObj(i -> new Artifact("javax.example", "dep-" + i, "1.0.0", "compile", false))
                .toList()),
            new java.util.HashSet<>()
        );

        List<VersionRecommendation> largeRecommendations = createLargeRecommendationsList(1000);
        
        when(dependencyGraphBuilder.buildFromProject(any(Path.class))).thenReturn(largeGraph);
        when(dependencyAnalysisModule.recommendVersions(any())).thenReturn(largeRecommendations);

        // When
        long startTime = System.currentTimeMillis();
        String result = tools.recommendVersions(testProjectPath.toString());
        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertThat(result).contains("\"status\": \"success\"");
        assertThat(result).contains("\"recommendationCount\": 1000");
        assertThat(duration).isLessThan(4000); // Should complete within 4 seconds
    }

    @Test
    @DisplayName("Should create migration plan for large project (500+ files) within 3 seconds")
    void shouldCreateMigrationPlanForLargeProjectWithinTimeLimit() {
        // Given
        DependencyAnalysisReport report = new DependencyAnalysisReport(
            new DependencyGraph(new java.util.HashSet<>(), new java.util.HashSet<>()),
            new NamespaceCompatibilityMap(java.util.Map.of()),
            List.of(),
            List.of(),
            new RiskAssessment(0.3, List.of(), List.of()),
            new MigrationReadinessScore(0.8, "Ready")
        );

        // Create large file list
        List<String> largeFileList = IntStream.range(0, 500)
            .mapToObj(i -> "src/main/java/com/example/File" + i + ".java")
            .toList();

        // Create at least one phase to satisfy MigrationPlan validation
        adrianmikula.jakartamigration.coderefactoring.domain.RefactoringPhase phase = 
            new adrianmikula.jakartamigration.coderefactoring.domain.RefactoringPhase(
                1,
                "Phase 1",
                largeFileList.subList(0, Math.min(100, largeFileList.size())),
                List.of(), // actions
                List.of("AddJakartaNamespace"),
                List.of(),
                Duration.ofMinutes(30)
            );
        
        adrianmikula.jakartamigration.coderefactoring.domain.MigrationPlan largePlan = 
            new adrianmikula.jakartamigration.coderefactoring.domain.MigrationPlan(
                List.of(phase),
                largeFileList,
                Duration.ofHours(2),
                new RiskAssessment(0.3, List.of(), List.of()),
                List.of()
            );

        when(dependencyAnalysisModule.analyzeProject(any(Path.class))).thenReturn(report);
        when(migrationPlanner.createPlan(any(), any(DependencyAnalysisReport.class))).thenReturn(largePlan);

        // When
        long startTime = System.currentTimeMillis();
        String result = tools.createMigrationPlan(testProjectPath.toString());
        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertThat(result).contains("\"status\": \"success\"");
        assertThat(duration).isLessThan(3000); // Should complete within 3 seconds
    }

    @Test
    @DisplayName("Should handle JSON serialization of large responses efficiently")
    void shouldHandleJsonSerializationOfLargeResponsesEfficiently() {
        // Given - Create response with many items
        List<Blocker> manyBlockers = createLargeBlockersList(1000);
        
        DependencyGraph graph = new DependencyGraph(
            new java.util.HashSet<>(IntStream.range(0, 1000)
                .mapToObj(i -> new Artifact("com.example", "dep-" + i, "1.0.0", "compile", false))
                .toList()),
            new java.util.HashSet<>()
        );

        when(dependencyGraphBuilder.buildFromProject(any(Path.class))).thenReturn(graph);
        when(dependencyAnalysisModule.detectBlockers(any(DependencyGraph.class))).thenReturn(manyBlockers);

        // When
        long startTime = System.currentTimeMillis();
        String result = tools.detectBlockers(testProjectPath.toString());
        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertThat(result).contains("\"status\": \"success\"");
        assertThat(result.length()).isGreaterThan(10000); // Large JSON response
        assertThat(duration).isLessThan(2000); // Should serialize quickly
    }

    @Test
    @DisplayName("Should process multiple concurrent requests efficiently")
    void shouldProcessMultipleConcurrentRequestsEfficiently() throws InterruptedException {
        // Given
        DependencyAnalysisReport report = new DependencyAnalysisReport(
            new DependencyGraph(new java.util.HashSet<>(), new java.util.HashSet<>()),
            new NamespaceCompatibilityMap(java.util.Map.of()),
            List.of(),
            List.of(),
            new RiskAssessment(0.3, List.of(), List.of()),
            new MigrationReadinessScore(0.8, "Ready")
        );

        when(dependencyAnalysisModule.analyzeProject(any(Path.class))).thenReturn(report);

        // When - Process 10 concurrent requests
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        long[] durations = new long[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                long start = System.currentTimeMillis();
                tools.analyzeJakartaReadiness(testProjectPath.toString());
                durations[index] = System.currentTimeMillis() - start;
            });
        }

        long totalStart = System.currentTimeMillis();
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        long totalDuration = System.currentTimeMillis() - totalStart;

        // Then
        assertThat(totalDuration).isLessThan(5000); // All requests should complete within 5 seconds
        for (long duration : durations) {
            assertThat(duration).isLessThan(2000); // Individual requests should be fast
        }
    }

    // Helper methods

    private List<Blocker> createLargeBlockersList(int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> new Blocker(
                new Artifact("com.legacy", "legacy-lib-" + i, "1.0.0", "compile", false),
                BlockerType.NO_JAKARTA_EQUIVALENT,
                "No Jakarta equivalent found for library " + i,
                List.of("Find alternative", "Check compatibility"),
                0.9
            ))
            .toList();
    }

    private List<VersionRecommendation> createLargeRecommendationsList(int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> new VersionRecommendation(
                new Artifact("javax.example", "dep-" + i, "1.0.0", "compile", false),
                new Artifact("jakarta.example", "dep-" + i, "2.0.0", "compile", false),
                "Migrate to Jakarta namespace",
                List.of("Update imports", "Update dependencies"),
                0.95
            ))
            .toList();
    }

    private List<String> createLargeStringList(int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> "Risk factor " + i)
            .toList();
    }
}

