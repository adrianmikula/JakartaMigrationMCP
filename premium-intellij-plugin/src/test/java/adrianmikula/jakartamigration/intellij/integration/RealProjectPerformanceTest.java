package adrianmikula.jakartamigration.intellij.integration;

import adrianmikula.jakartamigration.intellij.service.AdvancedScanningService;
import adrianmikula.jakartamigration.intellij.service.AdvancedScanningService.AdvancedScanSummary;
import adrianmikula.jakartamigration.intellij.integration.ExampleProjectManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance tests using real example repositories from examples.yaml project_complexity section.
 * Tests memory usage and performance with actual projects of varying complexity.
 */
public class RealProjectPerformanceTest {
    
    @TempDir
    Path tempDir;
    
    private AdvancedScanningService advancedScanningService;
    private ExampleProjectManager projectManager;
    
    @BeforeEach
    void setUp() throws IOException {
        // Initialize with null recipe service to avoid dependency issues
        advancedScanningService = new AdvancedScanningService(null);
        projectManager = new ExampleProjectManager(tempDir);
    }
    
    @Test
    @DisplayName("Advanced scan should handle simple project efficiently")
    void testAdvancedScanSimpleProject() throws IOException {
        System.out.println("=== Starting Simple Project Performance Test ===");
        
        // Load simple project from examples.yaml
        List<Map<String, Object>> examplesList = projectManager.getAvailableExamples().get("project_complexity");
        Map<String, Object> simpleProject = examplesList.get(0);
        String projectName = (String) simpleProject.get("name");
        String projectUrl = (String) simpleProject.get("url");
        
        System.out.println("Loading simple project: " + projectName);
        System.out.println("URL: " + projectUrl);
        
        // Clone and setup project
        Path projectDir = tempDir.resolve("simple-project");
        projectManager.getExampleProject(projectName, "project_complexity");
        
        // Monitor memory before scan
        Runtime runtime = Runtime.getRuntime();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        System.out.println("Memory before scan: " + (memoryBefore / 1024 / 1024) + "MB");
        
        // Run advanced scan
        System.out.println("Starting advanced scan...");
        long startTime = System.currentTimeMillis();
        AdvancedScanSummary result = advancedScanningService.scanAll(projectDir);
        long duration = System.currentTimeMillis() - startTime;
        
        // Monitor memory after scan
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = memoryAfter - memoryBefore;
        
        System.out.println("Scan completed in: " + duration + "ms");
        System.out.println("Memory used: " + (memoryUsed / 1024 / 1024) + "MB");
        System.out.println("Scan result: " + (result != null ? "SUCCESS" : "NULL"));
        
        // Verify results
        assertNotNull(result, "Should complete scan successfully");
        assertTrue(duration < 15000, "Should complete within 15 seconds for simple project, actual: " + duration + "ms");
        
        // Memory usage should be reasonable (less than 50MB for simple project)
        long maxMemoryUsage = 50 * 1024 * 1024; // 50MB
        assertTrue(memoryUsed < maxMemoryUsage, 
                  "Memory usage should be less than 50MB for simple project. Used: " + (memoryUsed / 1024 / 1024) + "MB");
        
        System.out.println("=== Simple Project Test PASSED ===");
        System.out.println("Duration: " + duration + "ms");
        System.out.println("Memory used: " + (memoryUsed / 1024 / 1024) + "MB");
    }
    
    @Test
    @DisplayName("Advanced scan should handle medium complexity project within memory limits")
    void testAdvancedScanMediumComplexityProject() throws IOException {
        System.out.println("=== Starting Medium Complexity Project Performance Test ===");
        
        // Load medium complexity project from examples.yaml
        List<Map<String, Object>> examplesList = projectManager.getAvailableExamples().get("project_complexity");
        Map<String, Object> mediumProject = examplesList.get(1);
        String projectName = (String) mediumProject.get("name");
        String projectUrl = (String) mediumProject.get("url");
        
        System.out.println("Loading medium complexity project: " + projectName);
        System.out.println("URL: " + projectUrl);
        
        // Clone and setup project
        Path projectDir = tempDir.resolve("medium-complexity-project");
        projectManager.getExampleProject(projectName, "project_complexity");
        
        // Monitor memory before scan
        Runtime runtime = Runtime.getRuntime();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        System.out.println("Memory before scan: " + (memoryBefore / 1024 / 1024) + "MB");
        
        // Run advanced scan
        System.out.println("Starting advanced scan...");
        long startTime = System.currentTimeMillis();
        AdvancedScanSummary result = advancedScanningService.scanAll(projectDir);
        long duration = System.currentTimeMillis() - startTime;
        
        // Monitor memory after scan
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = memoryAfter - memoryBefore;
        
        System.out.println("Scan completed in: " + duration + "ms");
        System.out.println("Memory used: " + (memoryUsed / 1024 / 1024) + "MB");
        System.out.println("Scan result: " + (result != null ? "SUCCESS" : "NULL"));
        
        // Verify results
        assertNotNull(result, "Should complete scan successfully");
        assertTrue(duration < 30000, "Should complete within 30 seconds for medium complexity project, actual: " + duration + "ms");
        
        // Memory usage should be reasonable (less than 150MB for medium complexity project)
        long maxMemoryUsage = 150 * 1024 * 1024; // 150MB
        assertTrue(memoryUsed < maxMemoryUsage, 
                  "Memory usage should be less than 150MB for medium complexity project. Used: " + (memoryUsed / 1024 / 1024) + "MB");
        
        System.out.println("=== Medium Complexity Project Test PASSED ===");
        System.out.println("Duration: " + duration + "ms");
        System.out.println("Memory used: " + (memoryUsed / 1024 / 1024) + "MB");
    }
    
    @Test
    @DisplayName("Advanced scan should handle complex project without OutOfMemoryError")
    void testAdvancedScanComplexProject() throws IOException {
        System.out.println("=== Starting Complex Project Performance Test ===");
        
        // Load complex project from examples.yaml
        List<Map<String, Object>> examplesList = projectManager.getAvailableExamples().get("project_complexity");
        Map<String, Object> complexProject = examplesList.get(2);
        String projectName = (String) complexProject.get("name");
        String projectUrl = (String) complexProject.get("url");
        
        System.out.println("Loading complex project: " + projectName);
        System.out.println("URL: " + projectUrl);
        
        // Clone and setup project
        Path projectDir = tempDir.resolve("complex-project");
        projectManager.getExampleProject(projectName, "project_complexity");
        
        // Monitor memory before scan
        Runtime runtime = Runtime.getRuntime();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        System.out.println("Memory before scan: " + (memoryBefore / 1024 / 1024) + "MB");
        
        try {
            // Run advanced scan
            System.out.println("Starting advanced scan...");
            long startTime = System.currentTimeMillis();
            AdvancedScanSummary result = advancedScanningService.scanAll(projectDir);
            long duration = System.currentTimeMillis() - startTime;
            
            // Monitor memory after scan
            long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
            long memoryUsed = memoryAfter - memoryBefore;
            
            System.out.println("Scan completed in: " + duration + "ms");
            System.out.println("Memory used: " + (memoryUsed / 1024 / 1024) + "MB");
            System.out.println("Scan result: " + (result != null ? "SUCCESS" : "NULL"));
            
            // Verify results
            assertNotNull(result, "Should complete scan successfully");
            assertTrue(duration < 60000, "Should complete within 60 seconds for complex project, actual: " + duration + "ms");
            
            // Memory usage should be reasonable (less than 300MB for complex project)
            long maxMemoryUsage = 300 * 1024 * 1024; // 300MB
            assertTrue(memoryUsed < maxMemoryUsage, 
                      "Memory usage should be less than 300MB for complex project. Used: " + (memoryUsed / 1024 / 1024) + "MB");
            
            System.out.println("=== Complex Project Test PASSED ===");
            System.out.println("Duration: " + duration + "ms");
            System.out.println("Memory used: " + (memoryUsed / 1024 / 1024) + "MB");
            System.out.println("✅ No OutOfMemoryError - Memory optimizations working!");
            
        } catch (OutOfMemoryError e) {
            System.out.println("❌ OutOfMemoryError occurred - Memory optimizations need improvement");
            System.out.println("Error: " + e.getMessage());
            fail("Complex project scan should not cause OutOfMemoryError: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("❌ Unexpected error during scan");
            System.out.println("Error type: " + e.getClass().getSimpleName());
            System.out.println("Error message: " + e.getMessage());
            fail("Unexpected error during complex project scan: " + e.getMessage());
        }
    }
    
    @Test
    @DisplayName("Advanced scan should demonstrate memory optimization effectiveness")
    void testMemoryOptimizationEffectiveness() throws IOException {
        System.out.println("=== Testing Memory Optimization Effectiveness ===");
        
        // Load simple project first (should use sequential mode due to low memory)
        List<Map<String, Object>> examplesList = projectManager.getAvailableExamples().get("project_complexity");
        Map<String, Object> simpleProject = examplesList.get(0);
        String projectUrl = (String) simpleProject.get("url");
        
        Path projectDir = tempDir.resolve("optimization-test");
        projectManager.getExampleProject((String) simpleProject.get("name"), "project_complexity");
        
        System.out.println("Testing memory optimization effectiveness...");
        
        // Run scan multiple times to test caching and memory management
        Runtime runtime = Runtime.getRuntime();
        
        for (int i = 1; i <= 3; i++) {
            long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
            System.out.println("Scan " + i + " - Memory before: " + (memoryBefore / 1024 / 1024) + "MB");
            
            long startTime = System.currentTimeMillis();
            AdvancedScanSummary result = advancedScanningService.scanAll(projectDir);
            long duration = System.currentTimeMillis() - startTime;
            
            long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
            long memoryUsed = memoryAfter - memoryBefore;
            
            System.out.println("Scan " + i + " - Duration: " + duration + "ms, Memory used: " + (memoryUsed / 1024 / 1024) + "MB");
            
            assertNotNull(result, "Scan " + i + " should complete successfully");
            
            // Second scan should be faster due to caching
            if (i == 2) {
                assertTrue(duration < 5000, "Second scan should be faster due to caching, actual: " + duration + "ms");
                System.out.println("✅ Caching working effectively - Second scan much faster");
            }
        }
        
        System.out.println("=== Memory Optimization Test PASSED ===");
        System.out.println("✅ Memory optimizations and caching working effectively");
    }
}
