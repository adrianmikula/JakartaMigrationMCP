package unit.jakartamigration.dependencyanalysis;

import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Dependency;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DependencyGraph domain model and conversion logic.
 * Tests the core data structures without UI dependencies.
 */
class DependencyGraphConversionLogicTest {

    @Test
    @DisplayName("Simple graph should have correct node and edge counts")
    void testSimpleGraphStructure() {
        // Arrange
        Set<Artifact> nodes = new HashSet<>();
        Set<Dependency> edges = new HashSet<>();

        Artifact root = new Artifact("root", "project", "1.0.0", "compile", false);
        Artifact dep1 = new Artifact("org.example", "dep1", "1.0.0", "compile", false);

        nodes.add(root);
        nodes.add(dep1);
        edges.add(new Dependency(root, dep1, "compile", false));

        DependencyGraph graph = new DependencyGraph(nodes, edges);

        // Assert
        assertEquals(2, graph.nodeCount());
        assertEquals(1, graph.edgeCount());
        assertTrue(graph.containsNode(root));
        assertTrue(graph.containsNode(dep1));
        assertTrue(graph.containsEdge(edges.iterator().next()));
    }

    @Test
    @DisplayName("Chain graph should preserve all edges")
    void testChainGraphStructure() {
        // Arrange
        Set<Artifact> nodes = new HashSet<>();
        Set<Dependency> edges = new HashSet<>();

        Artifact root = new Artifact("root", "project", "1.0.0", "compile", false);
        Artifact dep1 = new Artifact("org.example", "dep1", "1.0.0", "compile", false);
        Artifact dep2 = new Artifact("org.example", "dep2", "2.0.0", "compile", true);

        nodes.add(root);
        nodes.add(dep1);
        nodes.add(dep2);

        Dependency edge1 = new Dependency(root, dep1, "compile", false);
        Dependency edge2 = new Dependency(dep1, dep2, "compile", false);
        edges.add(edge1);
        edges.add(edge2);

        DependencyGraph graph = new DependencyGraph(nodes, edges);

        // Assert
        assertEquals(3, graph.nodeCount());
        assertEquals(2, graph.edgeCount());
        assertTrue(graph.containsEdge(edge1));
        assertTrue(graph.containsEdge(edge2));
    }

    @Test
    @DisplayName("Diamond graph should have correct structure")
    void testDiamondGraphStructure() {
        // Arrange
        Set<Artifact> nodes = new HashSet<>();
        Set<Dependency> edges = new HashSet<>();

        Artifact root = new Artifact("root", "project", "1.0.0", "compile", false);
        Artifact dep1 = new Artifact("org.example", "dep1", "1.0.0", "compile", false);
        Artifact dep2 = new Artifact("org.example", "dep2", "1.0.0", "compile", false);
        Artifact dep3 = new Artifact("org.example", "dep3", "1.0.0", "compile", true);

        nodes.add(root);
        nodes.add(dep1);
        nodes.add(dep2);
        nodes.add(dep3);

        edges.add(new Dependency(root, dep1, "compile", false));
        edges.add(new Dependency(root, dep2, "compile", false));
        edges.add(new Dependency(dep1, dep3, "compile", false));
        edges.add(new Dependency(dep2, dep3, "compile", false));

        DependencyGraph graph = new DependencyGraph(nodes, edges);

        // Assert
        assertEquals(4, graph.nodeCount());
        assertEquals(4, graph.edgeCount());
    }

    @Test
    @DisplayName("Artifact transitive flag should be preserved")
    void testTransitiveFlagPreserved() {
        // Arrange
        Artifact direct = new Artifact("org.example", "direct", "1.0.0", "compile", false);
        Artifact transitive = new Artifact("org.example", "transitive", "2.0.0", "compile", true);

        // Assert
        assertFalse(direct.transitive());
        assertTrue(transitive.transitive());
    }

    @Test
    @DisplayName("Artifact toIdentifier should generate correct ID")
    void testArtifactToIdentifier() {
        // Arrange
        Artifact artifact = new Artifact("org.example", "my-lib", "1.0.0", "compile", false);

        // Act
        String id = artifact.toIdentifier();

        // Assert
        assertEquals("org.example:my-lib", id);
    }

    @Test
    @DisplayName("Empty graph should have zero nodes and edges")
    void testEmptyGraph() {
        // Act
        DependencyGraph graph = new DependencyGraph();

        // Assert
        assertEquals(0, graph.nodeCount());
        assertEquals(0, graph.edgeCount());
        assertTrue(graph.getNodes().isEmpty());
        assertTrue(graph.getEdges().isEmpty());
    }

    @Test
    @DisplayName("Graph should deduplicate nodes")
    void testNodeDeduplication() {
        // Arrange
        Set<Artifact> nodes = new HashSet<>();
        Artifact artifact = new Artifact("org.example", "dep1", "1.0.0", "compile", false);
        
        // Act - Add same artifact twice
        nodes.add(artifact);
        nodes.add(artifact);

        DependencyGraph graph = new DependencyGraph(nodes, new HashSet<>());

        // Assert
        assertEquals(1, graph.nodeCount());
    }

    @Test
    @DisplayName("Graph should deduplicate edges")
    void testEdgeDeduplication() {
        // Arrange
        Set<Artifact> nodes = new HashSet<>();
        Set<Dependency> edges = new HashSet<>();
        
        Artifact root = new Artifact("root", "project", "1.0.0", "compile", false);
        Artifact dep1 = new Artifact("org.example", "dep1", "1.0.0", "compile", false);
        nodes.add(root);
        nodes.add(dep1);

        Dependency edge = new Dependency(root, dep1, "compile", false);
        edges.add(edge);
        edges.add(edge); // Add same edge twice

        DependencyGraph graph = new DependencyGraph(nodes, edges);

        // Assert
        assertEquals(1, graph.edgeCount());
    }

    @Test
    @DisplayName("Node with no incoming edges should be identifiable")
    void testIdentifyRootNodes() {
        // Arrange
        Set<Artifact> nodes = new HashSet<>();
        Set<Dependency> edges = new HashSet<>();

        Artifact root = new Artifact("root", "project", "1.0.0", "compile", false);
        Artifact dep1 = new Artifact("org.example", "dep1", "1.0.0", "compile", false);
        Artifact dep2 = new Artifact("org.example", "dep2", "2.0.0", "compile", true);

        nodes.add(root);
        nodes.add(dep1);
        nodes.add(dep2);

        edges.add(new Dependency(root, dep1, "compile", false));
        edges.add(new Dependency(dep1, dep2, "compile", false));

        DependencyGraph graph = new DependencyGraph(nodes, edges);

        // Act - Find nodes with no incoming edges
        Set<String> targetIds = new HashSet<>();
        for (Dependency edge : graph.getEdges()) {
            targetIds.add(edge.to().toIdentifier());
        }

        Set<String> rootIds = new HashSet<>();
        for (Artifact node : graph.getNodes()) {
            if (!targetIds.contains(node.toIdentifier())) {
                rootIds.add(node.toIdentifier());
            }
        }

        // Assert
        assertEquals(1, rootIds.size());
        assertTrue(rootIds.contains("root:project"));
    }

    @Test
    @DisplayName("Multiple direct dependencies should all have no incoming edges")
    void testMultipleDirectDependencies() {
        // Arrange
        Set<Artifact> nodes = new HashSet<>();
        Set<Dependency> edges = new HashSet<>();

        Artifact root = new Artifact("root", "project", "1.0.0", "compile", false);
        Artifact dep1 = new Artifact("org.example", "dep1", "1.0.0", "compile", false);
        Artifact dep2 = new Artifact("org.example", "dep2", "2.0.0", "compile", false);
        Artifact dep3 = new Artifact("org.example", "dep3", "3.0.0", "compile", true);

        nodes.add(root);
        nodes.add(dep1);
        nodes.add(dep2);
        nodes.add(dep3);

        edges.add(new Dependency(root, dep1, "compile", false));
        edges.add(new Dependency(root, dep2, "compile", false));
        edges.add(new Dependency(root, dep3, "compile", false));

        DependencyGraph graph = new DependencyGraph(nodes, edges);

        // Act - Find nodes with no incoming edges
        Set<String> targetIds = new HashSet<>();
        for (Dependency edge : graph.getEdges()) {
            targetIds.add(edge.to().toIdentifier());
        }

        Set<String> rootIds = new HashSet<>();
        for (Artifact node : graph.getNodes()) {
            if (!targetIds.contains(node.toIdentifier())) {
                rootIds.add(node.toIdentifier());
            }
        }

        // Assert - Only root should have no incoming edges
        assertEquals(1, rootIds.size());
        assertTrue(rootIds.contains("root:project"));
    }
}
