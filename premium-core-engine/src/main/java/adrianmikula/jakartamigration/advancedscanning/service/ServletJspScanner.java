package adrianmikula.jakartamigration.advancedscanning.service;

import adrianmikula.jakartamigration.advancedscanning.domain.FileScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.ProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.ServletJspUsage;

import java.nio.file.Path;

public interface ServletJspScanner {
    ProjectScanResult<FileScanResult<ServletJspUsage>> scanProject(Path projectPath);
    FileScanResult<ServletJspUsage> scanFile(Path filePath);
}
