package adrianmikula.jakartamigration.advancedscanning.service;

import adrianmikula.jakartamigration.advancedscanning.domain.FileScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.JavaxUsage;
import adrianmikula.jakartamigration.advancedscanning.domain.ProjectScanResult;

import java.nio.file.Path;

public interface RestSoapScanner {
    ProjectScanResult<FileScanResult<JavaxUsage>> scanProject(Path projectPath);
    FileScanResult<JavaxUsage> scanFile(Path filePath);
}
