package adrianmikula.jakartamigration.intellij.mcp;

import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for DefaultMcpClientService
 * Tests HTTP client functionality and mock data generation
 */
public class DefaultMcpClientServiceTest {

    private DefaultMcpClientService mcpClient;

    @BeforeEach
    public void setUp() {
        // Use a non-existent server to trigger mock data fallback
        mcpClient = new DefaultMcpClientService("http://localhost:9999", 1, 5);
    }

    @Test
    @DisplayName("DefaultMcpClientService should return server URL")
    public void testGetServerUrl() {
        assertThat(mcpClient.getServerUrl()).isEqualTo("http://localhost:9999");
    }

    @Test
    @DisplayName("isServerAvailable should return false for unreachable server")
    public void testServerUnavailable() throws Exception {
        CompletableFuture<Boolean> future = mcpClient.isServerAvailable();
        Boolean available = future.get(10, TimeUnit.SECONDS);
        assertThat(available).isFalse();
    }

    @Test
    @DisplayName("analyzeMigrationImpact should return response with mock data")
    public void testAnalyzeMigrationImpactReturnsMockData() throws Exception {
        CompletableFuture<AnalyzeMigrationImpactResponse> future =
            mcpClient.analyzeMigrationImpact("/test/path");

        AnalyzeMigrationImpactResponse response = future.get(10, TimeUnit.SECONDS);
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("detectBlockers should return mock blockers")
    public void testDetectBlockersReturnsMockData() throws Exception {
        CompletableFuture<List<DependencyInfo>> future =
            mcpClient.detectBlockers("/test/path");

        List<DependencyInfo> blockers = future.get(10, TimeUnit.SECONDS);
        assertThat(blockers).isNotNull();
        assertThat(blockers).isNotEmpty();

        // Verify mock blockers have NO_JAKARTA_VERSION status
        assertThat(blockers.stream().allMatch(b ->
            b.getMigrationStatus() == DependencyMigrationStatus.NO_JAKARTA_VERSION)).isTrue();
    }

    @Test
    @DisplayName("recommendVersions should return mock recommendations")
    public void testRecommendVersionsReturnsMockData() throws Exception {
        CompletableFuture<List<DependencyInfo>> future =
            mcpClient.recommendVersions("/test/path");

        List<DependencyInfo> recommendations = future.get(10, TimeUnit.SECONDS);
        assertThat(recommendations).isNotNull();
        assertThat(recommendations).isNotEmpty();
    }

    @Test
    @DisplayName("detectBlockers mock data should have correct structure")
    public void testMockBlockersStructure() throws Exception {
        CompletableFuture<List<DependencyInfo>> future =
            mcpClient.detectBlockers("/test/path");

        List<DependencyInfo> blockers = future.get(10, TimeUnit.SECONDS);

        for (DependencyInfo blocker : blockers) {
            assertThat(blocker.getGroupId()).isNotNull();
            assertThat(blocker.getArtifactId()).isNotNull();
            assertThat(blocker.getCurrentVersion()).isNotNull();
            assertThat(blocker.getMigrationStatus()).isNotNull();
        }
    }

    @Test
    @DisplayName("recommendVersions mock data should have recommended versions")
    public void testMockRecommendationsHaveVersions() throws Exception {
        CompletableFuture<List<DependencyInfo>> future =
            mcpClient.recommendVersions("/test/path");

        List<DependencyInfo> recommendations = future.get(10, TimeUnit.SECONDS);

        for (DependencyInfo rec : recommendations) {
            assertThat(rec.getGroupId()).isNotNull();
            assertThat(rec.getArtifactId()).isNotNull();
            assertThat(rec.getCurrentVersion()).isNotNull();
            assertThat(rec.getRecommendedVersion()).isNotNull();
            assertThat(rec.getMigrationStatus()).isEqualTo(DependencyMigrationStatus.NEEDS_UPGRADE);
        }
    }

    @Test
    @DisplayName("Mock recommendations should have at least one with recommended version")
    public void testMockRecommendationsHaveAtLeastOneWithVersion() throws Exception {
        CompletableFuture<List<DependencyInfo>> future =
            mcpClient.recommendVersions("/test/path");

        List<DependencyInfo> recommendations = future.get(10, TimeUnit.SECONDS);

        // At least one recommendation should have a non-null recommended version
        boolean hasRecommendedVersion = recommendations.stream()
            .anyMatch(r -> r.getRecommendedVersion() != null && !r.getRecommendedVersion().isEmpty());
        assertThat(hasRecommendedVersion).isTrue();
    }
}
