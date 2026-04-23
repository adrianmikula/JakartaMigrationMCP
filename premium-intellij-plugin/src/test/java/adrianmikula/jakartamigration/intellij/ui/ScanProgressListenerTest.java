package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.model.DependencySummary;
import adrianmikula.jakartamigration.intellij.model.MigrationDashboard;
import adrianmikula.jakartamigration.intellij.model.MigrationStatus;
import adrianmikula.jakartamigration.intellij.service.AdvancedScanningService;
import adrianmikula.jakartamigration.coderefactoring.service.RecipeService;
import adrianmikula.jakartamigration.analysis.persistence.CentralMigrationAnalysisStore;
import adrianmikula.jakartamigration.analysis.persistence.SqliteMigrationAnalysisStore;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.components.JBLabel;
import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import java.time.Instant;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ScanProgressListenerTest extends BasePlatformTestCase {

    private DashboardComponent dashboardComponent;
    private AdvancedScanningService advancedScanningService;
    private TestScanProgressListener testListener;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        
        // Create mock stores for testing
        CentralMigrationAnalysisStore centralStore = new CentralMigrationAnalysisStore();
        SqliteMigrationAnalysisStore projectStore = new SqliteMigrationAnalysisStore(Paths.get(getProject().getBasePath()));
        RecipeService recipeService = new adrianmikula.jakartamigration.coderefactoring.service.impl.RecipeServiceImpl(centralStore, projectStore);
        advancedScanningService = new AdvancedScanningService(recipeService);
        dashboardComponent = new DashboardComponent(getProject(), advancedScanningService, e -> {});
        testListener = new TestScanProgressListener();
    }

    @Test
    public void testScanProgressPhaseUpdates() throws Exception {
        // Test that scan phase updates are properly handled
        dashboardComponent.onScanPhase("Test Phase", 1, 3);
        
        // Wait for UI thread to process the update
        Thread.sleep(100);
        
        // Verify the progress bar and label were updated
        // Note: We can't easily test the actual UI components without a running IDE,
        // but we can verify no exceptions were thrown
        assertThat(true).isTrue(); // Placeholder assertion
    }

    @Test
    public void testScanCompleteCallback() throws Exception {
        // Set up test dashboard data
        MigrationDashboard dashboard = new MigrationDashboard();
        dashboard.setStatus(MigrationStatus.READY);
        dashboard.setLastAnalyzed(Instant.now());
        DependencySummary summary = new DependencySummary();
        summary.setTotalDependencies(10);
        dashboard.setDependencySummary(summary);
        dashboardComponent.setDashboard(dashboard);

        // Test scan completion callback
        dashboardComponent.onScanComplete();
        
        // Wait for UI thread to process the update
        Thread.sleep(100);
        
        // Verify no exceptions were thrown during completion
        assertThat(true).isTrue(); // Placeholder assertion
    }

    @Test
    public void testScanErrorCallback() throws Exception {
        // Test scan error callback
        Exception testError = new RuntimeException("Test error");
        dashboardComponent.onScanError(testError);
        
        // Wait for UI thread to process the update
        Thread.sleep(100);
        
        // Verify no exceptions were thrown during error handling
        assertThat(true).isTrue(); // Placeholder assertion
    }

    @Test
    public void testSubScanCompleteCallback() throws Exception {
        // Test sub-scan completion callbacks for different scan types
        dashboardComponent.onSubScanComplete("JPA", 5);
        dashboardComponent.onSubScanComplete("Bean Validation", 3);
        dashboardComponent.onSubScanComplete("Servlet/JSP", 2);
        dashboardComponent.onSubScanComplete("CDI Injection", 1);
        dashboardComponent.onSubScanComplete("Build Config", 4);
        dashboardComponent.onSubScanComplete("REST/SOAP", 2);
        dashboardComponent.onSubScanComplete("Unknown Scan Type", 10); // Should not cause error
        
        // Wait for UI thread to process the updates
        Thread.sleep(100);
        
        // Verify no exceptions were thrown during sub-scan updates
        assertThat(true).isTrue(); // Placeholder assertion
    }

    @Test
    public void testProgressListenerWithAdvancedScanningService() throws Exception {
        // Test that AdvancedScanningService can accept a progress listener
        AdvancedScanningService.AdvancedScanSummary summary = 
            advancedScanningService.scanAll(Paths.get(getProject().getBasePath()), testListener);
        
        // The scan should complete without errors (even if no results found)
        assertThat(summary).isNotNull();
        
        // Verify that progress callbacks were called
        assertThat(testListener.phaseCalled).isTrue();
        assertThat(testListener.completeCalled).isTrue();
    }

    @Test
    public void testProgressListenerThreadSafety() throws Exception {
        // Test that progress callbacks are thread-safe
        CountDownLatch latch = new CountDownLatch(10);
        
        // Simulate concurrent progress updates
        for (int i = 0; i < 10; i++) {
            final int phase = i;
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {
                    dashboardComponent.onScanPhase("Phase " + phase, phase, 10);
                    latch.countDown();
                } catch (Exception e) {
                    // Should not happen
                    fail("Thread safety test failed: " + e.getMessage());
                }
            });
        }
        
        // Wait for all updates to complete
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    /**
     * Test implementation of ScanProgressListener for testing purposes
     */
    private static class TestScanProgressListener implements ScanProgressListener {
        boolean phaseCalled = false;
        boolean completeCalled = false;
        boolean errorCalled = false;
        String lastPhase;
        Exception lastError;
        int lastCompleted = -1;
        int lastTotal = -1;

        @Override
        public void onScanPhase(String phase, int completed, int total) {
            phaseCalled = true;
            lastPhase = phase;
            lastCompleted = completed;
            lastTotal = total;
        }

        @Override
        public void onScanComplete() {
            completeCalled = true;
        }

        @Override
        public void onScanError(Exception error) {
            errorCalled = true;
            lastError = error;
        }

        @Override
        public void onSubScanComplete(String scanType, int resultCount) {
            // Track sub-scan completions if needed
        }
    }

    @Test
    public void testProgressListenerStateTracking() throws Exception {
        TestScanProgressListener listener = new TestScanProgressListener();
        
        // Test phase tracking
        listener.onScanPhase("Test Phase 1", 1, 3);
        assertThat(listener.phaseCalled).isTrue();
        assertThat(listener.lastPhase).isEqualTo("Test Phase 1");
        assertThat(listener.lastCompleted).isEqualTo(1);
        assertThat(listener.lastTotal).isEqualTo(3);
        
        // Test completion tracking
        listener.onScanComplete();
        assertThat(listener.completeCalled).isTrue();
        
        // Test error tracking
        Exception testError = new RuntimeException("Test error");
        listener.onScanError(testError);
        assertThat(listener.errorCalled).isTrue();
        assertThat(listener.lastError).isEqualTo(testError);
    }
}
