package integration.mcp;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class for creating and managing test projects in MCP integration tests.
 * This eliminates duplication across multiple test classes.
 */
final class TestProjectHelper {

    private TestProjectHelper() {
        // Utility class - prevent instantiation
    }

    /**
     * Creates a minimal test project with a basic pom.xml containing javax.servlet dependency.
     *
     * @return Path to the created test project
     * @throws RuntimeException if project creation fails
     */
    static Path createTestProject() {
        try {
            Path testProject = Paths.get(System.getProperty("java.io.tmpdir"), "mcp-test-project-" + System.currentTimeMillis());
            testProject.toFile().mkdirs();

            Path pomXml = testProject.resolve("pom.xml");
            Files.writeString(pomXml, """
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
                """);

            return testProject;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test project", e);
        }
    }

    /**
     * Deletes a test project directory and all its contents.
     *
     * @param testProject Path to the test project to delete
     */
    static void deleteTestProject(Path testProject) {
        if (testProject == null) {
            return;
        }
        try {
            File file = testProject.toFile();
            if (file.exists()) {
                deleteRecursively(file);
            }
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    private static void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }
}
