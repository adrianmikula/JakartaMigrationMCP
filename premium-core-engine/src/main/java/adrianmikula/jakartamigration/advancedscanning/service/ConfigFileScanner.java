package adrianmikula.jakartamigration.advancedscanning.service;

import adrianmikula.jakartamigration.advancedscanning.domain.ConfigFileProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.ConfigFileScanResult;
import java.nio.file.Path;

public interface ConfigFileScanner {
    ConfigFileProjectScanResult scanProject(Path projectPath);
    ConfigFileScanResult scanFile(Path filePath);
}
