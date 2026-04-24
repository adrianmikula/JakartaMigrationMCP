package adrianmikula.jakartamigration.advancedscanning.service;

import adrianmikula.jakartamigration.advancedscanning.domain.DockerCicdUsage;
import adrianmikula.jakartamigration.advancedscanning.domain.FileScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.ProjectScanResult;

import java.nio.file.Path;

/**
 * Service for scanning Docker and CI/CD configuration files for Java references.
 * Detects Java usage in build, test, static analysis, packaging, and runtime contexts.
 */
public interface DockerCicdScanner {

    /**
     * Scans a project for Docker and CI/CD files containing Java references.
     *
     * @param projectPath The path to the project directory
     * @return ProjectScanResult containing all Java references found
     */
    ProjectScanResult<FileScanResult<DockerCicdUsage>> scanProject(Path projectPath);

    /**
     * Scans a single file for Java references.
     *
     * @param filePath The path to the file to scan
     * @return FileScanResult containing all Java references found
     */
    FileScanResult<DockerCicdUsage> scanFile(Path filePath);
}
