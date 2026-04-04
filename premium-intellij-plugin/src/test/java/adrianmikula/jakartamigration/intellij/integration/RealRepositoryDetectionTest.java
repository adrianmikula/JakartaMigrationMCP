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
 * Test platform detection with structures that match real repositories.
 */
public class RealRepositoryDetectionTest {
    
    @TempDir
    Path tempDir;
    
    private PlatformDetectionService platformDetectionService;
    
    @BeforeEach
    void setUp() {
        platformDetectionService = new PlatformDetectionService();
    }
    
    @Test
    @DisplayName("Platform detection should detect WildFly from real repository structure")
    void testDetectWildFlyFromRealRepository() throws IOException {
        // Create a structure similar to wildfly-client-interoperability-master
        Path projectDir = tempDir.resolve("wildfly-project");
        Files.createDirectories(projectDir.resolve("sampleApp-ee8/src/main/java/com/example"));
        
        // Create parent pom.xml
        String parentPom = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
                     http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.example</groupId>
                <artifactId>client-interoperability</artifactId>
                <version>1.0-SNAPSHOT</version>
                <packaging>pom</packaging>
            </project>
            """;
        
        Files.write(projectDir.resolve("pom.xml"), parentPom.getBytes());
        
        // Create EJB module pom.xml
        Path ejbModule = projectDir.resolve("sampleApp-ee8");
        Files.createDirectories(ejbModule.resolve("src/main/java/com/example"));
        
        String ejbPom = """
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
            </project>
            """;
        
        Files.write(ejbModule.resolve("pom.xml"), ejbPom.getBytes());
        
        // Create a simple EJB class
        String ejbClass = """
            package com.example;
            
            import javax.ejb.Stateless;
            import javax.ejb.LocalBean;
            
            @Stateless
            @LocalBean
            public class TestService {
                public String getMessage() {
                    return "Hello from WildFly EJB!";
                }
            }
            """;
        
        Files.write(ejbModule.resolve("src/main/java/com/example/TestService.java"), ejbClass.getBytes());
        
        // Create jboss-web.xml deployment descriptor for reliable detection
        String jbossWebXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <jboss-web>
                <context-root>/sampleApp-ee8</context-root>
                <security-domain>other</security-domain>
            </jboss-web>
            """;
        
        Files.createDirectories(ejbModule.resolve("src/main/webapp/WEB-INF"));
        Files.write(ejbModule.resolve("src/main/webapp/WEB-INF/jboss-web.xml"), jbossWebXml.getBytes());
        
        // Run platform detection on EJB module (this is what would be scanned in reality)
        PlatformScanResult result = platformDetectionService.scanProject(ejbModule);
        
        // Verify result
        assertNotNull(result);
        assertNotNull(result.detectedPlatforms());
        
        // Print detailed results
        System.out.println("=== Real Repository WildFly Detection Results ===");
        System.out.println("Detected platforms: " + result.detectedPlatforms().size());
        result.detectedPlatforms().forEach(d -> 
            System.out.println("  - " + d.platformName() + " (type: " + d.platformType() + ", version: " + d.detectedVersion() + ")"));
        System.out.println("Total risk score: " + result.totalRiskScore());
        System.out.println("Recommendations: " + result.recommendations().size());
        
        // Check for WildFly detection
        boolean wildflyDetected = result.detectedPlatforms().stream()
            .anyMatch(detection -> detection.platformName().toLowerCase().contains("wildfly"));
        
        System.out.println("WildFly detected: " + wildflyDetected);
        
        // Should detect WildFly due to EJB packaging and javax.ejb dependency
        assertTrue(wildflyDetected, "Should detect WildFly platform from real repository structure");
        assertTrue(result.detectedPlatforms().size() > 0, "Should detect at least one platform");
    }
    
    @Test
    @DisplayName("Platform detection should detect Tomcat from real web application")
    void testDetectTomcatFromRealRepository() throws IOException {
        // Create a structure similar to a real Tomcat web application
        Path projectDir = tempDir.resolve("tomcat-webapp");
        Files.createDirectories(projectDir.resolve("src/main/webapp/WEB-INF"));
        Files.createDirectories(projectDir.resolve("src/main/java/com/example/web"));
        
        // Create pom.xml for Tomcat web application
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
                    <dependency>
                        <groupId>javax.servlet.jsp.jstl</groupId>
                        <artifactId>javax.servlet.jsp.jstl-api</artifactId>
                        <version>1.2.2</version>
                    </dependency>
                </dependencies>
                
                <build>
                    <finalName>tomcat-webapp</finalName>
                    <plugins>
                        <plugin>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-war-plugin</artifactId>
                            <version>3.3.2</version>
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
                <display-name>Tomcat Web Application</display-name>
                <description>A web application for Tomcat</description>
                
                <servlet>
                    <servlet-name>HelloServlet</servlet-name>
                    <servlet-class>com.example.web.HelloServlet</servlet-class>
                </servlet>
                <servlet-mapping>
                    <servlet-name>HelloServlet</servlet-name>
                    <url-pattern>/hello</url-pattern>
                </servlet-mapping>
            </web-app>
            """;
        
        Files.write(projectDir.resolve("src/main/webapp/WEB-INF/web.xml"), webXml.getBytes());
        
        // Create a servlet
        String servletContent = """
            package com.example.web;
            
            import javax.servlet.ServletException;
            import javax.servlet.http.HttpServlet;
            import javax.servlet.http.HttpServletRequest;
            import javax.servlet.http.HttpServletResponse;
            import java.io.IOException;
            
            public class HelloServlet extends HttpServlet {
                @Override
                protected void doGet(HttpServletRequest request, HttpServletResponse response) 
                        throws ServletException, IOException {
                    response.setContentType("text/html");
                    response.getWriter().println("<h1>Hello from Tomcat!</h1>");
                }
            }
            """;
        
        Files.write(projectDir.resolve("src/main/java/com/example/web/HelloServlet.java"), servletContent.getBytes());
        
        // Run platform detection
        PlatformScanResult result = platformDetectionService.scanProject(projectDir);
        
        // Verify result
        assertNotNull(result);
        assertNotNull(result.detectedPlatforms());
        
        // Print detailed results
        System.out.println("=== Real Repository Tomcat Detection Results ===");
        System.out.println("Detected platforms: " + result.detectedPlatforms().size());
        result.detectedPlatforms().forEach(d -> 
            System.out.println("  - " + d.platformName() + " (type: " + d.platformType() + ", version: " + d.detectedVersion() + ")"));
        System.out.println("Total risk score: " + result.totalRiskScore());
        System.out.println("Recommendations: " + result.recommendations().size());
        
        // Check for Tomcat detection
        boolean tomcatDetected = result.detectedPlatforms().stream()
            .anyMatch(detection -> detection.platformName().toLowerCase().contains("tomcat"));
        
        System.out.println("Tomcat detected: " + tomcatDetected);
        
        // Should detect Tomcat due to WAR packaging and servlet dependencies
        assertTrue(tomcatDetected, "Should detect Tomcat platform from real repository structure");
        assertTrue(result.detectedPlatforms().size() > 0, "Should detect at least one platform");
    }
    
    @Test
    @DisplayName("Platform detection should detect Payara from deployment descriptors")
    void testDetectPayaraFromDeploymentDescriptors() throws IOException {
        // Create a Payara web application structure
        Path projectDir = tempDir.resolve("payara-webapp");
        Files.createDirectories(projectDir.resolve("src/main/webapp/WEB-INF"));
        Files.createDirectories(projectDir.resolve("src/main/java/com/example"));
        
        // Create pom.xml
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
                     http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>payara-webapp</artifactId>
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
        
        // Create Payara-specific deployment descriptor
        String glassfishWebXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE glassfish-web-app PUBLIC "-//GlassFish.org//DTD GlassFish Application Server 3.1 Servlet 3.0//EN" "http://glassfish.org/dtds/glassfish-web-app_3_0-1.0.dtd">
            <glassfish-web-app>
                <context-root>/payara-webapp</context-root>
                <parameter-encoding default-charset="UTF-8"/>
            </glassfish-web-app>
            """;
        
        Files.write(projectDir.resolve("src/main/webapp/WEB-INF/glassfish-web.xml"), glassfishWebXml.getBytes());
        
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
        
        // Print detailed results
        System.out.println("=== Payara Deployment Descriptor Detection Results ===");
        System.out.println("Detected platforms: " + result.detectedPlatforms().size());
        result.detectedPlatforms().forEach(d -> 
            System.out.println("  - " + d.platformName() + " (type: " + d.platformType() + ", version: " + d.detectedVersion() + ")"));
        
        // Check for Payara detection
        boolean payaraDetected = result.detectedPlatforms().stream()
            .anyMatch(detection -> detection.platformName().toLowerCase().contains("payara"));
        
        System.out.println("Payara detected: " + payaraDetected);
        
        // Should detect Payara due to glassfish-web.xml
        assertTrue(payaraDetected, "Should detect Payara platform from glassfish-web.xml deployment descriptor");
        assertTrue(result.detectedPlatforms().size() > 0, "Should detect at least one platform");
    }
    
    @Test
    @DisplayName("Platform detection should detect Jetty from deployment descriptors")
    void testDetectJettyFromDeploymentDescriptors() throws IOException {
        // Create a Jetty web application structure
        Path projectDir = tempDir.resolve("jetty-webapp");
        Files.createDirectories(projectDir.resolve("src/main/webapp/WEB-INF"));
        Files.createDirectories(projectDir.resolve("src/main/java/com/example"));
        
        // Create pom.xml
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
                     http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>jetty-webapp</artifactId>
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
        
        // Create Jetty-specific deployment descriptor
        String jettyWebXml = """
            <?xml version="1.0"?>
            <!DOCTYPE Configure PUBLIC "-//Jetty//Configure//EN" "http://www.eclipse.org/jetty/configure_9_3.dtd">
            <Configure class="org.eclipse.jetty.webapp.WebAppContext">
                <Set name="contextPath">/jetty-webapp</Set>
                <Set name="resourceBase"><SystemProperty name="jetty.base" default="."/>/webapps/jetty-webapp</Set>
            </Configure>
            """;
        
        Files.write(projectDir.resolve("src/main/webapp/WEB-INF/jetty-web.xml"), jettyWebXml.getBytes());
        
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
        
        // Print detailed results
        System.out.println("=== Jetty Deployment Descriptor Detection Results ===");
        System.out.println("Detected platforms: " + result.detectedPlatforms().size());
        result.detectedPlatforms().forEach(d -> 
            System.out.println("  - " + d.platformName() + " (type: " + d.platformType() + ", version: " + d.detectedVersion() + ")"));
        
        // Check for Jetty detection
        boolean jettyDetected = result.detectedPlatforms().stream()
            .anyMatch(detection -> detection.platformName().toLowerCase().contains("jetty"));
        
        System.out.println("Jetty detected: " + jettyDetected);
        
        // Should detect Jetty due to jetty-web.xml
        assertTrue(jettyDetected, "Should detect Jetty platform from jetty-web.xml deployment descriptor");
        assertTrue(result.detectedPlatforms().size() > 0, "Should detect at least one platform");
    }
    
    @Test
    @DisplayName("Platform detection should detect WildFly from jboss-web.xml")
    void testDetectWildFlyFromJBossDeploymentDescriptor() throws IOException {
        // Create a WildFly web application structure
        Path projectDir = tempDir.resolve("wildfly-webapp");
        Files.createDirectories(projectDir.resolve("src/main/webapp/WEB-INF"));
        Files.createDirectories(projectDir.resolve("src/main/java/com/example"));
        
        // Create pom.xml
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
                     http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>wildfly-webapp</artifactId>
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
        
        // Create WildFly-specific deployment descriptor
        String jbossWebXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <jboss-web>
                <context-root>/wildfly-webapp</context-root>
                <security-domain>other</security-domain>
                <resource-ref>
                    <res-ref-name>jdbc/TestDS</res-ref-name>
                    <jndi-name>java:/jdbc/TestDS</jndi-name>
                </resource-ref>
            </jboss-web>
            """;
        
        Files.write(projectDir.resolve("src/main/webapp/WEB-INF/jboss-web.xml"), jbossWebXml.getBytes());
        
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
        
        // Print detailed results
        System.out.println("=== WildFly JBoss Deployment Descriptor Detection Results ===");
        System.out.println("Detected platforms: " + result.detectedPlatforms().size());
        result.detectedPlatforms().forEach(d -> 
            System.out.println("  - " + d.platformName() + " (type: " + d.platformType() + ", version: " + d.detectedVersion() + ")"));
        
        // Check for WildFly detection
        boolean wildflyDetected = result.detectedPlatforms().stream()
            .anyMatch(detection -> detection.platformName().toLowerCase().contains("wildfly"));
        
        System.out.println("WildFly detected: " + wildflyDetected);
        
        // Should detect WildFly due to jboss-web.xml
        assertTrue(wildflyDetected, "Should detect WildFly platform from jboss-web.xml deployment descriptor");
        assertTrue(result.detectedPlatforms().size() > 0, "Should detect at least one platform");
    }
}
