/*
 * Copyright © 2026 Adrian Mikula
 *
 * All rights reserved.
 *
 * This software is proprietary and may not be used, copied,
 * modified, or distributed except under the terms of a
 * separate commercial license agreement.
 */
package adrianmikula.jakartamigration.intellij.mcp;

import adrianmikula.jakartamigration.intellij.model.DependencyInfo;

import java.util.EventListener;

/**
 * Listener interface for SSE updates.
 */
public interface SseUpdateListener extends EventListener {
    void onDependencyUpdate(DependencyInfo dependency);
    void onAnalysisProgress(int progress, String message);
    void onAnalysisComplete();
    void onError(String error);
}
