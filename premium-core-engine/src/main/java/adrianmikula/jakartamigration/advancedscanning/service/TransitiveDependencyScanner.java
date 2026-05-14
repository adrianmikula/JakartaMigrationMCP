package adrianmikula.jakartamigration.advancedscanning.service;

import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyScanResult;
import java.nio.file.Path;
import java.util.List;

public interface TransitiveDependencyScanner {
    TransitiveDependencyProjectScanResult scanProject(Path projectPath);
    
    /**
     * Scans a project using pre-discovered build files.
     * @param filesToScan list of build file paths (pom.xml, build.gradle) to scan
     * @return Project scan result with all findings
     */
    TransitiveDependencyProjectScanResult scanProject(List<Path> filesToScan);
    
    TransitiveDependencyScanResult scanFile(Path filePath);

    /**
     * Scans a project with optional progress reporting.
     *
     * @param projectPath Path to the project root
     * @param progressListener Optional callback for progress updates, may be null
     * @return TransitiveDependencyProjectScanResult with all dependencies
     */
    TransitiveDependencyProjectScanResult scanProject(Path projectPath, ScanProgressCallback progressListener);
    
    /**
     * Scans a project using pre-discovered build files with progress reporting.
     * @param filesToScan list of build file paths to scan
     * @param progressListener Optional callback for progress updates
     * @return Project scan result with all findings
     */
    TransitiveDependencyProjectScanResult scanProject(List<Path> filesToScan, ScanProgressCallback progressListener);
}
