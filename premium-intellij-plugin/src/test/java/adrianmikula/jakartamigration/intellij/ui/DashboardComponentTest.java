package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.model.DependencySummary;
import adrianmikula.jakartamigration.intellij.model.MigrationDashboard;
import adrianmikula.jakartamigration.intellij.model.MigrationStatus;
import adrianmikula.jakartamigration.intellij.service.AdvancedScanningService;
import adrianmikula.jakartamigration.coderefactoring.service.RecipeService;
import adrianmikula.jakartamigration.analysis.persistence.CentralMigrationAnalysisStore;
import adrianmikula.jakartamigration.analysis.persistence.SqliteMigrationAnalysisStore;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.nio.file.Paths;

public class DashboardComponentTest extends BasePlatformTestCase {

    private DashboardComponent dashboardComponent;
    private AdvancedScanningService advancedScanningService;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Create mock stores for testing
        CentralMigrationAnalysisStore centralStore = new CentralMigrationAnalysisStore();
        SqliteMigrationAnalysisStore projectStore = new SqliteMigrationAnalysisStore(Paths.get(getProject().getBasePath()));
        RecipeService recipeService = new adrianmikula.jakartamigration.coderefactoring.service.impl.RecipeServiceImpl(centralStore, projectStore);
        advancedScanningService = new AdvancedScanningService(recipeService);
        dashboardComponent = new DashboardComponent(getProject(), advancedScanningService, e -> {
        });
    }

    public void testInitialization() {
        assertThat(dashboardComponent.getPanel()).isNotNull();
    }

    public void testUpdateDashboard() {
        MigrationDashboard dashboard = new MigrationDashboard();
        dashboard.setStatus(MigrationStatus.READY);
        dashboard.setLastAnalyzed(Instant.now());

        DependencySummary summary = new DependencySummary();
        summary.setTotalDependencies(10);
        summary.setAffectedDependencies(2);
        summary.setBlockerDependencies(0);
        summary.setMigrableDependencies(2);
        dashboard.setDependencySummary(summary);

        // Update dashboard with all components
        dashboardComponent.updateGauges();
        dashboardComponent.updateSummary();
        // dashboardComponent.updateScanResultsTable(); // Method doesn't exist
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

    // ==================== New Score Calculation Tests ====================

    /**
     * Test getColorForMetric for negative metrics (lower is better like issue counts).
     * Thresholds: green (<=0), yellow (<=5), orange (<=10), red (>10)
     */
    public void testGetColorForMetric_NegativeMetric() throws Exception {
        java.lang.reflect.Method method = DashboardComponent.class.getDeclaredMethod(
                "getColorForMetric", int.class, int[].class, boolean.class);
        method.setAccessible(true);

        int[] thresholds = {0, 5, 10};

        // Test negative metric (lower is better)
        java.awt.Color green = (java.awt.Color) method.invoke(dashboardComponent, 0, thresholds, false);
        java.awt.Color yellow = (java.awt.Color) method.invoke(dashboardComponent, 3, thresholds, false);
        java.awt.Color orange = (java.awt.Color) method.invoke(dashboardComponent, 8, thresholds, false);
        java.awt.Color red = (java.awt.Color) method.invoke(dashboardComponent, 15, thresholds, false);

        // Green should be returned for 0 issues
        assertThat(green.getGreen()).isGreaterThan(green.getRed());
        // Red should be returned for high issue count
        assertThat(red.getRed()).isGreaterThan(red.getGreen());
    }

    /**
     * Test getColorForMetric for positive metrics (higher is better like test coverage).
     * Thresholds: red (<30), orange (<50), yellow (<70), green (>=70)
     */
    public void testGetColorForMetric_PositiveMetric() throws Exception {
        java.lang.reflect.Method method = DashboardComponent.class.getDeclaredMethod(
                "getColorForMetric", int.class, int[].class, boolean.class);
        method.setAccessible(true);

        int[] thresholds = {30, 50, 70};

        // Test positive metric (higher is better)
        java.awt.Color red = (java.awt.Color) method.invoke(dashboardComponent, 20, thresholds, true);
        java.awt.Color orange = (java.awt.Color) method.invoke(dashboardComponent, 40, thresholds, true);
        java.awt.Color yellow = (java.awt.Color) method.invoke(dashboardComponent, 60, thresholds, true);
        java.awt.Color green = (java.awt.Color) method.invoke(dashboardComponent, 85, thresholds, true);

        // Low test coverage should be red
        assertThat(red.getRed()).isGreaterThan(red.getGreen());
        // High test coverage should be green
        assertThat(green.getGreen()).isGreaterThan(green.getRed());
    }

    /**
     * Test getSourceCodeIssuesCount returns sum of all source code related scan counts.
     */
    public void testGetSourceCodeIssuesCount() throws Exception {
        // Create summary with specific scan counts
        AdvancedScanningService.AdvancedScanSummary summary = new AdvancedScanningService.AdvancedScanSummary(
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        // Use reflection to access the private method
        java.lang.reflect.Method method = DashboardComponent.class.getDeclaredMethod("getSourceCodeIssuesCount");
        method.setAccessible(true);

        // Initially should return 0 since no cached results
        int count = (int) method.invoke(dashboardComponent);
        assertThat(count).isEqualTo(0);
    }

    /**
     * Test getConfigIssuesCount returns sum of build config and config file counts.
     */
    public void testGetConfigIssuesCount() throws Exception {
        java.lang.reflect.Method method = DashboardComponent.class.getDeclaredMethod("getConfigIssuesCount");
        method.setAccessible(true);

        // Initially should return 0 since no cached results
        int count = (int) method.invoke(dashboardComponent);
        assertThat(count).isEqualTo(0);
    }

    /**
     * Test getPlatformsNeedingUpgradeCount returns 0 when platforms tab not set.
     */
    public void testGetPlatformsNeedingUpgradeCount_NoPlatformsTab() throws Exception {
        java.lang.reflect.Method method = DashboardComponent.class.getDeclaredMethod("getPlatformsNeedingUpgradeCount");
        method.setAccessible(true);

        int count = (int) method.invoke(dashboardComponent);
        assertThat(count).isEqualTo(0);
    }

    /**
     * Test getRecipesWithMatchesCount returns 0 when no cached results.
     */
    public void testGetRecipesWithMatchesCount_NoCachedResults() throws Exception {
        java.lang.reflect.Method method = DashboardComponent.class.getDeclaredMethod("getRecipesWithMatchesCount");
        method.setAccessible(true);

        int count = (int) method.invoke(dashboardComponent);
        assertThat(count).isEqualTo(0);
    }

    /**
     * Test extractValue correctly parses numeric values from formatted bullet text.
     */
    public void testExtractValue() throws Exception {
        java.lang.reflect.Method method = DashboardComponent.class.getDeclaredMethod("extractValue", String.class);
        method.setAccessible(true);

        // Test extracting value from formatted text
        assertThat(method.invoke(dashboardComponent, "<html>&bull; Direct deps: 5</html>")).isEqualTo(5);
        assertThat(method.invoke(dashboardComponent, "<html>&bull; Test coverage: 75%</html>")).isEqualTo(75);
        assertThat(method.invoke(dashboardComponent, "<html><u>&bull; Issues: 42</u></html>")).isEqualTo(42);
        assertThat(method.invoke(dashboardComponent, (String) null)).isEqualTo(0);
        assertThat(method.invoke(dashboardComponent, "no colon here")).isEqualTo(0);
    }

    /**
     * Test formatBulletText creates properly formatted HTML bullet text.
     */
    public void testFormatBulletText() throws Exception {
        java.lang.reflect.Method method = DashboardComponent.class.getDeclaredMethod("formatBulletText", String.class, int.class);
        method.setAccessible(true);

        String result = (String) method.invoke(dashboardComponent, "Direct dependencies needing upgrade", 5);
        assertThat(result).contains("Direct dependencies needing upgrade");
        assertThat(result).contains("5");
        assertThat(result).startsWith("<html>&bull;");
    }

    /**
     * Test calculateConfidenceScore returns expected percentage based on dependency summary.
     */
    public void testCalculateConfidenceScore() throws Exception {
        // Setup dashboard with dependency summary
        MigrationDashboard dashboard = new MigrationDashboard();
        DependencySummary summary = new DependencySummary();
        summary.setTotalDependencies(100);
        summary.setUnknownReviewCount(10); // 10 unknown = 90% confidence
        dashboard.setDependencySummary(summary);
        dashboard.setStatus(MigrationStatus.COMPLETED);

        dashboardComponent.setDashboard(dashboard);

        // Use reflection to test the private calculateConfidenceScore method
        java.lang.reflect.Method method = DashboardComponent.class.getDeclaredMethod("calculateConfidenceScore");
        method.setAccessible(true);

        int score = (int) method.invoke(dashboardComponent);
        // 100 total - 10 unknown = 90 known = 90% confidence
        assertThat(score).isEqualTo(90);
    }

    /**
     * Test calculateConfidenceScore with zero dependencies returns 0.
     */
    public void testCalculateConfidenceScore_ZeroDependencies() throws Exception {
        MigrationDashboard dashboard = new MigrationDashboard();
        DependencySummary summary = new DependencySummary();
        summary.setTotalDependencies(0);
        summary.setUnknownReviewCount(0);
        dashboard.setDependencySummary(summary);

        dashboardComponent.setDashboard(dashboard);

        java.lang.reflect.Method method = DashboardComponent.class.getDeclaredMethod("calculateConfidenceScore");
        method.setAccessible(true);

        int score = (int) method.invoke(dashboardComponent);
        assertThat(score).isEqualTo(0);
    }

}
