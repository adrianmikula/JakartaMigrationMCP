package unit.jakartamigration.dependencyanalysis.service;

import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphBuilder;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DependencyGraphBuilder Tests")
class DependencyGraphBuilderTest {
    
    private DependencyGraphBuilder builder;
    
    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        builder = new adrianmikula.jakartamigration.dependencyanalysis.service.impl.MavenDependencyGraphBuilder();
    }
    
    @Test
    @DisplayName("Should throw exception when pom.xml does not exist")
    void shouldThrowWhenPomXmlNotFound() {
        // Given
        Path nonExistentPom = Paths.get("non-existent", "pom.xml");
        
        // When & Then
        assertThrows(DependencyGraphException.class, () -> 
            builder.buildFromMaven(nonExistentPom)
        );
    }
    
    @Test
    @DisplayName("Should throw exception when build.gradle does not exist")
    void shouldThrowWhenGradleFileNotFound() {
        // Given
        Path nonExistentGradle = Paths.get("non-existent", "build.gradle");
        
        // When & Then
        assertThrows(DependencyGraphException.class, () -> 
            builder.buildFromGradle(nonExistentGradle)
        );
    }
    
    @Test
    @DisplayName("Should throw exception when project root has no build files")
    void shouldThrowWhenNoBuildFilesFound() {
        // Given
        Path emptyProject = Paths.get("empty-project");
        
        // When & Then
        assertThrows(DependencyGraphException.class, () -> 
            builder.buildFromProject(emptyProject)
        );
    }
    
    // Note: Integration tests with actual pom.xml/build.gradle files will be added
    // once we implement the builder
}

