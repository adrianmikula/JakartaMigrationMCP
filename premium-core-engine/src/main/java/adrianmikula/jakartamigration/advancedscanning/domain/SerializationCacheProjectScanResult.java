package adrianmikula.jakartamigration.advancedscanning.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Result of scanning an entire project for serialization/cache compatibility issues.
 */
public class SerializationCacheProjectScanResult {
    private final String projectPath;
    private final List<SerializationCacheScanResult> fileResults;
    private final int totalFilesScanned;
    private final int totalFindings;

    public SerializationCacheProjectScanResult(String projectPath) {
        this.projectPath = projectPath;
        this.fileResults = new ArrayList<>();
        this.totalFilesScanned = 0;
        this.totalFindings = 0;
    }

    public SerializationCacheProjectScanResult(String projectPath, 
                                               List<SerializationCacheScanResult> fileResults) {
        this.projectPath = projectPath;
        this.fileResults = fileResults;
        this.totalFilesScanned = fileResults.size();
        this.totalFindings = fileResults.stream()
            .mapToInt(SerializationCacheScanResult::getTotalFindings)
            .sum();
    }

    public String getProjectPath() {
        return projectPath;
    }

    public List<SerializationCacheScanResult> getFileResults() {
        return fileResults;
    }

    public int getTotalFilesScanned() {
        return totalFilesScanned;
    }

    public int getTotalFindings() {
        return totalFindings;
    }

    public List<SerializationCacheUsage> getAllUsages() {
        return fileResults.stream()
            .flatMap(r -> r.getUsages().stream())
            .collect(Collectors.toList());
    }

    public boolean hasFindings() {
        return totalFindings > 0;
    }

    /**
     * Returns findings grouped by usage type.
     */
    public List<SerializationCacheUsage> getUsagesByType(String usageType) {
        return getAllUsages().stream()
            .filter(u -> u.getUsageType().equals(usageType))
            .collect(Collectors.toList());
    }

    /**
     * Returns a risk level based on the number and type of findings.
     */
    public RiskLevel getRiskLevel() {
        if (totalFindings == 0) return RiskLevel.NONE;
        
        // Check for high-risk findings
        long highRiskCount = getAllUsages().stream()
            .filter(u -> u.getRiskAssessment().startsWith("HIGH"))
            .count();
        
        if (highRiskCount > 0) return RiskLevel.HIGH;
        if (totalFindings < 5) return RiskLevel.LOW;
        if (totalFindings < 20) return RiskLevel.MEDIUM;
        return RiskLevel.HIGH;
    }

    public enum RiskLevel {
        NONE, LOW, MEDIUM, HIGH
    }
}
