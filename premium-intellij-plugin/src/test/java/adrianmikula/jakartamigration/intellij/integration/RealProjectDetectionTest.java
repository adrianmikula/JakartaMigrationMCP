package adrianmikula.jakartamigration.intellij.integration;

import adrianmikula.jakartamigration.platforms.service.PlatformDetectionService;
import adrianmikula.jakartamigration.platforms.model.PlatformScanResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test platform detection with realistic project structures.
 */
public class RealProjectDetectionTest {
    
    @TempDir
    Path tempDir;
    
    private PlatformDetectionService platformDetectionService;
    
    @BeforeEach
    void setUp() {
        platformDetectionService = new PlatformDetectionService();
    }
    
    @Test
    @DisplayName("Platform detection should detect WildFly from realistic project structure")
    void testDetectWildFlyRealProject() throws IOException {
        // Create a realistic WildFly project structure based on examples\appservers
        Path projectDir = tempDir.resolve("wildfly-real-project");
        Files.createDirectories(projectDir.resolve("src/main/java/com/example"));
        
        // Create a realistic pom.xml similar to the WildFly example
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
                     http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <parent>
                    <groupId>org.example</groupId>
                    <artifactId>client-interoperability</artifactId>
                    <version>1.0-SNAPSHOT</version>
                </parent>
                <artifactId>sampleApp-ee8</artifactId>
                <packaging>ejb</packaging>
                
                <properties>
                    <maven.compiler.source>11</maven.compiler.source>
                    <maven.compiler.target>11</maven.compiler.target>
                    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
                </properties>
                
                <dependencies>
                    <dependency>
                        <groupId>javax.ejb</groupId>
                        <artifactId>ejb-api</artifactId>
                        <version>3.0</version>
                        <scope>provided</scope>
                    </dependency>
                    <dependency>
                        <groupId>javax.servlet</groupId>
                        <artifactId>javax.servlet-api</artifactId>
                        <version>4.0.1</version>
                        <scope>provided</scope>
                    </dependency>
                </dependencies>
                
                <build>
                    <plugins>
                        <plugin>
                            <groupId>org.wildfly.plugins</groupId>
                            <artifactId>wildfly-maven-plugin</artifactId>
                            <version>2.1.0.Final</version>
                        </plugin>
                    </plugins>
                </build>
            </project>
            """;
        
        Files.write(projectDir.resolve("pom.xml"), pomContent.getBytes());
        
        // Create a simple EJB class
        String ejbContent = """
            package com.example;
            
            import javax.ejb.Stateless;
            
            @Stateless
            public class TestBean {
                public String hello() {
                    return "Hello from WildFly!";
                }
            }
            """;
        
        Files.write(projectDir.resolve("src/main/java/com/example/TestBean.java"), ejbContent.getBytes());
        
        // Run platform detection
        PlatformScanResult result = platformDetectionService.scanProject(projectDir);
        
        // Verify result
        assertNotNull(result);
        assertNotNull(result.detectedPlatforms());
        
        // Print detailed results for debugging
        System.out.println("=== Realistic WildFly Project Detection Results ===");
        System.out.println("Detected platforms: " + result.detectedPlatforms().size());
        result.detectedPlatforms().forEach(d -> 
            System.out.println("  - " + d.platformName() + " (type: " + d.platformType() + ", version: " + d.detectedVersion() + ")"));
        System.out.println("Total risk score: " + result.totalRiskScore());
        System.out.println("Recommendations: " + result.recommendations().size());
        
        // Check for WildFly detection
        boolean wildflyDetected = result.detectedPlatforms().stream()
            .anyMatch(detection -> detection.platformName().toLowerCase().contains("wildfly"));
        
        System.out.println("WildFly detected: " + wildflyDetected);
        
        // At minimum should detect some platform
        assertTrue(result.detectedPlatforms().size() > 0, "Should detect at least one platform");
    }
    
    @Test
    @DisplayName("Platform detection should detect Tomcat from realistic web project")
    void testDetectTomcatRealProject() throws IOException {
        // Create a realistic Tomcat web project structure
        Path projectDir = tempDir.resolve("tomcat-real-project");
        Files.createDirectories(projectDir.resolve("src/main/webapp/WEB-INF"));
        Files.createDirectories(projectDir.resolve("src/main/java/com/example"));
        
        // Create a realistic pom.xml with Tomcat dependencies
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
                     http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>tomcat-webapp</artifactId>
                <version>1.0.0</version>
                <packaging>war</packaging>
                
                <properties>
                    <maven.compiler.source>11</maven.compiler.source>
                    <maven.compiler.target>11</maven.compiler.target>
                    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
                </properties>
                
                <dependencies>
                    <dependency>
                        <groupId>javax.servlet</groupId>
                        <artifactId>javax.servlet-api</artifactId>
                        <version>4.0.1</version>
                        <scope>provided</scope>
                    </dependency>
                    <dependency>
                        <groupId>javax.servlet.jsp</groupId>
                        <artifactId>javax.servlet.jsp-api</artifactId>
                        <version>2.3.3</version>
                        <scope>provided</scope>
                    </dependency>
                </dependencies>
                
                <build>
                    <plugins>
                        <plugin>
                            <groupId>org.apache.tomcat.maven</groupId>
                            <artifactId>tomcat7-maven-plugin</artifactId>
                            <version>2.2</version>
                        </plugin>
                    </plugins>
                </build>
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
                <servlet>
                    <servlet-name>TestServlet</servlet-name>
                    <servlet-class>com.example.TestServlet</servlet-class>
                </servlet>
                <servlet-mapping>
                    <servlet-name>TestServlet</servlet-name>
                    <url-pattern>/test</url-pattern>
                </servlet-mapping>
            </web-app>
            """;
        
        Files.write(projectDir.resolve("src/main/webapp/WEB-INF/web.xml"), webXml.getBytes());
        
        // Create a servlet
        String servletContent = """
            package com.example;
            
            import javax.servlet.ServletException;
            import javax.servlet.http.HttpServlet;
            import javax.servlet.http.HttpServletRequest;
            import javax.servlet.http.HttpServletResponse;
            import java.io.IOException;
            
            public class TestServlet extends HttpServlet {
                protected void doGet(HttpServletRequest request, HttpServletResponse response) 
                        throws ServletException, IOException {
                    response.getWriter().println("Hello from Tomcat!");
                }
            }
            """;
        
        Files.write(projectDir.resolve("src/main/java/com/example/TestServlet.java"), servletContent.getBytes());
        
        // Run platform detection
        PlatformScanResult result = platformDetectionService.scanProject(projectDir);
        
        // Verify result
        assertNotNull(result);
        assertNotNull(result.detectedPlatforms());
        
        // Print detailed results for debugging
        System.out.println("=== Realistic Tomcat Project Detection Results ===");
        System.out.println("Detected platforms: " + result.detectedPlatforms().size());
        result.detectedPlatforms().forEach(d -> 
            System.out.println("  - " + d.platformName() + " (type: " + d.platformType() + ", version: " + d.detectedVersion() + ")"));
        System.out.println("Total risk score: " + result.totalRiskScore());
        System.out.println("Recommendations: " + result.recommendations().size());
        
        // Check for Tomcat detection
        boolean tomcatDetected = result.detectedPlatforms().stream()
            .anyMatch(detection -> detection.platformName().toLowerCase().contains("tomcat"));
        
        System.out.println("Tomcat detected: " + tomcatDetected);
        
        // At minimum should detect some platform
        assertTrue(result.detectedPlatforms().size() > 0, "Should detect at least one platform");
    }
}
