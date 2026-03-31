package adrianmikula.jakartamigration.reporting.service.impl;

import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.reporting.domain.ComprehensiveScanResults;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * TEMPORARY STUB: Implementation of comprehensive report service.
 * PDF generation temporarily disabled due to compilation issues.
 */
public class ComprehensiveReportServiceImpl implements adrianmikula.jakartamigration.reporting.service.ComprehensiveReportService {

    @Override
    public String generateComprehensiveReport(
            @NotNull Project project,
            @NotNull DependencyGraph dependencyGraph,
            @NotNull ComprehensiveScanResults scanResults,
            @NotNull String outputPath,
            @NotNull Map<String, String> customData
    ) {
        // TEMPORARY: Return stub message until compilation issues are resolved
        return "PDF generation temporarily disabled - compilation issues being resolved. " +
               "All other features (experimental features, support links, migration phases, " +
               "Jakarta-compatible version display) are working correctly.";
    }
}
