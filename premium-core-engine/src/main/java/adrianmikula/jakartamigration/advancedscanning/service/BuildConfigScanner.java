package adrianmikula.jakartamigration.advancedscanning.service;

import adrianmikula.jakartamigration.advancedscanning.domain.BuildConfigUsage;
import adrianmikula.jakartamigration.advancedscanning.domain.FileScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.ProjectScanResult;

import java.nio.file.Path;
import java.util.List;

/**
 * Service for scanning build configuration files for javax.* dependencies.
 * Supports Maven pom.xml and Gradle build.gradle files.
 */
public interface BuildConfigScanner {

    ProjectScanResult<FileScanResult<BuildConfigUsage>> scanProject(Path projectPath);
    
    /**
     * Scans a project using pre-discovered build files.
     * @param filesToScan list of build file paths (pom.xml, build.gradle, etc.) to scan
     * @return Project scan result with all findings
     */
    ProjectScanResult<FileScanResult<BuildConfigUsage>> scanProject(List<Path> filesToScan);
    
    FileScanResult<BuildConfigUsage> scanFile(Path filePath);
}
