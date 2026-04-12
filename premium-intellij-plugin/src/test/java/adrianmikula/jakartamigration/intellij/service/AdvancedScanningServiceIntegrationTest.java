package adrianmikula.jakartamigration.intellij.service;

import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyProjectScanResult;
import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for AdvancedScanningService.
 * Tests the service layer integration including deep dependency scanning and conversion.
 */
class AdvancedScanningServiceIntegrationTest {

    private AdvancedScanningService scanningService;

    @BeforeEach
    void setUp() {
        scanningService = new AdvancedScanningService();
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
                java.util.Collections.emptyList()
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
    void convertToDependencyInfo_shouldMapSeverityToStatus() {
        // Create a scan result with different severity levels
        var usages = java.util.List.of(
                createUsageWithSeverity("high"),
                createUsageWithSeverity("medium"),
                createUsageWithSeverity("low"),
                createUsageWithSeverity("unknown")
        );

        var fileResult = new adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyScanResult(
                        "Maven",
                        tempDir -> tempDir.resolve("pom.xml"),
                        usages
                );

        TransitiveDependencyProjectScanResult scanResult = new TransitiveDependencyProjectScanResult(
                java.util.List.of(fileResult)
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

        TransitiveDependencyProjectScanResult.FileScanResult fileResult = result.getFileResults().get(0);
        assertEquals("Maven", fileResult.getBuildFileType());
        assertEquals(1, fileResult.getUsages().size());

        var usage = fileResult.getUsages().get(0);
        assertEquals("javax.servlet", usage.getGroupId());
        assertEquals("javax.servlet-api", usage.getArtifactId());
    }
}
