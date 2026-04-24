package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphException;
import adrianmikula.jakartamigration.dependencyanalysis.service.impl.MavenDependencyGraphBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Eclipse project handling without requiring full IntelliJ Platform.
 * These tests verify that the "No build file found" error is handled gracefully.
 */
public class ReportsTabComponentEclipseTest {
    
    @TempDir
    Path tempDir;
    
    private MavenDependencyGraphBuilder dependencyGraphBuilder;
    
    @BeforeEach
    void setUp() {
        dependencyGraphBuilder = new MavenDependencyGraphBuilder();
    }
    
    @Test
    void testEclipseProjectWithoutBuildFilesThrowsDependencyGraphException() {
        // Arrange - Create an empty directory (simulating Eclipse project without build files)
        Path eclipseProjectDir = tempDir.resolve("eclipse-project");
        
        // Act & Assert - Should throw DependencyGraphException with "No build file found" message
        DependencyGraphException exception = assertThrows(DependencyGraphException.class, () -> {
            dependencyGraphBuilder.buildFromProject(eclipseProjectDir);
        });
        
        // Assert - Verify the error message contains "No build file found"
        assertTrue(exception.getMessage().contains("No build file found"));
        assertTrue(exception.getMessage().contains(eclipseProjectDir.toString()));
    }
    
    @Test
    void testEclipseProjectWithOnlyClasspathFileStillThrowsException() throws IOException {
        // Arrange - Create an Eclipse project with only .classpath file
        Path eclipseProjectDir = tempDir.resolve("eclipse-project");
        Files.createDirectories(eclipseProjectDir);
        
        // Create a .classpath file (typical Eclipse project file)
        Path classpathFile = eclipseProjectDir.resolve(".classpath");
        String classpathContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<classpath>\n" +
            "    <classpathentry kind=\"src\" path=\"src\"/>\n" +
            "    <classpathentry kind=\"con\" path=\"org.eclipse.jdt.launching.JRE_CONTAINER\"/>\n" +
            "    <classpathentry kind=\"output\" path=\"build/classes\"/>\n" +
            "</classpath>";
        Files.writeString(classpathFile, classpathContent);
        
        // Act & Assert - Should still throw DependencyGraphException
        DependencyGraphException exception = assertThrows(DependencyGraphException.class, () -> {
            dependencyGraphBuilder.buildFromProject(eclipseProjectDir);
        });
        
        // Assert - Verify the error message
        assertTrue(exception.getMessage().contains("No build file found"));
    }
    
    @Test
    void testProjectWithMavenPomWorks() throws IOException {
        // Arrange - Create a project with pom.xml
        Path mavenProjectDir = tempDir.resolve("maven-project");
        Files.createDirectories(mavenProjectDir);
        
        // Create a minimal pom.xml file
        Path pomFile = mavenProjectDir.resolve("pom.xml");
        String pomContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<project xmlns=\"http://maven.apache.org/POM/4.0.0\">\n" +
            "    <modelVersion>4.0.0</modelVersion>\n" +
            "    <groupId>com.example</groupId>\n" +
            "    <artifactId>test-project</artifactId>\n" +
            "    <version>1.0.0</version>\n" +
            "    \n" +
            "    <dependencies>\n" +
            "        <dependency>\n" +
            "            <groupId>javax.servlet</groupId>\n" +
            "            <artifactId>javax.servlet-api</artifactId>\n" +
            "            <version>3.1.0</version>\n" +
            "        </dependency>\n" +
            "    </dependencies>\n" +
            "</project>";
        Files.writeString(pomFile, pomContent);
        
        // Act & Assert - Should not throw an exception
        assertDoesNotThrow(() -> {
            var result = dependencyGraphBuilder.buildFromProject(mavenProjectDir);
            assertNotNull(result);
            assertFalse(result.getNodes().isEmpty());
        });
    }
    
    @Test
    void testProjectWithGradleBuildWorks() throws IOException {
        // Arrange - Create a project with build.gradle
        Path gradleProjectDir = tempDir.resolve("gradle-project");
        Files.createDirectories(gradleProjectDir);
        
        // Create a minimal build.gradle file
        Path buildFile = gradleProjectDir.resolve("build.gradle");
        String gradleContent = "plugins {\n" +
            "    java\n" +
            "}\n" +
            "\n" +
            "dependencies {\n" +
            "    implementation 'javax.servlet:javax.servlet-api:3.1.0'\n" +
            "}";
        Files.writeString(buildFile, gradleContent);
        
        // Act & Assert - Should not throw an exception
        assertDoesNotThrow(() -> {
            var result = dependencyGraphBuilder.buildFromProject(gradleProjectDir);
            assertNotNull(result);
        });
    }
    
    @Test
    void testProjectWithGradleKtsWorks() throws IOException {
        // Arrange - Create a project with build.gradle.kts
        Path gradleKtsProjectDir = tempDir.resolve("gradle-kts-project");
        Files.createDirectories(gradleKtsProjectDir);
        
        // Create a minimal build.gradle.kts file
        Path buildFile = gradleKtsProjectDir.resolve("build.gradle.kts");
        String gradleKtsContent = "plugins {\n" +
            "    java\n" +
            "}\n" +
            "\n" +
            "dependencies {\n" +
            "    implementation(\"javax.servlet:javax.servlet-api:3.1.0\")\n" +
            "}";
        Files.writeString(buildFile, gradleKtsContent);
        
        // Act & Assert - Should not throw an exception
        assertDoesNotThrow(() -> {
            var result = dependencyGraphBuilder.buildFromProject(gradleKtsProjectDir);
            assertNotNull(result);
        });
    }
}
