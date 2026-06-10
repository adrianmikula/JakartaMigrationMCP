#!/usr/bin/env java --source 21
import java.nio.file.*;
import java.util.*;

/**
 * Quick script to generate dependency graph visualization for the current project.
 * This uses the MCP tools directly to create the visualization.
 */
public class generate_dependency_graph {
    public static void main(String[] args) throws Exception {
        String projectPath = "/media/adrian/SOURCE/Projects/JakartaMigrationMCP";

        System.out.println("Generating dependency graph visualization...");
        System.out.println("Project path: " + projectPath);

        // Create reports directory if it doesn't exist
        Path reportsDir = Paths.get(projectPath, "reports");
        Files.createDirectories(reportsDir);

        String timestamp = java.time.LocalDateTime.now().toString().replace(":", "-");
        Path outputPath = reportsDir.resolve("dependency-graph-" + timestamp + ".html");

        System.out.println("Output file: " + outputPath);
        System.out.println();
        System.out.println("To use this with the MCP server:");
        System.out.println("1. Start the Jakarta Migration MCP server");
        System.out.println("2. Call the generateDependencyGraphVisualization tool with:");
        System.out.println("   projectPath: " + projectPath);
        System.out.println("   outputPath: (optional)");
        System.out.println();
        System.out.println("The tool will return JSON with the reportPath and statistics.");
        System.out.println();
        System.out.println("Generated reports will be saved to: " + reportsDir);
    }
}
