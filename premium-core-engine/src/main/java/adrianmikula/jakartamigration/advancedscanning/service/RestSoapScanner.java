package adrianmikula.jakartamigration.advancedscanning.service;

import adrianmikula.jakartamigration.advancedscanning.domain.FileScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.JavaxUsage;
import adrianmikula.jakartamigration.advancedscanning.domain.ProjectScanResult;

import java.nio.file.Path;
import java.util.List;

public interface RestSoapScanner {
    ProjectScanResult<FileScanResult<JavaxUsage>> scanProject(Path projectPath);
    
    /**
     * Scans a project using pre-discovered Java files.
     * @param filesToScan list of Java file paths to scan
     * @return Project scan result with all findings
     */
    ProjectScanResult<FileScanResult<JavaxUsage>> scanProject(List<Path> filesToScan);
    
    FileScanResult<JavaxUsage> scanFile(Path filePath);
}
