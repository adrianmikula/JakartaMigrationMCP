package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.DependencyTreeResult;
import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyUsage;
import adrianmikula.jakartamigration.advancedscanning.service.DependencyDeduplicationService;
import adrianmikula.jakartamigration.advancedscanning.service.DependencyTreeCommandExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransitiveDependencyScannerImplTest {

    @Test
    void scanProject_shouldReturnEmptyResultForEmptyDirectory(@TempDir Path tempDir) {
        TransitiveDependencyScannerImpl scanner = new TransitiveDependencyScannerImpl();

        TransitiveDependencyProjectScanResult result = scanner.scanProject(tempDir);

        assertTrue(result.getFileResults().isEmpty());
        assertEquals(0, result.getTotalBuildFilesScanned());
    }

    @Test
    void scanProject_shouldSkipNonBuildFiles(@TempDir Path tempDir) throws IOException {
        Files.createFile(tempDir.resolve("readme.txt"));
        Files.createFile(tempDir.resolve("some-file.java"));

        TransitiveDependencyScannerImpl scanner = new TransitiveDependencyScannerImpl();

        TransitiveDependencyProjectScanResult result = scanner.scanProject(tempDir);

        assertTrue(result.getFileResults().isEmpty());
    }

    @Test
    void scanFile_shouldReturnEmptyResultForNonExistentFile() {
        TransitiveDependencyScannerImpl scanner = new TransitiveDependencyScannerImpl();
        Path nonExistent = Path.of("/non/existent/file.xml");

        TransitiveDependencyScanResult result = scanner.scanFile(nonExistent);

        assertNull(result.getBuildFileType());
        assertTrue(result.getUsages().isEmpty());
    }

    @Test
    void scanFile_shouldUseFallbackForNonBuildFile(@TempDir Path tempDir) throws IOException {
        Path txtFile = Files.createFile(tempDir.resolve("test.txt"));

        TransitiveDependencyScannerImpl scanner = new TransitiveDependencyScannerImpl();
        TransitiveDependencyScanResult result = scanner.scanFile(txtFile);

        assertNull(result.getBuildFileType());
        assertTrue(result.getUsages().isEmpty());
    }

    @Test
    void scanFile_shouldFallbackToRegexParsingWhenCommandFails(@TempDir Path tempDir) throws IOException {
        String pomContent = """
            <project>
                <dependencies>
                    <dependency>
                        <groupId>javax.xml.bind</groupId>
                        <artifactId>jaxb-api</artifactId>
                        <version>2.3.1</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        Path pomFile = tempDir.resolve("pom.xml");
        Files.writeString(pomFile, pomContent);

        // Create mock executor that returns error
        DependencyTreeCommandExecutor mockExecutor = mock(DependencyTreeCommandExecutor.class);
        DependencyTreeResult errorResult = DependencyTreeResult.error("Command failed");
        when(mockExecutor.executeMavenDependencyTreeAsync(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(errorResult));

        DependencyDeduplicationService dedupService = new DependencyDeduplicationServiceImpl();
        TransitiveDependencyScannerImpl scanner = new TransitiveDependencyScannerImpl(mockExecutor, dedupService);

        TransitiveDependencyScanResult result = scanner.scanFile(pomFile);

        assertEquals("Maven", result.getBuildFileType());
        assertFalse(result.getUsages().isEmpty());
        assertTrue(result.getUsages().stream()
                .anyMatch(u -> u.getArtifactId().equals("jaxb-api")));
    }

    @Test
    void scanFileFallback_shouldParseMavenPomWithJavaxDependency(@TempDir Path tempDir) throws IOException {
        String pomContent = """
            <project>
                <dependencies>
                    <dependency>
                        <groupId>javax.xml.bind</groupId>
                        <artifactId>jaxb-api</artifactId>
                        <version>2.3.1</version>
                    </dependency>
                    <dependency>
                        <groupId>org.example</groupId>
                        <artifactId>normal-lib</artifactId>
                        <version>1.0.0</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        Path pomFile = tempDir.resolve("pom.xml");
        Files.writeString(pomFile, pomContent);

        TransitiveDependencyScannerImpl scanner = new TransitiveDependencyScannerImpl();
        TransitiveDependencyScanResult result = scanner.scanFile(pomFile);

        assertEquals("Maven", result.getBuildFileType());
        assertTrue(result.hasJavaxUsage());

        // Should find jaxb-api but not normal-lib
        assertTrue(result.getUsages().stream()
                .anyMatch(u -> u.getArtifactId().equals("jaxb-api")));
        assertFalse(result.getUsages().stream()
                .anyMatch(u -> u.getArtifactId().equals("normal-lib")));
    }

    @Test
    void scanFileFallback_shouldParseGradleBuildFile(@TempDir Path tempDir) throws IOException {
        String buildGradle = """
            dependencies {
                implementation 'javax.jms:javax.jms-api:2.0.1'
                implementation 'org.example:normal-lib:1.0.0'
            }
            """;
        Path buildFile = tempDir.resolve("build.gradle");
        Files.writeString(buildFile, buildGradle);

        TransitiveDependencyScannerImpl scanner = new TransitiveDependencyScannerImpl();
        TransitiveDependencyScanResult result = scanner.scanFile(buildFile);

        assertEquals("Gradle", result.getBuildFileType());
        assertTrue(result.hasJavaxUsage());
        assertTrue(result.getUsages().stream()
                .anyMatch(u -> u.getArtifactId().equals("javax.jms-api")));
    }

    @Test
    void scanProject_shouldReturnEmptyResultForNullPath() {
        TransitiveDependencyScannerImpl scanner = new TransitiveDependencyScannerImpl();

        TransitiveDependencyProjectScanResult result = scanner.scanProject(null);

        assertNotNull(result);
        assertTrue(result.getFileResults().isEmpty());
        assertEquals(0, result.getTotalBuildFilesScanned());
    }

    @Test
    void scanProject_shouldHandleMissingBuildFiles(@TempDir Path tempDir) throws IOException {
        // Create only non-build files
        Files.createFile(tempDir.resolve("README.md"));
        Files.createFile(tempDir.resolve("src/main/java/Main.java"));
        Files.createDirectory(tempDir.resolve("src/test/resources"));

        TransitiveDependencyScannerImpl scanner = new TransitiveDependencyScannerImpl();
        TransitiveDependencyProjectScanResult result = scanner.scanProject(tempDir);

        assertTrue(result.getFileResults().isEmpty());
        assertEquals(0, result.getTotalBuildFilesScanned());
    }

    @Test
    void scanFile_shouldParsePomWithMultipleDependencyLevels(@TempDir Path tempDir) throws IOException {
        String pomContent = """
            <project>
                <dependencies>
                    <!-- Direct dependency (depth 0) -->
                    <dependency>
                        <groupId>javax.servlet</groupId>
                        <artifactId>javax.servlet-api</artifactId>
                        <version>4.0.1</version>
                        <scope>provided</scope>
                    </dependency>
                    <!-- Another direct dependency -->
                    <dependency>
                        <groupId>org.springframework</groupId>
                        <artifactId>spring-context</artifactId>
                        <version>5.3.21</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        Path pomFile = tempDir.resolve("pom.xml");
        Files.writeString(pomFile, pomContent);

        TransitiveDependencyScannerImpl scanner = new TransitiveDependencyScannerImpl();
        TransitiveDependencyScanResult result = scanner.scanFile(pomFile);

        assertEquals("Maven", result.getBuildFileType());
        // Only javax dependencies are tracked, so only 1 result (javax.servlet-api)
        assertEquals(1, result.getUsages().size());

        // All should be marked as direct dependencies (depth 0)
        assertTrue(result.getUsages().stream()
                .allMatch(u -> u.getDepth() == 0));
    }

    @Test
    void scanFile_shouldDetectJavaxPackagesAtDirectLevel(@TempDir Path tempDir) throws IOException {
        String pomContent = """
            <project>
                <dependencies>
                    <dependency>
                        <groupId>javax.servlet</groupId>
                        <artifactId>javax.servlet-api</artifactId>
                        <version>4.0.1</version>
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
                    <!-- Non-javax dependency -->
                    <dependency>
                        <groupId>org.junit</groupId>
                        <artifactId>junit</artifactId>
                        <version>4.13.2</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        Path pomFile = tempDir.resolve("pom.xml");
        Files.writeString(pomFile, pomContent);

        TransitiveDependencyScannerImpl scanner = new TransitiveDependencyScannerImpl();
        TransitiveDependencyScanResult result = scanner.scanFile(pomFile);

        assertEquals("Maven", result.getBuildFileType());
        assertTrue(result.hasJavaxUsage());

        // Should find 3 javax dependencies
        long javaxCount = result.getUsages().stream()
                .filter(u -> u.getJavaxPackage() != null)
                .count();
        assertEquals(3, javaxCount);
    }

    @Test
    void scanFile_shouldHandlePomWithNoDependencies(@TempDir Path tempDir) throws IOException {
        String pomContent = """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.test</groupId>
                <artifactId>no-deps</artifactId>
                <version>1.0.0</version>
                <!-- No dependencies section -->
            </project>
            """;
        Path pomFile = tempDir.resolve("pom.xml");
        Files.writeString(pomFile, pomContent);

        TransitiveDependencyScannerImpl scanner = new TransitiveDependencyScannerImpl();
        TransitiveDependencyScanResult result = scanner.scanFile(pomFile);

        assertEquals("Maven", result.getBuildFileType());
        assertTrue(result.getUsages().isEmpty());
        assertFalse(result.hasJavaxUsage());
    }

    @Test
    void scanFile_shouldHandleEmptyDependenciesSection(@TempDir Path tempDir) throws IOException {
        String pomContent = """
            <project>
                <dependencies>
                </dependencies>
            </project>
            """;
        Path pomFile = tempDir.resolve("pom.xml");
        Files.writeString(pomFile, pomContent);

        TransitiveDependencyScannerImpl scanner = new TransitiveDependencyScannerImpl();
        TransitiveDependencyScanResult result = scanner.scanFile(pomFile);

        assertEquals("Maven", result.getBuildFileType());
        assertTrue(result.getUsages().isEmpty());
    }

    @Test
    void scanFile_shouldDetectMultipleJavaxPackages(@TempDir Path tempDir) throws IOException {
        String buildGradle = """
            dependencies {
                implementation 'javax.servlet:javax.servlet-api:4.0.1'
                implementation 'javax.jms:javax.jms-api:2.0.1'
                implementation 'javax.xml.bind:jaxb-api:2.3.1'
                implementation 'javax.persistence:javax.persistence-api:2.2'
                implementation 'javax.validation:validation-api:2.0.1.Final'
                implementation 'javax.ws.rs:javax.ws.rs-api:2.1.1'
                // Jakarta EE 9+ dependency (should NOT be flagged)
                implementation 'jakarta.servlet:jakarta.servlet-api:5.0.0'
            }
            """;
        Path buildFile = tempDir.resolve("build.gradle");
        Files.writeString(buildFile, buildGradle);

        TransitiveDependencyScannerImpl scanner = new TransitiveDependencyScannerImpl();
        TransitiveDependencyScanResult result = scanner.scanFile(buildFile);

        assertEquals("Gradle", result.getBuildFileType());
        assertTrue(result.hasJavaxUsage());

        // Should find 6 javax dependencies (not the jakarta one)
        Set<String> javaxArtifacts = Set.of(
            "javax.servlet-api",
            "javax.jms-api",
            "jaxb-api",
            "javax.persistence-api",
            "validation-api",
            "javax.ws.rs-api"
        );

        for (TransitiveDependencyUsage usage : result.getUsages()) {
            assertTrue(javaxArtifacts.contains(usage.getArtifactId()),
                "Unexpected artifact: " + usage.getArtifactId());
        }

        assertEquals(6, result.getUsages().size());
    }

    @Test
    void scanFile_shouldExtractScopeFromDependencies(@TempDir Path tempDir) throws IOException {
        String pomContent = """
            <project>
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
                        <scope>compile</scope>
                    </dependency>
                    <dependency>
                        <groupId>javax.jms</groupId>
                        <artifactId>javax.jms-api</artifactId>
                        <version>2.0.1</version>
                        <scope>runtime</scope>
                    </dependency>
                </dependencies>
            </project>
            """;
        Path pomFile = tempDir.resolve("pom.xml");
        Files.writeString(pomFile, pomContent);

        TransitiveDependencyScannerImpl scanner = new TransitiveDependencyScannerImpl();
        TransitiveDependencyScanResult result = scanner.scanFile(pomFile);

        assertEquals("Maven", result.getBuildFileType());
        assertEquals(3, result.getUsages().size());

        // Verify scopes are correctly extracted
        assertTrue(result.getUsages().stream()
                .anyMatch(u -> u.getArtifactId().equals("javax.servlet-api") && "provided".equals(u.getScope())));
        assertTrue(result.getUsages().stream()
                .anyMatch(u -> u.getArtifactId().equals("jaxb-api") && "compile".equals(u.getScope())));
        assertTrue(result.getUsages().stream()
                .anyMatch(u -> u.getArtifactId().equals("javax.jms-api") && "runtime".equals(u.getScope())));
    }
}
