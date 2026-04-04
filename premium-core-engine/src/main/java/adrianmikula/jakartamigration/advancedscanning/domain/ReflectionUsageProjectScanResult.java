package adrianmikula.jakartamigration.advancedscanning.domain;

import java.util.List;

/**
 * Result of scanning a project for reflection usage.
 */
public class ReflectionUsageProjectScanResult {
    private final String projectPath;
    private final List<ReflectionUsageScanResult> fileResults;

    public ReflectionUsageProjectScanResult(String projectPath, List<ReflectionUsageScanResult> fileResults) {
        this.projectPath = projectPath;
        this.fileResults = fileResults;
    }

    public String getProjectPath() {
        return projectPath;
    }

    public List<ReflectionUsageScanResult> getFileResults() {
        return fileResults;
    }

    /**
     * Returns the total number of reflection usages found across all files.
     */
    public int getTotalUsagesFound() {
        return fileResults.stream()
                .mapToInt(ReflectionUsageScanResult::totalUsagesFound)
                .sum();
    }

    /**
     * Returns true if any reflection usage was found.
     */
    public boolean hasFindings() {
        return fileResults.stream()
                .anyMatch(ReflectionUsageScanResult::hasFindings);
    }

    /**
     * Returns the number of files with reflection usage findings.
     */
    public int getFilesWithFindings() {
        return (int) fileResults.stream()
                .filter(ReflectionUsageScanResult::hasFindings)
                .count();
    }
}
