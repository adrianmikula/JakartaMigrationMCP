package adrianmikula.jakartamigration.integration.dependencyanalysis;

import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.service.impl.MavenDependencyGraphBuilder;
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
 * Integration test for Maven property resolution using real GitHub projects.
 * This test downloads actual projects to verify our property resolution fixes work against real-world code.
 */
@Tag("slow") // Integration test - downloads real projects
class MavenPropertyResolutionIntegrationTest {
    
    private MavenDependencyGraphBuilder dependencyGraphBuilder;
    
    @BeforeEach
    void setUp() {
        dependencyGraphBuilder = new MavenDependencyGraphBuilder();
    }
    
    @Test
    void shouldResolveMavenPropertiesInRealSpringBootProject(@TempDir Path tempDir) throws IOException {
        // Given - Download the actual Spring Boot parent example project from GitHub
        // This project uses Maven properties extensively
        String projectUrl = "https://github.com/EightAugusto/spring-boot-parent-example/archive/refs/heads/main.zip";
        String zipPath = tempDir.resolve("spring-boot-parent-example.zip").toString();
        String extractPath = tempDir.resolve("extracted").toString();
        
        // Download and extract the project
        downloadAndExtractProject(projectUrl, zipPath, extractPath);
        
        Path projectPath = Paths.get(extractPath, "spring-boot-parent-example-main");
        Path pomXmlPath = projectPath.resolve("pom.xml");
        
        // Verify the project was downloaded and has a pom.xml
        assertThat(Files.exists(pomXmlPath)).isTrue();
        
        // When - Build dependency graph from the real project
        DependencyGraph graph = dependencyGraphBuilder.buildFromMaven(pomXmlPath);
        
        // Then - Should resolve Maven properties correctly
        assertThat(graph.nodeCount()).isGreaterThan(1); // Project + dependencies
        
        // Print all dependencies for debugging
        System.out.println("=== Dependencies found in Spring Boot parent example ===");
        graph.getNodes().stream()
            .filter(Artifact::transitive)
            .forEach(node -> {
                System.out.println("  " + node.groupId() + ":" + node.artifactId() + ":" + node.version());
            });
        
        // Verify that dependencies with property references are resolved to actual versions
        // (not left as "${property.name}" or "unknown")
        boolean hasUnresolvedProperties = graph.getNodes().stream()
            .filter(Artifact::transitive)
            .anyMatch(node -> node.version().startsWith("${") || "unknown".equals(node.version()));
        
        assertThat(hasUnresolvedProperties)
            .as("Should not have any dependencies with unresolved property references")
            .isFalse();
    }
    
    @Test
    void shouldResolveMavenPropertiesInRealJ2EESamplesProject(@TempDir Path tempDir) throws IOException {
        // Given - Download the actual J2EE7 Samples project from GitHub
        // This project uses many Maven properties for version management
        String projectUrl = "https://github.com/tgupta018/J2EE7Samples/archive/refs/heads/main.zip";
        String zipPath = tempDir.resolve("J2EE7Samples.zip").toString();
        String extractPath = tempDir.resolve("extracted").toString();
        
        // Download and extract the project
        downloadAndExtractProject(projectUrl, zipPath, extractPath);
        
        Path projectPath = Paths.get(extractPath, "J2EE7Samples-main");
        Path pomXmlPath = projectPath.resolve("pom.xml");
        
        // Verify the project was downloaded and has a pom.xml
        assertThat(Files.exists(pomXmlPath)).isTrue();
        
        // When - Build dependency graph from the real project
        DependencyGraph graph = dependencyGraphBuilder.buildFromMaven(pomXmlPath);
        
        // Then - Should resolve Maven properties correctly
        assertThat(graph.nodeCount()).isGreaterThan(1); // Project + dependencies
        
        // Print all dependencies for debugging
        System.out.println("=== Dependencies found in J2EE7 Samples ===");
        graph.getNodes().stream()
            .filter(Artifact::transitive)
            .forEach(node -> {
                System.out.println("  " + node.groupId() + ":" + node.artifactId() + ":" + node.version());
            });
        
        // Verify that dependencies with property references are resolved to actual versions
        // (not left as "${property.name}" or "unknown")
        boolean hasUnresolvedProperties = graph.getNodes().stream()
            .filter(Artifact::transitive)
            .anyMatch(node -> node.version().startsWith("${") || "unknown".equals(node.version()));
        
        assertThat(hasUnresolvedProperties)
            .as("Should not have any dependencies with unresolved property references")
            .isFalse();
        
        // Specifically check for common Arquillian and server dependencies that should be resolved
        boolean hasArquillianWithVersion = graph.getNodes().stream()
            .filter(Artifact::transitive)
            .anyMatch(node -> 
                node.groupId().equals("org.jboss.arquillian") && 
                !node.version().startsWith("${") && 
                !"unknown".equals(node.version())
            );
        
        assertThat(hasArquillianWithVersion)
            .as("Should have Arquillian dependencies with resolved versions")
            .isTrue();
    }
    
    @Test
    void shouldResolveMavenPropertiesInRealMavenExamplesProject(@TempDir Path tempDir) throws IOException {
        // Given - Download the actual Maven examples project from GitHub
        // This project demonstrates various Maven patterns including property usage
        String projectUrl = "https://github.com/mkyong/maven-examples/archive/refs/heads/master.zip";
        String zipPath = tempDir.resolve("maven-examples.zip").toString();
        String extractPath = tempDir.resolve("extracted").toString();
        
        // Download and extract the project
        downloadAndExtractProject(projectUrl, zipPath, extractPath);
        
        Path projectPath = Paths.get(extractPath, "maven-examples-master");
        
        // Find a sub-project with pom.xml that might use properties
        Path pomXmlPath = findPomWithProperties(projectPath);
        
        if (pomXmlPath != null) {
            // When - Build dependency graph from the real project
            DependencyGraph graph = dependencyGraphBuilder.buildFromMaven(pomXmlPath);
            
            // Then - Should resolve Maven properties correctly
            assertThat(graph.nodeCount()).isGreaterThan(1); // Project + dependencies
            
            // Print all dependencies for debugging
            System.out.println("=== Dependencies found in Maven Examples ===");
            graph.getNodes().stream()
                .filter(Artifact::transitive)
                .forEach(node -> {
                    System.out.println("  " + node.groupId() + ":" + node.artifactId() + ":" + node.version());
                });
            
            // Verify that dependencies with property references are resolved to actual versions
            boolean hasUnresolvedProperties = graph.getNodes().stream()
                .filter(Artifact::transitive)
                .anyMatch(node -> node.version().startsWith("${") || "unknown".equals(node.version()));
            
            assertThat(hasUnresolvedProperties)
                .as("Should not have any dependencies with unresolved property references")
                .isFalse();
        } else {
            System.out.println("No pom.xml with properties found in maven-examples project, skipping test");
        }
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
    
    /**
     * Find a pom.xml file that contains property references
     */
    private Path findPomWithProperties(Path projectPath) throws IOException {
        try (var stream = Files.walk(projectPath)) {
            return stream
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().equals("pom.xml"))
                .filter(path -> {
                    try {
                        String content = Files.readString(path);
                        return content.contains("${") && content.contains("<properties>");
                    } catch (IOException e) {
                        return false;
                    }
                })
                .findFirst()
                .orElse(null);
        }
    }
}
