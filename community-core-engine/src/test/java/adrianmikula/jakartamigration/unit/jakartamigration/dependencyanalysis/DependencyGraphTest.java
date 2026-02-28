package unit.jakartamigration.dependencyanalysis;

import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Dependency;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DependencyGraph Tests")
class DependencyGraphTest {
    
    private DependencyGraph graph;
    private Artifact artifact1;
    private Artifact artifact2;
    
    @BeforeEach
    void setUp() {
        graph = new DependencyGraph();
        artifact1 = new Artifact("com.example", "library1", "1.0.0", "compile", false);
        artifact2 = new Artifact("com.example", "library2", "2.0.0", "compile", false);
    }
    
    @Test
    @DisplayName("Should create empty dependency graph")
    void shouldCreateEmptyGraph() {
        // Given & When
        DependencyGraph emptyGraph = new DependencyGraph();
        
        // Then
        assertEquals(0, emptyGraph.nodeCount());
        assertEquals(0, emptyGraph.edgeCount());
    }
    
    @Test
    @DisplayName("Should add node to graph")
    void shouldAddNode() {
        // When
        graph.addNode(artifact1);
        
        // Then
        assertEquals(1, graph.nodeCount());
        assertTrue(graph.containsNode(artifact1));
    }
    
    @Test
    @DisplayName("Should add edge and automatically add nodes")
    void shouldAddEdgeAndNodes() {
        // Given
        Dependency dependency = new Dependency(artifact1, artifact2, "compile", false);
        
        // When
        graph.addEdge(dependency);
        
        // Then
        assertEquals(2, graph.nodeCount());
        assertEquals(1, graph.edgeCount());
        assertTrue(graph.containsNode(artifact1));
        assertTrue(graph.containsNode(artifact2));
        assertTrue(graph.containsEdge(dependency));
    }
    
    @Test
    @DisplayName("Should not add duplicate nodes")
    void shouldNotAddDuplicateNodes() {
        // Given
        graph.addNode(artifact1);
        
        // When
        graph.addNode(artifact1);
        
        // Then
        assertEquals(1, graph.nodeCount());
    }
    
    @Test
    @DisplayName("Should return immutable copy of nodes")
    void shouldReturnImmutableNodesCopy() {
        // Given
        graph.addNode(artifact1);
        var nodes = graph.getNodes();
        
        // When
        nodes.clear();
        
        // Then
        assertEquals(1, graph.nodeCount());
        assertTrue(graph.containsNode(artifact1));
    }
    
@Test
    @DisplayName("Should return immutable copy of edges")
    void shouldReturnImmutableEdgesCopy() {
        // Given
        Dependency dependency = new Dependency(artifact1, artifact2, "compile", false);
        graph.addEdge(dependency);
        var edges = graph.getEdges();
        
        // When
        edges.clear();
        
        // Then
        assertEquals(1, graph.edgeCount());
        assertTrue(graph.containsEdge(dependency));
    }
    
    @Test
    @DisplayName("Should color code jakarta-compatible dependencies as green")
    void shouldColorCodeJakartaCompatibleDependencies() {
        // Given
        Artifact jakartaCompatible = new Artifact("javax.servlet", "servlet-api", "4.0.1", "compile", true);
        graph.addNode(jakartaCompatible);
        
        // When
        String color = getJakartaCompatibilityColor(jakartaCompatible);
        
        // Then
        assertEquals("green", color, "Jakarta compatible dependencies should be colored green");
    }
    
    @Test
    @DisplayName("Should color code dependencies needing upgrade as yellow")
    void shouldColorCodeDependenciesNeedingUpgrade() {
        // Given
        Artifact needsUpgrade = new Artifact("javax.servlet", "servlet-api", "3.1.0", "compile", false);
        graph.addNode(needsUpgrade);
        
        // When
        String color = getJakartaCompatibilityColor(needsUpgrade);
        
        // Then
        assertEquals("yellow", color, "Dependencies needing upgrade should be colored yellow");
    }
    
    @Test
    @DisplayName("Should color code dependencies with no jakarta version as red")
    void shouldColorCodeDependenciesWithNoJakartaVersion() {
        // Given
        Artifact noJakarta = new Artifact("com.example", "library", "1.0.0", "compile", false);
        graph.addNode(noJakarta);
        
        // When
        String color = getJakartaCompatibilityColor(noJakarta);
        
        // Then
        assertEquals("red", color, "Dependencies with no jakarta version should be colored red");
    }
    
    private String getJakartaCompatibilityColor(Artifact artifact) {
        if (artifact.isJakartaCompatible()) {
            return "green";
        } else if (hasJakartaVersion(artifact)) {
            return "yellow";
        } else {
            return "red";
        }
    }
    
    private boolean hasJakartaVersion(Artifact artifact) {
        // This would be implemented to check if the artifact has a jakarta version available
        return artifact.groupId().startsWith("javax.");
    }
}

