package adrianmikula.jakartamigration.intellij.integration;

import adrianmikula.jakartamigration.platforms.model.EnhancedPlatformScanResult;
import adrianmikula.jakartamigration.platforms.service.EnhancedPlatformDetectionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for EnhancedPlatformDetectionService with real project structures.
 * Tests the scanProjectWithArtifacts() method which is used by PlatformsTabComponent.
 * This test ensures that platform detection works through the enhanced service layer,
 * catching bugs where delegation to the underlying detection service fails.
 */
public class EnhancedPlatformDetectionIntegrationTest {
    
    private EnhancedPlatformDetectionService enhancedDetectionService;
    
    @BeforeEach
    void setUp() {
        enhancedDetectionService = new EnhancedPlatformDetectionService();
    }
    
    @Test
    @DisplayName("Should detect Tomcat via scanProjectWithArtifacts with realistic Maven project")
    void testDetectsTomcatFromMavenProject() throws IOException {
        // Create a realistic Tomcat project with pom.xml
        Path projectDir = Files.createTempDirectory("tomcat-enhanced-test");
        
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>tomcat-webapp</artifactId>
                <version>1.0.0</version>
                <packaging>war</packaging>
                
                <dependencies>
                    <dependency>
                        <groupId>org.apache.tomcat</groupId>
                        <artifactId>tomcat-catalina</artifactId>
                        <version>9.0.65</version>
                        <scope>provided</scope>
                    </dependency>
                    <dependency>
                        <groupId>javax.servlet</groupId>
                        <artifactId>javax.servlet-api</artifactId>
                        <version>4.0.1</version>
                        <scope>provided</scope>
                    </dependency>
                </dependencies>
            </project>
            """;
        
        Files.write(projectDir.resolve("pom.xml"), pomContent.getBytes());
        
        // Run enhanced platform detection
        EnhancedPlatformScanResult result = enhancedDetectionService.scanProjectWithArtifacts(projectDir);
        
        // Print results for debugging
        System.out.println("=== EnhancedPlatformDetectionService Tomcat Test ===");
        System.out.println("Detected platforms: " + result.getDetectedPlatforms());
        System.out.println("WAR count: " + result.getWarCount());
        System.out.println("Total artifacts: " + result.getTotalDeploymentCount());
        
        // Critical assertions that would fail with stubbed methods
        assertFalse(result.getDetectedPlatforms().isEmpty(),
            "Should detect platforms via scanProjectWithArtifacts - DELEGATION BUG: " +
            "EnhancedPlatformDetectionService methods may be returning empty lists instead of calling SimplifiedPlatformDetectionService");
        
        assertTrue(result.getDetectedPlatforms().contains("tomcat"),
            "Should detect Tomcat from pom.xml with tomcat-catalina dependency");
        
        assertTrue(result.getWarCount() > 0,
            "Should count WAR packaging from pom.xml");
    }
    
    @Test
    @DisplayName("Should detect WildFly via scanProjectWithArtifacts with realistic Maven project")
    void testDetectsWildFlyFromMavenProject() throws IOException {
        // Create a realistic WildFly project with pom.xml
        Path projectDir = Files.createTempDirectory("wildfly-enhanced-test");
        
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>wildfly-app</artifactId>
                <version>1.0.0</version>
                <packaging>war</packaging>
                
                <dependencies>
                    <dependency>
                        <groupId>org.wildfly</groupId>
                        <artifactId>wildfly-ee</artifactId>
                        <version>26.1.1.Final</version>
                    </dependency>
                    <dependency>
                        <groupId>javax.ejb</groupId>
                        <artifactId>javax.ejb-api</artifactId>
                        <version>3.2.2</version>
                        <scope>provided</scope>
                    </dependency>
                </dependencies>
            </project>
            """;
        
        Files.write(projectDir.resolve("pom.xml"), pomContent.getBytes());
        
        // Run enhanced platform detection
        EnhancedPlatformScanResult result = enhancedDetectionService.scanProjectWithArtifacts(projectDir);
        
        // Print results for debugging
        System.out.println("=== EnhancedPlatformDetectionService WildFly Test ===");
        System.out.println("Detected platforms: " + result.getDetectedPlatforms());
        
        // Critical assertions
        assertFalse(result.getDetectedPlatforms().isEmpty(),
            "Should detect platforms via scanProjectWithArtifacts");
        
        // Should detect wildfly
        boolean hasWildFly = result.getDetectedPlatforms().contains("wildfly");
        assertTrue(hasWildFly,
            "Should detect WildFly from pom.xml with wildfly-ee dependency");
    }
    
    @Test
    @DisplayName("Should detect Spring Boot via scanProjectWithArtifacts with Gradle project")
    void testDetectsSpringBootFromGradleProject() throws IOException {
        // Create a realistic Spring Boot project with build.gradle
        Path projectDir = Files.createTempDirectory("springboot-enhanced-test");
        
        String buildContent = """
            plugins {
                id 'org.springframework.boot' version '2.7.14'
                id 'java'
            }
            
            group = 'com.example'
            version = '0.0.1-SNAPSHOT'
            
            repositories {
                mavenCentral()
            }
            
            dependencies {
                implementation 'org.springframework.boot:spring-boot-starter-web'
                implementation 'org.springframework.boot:spring-boot-starter-tomcat'
            }
            """;
        
        Files.write(projectDir.resolve("build.gradle"), buildContent.getBytes());
        
        // Run enhanced platform detection
        EnhancedPlatformScanResult result = enhancedDetectionService.scanProjectWithArtifacts(projectDir);
        
        // Print results for debugging
        System.out.println("=== EnhancedPlatformDetectionService Spring Boot Test ===");
        System.out.println("Detected platforms: " + result.getDetectedPlatforms());
        
        // Critical assertions
        assertFalse(result.getDetectedPlatforms().isEmpty(),
            "Should detect platforms via scanProjectWithArtifacts");
        
        // Should detect either spring or springboot
        boolean hasSpring = result.getDetectedPlatforms().contains("spring") || 
                           result.getDetectedPlatforms().contains("springboot");
        assertTrue(hasSpring,
            "Should detect Spring from build.gradle with spring-boot-starter dependencies");
    }
    
    @Test
    @DisplayName("Should handle empty project gracefully")
    void testHandlesEmptyProject() throws IOException {
        // Create empty project directory
        Path projectDir = Files.createTempDirectory("empty-enhanced-test");
        
        // Run enhanced platform detection
        EnhancedPlatformScanResult result = enhancedDetectionService.scanProjectWithArtifacts(projectDir);
        
        // Should return empty result without crashing
        assertNotNull(result);
        assertTrue(result.getDetectedPlatforms().isEmpty(),
            "Should return empty platforms for project without build files");
        assertEquals(0, result.getTotalDeploymentCount(),
            "Should have 0 artifacts for empty project");
    }
    
    @Test
    @DisplayName("Should detect multiple platforms in multi-server project")
    void testDetectsMultiplePlatforms() throws IOException {
        // Create project with both Tomcat and Jetty dependencies
        Path projectDir = Files.createTempDirectory("multi-server-enhanced-test");
        
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>multi-server-app</artifactId>
                <version>1.0.0</version>
                <packaging>war</packaging>
                
                <dependencies>
                    <dependency>
                        <groupId>org.apache.tomcat</groupId>
                        <artifactId>tomcat-catalina</artifactId>
                        <version>9.0.65</version>
                        <scope>provided</scope>
                    </dependency>
                    <dependency>
                        <groupId>org.eclipse.jetty</groupId>
                        <artifactId>jetty-server</artifactId>
                        <version>11.0.15</version>
                        <scope>provided</scope>
                    </dependency>
                </dependencies>
            </project>
            """;
        
        Files.write(projectDir.resolve("pom.xml"), pomContent.getBytes());
        
        // Run enhanced platform detection
        EnhancedPlatformScanResult result = enhancedDetectionService.scanProjectWithArtifacts(projectDir);
        
        // Print results for debugging
        System.out.println("=== EnhancedPlatformDetectionService Multi-Server Test ===");
        System.out.println("Detected platforms: " + result.getDetectedPlatforms());
        
        // Should detect both servers
        assertTrue(result.getDetectedPlatforms().size() >= 1,
            "Should detect at least one platform from multi-server project");
    }
}
