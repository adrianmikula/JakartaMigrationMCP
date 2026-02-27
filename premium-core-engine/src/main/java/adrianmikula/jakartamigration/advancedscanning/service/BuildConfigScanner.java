package adrianmikula.jakartamigration.advancedscanning.service;

import adrianmikula.jakartamigration.advancedscanning.domain.BuildConfigProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.BuildConfigScanResult;

import java.nio.file.Path;

/**
 * Service for scanning build configuration files for javax.* dependencies.
 * Supports Maven pom.xml and Gradle build.gradle files.
 */
public interface BuildConfigScanner {

    BuildConfigProjectScanResult scanProject(Path projectPath);

    BuildConfigScanResult scanFile(Path filePath);
}
