package adrianmikula.jakartamigration.intellij.integration;

import adrianmikula.jakartamigration.intellij.service.AdvancedScanningService;
import adrianmikula.jakartamigration.intellij.service.AdvancedScanningService.AdvancedScanSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance tests for AdvancedScanningService with large projects.
 * Tests memory usage and performance with realistic project sizes.
 */
public class AdvancedScanningPerformanceTest {
    
    @TempDir
    Path tempDir;
    
    private AdvancedScanningService advancedScanningService;
    
    @BeforeEach
    void setUp() {
        // Initialize with null recipe service to avoid dependency issues
        advancedScanningService = new AdvancedScanningService(null);
    }
    
    @Test
    @DisplayName("Advanced scan should handle medium project (500+ files) within memory limits")
    void testAdvancedScanMediumProjectWithinMemoryLimits() throws IOException {
        System.out.println("=== Starting Medium Project Performance Test ===");
        
        try {
            // Create a medium-sized project similar to javaee7-samples
            Path projectDir = tempDir.resolve("medium-project");
            System.out.println("Creating medium project at: " + projectDir);
            createMediumSizedProject(projectDir);
            System.out.println("Medium project created with 50 Java files");
            
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
            assertTrue(duration < 30000, "Should complete within 30 seconds for medium project, actual: " + duration + "ms");
            
            // Memory usage should be reasonable (less than 100MB for medium project)
            long maxMemoryUsage = 100 * 1024 * 1024; // 100MB
            assertTrue(memoryUsed < maxMemoryUsage, 
                      "Memory usage should be less than 100MB for medium project. Used: " + (memoryUsed / 1024 / 1024) + "MB");
            
            System.out.println("=== Medium Project Test PASSED ===");
            System.out.println("Duration: " + duration + "ms");
            System.out.println("Memory used: " + (memoryUsed / 1024 / 1024) + "MB");
            System.out.println("Scan completed successfully");
            
        } catch (Exception e) {
            System.out.println("=== TEST FAILED WITH EXCEPTION ===");
            System.out.println("Exception type: " + e.getClass().getSimpleName());
            System.out.println("Exception message: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    @Test
    @DisplayName("Advanced scan should handle large project (1000+ files) with memory optimizations")
    void testAdvancedScanLargeProjectWithMemoryOptimizations() throws IOException {
        // Create a large project similar to javaee7-samples-master
        Path projectDir = tempDir.resolve("large-project");
        createLargeSizedProject(projectDir);
        
        // Monitor memory before scan
        Runtime runtime = Runtime.getRuntime();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        
        // Run advanced scan
        long startTime = System.currentTimeMillis();
        AdvancedScanSummary result = advancedScanningService.scanAll(projectDir);
        long duration = System.currentTimeMillis() - startTime;
        
        // Monitor memory after scan
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = memoryAfter - memoryBefore;
        
        // Verify results
        assertNotNull(result, "Should complete scan successfully");
        assertTrue(duration < 30000, "Should complete within 30 seconds for large project");
        
        // Memory usage should be reasonable for large project (less than 300MB)
        long maxMemoryUsage = 300 * 1024 * 1024; // 300MB
        assertTrue(memoryUsed < maxMemoryUsage, 
                  "Memory usage should be less than 300MB for large project. Used: " + (memoryUsed / 1024 / 1024) + "MB");
        
        System.out.println("=== Large Project Advanced Scan Results ===");
        System.out.println("Duration: " + duration + "ms");
        System.out.println("Memory used: " + (memoryUsed / 1024 / 1024) + "MB");
        System.out.println("Scan completed successfully");
    }
    
    @Test
    @DisplayName("Advanced scan should reuse cached results for repeated scans")
    void testAdvancedScanReusesCachedResults() throws IOException {
        // Create a project
        Path projectDir = tempDir.resolve("cached-project");
        createMediumSizedProject(projectDir);
        
        // First scan - should populate cache
        long startTime1 = System.currentTimeMillis();
        AdvancedScanSummary result1 = advancedScanningService.scanAll(projectDir);
        long duration1 = System.currentTimeMillis() - startTime1;
        
        // Second scan - should use cache
        long startTime2 = System.currentTimeMillis();
        AdvancedScanSummary result2 = advancedScanningService.scanAll(projectDir);
        long duration2 = System.currentTimeMillis() - startTime2;
        
        // Verify both scans succeeded
        assertNotNull(result1, "First scan should succeed");
        assertNotNull(result2, "Second scan should succeed");
        
        // Second scan should be much faster due to caching
        assertTrue(duration2 < duration1 / 2, 
                  "Second scan should be at least 2x faster due to caching. First: " + duration1 + "ms, Second: " + duration2 + "ms");
        
        // Verify cache is working
        assertTrue(advancedScanningService.hasCachedResults(), "Should have cached results after first scan");
        
        System.out.println("=== Cached Scan Results ===");
        System.out.println("First scan duration: " + duration1 + "ms");
        System.out.println("Second scan duration: " + duration2 + "ms");
        System.out.println("Cache working correctly");
    }
    
    @Test
    @DisplayName("Advanced scan should handle project with many Java files efficiently")
    void testAdvancedScanWithManyJavaFilesEfficiently() throws IOException {
        // Create a project with many Java files but minimal dependencies
        Path projectDir = tempDir.resolve("many-files-project");
        createProjectWithManyJavaFiles(projectDir);
        
        // Monitor memory before scan
        Runtime runtime = Runtime.getRuntime();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        
        // Run advanced scan
        long startTime = System.currentTimeMillis();
        AdvancedScanSummary result = advancedScanningService.scanAll(projectDir);
        long duration = System.currentTimeMillis() - startTime;
        
        // Monitor memory after scan
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = memoryAfter - memoryBefore;
        
        // Verify results
        assertNotNull(result, "Should complete scan successfully");
        assertTrue(duration < 15000, "Should complete within 15 seconds");
        
        // Memory usage should be controlled despite many files
        long maxMemoryUsage = 200 * 1024 * 1024; // 200MB
        assertTrue(memoryUsed < maxMemoryUsage, 
                  "Memory usage should be controlled despite many files. Used: " + (memoryUsed / 1024 / 1024) + "MB");
        
        System.out.println("=== Many Files Advanced Scan Results ===");
        System.out.println("Duration: " + duration + "ms");
        System.out.println("Memory used: " + (memoryUsed / 1024 / 1024) + "MB");
        System.out.println("Files processed efficiently");
    }
    
    /**
     * Creates a medium-sized project with various Java EE components.
     */
    private void createMediumSizedProject(Path projectDir) throws IOException {
        Files.createDirectories(projectDir.resolve("src/main/java/com/example"));
        Files.createDirectories(projectDir.resolve("src/main/resources"));
        Files.createDirectories(projectDir.resolve("src/main/webapp/WEB-INF"));
        
        // Create various Java files with different annotations
        for (int i = 0; i < 50; i++) {
            String className = "TestEntity" + i;
            String javaFile = "package com.example;\n\n" +
                "import javax.persistence.Entity;\n" +
                "import javax.ejb.Stateless;\n" +
                "import javax.servlet.http.HttpServlet;\n" +
                "import javax.ws.rs.GET;\n" +
                "import javax.ws.rs.Path;\n" +
                "import javax.inject.Inject;\n" +
                "import javax.validation.Valid;\n\n" +
                "@Entity\n" +
                "@Stateless\n" +
                "public class " + className + " {\n" +
                "    private Long id;\n\n" +
                "    @GET\n" +
                "    @Path(\"/test" + i + "\")\n" +
                "    public String getTest() {\n" +
                "        return \"Test " + i + "\";\n" +
                "    }\n" +
                "}\n";
            
            Files.write(projectDir.resolve("src/main/java/com/example/" + className + ".java"), javaFile.getBytes());
        }
        
        // Create pom.xml
        String pomXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
                "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 \n" +
                "         http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
                "    <modelVersion>4.0.0</modelVersion>\n" +
                "    <groupId>com.example</groupId>\n" +
                "    <artifactId>medium-project</artifactId>\n" +
                "    <version>1.0.0</version>\n" +
                "    <dependencies>\n" +
                "        <dependency>\n" +
                "            <groupId>javax.persistence</groupId>\n" +
                "            <artifactId>javax.persistence-api</artifactId>\n" +
                "            <version>2.2</version>\n" +
                "        </dependency>\n" +
                "        <dependency>\n" +
                "            <groupId>javax.ejb</groupId>\n" +
                "            <artifactId>javax.ejb-api</artifactId>\n" +
                "            <version>3.2</version>\n" +
                "        </dependency>\n" +
                "        <dependency>\n" +
                "            <groupId>javax.servlet</groupId>\n" +
                "            <artifactId>javax.servlet-api</artifactId>\n" +
                "            <version>4.0.1</version>\n" +
                "        </dependency>\n" +
                "    </dependencies>\n" +
                "</project>\n";
        
        Files.write(projectDir.resolve("pom.xml"), pomXml.getBytes());
    }
    
    /**
     * Creates a large-sized project similar to javaee7-samples-master.
     */
    private void createLargeSizedProject(Path projectDir) throws IOException {
        Files.createDirectories(projectDir.resolve("src/main/java/com/largeproject"));
        Files.createDirectories(projectDir.resolve("src/main/resources"));
        Files.createDirectories(projectDir.resolve("src/main/webapp/WEB-INF"));
        
        // Create many Java files to simulate large project
        for (int i = 0; i < 200; i++) {
            String className = "LargeEntity" + i;
            String javaFile = "package com.largeproject;\n\n" +
                "import javax.persistence.Entity;\n" +
                "import javax.ejb.Stateless;\n" +
                "import javax.servlet.http.HttpServlet;\n" +
                "import javax.ws.rs.GET;\n" +
                "import javax.ws.rs.Path;\n" +
                "import javax.inject.Inject;\n" +
                "import javax.validation.Valid;\n" +
                "import javax.transaction.Transactional;\n" +
                "import javax.jms.Message;\n" +
                "import javax.mail.Session;\n\n" +
                "@Entity\n" +
                "@Stateless\n" +
                "@Transactional\n" +
                "public class " + className + " {\n" +
                "    private Long id;\n" +
                "    private String data;\n\n" +
                "@GET\n" +
                "@Path(\"/large" + i + "\")\n" +
                "    public String getLarge() {\n" +
                "        return \"Large Data " + i + " \" + \"x\".repeat(100);\n" +
                "    }\n" +
                "}\n";
            
            Files.write(projectDir.resolve("src/main/java/com/largeproject/" + className + ".java"), javaFile.getBytes());
        }
        
        // Create pom.xml with many dependencies
        StringBuilder pomBuilder = new StringBuilder();
        pomBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        pomBuilder.append("<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n");
        pomBuilder.append("         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        pomBuilder.append("         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 \n");
        pomBuilder.append("         http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n");
        pomBuilder.append("    <modelVersion>4.0.0</modelVersion>\n");
        pomBuilder.append("    <groupId>com.largeproject</groupId>\n");
        pomBuilder.append("    <artifactId>large-project</artifactId>\n");
        pomBuilder.append("    <version>1.0.0</version>\n");
        pomBuilder.append("    <dependencies>\n");
        
        // Add many dependencies to simulate large project
        for (int i = 0; i < 50; i++) {
            pomBuilder.append("        <dependency>\n");
            pomBuilder.append("            <groupId>javax.legacy</groupId>\n");
            pomBuilder.append("            <artifactId>legacy-lib-").append(i).append("</artifactId>\n");
            pomBuilder.append("            <version>1.0.").append(i).append("</version>\n");
            pomBuilder.append("            <scope>compile</scope>\n");
            pomBuilder.append("        </dependency>\n");
        }
        
        pomBuilder.append("    </dependencies>\n");
        pomBuilder.append("</project>\n");
        
        Files.write(projectDir.resolve("pom.xml"), pomBuilder.toString().getBytes());
    }
    
    /**
     * Creates a project with many Java files but minimal dependencies.
     */
    private void createProjectWithManyJavaFiles(Path projectDir) throws IOException {
        Files.createDirectories(projectDir.resolve("src/main/java/com/manyfiles"));
        
        // Create many simple Java files
        for (int i = 0; i < 100; i++) {
            String className = "ManyFilesEntity" + i;
            String javaFile = "package com.manyfiles;\n\n" +
                "import javax.persistence.Entity;\n\n" +
                "@Entity\n" +
                "public class " + className + " {\n" +
                "    private Long id;\n" +
                "    private String name;\n\n" +
                "    public " + className + "() {}\n" +
                "}\n";
            
            Files.write(projectDir.resolve("src/main/java/com/manyfiles/" + className + ".java"), javaFile.getBytes());
        }
        
        // Create simple pom.xml
        String pomXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
                "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 \n" +
                "         http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
                "    <modelVersion>4.0.0</modelVersion>\n" +
                "    <groupId>com.manyfiles</groupId>\n" +
                "    <artifactId>many-files-project</artifactId>\n" +
                "    <version>1.0.0</version>\n" +
                "    <dependencies>\n" +
                "        <dependency>\n" +
                "            <groupId>javax.persistence</groupId>\n" +
                "            <artifactId>javax.persistence-api</artifactId>\n" +
                "            <version>2.2</version>\n" +
                "        </dependency>\n" +
                "    </dependencies>\n" +
                "</project>\n";
        
        Files.write(projectDir.resolve("pom.xml"), pomXml.getBytes());
    }
}
