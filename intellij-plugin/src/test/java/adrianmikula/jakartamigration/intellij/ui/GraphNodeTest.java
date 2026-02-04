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
        assertEquals(4, GraphNode.NodeType.values().length);
        assertNotNull(GraphNode.NodeType.MODULE);
        assertNotNull(GraphNode.NodeType.DEPENDENCY);
        assertNotNull(GraphNode.NodeType.ROOT);
        assertNotNull(GraphNode.NodeType.ORG_INTERNAL);
    }

    @Test
    void testOrgInternalFlag() {
        GraphNode node = new GraphNode("test-id", "Test Label",
            GraphNode.NodeType.MODULE, RiskLevel.LOW);

        // Default should be false
        assertFalse(node.isOrgInternal());
        assertFalse(node.isAnalyzedForMigration());

        // Set and verify
        node.setOrgInternal(true);
        node.setAnalyzedForMigration(true);

        assertTrue(node.isOrgInternal());
        assertTrue(node.isAnalyzedForMigration());
    }

    @Test
    void testOrgInternalNodeCreation() {
        GraphNode node = new GraphNode("com.myorg:internal-lib", "internal-lib",
            GraphNode.NodeType.ORG_INTERNAL, RiskLevel.HIGH, true, false);

        assertEquals("com.myorg:internal-lib", node.getId());
        assertEquals("internal-lib", node.getLabel());
        assertEquals(GraphNode.NodeType.ORG_INTERNAL, node.getType());
        assertEquals(RiskLevel.HIGH, node.getRiskLevel());
        assertTrue(node.isOrgInternal());
        assertFalse(node.isAnalyzedForMigration());
    }

    @Test
    void testAnalyzedOrgNode() {
        GraphNode node = new GraphNode("com.myorg:analyzed-lib", "analyzed-lib",
            GraphNode.NodeType.ORG_INTERNAL, RiskLevel.MEDIUM, true, true);

        assertTrue(node.isOrgInternal());
        assertTrue(node.isAnalyzedForMigration());
    }
}
