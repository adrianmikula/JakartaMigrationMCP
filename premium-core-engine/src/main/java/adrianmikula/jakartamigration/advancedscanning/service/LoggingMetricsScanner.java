package adrianmikula.jakartamigration.advancedscanning.service;

import adrianmikula.jakartamigration.advancedscanning.domain.LoggingMetricsProjectScanResult;
import java.nio.file.Path;
import java.util.List;

/**
 * Scanner interface for detecting javax.logging and JMX API usage.
 * This scanner helps identify observability/monitoring code that needs migration.
 */
public interface LoggingMetricsScanner {
    
    /**
     * Scans a project for logging and metrics API usage.
     * 
     * @param projectPath Path to the project root directory
     * @return LoggingMetricsProjectScanResult containing all findings
     */
    LoggingMetricsProjectScanResult scanProject(Path projectPath);
    
    /**
     * Scans a project using pre-discovered Java files.
     * @param filesToScan list of Java file paths to scan
     * @return Project scan result with all findings
     */
    LoggingMetricsProjectScanResult scanProject(List<Path> filesToScan);
}
