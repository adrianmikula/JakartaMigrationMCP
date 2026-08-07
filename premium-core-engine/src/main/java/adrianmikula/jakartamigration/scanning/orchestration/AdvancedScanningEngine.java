package adrianmikula.jakartamigration.scanning.orchestration;

import adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults;
import adrianmikula.jakartamigration.advancedscanning.service.ScanProgressCallback;

import java.nio.file.Path;

/**
 * Engine abstraction for running the advanced scan phase.
 * Implementations live in the caller module (IntelliJ plugin, MCP server, etc.).
 */
public interface AdvancedScanningEngine {
    ComprehensiveScanResults runAdvancedScans(Path projectPath, ScanMode mode, ScanProgressCallback progressCallback);
}
