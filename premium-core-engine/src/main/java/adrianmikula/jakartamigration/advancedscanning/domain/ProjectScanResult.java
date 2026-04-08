package adrianmikula.jakartamigration.advancedscanning.domain;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Generic project-level scan result that consolidates all the duplicate ProjectScanResult classes.
 * Replaces: BeanValidationProjectScanResult, BuildConfigProjectScanResult, CdiInjectionProjectScanResult,
 * ConfigFileProjectScanResult, DeprecatedApiProjectScanResult, IntegrationPointsProjectScanResult,
 * JmsMessagingProjectScanResult, JpaProjectScanResult, LoggingMetricsProjectScanResult,
 * ReflectionUsageProjectScanResult, RestSoapProjectScanResult, SecurityApiProjectScanResult,
 * SerializationCacheProjectScanResult, ServletJspProjectScanResult, TestContainersProjectScanResult,
 * ThirdPartyLibProjectScanResult, TransitiveDependencyProjectScanResult, UnitTestProjectScanResult
 *
 * @param <T> The type of file scan result (e.g., BeanValidationScanResult)
 */
public record ProjectScanResult<T>(
    List<T> fileResults,
    int totalFilesScanned,
    int filesWithIssues,
    int totalIssuesFound
) {
    public ProjectScanResult {
        Objects.requireNonNull(fileResults, "fileResults cannot be null");
        fileResults = List.copyOf(fileResults);
    }

    /**
     * Returns true if any javax.* usage was found.
     */
    public boolean hasIssues() {
        return totalIssuesFound > 0;
    }

    /**
     * Returns true if any files with issues were found.
     */
    public boolean hasFilesWithIssues() {
        return filesWithIssues > 0;
    }

    /**
     * Creates an empty result.
     */
    public static <T> ProjectScanResult<T> empty() {
        return new ProjectScanResult<>(Collections.emptyList(), 0, 0, 0);
    }
}
