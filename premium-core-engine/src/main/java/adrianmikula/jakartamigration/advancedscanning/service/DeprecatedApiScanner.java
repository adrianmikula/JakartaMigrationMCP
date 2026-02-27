package adrianmikula.jakartamigration.advancedscanning.service;

import adrianmikula.jakartamigration.advancedscanning.domain.DeprecatedApiProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.DeprecatedApiScanResult;
import java.nio.file.Path;

public interface DeprecatedApiScanner {
    DeprecatedApiProjectScanResult scanProject(Path projectPath);
    DeprecatedApiScanResult scanFile(Path filePath);
}
