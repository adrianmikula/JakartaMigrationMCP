package adrianmikula.jakartamigration.advancedscanning.service;

import adrianmikula.jakartamigration.advancedscanning.domain.SecurityApiProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.SecurityApiScanResult;
import java.nio.file.Path;

public interface SecurityApiScanner {
    SecurityApiProjectScanResult scanProject(Path projectPath);
    SecurityApiScanResult scanFile(Path filePath);
}
