package adrianmikula.jakartamigration.reporting.domain;

import java.util.List;

/**
 * Results from comprehensive scanning across all categories
 */
public class ComprehensiveScanResults {
    private final List<String> securityIssues;
    private final List<String> performanceIssues;
    private final List<String> compatibilityIssues;
    private final List<String> dataMigrationIssues;
    private final List<String> configurationIssues;
    private final List<String> buildSystemIssues;
    private final List<String> thirdPartyLibIssues;
    private final int totalDependencies;
    private final int totalIssuesFound;
    
    public ComprehensiveScanResults(
            List<String> securityIssues,
            List<String> performanceIssues,
            List<String> compatibilityIssues,
            List<String> dataMigrationIssues,
            List<String> configurationIssues,
            List<String> buildSystemIssues,
            List<String> thirdPartyLibIssues,
            int totalDependencies,
            int totalIssuesFound
    ) {
        this.securityIssues = securityIssues;
        this.performanceIssues = performanceIssues;
        this.compatibilityIssues = compatibilityIssues;
        this.dataMigrationIssues = dataMigrationIssues;
        this.configurationIssues = configurationIssues;
        this.buildSystemIssues = buildSystemIssues;
        this.thirdPartyLibIssues = thirdPartyLibIssues;
        this.totalDependencies = totalDependencies;
        this.totalIssuesFound = totalIssuesFound;
    }
    
    // Security-related getters
    public List<String> getSecurityIssues() {
        return securityIssues;
    }
    
    public int getSecurityApiCount() {
        return countCriticalIssues(securityIssues);
    }
    
    public int getDeprecatedApiCount() {
        return countCriticalIssues(compatibilityIssues);
    }
    
    // Performance-related getters
    public List<String> getPerformanceIssues() {
        return performanceIssues;
    }
    
    public int getSerializationCacheCount() {
        return countCriticalIssues(performanceIssues);
    }
    
    public int getClassloaderModuleCount() {
        return countCriticalIssues(performanceIssues);
    }
    
    public int getLoggingMetricsCount() {
        return countCriticalIssues(performanceIssues);
    }
    
    // Compatibility-related getters
    public List<String> getCompatibilityIssues() {
        return compatibilityIssues;
    }
    
    public int getConfigFileCount() {
        return countCriticalIssues(compatibilityIssues);
    }
    
    public int getBuildConfigCount() {
        return countCriticalIssues(compatibilityIssues);
    }
    
    public int getJmsMessagingCount() {
        return countCriticalIssues(compatibilityIssues);
    }
    
    public int getThirdPartyLibCount() {
        return countCriticalIssues(thirdPartyLibIssues);
    }
    
    // Data migration getters
    public List<String> getDataMigrationIssues() {
        return dataMigrationIssues;
    }
    
    public int getTransitiveDependencyCount() {
        return countCriticalIssues(dataMigrationIssues);
    }
    
    // Configuration getters
    public List<String> getConfigurationIssues() {
        return configurationIssues;
    }
    
    public int getBuildSystemCount() {
        return countCriticalIssues(buildSystemIssues);
    }
    
    // Third-party library getters
    public List<String> getThirdPartyLibIssues() {
        return thirdPartyLibIssues;
    }
    
    // Overall getters
    public int getTotalDependencies() {
        return totalDependencies;
    }
    
    public int getTotalIssuesFound() {
        return totalIssuesFound;
    }
    
    /**
     * Helper method to count critical issues in a list
     */
    private int countCriticalIssues(List<String> issues) {
        return issues != null ? issues.size() : 0;
    }
}
