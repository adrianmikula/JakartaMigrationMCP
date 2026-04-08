package adrianmikula.jakartamigration.advancedscanning.service;

import adrianmikula.jakartamigration.advancedscanning.domain.FileScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.JpaAnnotationUsage;
import adrianmikula.jakartamigration.advancedscanning.domain.ProjectScanResult;

import java.nio.file.Path;

/**
 * Service for scanning source code for javax.persistence.* (JPA/Hibernate) usage.
 * This is a premium feature that provides detailed analysis of JPA annotations.
 */
public interface JpaAnnotationScanner {
    ProjectScanResult<FileScanResult<JpaAnnotationUsage>> scanProject(Path projectPath);
    FileScanResult<JpaAnnotationUsage> scanFile(Path filePath);
}
