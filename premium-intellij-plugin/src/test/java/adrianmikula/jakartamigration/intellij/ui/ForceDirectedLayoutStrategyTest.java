package adrianmikula.jakartamigration.intellij.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;

import adrianmikula.jakartamigration.intellij.model.RiskLevel;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ForceDirectedLayoutStrategy dynamic spacing behavior.
 * Tests that node spacing automatically adjusts based on total node count.
 *
 * @see ADR-0065 (Dynamic Graph Spacing and Standard Panning)
 * @see ForceDirectedLayoutStrategy
 */
class ForceDirectedLayoutStrategyTest {

    private ForceDirectedLayoutStrategy strategy = new ForceDirectedLayoutStrategy();
    private static final int CANVAS_WIDTH = 1200;
    private static final int CANVAS_HEIGHT = 800;

    @Test
    @DisplayName("Layout with 10 nodes should produce larger nodes and spacing than 100 nodes")
    void testNodeSizeDecreasesWithCount() {
        // Arrange: create 10 nodes
        List<GraphNode> nodes10 = createNodes(10);
        List<GraphEdge> edges10 = createEdges(nodes10);

        // Act: layout
        strategy.layout(nodes10, edges10, CANVAS_WIDTH, CANVAS_HEIGHT);

        double avgWidth10 = averageWidth(nodes10);
        double avgHeight10 = averageHeight(nodes10);
        double avgSeparation10 = averageClosestDistance(nodes10);

        // Arrange: create 100 nodes
        List<GraphNode> nodes100 = createNodes(100);
        List<GraphEdge> edges100 = createEdges(nodes100);

        // Act: layout
        strategy.layout(nodes100, edges100, CANVAS_WIDTH, CANVAS_HEIGHT);

        double avgWidth100 = averageWidth(nodes100);
        double avgHeight100 = averageHeight(nodes100);
        double avgSeparation100 = averageClosestDistance(nodes100);

        // Assert: smaller graphs should have larger nodes and larger separation
        assertTrue(avgWidth10 > avgWidth100, "Average width should be larger for 10 nodes than 100 nodes");
        assertTrue(avgHeight10 > avgHeight100, "Average height should be larger for 10 nodes than 100 nodes");
        assertTrue(avgSeparation10 > avgSeparation100, "Average separation should be larger for 10 nodes than 100 nodes");
    }

    @Test
    @DisplayName("Layout with 1000 nodes should have minimum node sizes to remain readable")
    void testMinimumNodeSizesForLargeGraphs() {
        // Arrange
        List<GraphNode> nodes = createNodes(1000);
        List<GraphEdge> edges = createEdges(nodes);

        // Act
        strategy.layout(nodes, edges, CANVAS_WIDTH, CANVAS_HEIGHT);

        // Assert: all nodes meet minimum size constraints
        for (GraphNode node : nodes) {
            assertTrue(node.getWidth() >= 60, "Node width should be at least 60 for readability");
            assertTrue(node.getHeight() >= 25, "Node height should be at least 25 for readability");
        }
    }

    @Test
    @DisplayName("Layout should produce no overlapping nodes for any count")
    void testNoOverlappingNodes() {
        // Test various node counts
        int[] counts = {5, 20, 50, 150, 500};
        for (int count : counts) {
            List<GraphNode> nodes = createNodes(count);
            List<GraphEdge> edges = createEdges(nodes);
            strategy.layout(nodes, edges, CANVAS_WIDTH, CANVAS_HEIGHT);

            assertNoOverlap(nodes, "Overlap detected for count=" + count);
        }
    }

    @Test
    @DisplayName("Layout should center graph within canvas")
    void testGraphCentering() {
        // Arrange
        List<GraphNode> nodes = createNodes(30);
        List<GraphEdge> edges = createEdges(nodes);
        strategy.layout(nodes, edges, CANVAS_WIDTH, CANVAS_HEIGHT);

        // Act: compute average center
        double sumX = 0, sumY = 0;
        for (GraphNode node : nodes) {
            sumX += node.getCenterX();
            sumY += node.getCenterY();
        }
        double avgX = sumX / nodes.size();
        double avgY = sumY / nodes.size();

        // Assert: graph center should be close to canvas center
        double centerTolerance = 20; // Allow some deviation
        assertEquals(CANVAS_WIDTH / 2.0, avgX, centerTolerance, "Graph should be centered horizontally");
        assertEquals(CANVAS_HEIGHT / 2.0, avgY, centerTolerance, "Graph should be centered vertically");
    }

    @Test
    @DisplayName("Layout should converge within reasonable iterations for 200 nodes")
    void testConvergenceForModerateGraphs() {
        // Arrange
        List<GraphNode> nodes = createNodes(200);
        List<GraphEdge> edges = createEdges(nodes);

        // Act: layout runs with adaptive iteration count
        strategy.layout(nodes, edges, CANVAS_WIDTH, CANVAS_HEIGHT);

        // Assert: all positions are finite and not NaN
        for (GraphNode node : nodes) {
            assertTrue(Double.isFinite(node.getX()), "Node X should be finite");
            assertTrue(Double.isFinite(node.getY()), "Node Y should be finite");
            assertTrue(node.getWidth() >= 60, "Node should have minimum readable width");
            assertTrue(node.getHeight() >= 25, "Node should have minimum readable height");
        }
    }

    // Helper methods

    private List<GraphNode> createNodes(int count) {
        List<GraphNode> nodes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            GraphNode node = new GraphNode(
                "node-" + i,
                "Node " + i,
                GraphNode.NodeType.DEPENDENCY,
                RiskLevel.LOW
            );
            nodes.add(node);
        }
        return nodes;
    }

    private List<GraphEdge> createEdges(List<GraphNode> nodes) {
        List<GraphEdge> edges = new ArrayList<>();
        // Create a simple chain to ensure graph connectivity
        for (int i = 0; i < nodes.size() - 1; i++) {
            edges.add(new GraphEdge(nodes.get(i), nodes.get(i+1), GraphEdge.EdgeType.DEPENDENCY, false));
        }
        // Add a few random edges for more realism
        if (nodes.size() >= 3) {
            edges.add(new GraphEdge(nodes.get(0), nodes.get(2), GraphEdge.EdgeType.DEPENDENCY, false));
        }
        if (nodes.size() >= 5) {
            edges.add(new GraphEdge(nodes.get(1), nodes.get(4), GraphEdge.EdgeType.DEPENDENCY, false));
        }
        return edges;
    }

    private double averageWidth(List<GraphNode> nodes) {
        double sum = 0;
        for (GraphNode n : nodes) sum += n.getWidth();
        return sum / nodes.size();
    }

    private double averageHeight(List<GraphNode> nodes) {
        double sum = 0;
        for (GraphNode n : nodes) sum += n.getHeight();
        return sum / nodes.size();
    }

    private double averageClosestDistance(List<GraphNode> nodes) {
        double sum = 0;
        int count = 0;
        for (int i = 0; i < nodes.size(); i++) {
            GraphNode a = nodes.get(i);
            double minDist = Double.MAX_VALUE;
            for (int j = 0; j < nodes.size(); j++) {
                if (i == j) continue;
                GraphNode b = nodes.get(j);
                double dx = a.getCenterX() - b.getCenterX();
                double dy = a.getCenterY() - b.getCenterY();
                double dist = Math.sqrt(dx*dx + dy*dy);
                if (dist < minDist) minDist = dist;
            }
            sum += minDist;
            count++;
        }
        return count > 0 ? sum / count : 0;
    }

    private void assertNoOverlap(List<GraphNode> nodes, String message) {
        for (int i = 0; i < nodes.size(); i++) {
            GraphNode a = nodes.get(i);
            for (int j = i+1; j < nodes.size(); j++) {
                GraphNode b = nodes.get(j);
                if (rectanglesOverlap(a, b)) {
                    fail(message + ": nodes " + a.getId() + " and " + b.getId() + " overlap");
                }
            }
        }
    }

    private boolean rectanglesOverlap(GraphNode a, GraphNode b) {
        return a.getX() < b.getX() + b.getWidth() &&
               a.getX() + a.getWidth() > b.getX() &&
               a.getY() < b.getY() + b.getHeight() &&
               a.getY() + a.getHeight() > b.getY();
    }
}
