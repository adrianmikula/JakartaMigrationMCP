package adrianmikula.jakartamigration.advancedscanning.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of scanning a single file for logging/metrics API usage.
 */
public class LoggingMetricsScanResult {
    private final String filePath;
    private final List<LoggingMetricsUsage> usages;
    private final int totalFindings;

    public LoggingMetricsScanResult(String filePath) {
        this.filePath = filePath;
        this.usages = new ArrayList<>();
        this.totalFindings = 0;
    }

    public LoggingMetricsScanResult(String filePath, List<LoggingMetricsUsage> usages) {
        this.filePath = filePath;
        this.usages = usages;
        this.totalFindings = usages.size();
    }

    public String getFilePath() {
        return filePath;
    }

    public List<LoggingMetricsUsage> getUsages() {
        return usages;
    }

    public int getTotalFindings() {
        return totalFindings;
    }

    public boolean hasFindings() {
        return !usages.isEmpty();
    }
}
