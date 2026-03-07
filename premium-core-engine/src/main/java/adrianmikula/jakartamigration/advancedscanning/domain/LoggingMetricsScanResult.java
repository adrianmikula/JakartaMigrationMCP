package adrianmikula.jakartamigration.advancedscanning.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of scanning a single file for logging/metrics API usage.
 */
@Getter
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
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

    @JsonIgnore
    public boolean hasFindings() {
        return !usages.isEmpty();
    }
}
