package adrianmikula.jakartamigration.advancedscanning.service;

import adrianmikula.jakartamigration.advancedscanning.domain.ReflectionUsageProjectScanResult;
import java.nio.file.Path;
import java.util.List;

/**
 * Scanner interface for detecting reflection usage of javax packages.
 * This scanner helps identify code that uses reflection to access javax.* classes,
 * which may cause compatibility issues during Jakarta EE migration.
 */
public interface ReflectionUsageScanner {
    
    /**
     * Scans a project for reflection usage of javax packages.
     * 
     * @param projectPath Path to project root directory
     * @return ReflectionUsageProjectScanResult containing all findings
     */
    ReflectionUsageProjectScanResult scanProject(Path projectPath);
    
    /**
     * Scans a project using pre-discovered Java/Kotlin/Scala files.
     * @param filesToScan list of source file paths (.java, .kt, .scala) to scan
     * @return Project scan result with all findings
     */
    ReflectionUsageProjectScanResult scanProject(List<Path> filesToScan);
}
