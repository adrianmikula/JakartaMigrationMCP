package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.DependencyTreeResult;
import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyUsage;
import adrianmikula.jakartamigration.advancedscanning.service.DependencyDeduplicationService;
import adrianmikula.jakartamigration.advancedscanning.service.DependencyTreeCommandExecutor;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("slow")
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

        // ALL 2 dependencies should be captured now (not just javax ones)
        assertEquals(2, result.getUsages().size(), "Should capture ALL dependencies including non-javax");

        // Should find jaxb-api with high severity
        assertTrue(result.getUsages().stream()
                .anyMatch(u -> u.getArtifactId().equals("jaxb-api") && "high".equals(u.getSeverity())));

        // Should also find normal-lib with low severity (not filtered out anymore)
        assertTrue(result.getUsages().stream()
                .anyMatch(u -> u.getArtifactId().equals("normal-lib") && "low".equals(u.getSeverity())));
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

        // ALL 2 dependencies should be captured now (not just javax ones)
        assertEquals(2, result.getUsages().size(), "Should capture ALL dependencies including non-javax");

        // Should find javax.jms-api with high severity
        assertTrue(result.getUsages().stream()
                .anyMatch(u -> u.getArtifactId().equals("javax.jms-api") && "high".equals(u.getSeverity())));

        // Should also find normal-lib with low severity (not filtered out anymore)
        assertTrue(result.getUsages().stream()
                .anyMatch(u -> u.getArtifactId().equals("normal-lib") && "low".equals(u.getSeverity())));
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
        Files.createDirectories(tempDir.resolve("src/main/java"));
        Files.createFile(tempDir.resolve("src/main/java/Main.java"));
        Files.createDirectories(tempDir.resolve("src/test/resources"));

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
        // ALL dependencies are now captured, not just javax ones
        assertEquals(2, result.getUsages().size(), "Should capture both javax.servlet-api and spring-context");

        // Verify both dependencies are present
        assertTrue(result.getUsages().stream().anyMatch(u -> u.getArtifactId().equals("javax.servlet-api")));
        assertTrue(result.getUsages().stream().anyMatch(u -> u.getArtifactId().equals("spring-context")));

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

        // ALL 4 dependencies should be captured now
        assertEquals(4, result.getUsages().size(), "Should capture ALL dependencies including non-javax ones");

        // Should find 3 javax dependencies with javaxPackage field set
        long javaxCount = result.getUsages().stream()
                .filter(u -> u.getJavaxPackage() != null)
                .count();
        assertEquals(3, javaxCount, "Should have 3 javax dependencies with javaxPackage info");

        // Should find 1 non-javax dependency (junit)
        long nonJavaxCount = result.getUsages().stream()
                .filter(u -> u.getJavaxPackage() == null)
                .count();
        assertEquals(1, nonJavaxCount, "Should have 1 non-javax dependency");
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
                // Jakarta EE 9+ dependency (also captured now with "low" severity)
                implementation 'jakarta.servlet:jakarta.servlet-api:5.0.0'
            }
            """;
        Path buildFile = tempDir.resolve("build.gradle");
        Files.writeString(buildFile, buildGradle);

        TransitiveDependencyScannerImpl scanner = new TransitiveDependencyScannerImpl();
        TransitiveDependencyScanResult result = scanner.scanFile(buildFile);

        assertEquals("Gradle", result.getBuildFileType());
        assertTrue(result.hasJavaxUsage());

        // ALL 7 dependencies are now captured (6 javax + 1 jakarta)
        assertEquals(7, result.getUsages().size(), "Should capture ALL dependencies including jakarta");

        // Should find 6 javax dependencies with high severity and javaxPackage set
        long javaxCount = result.getUsages().stream()
                .filter(u -> u.getJavaxPackage() != null)
                .count();
        assertEquals(6, javaxCount, "Should have 6 javax dependencies with javaxPackage info");

        // Should find 1 jakarta dependency with low severity and no javaxPackage
        long jakartaCount = result.getUsages().stream()
                .filter(u -> u.getArtifactId().equals("jakarta.servlet-api"))
                .filter(u -> u.getJavaxPackage() == null)
                .filter(u -> "low".equals(u.getSeverity()))
                .count();
        assertEquals(1, jakartaCount, "Should have 1 jakarta dependency with low severity");
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

    @Test
    void scanFileFallback_shouldCaptureAllDependencies_NotJustJavax(@TempDir Path tempDir) throws IOException {
        // This test verifies the fix for the bug where only javax dependencies were captured
        String pomContent = """
            <project>
                <dependencies>
                    <dependency>
                        <groupId>javax.servlet</groupId>
                        <artifactId>javax.servlet-api</artifactId>
                        <version>4.0.1</version>
                    </dependency>
                    <dependency>
                        <groupId>org.springframework</groupId>
                        <artifactId>spring-core</artifactId>
                        <version>5.3.21</version>
                    </dependency>
                    <dependency>
                        <groupId>com.fasterxml.jackson.core</groupId>
                        <artifactId>jackson-databind</artifactId>
                        <version>2.13.0</version>
                    </dependency>
                    <dependency>
                        <groupId>org.slf4j</groupId>
                        <artifactId>slf4j-api</artifactId>
                        <version>1.7.36</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        Path pomFile = tempDir.resolve("pom.xml");
        Files.writeString(pomFile, pomContent);

        TransitiveDependencyScannerImpl scanner = new TransitiveDependencyScannerImpl();
        TransitiveDependencyScanResult result = scanner.scanFile(pomFile);

        assertEquals("Maven", result.getBuildFileType());

        // Should capture ALL 4 dependencies, not just the javax one
        assertEquals(4, result.getUsages().size(), "Should capture all dependencies, not just javax ones");

        // Verify all dependencies are present
        assertTrue(result.getUsages().stream()
                .anyMatch(u -> u.getArtifactId().equals("javax.servlet-api")));
        assertTrue(result.getUsages().stream()
                .anyMatch(u -> u.getArtifactId().equals("spring-core")));
        assertTrue(result.getUsages().stream()
                .anyMatch(u -> u.getArtifactId().equals("jackson-databind")));
        assertTrue(result.getUsages().stream()
                .anyMatch(u -> u.getArtifactId().equals("slf4j-api")));

        // Verify non-javax dependencies have 'low' severity
        TransitiveDependencyUsage springDep = result.getUsages().stream()
                .filter(u -> u.getArtifactId().equals("spring-core"))
                .findFirst()
                .orElseThrow();
        assertEquals("low", springDep.getSeverity());

        // Verify javax dependencies have 'high' severity
        TransitiveDependencyUsage javaxDep = result.getUsages().stream()
                .filter(u -> u.getArtifactId().equals("javax.servlet-api"))
                .findFirst()
                .orElseThrow();
        assertEquals("high", javaxDep.getSeverity());
    }

    @Test
    void scanFile_shouldCaptureDeepTransitiveDependenciesWithCorrectDepth() throws IOException {
        // Create mock executor that simulates deep transitive dependencies from Maven/Gradle
        DependencyTreeCommandExecutor mockExecutor = mock(DependencyTreeCommandExecutor.class);

        // Create a dependency tree with multiple depth levels:
        // Level 0: project (direct)
        // Level 1: spring-boot-starter (direct)
        // Level 2: spring-core (transitive, depth 1)
        // Level 3: jackson-databind (transitive, depth 2)
        // Level 4: jackson-core (transitive, depth 3)
        List<DependencyTreeResult.DependencyNode> dependencies = Arrays.asList(
            new DependencyTreeResult.DependencyNode("org.springframework.boot", "spring-boot-starter", "2.7.0", "compile", 0, false),
            new DependencyTreeResult.DependencyNode("org.springframework", "spring-core", "5.3.21", "compile", 1, true),
            new DependencyTreeResult.DependencyNode("com.fasterxml.jackson.core", "jackson-databind", "2.13.0", "compile", 2, true),
            new DependencyTreeResult.DependencyNode("com.fasterxml.jackson.core", "jackson-core", "2.13.0", "compile", 3, true),
            new DependencyTreeResult.DependencyNode("javax.servlet", "javax.servlet-api", "4.0.1", "provided", 2, true)
        );

        DependencyTreeResult treeResult = new DependencyTreeResult(dependencies, Set.of("compile", "provided"));
        when(mockExecutor.executeMavenDependencyTreeAsync(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(treeResult));

        DependencyDeduplicationService dedupService = new DependencyDeduplicationServiceImpl();
        TransitiveDependencyScannerImpl scanner = new TransitiveDependencyScannerImpl(mockExecutor, dedupService);

        // Create a temporary pom.xml (content doesn't matter as we're mocking the executor)
        Path tempDir = Files.createTempDirectory("test");
        Path pomFile = tempDir.resolve("pom.xml");
        Files.writeString(pomFile, "<project></project>");

        TransitiveDependencyScanResult result = scanner.scanFile(pomFile);

        // Verify all 5 dependencies are captured with correct depths
        assertEquals(5, result.getUsages().size(), "Should capture all transitive dependencies at all depths");

        // Verify depth tracking
        assertTrue(result.getUsages().stream()
                .anyMatch(u -> u.getArtifactId().equals("spring-boot-starter") && u.getDepth() == 0));
        assertTrue(result.getUsages().stream()
                .anyMatch(u -> u.getArtifactId().equals("spring-core") && u.getDepth() == 1));
        assertTrue(result.getUsages().stream()
                .anyMatch(u -> u.getArtifactId().equals("jackson-databind") && u.getDepth() == 2));
        assertTrue(result.getUsages().stream()
                .anyMatch(u -> u.getArtifactId().equals("jackson-core") && u.getDepth() == 3));
        assertTrue(result.getUsages().stream()
                .anyMatch(u -> u.getArtifactId().equals("javax.servlet-api") && u.getDepth() == 2));

        // Cleanup
        Files.deleteIfExists(pomFile);
        Files.deleteIfExists(tempDir);
    }

    @Test
    void scanFile_shouldCaptureMixedJavaxAndNonJavaxDependencies(@TempDir Path tempDir) throws IOException {
        // Test that verifies both javax and non-javax dependencies are properly captured
        String buildGradle = """
            dependencies {
                implementation 'javax.servlet:javax.servlet-api:4.0.1'
                implementation 'org.apache.commons:commons-lang3:3.12.0'
                implementation 'javax.persistence:javax.persistence-api:2.2'
                implementation 'org.slf4j:slf4j-api:1.7.36'
                testImplementation 'junit:junit:4.13.2'
            }
            """;
        Path buildFile = tempDir.resolve("build.gradle");
        Files.writeString(buildFile, buildGradle);

        TransitiveDependencyScannerImpl scanner = new TransitiveDependencyScannerImpl();
        TransitiveDependencyScanResult result = scanner.scanFile(buildFile);

        assertEquals("Gradle", result.getBuildFileType());

        // Should capture all 5 dependencies (2 javax + 3 non-javax)
        assertEquals(5, result.getUsages().size(), "Should capture both javax and non-javax dependencies");

        // Count by type
        long javaxCount = result.getUsages().stream()
                .filter(u -> u.getGroupId().startsWith("javax"))
                .count();
        long nonJavaxCount = result.getUsages().stream()
                .filter(u -> !u.getGroupId().startsWith("javax"))
                .count();

        assertEquals(2, javaxCount, "Should have 2 javax dependencies");
        assertEquals(3, nonJavaxCount, "Should have 3 non-javax dependencies");

        // Verify all are present
        assertTrue(result.getUsages().stream().anyMatch(u -> u.getArtifactId().equals("javax.servlet-api")));
        assertTrue(result.getUsages().stream().anyMatch(u -> u.getArtifactId().equals("commons-lang3")));
        assertTrue(result.getUsages().stream().anyMatch(u -> u.getArtifactId().equals("javax.persistence-api")));
        assertTrue(result.getUsages().stream().anyMatch(u -> u.getArtifactId().equals("slf4j-api")));
        assertTrue(result.getUsages().stream().anyMatch(u -> u.getArtifactId().equals("junit")));
    }

    @Test
    void scanFile_shouldCorrectlyIdentifyDirectVsTransitive(@TempDir Path tempDir) throws IOException {
        // Test that direct dependencies (depth 0) are correctly identified as non-transitive
        String pomContent = """
            <project>
                <dependencies>
                    <!-- Direct dependencies -->
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter</artifactId>
                        <version>2.7.0</version>
                    </dependency>
                    <dependency>
                        <groupId>org.apache.commons</groupId>
                        <artifactId>commons-lang3</artifactId>
                        <version>3.12.0</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        Path pomFile = tempDir.resolve("pom.xml");
        Files.writeString(pomFile, pomContent);

        TransitiveDependencyScannerImpl scanner = new TransitiveDependencyScannerImpl();
        TransitiveDependencyScanResult result = scanner.scanFile(pomFile);

        // All dependencies should have depth 0 and isTransitive=false (direct dependencies)
        assertTrue(result.getUsages().stream().allMatch(u -> u.getDepth() == 0));
        assertTrue(result.getUsages().stream().noneMatch(TransitiveDependencyUsage::isTransitive));
    }

    @Test
    void scanProject_shouldAggregateAllDependenciesFromMultipleFiles(@TempDir Path tempDir) throws IOException {
        // Create a project with multiple build files
        Path module1 = tempDir.resolve("module1");
        Path module2 = tempDir.resolve("module2");
        Files.createDirectories(module1);
        Files.createDirectories(module2);

        // Module 1: Maven with mixed dependencies
        String module1Pom = """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <artifactId>module1</artifactId>
                <dependencies>
                    <dependency>
                        <groupId>javax.servlet</groupId>
                        <artifactId>javax.servlet-api</artifactId>
                        <version>4.0.1</version>
                    </dependency>
                    <dependency>
                        <groupId>org.springframework</groupId>
                        <artifactId>spring-core</artifactId>
                        <version>5.3.21</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        Files.writeString(module1.resolve("pom.xml"), module1Pom);

        // Module 2: Gradle with mixed dependencies
        String module2Gradle = """
            dependencies {
                implementation 'javax.persistence:javax.persistence-api:2.2'
                implementation 'com.fasterxml.jackson.core:jackson-databind:2.13.0'
            }
            """;
        Files.writeString(module2.resolve("build.gradle"), module2Gradle);

        TransitiveDependencyScannerImpl scanner = new TransitiveDependencyScannerImpl();
        TransitiveDependencyProjectScanResult result = scanner.scanProject(tempDir);

        // Should scan both files
        assertEquals(2, result.getTotalBuildFilesScanned());

        // Collect all usages
        long totalUsages = result.getFileResults().stream()
                .mapToLong(r -> r.getUsages().size())
                .sum();

        // Should have 4 total: 2 from each module
        assertEquals(4, totalUsages, "Should aggregate all dependencies from all modules");

        // Verify both javax and non-javax from both modules
        boolean foundServlet = result.getFileResults().stream()
                .flatMap(r -> r.getUsages().stream())
                .anyMatch(u -> u.getArtifactId().equals("javax.servlet-api"));
        boolean foundSpring = result.getFileResults().stream()
                .flatMap(r -> r.getUsages().stream())
                .anyMatch(u -> u.getArtifactId().equals("spring-core"));
        boolean foundPersistence = result.getFileResults().stream()
                .flatMap(r -> r.getUsages().stream())
                .anyMatch(u -> u.getArtifactId().equals("javax.persistence-api"));
        boolean foundJackson = result.getFileResults().stream()
                .flatMap(r -> r.getUsages().stream())
                .anyMatch(u -> u.getArtifactId().equals("jackson-databind"));

        assertTrue(foundServlet && foundSpring && foundPersistence && foundJackson,
            "Should find all dependencies from all modules");
    }
}
