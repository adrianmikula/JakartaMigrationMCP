package adrianmikula.jakartamigration.advancedscanning.service;

import adrianmikula.jakartamigration.advancedscanning.domain.UnitTestProjectScanResult;
import java.nio.file.Path;

/**
 * Scanner for detecting javax.* usage in test files.
 */
public interface UnitTestScanner {
    UnitTestProjectScanResult scanProject(Path projectPath);
}
