package adrianmikula.jakartamigration.advancedscanning.service;

import adrianmikula.jakartamigration.advancedscanning.domain.UnitTestProjectScanResult;
import java.nio.file.Path;
import java.util.List;

/**
 * Scanner for detecting javax.* usage in test files.
 */
public interface UnitTestScanner {
    UnitTestProjectScanResult scanProject(Path projectPath);
    
    /**
     * Scans a project using pre-discovered test files.
     * @param filesToScan list of test file paths to scan
     * @return Project scan result with all findings
     */
    UnitTestProjectScanResult scanProject(List<Path> filesToScan);
}
