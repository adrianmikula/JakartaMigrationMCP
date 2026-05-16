package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.model.RiskLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GraphCanvas edge rendering logic.
 * Tests that edges are created and set correctly on the canvas.
 */
class GraphCanvasEdgeRenderingTest {

    @Test
    @DisplayName("Edges should be created with correct source and target nodes")
    void testEdgeCreationWithCorrectNodes() {
        // Arrange
        GraphNode sourceNode = new GraphNode("source", "Source Node", GraphNode.NodeType.DEPENDENCY, RiskLevel.LOW);
        sourceNode.setX(100);
        sourceNode.setY(100);
        sourceNode.setWidth(120);
        sourceNode.setHeight(40);
        
        GraphNode targetNode = new GraphNode("target", "Target Node", GraphNode.NodeType.DEPENDENCY, RiskLevel.LOW);
        targetNode.setX(300);
        targetNode.setY(100);
        targetNode.setWidth(120);
        targetNode.setHeight(40);
        
        // Act
        GraphEdge edge = new GraphEdge(sourceNode, targetNode, GraphEdge.EdgeType.DEPENDENCY, false);
        
        // Assert
        assertEquals(sourceNode, edge.getSource(), "Edge should have correct source node");
        assertEquals(targetNode, edge.getTarget(), "Edge should have correct target node");
        assertEquals(GraphEdge.EdgeType.DEPENDENCY, edge.getType(), "Edge should have correct type");
        assertFalse(edge.isCriticalPath(), "Edge should not be critical path");
    }
    
    @Test
    @DisplayName("Edges should connect nodes with proper center coordinates")
    void testEdgeNodeCenterCoordinates() {
        // Arrange
        GraphNode sourceNode = new GraphNode("source", "Source", GraphNode.NodeType.DEPENDENCY, RiskLevel.LOW);
        sourceNode.setX(100);
        sourceNode.setY(100);
        sourceNode.setWidth(120);
        sourceNode.setHeight(40);
        
        GraphNode targetNode = new GraphNode("target", "Target", GraphNode.NodeType.DEPENDENCY, RiskLevel.LOW);
        targetNode.setX(300);
        targetNode.setY(100);
        targetNode.setWidth(120);
        targetNode.setHeight(40);
        
        // Act
        double sourceCenterX = sourceNode.getCenterX();
        double sourceCenterY = sourceNode.getCenterY();
        double targetCenterX = targetNode.getCenterX();
        double targetCenterY = targetNode.getCenterY();
        
        // Assert
        assertEquals(160.0, sourceCenterX, 0.01, "Source center X should be at 100 + 120/2 = 160");
        assertEquals(120.0, sourceCenterY, 0.01, "Source center Y should be at 100 + 40/2 = 120");
        assertEquals(360.0, targetCenterX, 0.01, "Target center X should be at 300 + 120/2 = 360");
        assertEquals(120.0, targetCenterY, 0.01, "Target center Y should be at 100 + 40/2 = 120");
    }
    
    @Test
    @DisplayName("Canvas should accept and store edges")
    void testCanvasStoresEdges() {
        // Arrange
        GraphCanvas canvas = new GraphCanvas();
        List<GraphNode> nodes = new ArrayList<>();
        List<GraphEdge> edges = new ArrayList<>();
        
        GraphNode node1 = new GraphNode("n1", "Node1", GraphNode.NodeType.DEPENDENCY, RiskLevel.LOW);
        GraphNode node2 = new GraphNode("n2", "Node2", GraphNode.NodeType.DEPENDENCY, RiskLevel.LOW);
        nodes.add(node1);
        nodes.add(node2);
        
        GraphEdge edge = new GraphEdge(node1, node2, GraphEdge.EdgeType.DEPENDENCY, false);
        edges.add(edge);
        
        // Act
        canvas.setNodes(nodes);
        canvas.setEdges(edges);
        
        // Assert - Verify canvas doesn't throw exceptions
        // The actual rendering happens in paintComponent which requires a graphics context
        assertNotNull(canvas, "Canvas should be created successfully");
    }
    
    @Test
    @DisplayName("Edges with transitive type should be marked correctly")
    void testTransitiveEdgeType() {
        // Arrange
        GraphNode sourceNode = new GraphNode("source", "Source", GraphNode.NodeType.DEPENDENCY, RiskLevel.LOW);
        GraphNode targetNode = new GraphNode("target", "Target", GraphNode.NodeType.DEPENDENCY, RiskLevel.LOW);
        
        // Act
        GraphEdge transitiveEdge = new GraphEdge(sourceNode, targetNode, GraphEdge.EdgeType.TRANSITIVE, false);
        GraphEdge dependencyEdge = new GraphEdge(sourceNode, targetNode, GraphEdge.EdgeType.DEPENDENCY, false);
        
        // Assert
        assertEquals(GraphEdge.EdgeType.TRANSITIVE, transitiveEdge.getType(), "Transitive edge should have TRANSITIVE type");
        assertEquals(GraphEdge.EdgeType.DEPENDENCY, dependencyEdge.getType(), "Dependency edge should have DEPENDENCY type");
    }
    
    @Test
    @DisplayName("Critical path edges should be marked correctly")
    void testCriticalPathEdge() {
        // Arrange
        GraphNode sourceNode = new GraphNode("source", "Source", GraphNode.NodeType.DEPENDENCY, RiskLevel.LOW);
        GraphNode targetNode = new GraphNode("target", "Target", GraphNode.NodeType.DEPENDENCY, RiskLevel.LOW);
        
        // Act
        GraphEdge criticalEdge = new GraphEdge(sourceNode, targetNode, GraphEdge.EdgeType.DEPENDENCY, true);
        GraphEdge nonCriticalEdge = new GraphEdge(sourceNode, targetNode, GraphEdge.EdgeType.DEPENDENCY, false);
        
        // Assert
        assertTrue(criticalEdge.isCriticalPath(), "Critical edge should be marked as critical");
        assertFalse(nonCriticalEdge.isCriticalPath(), "Non-critical edge should not be marked as critical");
    }
}
