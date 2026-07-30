package adrianmikula.jakartamigration.scanning.orchestration;

import adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults;
import adrianmikula.jakartamigration.config.FeatureFlagsProperties;
import adrianmikula.jakartamigration.config.FeatureFlagsService;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyAnalysisReport;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyAnalysisModule;
import adrianmikula.jakartamigration.sourcecodescanning.domain.SourceCodeAnalysisResult;
import adrianmikula.jakartamigration.sourcecodescanning.service.SourceCodeScanner;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("slow")
@ExtendWith(MockitoExtension.class)
class PremiumScanOrchestratorPerformanceTest {

    @Mock
    private SourceCodeScanner sourceCodeScanner;
    @Mock
    private DependencyAnalysisModule dependencyAnalysisModule;
    @Mock
    private AdvancedScanningEngine advancedScanningEngine;
    @Mock
    private FeatureFlagsService featureFlagsService;

    @Test
    void quickScan_runs_source_and_dependency_in_parallel() throws Exception {
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

        when(sourceCodeScanner.scanProject(projectPath)).thenAnswer(invocation -> {
            Thread.sleep(100);
            return sourceResult;
        });
        when(dependencyAnalysisModule.analyzeProject(projectPath)).thenAnswer(invocation -> {
            Thread.sleep(100);
            return depResult;
        });
        when(advancedScanningEngine.runAdvancedScans(eq(projectPath), eq(ScanMode.QUICK), any()))
                .thenAnswer(invocation -> {
                    Thread.sleep(50);
                    return advancedResult;
                });

        PremiumScanOrchestrator orchestrator = new PremiumScanOrchestrator(
                sourceCodeScanner, dependencyAnalysisModule, advancedScanningEngine, featureFlagsService);

        ScanRequest request = new ScanRequest(
                projectPath, ScanMode.QUICK, Set.of(), FeatureFlagsProperties.LicenseTier.PREMIUM);

        long start = System.currentTimeMillis();
        ScanResult result = orchestrator.orchestrate(request, (phase, completed, total) -> {})
                .get(1, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - start;

        assertThat(result.sourceCodeResult()).isEqualTo(sourceResult);
        assertThat(result.dependencyReport()).isEqualTo(depResult);
        assertThat(result.advancedScanResults()).isEqualTo(advancedResult);
        // Parallel start means total wall time should be well under the sequential 250ms.
        assertThat(elapsed).isLessThan(250);
    }
}
