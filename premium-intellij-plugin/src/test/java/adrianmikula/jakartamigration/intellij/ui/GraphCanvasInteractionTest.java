package adrianmikula.jakartamigration.intellij.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

import adrianmikula.jakartamigration.intellij.model.RiskLevel;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GraphCanvas mouse interaction behaviors:
 * - Left-click drag panning with threshold
 * - Left-click tap selection
 * - Non-left button panning (backward compatibility)
 *
 * @see ADR-0065 (Dynamic Graph Spacing and Standard Panning)
 * @see GraphCanvas
 */
class GraphCanvasInteractionTest {

    // Simple layout strategy for tests: positions nodes at predefined coordinates
    private static class TestLayoutStrategy implements GraphLayoutStrategy {
        private final double nodeX;
        private final double nodeY;

        TestLayoutStrategy(double nodeX, double nodeY) {
            this.nodeX = nodeX;
            this.nodeY = nodeY;
        }

        @Override
        public void layout(java.util.List<GraphNode> nodes, java.util.List<GraphEdge> edges, int canvasWidth, int canvasHeight) {
            for (GraphNode node : nodes) {
                node.setX(nodeX);
                node.setY(nodeY);
                node.setWidth(120);
                node.setHeight(40);
            }
        }
    }

    @Test
    @DisplayName("Left button drag with movement > threshold should pan the view")
    void testLeftDragPans() throws Exception {
        // Arrange
        GraphCanvas canvas = new GraphCanvas();
        canvas.setNodes(createNodes(1)); // one node not needed for panning but for context
        // Use a dummy layout to avoid NPE on edges
        canvas.setLayoutStrategy(new TestLayoutStrategy(100, 100));

        // Initial offsets
        setOffsets(canvas, 0, 0);

        // Simulate: mouse press
        MouseEvent press = createMouseEvent(canvas, MouseEvent.MOUSE_PRESSED, 100, 100, 1, InputEvent.BUTTON1_DOWN_MASK);
        invokePrivate(canvas, "handleMousePressed", press);

        // Simulate: mouse drag to (150, 150) - delta (50,50) exceeds threshold
        MouseEvent drag = createMouseEvent(canvas, MouseEvent.MOUSE_DRAGGED, 150, 150, 0, InputEvent.BUTTON1_DOWN_MASK);
        invokePrivate(canvas, "handleMouseDragged", drag);

        // Assert: offsets changed by 50
        assertEquals(50, getOffsetX(canvas), "Offset X should increase by drag delta X");
        assertEquals(50, getOffsetY(canvas), "Offset Y should increase by drag delta Y");
    }

    @Test
    @DisplayName("Left button click with small movement should select node, not pan")
    void testLeftClickSelectsNode() throws Exception {
        // Arrange: canvas with one node at (100, 100) size 120x40
        GraphCanvas canvas = new GraphCanvas();
        GraphNode node = createNode("test", "Test Node");
        canvas.setNodes(java.util.Collections.singletonList(node));
        canvas.setLayoutStrategy(new TestLayoutStrategy(100, 100));
        setOffsets(canvas, 0, 0);

        // Simulate: press at point inside node (110, 110)
        MouseEvent press = createMouseEvent(canvas, MouseEvent.MOUSE_PRESSED, 110, 110, 1, InputEvent.BUTTON1_DOWN_MASK);
        invokePrivate(canvas, "handleMousePressed", press);

        // Simulate: release without moving beyond threshold (press at (110,110), release at (112,112) -> dist ~2.8)
        MouseEvent release = createMouseEvent(canvas, MouseEvent.MOUSE_RELEASED, 112, 112, 1, InputEvent.BUTTON1_DOWN_MASK);
        invokePrivate(canvas, "handleMouseReleased", release);

        // Assert: node selected, offsets unchanged
        assertEquals(node, getSelectedNode(canvas), "Node should be selected on tap");
        assertEquals(0, getOffsetX(canvas), "Offset X should remain 0");
        assertEquals(0, getOffsetY(canvas), "Offset Y should remain 0");
    }

    @Test
    @DisplayName("Left button drag starting inside node should not select node once dragging begins")
    void testDragFromNodeClearsSelection() throws Exception {
        // Arrange: node at (100,100)
        GraphCanvas canvas = new GraphCanvas();
        GraphNode node = createNode("test", "Test Node");
        canvas.setNodes(java.util.Collections.singletonList(node));
        canvas.setLayoutStrategy(new TestLayoutStrategy(100, 100));
        setOffsets(canvas, 0, 0);

        // Press inside node
        MouseEvent press = createMouseEvent(canvas, MouseEvent.MOUSE_PRESSED, 110, 110, 1, InputEvent.BUTTON1_DOWN_MASK);
        invokePrivate(canvas, "handleMousePressed", press);

        // Drag beyond threshold (move 10 pixels)
        MouseEvent drag1 = createMouseEvent(canvas, MouseEvent.MOUSE_DRAGGED, 120, 120, 0, InputEvent.BUTTON1_DOWN_MASK);
        invokePrivate(canvas, "handleMouseDragged", drag1);
        // At this point, isDragging should be true, selection cleared

        // Release
        MouseEvent release = createMouseEvent(canvas, MouseEvent.MOUSE_RELEASED, 120, 120, 1, InputEvent.BUTTON1_DOWN_MASK);
        invokePrivate(canvas, "handleMouseReleased", release);

        // Assert: no node selected (drag cleared selection)
        assertNull(getSelectedNode(canvas), "Selection should be cleared after dragging");
        assertEquals(10, getOffsetX(canvas), "Offset X should reflect panning");
        assertEquals(10, getOffsetY(canvas), "Offset Y should reflect panning");
    }

    @Test
    @DisplayName("Middle/right button drag should pan immediately (backward compatibility)")
    void testNonLeftButtonPansImmediately() throws Exception {
        // Arrange
        GraphCanvas canvas = new GraphCanvas();
        canvas.setNodes(createNodes(1));
        canvas.setLayoutStrategy(new TestLayoutStrategy(100, 100));
        setOffsets(canvas, 0, 0);

        // Simulate right button press (BUTTON3)
        MouseEvent press = createMouseEvent(canvas, MouseEvent.MOUSE_PRESSED, 100, 100, 1, InputEvent.BUTTON3_DOWN_MASK);
        invokePrivate(canvas, "handleMousePressed", press);

        // Simulate right button drag to (130,130)
        MouseEvent drag = createMouseEvent(canvas, MouseEvent.MOUSE_DRAGGED, 130, 130, 0, InputEvent.BUTTON3_DOWN_MASK);
        invokePrivate(canvas, "handleMouseDragged", drag);

        // Assert: panning occurred immediately without threshold
        assertEquals(30, getOffsetX(canvas));
        assertEquals(30, getOffsetY(canvas));
    }

    @Test
    @DisplayName("Click on empty background with left button should not select any node")
    void testClickOnEmptyBackground() throws Exception {
        // Arrange: node at (300,300), click at (50,50) empty
        GraphCanvas canvas = new GraphCanvas();
        GraphNode node = createNode("test", "Test Node");
        canvas.setNodes(java.util.Collections.singletonList(node));
        canvas.setLayoutStrategy(new TestLayoutStrategy(300, 300));
        setOffsets(canvas, 0, 0);

        // Simulate press in empty area
        MouseEvent press = createMouseEvent(canvas, MouseEvent.MOUSE_PRESSED, 50, 50, 1, InputEvent.BUTTON1_DOWN_MASK);
        invokePrivate(canvas, "handleMousePressed", press);

        // Release without dragging
        MouseEvent release = createMouseEvent(canvas, MouseEvent.MOUSE_RELEASED, 52, 52, 1, InputEvent.BUTTON1_DOWN_MASK);
        invokePrivate(canvas, "handleMouseReleased", release);

        // Assert: no node selected
        assertNull(getSelectedNode(canvas));
    }

    @Test
    @DisplayName("Left button drag with movement < threshold should not pan, offsets stay zero")
    void testClickVsDragThreshold() throws Exception {
        // Arrange
        GraphCanvas canvas = new GraphCanvas();
        canvas.setNodes(createNodes(1));
        canvas.setLayoutStrategy(new TestLayoutStrategy(100, 100));
        setOffsets(canvas, 0, 0);

        // Press at (100,100)
        MouseEvent press = createMouseEvent(canvas, MouseEvent.MOUSE_PRESSED, 100, 100, 1, InputEvent.BUTTON1_DOWN_MASK);
        invokePrivate(canvas, "handleMousePressed", press);

        // Drag to (103,103): distance sqrt(18) ≈ 4.24 < 5 threshold
        MouseEvent drag = createMouseEvent(canvas, MouseEvent.MOUSE_DRAGGED, 103, 103, 0, InputEvent.BUTTON1_DOWN_MASK);
        invokePrivate(canvas, "handleMouseDragged", drag);

        // Release
        MouseEvent release = createMouseEvent(canvas, MouseEvent.MOUSE_RELEASED, 103, 103, 1, InputEvent.BUTTON1_DOWN_MASK);
        invokePrivate(canvas, "handleMouseReleased", release);

        // Assert: no panning occurred; offsets still 0
        assertEquals(0, getOffsetX(canvas));
        assertEquals(0, getOffsetY(canvas));
    }

    // ---------- Helper Methods ----------

    private GraphNode createNode(String id, String label) {
        return new GraphNode(id, label, GraphNode.NodeType.DEPENDENCY, RiskLevel.LOW);
    }

    private java.util.List<GraphNode> createNodes(int count) {
        java.util.List<GraphNode> list = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(createNode("n" + i, "Node" + i));
        }
        return list;
    }

    private MouseEvent createMouseEvent(Component component, int id, int x, int y, int button, int modifiers) {
        return new MouseEvent(
            component,
            id,
            System.currentTimeMillis(),
            modifiers,
            x, y,
            1, // click count
            false, // popupTrigger
            button
        );
    }

    private void invokePrivate(Object target, String methodName, MouseEvent e) throws Exception {
        var method = target.getClass().getDeclaredMethod(methodName, MouseEvent.class);
        method.setAccessible(true);
        method.invoke(target, e);
    }

    private double getOffsetX(GraphCanvas canvas) throws Exception {
        return (double) getPrivateField(canvas, "offsetX");
    }

    private double getOffsetY(GraphCanvas canvas) throws Exception {
        return (double) getPrivateField(canvas, "offsetY");
    }

    private void setOffsets(GraphCanvas canvas, double x, double y) throws Exception {
        setPrivateField(canvas, "offsetX", x);
        setPrivateField(canvas, "offsetY", y);
    }

    private GraphNode getSelectedNode(GraphCanvas canvas) throws Exception {
        return (GraphNode) getPrivateField(canvas, "selectedNode");
    }

    private Object getPrivateField(Object target, String fieldName) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
