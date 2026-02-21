package adrianmikula.jakartamigration.advancedscanning.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of scanning a single file for serialization/cache compatibility issues.
 */
public class SerializationCacheScanResult {
    private final String filePath;
    private final List<SerializationCacheUsage> usages;
    private final int totalFindings;

    public SerializationCacheScanResult(String filePath) {
        this.filePath = filePath;
        this.usages = new ArrayList<>();
        this.totalFindings = 0;
    }

    public SerializationCacheScanResult(String filePath, List<SerializationCacheUsage> usages) {
        this.filePath = filePath;
        this.usages = usages;
        this.totalFindings = usages.size();
    }

    public String getFilePath() {
        return filePath;
    }

    public List<SerializationCacheUsage> getUsages() {
        return usages;
    }

    public int getTotalFindings() {
        return totalFindings;
    }

    public boolean hasFindings() {
        return !usages.isEmpty();
    }
}
