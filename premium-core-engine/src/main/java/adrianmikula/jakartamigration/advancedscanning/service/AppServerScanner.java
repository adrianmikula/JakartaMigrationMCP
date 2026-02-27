package adrianmikula.jakartamigration.advancedscanning.service;

import adrianmikula.jakartamigration.advancedscanning.domain.AppServerProjectScanResult;
import java.nio.file.Path;

/**
 * Scanner for detecting application server configurations that need migration.
 */
public interface AppServerScanner {
    AppServerProjectScanResult scanProject(Path projectPath);
}
