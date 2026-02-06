package adrianmikula.jakartamigration.intellij.ui;

import java.util.List;

/**
 * Interface for graph layout algorithms.
 */
public interface GraphLayoutStrategy {
    /**
     * Layout the nodes and edges in the graph.
     *
     * @param nodes the list of nodes to layout
     * @param edges the list of edges (for reference)
     * @param canvasWidth the available width for the layout
     * @param canvasHeight the available height for the layout
     */
    void layout(List<GraphNode> nodes, List<GraphEdge> edges, int canvasWidth, int canvasHeight);
}
