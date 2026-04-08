package adrianmikula.jakartamigration.advancedscanning.service;

import adrianmikula.jakartamigration.advancedscanning.domain.BuildConfigUsage;
import adrianmikula.jakartamigration.advancedscanning.domain.FileScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.ProjectScanResult;

import java.nio.file.Path;

/**
 * Service for scanning build configuration files for javax.* dependencies.
 * Supports Maven pom.xml and Gradle build.gradle files.
 */
public interface BuildConfigScanner {

    ProjectScanResult<FileScanResult<BuildConfigUsage>> scanProject(Path projectPath);

    FileScanResult<BuildConfigUsage> scanFile(Path filePath);
}
