package adrianmikula.jakartamigration.advancedscanning.domain;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;

/**
 * Comprehensive results from all advanced scanning operations.
 * Aggregates results from multiple scanner types.
 */
public record ComprehensiveScanResults(
    String projectPath,
    LocalDateTime scanTime,
    Map<String, Object> jpaResults,
    Map<String, Object> beanValidationResults,
    Map<String, Object> servletJspResults,
    Map<String, Object> thirdPartyLibResults,
    Map<String, Object> transitiveDependencyResults,
    Map<String, Object> buildConfigResults,
    List<String> recommendations,
    int totalIssuesFound,
    ScanSummary summary
) {
    
    /**
     * Summary of scan results.
     */
    public record ScanSummary(
        int totalFilesScanned,
        int filesWithIssues,
        int criticalIssues,
        int warningIssues,
        int infoIssues,
        double readinessScore
    ) {}
}
