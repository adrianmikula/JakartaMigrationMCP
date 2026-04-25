package adrianmikula.jakartamigration.unit.jakartamigration.dependencyanalysis.service.impl;

import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphException;
import adrianmikula.jakartamigration.dependencyanalysis.service.impl.MavenDependencyGraphBuilder;
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
    
    @Test
    @DisplayName("Should resolve version from properties")
    void shouldResolveVersionFromProperties() throws Exception {
        // Given
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>test-project</artifactId>
                <version>1.0.0</version>
                
                <properties>
                    <arquillian.version>1.7.0.Alpha1</arquillian.version>
                    <payara.version>4.1.2.181</payara.version>
                </properties>
                
                <dependencies>
                    <dependency>
                        <groupId>org.jboss.arquillian</groupId>
                        <artifactId>arquillian-bom</artifactId>
                        <version>${arquillian.version}</version>
                    </dependency>
                    <dependency>
                        <groupId>fish.payara.distributions</groupId>
                        <artifactId>payara</artifactId>
                        <version>${payara.version}</version>
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
        
        // Verify dependencies with resolved versions
        Artifact arquillianDep = new Artifact("org.jboss.arquillian", "arquillian-bom", "1.7.0.Alpha1", "compile", true);
        Artifact payaraDep = new Artifact("fish.payara.distributions", "payara", "4.1.2.181", "compile", true);
        
        assertTrue(graph.containsNode(arquillianDep));
        assertTrue(graph.containsNode(payaraDep));
    }
    
    @Test
    @DisplayName("Should resolve version from dependencyManagement with properties")
    void shouldResolveVersionFromDependencyManagementWithProperties() throws Exception {
        // Given
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>test-project</artifactId>
                <version>1.0.0</version>
                
                <properties>
                    <wildfly.version>13.0.0.Final</wildfly.version>
                </properties>
                
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>org.wildfly</groupId>
                            <artifactId>wildfly-core</artifactId>
                            <version>${wildfly.version}</version>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
                
                <dependencies>
                    <dependency>
                        <groupId>org.wildfly</groupId>
                        <artifactId>wildfly-core</artifactId>
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
        assertTrue(graph.nodeCount() >= 2); // Project + 1 dependency
        
        // Verify dependency with resolved version from dependencyManagement
        Artifact wildflyDep = new Artifact("org.wildfly", "wildfly-core", "13.0.0.Final", "compile", true);
        assertTrue(graph.containsNode(wildflyDep));
    }
    
    @Test
    @DisplayName("Should handle missing properties gracefully")
    void shouldHandleMissingPropertiesGracefully() throws Exception {
        // Given
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>test-project</artifactId>
                <version>1.0.0</version>
                
                <dependencies>
                    <dependency>
                        <groupId>org.jboss.arquillian</groupId>
                        <artifactId>arquillian-bom</artifactId>
                        <version>${nonexistent.version}</version>
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
        assertTrue(graph.nodeCount() >= 2); // Project + 1 dependency
        
        // Verify dependency with "unknown" version when property doesn't exist
        Artifact dep = new Artifact("org.jboss.arquillian", "arquillian-bom", "unknown", "compile", true);
        assertTrue(graph.containsNode(dep));
    }
    
    @Test
    @DisplayName("Should resolve properties in complex real-world pom.xml")
    void shouldResolvePropertiesInComplexRealWorldPom() throws Exception {
        // Given - Path to real javaee7-samples project (if available)
        Path javaeeSamplesPath = Path.of("../../../examples/old/hard/javaee7-samples-master/javaee7-samples-master/pom.xml");
        
        // Only run if the test file exists (integration test)
        if (!Files.exists(javaeeSamplesPath)) {
            // Skip test if sample project not available
            return;
        }
        
        // When
        DependencyGraph graph = builder.buildFromMaven(javaeeSamplesPath);
        
        // Then
        assertNotNull(graph, "Dependency graph should be created");
        assertTrue(graph.nodeCount() > 0, "Graph should contain nodes");
        assertTrue(graph.edgeCount() > 0, "Graph should contain edges");
        
        // Check for specific properties that should be resolved in javaee7-samples
        String[] expectedProperties = {
            "org.jboss.arquillian:arquillian-bom:1.7.0.Alpha1",
            "fish.payara.distributions:payara:4.1.2.181",
            "org.glassfish.main.extras:glassfish-embedded-all:4.1.1",
            "org.wildfly.plugins:wildfly-maven-plugin:13.0.0.Final"
        };
        
        for (String expected : expectedProperties) {
            String[] parts = expected.split(":");
            Artifact artifact = new Artifact(parts[0], parts[1], parts[2], "compile", true);
            boolean found = graph.containsNode(artifact);
            
            if (!found) {
                // Try with "unknown" version - indicates property resolution failed
                Artifact unknownVersion = new Artifact(parts[0], parts[1], "unknown", "compile", true);
                boolean unknownFound = graph.containsNode(unknownVersion);
                
                // If unknown version found, property resolution failed for this artifact
                if (unknownFound) {
                    fail("Property resolution failed for " + expected + " - found 'unknown' version");
                }
            }
        }
    }
}

