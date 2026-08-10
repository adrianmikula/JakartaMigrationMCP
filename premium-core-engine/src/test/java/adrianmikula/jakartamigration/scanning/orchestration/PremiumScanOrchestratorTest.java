package adrianmikula.jakartamigration.scanning.orchestration;

import adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults;
import adrianmikula.jakartamigration.advancedscanning.service.ScanProgressCallback;
import adrianmikula.jakartamigration.config.FeatureFlagsProperties;
import adrianmikula.jakartamigration.config.FeatureFlagsService;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyAnalysisReport;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyAnalysisModule;
import adrianmikula.jakartamigration.sourcecodescanning.domain.SourceCodeAnalysisResult;
import adrianmikula.jakartamigration.sourcecodescanning.service.SourceCodeScanner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PremiumScanOrchestratorTest {

    @Mock
    private SourceCodeScanner sourceCodeScanner;
    @Mock
    private DependencyAnalysisModule dependencyAnalysisModule;
    @Mock
    private AdvancedScanningEngine advancedScanningEngine;
    @Mock
    private FeatureFlagsService featureFlagsService;

    @Test
    void orchestrate_startsSourceAndDependencyScansInParallel() throws Exception {
        Path projectPath = Path.of("/tmp/demo");
        SourceCodeAnalysisResult sourceResult = new SourceCodeAnalysisResult(List.of(), 1, 0, 0);
        DependencyAnalysisReport depResult = mock(DependencyAnalysisReport.class);
        ComprehensiveScanResults advancedResult = new ComprehensiveScanResults(
                projectPath.toString(),
                LocalDateTime.now(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                List.of(),
                0,
                new ComprehensiveScanResults.ScanSummary(0, 0, 0, 0, 0, 1.0));

        CountDownLatch bothStarted = new CountDownLatch(2);

        when(sourceCodeScanner.scanProject(projectPath)).thenAnswer(invocation -> {
            bothStarted.countDown();
            bothStarted.await(2, TimeUnit.SECONDS);
            return sourceResult;
        });
        when(dependencyAnalysisModule.analyzeProject(projectPath)).thenAnswer(invocation -> {
            bothStarted.countDown();
            bothStarted.await(2, TimeUnit.SECONDS);
            return depResult;
        });
        when(advancedScanningEngine.runAdvancedScans(eq(projectPath), eq(ScanMode.QUICK), any()))
                .thenReturn(advancedResult);

        PremiumScanOrchestrator orchestrator = new PremiumScanOrchestrator(
                sourceCodeScanner, dependencyAnalysisModule, advancedScanningEngine, featureFlagsService);

        ScanRequest request = new ScanRequest(
                projectPath, ScanMode.QUICK, Set.of(), FeatureFlagsProperties.LicenseTier.PREMIUM);

        ScanResult result = orchestrator.orchestrate(request, (phase, completed, total) -> {})
                .get(5, TimeUnit.SECONDS);

        assertThat(result.sourceCodeResult()).isEqualTo(sourceResult);
        assertThat(result.dependencyReport()).isEqualTo(depResult);
        assertThat(result.advancedScanResults()).isEqualTo(advancedResult);
    }

    @Test
    void orchestrate_reportsHighLevelPhaseCompletion() throws Exception {
        Path projectPath = Path.of("/tmp/demo");
        SourceCodeAnalysisResult sourceResult = new SourceCodeAnalysisResult(List.of(), 1, 0, 0);
        DependencyAnalysisReport depResult = mock(DependencyAnalysisReport.class);
        ComprehensiveScanResults advancedResult = new ComprehensiveScanResults(
                projectPath.toString(),
                LocalDateTime.now(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                List.of(),
                0,
                new ComprehensiveScanResults.ScanSummary(0, 0, 0, 0, 0, 1.0));

        when(sourceCodeScanner.scanProject(projectPath)).thenReturn(sourceResult);
        when(dependencyAnalysisModule.analyzeProject(projectPath)).thenReturn(depResult);
        when(advancedScanningEngine.runAdvancedScans(eq(projectPath), eq(ScanMode.QUICK), any()))
                .thenReturn(advancedResult);

        PremiumScanOrchestrator orchestrator = new PremiumScanOrchestrator(
                sourceCodeScanner, dependencyAnalysisModule, advancedScanningEngine, featureFlagsService);

        ScanRequest request = new ScanRequest(
                projectPath, ScanMode.QUICK, Set.of(), FeatureFlagsProperties.LicenseTier.PREMIUM);

        Map<String, Integer> phaseCompleted = new ConcurrentHashMap<>();
        ScanProgressCallback callback = (phase, completed, total) -> phaseCompleted.put(phase, completed);

        ScanResult result = orchestrator.orchestrate(request, callback).get(5, TimeUnit.SECONDS);

        assertThat(result).isNotNull();
        assertThat(phaseCompleted).containsEntry("Source Code Scanning", 1);
        assertThat(phaseCompleted).containsEntry("Dependency Analysis", 2);
        assertThat(phaseCompleted).containsEntry("Advanced Scans", 3);
    }
}
