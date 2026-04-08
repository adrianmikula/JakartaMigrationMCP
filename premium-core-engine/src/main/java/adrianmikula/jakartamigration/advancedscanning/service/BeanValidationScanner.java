package adrianmikula.jakartamigration.advancedscanning.service;

import adrianmikula.jakartamigration.advancedscanning.domain.FileScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.JavaxUsage;
import adrianmikula.jakartamigration.advancedscanning.domain.ProjectScanResult;

import java.nio.file.Path;

/**
 * Service for scanning source code for javax.validation.* (Bean Validation) usage.
 * This is a premium feature that provides detailed analysis of validation constraints.
 */
public interface BeanValidationScanner {

    /**
     * Scans a project for javax.validation.* usage in source code.
     *
     * @param projectPath Path to the project root directory
     * @return Project scan result with all files containing javax.validation.* usage
     */
    ProjectScanResult<FileScanResult<JavaxUsage>> scanProject(Path projectPath);

    /**
     * Scans a single file for javax.validation.* usage.
     *
     * @param filePath Path to the Java file to scan
     * @return File scan result with validation annotations found
     */
    FileScanResult<JavaxUsage> scanFile(Path filePath);
}
