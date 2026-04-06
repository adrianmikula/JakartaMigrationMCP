package adrianmikula.jakartamigration.advancedscanning.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Result of scanning an entire project for logging/metrics API usage.
 */
@Getter
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoggingMetricsProjectScanResult {
    private final String projectPath;
    private final List<LoggingMetricsScanResult> fileResults;
    private final int totalFilesScanned;
    private final int totalFindings;

    public LoggingMetricsProjectScanResult(String projectPath) {
        this.projectPath = projectPath;
        this.fileResults = new ArrayList<>();
        this.totalFilesScanned = 0;
        this.totalFindings = 0;
    }

    public LoggingMetricsProjectScanResult(String projectPath,
            List<LoggingMetricsScanResult> fileResults) {
        this.projectPath = projectPath;
        this.fileResults = fileResults;
        this.totalFilesScanned = fileResults.size();
        this.totalFindings = fileResults.stream()
                .mapToInt(LoggingMetricsScanResult::getTotalFindings)
                .sum();
    }

    /**
     * Returns all usages across all scanned files.
     */
    @JsonIgnore
    public List<LoggingMetricsUsage> getAllUsages() {
        return fileResults.stream()
                .flatMap(r -> r.getUsages().stream())
                .collect(Collectors.toList());
    }

    /**
     * Returns findings grouped by usage type.
     */
    @JsonIgnore
    public List<LoggingMetricsUsage> getUsagesByType(String usageType) {
        return getAllUsages().stream()
                .filter(u -> u.getUsageType().equals(usageType))
                .collect(Collectors.toList());
    }

    /**
     * Returns true if any findings were detected.
     */
    @JsonIgnore
    public boolean hasFindings() {
        return totalFindings > 0;
    }

    /**
     * Returns a risk level based on the number of findings.
     */
    @JsonIgnore
    public RiskLevel getRiskLevel() {
        if (totalFindings == 0)
            return RiskLevel.NONE;
        if (totalFindings < 5)
            return RiskLevel.LOW;
        if (totalFindings < 20)
            return RiskLevel.MEDIUM;
        return RiskLevel.HIGH;
    }

    public enum RiskLevel {
        NONE, LOW, MEDIUM, HIGH
    }
}
