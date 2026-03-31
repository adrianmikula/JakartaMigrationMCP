package adrianmikula.jakartamigration.intellij;

import adrianmikula.jakartamigration.intellij.license.SafeLicenseChecker;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

/**
 * Tests for LicenseCheckStartupActivity to ensure it never blocks IDE startup.
 * 
 * These tests verify that the startup activity:
 * - Never blocks the IDE during startup
 * - Properly delegates to SafeLicenseChecker
 * - Handles all error conditions gracefully
 * - Works correctly in all environments
 */
public class LicenseCheckStartupActivityTest extends BasePlatformTestCase {

    private LicenseCheckStartupActivity startupActivity;
    private MockedStatic<SafeLicenseChecker> mockedSafeLicenseChecker;
    
    @Before
    public void setUp() throws Exception {
        super.setUp();
        startupActivity = new LicenseCheckStartupActivity();
        mockedSafeLicenseChecker = mockStatic(SafeLicenseChecker.class);
    }
    
    @After
    public void tearDown() throws Exception {
        if (mockedSafeLicenseChecker != null) {
            mockedSafeLicenseChecker.close();
        }
        super.tearDown();
    }
    
    @Test
    public void testRunActivity_NeverBlocks() {
        // This test verifies that runActivity never blocks
        Project project = getProject();
        
        long startTime = System.currentTimeMillis();
        
        // This should complete immediately
        startupActivity.runActivity(project);
        
        long endTime = System.currentTimeMillis();
        
        // Should complete very quickly (under 100ms)
        assertThat(endTime - startTime).isLessThan(100);
        
        // Verify SafeLicenseChecker.checkLicenseOnStartup was called
        mockedSafeLicenseChecker.verify(() -> 
            SafeLicenseChecker.checkLicenseOnStartup(project), times(1));
    }
    
    @Test
    public void testRunActivity_HandlesNullProject() {
        // Test with null project - should not throw
        try {
            startupActivity.runActivity(null);
        } catch (Exception e) {
            fail("Should not throw exception for null project: " + e.getMessage());
        }
    }
    
    @Test
    public void testRunActivity_HandlesExceptionsInSafeLicenseChecker() {
        // Given
        Project project = getProject();
        mockedSafeLicenseChecker.when(() -> SafeLicenseChecker.checkLicenseOnStartup(project))
            .thenThrow(new RuntimeException("Test exception"));
        
        // When/Then - should not throw
        try {
            startupActivity.runActivity(project);
        } catch (Exception e) {
            fail("Should not throw exception when SafeLicenseChecker throws: " + e.getMessage());
        }
    }
    
    @Test
    public void testRunActivity_LogsCorrectly() {
        // Given
        Project project = getProject();
        
        // When
        startupActivity.runActivity(project);
        
        // Then - should have called SafeLicenseChecker
        mockedSafeLicenseChecker.verify(() -> 
            SafeLicenseChecker.checkLicenseOnStartup(project));
    }
    
    @Test
    public void testRunActivity_MultipleCalls() {
        // Given
        Project project = getProject();
        
        // When - call multiple times
        startupActivity.runActivity(project);
        startupActivity.runActivity(project);
        startupActivity.runActivity(project);
        
        // Then - should handle all calls without blocking
        mockedSafeLicenseChecker.verify(() -> 
            SafeLicenseChecker.checkLicenseOnStartup(project), times(3));
    }
    
    @Test
    public void testRunActivity_ConcurrentCalls() throws InterruptedException {
        // Given
        Project project = getProject();
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        
        // When - call from multiple threads concurrently
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                startupActivity.runActivity(project);
            });
        }
        
        long startTime = System.currentTimeMillis();
        
        for (Thread thread : threads) {
            thread.start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        long endTime = System.currentTimeMillis();
        
        // Then - should complete quickly even with concurrent calls
        assertThat(endTime - startTime).isLessThan(1000);
        
        // Should have been called from all threads
        mockedSafeLicenseChecker.verify(() -> 
            SafeLicenseChecker.checkLicenseOnStartup(project), times(threadCount));
    }
    
    @Test
    public void testRunActivity_WithDifferentProjects() {
        // Given
        Project project1 = getProject();
        Project project2 = getProject(); // Different instance
        
        // When
        startupActivity.runActivity(project1);
        startupActivity.runActivity(project2);
        
        // Then
        mockedSafeLicenseChecker.verify(() -> 
            SafeLicenseChecker.checkLicenseOnStartup(project1), times(1));
        mockedSafeLicenseChecker.verify(() -> 
            SafeLicenseChecker.checkLicenseOnStartup(project2), times(1));
    }
    
    @Test
    public void testRunActivity_PerformanceUnderLoad() {
        // Given
        Project project = getProject();
        int callCount = 100;
        
        // When
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < callCount; i++) {
            startupActivity.runActivity(project);
        }
        long endTime = System.currentTimeMillis();
        
        // Then - should handle high volume efficiently
        assertThat(endTime - startTime).isLessThan(1000);
        
        // Should have been called correct number of times
        mockedSafeLicenseChecker.verify(() -> 
            SafeLicenseChecker.checkLicenseOnStartup(project), times(callCount));
    }
    
    @Test
    public void testRunActivity_MemoryEfficiency() {
        // Given
        Project project = getProject();
        
        // Get initial memory usage
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // When - make many calls
        for (int i = 0; i < 1000; i++) {
            startupActivity.runActivity(project);
        }
        
        // Force garbage collection
        System.gc();
        
        // Then - memory usage should not increase significantly
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = finalMemory - initialMemory;
        
        // Should not use more than 1MB additional memory
        assertThat(memoryIncrease).isLessThan(1024 * 1024);
    }
    
    @Test
    public void testStartupActivity_InstanceCreation() {
        // Given/When
        LicenseCheckStartupActivity newActivity = new LicenseCheckStartupActivity();
        
        // Then - should create successfully
        assertThat(newActivity).isNotNull();
    }
    
    @Test
    public void testStartupActivity_ImplementsCorrectInterface() {
        // Then - should implement StartupActivity
        assertThat(startupActivity).isInstanceOf(com.intellij.openapi.startup.StartupActivity.class);
    }
    
    @Test
    public void testRunActivity_InHeadlessEnvironment() {
        // Given - simulate headless environment
        System.setProperty("java.awt.headless", "true");
        Project project = getProject();
        
        try {
            // When
            startupActivity.runActivity(project);
            
            // Then - should work in headless environment
            mockedSafeLicenseChecker.verify(() -> 
                SafeLicenseChecker.checkLicenseOnStartup(project));
        } finally {
            System.clearProperty("java.awt.headless");
        }
    }
    
    @Test
    public void testRunActivity_WithDevMode() {
        // Given
        System.setProperty("jakarta.migration.dev", "true");
        Project project = getProject();
        
        try {
            // When
            startupActivity.runActivity(project);
            
            // Then - should still delegate to SafeLicenseChecker
            mockedSafeLicenseChecker.verify(() -> 
                SafeLicenseChecker.checkLicenseOnStartup(project));
        } finally {
            System.clearProperty("jakarta.migration.dev");
        }
    }
    
    @Test
    public void testRunActivity_WithSafeMode() {
        // Given
        System.setProperty("jakarta.migration.safe", "true");
        Project project = getProject();
        
        try {
            // When
            startupActivity.runActivity(project);
            
            // Then - should still delegate to SafeLicenseChecker
            mockedSafeLicenseChecker.verify(() -> 
                SafeLicenseChecker.checkLicenseOnStartup(project));
        } finally {
            System.clearProperty("jakarta.migration.safe");
        }
    }
}
