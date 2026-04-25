package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyUsage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for TransitiveDependencyScannerImpl using TestProjectHelper pattern.
 * Tests real project scanning scenarios with temporary project creation.
 * Tagged as slow due to integration test nature.
 */
@Tag("slow")
class TransitiveDependencyScannerIntegrationTest {

    private TransitiveDependencyScannerImpl scanner;
    private Path testProject;

    @BeforeEach
    void setUp() {
        scanner = new TransitiveDependencyScannerImpl();
    }

    @AfterEach
    void tearDown() {
        // Cleanup is handled by @TempDir, but we can add explicit cleanup if needed
    }

    @Test
    void scanProject_withMavenProject_shouldFindDependencies(@TempDir Path tempDir) throws IOException {
        // Create a Maven project structure
        testProject = tempDir.resolve("maven-test-project");
        Files.createDirectories(testProject);

        // Create pom.xml with javax dependencies
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                     http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.test</groupId>
                <artifactId>integration-test</artifactId>
                <version>1.0.0</version>
                <dependencies>
                    <dependency>
                        <groupId>javax.servlet</groupId>
                        <artifactId>javax.servlet-api</artifactId>
                        <version>4.0.1</version>
                        <scope>provided</scope>
                    </dependency>
                    <dependency>
                        <groupId>javax.xml.bind</groupId>
                        <artifactId>jaxb-api</artifactId>
                        <version>2.3.1</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        Files.writeString(testProject.resolve("pom.xml"), pomContent);

        // Scan the project
        TransitiveDependencyProjectScanResult result = scanner.scanProject(testProject);

        // Verify results
        assertNotNull(result);
        assertEquals(1, result.getTotalBuildFilesScanned());
        assertFalse(result.getFileResults().isEmpty());

        TransitiveDependencyScanResult fileResult = result.getFileResults().get(0);
        assertEquals("Maven", fileResult.getBuildFileType());
        assertTrue(fileResult.hasJavaxUsage());
        assertEquals(2, fileResult.getUsages().size());

        // Verify both javax dependencies were found
        assertTrue(fileResult.getUsages().stream()
                .anyMatch(u -> u.getArtifactId().equals("javax.servlet-api")));
        assertTrue(fileResult.getUsages().stream()
                .anyMatch(u -> u.getArtifactId().equals("jaxb-api")));
    }

    @Test
    void scanProject_withGradleProject_shouldFindDependencies(@TempDir Path tempDir) throws IOException {
        // Create a Gradle project structure
        testProject = tempDir.resolve("gradle-test-project");
        Files.createDirectories(testProject);

        // Create build.gradle with javax dependencies
        String buildGradle = """
            plugins {
                id 'java'
            }
            
            repositories {
                mavenCentral()
            }
            
            dependencies {
                implementation 'javax.jms:javax.jms-api:2.0.1'
                implementation 'javax.persistence:javax.persistence-api:2.2'
                testImplementation 'junit:junit:4.13.2'
            }
            """;
        Files.writeString(testProject.resolve("build.gradle"), buildGradle);

        // Scan the project
        TransitiveDependencyProjectScanResult result = scanner.scanProject(testProject);

        // Verify results
        assertNotNull(result);
        assertEquals(1, result.getTotalBuildFilesScanned());
        assertFalse(result.getFileResults().isEmpty());

        TransitiveDependencyScanResult fileResult = result.getFileResults().get(0);
        assertEquals("Gradle", fileResult.getBuildFileType());
        assertTrue(fileResult.hasJavaxUsage());

        // Verify javax dependencies were found (junit should be filtered out)
        assertTrue(fileResult.getUsages().stream()
                .anyMatch(u -> u.getArtifactId().equals("javax.jms-api")));
        assertTrue(fileResult.getUsages().stream()
                .anyMatch(u -> u.getArtifactId().equals("javax.persistence-api")));
    }

    @Test
    void scanProject_withMixedBuildFiles_shouldScanBoth(@TempDir Path tempDir) throws IOException {
        // Create a project with both pom.xml and build.gradle
        testProject = tempDir.resolve("mixed-test-project");
        Files.createDirectories(testProject);

        // Create pom.xml
        String pomContent = """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.test</groupId>
                <artifactId>mixed-test</artifactId>
                <version>1.0.0</version>
                <dependencies>
                    <dependency>
                        <groupId>javax.servlet</groupId>
                        <artifactId>javax.servlet-api</artifactId>
                        <version>4.0.1</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        Files.writeString(testProject.resolve("pom.xml"), pomContent);

        // Create build.gradle
        String buildGradle = """
            dependencies {
                implementation 'javax.xml.bind:jaxb-api:2.3.1'
            }
            """;
        Files.writeString(testProject.resolve("build.gradle"), buildGradle);

        // Scan the project
        TransitiveDependencyProjectScanResult result = scanner.scanProject(testProject);

        // Should scan both files
        assertNotNull(result);
        assertEquals(2, result.getTotalBuildFilesScanned());
        assertEquals(2, result.getFileResults().size());

        // Verify both build files were scanned
        boolean foundMaven = result.getFileResults().stream()
                .anyMatch(r -> "Maven".equals(r.getBuildFileType()));
        boolean foundGradle = result.getFileResults().stream()
                .anyMatch(r -> "Gradle".equals(r.getBuildFileType()));

        assertTrue(foundMaven, "Should find Maven build file");
        assertTrue(foundGradle, "Should find Gradle build file");
    }

    @Test
    void scanProject_withNestedProjectStructure_shouldScanAll(@TempDir Path tempDir) throws IOException {
        // Create a nested project structure
        testProject = tempDir.resolve("nested-project");
        Path module1 = testProject.resolve("module1");
        Path module2 = testProject.resolve("module2");
        Files.createDirectories(module1);
        Files.createDirectories(module2);

        // Create parent pom.xml
        String parentPom = """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.test</groupId>
                <artifactId>parent</artifactId>
                <version>1.0.0</version>
                <packaging>pom</packaging>
                <modules>
                    <module>module1</module>
                    <module>module2</module>
                </modules>
            </project>
            """;
        Files.writeString(testProject.resolve("pom.xml"), parentPom);

        // Create module1 pom.xml with javax.servlet
        String module1Pom = """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <parent>
                    <groupId>com.test</groupId>
                    <artifactId>parent</artifactId>
                    <version>1.0.0</version>
                </parent>
                <artifactId>module1</artifactId>
                <dependencies>
                    <dependency>
                        <groupId>javax.servlet</groupId>
                        <artifactId>javax.servlet-api</artifactId>
                        <version>4.0.1</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        Files.writeString(module1.resolve("pom.xml"), module1Pom);

        // Create module2 pom.xml with jaxb
        String module2Pom = """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <parent>
                    <groupId>com.test</groupId>
                    <artifactId>parent</artifactId>
                    <version>1.0.0</version>
                </parent>
                <artifactId>module2</artifactId>
                <dependencies>
                    <dependency>
                        <groupId>javax.xml.bind</groupId>
                        <artifactId>jaxb-api</artifactId>
                        <version>2.3.1</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        Files.writeString(module2.resolve("pom.xml"), module2Pom);

        // Scan the entire project
        TransitiveDependencyProjectScanResult result = scanner.scanProject(testProject);

        // Should scan all 3 pom files
        assertNotNull(result);
        assertEquals(3, result.getTotalBuildFilesScanned());

        // Collect all usages
        long totalUsages = result.getFileResults().stream()
                .mapToLong(r -> r.getUsages().size())
                .sum();

        assertEquals(2, totalUsages, "Should find 2 javax dependencies total");

        // Verify both servlet and jaxb were found
        boolean foundServlet = result.getFileResults().stream()
                .flatMap(r -> r.getUsages().stream())
                .anyMatch(u -> u.getArtifactId().equals("javax.servlet-api"));
        boolean foundJaxb = result.getFileResults().stream()
                .flatMap(r -> r.getUsages().stream())
                .anyMatch(u -> u.getArtifactId().equals("jaxb-api"));

        assertTrue(foundServlet, "Should find servlet-api");
        assertTrue(foundJaxb, "Should find jaxb-api");
    }

    @Test
    void scanProject_withMultipleJavaxPackages_shouldDetectAll(@TempDir Path tempDir) throws IOException {
        // Create a project with multiple javax packages
        testProject = tempDir.resolve("multi-javax-project");
        Files.createDirectories(testProject);

        String pomContent = """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.test</groupId>
                <artifactId>multi-javax</artifactId>
                <version>1.0.0</version>
                <dependencies>
                    <dependency>
                        <groupId>javax.servlet</groupId>
                        <artifactId>javax.servlet-api</artifactId>
                        <version>4.0.1</version>
                    </dependency>
                    <dependency>
                        <groupId>javax.jms</groupId>
                        <artifactId>javax.jms-api</artifactId>
                        <version>2.0.1</version>
                    </dependency>
                    <dependency>
                        <groupId>javax.xml.bind</groupId>
                        <artifactId>jaxb-api</artifactId>
                        <version>2.3.1</version>
                    </dependency>
                    <dependency>
                        <groupId>javax.persistence</groupId>
                        <artifactId>javax.persistence-api</artifactId>
                        <version>2.2</version>
                    </dependency>
                    <dependency>
                        <groupId>javax.validation</groupId>
                        <artifactId>validation-api</artifactId>
                        <version>2.0.1.Final</version>
                    </dependency>
                    <!-- Jakarta EE 9+ dependency (should NOT be flagged) -->
                    <dependency>
                        <groupId>jakarta.servlet</groupId>
                        <artifactId>jakarta.servlet-api</artifactId>
                        <version>5.0.0</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        Files.writeString(testProject.resolve("pom.xml"), pomContent);

        // Scan the project
        TransitiveDependencyProjectScanResult result = scanner.scanProject(testProject);

        assertNotNull(result);
        assertEquals(1, result.getTotalBuildFilesScanned());

        TransitiveDependencyScanResult fileResult = result.getFileResults().get(0);

        // Should find all 6 dependencies (5 javax with high severity + 1 jakarta with low severity)
        assertEquals(6, fileResult.getUsages().size());
        assertTrue(fileResult.hasJavaxUsage());

        // Verify all javax packages are detected with high severity
        assertTrue(fileResult.getUsages().stream()
                .anyMatch(u -> u.getGroupId().equals("javax.servlet") && "high".equals(u.getSeverity())));
        assertTrue(fileResult.getUsages().stream()
                .anyMatch(u -> u.getGroupId().equals("javax.jms") && "high".equals(u.getSeverity())));
        assertTrue(fileResult.getUsages().stream()
                .anyMatch(u -> u.getGroupId().equals("javax.xml.bind") && "high".equals(u.getSeverity())));
        assertTrue(fileResult.getUsages().stream()
                .anyMatch(u -> u.getGroupId().equals("javax.persistence") && "high".equals(u.getSeverity())));
        assertTrue(fileResult.getUsages().stream()
                .anyMatch(u -> u.getGroupId().equals("javax.validation") && "high".equals(u.getSeverity())));

        // Verify jakarta is included with low severity (not a javax dependency)
        assertTrue(fileResult.getUsages().stream()
                .anyMatch(u -> u.getGroupId().equals("jakarta.servlet") && "low".equals(u.getSeverity())));
    }

    @Test
    void scanProject_shouldTrackDependencyMetadata(@TempDir Path tempDir) throws IOException {
        // Create a project to test metadata extraction
        testProject = tempDir.resolve("metadata-test-project");
        Files.createDirectories(testProject);

        String pomContent = """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.test</groupId>
                <artifactId>metadata-test</artifactId>
                <version>1.0.0</version>
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
        Files.writeString(testProject.resolve("pom.xml"), pomContent);

        // Scan the project
        TransitiveDependencyProjectScanResult result = scanner.scanProject(testProject);

        assertNotNull(result);
        TransitiveDependencyScanResult fileResult = result.getFileResults().get(0);
        assertEquals(1, fileResult.getUsages().size());

        TransitiveDependencyUsage usage = fileResult.getUsages().get(0);

        // Verify all metadata fields
        assertEquals("javax.servlet", usage.getGroupId());
        assertEquals("javax.servlet-api", usage.getArtifactId());
        assertEquals("4.0.1", usage.getVersion());
        assertEquals("provided", usage.getScope());
        assertEquals(0, usage.getDepth()); // Direct dependency
        assertFalse(usage.isTransitive());
        // Check javaxPackage field instead of hasJavaxUsage method
        assertNotNull(usage.getJavaxPackage());
        assertNotNull(usage.getRecommendation());
    }
}
