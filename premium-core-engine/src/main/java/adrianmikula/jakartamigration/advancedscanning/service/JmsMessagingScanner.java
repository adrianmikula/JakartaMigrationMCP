package adrianmikula.jakartamigration.advancedscanning.service;

import adrianmikula.jakartamigration.advancedscanning.domain.JmsMessagingProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.JmsMessagingScanResult;
import java.nio.file.Path;
import java.util.List;

public interface JmsMessagingScanner {
    JmsMessagingProjectScanResult scanProject(Path projectPath);
    
    /**
     * Scans a project using pre-discovered Java files.
     * @param filesToScan list of Java file paths to scan
     * @return Project scan result with all findings
     */
    JmsMessagingProjectScanResult scanProject(List<Path> filesToScan);
    
    JmsMessagingScanResult scanFile(Path filePath);
}
