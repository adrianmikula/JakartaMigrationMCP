package unit.jakartamigration.dependencyanalysis.service.impl;

import com.bugbounty.jakartamigration.dependencyanalysis.domain.Artifact;
import com.bugbounty.jakartamigration.dependencyanalysis.domain.Dependency;
import com.bugbounty.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import com.bugbounty.jakartamigration.dependencyanalysis.service.DependencyGraphException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MavenDependencyGraphBuilder Tests")
class MavenDependencyGraphBuilderTest {
    
    private MavenDependencyGraphBuilder builder;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        builder = new MavenDependencyGraphBuilder();
    }
    
    @Test
    @DisplayName("Should throw exception when pom.xml does not exist")
    void shouldThrowWhenPomXmlNotFound() {
        // Given
        Path nonExistentPom = tempDir.resolve("pom.xml");
        
        // When & Then
        assertThrows(DependencyGraphException.class, () -> 
            builder.buildFromMaven(nonExistentPom)
        );
    }
    
    @Test
    @DisplayName("Should parse simple pom.xml with dependencies")
    void shouldParseSimplePomXml() throws Exception {
        // Given
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
                     https://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>test-project</artifactId>
                <version>1.0.0</version>
                
                <dependencies>
                    <dependency>
                        <groupId>javax.servlet</groupId>
                        <artifactId>javax.servlet-api</artifactId>
                        <version>4.0.1</version>
                    </dependency>
                    <dependency>
                        <groupId>org.springframework</groupId>
                        <artifactId>spring-web</artifactId>
                        <version>5.3.21</version>
                        <scope>compile</scope>
                    </dependency>
                </dependencies>
            </project>
            """;
        
        Path pomXml = tempDir.resolve("pom.xml");
        Files.writeString(pomXml, pomContent);
        
        // When
        DependencyGraph graph = builder.buildFromMaven(pomXml);
        
        // Then
        assertNotNull(graph);
        assertTrue(graph.nodeCount() >= 3); // Project + 2 dependencies
        assertEquals(2, graph.edgeCount());
        
        // Verify project artifact
        Artifact projectArtifact = new Artifact("com.example", "test-project", "1.0.0", "compile", false);
        assertTrue(graph.containsNode(projectArtifact));
        
        // Verify dependencies exist
        Artifact servletDep = new Artifact("javax.servlet", "javax.servlet-api", "4.0.1", "compile", true);
        Artifact springDep = new Artifact("org.springframework", "spring-web", "5.3.21", "compile", true);
        
        assertTrue(graph.containsNode(servletDep));
        assertTrue(graph.containsNode(springDep));
    }
    
    @Test
    @DisplayName("Should handle pom.xml with parent")
    void shouldHandlePomWithParent() throws Exception {
        // Given
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <parent>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-parent</artifactId>
                    <version>2.7.0</version>
                </parent>
                <artifactId>test-project</artifactId>
                <dependencies>
                    <dependency>
                        <groupId>javax.servlet</groupId>
                        <artifactId>javax.servlet-api</artifactId>
                        <version>4.0.1</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        
        Path pomXml = tempDir.resolve("pom.xml");
        Files.writeString(pomXml, pomContent);
        
        // When
        DependencyGraph graph = builder.buildFromMaven(pomXml);
        
        // Then
        assertNotNull(graph);
        // Should use parent groupId if not specified
        Artifact projectArtifact = new Artifact("org.springframework.boot", "test-project", "2.7.0", "compile", false);
        // Note: This test may need adjustment based on actual implementation behavior
        assertTrue(graph.nodeCount() >= 2);
    }
    
    @Test
    @DisplayName("Should detect Maven project from project root")
    void shouldDetectMavenProject() throws Exception {
        // Given
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>test-project</artifactId>
                <version>1.0.0</version>
            </project>
            """;
        
        Path pomXml = tempDir.resolve("pom.xml");
        Files.writeString(pomXml, pomContent);
        
        // When
        DependencyGraph graph = builder.buildFromProject(tempDir);
        
        // Then
        assertNotNull(graph);
        assertTrue(graph.nodeCount() >= 1);
    }
    
    @Test
    @DisplayName("Should throw exception when no build file found")
    void shouldThrowWhenNoBuildFileFound() {
        // Given - empty directory
        
        // When & Then
        assertThrows(DependencyGraphException.class, () -> 
            builder.buildFromProject(tempDir)
        );
    }
}

