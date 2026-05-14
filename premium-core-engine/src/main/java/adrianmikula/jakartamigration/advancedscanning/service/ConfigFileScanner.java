package adrianmikula.jakartamigration.advancedscanning.service;

import adrianmikula.jakartamigration.advancedscanning.domain.ConfigFileProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.ConfigFileScanResult;
import java.nio.file.Path;
import java.util.List;

public interface ConfigFileScanner {
    ConfigFileProjectScanResult scanProject(Path projectPath);
    
    /**
     * Scans a project using pre-discovered config files.
     * @param filesToScan list of config file paths (.xml, .properties, .yaml, .yml) to scan
     * @return Project scan result with all findings
     */
    ConfigFileProjectScanResult scanProject(List<Path> filesToScan);
    
    ConfigFileScanResult scanFile(Path filePath);
}
