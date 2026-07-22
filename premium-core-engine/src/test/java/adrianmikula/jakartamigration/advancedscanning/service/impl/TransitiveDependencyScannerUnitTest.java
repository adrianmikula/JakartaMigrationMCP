package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.DependencyTreeResult;
import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyUsage;
import adrianmikula.jakartamigration.advancedscanning.service.DependencyTreeCommandExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TransitiveDependencyScannerImpl using synthetic test projects.
 * Tests scanning logic with temporary project creation.
 */
class TransitiveDependencyScannerUnitTest {

    private TransitiveDependencyScannerImpl scanner;
    private Path testProject;

    @BeforeEach
    void setUp() {
        scanner = new TransitiveDependencyScannerImpl();
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

    @Test
    void scanProject_shouldDetectMultiModuleGradle_withSettingsGradle(@TempDir Path tempDir) throws IOException {
        Path root = tempDir.resolve("gradle-multi");
        Path module1 = root.resolve("module1");
        Path module2 = root.resolve("module2");
        Files.createDirectories(module1);
        Files.createDirectories(module2);

        Files.writeString(root.resolve("settings.gradle"), "include 'module1', 'module2'");

        String buildGradle = """
            dependencies {
                implementation 'javax.servlet:javax.servlet-api:4.0.1'
            }
            """;
        Files.writeString(module1.resolve("build.gradle"), buildGradle);
        Files.writeString(module2.resolve("build.gradle"), buildGradle);

        DependencyTreeCommandExecutor mockExecutor = mock(DependencyTreeCommandExecutor.class);
        List<DependencyTreeResult.DependencyNode> rootDeps = Arrays.asList(
            new DependencyTreeResult.DependencyNode("javax.servlet", "javax.servlet-api", "4.0.1", "compile", 0, false, null),
            new DependencyTreeResult.DependencyNode("javax.xml.bind", "jaxb-api", "2.3.1", "compile", 0, false, null)
        );
        when(mockExecutor.executeGradleDependenciesAsync(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(new DependencyTreeResult(rootDeps, Set.of("compile"))));

        TransitiveDependencyScannerImpl scanner = new TransitiveDependencyScannerImpl(mockExecutor,
                new adrianmikula.jakartamigration.advancedscanning.service.impl.DependencyDeduplicationServiceImpl());

        TransitiveDependencyProjectScanResult result = scanner.scanProject(root);

        assertFalse(result.getFileResults().isEmpty());
        long totalUsages = result.getFileResults().stream()
                .mapToLong(r -> r.getUsages().size())
                .sum();
        assertEquals(2, totalUsages, "Should find all dependencies from root-level scan");
    }

    @Test
    void scanProject_shouldFallbackToPerFile_whenMultiModuleRootFails(@TempDir Path tempDir) throws IOException {
        Path root = tempDir.resolve("maven-multi");
        Path module1 = root.resolve("module1");
        Files.createDirectories(module1);

        Files.writeString(root.resolve("pom.xml"),
            "<project><modelVersion>4.0.0</modelVersion><groupId>com.test</groupId>" +
            "<artifactId>parent</artifactId><version>1.0.0</version><packaging>pom</packaging>" +
            "<modules><module>module1</module></modules></project>");

        Files.writeString(module1.resolve("pom.xml"),
            "<project><modelVersion>4.0.0</modelVersion><parent><groupId>com.test</groupId>" +
            "<artifactId>parent</artifactId><version>1.0.0</version></parent>" +
            "<artifactId>module1</artifactId><dependencies>" +
            "<dependency><groupId>javax.servlet</groupId><artifactId>javax.servlet-api</artifactId>" +
            "<version>4.0.1</version></dependency></dependencies></project>");

        DependencyTreeCommandExecutor mockExecutor = mock(DependencyTreeCommandExecutor.class);
        when(mockExecutor.executeMavenDependencyTreeAsync(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(DependencyTreeResult.error("mvn command not found")));

        TransitiveDependencyScannerImpl scanner = new TransitiveDependencyScannerImpl(mockExecutor,
                new adrianmikula.jakartamigration.advancedscanning.service.impl.DependencyDeduplicationServiceImpl());

        TransitiveDependencyProjectScanResult result = scanner.scanProject(root);

        assertFalse(result.getFileResults().isEmpty());
        boolean foundServlet = result.getFileResults().stream()
                .flatMap(r -> r.getUsages().stream())
                .anyMatch(u -> u.getArtifactId().equals("javax.servlet-api"));
        assertTrue(foundServlet, "Should find servlet-api via regex fallback");
    }

    @Test
    void scanProject_singleModuleGradle_shouldNotTriggerMultiModulePath(@TempDir Path tempDir) throws IOException {
        Path project = tempDir.resolve("single-gradle");
        Files.createDirectories(project);

        Files.writeString(project.resolve("build.gradle"),
            "dependencies { implementation 'javax.jms:javax.jms-api:2.0.1' }");

        DependencyTreeCommandExecutor mockExecutor = mock(DependencyTreeCommandExecutor.class);
        List<DependencyTreeResult.DependencyNode> deps = Arrays.asList(
            new DependencyTreeResult.DependencyNode("javax.jms", "javax.jms-api", "2.0.1", "compile", 0, false, null)
        );
        when(mockExecutor.executeGradleDependenciesAsync(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(new DependencyTreeResult(deps, Set.of("compile"))));

        TransitiveDependencyScannerImpl scanner = new TransitiveDependencyScannerImpl(mockExecutor,
                new adrianmikula.jakartamigration.advancedscanning.service.impl.DependencyDeduplicationServiceImpl());

        TransitiveDependencyProjectScanResult result = scanner.scanProject(project);

        assertEquals(1, result.getFileResults().size());
        assertTrue(result.getFileResults().get(0).getUsages().stream()
                .anyMatch(u -> u.getArtifactId().equals("javax.jms-api")));
    }

    @Test
    void scanProject_multiModuleMaven_fallsBackPerFile_whenRootReturnsEmpty(@TempDir Path tempDir) throws IOException {
        Path root = tempDir.resolve("maven-multi-empty");
        Path module1 = root.resolve("module1");
        Files.createDirectories(module1);

        Files.writeString(root.resolve("pom.xml"),
            "<project><modelVersion>4.0.0</modelVersion><groupId>com.test</groupId>" +
            "<artifactId>parent</artifactId><version>1.0.0</version><packaging>pom</packaging>" +
            "<modules><module>module1</module></modules></project>");

        Files.writeString(module1.resolve("pom.xml"),
            "<project><modelVersion>4.0.0</modelVersion><parent><groupId>com.test</groupId>" +
            "<artifactId>parent</artifactId><version>1.0.0</version></parent>" +
            "<artifactId>module1</artifactId><dependencies>" +
            "<dependency><groupId>javax.xml.bind</groupId><artifactId>jaxb-api</artifactId>" +
            "<version>2.3.1</version></dependency></dependencies></project>");

        DependencyTreeCommandExecutor mockExecutor = mock(DependencyTreeCommandExecutor.class);
        when(mockExecutor.executeMavenDependencyTreeAsync(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(DependencyTreeResult.empty()));

        TransitiveDependencyScannerImpl scanner = new TransitiveDependencyScannerImpl(mockExecutor,
                new adrianmikula.jakartamigration.advancedscanning.service.impl.DependencyDeduplicationServiceImpl());

        TransitiveDependencyProjectScanResult result = scanner.scanProject(root);

        assertFalse(result.getFileResults().isEmpty());
        boolean foundJaxb = result.getFileResults().stream()
                .flatMap(r -> r.getUsages().stream())
                .anyMatch(u -> u.getArtifactId().equals("jaxb-api"));
        assertTrue(foundJaxb, "Should find jaxb-api via regex fallback after empty root result");
    }
}
