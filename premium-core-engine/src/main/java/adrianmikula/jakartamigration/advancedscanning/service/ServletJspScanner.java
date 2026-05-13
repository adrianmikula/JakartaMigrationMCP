package adrianmikula.jakartamigration.advancedscanning.service;

import adrianmikula.jakartamigration.advancedscanning.domain.FileScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.ProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.ServletJspUsage;

import java.nio.file.Path;
import java.util.List;

public interface ServletJspScanner {
    ProjectScanResult<FileScanResult<ServletJspUsage>> scanProject(Path projectPath);
    
    /**
     * Scans a project using pre-discovered files (Java and JSP).
     * @param filesToScan list of file paths (.java and .jsp) to scan
     * @return Project scan result with all findings
     */
    ProjectScanResult<FileScanResult<ServletJspUsage>> scanProject(List<Path> filesToScan);
    
    FileScanResult<ServletJspUsage> scanFile(Path filePath);
}
