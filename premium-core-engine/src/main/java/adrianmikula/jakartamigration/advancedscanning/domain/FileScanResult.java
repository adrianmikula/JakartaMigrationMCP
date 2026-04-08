package adrianmikula.jakartamigration.advancedscanning.domain;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Generic file-level scan result that consolidates all the duplicate ScanResult classes.
 * Replaces: BeanValidationScanResult, BuildConfigScanResult, CdiInjectionScanResult,
 * ConfigFileScanResult, DeprecatedApiScanResult, IntegrationPointsScanResult,
 * JmsMessagingScanResult, JpaScanResult, LoggingMetricsScanResult, ReflectionUsageScanResult,
 * RestSoapScanResult, SecurityApiScanResult, SerializationCacheScanResult, ServletJspScanResult,
 * TestContainersScanResult, ThirdPartyLibScanResult, TransitiveDependencyScanResult, UnitTestScanResult
 *
 * @param <T> The type of usage found (e.g., BeanValidationUsage)
 */
public record FileScanResult<T>(
    Path filePath,
    List<T> usages,
    int lineCount
) {
    public FileScanResult {
        Objects.requireNonNull(filePath, "filePath cannot be null");
        Objects.requireNonNull(usages, "usages cannot be null");
        usages = List.copyOf(usages);
    }

    /**
     * Returns true if any javax.* usage was found.
     */
    public boolean hasIssues() {
        return !usages.isEmpty();
    }

    /**
     * Creates an empty result.
     */
    public static <T> FileScanResult<T> empty(Path filePath) {
        return new FileScanResult<>(filePath, Collections.emptyList(), 0);
    }
}
