package adrianmikula.jakartamigration.advancedscanning.service;

import adrianmikula.jakartamigration.advancedscanning.domain.ClassloaderModuleProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.ClassloaderModuleScanResult;
import java.nio.file.Path;

public interface ClassloaderModuleScanner {
    ClassloaderModuleProjectScanResult scanProject(Path projectPath);
    ClassloaderModuleScanResult scanFile(Path filePath);
}
