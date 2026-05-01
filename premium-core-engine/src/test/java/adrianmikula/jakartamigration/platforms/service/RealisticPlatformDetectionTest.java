package adrianmikula.jakartamigration.platforms.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Realistic platform detection tests using actual GitHub repositories from examples.yaml
 * These tests download real projects to verify our patterns work against real-world code
 */
@Tag("slow") // Integration test - downloads real projects
class RealisticPlatformDetectionTest {
    
    private SimplifiedPlatformDetectionService detectionService;
    
    @BeforeEach
    void setUp() {
        detectionService = new SimplifiedPlatformDetectionService();
    }
    
    @Test
    void shouldDetectSpringFrameworkInRealOAuth2Project(@TempDir Path tempDir) throws IOException {
        // Given - Download the actual Spring OAuth2 example project from GitHub
        String projectUrl = "https://github.com/EAG-es/spring2_oauth2_example/archive/refs/heads/master.zip";
        String zipPath = tempDir.resolve("spring2_oauth2_example.zip").toString();
        String extractPath = tempDir.resolve("extracted").toString();
        
        // Download and extract the project
        downloadAndExtractProject(projectUrl, zipPath, extractPath);
        
        Path projectPath = Paths.get(extractPath, "spring2_oauth2_example-master");
        
        // When - Run platform detection
        var result = detectionService.scanProjectWithArtifacts(projectPath);
        
        // Then - Should detect Spring Framework (this was the original issue)
        assertThat(result.getDetectedPlatforms()).contains("spring");
        assertThat(result.getDetectedPlatforms()).contains("springboot");
        
        // Verify Spring Boot dependencies are detected via common artifacts
        assertThat(result.getPlatformSpecificArtifacts()).containsKey("org.springframework.boot:spring-boot-starter-web");
        assertThat(result.getPlatformSpecificArtifacts()).containsKey("org.springframework.boot:spring-boot-starter-oauth2-client");
    }
    
    @Test
    void shouldDetectSpringFrameworkInRealBootExamplesProject(@TempDir Path tempDir) throws IOException {
        // Given - Download the actual Spring Boot examples project from GitHub
        String projectUrl = "https://github.com/joshlong-attic/boot-examples/archive/refs/heads/main.zip";
        String zipPath = tempDir.resolve("boot-examples.zip").toString();
        String extractPath = tempDir.resolve("extracted").toString();
        
        // Download and extract the project
        downloadAndExtractProject(projectUrl, zipPath, extractPath);
        
        Path projectPath = Paths.get(extractPath, "boot-examples-main");
        
        // When - Run platform detection
        var result = detectionService.scanProjectWithArtifacts(projectPath);
        
        // Then - Should detect Spring Framework
        assertThat(result.getDetectedPlatforms()).contains("springboot");
        assertThat(result.getDetectedPlatforms()).contains("spring");
    }
    
    /**
     * Helper method to download and extract a GitHub project
     */
    private void downloadAndExtractProject(String projectUrl, String zipPath, String extractPath) throws IOException {
        try {
            // Download the zip file
            URL url = new URL(projectUrl);
            Files.copy(url.openStream(), Paths.get(zipPath));
            
            // Extract the zip file
            java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(Paths.get(zipPath).toFile());
            var extractDir = Paths.get(extractPath);
            Files.createDirectories(extractDir);
            
            var entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    Path entryPath = extractDir.resolve(entry.getName());
                    Files.createDirectories(entryPath.getParent());
                    Files.copy(zipFile.getInputStream(entry), entryPath);
                }
            }
            zipFile.close();
            
            // Clean up zip file
            Files.deleteIfExists(Paths.get(zipPath));
            
        } catch (Exception e) {
            System.out.println("Failed to download/extract project: " + e.getMessage());
            // Skip test if download fails
            throw new RuntimeException("Download failed", e);
        }
    }
}
