package adrianmikula.jakartamigration.advancedscanning.service;

import adrianmikula.jakartamigration.advancedscanning.domain.TestContainersProjectScanResult;
import java.nio.file.Path;

/**
 * Scanner for detecting test containers and embedded servers.
 */
public interface TestContainersScanner {
    TestContainersProjectScanResult scanProject(Path projectPath);
}
