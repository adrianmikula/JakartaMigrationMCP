package adrianmikula.jakartamigration.advancedscanning.service;

import adrianmikula.jakartamigration.advancedscanning.domain.JmsMessagingProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.JmsMessagingScanResult;
import java.nio.file.Path;

public interface JmsMessagingScanner {
    JmsMessagingProjectScanResult scanProject(Path projectPath);
    JmsMessagingScanResult scanFile(Path filePath);
}
