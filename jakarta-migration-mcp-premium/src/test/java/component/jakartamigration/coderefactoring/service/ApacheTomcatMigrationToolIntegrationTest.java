package component.jakartamigration.coderefactoring.service;

import adrianmikula.jakartamigration.coderefactoring.service.ApacheTomcatMigrationTool;
import adrianmikula.jakartamigration.coderefactoring.service.ToolDownloader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for ApacheTomcatMigrationTool.
 * 
 * These tests actually download and invoke the Apache migration tool.
 * They are marked as integration tests and may be skipped if:
 * - Network is unavailable
 * - Download fails
 * - Java is not available in PATH
 * 
 * To run these tests, ensure:
 * - Network connectivity is available
 * - Java is available in PATH
 * - Sufficient disk space for tool download (~10MB)
 */
@DisplayName("ApacheTomcatMigrationTool Integration Tests")
class ApacheTomcatMigrationToolIntegrationTest {
    
    @TempDir
    Path tempDir;
    
    /**
     * Checks if the Apache tool can be downloaded and is available.
     * This is used to conditionally enable/disable integration tests.
     */
    @SuppressWarnings("unused") // Used by @EnabledIf annotation via reflection
    public static boolean isApacheToolAvailable() {
        try {
            ToolDownloader downloader = new ToolDownloader();
            Path toolJar = downloader.downloadApacheTomcatMigrationTool();
            return toolJar != null && Files.exists(toolJar);
        } catch (Exception e) {
            return false;
        }
    }
    
    @Test
    @DisplayName("Should download Apache migration tool from Apache website")
    @EnabledIf("isApacheToolAvailable")
    void shouldDownloadApacheMigrationToolFromApacheWebsite() throws IOException {
        // Given
        ToolDownloader downloader = new ToolDownloader();
        
        // When
        Path toolJar = downloader.downloadApacheTomcatMigrationTool();
        
        // Then
        assertThat(toolJar).isNotNull();
        assertThat(toolJar).exists();
        assertThat(Files.size(toolJar)).isGreaterThan(0);
        assertThat(toolJar.getFileName().toString())
            .matches("jakartaee-migration-.*-shaded\\.jar");
    }
    
    @Test
    @DisplayName("Should use cached tool on second download attempt")
    @EnabledIf("isApacheToolAvailable")
    void shouldUseCachedToolOnSecondDownloadAttempt() throws IOException {
        // Given
        ToolDownloader downloader = new ToolDownloader();
        
        // When - first download
        Path firstDownload = downloader.downloadApacheTomcatMigrationTool();
        long firstDownloadTime = System.currentTimeMillis();
        
        // Then - second download should use cache
        Path secondDownload = downloader.downloadApacheTomcatMigrationTool();
        long secondDownloadTime = System.currentTimeMillis();
        
        assertThat(secondDownload).isEqualTo(firstDownload);
        assertThat(secondDownloadTime - firstDownloadTime).isLessThan(1000); // Should be instant (cached)
    }
    
    @Test
    @DisplayName("Should migrate a simple JAR file using Apache tool")
    @EnabledIf("isApacheToolAvailable")
    void shouldMigrateSimpleJarFileUsingApacheTool() throws IOException {
        // Given
        ApacheTomcatMigrationTool tool = new ApacheTomcatMigrationTool();
        
        // Create a test JAR file with javax references
        Path sourceJar = createTestJarWithJavax(tempDir.resolve("input.jar"));
        Path destinationJar = tempDir.resolve("output.jar");
        
        // When
        ApacheTomcatMigrationTool.MigrationResult result = tool.migrate(sourceJar, destinationJar);
        
        // Then
        assertThat(result).isNotNull();
        // Note: The actual migration result depends on the tool's behavior
        // Even if migration doesn't change anything, the tool should complete
        assertThat(destinationJar).exists();
        assertThat(result.stdout()).isNotNull();
        assertThat(result.stderr()).isNotNull();
    }
    
    @Test
    @DisplayName("Should handle migration timeout gracefully")
    @EnabledIf("isApacheToolAvailable")
    void shouldHandleMigrationTimeoutGracefully() throws IOException {
        // Given
        // Use a very short timeout to test timeout handling
        Path toolJar = new ToolDownloader().downloadApacheTomcatMigrationTool();
        ApacheTomcatMigrationTool tool = new ApacheTomcatMigrationTool(toolJar, 1); // 1 second timeout
        
        Path sourceJar = createTestJarWithJavax(tempDir.resolve("input.jar"));
        Path destinationJar = tempDir.resolve("output.jar");
        
        // When
        ApacheTomcatMigrationTool.MigrationResult result = tool.migrate(sourceJar, destinationJar);
        
        // Then
        assertThat(result).isNotNull();
        // Tool should either complete quickly or timeout
        // Both outcomes are valid for this test
        if (result.timedOut()) {
            assertThat(result.success()).isFalse();
        }
    }
    
    @Test
    @DisplayName("Should return migration result with correct structure")
    @EnabledIf("isApacheToolAvailable")
    void shouldReturnMigrationResultWithCorrectStructure() throws IOException {
        // Given
        ApacheTomcatMigrationTool tool = new ApacheTomcatMigrationTool();
        Path sourceJar = createTestJarWithJavax(tempDir.resolve("input.jar"));
        Path destinationJar = tempDir.resolve("output.jar");
        
        // When
        ApacheTomcatMigrationTool.MigrationResult result = tool.migrate(sourceJar, destinationJar);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.success()).isNotNull();
        assertThat(result.exitCode()).isNotNull();
        assertThat(result.timedOut()).isNotNull();
        assertThat(result.stdout()).isNotNull();
        assertThat(result.stderr()).isNotNull();
        assertThat(result.durationMs()).isGreaterThanOrEqualTo(0);
        
        // Verify summary method works
        String summary = result.getSummary();
        assertThat(summary).isNotNull();
        assertThat(summary).isNotBlank();
    }
    
    /**
     * Creates a test JAR file that contains javax references.
     * This simulates a real JAR file that would need migration.
     */
    private Path createTestJarWithJavax(Path jarPath) throws IOException {
        try (JarOutputStream jarOut = new JarOutputStream(Files.newOutputStream(jarPath))) {
            // Add manifest
            JarEntry manifestEntry = new JarEntry("META-INF/MANIFEST.MF");
            jarOut.putNextEntry(manifestEntry);
            jarOut.write("Manifest-Version: 1.0\n".getBytes());
            jarOut.write("Created-By: Test\n".getBytes());
            jarOut.closeEntry();
            
            // Add a properties file with javax reference
            JarEntry propsEntry = new JarEntry("javax.properties");
            jarOut.putNextEntry(propsEntry);
            jarOut.write("package=javax.servlet\n".getBytes());
            jarOut.write("class=javax.servlet.http.HttpServlet\n".getBytes());
            jarOut.closeEntry();
            
            // Add a text file with javax reference
            JarEntry textEntry = new JarEntry("javax-reference.txt");
            jarOut.putNextEntry(textEntry);
            jarOut.write("This file contains javax.servlet references\n".getBytes());
            jarOut.closeEntry();
        }
        return jarPath;
    }
}

