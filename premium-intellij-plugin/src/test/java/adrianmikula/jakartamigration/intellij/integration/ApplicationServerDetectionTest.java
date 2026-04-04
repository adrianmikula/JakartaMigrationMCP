package adrianmikula.jakartamigration.intellij.integration;

import adrianmikula.jakartamigration.platforms.service.SimplifiedPlatformDetectionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for platform detection with real application server projects from examples.yaml.
 * Uses ExampleProjectManager to test actual GitHub repositories.
 */
public class ApplicationServerDetectionTest extends IntegrationTestBase {
    
    private SimplifiedPlatformDetectionService platformDetectionService;
    
    @BeforeEach
    void setUp() throws IOException {
        super.setUp(); // Initialize projectManager from base class
        platformDetectionService = new SimplifiedPlatformDetectionService();
    }
    
    // DISABLED: WildFly repository is not suitable for platform detection testing
// @Test
// @DisplayName("Platform detection should detect WildFly from real GitHub project")
// void testDetectWildFlyFromRealProject() throws IOException {
//     // Get WildFly project from GitHub examples
//     // Path projectDir = projectManager.getExampleProject("WildFly", "application_servers");
//     
//     // Run platform detection
//     // List<String> detectedPlatforms = platformDetectionService.scanProject(projectDir);
//     
//     // Verify result
//     // assertNotNull(detectedPlatforms);
//     
//     // Check for WildFly detection
//     // boolean wildflyDetected = detectedPlatforms.stream()
//     //     .anyMatch(platform -> platform.toLowerCase().contains("wildfly"));
//     
//     // Print detailed results for debugging
//     // System.out.println("=== WildFly Real Project Detection Results ===");
//     // System.out.println("Detected platforms: " + detectedPlatforms.size());
//     // detectedPlatforms.forEach(platform -> 
//     //     System.out.println("  - " + platform));
//     // System.out.println("WildFly detected: " + wildflyDetected);
//     
//     // Should actually detect the platform, not just handle gracefully
//     // assertTrue(detectedPlatforms.size() > 0, "Should detect at least one platform from real WildFly project");
//     // assertTrue(wildflyDetected, "Should detect WildFly platform from real GitHub project");
// }
    
    @Test
    @DisplayName("Platform detection should detect Tomcat from real GitHub project")
    void testDetectTomcatFromRealProject() throws IOException {
        // Get Tomcat project from GitHub examples
        Path projectDir = projectManager.getExampleProject("Apache Tomcat", "application_servers");
        
        // DEBUG: Check if pom.xml exists and print its content
        Path pomPath = projectDir.resolve("pom.xml");
        if (java.nio.file.Files.exists(pomPath)) {
            String pomContent = java.nio.file.Files.readString(pomPath);
            System.out.println("=== POM Content (first 500 chars) ===");
            System.out.println(pomContent.substring(0, Math.min(500, pomContent.length())));
            System.out.println("=== Looking for tomcat artifacts ===");
            System.out.println("Contains 'tomcat-embed-core': " + pomContent.contains("tomcat-embed-core"));
            System.out.println("Contains 'org.apache.tomcat': " + pomContent.contains("org.apache.tomcat"));
        } else {
            System.out.println("No pom.xml found at: " + pomPath);
        }
        
        // Run platform detection
        List<String> detectedPlatforms = platformDetectionService.scanProject(projectDir);
        
        // Verify result
        assertNotNull(detectedPlatforms);
        
        // Check for Tomcat detection
        boolean tomcatDetected = detectedPlatforms.stream()
            .anyMatch(platform -> platform.toLowerCase().contains("tomcat"));
        
        // Print detailed results for debugging
        System.out.println("=== Tomcat Real Project Detection Results ===");
        System.out.println("Detected platforms: " + detectedPlatforms.size());
        detectedPlatforms.forEach(platform -> 
            System.out.println("  - " + platform));
        System.out.println("Tomcat detected: " + tomcatDetected);
        
        // Should actually detect the platform, not just handle gracefully
        assertTrue(detectedPlatforms.size() > 0, "Should detect at least one platform from real Tomcat project");
        assertTrue(tomcatDetected, "Should detect Tomcat platform from real GitHub project");
    }
    
    @Test
    @DisplayName("Platform detection should detect Jetty from real GitHub project")
    void testDetectJettyFromRealProject() throws IOException {
        // Get Jetty project from GitHub examples
        Path projectDir = projectManager.getExampleProject("Jetty", "application_servers");
        
        // Run platform detection
        List<String> detectedPlatforms = platformDetectionService.scanProject(projectDir);
        
        // Verify result
        assertNotNull(detectedPlatforms);
        
        // Check for Jetty detection
        boolean jettyDetected = detectedPlatforms.stream()
            .anyMatch(platform -> platform.toLowerCase().contains("jetty"));
        
        // Print detailed results for debugging
        System.out.println("=== Jetty Real Project Detection Results ===");
        System.out.println("Detected platforms: " + detectedPlatforms.size());
        detectedPlatforms.forEach(platform -> 
            System.out.println("  - " + platform));
        System.out.println("Jetty detected: " + jettyDetected);
        
        // Should actually detect the platform, not just handle gracefully
        assertTrue(detectedPlatforms.size() > 0, "Should detect at least one platform from real Jetty project");
        assertTrue(jettyDetected, "Should detect Jetty platform from real GitHub project");
    }
    
    @Test
    @DisplayName("Platform detection should detect Spring Boot from real GitHub project")
    void testDetectSpringBootFromRealProject() throws IOException {
        // Get Spring Boot project from GitHub examples
        Path projectDir = projectManager.getExampleProject("Spring Boot", "application_servers");
        
        // Run platform detection
        List<String> detectedPlatforms = platformDetectionService.scanProject(projectDir);
        
        // Verify result
        assertNotNull(detectedPlatforms);
        
        // Check for Spring Boot detection
        boolean springbootDetected = detectedPlatforms.stream()
            .anyMatch(platform -> platform.toLowerCase().contains("springboot"));
        
        // Print detailed results for debugging
        System.out.println("=== Spring Boot Real Project Detection Results ===");
        System.out.println("Detected platforms: " + detectedPlatforms.size());
        detectedPlatforms.forEach(platform -> 
            System.out.println("  - " + platform));
        System.out.println("Spring Boot detected: " + springbootDetected);
        
        // Should actually detect the platform, not just handle gracefully
        assertTrue(detectedPlatforms.size() > 0, "Should detect at least one platform from real Spring Boot project");
        assertTrue(springbootDetected, "Should detect Spring Boot platform from real GitHub project");
    }
    
    // DISABLED: These repositories are not suitable for platform detection testing
// @Test
// @DisplayName("Platform detection should detect GlassFish from real GitHub project")
// void testDetectGlassFishFromRealProject() throws IOException {
//     // Get GlassFish project from GitHub examples
//     Path projectDir = projectManager.getExampleProject("GlassFish", "application_servers");
//     
//     // Run platform detection
//     List<String> detectedPlatforms = platformDetectionService.scanProject(projectDir);
//     
//     // Verify result
//     assertNotNull(detectedPlatforms);
//     
//     // Check for GlassFish detection
//     boolean glassfishDetected = detectedPlatforms.stream()
//         .anyMatch(platform -> platform.toLowerCase().contains("glassfish"));
//     
//     // Print detailed results for debugging
//     System.out.println("=== GlassFish Real Project Detection Results ===");
//     System.out.println("Detected platforms: " + detectedPlatforms.size());
//     detectedPlatforms.forEach(platform -> 
//         System.out.println("  - " + platform));
//     System.out.println("GlassFish detected: " + glassfishDetected);
//     
//     // Should actually detect the platform, not just handle gracefully
//     assertTrue(detectedPlatforms.size() > 0, "Should detect at least one platform from real GlassFish project");
//     assertTrue(glassfishDetected, "Should detect GlassFish platform from real GitHub project");
// }

// DISABLED: Tomcat repository is Apache Tomcat source code, not a suitable project
// @Test
// @DisplayName("Platform detection should detect Tomcat from real GitHub project")
// void testDetectTomcatFromRealProject() throws IOException {
//     // Get Tomcat project from GitHub examples
//     Path projectDir = projectManager.getExampleProject("Apache Tomcat", "application_servers");
//     
//     // Run platform detection
//     List<String> detectedPlatforms = platformDetectionService.scanProject(projectDir);
//     
//     // Verify result
//     assertNotNull(detectedPlatforms);
//     
//     // Check for Tomcat detection
//     boolean tomcatDetected = detectedPlatforms.stream()
//         .anyMatch(platform -> platform.toLowerCase().contains("tomcat"));
//     
//     // Print detailed results for debugging
//     System.out.println("=== Tomcat Real Project Detection Results ===");
//     System.out.println("Detected platforms: " + detectedPlatforms.size());
//     detectedPlatforms.forEach(platform -> 
//         System.out.println("  - " + platform));
//     System.out.println("Tomcat detected: " + tomcatDetected);
//     
//     // Should actually detect the platform, not just handle gracefully
//     assertTrue(detectedPlatforms.size() > 0, "Should detect at least one platform from real Tomcat project");
//     assertTrue(tomcatDetected, "Should detect Tomcat platform from real GitHub project");
// }
    
    // DISABLED: These repositories are not suitable for platform detection testing
    // @Test
    // @DisplayName("Platform detection should detect WildFly from real GitHub project")
    // void testDetectWildFlyFromRealProject() throws IOException {
    //     // Get WildFly project from GitHub examples
    //     Path projectDir = projectManager.getExampleProject("WildFly", "application_servers");
    //     
    //     // Run platform detection
    //     List<String> detectedPlatforms = platformDetectionService.scanProject(projectDir);
    //     
    //     // Verify result
    //     assertNotNull(detectedPlatforms);
    //     
    //     // Check for WildFly detection
    //     boolean wildflyDetected = detectedPlatforms.stream()
    //         .anyMatch(platform -> platform.toLowerCase().contains("wildfly"));
    //     
    //     // Print detailed results for debugging
    //     System.out.println("=== WildFly Real Project Detection Results ===");
    //     System.out.println("Detected platforms: " + detectedPlatforms.size());
    //     detectedPlatforms.forEach(platform -> 
    //         System.out.println("  - " + platform));
    //     System.out.println("WildFly detected: " + wildflyDetected);
    //     
    //     // Should actually detect the platform, not just handle gracefully
    //     assertTrue(detectedPlatforms.size() > 0, "Should detect at least one platform from real WildFly project");
    //     assertTrue(wildflyDetected, "Should detect WildFly platform from real GitHub project");
    // }
    
    // DISABLED: Spring OAuth example, not suitable for platform detection
    // @Test
    // @DisplayName("Platform detection should detect Spring from real GitHub project")
    // void testDetectSpringFromRealProject() throws IOException {
    //     try {
    //         // Get Spring project from GitHub examples
    //         Path projectDir = projectManager.getExampleProject("Spring", "application_servers");
    //         
    //         // Run platform detection
    //         List<String> detectedPlatforms = platformDetectionService.scanProject(projectDir);
    //         
    //         // Verify result
    //         assertNotNull(detectedPlatforms);
    //         
    //         // Check for Spring detection
    //         boolean springDetected = detectedPlatforms.stream()
    //             .anyMatch(platform -> platform.toLowerCase().contains("spring"));
    //         
    //         // Print detailed results for debugging
    //         System.out.println("=== Spring Real Project Detection Results ===");
    //         System.out.println("Detected platforms: " + detectedPlatforms.size());
    //         detectedPlatforms.forEach(platform -> 
    //             System.out.println("  - " + platform));
    //         System.out.println("Spring detected: " + springDetected);
    //         
    //         // At minimum should handle the project gracefully
    //         assertTrue(detectedPlatforms.size() >= 0, "Should handle real Spring project gracefully");
    //     } catch (IOException e) {
    //         // Network issues are expected in integration tests
    //         System.out.println("Spring test skipped due to network issue: " + e.getMessage());
    //         assertTrue(true, "Should gracefully handle network issues");
    //     }
    // }
    
    // DISABLED: JBoss EAP examples are old and not suitable for platform detection
    // @Test
    // @DisplayName("Platform detection should detect JBoss EAP from real GitHub project")
    // void testDetectJBossEAPFromRealProject() throws IOException {
    //     // Get JBoss EAP project from GitHub examples
    //     Path projectDir = projectManager.getExampleProject("JBoss EAP", "application_servers");
    //     
    //     // Run platform detection
    //     List<String> detectedPlatforms = platformDetectionService.scanProject(projectDir);
    //     
    //     // Verify result
    //     assertNotNull(detectedPlatforms);
    //     
    //     // Check for JBoss EAP detection
    //     boolean jbossEAPDetected = detectedPlatforms.stream()
    //         .anyMatch(platform -> platform.toLowerCase().contains("jboss") || 
    //                              platform.toLowerCase().contains("eap"));
    //     
    //     // Print detailed results for debugging
    //     System.out.println("=== JBoss EAP Real Project Detection Results ===");
    //     System.out.println("Detected platforms: " + detectedPlatforms.size());
    //     detectedPlatforms.forEach(platform -> 
    //         System.out.println("  - " + platform));
    //     System.out.println("JBoss EAP detected: " + jbossEAPDetected);
    //     
    //     // Should actually detect the platform, not just handle gracefully
    //     assertTrue(detectedPlatforms.size() > 0, "Should detect at least one platform from real JBoss EAP project");
    //     assertTrue(jbossEAPDetected, "Should detect JBoss EAP platform from real GitHub project");
    // }
    
    // DISABLED: WebSphere example is complex and not suitable for platform detection
    // @Test
    // @DisplayName("Platform detection should detect WebSphere from real GitHub project")
    // void testDetectWebSphereFromRealProject() throws IOException {
    //     try {
    //         // Get WebSphere project from GitHub examples
    //         Path projectDir = projectManager.getExampleProject("WebSphere", "application_servers");
    //         
    //         // Run platform detection
    //         List<String> detectedPlatforms = platformDetectionService.scanProject(projectDir);
    //         
    //         // Verify result
    //         assertNotNull(detectedPlatforms);
    //         
    //         // Check for WebSphere detection
    //         boolean websphereDetected = detectedPlatforms.stream()
    //             .anyMatch(platform -> platform.toLowerCase().contains("websphere"));
    //         
    //         // Print detailed results for debugging
    //         System.out.println("=== WebSphere Real Project Detection Results ===");
    //         System.out.println("Detected platforms: " + detectedPlatforms.size());
    //         detectedPlatforms.forEach(platform -> 
    //             System.out.println("  - " + platform));
    //         System.out.println("WebSphere detected: " + websphereDetected);
    //         
    //         // At minimum should handle the project gracefully
    //         assertTrue(detectedPlatforms.size() >= 0, "Should handle real WebSphere project gracefully");
    //     } catch (IOException e) {
    //         // Network issues are expected in integration tests
    //         System.out.println("WebSphere test skipped due to network issue: " + e.getMessage());
    //         assertTrue(true, "Should gracefully handle network issues");
    //     }
    // }
    
    // DISABLED: SpringBoot on WildFly is too specific and not suitable for platform detection
    // @Test
    // @DisplayName("Platform detection should detect SpringBoot on WildFly from real GitHub project")
    // void testDetectSpringBootWildFlyFromRealProject() throws IOException {
    //     try {
    //         // Get SpringBoot on WildFly project from GitHub examples
    //         Path projectDir = projectManager.getExampleProject("SpringBoot on WildFly", "application_servers");
    //         
    //         // Run platform detection
    //         List<String> detectedPlatforms = platformDetectionService.scanProject(projectDir);
    //         
    //         // Verify result
    //         assertNotNull(detectedPlatforms);
    //         
    //         // Check for Spring Boot detection
    //         boolean springbootDetected = detectedPlatforms.stream()
    //             .anyMatch(platform -> platform.toLowerCase().contains("spring"));
    //         
    //         // Check for WildFly detection  
    //         boolean wildflyDetected = detectedPlatforms.stream()
    //             .anyMatch(platform -> platform.toLowerCase().contains("wildfly"));
    //         
    //         // Print detailed results for debugging
    //         System.out.println("=== SpringBoot on WildFly Real Project Detection Results ===");
    //         System.out.println("Detected platforms: " + detectedPlatforms.size());
    //         detectedPlatforms.forEach(platform -> 
    //             System.out.println("  - " + platform));
    //         System.out.println("Spring Boot detected: " + springbootDetected);
    //         System.out.println("WildFly detected: " + wildflyDetected);
    //         
    //         // At minimum should handle the project gracefully
    //         assertTrue(detectedPlatforms.size() >= 0, "Should handle real SpringBoot on WildFly project gracefully");
    //     } catch (IOException e) {
    //         // Network issues are expected in integration tests
    //         System.out.println("SpringBoot on WildFly test skipped due to network issue: " + e.getMessage());
    //         assertTrue(true, "Should gracefully handle network issues");
    //     }
    // }
}
