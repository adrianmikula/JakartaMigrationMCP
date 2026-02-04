package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.model.RiskLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for GraphEdge class.
 */
class GraphEdgeTest {

    @Test
    void testEdgeCreation() {
        GraphNode source = new GraphNode("source", "Source Node",
            GraphNode.NodeType.ROOT, RiskLevel.LOW);
        GraphNode target = new GraphNode("target", "Target Node",
            GraphNode.NodeType.MODULE, RiskLevel.HIGH);

        GraphEdge edge = new GraphEdge(source, target,
            GraphEdge.EdgeType.DEPENDENCY, true);

        assertEquals(source, edge.getSource());
        assertEquals(target, edge.getTarget());
        assertEquals(GraphEdge.EdgeType.DEPENDENCY, edge.getType());
        assertTrue(edge.isCriticalPath());
    }

    @Test
    void testEdgeTypes() {
        assertEquals(4, GraphEdge.EdgeType.values().length);
        assertNotNull(GraphEdge.EdgeType.DEPENDENCY);
        assertNotNull(GraphEdge.EdgeType.TRANSITIVE);
        assertNotNull(GraphEdge.EdgeType.COMPILES_WITH);
        assertNotNull(GraphEdge.EdgeType.RUNTIME_DEPENDENCY);
    }

    @Test
    void testGetOtherNode() {
        GraphNode source = new GraphNode("source", "Source",
            GraphNode.NodeType.MODULE, RiskLevel.LOW);
        GraphNode target = new GraphNode("target", "Target",
            GraphNode.NodeType.MODULE, RiskLevel.HIGH);

        GraphEdge edge = new GraphEdge(source, target,
            GraphEdge.EdgeType.DEPENDENCY, false);

        assertEquals(target, edge.getOtherNode(source));
        assertEquals(source, edge.getOtherNode(target));
        assertNull(edge.getOtherNode(new GraphNode("other", "Other",
            GraphNode.NodeType.MODULE, RiskLevel.LOW)));
    }

    @Test
    void testNonCriticalPathEdge() {
        GraphNode source = new GraphNode("source", "Source",
            GraphNode.NodeType.MODULE, RiskLevel.LOW);
        GraphNode target = new GraphNode("target", "Target",
            GraphNode.NodeType.MODULE, RiskLevel.MEDIUM);

        GraphEdge edge = new GraphEdge(source, target,
            GraphEdge.EdgeType.TRANSITIVE, false);

        assertFalse(edge.isCriticalPath());
        assertEquals(GraphEdge.EdgeType.TRANSITIVE, edge.getType());
    }
}
