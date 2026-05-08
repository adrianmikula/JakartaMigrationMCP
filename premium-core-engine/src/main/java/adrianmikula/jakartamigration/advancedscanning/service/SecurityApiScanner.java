package adrianmikula.jakartamigration.advancedscanning.service;

import adrianmikula.jakartamigration.advancedscanning.domain.SecurityApiProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.SecurityApiScanResult;
import java.nio.file.Path;
import java.util.List;

public interface SecurityApiScanner {
    SecurityApiProjectScanResult scanProject(Path projectPath);
    
    /**
     * Scans a project using pre-discovered Java files.
     * @param filesToScan list of Java file paths to scan
     * @return Project scan result with all findings
     */
    SecurityApiProjectScanResult scanProject(List<Path> filesToScan);
    
    SecurityApiScanResult scanFile(Path filePath);
}
