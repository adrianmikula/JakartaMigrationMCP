package adrianmikula.jakartamigration.advancedscanning.service;

import adrianmikula.jakartamigration.advancedscanning.domain.TestContainersProjectScanResult;
import java.nio.file.Path;
import java.util.List;

/**
 * Scanner for detecting test containers and embedded servers.
 */
public interface TestContainersScanner {
    TestContainersProjectScanResult scanProject(Path projectPath);
    
    /**
     * Scans a project using pre-discovered build files.
     * @param filesToScan list of build file paths (pom.xml, build.gradle) to scan
     * @return Project scan result with all findings
     */
    TestContainersProjectScanResult scanProject(List<Path> filesToScan);
}
