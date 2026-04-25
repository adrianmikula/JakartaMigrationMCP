package adrianmikula.jakartamigration.platforms.service;

import adrianmikula.jakartamigration.platforms.service.SimplifiedPlatformDetectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SimplifiedPlatformDetectionService
 * Tests common artifact matching, variable resolution, and file-based search patterns
 */
public class SimplifiedPlatformDetectionServiceTest {
    
    private SimplifiedPlatformDetectionService detectionService;
    
    @TempDir
    private Path tempDir;
    
    @BeforeEach
    void setUp() {
        detectionService = new SimplifiedPlatformDetectionService();
    }
    
    @Test
    @DisplayName("Should detect Tomcat via common artifacts in Maven project")
    void testDetectTomcatViaCommonArtifacts_MavenProject() throws IOException {
        // Given
        Path projectPath = createMavenProjectWithTomcatArtifacts();
        
        // When
        List<String> detectedServers = detectionService.scanProject(projectPath);
        
        // Then
        assertThat(detectedServers).contains("tomcat");
        assertThat(detectedServers).hasSize(1);
    }
    
    @Test
    @DisplayName("Should detect WildFly via common artifacts in Gradle project")
    void testDetectWildFlyViaCommonArtifacts_GradleProject() throws IOException {
        // Given
        Path projectPath = createGradleProjectWithWildFlyArtifacts();
        
        // When
        List<String> detectedServers = detectionService.scanProject(projectPath);
        
        // Then
        assertThat(detectedServers).contains("wildfly");
        assertThat(detectedServers).hasSize(1);
    }
    
    @Test
    @DisplayName("Should detect Spring Boot via common artifacts")
    void testDetectSpringBootViaCommonArtifacts() throws IOException {
        // Given
        Path projectPath = createSpringBootProjectWithArtifacts();
        
        // When
        List<String> detectedServers = detectionService.scanProject(projectPath);
        
        // Then
        assertThat(detectedServers).contains("springboot");
        assertThat(detectedServers).hasSize(2); // Detects springboot and spring due to overlapping artifacts
    }
    
    @Test
    @DisplayName("Should detect Spring Boot via parent POM reference")
    void testDetectSpringBootViaParentPOM() throws IOException {
        // Given - Spring Boot project using parent POM (like the problematic project)
        Path projectPath = createSpringBootProjectWithParentPOM();
        
        // When
        List<String> detectedServers = detectionService.scanProject(projectPath);
        
        // Then
        assertThat(detectedServers).contains("springboot");
        assertThat(detectedServers).hasSize(1); // Should detect only springboot
    }
    
    @Test
    @DisplayName("Should detect Spring Boot via spring-boot-maven-plugin")
    void testDetectSpringBootViaMavenPlugin() throws IOException {
        // Given - Spring Boot project with Maven plugin
        Path projectPath = createSpringBootProjectWithMavenPlugin();
        
        // When
        List<String> detectedServers = detectionService.scanProject(projectPath);
        
        // Then
        assertThat(detectedServers).contains("springboot");
        assertThat(detectedServers).hasSize(1); // Should detect only springboot
    }
    
    @Test
    @DisplayName("Should detect Spring Boot via version properties")
    void testDetectSpringBootViaVersionProperties() throws IOException {
        // Given - Spring Boot project using version properties
        Path projectPath = createSpringBootProjectWithVersionProperties();
        
        // When
        List<String> detectedServers = detectionService.scanProject(projectPath);
        
        // Then
        assertThat(detectedServers).contains("springboot");
        assertThat(detectedServers).hasSize(1); // Should detect only springboot
    }
    
    @Test
    @DisplayName("Should detect NetBeans via common artifacts")
    void testDetectNetBeansViaCommonArtifacts() throws IOException {
        // Given
        Path projectPath = createProjectWithNetBeansArtifacts();
        
        // When
        List<String> detectedServers = detectionService.scanProject(projectPath);
        
        // Then
        assertThat(detectedServers).contains("netbeans");
        assertThat(detectedServers).hasSize(1);
    }
    
    @Test
    @DisplayName("Should detect GlassFish via common artifacts")
    void testDetectGlassFishViaCommonArtifacts() throws IOException {
        // Given
        Path projectPath = createProjectWithGlassFishArtifacts();
        
        // When
        List<String> detectedServers = detectionService.scanProject(projectPath);
        
        // Then
        assertThat(detectedServers).contains("glassfish");
        assertThat(detectedServers).hasSize(1); // Only glassfish detected with specific group:name artifacts
    }
    
    @Test
    @DisplayName("Should detect multiple platforms in mixed project")
    void testDetectMultiplePlatforms_MixedProject() throws IOException {
        // Given
        Path projectPath = createMixedProjectWithMultiplePlatforms();
        
        // When
        List<String> detectedServers = detectionService.scanProject(projectPath);
        
        // Then
        assertThat(detectedServers).contains("tomcat");
        assertThat(detectedServers).contains("wildfly");
        assertThat(detectedServers).contains("springboot");
        assertThat(detectedServers).hasSizeGreaterThanOrEqualTo(3); // At least 3 platforms detected
    }
    
    @Test
    @DisplayName("Should not return duplicate platforms when detected by multiple sources")
    void testNoDuplicatePlatforms_WhenDetectedByMultipleSources() throws IOException {
        // Given - a project with both pom.xml and build.gradle containing the same platform
        Path projectPath = tempDir.resolve("multi-build-project");
        Files.createDirectories(projectPath);
        
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>tomcat-test</artifactId>
                <version>1.0.0</version>
                <dependencies>
                    <dependency>
                        <groupId>org.apache.tomcat.embed</groupId>
                        <artifactId>tomcat-embed-core</artifactId>
                        <version>10.1.15</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        Files.write(projectPath.resolve("pom.xml"), pomContent.getBytes());
        
        String gradleContent = """
            dependencies {
                implementation 'org.apache.tomcat.embed:tomcat-embed-core:10.1.15'
            }
            """;
        Files.write(projectPath.resolve("build.gradle"), gradleContent.getBytes());
        
        // When
        List<String> detectedServers = detectionService.scanProject(projectPath);
        
        // Then - should contain tomcat but only once (no duplicates)
        assertThat(detectedServers).contains("tomcat");
        assertThat(detectedServers).hasSize(1);
        
        // Verify no duplicates by checking distinct count equals size
        assertThat(detectedServers.stream().distinct().count())
            .isEqualTo(detectedServers.size())
            .as("All platforms should be unique with no duplicates");
    }
    
    @Test
    @DisplayName("Should return empty result for project with no platforms")
    void testScanProject_NoPlatformsFound_ReturnsEmptyResult() throws IOException {
        // Given
        Path emptyProjectPath = tempDir.resolve("empty-project");
        Files.createDirectories(emptyProjectPath);
        
        // When
        List<String> detectedServers = detectionService.scanProject(emptyProjectPath);
        
        // Then
        assertThat(detectedServers).isEmpty();
    }
    
    @Test
    @DisplayName("Should detect Arquillian via common artifacts")
    void testDetectArquillianViaCommonArtifacts() throws IOException {
        // Given
        Path projectPath = createProjectWithArquillianArtifacts();
        
        // When
        List<String> detectedServers = detectionService.scanProject(projectPath);
        
        // Then
        assertThat(detectedServers).contains("arquillian");
    }
    
    @Test
    @DisplayName("Should detect ShrinkWrap via common artifacts")
    void testDetectShrinkWrapViaCommonArtifacts() throws IOException {
        // Given
        Path projectPath = createProjectWithShrinkWrapArtifacts();
        
        // When
        List<String> detectedServers = detectionService.scanProject(projectPath);
        
        // Then
        assertThat(detectedServers).contains("shrinkwrap");
    }
    
    @Test
    @DisplayName("Should detect WebLogic via common artifacts")
    void testDetectWebLogicViaCommonArtifacts() throws IOException {
        // Given
        Path projectPath = createProjectWithWebLogicArtifacts();
        
        // When
        List<String> detectedServers = detectionService.scanProject(projectPath);
        
        // Then
        assertThat(detectedServers).contains("weblogic");
    }
    
    @Test
    @DisplayName("Should detect WildFly via server dependencies")
    void testDetectWildFlyViaServerDependencies() throws IOException {
        // Given - project with WildFly server dependencies
        Path projectPath = createProjectWithWildFlyServerDependencies();
        
        // When
        List<String> detectedServers = detectionService.scanProject(projectPath);
        
        // Then
        assertThat(detectedServers).contains("wildfly");
        assertThat(detectedServers).hasSize(1);
    }
    
    
    
    @Test
    @DisplayName("Should detect only WebSphere for real WebSphere project from examples")
    void testDetectOnlyWebSphere_RealProjectFromExamples() throws IOException {
        // Given - Real WebSphere project from examples.yaml
        Path projectPath = Path.of("../../../examples/websphere/extracted/app-modernization-plants-by-websphere-jee6-master");
        
        // When
        List<String> detectedServers = detectionService.scanProject(projectPath);
        
        // Then - Should only detect WebSphere, not multiple platforms
        assertThat(detectedServers).contains("websphere");
        assertThat(detectedServers).hasSize(1);
        
        // Verify no other platforms are detected
        assertThat(detectedServers).doesNotContain("wildfly");
        assertThat(detectedServers).doesNotContain("weblogic");
        assertThat(detectedServers).doesNotContain("glassfish");
        assertThat(detectedServers).doesNotContain("tomcat");
        assertThat(detectedServers).doesNotContain("jetty");
    }
    
    @Test
    @DisplayName("Should detect WebSphere via groupId pattern in pom.xml")
    void testDetectWebSphere_ViaGroupIdPattern() throws IOException {
        // Given - Project with WebSphere-specific groupId
        Path projectPath = tempDir.resolve("websphere-groupid-project");
        Files.createDirectories(projectPath);
        
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.ibm.websphere.pbw</groupId>
                    <artifactId>test-project</artifactId>
                    <version>1.0.0</version>
                    <packaging>ear</packaging>
                    <dependencies>
                        <dependency>
                            <groupId>net.wasdev.maven.tools.targets</groupId>
                            <artifactId>java-specs</artifactId>
                            <version>1.0</version>
                            <scope>provided</scope>
                        </dependency>
                    </dependencies>
                </project>
            """;
        Files.write(projectPath.resolve("pom.xml"), pomContent.getBytes());
        
        // When
        List<String> detectedServers = detectionService.scanProject(projectPath);
        
        // Then - Should detect WebSphere and nothing else
        assertThat(detectedServers).contains("websphere");
        assertThat(detectedServers).hasSize(1);
        
        // Verify no WildFly detection (this was the original bug)
        assertThat(detectedServers).doesNotContain("wildfly");
    }

    @Test
    @DisplayName("Should not fallback to WildFly for WebSphere EAR project")
    void testNoFallbackToWildFly_ForWebSphereEARProject() throws IOException {
        // Given - EAR project with WebSphere groupId but no explicit dependencies
        Path projectPath = tempDir.resolve("websphere-ear-only");
        Files.createDirectories(projectPath);
        
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.ibm.websphere.test</groupId>
                    <artifactId>ear-project</artifactId>
                    <packaging>ear</packaging>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>javax.servlet</groupId>
                            <artifactId>javax.servlet-api</artifactId>
                            <version>3.1.0</version>
                        </dependency>
                    </dependencies>
                </project>
            """;
        Files.write(projectPath.resolve("pom.xml"), pomContent.getBytes());
        
        // When
        List<String> detectedServers = detectionService.scanProject(projectPath);
        
        // Then - Should detect WebSphere, not WildFly (this was the original bug)
        assertThat(detectedServers).contains("websphere");
        assertThat(detectedServers).doesNotContain("wildfly");
        
        // Should only detect one platform
        assertThat(detectedServers).hasSize(1);
    }
    
    @Test
    @DisplayName("Should detect WebSphere via net.wasdev dependency")
    void testDetectWebSphere_ViaNetWasDevDependency() throws IOException {
        // Given - Project with net.wasdev.maven.tools.targets dependency
        Path projectPath = tempDir.resolve("websphere-netwasdev-project");
        Files.createDirectories(projectPath);
        
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test-project</artifactId>
                    <packaging>war</packaging>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>net.wasdev.maven.tools.targets</groupId>
                            <artifactId>java-specs</artifactId>
                            <version>1.0</version>
                            <scope>provided</scope>
                        </dependency>
                    </dependencies>
                </project>
            """;
        Files.write(projectPath.resolve("pom.xml"), pomContent.getBytes());
        
        // When
        List<String> detectedServers = detectionService.scanProject(projectPath);
        
        // Then - Should detect WebSphere, not WildFly (this was the original bug)
        assertThat(detectedServers).contains("websphere");
        assertThat(detectedServers).doesNotContain("wildfly");
        
        // Should only detect one platform
        assertThat(detectedServers).hasSize(1);
    }
    
    @Test
    @DisplayName("Should detect only WebLogic when WebLogic indicators present")
    void testDetectOnlyWebLogic_WhenWebLogicIndicatorsPresent() throws IOException {
        // Given - WebLogic project with specific indicators
        Path projectPath = createWebLogicProjectWithIndicators();
        
        // When
        List<String> detectedServers = detectionService.scanProject(projectPath);
        
        // Then
        assertThat(detectedServers).contains("weblogic");
        assertThat(detectedServers).hasSize(1);
    }

    @Test
    @DisplayName("Should detect only GlassFish when GlassFish indicators present")
    void testDetectOnlyGlassFish_WhenGlassFishIndicatorsPresent() throws IOException {
        // Given - GlassFish project with specific indicators
        Path projectPath = createGlassFishProjectWithIndicators();
        
        // When
        List<String> detectedServers = detectionService.scanProject(projectPath);
        
        // Then
        assertThat(detectedServers).contains("glassfish");
        assertThat(detectedServers).hasSize(1);
    }

    @Test
    @DisplayName("Should detect only Liberty when Liberty indicators present")
    void testDetectOnlyLiberty_WhenLibertyIndicatorsPresent() throws IOException {
        // Given - Liberty project with specific indicators
        Path projectPath = createLibertyProjectWithIndicators();
        
        // When
        List<String> detectedServers = detectionService.scanProject(projectPath);
        
        // Then
        assertThat(detectedServers).contains("liberty");
        assertThat(detectedServers).hasSize(1);
    }

    @Test
    @DisplayName("Should not over-detect platforms for project with only web.xml")
    void testNoOverDetection_OnlyWebXmlPresent() throws IOException {
        // Given - project with only web.xml (no specific platform indicators)
        Path projectPath = createProjectWithOnlyWebXml();
        
        // When
        List<String> detectedServers = detectionService.scanProject(projectPath);
        
        // Then - should be conservative and not detect multiple platforms
        assertThat(detectedServers).hasSizeLessThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Should not over-detect platforms for project with only WAR file")
    void testNoOverDetection_OnlyWarPresent() throws IOException {
        // Given - project with only WAR file (no specific platform indicators)
        Path projectPath = createProjectWithOnlyWar();
        
        // When
        List<String> detectedServers = detectionService.scanProject(projectPath);
        
        // Then - should be conservative and not detect multiple platforms
        assertThat(detectedServers).hasSizeLessThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Should not over-detect platforms for project with only EAR file")
    void testNoOverDetection_OnlyEarPresent() throws IOException {
        // Given - project with only EAR file (no specific platform indicators)
        Path projectPath = createProjectWithOnlyEar();
        
        // When
        List<String> detectedServers = detectionService.scanProject(projectPath);
        
        // Then - should be conservative and not detect multiple platforms
        assertThat(detectedServers).hasSizeLessThanOrEqualTo(1);
    }
    
    // Helper methods for creating test projects
    private Path createMavenProjectWithTomcatArtifacts() throws IOException {
        Path projectPath = tempDir.resolve("tomcat-maven");
        Files.createDirectories(projectPath);
        
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>tomcat-test</artifactId>
                <version>1.0.0</version>
                <dependencies>
                    <dependency>
                        <groupId>org.apache.tomcat.embed</groupId>
                        <artifactId>tomcat-embed-core</artifactId>
                        <version>10.1.15</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        Files.write(projectPath.resolve("pom.xml"), pomContent.getBytes());
        return projectPath;
    }
    
    private Path createGradleProjectWithWildFlyArtifacts() throws IOException {
        Path projectPath = tempDir.resolve("wildfly-gradle");
        Files.createDirectories(projectPath);
        
        String gradleContent = """
            dependencies {
                implementation 'org.wildfly:wildfly-ee:27.0.1.Final'
            }
            """;
        Files.write(projectPath.resolve("build.gradle"), gradleContent.getBytes());
        return projectPath;
    }
    
    private Path createSpringBootProjectWithArtifacts() throws IOException {
        Path projectPath = tempDir.resolve("spring-boot");
        Files.createDirectories(projectPath);
        
        String gradleContent = """
            dependencies {
                implementation 'org.springframework.boot:spring-boot-starter:3.1.5'
                implementation 'org.springframework.boot:spring-boot-starter-web:3.1.5'
            }
            """;
        Files.write(projectPath.resolve("build.gradle"), gradleContent.getBytes());
        return projectPath;
    }
    
    private Path createProjectWithNetBeansArtifacts() throws IOException {
        Path projectPath = tempDir.resolve("netbeans");
        Files.createDirectories(projectPath);
        
        String gradleContent = """
            dependencies {
                implementation 'org.netbeans.modules:org-netbeans-modules-java-platform:12.6'
                implementation 'org.netbeans.api:org-netbeans-api-java:12.6'
            }
            """;
        Files.write(projectPath.resolve("build.gradle"), gradleContent.getBytes());
        return projectPath;
    }
    
    private Path createProjectWithGlassFishArtifacts() throws IOException {
        Path projectPath = tempDir.resolve("glassfish");
        Files.createDirectories(projectPath);
        
        String gradleContent = """
            dependencies {
                implementation 'org.glassfish.main:glassfish-main:7.0.0'
            }
            """;
        Files.write(projectPath.resolve("build.gradle"), gradleContent.getBytes());
        return projectPath;
    }
    
    private Path createMixedProjectWithMultiplePlatforms() throws IOException {
        Path projectPath = tempDir.resolve("mixed-project");
        Files.createDirectories(projectPath);
        
        String gradleContent = """
            dependencies {
                implementation 'org.apache.tomcat.embed:tomcat-embed-core:10.1.15'
                implementation 'org.wildfly:wildfly-ee:27.0.1.Final'
                implementation 'org.springframework.boot:spring-boot-starter:3.1.5'
            }
            """;
        Files.write(projectPath.resolve("build.gradle"), gradleContent.getBytes());
        return projectPath;
    }
    
    private Path createProjectWithArquillianArtifacts() throws IOException {
        Path projectPath = tempDir.resolve("arquillian-project");
        Files.createDirectories(projectPath);
        
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>arquillian-test</artifactId>
                <version>1.0.0</version>
                <dependencies>
                    <dependency>
                        <groupId>org.jboss.arquillian</groupId>
                        <artifactId>arquillian-core</artifactId>
                        <version>1.7.0.Final</version>
                    </dependency>
                    <dependency>
                        <groupId>org.jboss.arquillian.container</groupId>
                        <artifactId>arquillian-container-managed</artifactId>
                        <version>1.7.0.Final</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        Files.write(projectPath.resolve("pom.xml"), pomContent.getBytes());
        return projectPath;
    }
    
    private Path createProjectWithShrinkWrapArtifacts() throws IOException {
        Path projectPath = tempDir.resolve("shrinkwrap-project");
        Files.createDirectories(projectPath);
        
        String gradleContent = """
            dependencies {
                implementation 'org.jboss.shrinkwrap:shrinkwrap-api:1.2.6'
                implementation 'org.jboss.shrinkwrap.resolver:shrinkwrap-resolver-api:3.1.4'
            }
            """;
        Files.write(projectPath.resolve("build.gradle"), gradleContent.getBytes());
        return projectPath;
    }
    
    private Path createProjectWithWebLogicArtifacts() throws IOException {
        Path projectPath = tempDir.resolve("weblogic-project");
        Files.createDirectories(projectPath);
        
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>weblogic-test</artifactId>
                <version>1.0.0</version>
                <dependencies>
                    <dependency>
                        <groupId>com.oracle.weblogic</groupId>
                        <artifactId>weblogic-server</artifactId>
                        <version>14.1.1.0</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        Files.write(projectPath.resolve("pom.xml"), pomContent.getBytes());
        return projectPath;
    }
    
    private Path createSpringBootProjectWithParentPOM() throws IOException {
        Path projectPath = tempDir.resolve("spring-boot-parent");
        Files.createDirectories(projectPath);
        
        // Create the exact structure from the problematic project
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <parent>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-parent</artifactId>
                    <version>2.7.7</version>
                    <relativePath/> <!-- lookup parent from repository -->
                </parent>
                <groupId>com.example</groupId>
                <artifactId>demo</artifactId>
                <version>0.0.1-SNAPSHOT</version>
                <name>demo</name>
                <description>Demo project for Spring Boot</description>
                <properties>
                    <java.version>11</java.version>
                </properties>
                <dependencies>
                    <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-web</artifactId>
                    </dependency>
                    <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-validation</artifactId>
                    </dependency>
                </dependencies>
                <build>
                    <plugins>
                            <plugin>
                                    <groupId>org.springframework.boot</groupId>
                                    <artifactId>spring-boot-maven-plugin</artifactId>
                            </plugin>
                    </plugins>
                </build>
            </project>
            """;
        Files.write(projectPath.resolve("pom.xml"), pomContent.getBytes());
        return projectPath;
    }
    
    private Path createSpringBootProjectWithMavenPlugin() throws IOException {
        Path projectPath = tempDir.resolve("spring-boot-plugin");
        Files.createDirectories(projectPath);
        
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>spring-boot-plugin-test</artifactId>
                <version>1.0.0</version>
                <build>
                    <plugins>
                        <plugin>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-maven-plugin</artifactId>
                        </plugin>
                    </plugins>
                </build>
            </project>
            """;
        Files.write(projectPath.resolve("pom.xml"), pomContent.getBytes());
        return projectPath;
    }
    
    private Path createSpringBootProjectWithVersionProperties() throws IOException {
        Path projectPath = tempDir.resolve("spring-boot-properties");
        Files.createDirectories(projectPath);
        
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>spring-boot-properties-test</artifactId>
                <version>1.0.0</version>
                <properties>
                    <spring-boot.version>2.7.7</spring-boot.version>
                </properties>
                <dependencies>
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-web</artifactId>
                    </dependency>
                </dependencies>
            </project>
            """;
        Files.write(projectPath.resolve("pom.xml"), pomContent.getBytes());
        return projectPath;
    }
    
    private Path createProjectWithWildFlyServerDependencies() throws IOException {
        Path projectPath = tempDir.resolve("wildfly-server");
        Files.createDirectories(projectPath);
        
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.example</groupId>
                <artifactId>wildfly-server-test</artifactId>
                <packaging>war</packaging>
                <version>1.0.0</version>
                <properties>
                    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
                    <version.wildfly>27.0.1.Final</version.wildfly>
                </properties>
                <dependencies>
                    <dependency>
                        <groupId>org.wildfly</groupId>
                        <artifactId>wildfly-ee</artifactId>
                        <version>${version.wildfly}</version>
                        <scope>provided</scope>
                    </dependency>
                    <dependency>
                        <groupId>org.wildfly</groupId>
                        <artifactId>wildfly-undertow</artifactId>
                        <version>${version.wildfly}</version>
                        <scope>provided</scope>
                    </dependency>
                    <dependency>
                        <groupId>org.wildfly</groupId>
                        <artifactId>wildfly-client</artifactId>
                        <version>${version.wildfly}</version>
                        <scope>provided</scope>
                    </dependency>
                </dependencies>
                <build>
                    <plugins>
                        <plugin>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-war-plugin</artifactId>
                            <version>3.3.1</version>
                            <configuration>
                                <failOnMissingWebXml>false</failOnMissingWebXml>
                            </configuration>
                        </plugin>
                    </plugins>
                </build>
            </project>
            """;
        Files.write(projectPath.resolve("pom.xml"), pomContent.getBytes());
        return projectPath;
    }
    
        
        
    private Path createWebLogicProjectWithIndicators() throws IOException {
        Path projectPath = tempDir.resolve("weblogic-indicators");
        Files.createDirectories(projectPath);
        
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>weblogic-test</artifactId>
                <version>1.0.0</version>
                <dependencies>
                    <dependency>
                        <groupId>com.oracle.weblogic</groupId>
                        <artifactId>weblogic-server</artifactId>
                        <version>14.1.1.0</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        Files.write(projectPath.resolve("pom.xml"), pomContent.getBytes());
        return projectPath;
    }
    
    private Path createGlassFishProjectWithIndicators() throws IOException {
        Path projectPath = tempDir.resolve("glassfish-indicators");
        Files.createDirectories(projectPath);
        
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>glassfish-test</artifactId>
                <version>1.0.0</version>
                <dependencies>
                    <dependency>
                        <groupId>org.glassfish.main</groupId>
                        <artifactId>glassfish-main</artifactId>
                        <version>7.0.0</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        Files.write(projectPath.resolve("pom.xml"), pomContent.getBytes());
        return projectPath;
    }
    
    private Path createLibertyProjectWithIndicators() throws IOException {
        Path projectPath = tempDir.resolve("liberty-indicators");
        Files.createDirectories(projectPath);
        
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>liberty-test</artifactId>
                <version>1.0.0</version>
                <dependencies>
                    <dependency>
                        <groupId>io.openliberty</groupId>
                        <artifactId>openliberty-kernel</artifactId>
                        <version>23.0.0</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        Files.write(projectPath.resolve("pom.xml"), pomContent.getBytes());
        return projectPath;
    }
    
    private Path createProjectWithOnlyWebXml() throws IOException {
        Path projectPath = tempDir.resolve("webxml-only");
        Files.createDirectories(projectPath);
        Files.createDirectories(projectPath.resolve("src/main/webapp/WEB-INF"));
        
        String webXmlContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee 
                     http://xmlns.jcp.org/xml/ns/javaee/web-app_4_0.xsd"
                     version="4.0">
                <display-name>Test Web App</display-name>
            </web-app>
            """;
        Files.write(projectPath.resolve("src/main/webapp/WEB-INF/web.xml"), webXmlContent.getBytes());
        return projectPath;
    }
    
    private Path createProjectWithOnlyWar() throws IOException {
        Path projectPath = tempDir.resolve("war-only");
        Files.createDirectories(projectPath);
        
        // Create a WAR file
        Files.createDirectories(projectPath.resolve("target"));
        Files.write(projectPath.resolve("target/test.war"), "dummy war content".getBytes());
        return projectPath;
    }
    
    private Path createProjectWithOnlyEar() throws IOException {
        Path projectPath = tempDir.resolve("ear-only");
        Files.createDirectories(projectPath);
        
        // Create an EAR file
        Files.createDirectories(projectPath.resolve("target"));
        Files.write(projectPath.resolve("target/test.ear"), "dummy ear content".getBytes());
        return projectPath;
    }
    
    @Test
    @DisplayName("Should detect Payara via Payara Micro dependency")
    void shouldDetectPayaraViaPayaraMicroDependency() throws IOException {
        // Given - Project with Payara Micro dependency
        Path projectPath = tempDir.resolve("payara-micro-project");
        Files.createDirectories(projectPath);
        
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>payara.reactive</groupId>
                <artifactId>reactive-web</artifactId>
                <version>1.0-SNAPSHOT</version>
                <packaging>war</packaging>
                
                <dependencies>
                    <dependency>
                        <groupId>fish.payara.extras</groupId>
                        <artifactId>payara-micro</artifactId>
                        <version>4.1.1.163</version>
                        <scope>provided</scope>
                    </dependency>
                    <dependency>
                        <groupId>javax</groupId>
                        <artifactId>javaee-web-api</artifactId>
                        <version>7.0</version>
                        <scope>provided</scope>
                    </dependency>
                </dependencies>
            </project>
            """;
        Files.write(projectPath.resolve("pom.xml"), pomContent.getBytes());
        
        // When
        List<String> detectedServers = detectionService.scanProject(projectPath);
        
        // Then
        assertThat(detectedServers).contains("payara");
        assertThat(detectedServers).hasSize(1); // Should only detect Payara
    }
    
    @Test
    @DisplayName("Should detect Payara in real Payara project from examples")
    void shouldDetectPayaraInRealPayaraProject() throws IOException {
        // Given - Real Payara project from examples (if available)
        Path payaraProjectPath = Path.of("../../../examples/paraya/Payara-Micro-Reactive-Example-master/Payara-Micro-Reactive-Example-master/webapp");
        
        // Only run if the project exists (integration test)
        if (!Files.exists(payaraProjectPath)) {
            // Skip test if Payara project not available
            return;
        }
        
        // When
        List<String> detectedServers = detectionService.scanProject(payaraProjectPath);
        
        // Then
        assertThat(detectedServers).contains("payara");
        // Should detect Payara among potentially other platforms
    }
}
