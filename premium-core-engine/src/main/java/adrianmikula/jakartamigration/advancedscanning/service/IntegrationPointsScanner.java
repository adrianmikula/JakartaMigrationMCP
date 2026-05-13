package adrianmikula.jakartamigration.advancedscanning.service;

import adrianmikula.jakartamigration.advancedscanning.domain.IntegrationPointsProjectScanResult;
import java.nio.file.Path;
import java.util.List;

/**
 * Scanner for detecting integration points (RMI, CORBA, etc.) with javax.* dependencies.
 */
public interface IntegrationPointsScanner {
    IntegrationPointsProjectScanResult scanProject(Path projectPath);
    
    /**
     * Scans a project using pre-discovered Java files.
     * @param filesToScan list of Java file paths to scan
     * @return Project scan result with all findings
     */
    IntegrationPointsProjectScanResult scanProject(List<Path> filesToScan);
}
