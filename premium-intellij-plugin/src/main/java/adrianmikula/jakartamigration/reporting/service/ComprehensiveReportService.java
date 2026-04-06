package adrianmikula.jakartamigration.reporting.service;

import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.reporting.domain.ComprehensiveScanResults;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Map;

/**
 * Service interface for generating comprehensive migration reports
 * that combine dashboard data, dependency analysis, and scan results
 * into professional PDF documents.
 */
public interface ComprehensiveReportService {
    
    /**
     * Generates a comprehensive PDF report combining:
     * - Risk scores from dashboard
     * - Circular dependency graph visualization
     * - Detailed results from all basic and advanced scans
     * - Footer with support tab links
     *
     * @param project Current IntelliJ project
     * @param dependencyGraph Dependency graph for circular layout
     * @param scanResults Comprehensive scan results
     * @param outputPath Output path for the PDF
     * @param customData Custom data like project name, version, etc.
     * @return Path to generated PDF file
     */
    String generateComprehensiveReport(
            @NotNull Project project,
            @NotNull DependencyGraph dependencyGraph,
            @NotNull ComprehensiveScanResults scanResults,
            @NotNull String outputPath,
            @NotNull Map<String, String> customData
    );
}
