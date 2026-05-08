package adrianmikula.jakartamigration.advancedscanning.service;

import adrianmikula.jakartamigration.advancedscanning.domain.ThirdPartyLibProjectScanResult;
import java.nio.file.Path;
import java.util.List;

/**
 * Scanner interface for detecting third-party libraries that haven't been migrated to Jakarta EE.
 * This scanner analyzes Maven pom.xml and Gradle build.gradle files.
 */
public interface ThirdPartyLibScanner {
    
    /**
     * Scans a project for third-party libraries that need migration.
     * 
     * @param projectPath Path to the project root directory
     * @return ThirdPartyLibProjectScanResult containing all findings
     */
    ThirdPartyLibProjectScanResult scanProject(Path projectPath);
    
    /**
     * Scans a project using pre-discovered build files (pom.xml, build.gradle, Dockerfile).
     * @param filesToScan list of build file paths to scan
     * @return ThirdPartyLibProjectScanResult containing all findings
     */
    ThirdPartyLibProjectScanResult scanProject(List<Path> filesToScan);
}
