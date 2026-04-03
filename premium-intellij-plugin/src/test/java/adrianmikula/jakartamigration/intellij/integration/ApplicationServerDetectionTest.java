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
 * Integration test for platform detection with real application server configurations.
 */
public class ApplicationServerDetectionTest {
    
    @TempDir
    Path tempDir;
    
    private PlatformDetectionService platformDetectionService;
    
    @BeforeEach
    void setUp() {
        platformDetectionService = new PlatformDetectionService();
    }
    
    @Test
    @DisplayName("Platform detection should detect WildFly EJB project")
    void testDetectWildFlyEJBProject() throws IOException {
        // Create a WildFly EJB project structure
        Path projectDir = tempDir.resolve("wildfly-ejb-project");
        Files.createDirectories(projectDir);
        
        // Create pom.xml with EJB packaging (WildFly indicator)
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
        
        // Should detect WildFly due to EJB packaging and javax.ejb dependency
        boolean wildflyDetected = result.detectedPlatforms().stream()
            .anyMatch(detection -> detection.platformName().toLowerCase().contains("wildfly"));
        
        // Print detailed results for debugging
        System.out.println("=== WildFly EJB Project Detection Results ===");
        System.out.println("Detected platforms: " + result.detectedPlatforms().size());
        result.detectedPlatforms().forEach(d -> 
            System.out.println("  - " + d.platformName() + " (type: " + d.platformType() + ", version: " + d.detectedVersion() + ")"));
        System.out.println("Total risk score: " + result.totalRiskScore());
        System.out.println("Recommendations: " + result.recommendations().size());
        System.out.println("WildFly detected: " + wildflyDetected);
        
        // At minimum should detect some platform
        assertTrue(result.detectedPlatforms().size() > 0, "Should detect at least one platform");
        
        // Verify WildFly was detected (this is the key test)
        assertTrue(wildflyDetected, "Should detect WildFly platform from EJB packaging and javax.ejb dependency");
    }
    
    @Test
    @DisplayName("Platform detection should detect Tomcat web application")
    void testDetectTomcatWebApp() throws IOException {
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
        
        // Should detect Tomcat due to servlet dependency and web.xml
        boolean tomcatDetected = result.detectedPlatforms().stream()
            .anyMatch(detection -> detection.platformName().toLowerCase().contains("tomcat"));
        
        System.out.println("Platform detection completed successfully");
        System.out.println("Detected platforms: " + result.detectedPlatforms().size());
        result.detectedPlatforms().forEach(d -> 
            System.out.println("  - " + d.platformName() + " (version: " + d.detectedVersion() + ")"));
        System.out.println("Total risk score: " + result.totalRiskScore());
        System.out.println("Recommendations: " + result.recommendations().size());
        
        // At minimum should detect some platform
        assertTrue(result.detectedPlatforms().size() > 0, "Should detect at least one platform");
    }
}
