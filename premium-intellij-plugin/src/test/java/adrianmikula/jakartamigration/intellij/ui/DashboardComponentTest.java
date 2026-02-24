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

    public void testClearMetrics() {
        dashboardComponent.clearMetrics();
        assertThat(dashboardComponent.getReadinessScoreValue().getText()).isEqualTo("-");
        assertThat(dashboardComponent.getStatusValue().getText()).isEqualTo("NOT_ANALYZED");
    }
}
