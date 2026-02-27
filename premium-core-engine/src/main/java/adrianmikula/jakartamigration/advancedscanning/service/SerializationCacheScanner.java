package adrianmikula.jakartamigration.advancedscanning.service;

import adrianmikula.jakartamigration.advancedscanning.domain.SerializationCacheProjectScanResult;

import java.nio.file.Path;

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
}
