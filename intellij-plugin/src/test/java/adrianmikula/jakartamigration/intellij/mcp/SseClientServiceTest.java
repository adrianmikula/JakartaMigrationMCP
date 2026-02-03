package adrianmikula.jakartamigration.intellij.mcp;

import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus;
import adrianmikula.jakartamigration.intellij.model.RiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for DefaultSseClientService
 */
class SseClientServiceTest {

    private DefaultSseClientService sseClient;

    @BeforeEach
    public void setUp() {
        sseClient = new DefaultSseClientService();
    }

    @Test
    @DisplayName("SseClientService should not be connected initially")
    public void testNotConnectedInitially() {
        assertThat(sseClient.isConnected()).isFalse();
    }

    @Test
    @DisplayName("SseClientService should add and remove listeners")
    public void testListenerManagement() {
        SseUpdateListener listener = new SseUpdateListener() {
            @Override
            public void onDependencyUpdate(DependencyInfo dependency) {}

            @Override
            public void onAnalysisProgress(int progress, String message) {}

            @Override
            public void onAnalysisComplete() {}

            @Override
            public void onError(String error) {}
        };

        sseClient.addListener(listener);
        sseClient.removeListener(listener);
    }

    @Test
    @DisplayName("SseUpdateListener should have all required methods")
    public void testListenerInterface() {
        SseUpdateListener listener = new SseUpdateListener() {
            @Override
            public void onDependencyUpdate(DependencyInfo dependency) {}

            @Override
            public void onAnalysisProgress(int progress, String message) {}

            @Override
            public void onAnalysisComplete() {}

            @Override
            public void onError(String error) {}
        };

        assertThat(listener).isNotNull();
    }

    @Test
    @DisplayName("SseClientService should disconnect properly")
    public void testDisconnect() {
        sseClient.disconnect();
        assertThat(sseClient.isConnected()).isFalse();
    }
}
