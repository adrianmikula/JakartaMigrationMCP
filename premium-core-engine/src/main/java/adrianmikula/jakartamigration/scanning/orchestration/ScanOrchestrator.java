package adrianmikula.jakartamigration.scanning.orchestration;

import adrianmikula.jakartamigration.advancedscanning.service.ScanProgressCallback;

import java.util.concurrent.CompletableFuture;

/**
 * Central contract for orchestrating Jakarta migration scans.
 */
public interface ScanOrchestrator {
    CompletableFuture<ScanResult> orchestrate(ScanRequest request, ScanProgressCallback progressCallback);
}
