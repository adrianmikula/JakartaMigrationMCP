package adrianmikula.jakartamigration.advancedscanning.service;

import adrianmikula.jakartamigration.advancedscanning.domain.IntegrationPointsProjectScanResult;
import java.nio.file.Path;

/**
 * Scanner for detecting integration points (RMI, CORBA, etc.) with javax.* dependencies.
 */
public interface IntegrationPointsScanner {
    IntegrationPointsProjectScanResult scanProject(Path projectPath);
}
