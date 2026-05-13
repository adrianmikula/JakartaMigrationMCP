package adrianmikula.jakartamigration.advancedscanning.service;

import adrianmikula.jakartamigration.advancedscanning.domain.SerializationCacheProjectScanResult;
import java.nio.file.Path;
import java.util.List;

/**
 * Scanner interface for detecting serialization and cache compatibility issues.
 * This scanner helps identify code that serializes or caches javax.* objects.
 */
public interface SerializationCacheScanner {
    
    /**
     * Scans a project for serialization and cache compatibility issues.
     * 
     * @param projectPath Path to the project root directory
     * @return SerializationCacheProjectScanResult containing all findings
     */
    SerializationCacheProjectScanResult scanProject(Path projectPath);
    
    /**
     * Scans a project using pre-discovered Java files.
     * @param filesToScan list of Java file paths to scan
     * @return Project scan result with all findings
     */
    SerializationCacheProjectScanResult scanProject(List<Path> filesToScan);
}
