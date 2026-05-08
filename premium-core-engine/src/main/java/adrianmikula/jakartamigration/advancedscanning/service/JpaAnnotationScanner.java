package adrianmikula.jakartamigration.advancedscanning.service;

import adrianmikula.jakartamigration.advancedscanning.domain.FileScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.JpaAnnotationUsage;
import adrianmikula.jakartamigration.advancedscanning.domain.ProjectScanResult;

import java.nio.file.Path;
import java.util.List;

/**
 * Service for scanning source code for javax.persistence.* (JPA/Hibernate) usage.
 * This is a premium feature that provides detailed analysis of JPA annotations.
 */
public interface JpaAnnotationScanner {
    ProjectScanResult<FileScanResult<JpaAnnotationUsage>> scanProject(Path projectPath);
    
    /**
     * Scans a project using pre-discovered Java files.
     * @param filesToScan list of Java file paths to scan
     * @return Project scan result with all findings
     */
    ProjectScanResult<FileScanResult<JpaAnnotationUsage>> scanProject(List<Path> filesToScan);
    
    FileScanResult<JpaAnnotationUsage> scanFile(Path filePath);
}
