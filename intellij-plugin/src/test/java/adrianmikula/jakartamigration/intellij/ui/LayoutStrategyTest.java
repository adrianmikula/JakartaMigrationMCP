package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.model.RiskLevel;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for graph layout strategies.
 */
class LayoutStrategyTest {

    @Test
    void testHierarchicalLayout() {
        HierarchicalLayoutStrategy layout = new HierarchicalLayoutStrategy();

        List<GraphNode> nodes = new ArrayList<>();
        List<GraphEdge> edges = new ArrayList<>();

        // Create nodes
        GraphNode root = new GraphNode("root", "Root", GraphNode.NodeType.ROOT, RiskLevel.LOW);
        GraphNode node1 = new GraphNode("node1", "Node1", GraphNode.NodeType.MODULE, RiskLevel.HIGH);
        GraphNode node2 = new GraphNode("node2", "Node2", GraphNode.NodeType.MODULE, RiskLevel.MEDIUM);
        GraphNode node3 = new GraphNode("node3", "Node3", GraphNode.NodeType.DEPENDENCY, RiskLevel.CRITICAL);

        nodes.add(root);
        nodes.add(node1);
        nodes.add(node2);
        nodes.add(node3);

        // Create edges
        edges.add(new GraphEdge(root, node1, GraphEdge.EdgeType.DEPENDENCY, true));
        edges.add(new GraphEdge(root, node2, GraphEdge.EdgeType.DEPENDENCY, false));
        edges.add(new GraphEdge(node1, node3, GraphEdge.EdgeType.TRANSITIVE, true));

        layout.layout(nodes, edges, 800, 600);

        // Verify nodes have positions
        for (GraphNode node : nodes) {
            assertTrue(node.getX() >= 0, "Node " + node.getId() + " should have X >= 0");
            assertTrue(node.getY() >= 0, "Node " + node.getId() + " should have Y >= 0");
            assertTrue(node.getWidth() > 0, "Node " + node.getId() + " should have width > 0");
            assertTrue(node.getHeight() > 0, "Node " + node.getId() + " should have height > 0");
        }

        // Root should be at the top (lowest Y)
        assertTrue(root.getY() <= node1.getY());
        assertTrue(root.getY() <= node2.getY());
    }

    @Test
    void testHierarchicalLayoutEmpty() {
        HierarchicalLayoutStrategy layout = new HierarchicalLayoutStrategy();

        List<GraphNode> nodes = new ArrayList<>();
        List<GraphEdge> edges = new ArrayList<>();

        // Should not throw exception
        layout.layout(nodes, edges, 800, 600);
    }

    @Test
    void testCircularLayout() {
        CircularLayoutStrategy layout = new CircularLayoutStrategy();

        List<GraphNode> nodes = new ArrayList<>();
        List<GraphEdge> edges = new ArrayList<>();

        GraphNode root = new GraphNode("root", "Root", GraphNode.NodeType.ROOT, RiskLevel.LOW);
        GraphNode node1 = new GraphNode("node1", "Node1", GraphNode.NodeType.MODULE, RiskLevel.HIGH);
        GraphNode node2 = new GraphNode("node2", "Node2", GraphNode.NodeType.MODULE, RiskLevel.MEDIUM);

        nodes.add(root);
        nodes.add(node1);
        nodes.add(node2);

        layout.layout(nodes, edges, 800, 600);

        // Verify nodes have positions
        for (GraphNode node : nodes) {
            assertTrue(node.getX() >= 0);
            assertTrue(node.getY() >= 0);
        }
    }

    @Test
    void testCircularLayoutEmpty() {
        CircularLayoutStrategy layout = new CircularLayoutStrategy();

        List<GraphNode> nodes = new ArrayList<>();
        List<GraphEdge> edges = new ArrayList<>();

        layout.layout(nodes, edges, 800, 600);
    }

    @Test
    void testTreeLayout() {
        TreeLayoutStrategy layout = new TreeLayoutStrategy();

        List<GraphNode> nodes = new ArrayList<>();
        List<GraphEdge> edges = new ArrayList<>();

        GraphNode root = new GraphNode("root", "Root", GraphNode.NodeType.ROOT, RiskLevel.LOW);
        GraphNode node1 = new GraphNode("node1", "Node1", GraphNode.NodeType.MODULE, RiskLevel.HIGH);
        GraphNode node2 = new GraphNode("node2", "Node2", GraphNode.NodeType.MODULE, RiskLevel.MEDIUM);

        nodes.add(root);
        nodes.add(node1);
        nodes.add(node2);

        edges.add(new GraphEdge(root, node1, GraphEdge.EdgeType.DEPENDENCY, true));
        edges.add(new GraphEdge(root, node2, GraphEdge.EdgeType.DEPENDENCY, false));

        layout.layout(nodes, edges, 800, 600);

        // Verify nodes have positions
        for (GraphNode node : nodes) {
            assertTrue(node.getX() >= 0);
            assertTrue(node.getY() >= 0);
        }

        // Root should be above children
        assertTrue(root.getY() < node1.getY());
        assertTrue(root.getY() < node2.getY());
    }

    @Test
    void testTreeLayoutEmpty() {
        TreeLayoutStrategy layout = new TreeLayoutStrategy();

        List<GraphNode> nodes = new ArrayList<>();
        List<GraphEdge> edges = new ArrayList<>();

        layout.layout(nodes, edges, 800, 600);
    }

    @Test
    void testForceDirectedLayout() {
        ForceDirectedLayoutStrategy layout = new ForceDirectedLayoutStrategy();

        List<GraphNode> nodes = new ArrayList<>();
        List<GraphEdge> edges = new ArrayList<>();

        GraphNode root = new GraphNode("root", "Root", GraphNode.NodeType.ROOT, RiskLevel.LOW);
        GraphNode node1 = new GraphNode("node1", "Node1", GraphNode.NodeType.MODULE, RiskLevel.HIGH);
        GraphNode node2 = new GraphNode("node2", "Node2", GraphNode.NodeType.MODULE, RiskLevel.MEDIUM);

        nodes.add(root);
        nodes.add(node1);
        nodes.add(node2);

        edges.add(new GraphEdge(root, node1, GraphEdge.EdgeType.DEPENDENCY, true));
        edges.add(new GraphEdge(root, node2, GraphEdge.EdgeType.DEPENDENCY, false));

        layout.layout(nodes, edges, 800, 600);

        // Verify nodes have positions
        for (GraphNode node : nodes) {
            assertTrue(node.getX() >= 0);
            assertTrue(node.getY() >= 0);
        }
    }

    @Test
    void testForceDirectedLayoutEmpty() {
        ForceDirectedLayoutStrategy layout = new ForceDirectedLayoutStrategy();

        List<GraphNode> nodes = new ArrayList<>();
        List<GraphEdge> edges = new ArrayList<>();

        layout.layout(nodes, edges, 800, 600);
    }
}
