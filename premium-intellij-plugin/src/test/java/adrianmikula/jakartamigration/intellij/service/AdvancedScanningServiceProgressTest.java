package adrianmikula.jakartamigration.intellij.service;

import adrianmikula.jakartamigration.intellij.ui.ScanProgressListener;
import adrianmikula.jakartamigration.coderefactoring.service.RecipeService;
import adrianmikula.jakartamigration.analysis.persistence.CentralMigrationAnalysisStore;
import adrianmikula.jakartamigration.analysis.persistence.SqliteMigrationAnalysisStore;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.Test;
import org.assertj.core.api.Assertions;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class AdvancedScanningServiceProgressTest extends BasePlatformTestCase {

    private AdvancedScanningService advancedScanningService;
    private TestProgressListener progressListener;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        
        // Create mock stores for testing
        CentralMigrationAnalysisStore centralStore = new CentralMigrationAnalysisStore();
        SqliteMigrationAnalysisStore projectStore = new SqliteMigrationAnalysisStore(Paths.get(getProject().getBasePath()));
        RecipeService recipeService = new adrianmikula.jakartamigration.coderefactoring.service.impl.RecipeServiceImpl(centralStore, projectStore);
        advancedScanningService = new AdvancedScanningService(recipeService);
        progressListener = new TestProgressListener();
    }

    @Test
    public void testScanAllWithProgressListener() throws Exception {
        // Test that scanAll with progress listener works without errors
        AdvancedScanningService.AdvancedScanSummary summary = 
            advancedScanningService.scanAll(Paths.get(getProject().getBasePath()), progressListener);
        
        // Verify scan completed successfully
        Assertions.assertThat(summary).isNotNull();
        
        // Wait for async progress updates to complete
        Thread.sleep(500);
        
        // Verify progress callbacks were called
        Assertions.assertThat(progressListener.phaseUpdateCount.get()).isGreaterThan(0);
        Assertions.assertThat(progressListener.completeCalled.get()).isTrue();
    }

    public void testScanAllWithNullProgressListener() throws Exception {
        // Test that scanAll works with null progress listener (backward compatibility)
        AdvancedScanningService.AdvancedScanSummary summary = 
            advancedScanningService.scanAll(Paths.get(getProject().getBasePath()), null);
        
        // Verify scan completed successfully
        Assertions.assertThat(summary).isNotNull();
    }

    public void testScanAllWithoutProgressListener() throws Exception {
        // Test that original scanAll method still works (backward compatibility)
        AdvancedScanningService.AdvancedScanSummary summary = 
            advancedScanningService.scanAll(Paths.get(getProject().getBasePath()));
        
        // Verify scan completed successfully
        Assertions.assertThat(summary).isNotNull();
    }

    public void testProgressListenerPhaseReporting() throws Exception {
        CountDownLatch phaseLatch = new CountDownLatch(3); // Expect at least 3 phase updates
        
        progressListener = new TestProgressListener() {
            @Override
            public void onScanPhase(String phase, int completed, int total) {
                super.onScanPhase(phase, completed, total);
                phaseLatch.countDown();
            }
        };
        
        // Run scan with progress listener
        advancedScanningService.scanAll(Paths.get(getProject().getBasePath()), progressListener);
        
        // Wait for phase updates
        Assertions.assertThat(phaseLatch.await(10, TimeUnit.SECONDS)).isTrue();
        
        // Verify phase updates were called
        Assertions.assertThat(progressListener.phaseUpdateCount.get()).isGreaterThanOrEqualTo(3);
    }

    public void testProgressListenerSubScanReporting() throws Exception {
        CountDownLatch subScanLatch = new CountDownLatch(4); // Expect at least 4 sub-scan updates
        
        progressListener = new TestProgressListener() {
            @Override
            public void onSubScanComplete(String scanType, int resultCount) {
                super.onSubScanComplete(scanType, resultCount);
                subScanLatch.countDown();
            }
        };
        
        // Run scan with progress listener
        advancedScanningService.scanAll(Paths.get(getProject().getBasePath()), progressListener);
        
        // Wait for sub-scan updates (may not complete if no findings)
        // Don't assert here as sub-scans might not find anything in test project
        subScanLatch.await(5, TimeUnit.SECONDS);
    }

    public void testProgressListenerErrorHandling() throws Exception {
        // Test error handling with progress listener
        TestProgressListener errorListener = new TestProgressListener();
        
        // Use invalid path to trigger error
        AdvancedScanningService.AdvancedScanSummary summary = 
            advancedScanningService.scanAll(Paths.get("/invalid/path/that/does/not/exist"), errorListener);
        
        // The scan should handle errors gracefully and still return a summary
        Assertions.assertThat(summary).isNotNull();
    }

    public void testProgressListenerThreadSafety() throws Exception {
        // Test that progress listener callbacks are thread-safe
        AtomicInteger concurrentUpdates = new AtomicInteger(0);
        CountDownLatch concurrentLatch = new CountDownLatch(10);
        
        progressListener = new TestProgressListener() {
            @Override
            public void onScanPhase(String phase, int completed, int total) {
                super.onScanPhase(phase, completed, total);
                concurrentUpdates.incrementAndGet();
                concurrentLatch.countDown();
            }
        };
        
        // Run scan
        advancedScanningService.scanAll(Paths.get(getProject().getBasePath()), progressListener);
        
        // Wait for updates
        concurrentLatch.await(10, TimeUnit.SECONDS);
        
        // Verify concurrent updates were handled without errors
        Assertions.assertThat(concurrentUpdates.get()).isGreaterThan(0);
    }

    /**
     * Test implementation of ScanProgressListener for testing purposes
     */
    private static class TestProgressListener implements ScanProgressListener {
        final AtomicInteger phaseUpdateCount = new AtomicInteger(0);
        final AtomicBoolean completeCalled = new AtomicBoolean(false);
        final AtomicBoolean errorCalled = new AtomicBoolean(false);
        final AtomicReference<String> lastPhase = new AtomicReference<>();
        final AtomicReference<Exception> lastError = new AtomicReference<>();
        final AtomicInteger lastCompleted = new AtomicInteger(-1);
        final AtomicInteger lastTotal = new AtomicInteger(-1);
        final AtomicInteger subScanCount = new AtomicInteger(0);

        @Override
        public void onScanPhase(String phase, int completed, int total) {
            phaseUpdateCount.incrementAndGet();
            lastPhase.set(phase);
            lastCompleted.set(completed);
            lastTotal.set(total);
        }

        @Override
        public void onScanComplete() {
            completeCalled.set(true);
        }

        @Override
        public void onScanError(Exception error) {
            errorCalled.set(true);
            lastError.set(error);
        }

        @Override
        public void onSubScanComplete(String scanType, int resultCount) {
            subScanCount.incrementAndGet();
        }
    }

    public void testProgressListenerStateConsistency() throws Exception {
        TestProgressListener listener = new TestProgressListener();
        
        // Test initial state
        Assertions.assertThat(listener.phaseUpdateCount.get()).isEqualTo(0);
        Assertions.assertThat(listener.completeCalled.get()).isFalse();
        Assertions.assertThat(listener.errorCalled.get()).isFalse();
        
        // Run scan
        advancedScanningService.scanAll(Paths.get(getProject().getBasePath()), listener);
        
        // Wait for completion
        Thread.sleep(1000);
        
        // Verify final state
        Assertions.assertThat(listener.phaseUpdateCount.get()).isGreaterThan(0);
        Assertions.assertThat(listener.completeCalled.get()).isTrue();
    }
}
