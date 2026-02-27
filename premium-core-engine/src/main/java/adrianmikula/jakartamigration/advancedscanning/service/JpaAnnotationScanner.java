package adrianmikula.jakartamigration.advancedscanning.service;

import adrianmikula.jakartamigration.advancedscanning.domain.JpaProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.JpaScanResult;

import java.nio.file.Path;

/**
 * Service for scanning source code for javax.persistence.* (JPA/Hibernate) usage.
 * This is a premium feature that provides detailed analysis of JPA annotations.
 */
public interface JpaAnnotationScanner {

    /**
     * Scans a project for javax.persistence.* usage in source code.
     *
     * @param projectPath Path to the project root directory
     * @return Project scan result with all files containing javax.persistence.* usage
     */
    JpaProjectScanResult scanProject(Path projectPath);

    /**
     * Scans a single file for javax.persistence.* usage.
     *
     * @param filePath Path to the Java file to scan
     * @return File scan result with JPA annotations found
     */
    JpaScanResult scanFile(Path filePath);
}
