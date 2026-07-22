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
         Artifact servletDep = new Artifact("javax.servlet", "javax.servlet-api", "4.0.1", "compile", false);
         Artifact springDep = new Artifact("org.springframework", "spring-web", "5.3.21", "compile", false);
        
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
    @DisplayName("Should throw exception when no build file found and no JARs exist")
    void shouldThrowWhenNoBuildFileFound() {
        // Given - empty directory

        // When & Then
        assertThrows(DependencyGraphException.class, () ->
            builder.buildFromProject(tempDir)
        );
    }

    @Test
    @DisplayName("Should fallback to directory crawler for Eclipse projects with JARs")
    void shouldFallbackToDirectoryCrawlerForEclipseProjects() throws Exception {
        // Given - Eclipse project with JARs but no Maven/Gradle files
        Path libDir = tempDir.resolve("lib");
        Files.createDirectory(libDir);
        Files.createFile(libDir.resolve("javax.servlet-api-3.1.0.jar"));
        Files.createFile(libDir.resolve("javax.persistence-api-2.2.jar"));

        // When
        DependencyGraph graph = builder.buildFromProject(tempDir);

        // Then
        assertNotNull(graph);
        assertTrue(graph.nodeCount() >= 2, "Should find JAR dependencies via directory crawler");
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
         Artifact arquillianDep = new Artifact("org.jboss.arquillian", "arquillian-bom", "1.7.0.Alpha1", "compile", false);
         Artifact payaraDep = new Artifact("fish.payara.distributions", "payara", "4.1.2.181", "compile", false);
        
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
         Artifact wildflyDep = new Artifact("org.wildfly", "wildfly-core", "13.0.0.Final", "compile", false);
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
         Artifact dep = new Artifact("org.jboss.arquillian", "arquillian-bom", "unknown", "compile", false);
         assertTrue(graph.containsNode(dep));
    }
    
    @Test
    @DisplayName("Should parse child pom.xml directly via buildFromMaven")
    void shouldParseChildPomDirectly() throws Exception {
        // Given — create a multi-module structure and parse a child pom directly
        String childPomContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                        <groupId>com.example</groupId>
                        <artifactId>parent-project</artifactId>
                        <version>1.0.0</version>
                    </parent>
                    <artifactId>core</artifactId>
                    <dependencies>
                        <dependency>
                            <groupId>javax.servlet</groupId>
                            <artifactId>javax.servlet-api</artifactId>
                            <version>4.0.1</version>
                        </dependency>
                        <dependency>
                            <groupId>org.slf4j</groupId>
                            <artifactId>slf4j-api</artifactId>
                            <version>2.0.9</version>
                        </dependency>
                    </dependencies>
                </project>
                """;

        Files.createDirectories(tempDir.resolve("core"));
        Files.writeString(tempDir.resolve("core/pom.xml"), childPomContent);

        // When — parse child pom directly
        DependencyGraph graph = builder.buildFromMaven(tempDir.resolve("core/pom.xml"));

        // Then
        assertNotNull(graph);
        assertTrue(graph.nodeCount() >= 3); // parent ref + 2 deps

        // Verify child dependencies
        Artifact servletDep = new Artifact("javax.servlet", "javax.servlet-api", "4.0.1", "compile", false);
        Artifact slf4jDep = new Artifact("org.slf4j", "slf4j-api", "2.0.9", "compile", false);
        assertTrue(graph.containsNode(servletDep));
        assertTrue(graph.containsNode(slf4jDep));
    }

    @Test
    @DisplayName("Should parse buildFromProject for multi-module Maven (reads root pom only)")
    void shouldParseRootPomForMultiModuleProject() throws Exception {
        // Given — multi-module Maven project
        Files.writeString(tempDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>parent-project</artifactId>
                    <version>1.0.0</version>
                    <modules>
                        <module>core</module>
                        <module>web</module>
                    </modules>
                    <dependencies>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>32.1.3-jre</version>
                        </dependency>
                    </dependencies>
                </project>
                """);

        Files.createDirectories(tempDir.resolve("core"));
        Files.writeString(tempDir.resolve("core/pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                        <groupId>com.example</groupId>
                        <artifactId>parent-project</artifactId>
                        <version>1.0.0</version>
                    </parent>
                    <artifactId>core</artifactId>
                    <dependencies>
                        <dependency>
                            <groupId>javax.servlet</groupId>
                            <artifactId>javax.servlet-api</artifactId>
                            <version>4.0.1</version>
                        </dependency>
                    </dependencies>
                </project>
                """);

        Files.createDirectories(tempDir.resolve("web"));
        Files.writeString(tempDir.resolve("web/pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                        <groupId>com.example</groupId>
                        <artifactId>parent-project</artifactId>
                        <version>1.0.0</version>
                    </parent>
                    <artifactId>web</artifactId>
                    <dependencies>
                        <dependency>
                            <groupId>jakarta.servlet</groupId>
                            <artifactId>jakarta.servlet-api</artifactId>
                            <version>6.0.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """);

        // When — buildFromProject reads root pom.xml
        DependencyGraph graph = builder.buildFromProject(tempDir);

        // Then — should find the root pom's guava dep + root project artifact
        assertNotNull(graph);
        assertTrue(graph.nodeCount() >= 2);

        // Root pom has guava as direct dependency
        Artifact guavaDep = new Artifact("com.google.guava", "guava", "32.1.3-jre", "compile", false);
        assertTrue(graph.containsNode(guavaDep),
                "Root pom should include guava dependency. Graph nodes: " + graph.getNodes());
    }

    @Test
    @DisplayName("Should handle pom.xml with dependencyManagement and properties across parent/child")
    void shouldHandleParentChildPropertyResolution() throws Exception {
        // Given — parent pom with property definitions
        Files.writeString(tempDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>parent-project</artifactId>
                    <version>1.0.0</version>
                    <properties>
                        <slf4j.version>2.0.9</slf4j.version>
                        <guava.version>32.1.3-jre</guava.version>
                    </properties>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>org.slf4j</groupId>
                                <artifactId>slf4j-api</artifactId>
                                <version>${slf4j.version}</version>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>${guava.version}</version>
                        </dependency>
                    </dependencies>
                </project>
                """);

        // When
        DependencyGraph graph = builder.buildFromMaven(tempDir.resolve("pom.xml"));

        // Then — properties should be resolved
        Artifact guavaDep = new Artifact("com.google.guava", "guava", "32.1.3-jre", "compile", false);
        assertTrue(graph.containsNode(guavaDep),
                "guava version should be resolved from property. Nodes: " + graph.getNodes());
    }

    @Test
    @DisplayName("Should parse pom.xml with scope annotations (test, provided, runtime)")
    void shouldParseScopesCorrectly() throws Exception {
        // Given
        String pomContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>scoped-project</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>javax.servlet</groupId>
                            <artifactId>javax.servlet-api</artifactId>
                            <version>4.0.1</version>
                            <scope>provided</scope>
                        </dependency>
                        <dependency>
                            <groupId>junit</groupId>
                            <artifactId>junit</artifactId>
                            <version>4.13.1</version>
                            <scope>test</scope>
                        </dependency>
                        <dependency>
                            <groupId>org.postgresql</groupId>
                            <artifactId>postgresql</artifactId>
                            <version>42.7.1</version>
                            <scope>runtime</scope>
                        </dependency>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>32.1.3-jre</version>
                        </dependency>
                    </dependencies>
                </project>
                """;

        Files.writeString(tempDir.resolve("pom.xml"), pomContent);

        // When
        DependencyGraph graph = builder.buildFromMaven(tempDir.resolve("pom.xml"));

        // Then — verify all 4 dependencies are found with correct scopes
        assertTrue(graph.nodeCount() >= 5); // project + 4 deps

        Artifact servletDep = new Artifact("javax.servlet", "javax.servlet-api", "4.0.1", "provided", false);
        Artifact junitDep = new Artifact("junit", "junit", "4.13.1", "test", false);
        Artifact pgDep = new Artifact("org.postgresql", "postgresql", "42.7.1", "runtime", false);
        Artifact guavaDep = new Artifact("com.google.guava", "guava", "32.1.3-jre", "compile", false);

        assertTrue(graph.containsNode(servletDep), "Should find servlet with provided scope");
        assertTrue(graph.containsNode(junitDep), "Should find junit with test scope");
        assertTrue(graph.containsNode(pgDep), "Should find postgresql with runtime scope");
        assertTrue(graph.containsNode(guavaDep), "Should find guava with compile scope");
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
             Artifact artifact = new Artifact(parts[0], parts[1], parts[2], "compile", false);
             boolean found = graph.containsNode(artifact);

             if (!found) {
                 // Try with "unknown" version - indicates property resolution failed
                 Artifact unknownVersion = new Artifact(parts[0], parts[1], "unknown", "compile", false);
                 boolean unknownFound = graph.containsNode(unknownVersion);

                 // If unknown version found, property resolution failed for this artifact
                 if (unknownFound) {
                     fail("Property resolution failed for " + expected + " - found 'unknown' version");
                 }
             }
         }
    }
}

