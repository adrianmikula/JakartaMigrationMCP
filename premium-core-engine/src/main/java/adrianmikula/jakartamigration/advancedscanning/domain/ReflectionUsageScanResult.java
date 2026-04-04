package adrianmikula.jakartamigration.advancedscanning.domain;

import java.util.List;

/**
 * Result of scanning a single file for reflection usage.
 */
public class ReflectionUsageScanResult {
    private final String filePath;
    private final List<ReflectionUsage> usages;

    public ReflectionUsageScanResult(String filePath, List<ReflectionUsage> usages) {
        this.filePath = filePath;
        this.usages = usages;
    }

    public String getFilePath() {
        return filePath;
    }

    public List<ReflectionUsage> getUsages() {
        return usages;
    }

    /**
     * Returns true if this file has any reflection usage findings.
     */
    public boolean hasFindings() {
        return usages != null && !usages.isEmpty();
    }

    /**
     * Returns the total number of reflection usages found.
     */
    public int totalUsagesFound() {
        return usages != null ? usages.size() : 0;
    }
}
