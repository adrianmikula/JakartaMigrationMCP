package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.model.DependencySummary;
import adrianmikula.jakartamigration.intellij.model.MigrationDashboard;
import adrianmikula.jakartamigration.intellij.model.MigrationStatus;
import adrianmikula.jakartamigration.intellij.service.AdvancedScanningService;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

public class DashboardComponentTest extends BasePlatformTestCase {

    private DashboardComponent dashboardComponent;
    private AdvancedScanningService advancedScanningService;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        advancedScanningService = new AdvancedScanningService();
        dashboardComponent = new DashboardComponent(getProject(), advancedScanningService, e -> {
        });
    }

    public void testInitialization() {
        assertThat(dashboardComponent.getPanel()).isNotNull();
        assertThat(dashboardComponent.getStatusValue().getText()).isEqualTo("NOT_ANALYZED");
        assertThat(dashboardComponent.getReadinessScoreValue().getText()).isEqualTo("-");
    }

    public void testUpdateDashboard() {
        MigrationDashboard dashboard = new MigrationDashboard();
        dashboard.setReadinessScore(85);
        dashboard.setStatus(MigrationStatus.READY);
        dashboard.setLastAnalyzed(Instant.now());

        DependencySummary summary = new DependencySummary();
        summary.setTotalDependencies(10);
        summary.setAffectedDependencies(2);
        summary.setBlockerDependencies(0);
        summary.setMigrableDependencies(2);
        dashboard.setDependencySummary(summary);

        dashboardComponent.updateDashboard(dashboard);

        assertThat(dashboardComponent.getReadinessScoreValue().getText()).isEqualTo("85%");
        assertThat(dashboardComponent.getStatusValue().getText()).isEqualTo("READY");
    }

    public void testUpdateAdvancedScanCounts() {
        AdvancedScanningService.AdvancedScanSummary summary = new AdvancedScanningService.AdvancedScanSummary(
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        // We can't easily create these objects because they are complex classes in the
        // engine,
        // but we can use reflection or just check that it resets to zero first.
        // Actually, since it's a record with getters, let's see if we can create a mock
        // or just a dummy.

        // For now, let's just test that it calls the service.
        advancedScanningService.setCachedSummary(summary);
        dashboardComponent.updateAdvancedScanCounts();

        // Since summary has all nulls, counts should be 0
        assertThat(dashboardComponent.getJpaScanCountValue().getText()).isEqualTo("0");
    }

    public void testClearMetrics() {
        dashboardComponent.clearMetrics();
        assertThat(dashboardComponent.getReadinessScoreValue().getText()).isEqualTo("-");
        assertThat(dashboardComponent.getStatusValue().getText()).isEqualTo("NOT_ANALYZED");
    }
}
