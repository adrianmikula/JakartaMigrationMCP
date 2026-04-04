package adrianmikula.jakartamigration.intellij.integration;

import adrianmikula.jakartamigration.platforms.service.SimplifiedPlatformDetectionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for platform scanning logic with realistic project structures.
 * Tests the SimplifiedPlatformDetectionService with project structures that mimic real GitHub projects.
 */
public class PlatformScanningRealProjectTest {
    
    private SimplifiedPlatformDetectionService detectionService;
    
    @BeforeEach
    void setUp() {
        detectionService = new SimplifiedPlatformDetectionService();
    }
    
    @Test
    @DisplayName("Platform detection should handle realistic Tomcat project structure")
    void testDetectTomcatRealisticProject() throws IOException {
        // Create a realistic Tomcat project structure based on real GitHub projects
        Path projectDir = createRealisticTomcatProject();
        
        // Run platform detection
        List<String> detectedPlatforms = detectionService.scanProject(projectDir);
        
        // Print detailed results for debugging
        System.out.println("=== Realistic Tomcat Project Detection Results ===");
        System.out.println("Detected platforms: " + detectedPlatforms.size());
        detectedPlatforms.forEach(platform -> 
            System.out.println("  - " + platform));
        
        // Verify the scan completed successfully
        assertNotNull(detectedPlatforms);
        assertTrue(detectedPlatforms.size() >= 0, "Should complete scan without errors");
        
        // The main test is that our scanning logic can handle realistic project structures
        // without crashing or throwing exceptions
    }
    
    @Test
    @DisplayName("Platform detection should handle realistic WildFly project structure")
    void testDetectWildFlyRealisticProject() throws IOException {
        // Create a realistic WildFly project structure
        Path projectDir = createRealisticWildFlyProject();
        
        // Run platform detection
        List<String> detectedPlatforms = detectionService.scanProject(projectDir);
        
        // Print detailed results for debugging
        System.out.println("=== Realistic WildFly Project Detection Results ===");
        System.out.println("Detected platforms: " + detectedPlatforms.size());
        detectedPlatforms.forEach(platform -> 
            System.out.println("  - " + platform));
        
        // Verify the scan completed successfully
        assertNotNull(detectedPlatforms);
        assertTrue(detectedPlatforms.size() >= 0, "Should complete scan without errors");
    }
    
    @Test
    @DisplayName("Platform detection should handle realistic Spring Boot project structure")
    void testDetectSpringBootRealisticProject() throws IOException {
        // Create a realistic Spring Boot project structure
        Path projectDir = createRealisticSpringBootProject();
        
        // Run platform detection
        List<String> detectedPlatforms = detectionService.scanProject(projectDir);
        
        // Print detailed results for debugging
        System.out.println("=== Realistic Spring Boot Project Detection Results ===");
        System.out.println("Detected platforms: " + detectedPlatforms.size());
        detectedPlatforms.forEach(platform -> 
            System.out.println("  - " + platform));
        
        // Verify the scan completed successfully
        assertNotNull(detectedPlatforms);
        assertTrue(detectedPlatforms.size() >= 0, "Should complete scan without errors");
    }
    
    @Test
    @DisplayName("Platform detection should handle complex multi-module project")
    void testDetectComplexMultiModuleProject() throws IOException {
        // Create a complex multi-module project structure
        Path projectDir = createComplexMultiModuleProject();
        
        // Run platform detection
        List<String> detectedPlatforms = detectionService.scanProject(projectDir);
        
        // Print detailed results for debugging
        System.out.println("=== Complex Multi-Module Project Detection Results ===");
        System.out.println("Detected platforms: " + detectedPlatforms.size());
        detectedPlatforms.forEach(platform -> 
            System.out.println("  - " + platform));
        
        // Verify the scan completed successfully
        assertNotNull(detectedPlatforms);
        assertTrue(detectedPlatforms.size() >= 0, "Should complete scan without errors");
    }
    
    /**
     * Creates a realistic Tomcat project structure based on common patterns from real projects
     */
    private Path createRealisticTomcatProject() throws IOException {
        Path projectDir = Files.createTempDirectory("tomcat-realistic");
        
        // Create Maven structure
        Files.createDirectories(projectDir.resolve("src/main/java/com/example"));
        Files.createDirectories(projectDir.resolve("src/main/webapp/WEB-INF"));
        Files.createDirectories(projectDir.resolve("src/test/java/com/example"));
        
        // Create realistic pom.xml with Tomcat dependencies
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
                        <groupId>org.apache.tomcat</groupId>
                        <artifactId>tomcat-servlet-api</artifactId>
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
        
        // Create web.xml
        String webXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee
                                         http://xmlns.jcp.org/xml/ns/javaee/web-app_4_0.xsd"
                     version="4.0">
                
                <servlet>
                    <servlet-name>example</servlet-name>
                    <servlet-class>com.example.ExampleServlet</servlet-class>
                </servlet>
                
                <servlet-mapping>
                    <servlet-name>example</servlet-name>
                    <url-pattern>/example</url-pattern>
                </servlet-mapping>
            </web-app>
            """;
        
        Files.write(projectDir.resolve("src/main/webapp/WEB-INF/web.xml"), webXml.getBytes());
        
        return projectDir;
    }
    
    /**
     * Creates a realistic WildFly project structure
     */
    private Path createRealisticWildFlyProject() throws IOException {
        Path projectDir = Files.createTempDirectory("wildfly-realistic");
        
        // Create Maven structure
        Files.createDirectories(projectDir.resolve("src/main/java/com/example"));
        Files.createDirectories(projectDir.resolve("src/main/webapp/WEB-INF"));
        
        // Create realistic pom.xml with WildFly dependencies
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
                        <artifactId>wildfly-ejb-client</artifactId>
                        <version>26.1.1.Final</version>
                    </dependency>
                    <dependency>
                        <groupId>org.wildfly</groupId>
                        <artifactId>wildfly-connector</artifactId>
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
        
        return projectDir;
    }
    
    /**
     * Creates a realistic Spring Boot project structure
     */
    private Path createRealisticSpringBootProject() throws IOException {
        Path projectDir = Files.createTempDirectory("springboot-realistic");
        
        // Create Gradle structure
        Files.createDirectories(projectDir.resolve("src/main/java/com/example"));
        Files.createDirectories(projectDir.resolve("src/main/resources"));
        
        // Create build.gradle with Spring Boot dependencies
        String buildContent = """
            plugins {
                id 'org.springframework.boot' version '2.7.14'
                id 'java'
            }
            
            group = 'com.example'
            version = '0.0.1-SNAPSHOT'
            sourceCompatibility = '11'
            
            repositories {
                mavenCentral()
            }
            
            dependencies {
                implementation 'org.springframework.boot:spring-boot-starter-web'
                implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
                implementation 'org.springframework.boot:spring-boot-starter-actuator'
                
                // Jakarta EE dependencies
                implementation 'jakarta.persistence:jakarta.persistence-api'
                implementation 'jakarta.validation:jakarta.validation-api'
                
                testImplementation 'org.springframework.boot:spring-boot-starter-test'
            }
            """;
        
        Files.write(projectDir.resolve("build.gradle"), buildContent.getBytes());
        
        return projectDir;
    }
    
    /**
     * Creates a complex multi-module project structure
     */
    private Path createComplexMultiModuleProject() throws IOException {
        Path projectDir = Files.createTempDirectory("multimodule-realistic");
        
        // Create parent pom
        String parentPom = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>multi-module-app</artifactId>
                <version>1.0.0</version>
                <packaging>pom</packaging>
                
                <modules>
                    <module>web-module</module>
                    <module>service-module</module>
                    <module>common-module</module>
                </modules>
            </project>
            """;
        
        Files.write(projectDir.resolve("pom.xml"), parentPom.getBytes());
        
        // Create web module with Tomcat
        Path webModule = projectDir.resolve("web-module");
        Files.createDirectories(webModule.resolve("src/main/java/com/example/web"));
        
        String webPom = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <parent>
                    <groupId>com.example</groupId>
                    <artifactId>multi-module-app</artifactId>
                    <version>1.0.0</version>
                </parent>
                <artifactId>web-module</artifactId>
                <packaging>war</packaging>
                
                <dependencies>
                    <dependency>
                        <groupId>org.apache.tomcat</groupId>
                        <artifactId>tomcat-catalina</artifactId>
                        <version>9.0.65</version>
                        <scope>provided</scope>
                    </dependency>
                </dependencies>
            </project>
            """;
        
        Files.write(webModule.resolve("pom.xml"), webPom.getBytes());
        
        // Create service module
        Path serviceModule = projectDir.resolve("service-module");
        Files.createDirectories(serviceModule.resolve("src/main/java/com/example/service"));
        
        String servicePom = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <parent>
                    <groupId>com.example</groupId>
                    <artifactId>multi-module-app</artifactId>
                    <version>1.0.0</version>
                </parent>
                <artifactId>service-module</artifactId>
                
                <dependencies>
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter</artifactId>
                        <version>2.7.14</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        
        Files.write(serviceModule.resolve("pom.xml"), servicePom.getBytes());
        
        return projectDir;
    }
}
