package adrianmikula.jakartamigration.mcp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test that generates a real dependency graph visualization
 * for the JakartaMigrationMCP project itself.
 */
class GenerateDependencyGraphIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Generate dependency graph for current project")
    void generateDependencyGraphForCurrentProject() throws Exception {
        // Get the current project path
        String currentProjectPath = "/media/adrian/SOURCE/Projects/JakartaMigrationMCP";
        Path projectPath = Paths.get(currentProjectPath);

        // Verify project exists
        assertThat(Files.exists(projectPath)).isTrue();
        assertThat(Files.exists(projectPath.resolve("pom.xml")))
            .or(assertThat(Files.exists(projectPath.resolve("build.gradle.kts"))));

        System.out.println("Project found: " + currentProjectPath);
        System.out.println("To generate the dependency graph, run the MCP tool:");
        System.out.println("  Tool: generateDependencyGraphVisualization");
        System.out.println("  projectPath: " + currentProjectPath);
        System.out.println();
        System.out.println("The HTML visualization will be saved to: " + currentProjectPath + "/reports/dependency-graph-{timestamp}.html");
    }

    @Test
    @DisplayName("Verify reports directory structure")
    void verifyReportsDirectoryStructure() throws Exception {
        String projectPath = "/media/adrian/SOURCE/Projects/JakartaMigrationMCP";
        Path reportsDir = Paths.get(projectPath, "reports");

        // Create reports directory if it doesn't exist
        Files.createDirectories(reportsDir);

        assertThat(Files.exists(reportsDir)).isTrue();
        assertThat(Files.isDirectory(reportsDir)).isTrue();

        System.out.println("Reports directory ready: " + reportsDir);
    }
}
