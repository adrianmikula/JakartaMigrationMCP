package adrianmikula.jakartamigration.advancedscanning.service;

import adrianmikula.jakartamigration.advancedscanning.domain.AppServerProjectScanResult;
import java.nio.file.Path;
import java.util.List;

/**
 * Scanner for detecting application server configurations that need migration.
 */
public interface AppServerScanner {
    AppServerProjectScanResult scanProject(Path projectPath);
    
    /**
     * Scans a project using pre-discovered Java files.
     * @param filesToScan list of Java file paths to scan
     * @return Project scan result with all findings
     */
    AppServerProjectScanResult scanProject(List<Path> filesToScan);
}
