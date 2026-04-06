package adrianmikula.jakartamigration.intellij.integration;

import adrianmikula.jakartamigration.platforms.service.PlatformDetectionService;
import adrianmikula.jakartamigration.platforms.model.PlatformScanResult;
import adrianmikula.jakartamigration.platforms.model.PlatformDetection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for platform detection with mock project structures.
 * Tests the platform detection logic without relying on external downloads.
 */
public class MockPlatformDetectionTest {
    
    @TempDir
    Path tempDir;
    
    private PlatformDetectionService platformDetectionService;
    
    @BeforeEach
    void setUp() {
        platformDetectionService = new PlatformDetectionService();
    }
    
    @Test
    @DisplayName("Platform detection should detect WildFly from mock EJB project")
    void testDetectWildFlyFromMockProject() throws IOException {
        // Create a WildFly EJB project structure
        Path projectDir = tempDir.resolve("wildfly-ejb-project");
        Files.createDirectories(projectDir);
        
        // Create pom.xml with EJB packaging and javax.ejb dependency
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
                     http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>wildfly-test</artifactId>
                <version>1.0.0</version>
                <packaging>ejb</packaging>
                
                <dependencies>
                    <dependency>
                        <groupId>javax.ejb</groupId>
                        <artifactId>ejb-api</artifactId>
                        <version>3.0</version>
                        <scope>provided</scope>
                    </dependency>
                </dependencies>
            </project>
            """;
        
        Files.write(projectDir.resolve("pom.xml"), pomContent.getBytes());
        
        // Run platform detection
        PlatformScanResult result = platformDetectionService.scanProject(projectDir);
        
        // Verify result
        assertNotNull(result);
        assertNotNull(result.detectedPlatforms());
        
        // Check for WildFly detection
        boolean wildflyDetected = result.detectedPlatforms().stream()
            .anyMatch(detection -> detection.platformName().toLowerCase().contains("wildfly"));
        
        // Print detailed results for debugging
        System.out.println("=== Mock WildFly Project Detection Results ===");
        System.out.println("Detected platforms: " + result.detectedPlatforms().size());
        result.detectedPlatforms().forEach(d -> 
            System.out.println("  - " + d.platformName() + " (type: " + d.platformType() + ", version: " + d.detectedVersion() + ")"));
        System.out.println("Total risk score: " + result.totalRiskScore());
        System.out.println("Recommendations: " + result.recommendations().size());
        System.out.println("WildFly detected: " + wildflyDetected);
        
        // At minimum should detect some platform
        assertTrue(result.detectedPlatforms().size() > 0, "Should detect at least one platform from mock WildFly project");
        
        // Verify WildFly was detected (this is the key test)
        assertTrue(wildflyDetected, "Should detect WildFly platform from EJB packaging and javax.ejb dependency");
    }
    
    @Test
    @DisplayName("Platform detection should detect Tomcat from mock web application")
    void testDetectTomcatFromMockProject() throws IOException {
        // Create a Tomcat web application structure
        Path projectDir = tempDir.resolve("tomcat-webapp");
        Files.createDirectories(projectDir.resolve("src/main/webapp/WEB-INF"));
        
        // Create pom.xml with servlet dependency
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
                     http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>tomcat-test</artifactId>
                <version>1.0.0</version>
                <packaging>war</packaging>
                
                <dependencies>
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
        
        // Create web.xml
        String webXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee
                     http://xmlns.jcp.org/xml/ns/javaee/web-app_4_0.xsd"
                     version="4.0">
            </web-app>
            """;
        
        Files.write(projectDir.resolve("src/main/webapp/WEB-INF/web.xml"), webXml.getBytes());
        
        // Run platform detection
        PlatformScanResult result = platformDetectionService.scanProject(projectDir);
        
        // Verify result
        assertNotNull(result);
        assertNotNull(result.detectedPlatforms());
        
        // Check for Tomcat detection
        boolean tomcatDetected = result.detectedPlatforms().stream()
            .anyMatch(detection -> detection.platformName().toLowerCase().contains("tomcat"));
        
        // Print detailed results for debugging
        System.out.println("=== Mock Tomcat Web App Detection Results ===");
        System.out.println("Detected platforms: " + result.detectedPlatforms().size());
        result.detectedPlatforms().forEach(d -> 
            System.out.println("  - " + d.platformName() + " (type: " + d.platformType() + ", version: " + d.detectedVersion() + ")"));
        System.out.println("Total risk score: " + result.totalRiskScore());
        System.out.println("Recommendations: " + result.recommendations().size());
        System.out.println("Tomcat detected: " + tomcatDetected);
        
        // At minimum should detect some platform
        assertTrue(result.detectedPlatforms().size() > 0, "Should detect at least one platform from mock Tomcat project");
        
        // Verify Tomcat was detected
        assertTrue(tomcatDetected, "Should detect Tomcat platform from javax.servlet dependency");
    }
    
    @Test
    @DisplayName("Platform detection should detect Jetty from mock web application")
    void testDetectJettyFromMockProject() throws IOException {
        // Create a Jetty web application structure
        Path projectDir = tempDir.resolve("jetty-webapp");
        Files.createDirectories(projectDir.resolve("src/main/webapp/WEB-INF"));
        
        // Create pom.xml with servlet dependency and Jetty artifact
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
                     http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>jetty-test</artifactId>
                <version>1.0.0</version>
                <packaging>war</packaging>
                
                <dependencies>
                    <dependency>
                        <groupId>javax.servlet</groupId>
                        <artifactId>javax.servlet-api</artifactId>
                        <version>4.0.1</version>
                        <scope>provided</scope>
                    </dependency>
                    <dependency>
                        <groupId>org.eclipse.jetty</groupId>
                        <artifactId>jetty-server</artifactId>
                        <version>11.0.0</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        
        Files.write(projectDir.resolve("pom.xml"), pomContent.getBytes());
        
        // Run platform detection
        PlatformScanResult result = platformDetectionService.scanProject(projectDir);
        
        // Verify result
        assertNotNull(result);
        assertNotNull(result.detectedPlatforms());
        
        // Check for Jetty detection
        boolean jettyDetected = result.detectedPlatforms().stream()
            .anyMatch(detection -> detection.platformName().toLowerCase().contains("jetty"));
        
        // Print detailed results for debugging
        System.out.println("=== Mock Jetty Web App Detection Results ===");
        System.out.println("Detected platforms: " + result.detectedPlatforms().size());
        result.detectedPlatforms().forEach(d -> 
            System.out.println("  - " + d.platformName() + " (type: " + d.platformType() + ", version: " + d.detectedVersion() + ")"));
        System.out.println("Total risk score: " + result.totalRiskScore());
        System.out.println("Recommendations: " + result.recommendations().size());
        System.out.println("Jetty detected: " + jettyDetected);
        
        // At minimum should detect some platform
        assertTrue(result.detectedPlatforms().size() > 0, "Should detect at least one platform from mock Jetty project");
        
        // Verify Jetty was detected
        assertTrue(jettyDetected, "Should detect Jetty platform from org.eclipse.jetty dependency");
    }
    
    @Test
    @DisplayName("Platform detection should detect Payara from mock web application")
    void testDetectPayaraFromMockProject() throws IOException {
        // Create a Payara web application structure
        Path projectDir = tempDir.resolve("payara-webapp");
        Files.createDirectories(projectDir.resolve("src/main/webapp/WEB-INF"));
        
        // Create pom.xml with servlet dependency and Payara artifact
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
                     http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>payara-test</artifactId>
                <version>1.0.0</version>
                <packaging>war</packaging>
                
                <dependencies>
                    <dependency>
                        <groupId>javax.servlet</groupId>
                        <artifactId>javax.servlet-api</artifactId>
                        <version>4.0.1</version>
                        <scope>provided</scope>
                    </dependency>
                    <dependency>
                        <groupId>fish.payara</groupId>
                        <artifactId>payara-embedded-all</artifactId>
                        <version>5.2022.1</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        
        Files.write(projectDir.resolve("pom.xml"), pomContent.getBytes());
        
        // Run platform detection
        PlatformScanResult result = platformDetectionService.scanProject(projectDir);
        
        // Verify result
        assertNotNull(result);
        assertNotNull(result.detectedPlatforms());
        
        // Check for Payara detection
        boolean payaraDetected = result.detectedPlatforms().stream()
            .anyMatch(detection -> detection.platformName().toLowerCase().contains("payara"));
        
        // Print detailed results for debugging
        System.out.println("=== Mock Payara Web App Detection Results ===");
        System.out.println("Detected platforms: " + result.detectedPlatforms().size());
        result.detectedPlatforms().forEach(d -> 
            System.out.println("  - " + d.platformName() + " (type: " + d.platformType() + ", version: " + d.detectedVersion() + ")"));
        System.out.println("Total risk score: " + result.totalRiskScore());
        System.out.println("Recommendations: " + result.recommendations().size());
        System.out.println("Payara detected: " + payaraDetected);
        
        // At minimum should detect some platform
        assertTrue(result.detectedPlatforms().size() > 0, "Should detect at least one platform from mock Payara project");
        
        // Verify Payara was detected
        assertTrue(payaraDetected, "Should detect Payara platform from fish.payara dependency");
    }
}
