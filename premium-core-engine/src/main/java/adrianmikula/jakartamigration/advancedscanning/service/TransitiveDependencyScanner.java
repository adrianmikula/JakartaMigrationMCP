package adrianmikula.jakartamigration.advancedscanning.service;

import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyScanResult;
import java.nio.file.Path;

public interface TransitiveDependencyScanner {
    TransitiveDependencyProjectScanResult scanProject(Path projectPath);
    TransitiveDependencyScanResult scanFile(Path filePath);
}
