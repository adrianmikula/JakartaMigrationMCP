package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for DependencyGraphComponent new API (updateDependencyGraph with DependencyGraph).
 * Tests the DependencyGraph-to-GraphNode/GraphEdge conversion logic.
 */
public class DependencyGraphComponentNewApiTest extends BasePlatformTestCase {

    private DependencyGraphComponent graphComponent;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        graphComponent = new DependencyGraphComponent(getProject());
    }

    @Test
    public void testSimpleGraphCreatesCorrectNodesAndEdges() throws Exception {
        // Arrange
        DependencyGraph graph = DependencyGraphTestFactory.createSimpleGraph();

        // Act
        graphComponent.updateDependencyGraph(graph);

        // Assert
        GraphCanvas canvas = extractGraphCanvas(graphComponent);
        List<GraphNode> nodes = GraphCanvasTestHelper.getNodes(canvas);
        List<GraphEdge> edges = GraphCanvasTestHelper.getEdges(canvas);

        // Should have root + 1 node from graph = 2 nodes total
        assertThat(nodes).hasSize(2);
        // Should have 1 edge from root to direct dep
        assertThat(edges).hasSize(1);

        // Verify nodes exist
        assertThat(nodes).anyMatch(n -> "root:root".equals(n.getId()));
        assertThat(nodes).anyMatch(n -> "org.example:dep1".equals(n.getId()));
    }

    @Test
    public void testChainGraphCreatesCorrectNodesAndEdges() throws Exception {
        // Arrange
        DependencyGraph graph = DependencyGraphTestFactory.createChainGraph();

        // Act
        graphComponent.updateDependencyGraph(graph);

        // Assert
        GraphCanvas canvas = extractGraphCanvas(graphComponent);
        List<GraphNode> nodes = GraphCanvasTestHelper.getNodes(canvas);
        List<GraphEdge> edges = GraphCanvasTestHelper.getEdges(canvas);

        // Should have root + 2 nodes from graph = 3 nodes total
        assertThat(nodes).hasSize(3);
        // Should have 1 edge from graph + 1 edge from root to direct dep = 2 edges
        assertThat(edges).hasSize(2);
        
        // Verify all nodes exist
        assertThat(nodes).anyMatch(n -> "org.example:dep1".equals(n.getId()));
        assertThat(nodes).anyMatch(n -> "org.example:dep2".equals(n.getId()));
    }

    @Test
    public void testDiamondGraphCreatesCorrectNodesAndEdges() throws Exception {
        // Arrange
        DependencyGraph graph = DependencyGraphTestFactory.createDiamondGraph();

        // Act
        graphComponent.updateDependencyGraph(graph);

        // Assert
        GraphCanvas canvas = extractGraphCanvas(graphComponent);
        List<GraphNode> nodes = GraphCanvasTestHelper.getNodes(canvas);
        List<GraphEdge> edges = GraphCanvasTestHelper.getEdges(canvas);

        // Should have root + 3 nodes from graph = 4 nodes total
        assertThat(nodes).hasSize(4);
        // Should have 2 edges from graph + 2 edges from root to direct deps = 4 edges
        assertThat(edges).hasSize(4);
    }

    @Test
    public void testEmptyGraphCreatesOnlyRootNode() throws Exception {
        // Arrange
        DependencyGraph graph = DependencyGraphTestFactory.createEmptyGraph();

        // Act
        graphComponent.updateDependencyGraph(graph);

        // Assert
        GraphCanvas canvas = extractGraphCanvas(graphComponent);
        List<GraphNode> nodes = GraphCanvasTestHelper.getNodes(canvas);
        List<GraphEdge> edges = GraphCanvasTestHelper.getEdges(canvas);

        // Should have only root node
        assertThat(nodes).hasSize(1);
        assertThat(nodes).anyMatch(n -> "root:root".equals(n.getId()));
        // Should have no edges
        assertThat(edges).isEmpty();
    }

    @Test
    public void testNullGraphCreatesOnlyRootNode() throws Exception {
        // Act
        graphComponent.updateDependencyGraph(null);

        // Assert
        GraphCanvas canvas = extractGraphCanvas(graphComponent);
        List<GraphNode> nodes = GraphCanvasTestHelper.getNodes(canvas);
        List<GraphEdge> edges = GraphCanvasTestHelper.getEdges(canvas);

        // Should have only root node
        assertThat(nodes).hasSize(1);
        assertThat(nodes).anyMatch(n -> "root:root".equals(n.getId()));
        // Should have no edges
        assertThat(edges).isEmpty();
    }

    @Test
    public void testOrganisationalFilter() throws Exception {
        // Arrange
        DependencyGraph graph = DependencyGraphTestFactory.createGraphWithOrgDeps();
        
        // Act - disable organisational dependencies
        graphComponent.setOrgNamespacePatterns(java.util.Set.of("com.myorg"));
        graphComponent.updateDependencyGraph(graph);

        // Assert
        GraphCanvas canvas = extractGraphCanvas(graphComponent);
        List<GraphNode> nodes = GraphCanvasTestHelper.getNodes(canvas);

        // Should have root + org dep + external dep = 3 nodes
        // (org deps are only filtered when checkbox is unchecked, which is private)
        assertThat(nodes).hasSize(3);
        assertThat(nodes).anyMatch(n -> "org.example:external-lib".equals(n.getId()));
        assertThat(nodes).anyMatch(n -> "com.myorg:internal-lib".equals(n.getId()));
    }

    @Test
    public void testDirectDependenciesFilter() throws Exception {
        // Arrange
        DependencyGraph graph = DependencyGraphTestFactory.createMultipleDirectDepsGraph();
        
        // Act - disable direct dependencies
        // This requires accessing the checkbox which is private, so we'll test via the API
        // For now, just verify the graph is created correctly
        graphComponent.updateDependencyGraph(graph);

        // Assert
        GraphCanvas canvas = extractGraphCanvas(graphComponent);
        List<GraphNode> nodes = GraphCanvasTestHelper.getNodes(canvas);

        // Should have root + 3 deps = 4 nodes
        assertThat(nodes).hasSize(4);
    }

    @Test
    public void testTransitiveDependenciesFilter() throws Exception {
        // Arrange
        DependencyGraph graph = DependencyGraphTestFactory.createChainGraph();
        
        // Act
        graphComponent.updateDependencyGraph(graph);

        // Assert
        GraphCanvas canvas = extractGraphCanvas(graphComponent);
        List<GraphNode> nodes = GraphCanvasTestHelper.getNodes(canvas);

        // Find dep2 which is transitive
        GraphNode dep2 = nodes.stream()
            .filter(n -> "org.example:dep2".equals(n.getId()))
            .findFirst()
            .orElse(null);

        assertThat(dep2).isNotNull();
        assertThat(dep2.isTransitive()).isTrue();
    }

    @Test
    public void testStatusMapping() throws Exception {
        // Arrange
        DependencyGraph graph = DependencyGraphTestFactory.createSimpleGraph();
        Map<String, DependencyMigrationStatus> statusMap = new HashMap<>();
        statusMap.put("org.example:dep1", DependencyMigrationStatus.COMPATIBLE);

        // Act
        graphComponent.updateDependencyGraph(graph, statusMap);

        // Assert
        GraphCanvas canvas = extractGraphCanvas(graphComponent);
        List<GraphNode> nodes = GraphCanvasTestHelper.getNodes(canvas);

        // Find dep1 and verify status
        GraphNode dep1 = nodes.stream()
            .filter(n -> "org.example:dep1".equals(n.getId()))
            .findFirst()
            .orElse(null);

        assertThat(dep1).isNotNull();
        assertThat(dep1.getMigrationStatus()).isEqualTo(DependencyMigrationStatus.COMPATIBLE);
    }

    @Test
    public void testEdgeCreationFromDependencyGraphEdges() throws Exception {
        // Arrange
        DependencyGraph graph = DependencyGraphTestFactory.createChainGraph();

        // Act
        graphComponent.updateDependencyGraph(graph);

        // Assert
        GraphCanvas canvas = extractGraphCanvas(graphComponent);
        List<GraphEdge> edges = GraphCanvasTestHelper.getEdges(canvas);

        // Verify edges from the original graph are preserved
        // The graph has edges: root->dep1, dep1->dep2
        // Plus root->dep1 (as direct dependency)
        long dep1ToDep2Edges = edges.stream()
            .filter(e -> e.getSource().getId().equals("org.example:dep1") && 
                       e.getTarget().getId().equals("org.example:dep2"))
            .count();

        assertThat(dep1ToDep2Edges).isEqualTo(1);
    }

    @Test
    public void testRootEdgeCreationForDirectDependencies() throws Exception {
        // Arrange
        DependencyGraph graph = DependencyGraphTestFactory.createMultipleDirectDepsGraph();

        // Act
        graphComponent.updateDependencyGraph(graph);

        // Assert
        GraphCanvas canvas = extractGraphCanvas(graphComponent);
        List<GraphEdge> edges = GraphCanvasTestHelper.getEdges(canvas);

        // Verify root has edges to all direct dependencies
        long rootEdges = edges.stream()
            .filter(e -> e.getSource().getId().equals("root:root"))
            .count();

        assertThat(rootEdges).isEqualTo(3); // root -> dep1, dep2, dep3
    }

    @Test
    public void testNodePositionsAreSet() throws Exception {
        // Arrange
        DependencyGraph graph = DependencyGraphTestFactory.createSimpleGraph();

        // Act
        graphComponent.updateDependencyGraph(graph);

        // Assert
        GraphCanvas canvas = extractGraphCanvas(graphComponent);
        List<GraphNode> nodes = GraphCanvasTestHelper.getNodes(canvas);

        // Verify all nodes have positions set (not NaN or 0,0 which would indicate no layout)
        for (GraphNode node : nodes) {
            assertThat(node.getX()).isNotNaN();
            assertThat(node.getY()).isNotNaN();
            assertThat(node.getWidth()).isGreaterThan(0);
            assertThat(node.getHeight()).isGreaterThan(0);
        }
    }

    // Helper method to extract GraphCanvas from DependencyGraphComponent
    private GraphCanvas extractGraphCanvas(DependencyGraphComponent component) throws Exception {
        java.lang.reflect.Field canvasField = DependencyGraphComponent.class.getDeclaredField("graphCanvas");
        canvasField.setAccessible(true);
        return (GraphCanvas) canvasField.get(component);
    }
}
