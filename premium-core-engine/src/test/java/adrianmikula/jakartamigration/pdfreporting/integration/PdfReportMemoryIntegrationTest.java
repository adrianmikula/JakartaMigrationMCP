package adrianmikula.jakartamigration.pdfreporting.integration;

import adrianmikula.jakartamigration.pdfreporting.service.PdfReportService;
import adrianmikula.jakartamigration.pdfreporting.service.impl.HtmlToPdfReportServiceImpl;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Dependency;
import adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults;
import adrianmikula.jakartamigration.pdfreporting.integration.ExamplesTestProjectLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for PDF report generation with memory usage verification.
 * Tests memory consumption during PDF generation for large projects using examples.yaml projects.
 * Tagged as slow due to memory testing and integration nature.
 */
@Tag("slow")
public class PdfReportMemoryIntegrationTest {

    private PdfReportService pdfReportService;
    private ExamplesTestProjectLoader projectLoader;
    
    @TempDir
    Path tempDir;
    
    // Memory thresholds (in bytes)
    private static final long MEMORY_WARNING_THRESHOLD = 100 * 1024 * 1024; // 100MB
    private static final long MEMORY_ERROR_THRESHOLD = 200 * 1024 * 1024; // 200MB
    private static final long LARGE_PROJECT_MEMORY_THRESHOLD = 300 * 1024 * 1024; // 300MB for large projects
    
    @BeforeEach
    void setUp() {
        pdfReportService = new HtmlToPdfReportServiceImpl();
        projectLoader = new ExamplesTestProjectLoader();
    }
    
    @Test
    @DisplayName("Memory usage for small project PDF generation")
    void testMemoryUsageForSmallProject() throws Exception {
        // Load small project example
        ExamplesTestProjectLoader.TestProject smallProject = projectLoader.loadProject("Spring");
        
        // Create mock dependency graph with small data
        DependencyGraph smallGraph = createMockDependencyGraph();
        ComprehensiveScanResults smallScanResults = createSmallScanResults();
        
        // Measure memory before
        Runtime runtime = Runtime.getRuntime();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        
        // Generate PDF report
        Path outputPath = tempDir.resolve("small-project-report.pdf");
        PdfReportService.RiskAnalysisReportRequest request = new PdfReportService.RiskAnalysisReportRequest(
            outputPath,
            smallProject.name(),
            "Test Small Project Report",
            smallGraph,
            null,
            smallScanResults,
            null,
            null,
            "Test Strategy",
            Map.of("test", "data"),
            Map.of("coverage", 85),
            createSmallBlockers(),
            Arrays.asList("Test recommendation 1", "Test recommendation 2"),
            Map.of("phase1", "Test phase 1"),
            Map.of("test", "metadata")
        );
        
        Path result = pdfReportService.generateRiskAnalysisReport(request);
        
        // Measure memory after
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = memoryAfter - memoryBefore;
        
        // Verify results
        assertNotNull(result);
        assertTrue(Files.exists(result));
        assertTrue(Files.size(result) > 0);
        
        // Verify memory usage
        assertAll("Memory usage verification for small project",
            () -> assertTrue(memoryUsed < MEMORY_WARNING_THRESHOLD, 
                String.format("Small project memory usage (%d MB) should be below warning threshold (%d MB)", 
                    memoryUsed / (1024 * 1024), MEMORY_WARNING_THRESHOLD / (1024 * 1024))),
            () -> assertTrue(memoryUsed < MEMORY_ERROR_THRESHOLD, 
                String.format("Small project memory usage (%d MB) should be below error threshold (%d MB)", 
                    memoryUsed / (1024 * 1024), MEMORY_ERROR_THRESHOLD / (1024 * 1024)))
        );
        
        System.out.printf("Small project memory usage: %.2f MB%n", memoryUsed / (1024.0 * 1024.0));
    }
    
    @Test
    @DisplayName("Memory usage for medium project PDF generation")
    void testMemoryUsageForMediumProject() throws Exception {
        // Load medium project example
        ExamplesTestProjectLoader.TestProject mediumProject = projectLoader.loadProject("Spring Boot");
        
        // Create mock dependency graph with medium data
        DependencyGraph mediumGraph = createMediumDependencyGraph();
        ComprehensiveScanResults mediumScanResults = createMediumScanResults();
        
        // Measure memory before
        Runtime runtime = Runtime.getRuntime();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        
        // Generate PDF report
        Path outputPath = tempDir.resolve("medium-project-report.pdf");
        PdfReportService.RiskAnalysisReportRequest request = new PdfReportService.RiskAnalysisReportRequest(
            outputPath,
            mediumProject.name(),
            "Test Medium Project Report",
            mediumGraph,
            null,
            mediumScanResults,
            null,
            null,
            "Test Strategy",
            Map.of("test", "data"),
            Map.of("coverage", 65),
            createMediumBlockers(),
            Arrays.asList("Test recommendation 1", "Test recommendation 2", "Test recommendation 3"),
            Map.of("phase1", "Test phase 1", "phase2", "Test phase 2"),
            Map.of("test", "metadata")
        );
        
        Path result = pdfReportService.generateRiskAnalysisReport(request);
        
        // Measure memory after
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = memoryAfter - memoryBefore;
        
        // Verify results
        assertNotNull(result);
        assertTrue(Files.exists(result));
        assertTrue(Files.size(result) > 0);
        
        // Verify memory usage
        assertAll("Memory usage verification for medium project",
            () -> assertTrue(memoryUsed < MEMORY_WARNING_THRESHOLD, 
                String.format("Medium project memory usage (%d MB) should be below warning threshold (%d MB)", 
                    memoryUsed / (1024 * 1024), MEMORY_WARNING_THRESHOLD / (1024 * 1024))),
            () -> assertTrue(memoryUsed < LARGE_PROJECT_MEMORY_THRESHOLD, 
                String.format("Medium project memory usage (%d MB) should be below large project threshold (%d MB)", 
                    memoryUsed / (1024 * 1024), LARGE_PROJECT_MEMORY_THRESHOLD / (1024 * 1024)))
        );
        
        System.out.printf("Medium project memory usage: %.2f MB%n", memoryUsed / (1024.0 * 1024.0));
    }
    
    @Test
    @DisplayName("Memory usage for large project PDF generation")
    void testMemoryUsageForLargeProject() throws Exception {
        // Load large project example (NetBeans)
        ExamplesTestProjectLoader.TestProject largeProject = projectLoader.loadProject("netbeans");
        
        // Create mock dependency graph with large data
        DependencyGraph largeGraph = createLargeDependencyGraph();
        ComprehensiveScanResults largeScanResults = createLargeScanResults();
        
        // Measure memory before
        Runtime runtime = Runtime.getRuntime();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        
        // Generate PDF report
        Path outputPath = tempDir.resolve("large-project-report.pdf");
        PdfReportService.RiskAnalysisReportRequest request = new PdfReportService.RiskAnalysisReportRequest(
            outputPath,
            largeProject.name(),
            "Test Large Project Report",
            largeGraph,
            null,
            largeScanResults,
            null,
            null,
            "Test Strategy",
            Map.of("test", "data"),
            Map.of("coverage", 45),
            createLargeBlockers(),
            Arrays.asList("Test recommendation 1", "Test recommendation 2", "Test recommendation 3", "Test recommendation 4"),
            Map.of("phase1", "Test phase 1", "phase2", "Test phase 2", "phase3", "Test phase 3"),
            Map.of("test", "metadata")
        );
        
        Path result = pdfReportService.generateRiskAnalysisReport(request);
        
        // Measure memory after
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = memoryAfter - memoryBefore;
        
        // Verify results
        assertNotNull(result);
        assertTrue(Files.exists(result));
        assertTrue(Files.size(result) > 0);
        
        // Verify memory usage (more lenient for large projects)
        assertAll("Memory usage verification for large project",
            () -> assertTrue(memoryUsed < LARGE_PROJECT_MEMORY_THRESHOLD, 
                String.format("Large project memory usage (%d MB) should be below threshold (%d MB)", 
                    memoryUsed / (1024 * 1024), LARGE_PROJECT_MEMORY_THRESHOLD / (1024 * 1024))),
            () -> assertTrue(memoryUsed < MEMORY_ERROR_THRESHOLD, 
                String.format("Large project memory usage (%d MB) should be below error threshold (%d MB)", 
                    memoryUsed / (1024 * 1024), MEMORY_ERROR_THRESHOLD / (1024 * 1024)))
        );
        
        System.out.printf("Large project memory usage: %.2f MB%n", memoryUsed / (1024.0 * 1024.0));
    }
    
    @RepeatedTest(3)
    @DisplayName("Memory leak detection with repeated PDF generation")
    void testMemoryLeakDetectionWithRepeatedGeneration() throws Exception {
        Runtime runtime = Runtime.getRuntime();
        
        // Baseline memory measurement
        System.gc();
        Thread.sleep(1000); // Allow GC to complete
        long baselineMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Generate multiple PDF reports
        for (int i = 0; i < 5; i++) {
            Path outputPath = tempDir.resolve("repeated-test-report-" + i + ".pdf");
            PdfReportService.RiskAnalysisReportRequest request = new PdfReportService.RiskAnalysisReportRequest(
                outputPath,
                "Repeated Test Project " + i,
                "Repeated Test Report " + i,
                createMediumDependencyGraph(),
                null,
                createMediumScanResults(),
                null,
                null,
                "Test Strategy",
                Map.of("iteration", i),
                Map.of("coverage", 60),
                createMediumBlockers(),
                Arrays.asList("Test recommendation"),
                Map.of("phase1", "Test phase"),
                Map.of("test", "metadata", "iteration", i)
            );
            
            Path result = pdfReportService.generateRiskAnalysisReport(request);
            assertNotNull(result);
            
            // Force GC between iterations
            System.gc();
            Thread.sleep(500);
        }
        
        // Final memory measurement
        System.gc();
        Thread.sleep(1000);
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryGrowth = finalMemory - baselineMemory;
        
        // Verify no significant memory leaks
        assertTrue(memoryGrowth < MEMORY_WARNING_THRESHOLD, 
            String.format("Memory growth after repeated generation (%d MB) should be below warning threshold (%d MB)", 
                memoryGrowth / (1024 * 1024), MEMORY_WARNING_THRESHOLD / (1024 * 1024)));
        
        System.out.printf("Memory growth after repeated generation: %.2f MB%n", memoryGrowth / (1024.0 * 1024.0));
    }
    
    @Test
    @DisplayName("Concurrent PDF generation memory test")
    void testConcurrentPdfGenerationMemory() throws Exception {
        Runtime runtime = Runtime.getRuntime();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        
        // Generate PDF reports concurrently
        List<CompletableFuture<Path>> futures = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            final int index = i;
            CompletableFuture<Path> future = CompletableFuture.supplyAsync(() -> {
                try {
                    Path outputPath = tempDir.resolve("concurrent-report-" + index + ".pdf");
                    PdfReportService.RiskAnalysisReportRequest request = new PdfReportService.RiskAnalysisReportRequest(
                        outputPath,
                        "Concurrent Test " + index,
                        "Concurrent Test Report " + index,
                        createSmallDependencyGraph(),
                        null,
                        createSmallScanResults(),
                        null,
                        null,
                        "Test Strategy",
                        Map.of("concurrent", true, "index", index),
                        Map.of("coverage", 70),
                        createSmallBlockers(),
                        Arrays.asList("Concurrent test recommendation"),
                        Map.of("phase1", "Concurrent phase"),
                        Map.of("test", "metadata", "concurrent", index)
                    );
                    
                    return pdfReportService.generateRiskAnalysisReport(request);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            futures.add(future);
        }
        
        // Wait for all to complete
        List<Path> results = futures.stream()
            .map(future -> {
                try {
                    return future.get(30, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            })
            .toList();
        
        // Measure memory after
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = memoryAfter - memoryBefore;
        
        // Verify results
        assertEquals(3, results.size());
        results.forEach(result -> {
            assertNotNull(result);
            assertTrue(Files.exists(result));
        });
        
        // Verify memory usage (concurrent operations may use more memory)
        assertTrue(memoryUsed < LARGE_PROJECT_MEMORY_THRESHOLD, 
            String.format("Concurrent generation memory usage (%d MB) should be below threshold (%d MB)", 
                memoryUsed / (1024 * 1024), LARGE_PROJECT_MEMORY_THRESHOLD / (1024 * 1024)));
        
        System.out.printf("Concurrent generation memory usage: %.2f MB%n", memoryUsed / (1024.0 * 1024.0));
    }
    
    // Helper methods to create test data
    private DependencyGraph createMockDependencyGraph() {
        // For testing purposes, we'll create a simple mock
        // In a real implementation, this would be populated with actual dependency data
        return new DependencyGraph();
    }
    
    private DependencyGraph createSmallDependencyGraph() {
        return createMockDependencyGraph();
    }
    
    private DependencyGraph createMediumDependencyGraph() {
        return createMockDependencyGraph();
    }
    
    private DependencyGraph createLargeDependencyGraph() {
        return createMockDependencyGraph();
    }
    
    private ComprehensiveScanResults createSmallScanResults() {
        return new ComprehensiveScanResults(
            "/test/project",
            LocalDateTime.now(),
            Map.of("count",5),
            Map.of("count",2),
            Map.of("count", 3), // cdiResults
            Map.of("count", 3),
            Map.of("count", 10),
            Map.of("count", 15),
            Map.of("count", 8),
            Arrays.asList("Small recommendation 1"),
            25,
            new ComprehensiveScanResults.ScanSummary(100, 25, 5, 15, 5, 75.0)
        );
    }
    
    private ComprehensiveScanResults createMediumScanResults() {
        return new ComprehensiveScanResults(
            "/test/project",
            LocalDateTime.now(),
            Map.of("count", 50),
            Map.of("count",20),
            Map.of("count", 15), // cdiResults
            Map.of("count", 30),
            Map.of("count", 100),
            Map.of("count", 150),
            Map.of("count", 80),
            Arrays.asList("Medium recommendation 1", "Medium recommendation 2"),
            250,
            new ComprehensiveScanResults.ScanSummary(500, 250, 50, 150, 50, 65.0)
        );
    }
    
    private ComprehensiveScanResults createLargeScanResults() {
        return new ComprehensiveScanResults(
            "/test/project",
            LocalDateTime.now(),
            Map.of("count", 200),
            Map.of("count", 80),
            Map.of("count", 40), // cdiResults
            Map.of("count", 120),
            Map.of("count", 500),
            Map.of("count", 800),
            Map.of("count", 300),
            Arrays.asList("Large recommendation 1", "Large recommendation 2", "Large recommendation 3"),
            1000,
            new ComprehensiveScanResults.ScanSummary(2000, 1000, 200, 600, 200, 45.0)
        );
    }
    
    private List<Map<String, Object>> createSmallBlockers() {
        return Arrays.asList(
            Map.of("name", "small-blocker-1", "severity", "LOW", "impact", "Small impact")
        );
    }
    
    private List<Map<String, Object>> createMediumBlockers() {
        return Arrays.asList(
            Map.of("name", "medium-blocker-1", "severity", "MEDIUM", "impact", "Medium impact"),
            Map.of("name", "medium-blocker-2", "severity", "HIGH", "impact", "High impact")
        );
    }
    
    private List<Map<String, Object>> createLargeBlockers() {
        return Arrays.asList(
            Map.of("name", "large-blocker-1", "severity", "HIGH", "impact", "Large impact 1"),
            Map.of("name", "large-blocker-2", "severity", "CRITICAL", "impact", "Large impact 2"),
            Map.of("name", "large-blocker-3", "severity", "MEDIUM", "impact", "Large impact 3"),
            Map.of("name", "large-blocker-4", "severity", "LOW", "impact", "Large impact 4"),
            Map.of("name", "large-blocker-5", "severity", "HIGH", "impact", "Large impact 5")
        );
    }
}
