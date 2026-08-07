package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the full dependency graph rendering pipeline.
 * Tests the complete data flow from input to GraphCanvas.
 */
public class DependencyGraphRenderingIntegrationTest extends BasePlatformTestCase {

    private DependencyGraphComponent graphComponent;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        graphComponent = new DependencyGraphComponent(getProject());
    }

    public void testLegacyApiFullPipeline() throws Exception {
        // Arrange
        List<DependencyInfo> deps = DependencyInfoTestFactory.createMultipleDependenciesList();

        // Act
        graphComponent.updateGraph(deps);

        // Assert - Verify complete pipeline
        GraphCanvas canvas = extractGraphCanvas(graphComponent);
        List<GraphNode> nodes = GraphCanvasTestHelper.getNodes(canvas);
        List<GraphEdge> edges = GraphCanvasTestHelper.getEdges(canvas);

        // Verify node count
        assertThat(nodes).hasSize(4); // root + 3 deps
        
        // Verify edge count
        assertThat(edges).hasSize(3); // root -> each dep
        
        // Verify all nodes have positions set (layout was applied)
        for (GraphNode node : nodes) {
            assertThat(node.getX()).isNotNaN();
            assertThat(node.getY()).isNotNaN();
            assertThat(node.getWidth()).isGreaterThan(0);
            assertThat(node.getHeight()).isGreaterThan(0);
        }

        // Verify layout strategy was applied
        GraphLayoutStrategy strategy = GraphCanvasTestHelper.getLayoutStrategy(canvas);
        assertThat(strategy).isNotNull();
    }

    public void testNewApiFullPipeline() throws Exception {
        // Arrange
        DependencyGraph graph = DependencyGraphTestFactory.createChainGraph();
        Map<String, DependencyMigrationStatus> statusMap = new HashMap<>();
        statusMap.put("org.example:dep1", DependencyMigrationStatus.COMPATIBLE);
        statusMap.put("org.example:dep2", DependencyMigrationStatus.NEEDS_UPGRADE);

        // Act
        graphComponent.updateDependencyGraph(graph, statusMap);

        // Assert - Verify complete pipeline
        GraphCanvas canvas = extractGraphCanvas(graphComponent);
        List<GraphNode> nodes = GraphCanvasTestHelper.getNodes(canvas);
        List<GraphEdge> edges = GraphCanvasTestHelper.getEdges(canvas);

        // Verify node count
        assertThat(nodes).hasSize(4); // root + 3 from graph
        
        // Verify edge count
        assertThat(edges).hasSize(3); // 2 from graph + 1 from root to direct dep
        
        // Verify all nodes have positions set
        for (GraphNode node : nodes) {
            assertThat(node.getX()).isNotNaN();
            assertThat(node.getY()).isNotNaN();
            assertThat(node.getWidth()).isGreaterThan(0);
            assertThat(node.getHeight()).isGreaterThan(0);
        }

        // Verify status mapping was applied
        GraphNode dep1 = nodes.stream()
            .filter(n -> "org.example:dep1".equals(n.getId()))
            .findFirst()
            .orElse(null);
        assertThat(dep1).isNotNull();
        assertThat(dep1.getMigrationStatus()).isEqualTo(DependencyMigrationStatus.COMPATIBLE);

        GraphNode dep2 = nodes.stream()
            .filter(n -> "org.example:dep2".equals(n.getId()))
            .findFirst()
            .orElse(null);
        assertThat(dep2).isNotNull();
        assertThat(dep2.getMigrationStatus()).isEqualTo(DependencyMigrationStatus.NEEDS_UPGRADE);
    }

    public void testFilteringIntegration() throws Exception {
        // Arrange
        DependencyGraph graph = DependencyGraphTestFactory.createGraphWithOrgDeps();
        graphComponent.setOrgNamespacePatterns(java.util.Set.of("com.myorg"));

        // Act
        graphComponent.updateDependencyGraph(graph);

        // Assert - Verify filtering works end-to-end
        GraphCanvas canvas = extractGraphCanvas(graphComponent);
        List<GraphNode> nodes = GraphCanvasTestHelper.getNodes(canvas);

        // Should have root + external dep only (org dep filtered out)
        assertThat(nodes).hasSize(2);
        assertThat(nodes).anyMatch(n -> "org.example:external-lib".equals(n.getId()));
        assertThat(nodes).noneMatch(n -> "com.myorg:internal-lib".equals(n.getId()));
    }

    public void testStatusIntegration() throws Exception {
        // Arrange
        DependencyGraph graph = DependencyGraphTestFactory.createSimpleGraph();
        Map<String, DependencyMigrationStatus> statusMap = new HashMap<>();
        statusMap.put("org.example:dep1", DependencyMigrationStatus.NO_JAKARTA_VERSION);

        // Act
        graphComponent.updateDependencyGraph(graph, statusMap);

        // Assert - Verify status mapping affects node rendering
        GraphCanvas canvas = extractGraphCanvas(graphComponent);
        List<GraphNode> nodes = GraphCanvasTestHelper.getNodes(canvas);

        GraphNode dep1 = nodes.stream()
            .filter(n -> "org.example:dep1".equals(n.getId()))
            .findFirst()
            .orElse(null);

        assertThat(dep1).isNotNull();
        assertThat(dep1.getMigrationStatus()).isEqualTo(DependencyMigrationStatus.NO_JAKARTA_VERSION);
    }

    public void testEmptyGraphPipeline() throws Exception {
        // Arrange
        DependencyGraph graph = DependencyGraphTestFactory.createEmptyGraph();

        // Act
        graphComponent.updateDependencyGraph(graph);

        // Assert
        GraphCanvas canvas = extractGraphCanvas(graphComponent);
        List<GraphNode> nodes = GraphCanvasTestHelper.getNodes(canvas);
        List<GraphEdge> edges = GraphCanvasTestHelper.getEdges(canvas);

        assertThat(nodes).hasSize(1); // Only root
        assertThat(edges).isEmpty();
    }

    public void testNullGraphPipeline() throws Exception {
        // Act
        graphComponent.updateDependencyGraph(null);

        // Assert
        GraphCanvas canvas = extractGraphCanvas(graphComponent);
        List<GraphNode> nodes = GraphCanvasTestHelper.getNodes(canvas);
        List<GraphEdge> edges = GraphCanvasTestHelper.getEdges(canvas);

        assertThat(nodes).hasSize(1); // Only root
        assertThat(edges).isEmpty();
    }

    public void testLegacyApiWithNullVersionPipeline() throws Exception {
        // Arrange
        List<DependencyInfo> deps = List.of(DependencyInfoTestFactory.createDependencyWithNullVersion());

        // Act
        graphComponent.updateGraph(deps);

        // Assert - Should handle gracefully
        GraphCanvas canvas = extractGraphCanvas(graphComponent);
        List<GraphNode> nodes = GraphCanvasTestHelper.getNodes(canvas);

        assertThat(nodes).hasSize(2); // root + 1 dep
    }

    public void testDiamondGraphPipeline() throws Exception {
        // Arrange
        DependencyGraph graph = DependencyGraphTestFactory.createDiamondGraph();

        // Act
        graphComponent.updateDependencyGraph(graph);

        // Assert
        GraphCanvas canvas = extractGraphCanvas(graphComponent);
        List<GraphNode> nodes = GraphCanvasTestHelper.getNodes(canvas);
        List<GraphEdge> edges = GraphCanvasTestHelper.getEdges(canvas);

        // Should have root + 4 from graph = 5 nodes
        assertThat(nodes).hasSize(5);
        // Should have 4 from graph + 2 from root to direct deps = 6 edges
        assertThat(edges).hasSize(6);
    }

    public void testLayoutSelectionBasedOnNodeCount() throws Exception {
        // Test that optimal layout is selected based on node count
        // This tests the selectOptimalLayout method indirectly
        
        // Arrange - Small graph (<=5 nodes)
        DependencyGraph smallGraph = DependencyGraphTestFactory.createSimpleGraph(); // 2 nodes + root = 3
        
        // Act
        graphComponent.updateDependencyGraph(smallGraph);
        
        // Assert - Should select Tree layout for <=5 nodes
        GraphCanvas canvas = extractGraphCanvas(graphComponent);
        GraphLayoutStrategy strategy = GraphCanvasTestHelper.getLayoutStrategy(canvas);
        assertThat(strategy).isInstanceOf(TreeLayoutStrategy.class);
    }

    public void testMultipleUpdatesPipeline() throws Exception {
        // Arrange
        DependencyGraph graph1 = DependencyGraphTestFactory.createSimpleGraph();
        DependencyGraph graph2 = DependencyGraphTestFactory.createChainGraph();

        // Act - First update
        graphComponent.updateDependencyGraph(graph1);
        GraphCanvas canvas = extractGraphCanvas(graphComponent);
        List<GraphNode> nodes1 = GraphCanvasTestHelper.getNodes(canvas);
        
        // Assert first update
        assertThat(nodes1).hasSize(3);

        // Act - Second update
        graphComponent.updateDependencyGraph(graph2);
        List<GraphNode> nodes2 = GraphCanvasTestHelper.getNodes(canvas);
        
        // Assert second update replaced the first
        assertThat(nodes2).hasSize(4);
    }

    public void testQuickScanThenDeepScanEmptyGraphPreservesFlatDeps() throws Exception {
        // Simulates MigrationToolWindow.performDeepScan sequence:
        // 1. dependencyUIManager.updateAllDependencies(depInfos)  (flat list)
        // 2. dependencyGraphComponent.updateGraphFromDependencyGraph(deepGraph, statusMap)  (empty graph overwrite)

        // Arrange - flat deps from quick scan
        List<DependencyInfo> flatDeps = DependencyInfoTestFactory.createMultipleDependenciesList();
        Map<String, DependencyMigrationStatus> statusMap = new HashMap<>();
        statusMap.put("org.example:dep1", DependencyMigrationStatus.COMPATIBLE);
        statusMap.put("org.example:dep2", DependencyMigrationStatus.NEEDS_UPGRADE);
        statusMap.put("org.example:dep3", DependencyMigrationStatus.NO_JAKARTA_VERSION);

        // Act - step 1: load flat deps
        graphComponent.updateGraph(flatDeps);

        // Verify flat deps rendered
        GraphCanvas canvas = extractGraphCanvas(graphComponent);
        List<GraphNode> nodesAfterFlat = GraphCanvasTestHelper.getNodes(canvas);
        assertThat(nodesAfterFlat).hasSize(4); // root + 3

        // Act - step 2: empty graph overwrite (deep scan returned no edges)
        DependencyGraph emptyGraph = DependencyGraphTestFactory.createEmptyGraph();
        graphComponent.updateGraphFromDependencyGraph(emptyGraph, statusMap);

        // Assert - fallback should preserve flat deps with status colours
        List<GraphNode> nodesAfterFallback = GraphCanvasTestHelper.getNodes(canvas);
        assertThat(nodesAfterFallback).hasSize(4); // root + 3 (not just root)
        assertThat(nodesAfterFallback).anyMatch(n -> "org.example:dep1".equals(n.getId()));
        assertThat(nodesAfterFallback).anyMatch(n -> "org.example:dep2".equals(n.getId()));
        assertThat(nodesAfterFallback).anyMatch(n -> "org.example:dep3".equals(n.getId()));

        // Verify status colours preserved
        GraphNode dep1 = nodesAfterFallback.stream()
            .filter(n -> "org.example:dep1".equals(n.getId()))
            .findFirst()
            .orElse(null);
        assertThat(dep1).isNotNull();
        assertThat(dep1.getMigrationStatus()).isEqualTo(DependencyMigrationStatus.COMPATIBLE);

        GraphNode dep2 = nodesAfterFallback.stream()
            .filter(n -> "org.example:dep2".equals(n.getId()))
            .findFirst()
            .orElse(null);
        assertThat(dep2).isNotNull();
        assertThat(dep2.getMigrationStatus()).isEqualTo(DependencyMigrationStatus.NEEDS_UPGRADE);
    }

    // Helper method to extract GraphCanvas from DependencyGraphComponent
    private GraphCanvas extractGraphCanvas(DependencyGraphComponent component) throws Exception {
        java.lang.reflect.Field canvasField = DependencyGraphComponent.class.getDeclaredField("graphCanvas");
        canvasField.setAccessible(true);
        return (GraphCanvas) canvasField.get(component);
    }
}
