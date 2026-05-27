package adrianmikula.jakartamigration.intellij.ui;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Helper class for accessing private GraphCanvas state in tests.
 */
public class GraphCanvasTestHelper {

    /**
     * Get the list of nodes from a GraphCanvas instance using reflection.
     */
    @SuppressWarnings("unchecked")
    public static List<GraphNode> getNodes(GraphCanvas canvas) throws Exception {
        Field nodesField = GraphCanvas.class.getDeclaredField("nodes");
        nodesField.setAccessible(true);
        return (List<GraphNode>) nodesField.get(canvas);
    }

    /**
     * Get the list of edges from a GraphCanvas instance using reflection.
     */
    @SuppressWarnings("unchecked")
    public static List<GraphEdge> getEdges(GraphCanvas canvas) throws Exception {
        Field edgesField = GraphCanvas.class.getDeclaredField("edges");
        edgesField.setAccessible(true);
        return (List<GraphEdge>) edgesField.get(canvas);
    }

    /**
     * Get the layout strategy from a GraphCanvas instance using reflection.
     */
    public static GraphLayoutStrategy getLayoutStrategy(GraphCanvas canvas) throws Exception {
        Field layoutField = GraphCanvas.class.getDeclaredField("layoutStrategy");
        layoutField.setAccessible(true);
        return (GraphLayoutStrategy) layoutField.get(canvas);
    }

    /**
     * Get the scale factor from a GraphCanvas instance using reflection.
     */
    public static double getScale(GraphCanvas canvas) throws Exception {
        Field scaleField = GraphCanvas.class.getDeclaredField("scale");
        scaleField.setAccessible(true);
        return (double) scaleField.get(canvas);
    }

    /**
     * Get the offset X from a GraphCanvas instance using reflection.
     */
    public static double getOffsetX(GraphCanvas canvas) throws Exception {
        Field offsetXField = GraphCanvas.class.getDeclaredField("offsetX");
        offsetXField.setAccessible(true);
        return (double) offsetXField.get(canvas);
    }

    /**
     * Get the offset Y from a GraphCanvas instance using reflection.
     */
    public static double getOffsetY(GraphCanvas canvas) throws Exception {
        Field offsetYField = GraphCanvas.class.getDeclaredField("offsetY");
        offsetYField.setAccessible(true);
        return (double) offsetYField.get(canvas);
    }
}
