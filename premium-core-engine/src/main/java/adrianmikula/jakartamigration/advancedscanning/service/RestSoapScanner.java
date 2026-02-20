package adrianmikula.jakartamigration.advancedscanning.service;

import adrianmikula.jakartamigration.advancedscanning.domain.RestSoapProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.RestSoapScanResult;

import java.nio.file.Path;

public interface RestSoapScanner {
    RestSoapProjectScanResult scanProject(Path projectPath);
    RestSoapScanResult scanFile(Path filePath);
}
