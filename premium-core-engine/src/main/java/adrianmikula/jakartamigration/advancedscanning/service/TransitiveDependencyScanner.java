package adrianmikula.jakartamigration.advancedscanning.service;

import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyScanResult;
import java.nio.file.Path;

public interface TransitiveDependencyScanner {
    TransitiveDependencyProjectScanResult scanProject(Path projectPath);
    TransitiveDependencyScanResult scanFile(Path filePath);

    /**
     * Scans a project with optional progress reporting.
     *
     * @param projectPath Path to the project root
     * @param progressListener Optional callback for progress updates, may be null
     * @return TransitiveDependencyProjectScanResult with all dependencies
     */
    TransitiveDependencyProjectScanResult scanProject(Path projectPath, ScanProgressCallback progressListener);
}
