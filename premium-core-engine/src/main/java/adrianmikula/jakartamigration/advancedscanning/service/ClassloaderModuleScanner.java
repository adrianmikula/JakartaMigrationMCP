package adrianmikula.jakartamigration.advancedscanning.service;

import adrianmikula.jakartamigration.advancedscanning.domain.ClassloaderModuleProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.ClassloaderModuleScanResult;
import java.nio.file.Path;
import java.util.List;

public interface ClassloaderModuleScanner {
    ClassloaderModuleProjectScanResult scanProject(Path projectPath);
    
    /**
     * Scans a project using pre-discovered Java files.
     * @param filesToScan list of Java file paths to scan
     * @return Project scan result with all findings
     */
    ClassloaderModuleProjectScanResult scanProject(List<Path> filesToScan);
    
    ClassloaderModuleScanResult scanFile(Path filePath);
}
