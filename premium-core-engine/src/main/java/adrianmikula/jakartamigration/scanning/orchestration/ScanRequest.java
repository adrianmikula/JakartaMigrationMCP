package adrianmikula.jakartamigration.scanning.orchestration;

import adrianmikula.jakartamigration.config.FeatureFlagsProperties;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;

/**
 * Request for a unified project scan.
 */
public record ScanRequest(
        Path projectPath,
        ScanMode mode,
        Set<String> requestedScanTypes,
        FeatureFlagsProperties.LicenseTier licenseTier
) {
    public ScanRequest {
        Objects.requireNonNull(projectPath, "projectPath is required");
        Objects.requireNonNull(mode, "mode is required");
        if (requestedScanTypes == null) {
            requestedScanTypes = Set.of();
        }
        if (licenseTier == null) {
            licenseTier = FeatureFlagsProperties.LicenseTier.PREMIUM;
        }
    }
}
