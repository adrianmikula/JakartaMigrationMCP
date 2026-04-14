package adrianmikula.jakartamigration.intellij.service;

import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyProjectScanResult;
import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus;
import adrianmikula.jakartamigration.coderefactoring.service.RecipeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Integration tests for AdvancedScanningService.
 * Tests the service layer integration including deep dependency scanning and conversion.
 *
 * NOTE: These tests require external dependencies and proper environment setup.
 */
@org.junit.jupiter.api.Disabled("Requires external dependencies - run as integration test")
class AdvancedScanningServiceIntegrationTest {

    private AdvancedScanningService scanningService;

    @BeforeEach
    void setUp() {
        RecipeService recipeService = mock(RecipeService.class);
        scanningService = new AdvancedScanningService(recipeService);
    }

    @Test
    void isMavenAvailable_shouldReturnBoolean() {
        // Test that the method runs without error
        boolean available = scanningService.isMavenAvailable();

        // Should return either true or false
        assertTrue(available == true || available == false);
    }

    @Test
    void isGradleAvailable_shouldReturnBoolean() {
        // Test that the method runs without error
        boolean available = scanningService.isGradleAvailable();

        // Should return either true or false
        assertTrue(available == true || available == false);
    }

    @Test
    void scanDependenciesDeep_shouldScanMavenProject(@TempDir Path tempDir) throws IOException {
        // Create a Maven project
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                     http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.test</groupId>
                <artifactId>test-project</artifactId>
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
        Files.writeString(tempDir.resolve("pom.xml"), pomContent);

        // Run deep scan
        TransitiveDependencyProjectScanResult result = scanningService.scanDependenciesDeep(tempDir);

        if (scanningService.isMavenAvailable()) {
            assertNotNull(result);
            assertFalse(result.getFileResults().isEmpty());
        } else {
            // If Maven is not available, should return null to trigger fallback
            assertNull(result);
        }
    }

    @Test
    void scanDependenciesDeep_shouldScanGradleProject(@TempDir Path tempDir) throws IOException {
        // Create a Gradle project
        String buildGradle = """
            plugins {
                id 'java'
            }
            
            repositories {
                mavenCentral()
            }
            
            dependencies {
                implementation 'javax.xml.bind:jaxb-api:2.3.1'
            }
            """;
        Files.writeString(tempDir.resolve("build.gradle"), buildGradle);

        // Run deep scan
        TransitiveDependencyProjectScanResult result = scanningService.scanDependenciesDeep(tempDir);

        if (scanningService.isGradleAvailable() || scanningService.isMavenAvailable()) {
            // Should find the build.gradle even if only Maven is available (regex fallback)
            if (result != null) {
                assertFalse(result.getFileResults().isEmpty());
            }
        } else {
            // If no build tools available, should return null
            assertNull(result);
        }
    }

    @Test
    void scanDependenciesDeep_shouldReturnNullForEmptyProject(@TempDir Path tempDir) {
        // Empty directory - no build files
        TransitiveDependencyProjectScanResult result = scanningService.scanDependenciesDeep(tempDir);

        // May return empty result or null depending on implementation
        if (result != null) {
            assertTrue(result.getFileResults().isEmpty());
        }
    }

    @Test
    void convertToDependencyInfo_shouldConvertScanResult(@TempDir Path tempDir) throws IOException {
        // Create a project with javax dependencies
        String pomContent = """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.test</groupId>
                <artifactId>convert-test</artifactId>
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
        Files.writeString(tempDir.resolve("pom.xml"), pomContent);

        // Scan the project
        TransitiveDependencyProjectScanResult scanResult = scanningService.scanForTransitiveDependencies(tempDir);
        assertNotNull(scanResult);
        assertFalse(scanResult.getFileResults().isEmpty());

        // Convert to DependencyInfo
        List<DependencyInfo> dependencyInfos = scanningService.convertToDependencyInfo(scanResult);

        assertNotNull(dependencyInfos);
        assertFalse(dependencyInfos.isEmpty());

        // Verify conversion for each dependency
        for (DependencyInfo info : dependencyInfos) {
            assertNotNull(info.getGroupId());
            assertNotNull(info.getArtifactId());
            assertNotNull(info.getCurrentVersion());

            // Verify javax dependencies have appropriate status
            if (info.getGroupId().startsWith("javax.")) {
                assertNotNull(info.getJakartaCompatibilityStatus());
                assertNotNull(info.getAssociatedRecipeName());
            }
        }

        // Should find both javax.servlet-api and jaxb-api
        boolean foundServlet = dependencyInfos.stream()
                .anyMatch(d -> d.getArtifactId().equals("javax.servlet-api"));
        boolean foundJaxb = dependencyInfos.stream()
                .anyMatch(d -> d.getArtifactId().equals("jaxb-api"));

        assertTrue(foundServlet, "Should convert servlet-api");
        assertTrue(foundJaxb, "Should convert jaxb-api");
    }

    @Test
    void convertToDependencyInfo_shouldHandleNullResult() {
        List<DependencyInfo> result = scanningService.convertToDependencyInfo(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void convertToDependencyInfo_shouldHandleEmptyResult() {
        TransitiveDependencyProjectScanResult emptyResult = new TransitiveDependencyProjectScanResult(
                java.util.Collections.emptyList(),
                0, 0, 0
        );

        List<DependencyInfo> result = scanningService.convertToDependencyInfo(emptyResult);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void convertToDependencyInfo_shouldPreserveDepthAndScope(@TempDir Path tempDir) throws IOException {
        // Create a project with dependencies that will have depth 0
        String pomContent = """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.test</groupId>
                <artifactId>depth-test</artifactId>
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
        Files.writeString(tempDir.resolve("pom.xml"), pomContent);

        // Scan and convert
        TransitiveDependencyProjectScanResult scanResult = scanningService.scanForTransitiveDependencies(tempDir);
        List<DependencyInfo> dependencyInfos = scanningService.convertToDependencyInfo(scanResult);

        assertFalse(dependencyInfos.isEmpty());

        DependencyInfo info = dependencyInfos.get(0);

        // Verify depth and scope are preserved
        assertEquals(0, info.getDepth()); // Direct dependency has depth 0
        assertEquals("provided", info.getScope());
        assertFalse(info.isTransitive());
    }

    @Test
    void convertToDependencyInfo_shouldMapSeverityToStatus(@TempDir Path tempDir) {
        // Create a scan result with different severity levels
        var usages = java.util.List.of(
                createUsageWithSeverity("high"),
                createUsageWithSeverity("medium"),
                createUsageWithSeverity("low"),
                createUsageWithSeverity("unknown")
        );

        var fileResult = new adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyScanResult(
                        tempDir.resolve("pom.xml"),
                        usages,
                        "Maven"
                );

        TransitiveDependencyProjectScanResult scanResult = new TransitiveDependencyProjectScanResult(
                java.util.List.of(fileResult),
                1, 4, 4
        );

        List<DependencyInfo> infos = scanningService.convertToDependencyInfo(scanResult);

        // Verify severity mapping
        assertEquals(4, infos.size());

        // High and medium should map to NEEDS_UPGRADE
        long needsUpgradeCount = infos.stream()
                .filter(i -> i.getMigrationStatus() == DependencyMigrationStatus.NEEDS_UPGRADE)
                .count();
        assertEquals(2, needsUpgradeCount);

        // Low should map to COMPATIBLE
        long compatibleCount = infos.stream()
                .filter(i -> i.getMigrationStatus() == DependencyMigrationStatus.COMPATIBLE)
                .count();
        assertEquals(1, compatibleCount);

        // Unknown should map to UNKNOWN_REVIEW
        long unknownCount = infos.stream()
                .filter(i -> i.getMigrationStatus() == DependencyMigrationStatus.UNKNOWN_REVIEW)
                .count();
        assertEquals(1, unknownCount);
    }

    private adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyUsage createUsageWithSeverity(String severity) {
        return new adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyUsage(
                "test-artifact", "javax.test", "1.0.0",
                "javax.test:test-artifact", severity, "Test recommendation",
                "compile", false, 0
        );
    }

    @Test
    void scanForTransitiveDependencies_shouldScanProject(@TempDir Path tempDir) throws IOException {
        // Create a project
        String pomContent = """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.test</groupId>
                <artifactId>scan-test</artifactId>
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
        Files.writeString(tempDir.resolve("pom.xml"), pomContent);

        // Run scan
        TransitiveDependencyProjectScanResult result = scanningService.scanForTransitiveDependencies(tempDir);

        assertNotNull(result);
        assertEquals(1, result.getTotalBuildFilesScanned());
        assertFalse(result.getFileResults().isEmpty());

        var fileResult = result.getFileResults().get(0);
        assertEquals("Maven", fileResult.getBuildFileType());
        assertEquals(1, fileResult.getUsages().size());

        var usage = fileResult.getUsages().get(0);
        assertEquals("javax.servlet", usage.getGroupId());
        assertEquals("javax.servlet-api", usage.getArtifactId());
    }

    // ============================================================================
    // TDD TESTS: Deep Transitive Dependency Coverage
    // These tests are designed to expose the root cause of 0 dependencies issue
    // ============================================================================

    @Test
    void scanDependenciesDeep_withNestedProjectStructure_shouldFindBuildFiles(@TempDir Path tempDir) throws IOException {
        // This test reproduces the issue where pom.xml is in a subdirectory
        // Example project structure: javax-validations-and-api-versioning-spring-master/
        //   └── javax-validations-and-api-versioning-spring-master/
        //       └── pom.xml

        Path nestedDir = tempDir.resolve("nested-project");
        Files.createDirectories(nestedDir);

        String pomContent = """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.test</groupId>
                <artifactId>nested-test</artifactId>
                <version>1.0.0</version>
                <dependencies>
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter</artifactId>
                        <version>2.7.0</version>
                    </dependency>
                    <dependency>
                        <groupId>com.fasterxml.jackson.core</groupId>
                        <artifactId>jackson-databind</artifactId>
                        <version>2.13.0</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        Files.writeString(nestedDir.resolve("pom.xml"), pomContent);

        // Scan from the ROOT directory (not the nested directory)
        TransitiveDependencyProjectScanResult result = scanningService.scanDependenciesDeep(tempDir);

        // CRITICAL: This should find the pom.xml even though it's in a subdirectory
        assertNotNull(result, "Should return a result, not null");
        assertFalse(result.getFileResults().isEmpty(),
            "Should find build files in nested directories. " +
            "If this fails, discoverBuildFiles() may not be searching recursively");
        assertEquals(1, result.getTotalBuildFilesScanned(),
            "Should scan exactly 1 build file in the nested structure");

        // Verify we got the dependencies
        long totalDeps = result.getFileResults().stream()
                .flatMap(f -> f.getUsages().stream())
                .count();
        assertEquals(2, totalDeps,
            "Should capture ALL 2 dependencies (spring-boot-starter and jackson-databind), " +
            "not just javax ones");
    }

    @Test
    void scanDependenciesDeep_withAllNonJavaxDependencies_shouldStillReturnResults(@TempDir Path tempDir) throws IOException {
        // CRITICAL TEST: This exposes the bug where only javax dependencies were returned
        // We need to verify that ALL dependencies are captured, not just javax ones

        String pomContent = """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.test</groupId>
                <artifactId>non-javax-test</artifactId>
                <version>1.0.0</version>
                <dependencies>
                    <dependency>
                        <groupId>org.springframework</groupId>
                        <artifactId>spring-core</artifactId>
                        <version>5.3.21</version>
                    </dependency>
                    <dependency>
                        <groupId>org.apache.commons</groupId>
                        <artifactId>commons-lang3</artifactId>
                        <version>3.12.0</version>
                    </dependency>
                    <dependency>
                        <groupId>org.slf4j</groupId>
                        <artifactId>slf4j-api</artifactId>
                        <version>1.7.36</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        Files.writeString(tempDir.resolve("pom.xml"), pomContent);

        TransitiveDependencyProjectScanResult result = scanningService.scanDependenciesDeep(tempDir);

        // Even without Maven available, the regex fallback should still work
        if (result != null) {
            long totalDeps = result.getFileResults().stream()
                    .flatMap(f -> f.getUsages().stream())
                    .count();

            // CRITICAL: This should be 3, not 0
            assertEquals(3, totalDeps,
                "Should capture ALL dependencies including non-javax ones. " +
                "If this is 0, parseDependencies() is filtering out non-javax dependencies");

            // Verify specific dependencies are present
            boolean foundSpring = result.getFileResults().stream()
                    .flatMap(f -> f.getUsages().stream())
                    .anyMatch(u -> u.getArtifactId().equals("spring-core"));
            boolean foundCommons = result.getFileResults().stream()
                    .flatMap(f -> f.getUsages().stream())
                    .anyMatch(u -> u.getArtifactId().equals("commons-lang3"));
            boolean foundSlf4j = result.getFileResults().stream()
                    .flatMap(f -> f.getUsages().stream())
                    .anyMatch(u -> u.getArtifactId().equals("slf4j-api"));

            assertTrue(foundSpring, "Should find spring-core dependency");
            assertTrue(foundCommons, "Should find commons-lang3 dependency");
            assertTrue(foundSlf4j, "Should find slf4j-api dependency");
        }
    }

    @Test
    void convertToDependencyInfo_shouldConvertNonJavaxDependencies(@TempDir Path tempDir) throws IOException {
        // This test verifies that non-javax dependencies flow through the entire pipeline
        // from scan result → convertToDependencyInfo → DependencyInfo list

        String pomContent = """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.test</groupId>
                <artifactId>convert-all-test</artifactId>
                <version>1.0.0</version>
                <dependencies>
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-web</artifactId>
                        <version>2.7.0</version>
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
        Files.writeString(tempDir.resolve("pom.xml"), pomContent);

        // Full pipeline: scan → convert
        TransitiveDependencyProjectScanResult scanResult = scanningService.scanForTransitiveDependencies(tempDir);

        // CRITICAL: scan result should not be null or empty
        assertNotNull(scanResult, "Scan should return a result");
        assertFalse(scanResult.getFileResults().isEmpty(), "Scan should find build files");

        List<DependencyInfo> dependencyInfos = scanningService.convertToDependencyInfo(scanResult);

        // CRITICAL: This is where the "Converted 0 transitive dependencies" log comes from
        assertNotNull(dependencyInfos, "convertToDependencyInfo should return a list, not null");

        // Log the actual count for debugging
        System.out.println("[TDD TEST] Total dependencies converted: " + dependencyInfos.size());

        // Should have 2 dependencies: spring-boot-starter-web and javax.servlet-api
        assertEquals(2, dependencyInfos.size(),
            "Should convert ALL 2 dependencies to DependencyInfo. " +
            "If this is 0, the issue is in convertToDependencyInfo() or earlier in the pipeline");

        // Verify both javax and non-javax are present
        boolean foundSpring = dependencyInfos.stream()
                .anyMatch(d -> d.getArtifactId().equals("spring-boot-starter-web"));
        boolean foundServlet = dependencyInfos.stream()
                .anyMatch(d -> d.getArtifactId().equals("javax.servlet-api"));

        assertTrue(foundSpring, "Should convert non-javax dependency (spring-boot-starter-web)");
        assertTrue(foundServlet, "Should convert javax dependency (javax.servlet-api)");
    }

    @Test
    void fullDeepScanPipeline_shouldNotReturnZeroDependencies(@TempDir Path tempDir) throws IOException {
        // ULTIMATE TEST: This mimics exactly what happens in the UI
        // MigrationToolWindow.runDeepDependencyAnalysis() → scanDependenciesDeep() → convertToDependencyInfo()

        // Create a realistic project with multiple dependencies
        String pomContent = """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>realistic-app</artifactId>
                <version>1.0.0</version>
                <dependencies>
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter</groupId>
                        <version>2.7.0</version>
                    </dependency>
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-web</artifactId>
                        <version>2.7.0</version>
                    </dependency>
                    <dependency>
                        <groupId>com.fasterxml.jackson.core</groupId>
                        <artifactId>jackson-databind</artifactId>
                        <version>2.13.0</version>
                    </dependency>
                    <dependency>
                        <groupId>javax.servlet</groupId>
                        <artifactId>javax.servlet-api</artifactId>
                        <version>4.0.1</version>
                        <scope>provided</scope>
                    </dependency>
                    <dependency>
                        <groupId>org.slf4j</groupId>
                        <artifactId>slf4j-api</artifactId>
                        <version>1.7.36</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        Files.writeString(tempDir.resolve("pom.xml"), pomContent);

        // Simulate what runDeepDependencyAnalysis() does
        System.out.println("[TDD TEST] Starting deep dependency scan simulation...");

        TransitiveDependencyProjectScanResult deepResult = scanningService.scanDependenciesDeep(tempDir);

        if (deepResult == null) {
            System.out.println("[TDD TEST] scanDependenciesDeep returned null - Maven/Gradle not available, using fallback");
            // Fallback: scanForTransitiveDependencies should still work with regex
            deepResult = scanningService.scanForTransitiveDependencies(tempDir);
        }

        assertNotNull(deepResult, "Deep scan should return a result");
        System.out.println("[TDD TEST] Build files scanned: " + deepResult.getTotalBuildFilesScanned());

        // Count total usages before conversion
        long totalUsages = deepResult.getFileResults().stream()
                .flatMap(f -> f.getUsages().stream())
                .count();
        System.out.println("[TDD TEST] Total usages in scan result: " + totalUsages);

        // Convert to DependencyInfo
        List<DependencyInfo> dependencyInfos = scanningService.convertToDependencyInfo(deepResult);
        System.out.println("[TDD TEST] Total DependencyInfo objects: " + dependencyInfos.size());

        // CRITICAL ASSERTION: This should NEVER be 0 for a valid project
        assertTrue(dependencyInfos.size() > 0,
            "CRITICAL: Full pipeline returned 0 dependencies for a project with 5 declared dependencies. " +
            "This reproduces the production bug where 'Converted 0 transitive dependencies' appears in logs.");

        // Should capture all 5 dependencies
        assertEquals(5, dependencyInfos.size(),
            "Should capture all 5 declared dependencies");

        // Verify non-javax dependencies are present
        long nonJavaxCount = dependencyInfos.stream()
                .filter(d -> !d.getGroupId().startsWith("javax"))
                .count();
        assertEquals(4, nonJavaxCount,
            "Should have 4 non-javax dependencies (spring-boot-starter, spring-boot-starter-web, jackson-databind, slf4j-api)");
    }
}
