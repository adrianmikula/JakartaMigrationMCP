package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.model.RiskLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for GraphNode class.
 */
class GraphNodeTest {

    @Test
    void testNodeCreation() {
        GraphNode node = new GraphNode("test-id", "Test Label",
            GraphNode.NodeType.MODULE, RiskLevel.HIGH);

        assertEquals("test-id", node.getId());
        assertEquals("Test Label", node.getLabel());
        assertEquals(GraphNode.NodeType.MODULE, node.getType());
        assertEquals(RiskLevel.HIGH, node.getRiskLevel());
    }

    @Test
    void testNodePosition() {
        GraphNode node = new GraphNode("test-id", "Test Label",
            GraphNode.NodeType.DEPENDENCY, RiskLevel.LOW);

        node.setX(100);
        node.setY(200);

        assertEquals(100, node.getX());
        assertEquals(200, node.getY());
    }

    @Test
    void testNodeDimensions() {
        GraphNode node = new GraphNode("test-id", "Test Label",
            GraphNode.NodeType.ROOT, RiskLevel.CRITICAL);

        node.setWidth(150);
        node.setHeight(50);

        assertEquals(150, node.getWidth());
        assertEquals(50, node.getHeight());
    }

    @Test
    void testCenterPosition() {
        GraphNode node = new GraphNode("test-id", "Test Label",
            GraphNode.NodeType.MODULE, RiskLevel.MEDIUM);

        node.setX(100);
        node.setY(200);
        node.setWidth(100);
        node.setHeight(40);

        assertEquals(150, node.getCenterX());
        assertEquals(220, node.getCenterY());
    }

    @Test
    void testContains() {
        GraphNode node = new GraphNode("test-id", "Test Label",
            GraphNode.NodeType.MODULE, RiskLevel.LOW);

        node.setX(100);
        node.setY(200);
        node.setWidth(120);
        node.setHeight(40);

        assertTrue(node.contains(110, 210));
        assertTrue(node.contains(100, 200));
        assertTrue(node.contains(220, 240));
        assertFalse(node.contains(99, 200));
        assertFalse(node.contains(221, 200));
        assertFalse(node.contains(100, 199));
        assertFalse(node.contains(100, 241));
    }

    @Test
    void testNodeTypes() {
        assertEquals(3, GraphNode.NodeType.values().length);
        assertNotNull(GraphNode.NodeType.MODULE);
        assertNotNull(GraphNode.NodeType.DEPENDENCY);
        assertNotNull(GraphNode.NodeType.ROOT);
    }
}
