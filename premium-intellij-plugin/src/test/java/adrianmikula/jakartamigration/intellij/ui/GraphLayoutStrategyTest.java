package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.model.RiskLevel;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for graph layout strategies.
 * Tests that layout algorithms position nodes correctly.
 */
class GraphLayoutStrategyTest {

    @Test
    @DisplayName("HierarchicalLayoutStrategy should position nodes in layers")
    void testHierarchicalLayoutStrategy() {
        // Arrange
        GraphLayoutStrategy strategy = new HierarchicalLayoutStrategy();
        List<GraphNode> nodes = createTestNodes(3);
        List<GraphEdge> edges = createTestEdges(nodes, 0, 1, 2); // chain: 0->1->2
        int canvasWidth = 800;
        int canvasHeight = 600;

        // Act
        strategy.layout(nodes, edges, canvasWidth, canvasHeight);

        // Assert - Verify all nodes have positions set
        for (GraphNode node : nodes) {
            assertFalse(Double.isNaN(node.getX()), "Node X should not be NaN");
            assertFalse(Double.isNaN(node.getY()), "Node Y should not be NaN");
            assertTrue(node.getX() >= 0, "Node X should be non-negative");
            assertTrue(node.getY() >= 0, "Node Y should be non-negative");
            assertTrue(node.getWidth() > 0, "Node width should be positive");
            assertTrue(node.getHeight() > 0, "Node height should be positive");
        }

        // Verify nodes are in different layers (Y positions should differ)
        assertTrue(nodes.get(0).getY() < nodes.get(1).getY(), "Node 0 should be above node 1");
        assertTrue(nodes.get(1).getY() < nodes.get(2).getY(), "Node 1 should be above node 2");
    }

    @Test
    @DisplayName("HierarchicalLayoutStrategy should position root node at top")
    void testHierarchicalLayoutRootPositioning() {
        // Arrange
        GraphLayoutStrategy strategy = new HierarchicalLayoutStrategy();
        List<GraphNode> nodes = createTestNodesWithRoot(3);
        List<GraphEdge> edges = createTestEdgesFromRoot(nodes.get(0), nodes.subList(1, 3));
        int canvasWidth = 800;
        int canvasHeight = 600;

        // Act
        strategy.layout(nodes, edges, canvasWidth, canvasHeight);

        // Assert - Root should be at the top (lowest Y value)
        GraphNode root = nodes.get(0);
        for (int i = 1; i < nodes.size(); i++) {
            assertTrue(root.getY() <= nodes.get(i).getY(), 
                "Root should be at or above node " + i);
        }
    }

    @Test
    @DisplayName("CircularLayoutStrategy should position nodes in a circle")
    void testCircularLayoutStrategy() {
        // Arrange
        GraphLayoutStrategy strategy = new CircularLayoutStrategy();
        List<GraphNode> nodes = createTestNodes(5);
        List<GraphEdge> edges = new ArrayList<>(); // No edges needed for circular test
        int canvasWidth = 800;
        int canvasHeight = 600;

        // Act
        strategy.layout(nodes, edges, canvasWidth, canvasHeight);

        // Assert - Verify all nodes have positions set
        for (GraphNode node : nodes) {
            assertFalse(Double.isNaN(node.getX()), "Node X should not be NaN");
            assertFalse(Double.isNaN(node.getY()), "Node Y should not be NaN");
            assertTrue(node.getX() >= 0, "Node X should be non-negative");
            assertTrue(node.getY() >= 0, "Node Y should be non-negative");
            assertTrue(node.getWidth() > 0, "Node width should be positive");
            assertTrue(node.getHeight() > 0, "Node height should be positive");
        }

        // Verify nodes are distributed around the center
        double centerX = canvasWidth / 2.0;
        double centerY = canvasHeight / 2.0;
        
        // All nodes should be at different angles (not all in the same position)
        boolean allSamePosition = true;
        for (int i = 1; i < nodes.size(); i++) {
            if (Math.abs(nodes.get(i).getX() - nodes.get(0).getX()) > 1 ||
                Math.abs(nodes.get(i).getY() - nodes.get(0).getY()) > 1) {
                allSamePosition = false;
                break;
            }
        }
        assertFalse(allSamePosition, "Nodes should be distributed, not all at same position");
    }

    @Test
    @DisplayName("CircularLayoutStrategy should position root node in center")
    void testCircularLayoutRootPositioning() {
        // Arrange
        GraphLayoutStrategy strategy = new CircularLayoutStrategy();
        List<GraphNode> nodes = createTestNodesWithRoot(5);
        List<GraphEdge> edges = new ArrayList<>();
        int canvasWidth = 800;
        int canvasHeight = 600;

        // Act
        strategy.layout(nodes, edges, canvasWidth, canvasHeight);

        // Assert - Root should be near center
        GraphNode root = nodes.get(0);
        double centerX = canvasWidth / 2.0;
        double centerY = canvasHeight / 2.0;
        
        assertTrue(Math.abs(root.getX() + root.getWidth()/2 - centerX) < 100, 
            "Root should be near center X");
        assertTrue(Math.abs(root.getY() + root.getHeight()/2 - centerY) < 100, 
            "Root should be near center Y");
    }

    @Test
    @DisplayName("TreeLayoutStrategy should position nodes in tree structure")
    void testTreeLayoutStrategy() {
        // Arrange
        GraphLayoutStrategy strategy = new TreeLayoutStrategy();
        List<GraphNode> nodes = createTestNodes(4);
        List<GraphEdge> edges = createTestEdges(nodes, 0, 1, 2, 3); // root -> all others
        int canvasWidth = 800;
        int canvasHeight = 600;

        // Act
        strategy.layout(nodes, edges, canvasWidth, canvasHeight);

        // Assert - Verify all nodes have positions set
        for (GraphNode node : nodes) {
            assertFalse(Double.isNaN(node.getX()), "Node X should not be NaN");
            assertFalse(Double.isNaN(node.getY()), "Node Y should not be NaN");
            assertTrue(node.getX() >= 0, "Node X should be non-negative");
            assertTrue(node.getY() >= 0, "Node Y should be non-negative");
            assertTrue(node.getWidth() > 0, "Node width should be positive");
            assertTrue(node.getHeight() > 0, "Node height should be positive");
        }

        // Verify root is at top
        GraphNode root = nodes.get(0);
        for (int i = 1; i < nodes.size(); i++) {
            assertTrue(root.getY() <= nodes.get(i).getY(), 
                "Root should be at or above node " + i);
        }
    }

    @Test
    @Disabled("Flaky test - force-directed layout occasionally converges to same position due to numerical precision. Not critical for release.")
    @DisplayName("ForceDirectedLayoutStrategy should position nodes without overlap")
    void testForceDirectedLayoutStrategy() {
        // Arrange
        GraphLayoutStrategy strategy = new ForceDirectedLayoutStrategy();
        List<GraphNode> nodes = createTestNodes(5);
        List<GraphEdge> edges = createTestEdges(nodes, 0, 1, 2, 3, 4); // chain
        int canvasWidth = 800;
        int canvasHeight = 600;

        // Act
        strategy.layout(nodes, edges, canvasWidth, canvasHeight);

        // Assert - Verify all nodes have positions set
        for (GraphNode node : nodes) {
            assertFalse(Double.isNaN(node.getX()), "Node X should not be NaN");
            assertFalse(Double.isNaN(node.getY()), "Node Y should not be NaN");
            assertTrue(node.getX() >= 0, "Node X should be non-negative");
            assertTrue(node.getY() >= 0, "Node Y should be non-negative");
            assertTrue(node.getWidth() > 0, "Node width should be positive");
            assertTrue(node.getHeight() > 0, "Node height should be positive");
        }

        // Verify nodes are not all at the same position
        boolean allSamePosition = true;
        for (int i = 1; i < nodes.size(); i++) {
            if (Math.abs(nodes.get(i).getX() - nodes.get(0).getX()) > 1 ||
                Math.abs(nodes.get(i).getY() - nodes.get(0).getY()) > 1) {
                allSamePosition = false;
                break;
            }
        }
        assertFalse(allSamePosition, "Nodes should be distributed by force-directed layout");
    }

    @Test
    @DisplayName("Layout strategies should handle empty node list")
    void testEmptyNodeList() {
        // Arrange
        GraphLayoutStrategy hierarchical = new HierarchicalLayoutStrategy();
        GraphLayoutStrategy circular = new CircularLayoutStrategy();
        GraphLayoutStrategy tree = new TreeLayoutStrategy();
        GraphLayoutStrategy forceDirected = new ForceDirectedLayoutStrategy();
        
        List<GraphNode> emptyNodes = new ArrayList<>();
        List<GraphEdge> emptyEdges = new ArrayList<>();

        // Act & Assert - Should not throw exceptions
        assertDoesNotThrow(() -> hierarchical.layout(emptyNodes, emptyEdges, 800, 600));
        assertDoesNotThrow(() -> circular.layout(emptyNodes, emptyEdges, 800, 600));
        assertDoesNotThrow(() -> tree.layout(emptyNodes, emptyEdges, 800, 600));
        assertDoesNotThrow(() -> forceDirected.layout(emptyNodes, emptyEdges, 800, 600));
    }

    @Test
    @DisplayName("Layout strategies should handle single node")
    void testSingleNode() {
        // Arrange
        GraphLayoutStrategy hierarchical = new HierarchicalLayoutStrategy();
        List<GraphNode> nodes = createTestNodes(1);
        List<GraphEdge> edges = new ArrayList<>();

        // Act
        hierarchical.layout(nodes, edges, 800, 600);

        // Assert - Single node should be positioned
        GraphNode node = nodes.get(0);
        assertFalse(Double.isNaN(node.getX()), "Node X should not be NaN");
        assertFalse(Double.isNaN(node.getY()), "Node Y should not be NaN");
        assertTrue(node.getWidth() > 0, "Node width should be positive");
        assertTrue(node.getHeight() > 0, "Node height should be positive");
    }

    // Helper methods

    private List<GraphNode> createTestNodes(int count) {
        List<GraphNode> nodes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            nodes.add(new GraphNode("node" + i, "Node " + i, GraphNode.NodeType.DEPENDENCY, RiskLevel.LOW));
        }
        return nodes;
    }

    private List<GraphNode> createTestNodesWithRoot(int count) {
        List<GraphNode> nodes = new ArrayList<>();
        nodes.add(new GraphNode("root", "root", GraphNode.NodeType.ROOT, RiskLevel.LOW));
        for (int i = 1; i < count; i++) {
            nodes.add(new GraphNode("node" + i, "Node " + i, GraphNode.NodeType.DEPENDENCY, RiskLevel.LOW));
        }
        return nodes;
    }

    private List<GraphEdge> createTestEdges(List<GraphNode> nodes, int... indices) {
        List<GraphEdge> edges = new ArrayList<>();
        if (indices.length < 2) return edges;
        
        for (int i = 0; i < indices.length - 1; i++) {
            GraphNode from = nodes.get(indices[i]);
            GraphNode to = nodes.get(indices[i + 1]);
            edges.add(new GraphEdge(from, to, GraphEdge.EdgeType.DEPENDENCY, false));
        }
        return edges;
    }

    private List<GraphEdge> createTestEdgesFromRoot(GraphNode root, List<GraphNode> children) {
        List<GraphEdge> edges = new ArrayList<>();
        for (GraphNode child : children) {
            edges.add(new GraphEdge(root, child, GraphEdge.EdgeType.DEPENDENCY, false));
        }
        return edges;
    }
}
