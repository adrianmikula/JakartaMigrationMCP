package adrianmikula.jakartamigration.integration.dependencyanalysis;

import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.service.impl.MavenDependencyGraphBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simple integration test for Maven property resolution using realistic POM structures.
 * This test creates realistic POM files to verify our property resolution fixes work.
 */
@Tag("integration")
class SimpleMavenPropertyResolutionTest {
    
    private MavenDependencyGraphBuilder dependencyGraphBuilder;
    
    @BeforeEach
    void setUp() {
        dependencyGraphBuilder = new MavenDependencyGraphBuilder();
    }
    
    @Test
    void shouldResolvePropertiesInRealisticPomStructure(@TempDir Path tempDir) throws Exception {
        // Given - Create a realistic POM similar to javaee7-samples structure
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0" 
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
                     https://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.javaee7</groupId>
                <artifactId>samples-parent</artifactId>
                <version>1.0-SNAPSHOT</version>
                <packaging>pom</packaging>
                
                <properties>
                    <arquillian.version>1.7.0.Alpha1</arquillian.version>
                    <payara.version>4.1.2.181</payara.version>
                    <wildfly.version>13.0.0.Final</wildfly.version>
                    <glassfish.version>4.1.1</glassfish.version>
                    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
                </properties>
                
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>org.jboss.arquillian</groupId>
                            <artifactId>arquillian-bom</artifactId>
                            <version>${arquillian.version}</version>
                            <scope>import</scope>
                            <type>pom</type>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
                
                <dependencies>
                    <dependency>
                        <groupId>org.jboss.arquillian</groupId>
                        <artifactId>arquillian-bom</artifactId>
                        <version>${arquillian.version}</version>
                        <scope>import</scope>
                        <type>pom</type>
                    </dependency>
                    <dependency>
                        <groupId>fish.payara.distributions</groupId>
                        <artifactId>payara</artifactId>
                        <version>${payara.version}</version>
                        <type>zip</type>
                    </dependency>
                    <dependency>
                        <groupId>org.glassfish.main.extras</groupId>
                        <artifactId>glassfish-embedded-all</artifactId>
                        <version>${glassfish.version}</version>
                        <scope>test</scope>
                    </dependency>
                    <dependency>
                        <groupId>org.wildfly.plugins</groupId>
                        <artifactId>wildfly-maven-plugin</artifactId>
                        <version>${wildfly.version}</version>
                    </dependency>
                    <dependency>
                        <groupId>javax</groupId>
                        <artifactId>javaee-api</artifactId>
                        <version>7.0</version>
                        <scope>provided</scope>
                    </dependency>
                </dependencies>
            </project>
            """;
        
        Path pomXmlPath = tempDir.resolve("pom.xml");
        Files.writeString(pomXmlPath, pomContent);
        
        // When - Build dependency graph from the realistic POM
        DependencyGraph graph = dependencyGraphBuilder.buildFromMaven(pomXmlPath);
        
        // Then - Should resolve Maven properties correctly
        assertThat(graph.nodeCount()).isGreaterThan(1); // Project + dependencies
        
        // Print all dependencies for debugging
        System.out.println("=== Dependencies found in realistic POM ===");
        graph.getNodes().stream()
            .filter(Artifact::transitive)
            .forEach(node -> {
                System.out.println("  " + node.groupId() + ":" + node.artifactId() + ":" + node.version() + " (scope: " + node.scope() + ")");
            });
        
        // Verify that dependencies with property references are resolved to actual versions
        boolean hasUnresolvedProperties = graph.getNodes().stream()
            .filter(Artifact::transitive)
            .anyMatch(node -> node.version().startsWith("${") || "unknown".equals(node.version()));
        
        assertThat(hasUnresolvedProperties)
            .as("Should not have any dependencies with unresolved property references")
            .isFalse();
        
        // Verify specific properties are resolved correctly
        assertThat(graph.containsNode(new Artifact("org.jboss.arquillian", "arquillian-bom", "1.7.0.Alpha1", "import", true)))
            .as("Should resolve arquillian.version property")
            .isTrue();
            
        assertThat(graph.containsNode(new Artifact("fish.payara.distributions", "payara", "4.1.2.181", "compile", true)))
            .as("Should resolve payara.version property")
            .isTrue();
            
        assertThat(graph.containsNode(new Artifact("org.glassfish.main.extras", "glassfish-embedded-all", "4.1.1", "test", true)))
            .as("Should resolve glassfish.version property")
            .isTrue();
            
        assertThat(graph.containsNode(new Artifact("org.wildfly.plugins", "wildfly-maven-plugin", "13.0.0.Final", "compile", true)))
            .as("Should resolve wildfly.version property")
            .isTrue();
    }
    
    @Test
    void shouldHandleMissingPropertiesGracefully(@TempDir Path tempDir) throws Exception {
        // Given - Create a POM with missing property references
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>test-project</artifactId>
                <version>1.0.0</version>
                
                <properties>
                    <existing.version>1.2.3</existing.version>
                </properties>
                
                <dependencies>
                    <dependency>
                        <groupId>org.example</groupId>
                        <artifactId>existing-artifact</artifactId>
                        <version>${existing.version}</version>
                    </dependency>
                    <dependency>
                        <groupId>org.example</groupId>
                        <artifactId>missing-artifact</artifactId>
                        <version>${missing.version}</version>
                    </dependency>
                    <dependency>
                        <groupId>org.example</groupId>
                        <artifactId>direct-artifact</artifactId>
                        <version>4.5.6</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        
        Path pomXmlPath = tempDir.resolve("pom.xml");
        Files.writeString(pomXmlPath, pomContent);
        
        // When - Build dependency graph from the POM
        DependencyGraph graph = dependencyGraphBuilder.buildFromMaven(pomXmlPath);
        
        // Then - Should handle missing properties gracefully
        assertThat(graph.nodeCount()).isGreaterThan(1); // Project + dependencies
        
        // Print all dependencies for debugging
        System.out.println("=== Dependencies found in POM with missing properties ===");
        graph.getNodes().stream()
            .filter(Artifact::transitive)
            .forEach(node -> {
                System.out.println("  " + node.groupId() + ":" + node.artifactId() + ":" + node.version());
            });
        
        // Verify existing property is resolved
        assertThat(graph.containsNode(new Artifact("org.example", "existing-artifact", "1.2.3", "compile", true)))
            .as("Should resolve existing property")
            .isTrue();
            
        // Verify missing property falls back to the original reference or "unknown"
        boolean hasMissingPropertyHandled = graph.getNodes().stream()
            .filter(Artifact::transitive)
            .anyMatch(node -> 
                node.groupId().equals("org.example") && 
                node.artifactId().equals("missing-artifact") &&
                (node.version().equals("${missing.version}") || node.version().equals("unknown"))
            );
        
        assertThat(hasMissingPropertyHandled)
            .as("Should handle missing property gracefully")
            .isTrue();
            
        // Verify direct version is preserved
        assertThat(graph.containsNode(new Artifact("org.example", "direct-artifact", "4.5.6", "compile", true)))
            .as("Should preserve direct version")
            .isTrue();
    }
}
