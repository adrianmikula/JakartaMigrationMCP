package adrianmikula.jakartamigration.advancedscanning.service;

import adrianmikula.jakartamigration.advancedscanning.domain.CdiInjectionProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.CdiInjectionScanResult;

import java.nio.file.Path;

/**
 * Service for scanning source code for javax.inject and javax.enterprise (CDI) usage.
 * This is a premium feature that provides detailed analysis of dependency injection annotations.
 */
public interface CdiInjectionScanner {

    /**
     * Scans a project for javax.inject and javax.enterprise usage in source code.
     *
     * @param projectPath Path to the project root directory
     * @return Project scan result with all files containing CDI usage
     */
    CdiInjectionProjectScanResult scanProject(Path projectPath);

    /**
     * Scans a single file for CDI usage.
     *
     * @param filePath Path to the Java file to scan
     * @return File scan result with CDI usages found
     */
    CdiInjectionScanResult scanFile(Path filePath);
}
