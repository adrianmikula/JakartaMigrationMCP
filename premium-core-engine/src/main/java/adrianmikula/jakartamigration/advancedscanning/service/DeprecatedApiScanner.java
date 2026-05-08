package adrianmikula.jakartamigration.advancedscanning.service;

import adrianmikula.jakartamigration.advancedscanning.domain.DeprecatedApiProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.DeprecatedApiScanResult;
import java.nio.file.Path;
import java.util.List;

public interface DeprecatedApiScanner {
    DeprecatedApiProjectScanResult scanProject(Path projectPath);
    
    /**
     * Scans a project using pre-discovered Java files.
     * @param filesToScan list of Java file paths to scan
     * @return Project scan result with all findings
     */
    DeprecatedApiProjectScanResult scanProject(List<Path> filesToScan);
    
    DeprecatedApiScanResult scanFile(Path filePath);
}
