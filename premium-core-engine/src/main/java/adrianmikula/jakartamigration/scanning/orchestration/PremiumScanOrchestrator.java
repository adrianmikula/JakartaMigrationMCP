package adrianmikula.jakartamigration.scanning.orchestration;

import adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults;
import adrianmikula.jakartamigration.advancedscanning.service.ScanProgressCallback;
import adrianmikula.jakartamigration.config.FeatureFlag;
import adrianmikula.jakartamigration.config.FeatureFlagsService;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyAnalysisReport;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyAnalysisModule;
import adrianmikula.jakartamigration.sourcecodescanning.domain.SourceCodeAnalysisResult;
import adrianmikula.jakartamigration.sourcecodescanning.service.SourceCodeScanner;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Premium mode-aware scan orchestrator that starts source scanning in parallel with
 * dependency analysis and then runs the advanced scan phase.
 */
@Slf4j
public class PremiumScanOrchestrator implements ScanOrchestrator {

    private final SourceCodeScanner sourceCodeScanner;
    private final DependencyAnalysisModule dependencyAnalysisModule;
    private final AdvancedScanningEngine advancedScanningEngine;
    private final FeatureFlagsService featureFlagsService;

    public PremiumScanOrchestrator(SourceCodeScanner sourceCodeScanner,
                                   DependencyAnalysisModule dependencyAnalysisModule,
                                   AdvancedScanningEngine advancedScanningEngine,
                                   FeatureFlagsService featureFlagsService) {
        this.sourceCodeScanner = sourceCodeScanner;
        this.dependencyAnalysisModule = dependencyAnalysisModule;
        this.advancedScanningEngine = advancedScanningEngine;
        this.featureFlagsService = featureFlagsService;
    }

    @Override
    public CompletableFuture<ScanResult> orchestrate(ScanRequest request, ScanProgressCallback progressCallback) {
        Path projectPath = request.projectPath();
        ScanMode mode = request.mode();

        if (mode == ScanMode.DEEP) {
            featureFlagsService.requireEnabled(FeatureFlag.ADVANCED_ANALYSIS);
        }

        reportPhase(progressCallback, "Source Code Scanning", 0, 3);
        CompletableFuture<SourceCodeAnalysisResult> sourceFuture = CompletableFuture.supplyAsync(() -> {
            log.info("PremiumScanOrchestrator: starting source code scanning for {}", projectPath);
            return sourceCodeScanner.scanProject(projectPath);
        });

        reportPhase(progressCallback, "Dependency Analysis", 0, 3);
        CompletableFuture<DependencyAnalysisReport> depFuture = CompletableFuture.supplyAsync(() -> {
            log.info("PremiumScanOrchestrator: starting dependency analysis for {}", projectPath);
            return dependencyAnalysisModule.analyzeProject(projectPath);
        });

        reportPhase(progressCallback, "Advanced Scans", 0, 3);
        CompletableFuture<ComprehensiveScanResults> advFuture = CompletableFuture.supplyAsync(() -> {
            log.info("PremiumScanOrchestrator: starting advanced scanning for {}", projectPath);
            return advancedScanningEngine.runAdvancedScans(projectPath, mode, progressCallback);
        });

        return sourceFuture.thenCombine(depFuture, Partial::new)
                .thenCombine(advFuture, (partial, advanced) ->
                        new ScanResult(partial.dependency, partial.source, advanced));
    }

    private static void reportPhase(ScanProgressCallback callback, String phase, int completed, int total) {
        if (callback != null) {
            callback.onPhaseProgress(phase, completed, total);
        }
    }

    private record Partial(SourceCodeAnalysisResult source, DependencyAnalysisReport dependency) {
    }
}
