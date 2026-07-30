package adrianmikula.jakartamigration.scanning.orchestration;

import adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyAnalysisReport;
import adrianmikula.jakartamigration.sourcecodescanning.domain.SourceCodeAnalysisResult;

/**
 * Aggregate result of a unified project scan.
 */
public record ScanResult(
        DependencyAnalysisReport dependencyReport,
        SourceCodeAnalysisResult sourceCodeResult,
        ComprehensiveScanResults advancedScanResults
) {
}
