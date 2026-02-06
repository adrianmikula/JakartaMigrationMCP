package unit.jakartamigration.dependencyanalysis.service.impl;

import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphException;
import adrianmikula.jakartamigration.dependencyanalysis.service.impl.MavenDependencyGraphBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GradleDependencyGraphBuilderTest {
    
    private MavenDependencyGraphBuilder builder;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        builder = new MavenDependencyGraphBuilder();
    }
    
    @Test
    void shouldParseGradleBuildFile() throws Exception {
        // Given
        Path buildGradle = tempDir.resolve("build.gradle");
        String content = """
            plugins {
                id 'java'
            }
            
            dependencies {
                implementation 'javax.servlet:javax.servlet-api:4.0.1'
                implementation 'org.springframework.boot:spring-boot-starter-web:3.2.0'
                testImplementation 'junit:junit:4.13.1'
            }
            """;
        Files.writeString(buildGradle, content);
        
        // When
        DependencyGraph graph = builder.buildFromGradle(buildGradle);
        
        // Then
        assertThat(graph.nodeCount()).isGreaterThanOrEqualTo(4); // Project + 3 dependencies
        
        List<Artifact> artifacts = graph.getNodes().stream()
            .filter(a -> a.transitive())
            .collect(Collectors.toList());
        
        assertThat(artifacts).hasSize(3);
        
        Artifact servlet = artifacts.stream()
            .filter(a -> a.groupId().equals("javax.servlet"))
            .findFirst()
            .orElseThrow();
        assertThat(servlet.artifactId()).isEqualTo("javax.servlet-api");
        assertThat(servlet.version()).isEqualTo("4.0.1");
        assertThat(servlet.scope()).isEqualTo("compile");
        
        Artifact springBoot = artifacts.stream()
            .filter(a -> a.groupId().equals("org.springframework.boot"))
            .findFirst()
            .orElseThrow();
        assertThat(springBoot.artifactId()).isEqualTo("spring-boot-starter-web");
        assertThat(springBoot.version()).isEqualTo("3.2.0");
        
        Artifact junit = artifacts.stream()
            .filter(a -> a.groupId().equals("junit"))
            .findFirst()
            .orElseThrow();
        assertThat(junit.artifactId()).isEqualTo("junit");
        assertThat(junit.version()).isEqualTo("4.13.1");
        assertThat(junit.scope()).isEqualTo("test");
    }
    
    @Test
    void shouldParseGradleKotlinBuildFile() throws Exception {
        // Given
        Path buildGradleKts = tempDir.resolve("build.gradle.kts");
        String content = """
            plugins {
                java
            }
            
            dependencies {
                implementation("javax.servlet:javax.servlet-api:4.0.1")
                api("org.springframework.boot:spring-boot-starter-web:3.2.0")
            }
            """;
        Files.writeString(buildGradleKts, content);
        
        // When
        DependencyGraph graph = builder.buildFromGradle(buildGradleKts);
        
        // Then
        assertThat(graph.nodeCount()).isGreaterThanOrEqualTo(3); // Project + 2 dependencies
        
        List<Artifact> artifacts = graph.getNodes().stream()
            .filter(a -> a.transitive())
            .collect(Collectors.toList());
        
        assertThat(artifacts).hasSize(2);
    }
    
    @Test
    void shouldDetectGradleProject() throws Exception {
        // Given
        Path buildGradle = tempDir.resolve("build.gradle");
        Files.writeString(buildGradle, "dependencies { implementation 'javax.servlet:javax.servlet-api:4.0.1' }");
        
        // When
        DependencyGraph graph = builder.buildFromProject(tempDir);
        
        // Then
        assertThat(graph.nodeCount()).isGreaterThanOrEqualTo(2); // Project + 1 dependency
    }
    
    @Test
    void shouldDetectGradleKotlinProject() throws Exception {
        // Given
        Path buildGradleKts = tempDir.resolve("build.gradle.kts");
        Files.writeString(buildGradleKts, "dependencies { implementation(\"javax.servlet:javax.servlet-api:4.0.1\") }");
        
        // When
        DependencyGraph graph = builder.buildFromProject(tempDir);
        
        // Then
        assertThat(graph.nodeCount()).isGreaterThanOrEqualTo(2); // Project + 1 dependency
    }
    
    @Test
    void shouldThrowExceptionForNonExistentFile() {
        // When/Then
        assertThatThrownBy(() -> builder.buildFromGradle(tempDir.resolve("nonexistent.gradle")))
            .isInstanceOf(DependencyGraphException.class)
            .hasMessageContaining("Gradle build file does not exist");
    }
    
    @Test
    void shouldParseRuntimeDependencies() throws Exception {
        // Given
        Path buildGradle = tempDir.resolve("build.gradle");
        String content = """
            dependencies {
                runtimeOnly 'com.example:runtime-lib:1.0.0'
                compileOnly 'com.example:provided-lib:1.0.0'
            }
            """;
        Files.writeString(buildGradle, content);
        
        // When
        DependencyGraph graph = builder.buildFromGradle(buildGradle);
        
        // Then
        List<Artifact> artifacts = graph.getNodes().stream()
            .filter(a -> a.transitive())
            .collect(Collectors.toList());
        
        Artifact runtime = artifacts.stream()
            .filter(a -> a.groupId().equals("com.example") && a.artifactId().equals("runtime-lib"))
            .findFirst()
            .orElseThrow();
        assertThat(runtime.scope()).isEqualTo("runtime");
        
        Artifact provided = artifacts.stream()
            .filter(a -> a.groupId().equals("com.example") && a.artifactId().equals("provided-lib"))
            .findFirst()
            .orElseThrow();
        assertThat(provided.scope()).isEqualTo("provided");
    }
}

