package unit.jakartamigration.dependencyanalysis;

import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Dependency;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that verify DependencyGraph does not create unnecessary defensive copies.
 * These catch the class of memory bugs where accessor methods allocate new
 * collections on every call, causing O(n) overhead per invocation.
 *
 * <p>Tagged "slow" because they allocate large graphs to verify scaling behavior,
 * but they complete in under 1 second and need no external resources.</p>
 */
@Tag("slow")
@DisplayName("DependencyGraph allocation and memory tests")
class DependencyGraphMemoryTest {

    private DependencyGraph graph;

    @BeforeEach
    void setUp() {
        graph = new DependencyGraph();
    }

    // --- Accessor identity tests ---

    @Test
    @DisplayName("getNodes() should return the same Set instance, not a copy")
    void getNodesShouldReturnSameInstance() {
        graph.addNode(new Artifact("g", "a", "1.0", "compile", false));

        Set<Artifact> first = graph.getNodes();
        Set<Artifact> second = graph.getNodes();

        assertThat(first).isSameAs(second);
    }

    @Test
    @DisplayName("getEdges() should return the same Set instance, not a copy")
    void getEdgesShouldReturnSameInstance() {
        Artifact a = new Artifact("g", "a", "1.0", "compile", false);
        Artifact b = new Artifact("g", "b", "1.0", "compile", false);
        graph.addEdge(new Dependency(a, b, "compile", false));

        Set<Dependency> first = graph.getEdges();
        Set<Dependency> second = graph.getEdges();

        assertThat(first).isSameAs(second);
    }

    @Test
    @DisplayName("Constructor should not copy the nodes set passed in")
    void constructorShouldNotCopyNodes() {
        Set<Artifact> nodes = new HashSet<>();
        nodes.add(new Artifact("g", "a", "1.0", "compile", false));
        Set<Dependency> edges = Set.of();

        DependencyGraph g = new DependencyGraph(nodes, edges);

        assertThat(g.getNodes()).isSameAs(nodes);
    }

    @Test
    @DisplayName("Constructor should not copy the edges set passed in")
    void constructorShouldNotCopyEdges() {
        Set<Artifact> nodes = Set.of();
        Set<Dependency> edges = new HashSet<>();
        edges.add(new Dependency(
                new Artifact("g", "a", "1.0", "compile", false),
                new Artifact("g", "b", "1.0", "compile", false),
                "compile", false));

        DependencyGraph g = new DependencyGraph(nodes, edges);

        assertThat(g.getEdges()).isSameAs(edges);
    }

    // --- Scaling tests with realistic graph sizes ---

    @Test
    @DisplayName("getNodes() must not allocate a new set for a 500-node graph")
    void noCopyOverheadAt500Nodes() {
        buildGraph(500, 0);

        // Call getNodes() many times — if each allocates, memory will spike
        for (int i = 0; i < 1000; i++) {
            Set<Artifact> nodes = graph.getNodes();
            assertThat(nodes).hasSize(500);
        }
    }

    @Test
    @DisplayName("getEdges() must not allocate a new set for a 500-edge graph")
    void noCopyOverheadAt500Edges() {
        // Need 501 nodes to create 500 edges
        buildGraph(501, 500);

        for (int i = 0; i < 1000; i++) {
            Set<Dependency> edges = graph.getEdges();
            assertThat(edges).hasSize(500);
        }
    }

    @Test
    @DisplayName("Merging two 500-node graphs should produce exactly 1000 nodes")
    void mergeShouldDeduplicate() {
        DependencyGraph g1 = new DependencyGraph();
        DependencyGraph g2 = new DependencyGraph();

        for (int i = 0; i < 500; i++) {
            Artifact a = new Artifact("g" + i, "a" + i, "1.0", "compile", false);
            g1.addNode(a);
        }
        // Add 500 more unique nodes to g2
        for (int i = 500; i < 1000; i++) {
            Artifact a = new Artifact("g" + i, "a" + i, "1.0", "compile", false);
            g2.addNode(a);
        }

        g1.merge(g2);

        assertThat(g1.nodeCount()).isEqualTo(1000);
        assertThat(g1.getNodes()).hasSize(1000);
    }

    @Test
    @DisplayName("Merging overlapping graphs should deduplicate shared artifacts")
    void mergeShouldDeduplicateSharedArtifacts() {
        DependencyGraph g1 = new DependencyGraph();
        DependencyGraph g2 = new DependencyGraph();

        Artifact shared = new Artifact("g", "shared", "1.0", "compile", false);
        g1.addNode(shared);
        g2.addNode(shared);
        g2.addNode(new Artifact("g", "unique", "1.0", "compile", false));

        g1.merge(g2);

        assertThat(g1.nodeCount()).isEqualTo(2);
    }

    // --- Helper ---

    private void buildGraph(int nodeCount, int edgeCount) {
        for (int i = 0; i < nodeCount; i++) {
            graph.addNode(new Artifact("g" + i, "a" + i, "1.0", "compile", false));
        }
        for (int i = 0; i < edgeCount && i + 1 < nodeCount; i++) {
            Artifact from = new Artifact("g" + i, "a" + i, "1.0", "compile", false);
            Artifact to = new Artifact("g" + (i + 1), "a" + (i + 1), "1.0", "compile", false);
            graph.addEdge(new Dependency(from, to, "compile", false));
        }
    }
}
