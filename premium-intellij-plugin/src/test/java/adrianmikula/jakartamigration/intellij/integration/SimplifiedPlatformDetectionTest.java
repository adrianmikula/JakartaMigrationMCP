package adrianmikula.jakartamigration.intellij.integration;

import adrianmikula.jakartamigration.platforms.service.SimplifiedPlatformDetectionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for simplified platform detection service
 */
public class SimplifiedPlatformDetectionTest {
    
    @TempDir
    Path tempDir;
    
    @Test
    @DisplayName("Should detect WildFly from EJB project")
    void testDetectWildFly() throws IOException {
        // Create WildFly project
        Path projectDir = tempDir.resolve("wildfly-project");
        Files.createDirectories(projectDir);
        
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
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
        
        SimplifiedPlatformDetectionService service = new SimplifiedPlatformDetectionService();
        List<String> detectedServers = service.scanProject(projectDir);
        
        // Debug output to see what we actually detect
        System.out.println("Detected servers: " + detectedServers);
        
        assertTrue(detectedServers.contains("wildfly"), "Should detect wildfly (detected: " + detectedServers + ")");
    }
    
    @Test
    @DisplayName("Should detect Tomcat from WAR project")
    void testDetectTomcat() throws IOException {
        // Create Tomcat project
        Path projectDir = tempDir.resolve("tomcat-project");
        Files.createDirectories(projectDir);
        
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
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
        
        SimplifiedPlatformDetectionService service = new SimplifiedPlatformDetectionService();
        List<String> detectedServers = service.scanProject(projectDir);
        
        assertTrue(detectedServers.contains("tomcat"), "Should detect tomcat");
    }
    
    @Test
    @DisplayName("Should detect Jetty from dependency")
    void testDetectJetty() throws IOException {
        // Create Jetty project
        Path projectDir = tempDir.resolve("jetty-project");
        Files.createDirectories(projectDir);
        
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>jetty-test</artifactId>
                <version>1.0.0</version>
                
                <dependencies>
                    <dependency>
                        <groupId>org.eclipse.jetty</groupId>
                        <artifactId>jetty-server</artifactId>
                        <version>11.0.0</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        
        Files.write(projectDir.resolve("pom.xml"), pomContent.getBytes());
        
        SimplifiedPlatformDetectionService service = new SimplifiedPlatformDetectionService();
        List<String> detectedServers = service.scanProject(projectDir);
        
        assertTrue(detectedServers.contains("jetty"), "Should detect jetty");
    }
    
    @Test
    @DisplayName("Should return empty list for non-server project")
    void testNoServerDetection() throws IOException {
        // Create plain Java project
        Path projectDir = tempDir.resolve("plain-project");
        Files.createDirectories(projectDir);
        
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>plain-test</artifactId>
                <version>1.0.0</version>
                
                <dependencies>
                    <dependency>
                        <groupId>org.apache.commons</groupId>
                        <artifactId>commons-lang3</artifactId>
                        <version>3.12.0</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        
        Files.write(projectDir.resolve("pom.xml"), pomContent.getBytes());
        
        SimplifiedPlatformDetectionService service = new SimplifiedPlatformDetectionService();
        List<String> detectedServers = service.scanProject(projectDir);
        
        assertTrue(detectedServers.isEmpty(), "Should not detect any servers");
    }
}
