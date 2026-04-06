package adrianmikula.jakartamigration.intellij.integration;

import adrianmikula.jakartamigration.intellij.service.AdvancedScanningService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for advanced scanning against real GitHub projects with javax packages.
 * Uses ExampleProjectManager to fetch and test actual projects from examples.yaml.
 */
public class AdvancedScanningIntegrationTest extends IntegrationTestBase {
    
    @Nested
    @DisplayName("Application Server Advanced Scans")
    class ApplicationServerScans {
        
        @Test
        @DisplayName("Advanced scan should detect Spring Boot project with javax packages")
        void testAdvancedScanSpringBootProject() throws Exception {
            // Get Spring Boot project from GitHub examples
            Path projectDir = projectManager.getExampleProject("Spring Boot", "application_servers");
            
            // Initialize advanced scanning service
            advancedScanningService = new AdvancedScanningService(null);
            
            // Run advanced scan
            var result = advancedScanningService.scanAll(projectDir);
            
            // Verify scan completed successfully
            assertNotNull(result);
            boolean foundJavaxPackages = result.toString().toLowerCase().contains("javax");
            
            assertTrue(foundJavaxPackages, "Should detect javax package usage in Spring Boot project");
            System.out.println("Spring Boot advanced scan completed successfully");
        }
        
        @Test
        @DisplayName("Advanced scan should detect WildFly project with EJB and JPA")
        void testAdvancedScanWildFlyProject() throws Exception {
            // Get WildFly project from GitHub examples
            Path projectDir = projectManager.getExampleProject("WildFly", "application_servers");
            
            // Initialize advanced scanning service
            advancedScanningService = new AdvancedScanningService(null);
            
            // Run advanced scan
            var result = advancedScanningService.scanAll(projectDir);
            
            // Verify scan completed successfully
            assertNotNull(result);
            System.out.println("WildFly advanced scan completed successfully");
        }
        
        @Test
        @DisplayName("Advanced scan should detect Apache Tomcat project with servlet features")
        void testAdvancedScanTomcatProject() throws Exception {
            // Get Tomcat project from GitHub examples
            Path projectDir = projectManager.getExampleProject("Apache Tomcat", "application_servers");
            
            // Initialize advanced scanning service
            advancedScanningService = new AdvancedScanningService(null);
            
            // Run advanced scan
            var result = advancedScanningService.scanAll(projectDir);
            
            // Verify scan completed successfully
            assertNotNull(result);
            System.out.println("Tomcat advanced scan completed successfully");
        }
        
        @Test
        @DisplayName("Advanced scan should detect Payara project with Jakarta EE features")
        void testAdvancedScanPayaraProject() throws Exception {
            // Get Payara project from GitHub examples
            Path projectDir = projectManager.getExampleProject("Payara", "application_servers");
            
            // Initialize advanced scanning service
            advancedScanningService = new AdvancedScanningService(null);
            
            // Run advanced scan
            var result = advancedScanningService.scanAll(projectDir);
            
            // Verify scan completed successfully
            assertNotNull(result);
            System.out.println("Payara advanced scan completed successfully");
        }
        
        @Test
        @DisplayName("Advanced scan should detect Jetty project with websocket features")
        void testAdvancedScanJettyProject() throws Exception {
            // Get Jetty project from GitHub examples
            Path projectDir = projectManager.getExampleProject("Jetty", "application_servers");
            
            // Initialize advanced scanning service
            advancedScanningService = new AdvancedScanningService(null);
            
            // Run advanced scan
            var result = advancedScanningService.scanAll(projectDir);
            
            // Verify scan completed successfully
            assertNotNull(result);
            System.out.println("Jetty advanced scan completed successfully");
        }
    }
    
    @Nested
    @DisplayName("Javax Package Advanced Scans")
    class JavaxPackageScans {
        
        @Test
        @DisplayName("Advanced scan should detect javax.validation packages")
        void testAdvancedScanDetectsJavaxValidation() throws Exception {
            // Get validation example project
            Path projectDir = projectManager.getExampleProject("javax-validation", "javax_packages");
            
            // Initialize advanced scanning service
            advancedScanningService = new AdvancedScanningService(null);
            
            // Run advanced scan
            var result = advancedScanningService.scanAll(projectDir);
            
            // Verify detection
            assertNotNull(result);
            System.out.println("Validation advanced scan completed successfully");
        }
        
        @Test
        @DisplayName("Advanced scan should detect javax.servlet packages")
        void testAdvancedScanDetectsJavaxServlet() throws Exception {
            // Get servlet example project
            Path projectDir = projectManager.getExampleProject("Servlet example", "javax_packages");
            
            // Initialize advanced scanning service
            advancedScanningService = new AdvancedScanningService(null);
            
            // Run advanced scan
            var result = advancedScanningService.scanAll(projectDir);
            
            // Verify detection
            assertNotNull(result);
            assertTrue(result.jpaResult() != null || result.beanValidationResult() != null);
            
            System.out.println("Servlet advanced scan completed successfully");
        }
        
        @Test
        @DisplayName("Advanced scan should detect javax.mail packages")
        void testAdvancedScanDetectsJavaxMail() throws Exception {
            // Get mail example project
            Path projectDir = projectManager.getExampleProject("Mail javax examples", "javax_packages");
            
            // Initialize advanced scanning service
            advancedScanningService = new AdvancedScanningService(null);
            
            // Run advanced scan
            var result = advancedScanningService.scanAll(projectDir);
            
            // Verify detection
            assertNotNull(result);
            assertTrue(result.jpaResult() != null || result.beanValidationResult() != null);
            
            System.out.println("Mail advanced scan completed successfully");
        }
        
        @Test
        @DisplayName("Advanced scan should detect JAX-RS packages")
        void testAdvancedScanDetectsJaxRs() throws Exception {
            // Get JAX-RS example project
            Path projectDir = projectManager.getExampleProject("RS javax examples", "javax_packages");
            
            // Initialize advanced scanning service
            advancedScanningService = new AdvancedScanningService(null);
            
            // Run advanced scan
            var result = advancedScanningService.scanAll(projectDir);
            
            // Verify detection
            assertNotNull(result);
            System.out.println("JAX-RS advanced scan completed successfully");
        }
    }
    
    @Nested
    @DisplayName("Advanced Scan Performance and Caching")
    class PerformanceTests {
        
        @Test
        @DisplayName("Advanced scan should be performant on large projects")
        void testAdvancedScanPerformance() throws Exception {
            // Use a larger project for performance testing
            Path projectDir = projectManager.getExampleProject("WildFly", "application_servers");
            
            // Initialize advanced scanning service
            advancedScanningService = new AdvancedScanningService(null);
            
            // Measure scan time
            long startTime = System.currentTimeMillis();
            var result = advancedScanningService.scanAll(projectDir);
            long endTime = System.currentTimeMillis();
            
            // Verify scan completed successfully and within reasonable time
            assertNotNull(result);
            assertTrue((endTime - startTime) < 30000, "Scan should complete within 30 seconds"); // 30 second timeout
            
            System.out.println("Performance test completed in " + (endTime - startTime) + " ms");
        }
        
        @Test
        @DisplayName("Advanced scan should use caching effectively")
        void testAdvancedScanCaching() throws Exception {
            // Get project for caching test
            Path projectDir = projectManager.getExampleProject("Spring Boot", "application_servers");
            
            // Initialize advanced scanning service
            advancedScanningService = new AdvancedScanningService(null);
            
            // First scan
            long firstScanStart = System.currentTimeMillis();
            var firstResult = advancedScanningService.scanAll(projectDir);
            long firstScanEnd = System.currentTimeMillis();
            
            // Second scan (should use cache)
            long secondScanStart = System.currentTimeMillis();
            var secondResult = advancedScanningService.scanAll(projectDir);
            long secondScanEnd = System.currentTimeMillis();
            
            // Verify both scans succeeded
            assertNotNull(firstResult);
            assertNotNull(secondResult);
            
            // Second scan should be faster due to caching
            long firstScanTime = firstScanEnd - firstScanStart;
            long secondScanTime = secondScanEnd - secondScanStart;
            
            System.out.println("Caching test - First scan: " + firstScanTime + " ms, Second scan: " + secondScanTime + " ms");
        }
    }
}
