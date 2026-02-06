package adrianmikula.jakartamigration.intellij.ui;

import java.util.*;

/**
 * Tree layout strategy that positions nodes in a tree structure.
 */
public class TreeLayoutStrategy implements GraphLayoutStrategy {
    private static final double NODE_WIDTH = 120;
    private static final double NODE_HEIGHT = 40;
    private static final double HORIZONTAL_SPACING = 30;
    private static final double VERTICAL_SPACING = 60;

    @Override
    public void layout(List<GraphNode> nodes, List<GraphEdge> edges, int canvasWidth, int canvasHeight) {
        if (nodes.isEmpty()) return;

        // Build adjacency list
        Map<String, List<String>> children = new HashMap<>();
        String rootId = null;

        for (GraphNode node : nodes) {
            children.put(node.getId(), new ArrayList<>());
        }

        for (GraphEdge edge : edges) {
            children.computeIfAbsent(edge.getSource().getId(), k -> new ArrayList<>())
                    .add(edge.getTarget().getId());
        }

        // Find root node
        Set<String> allIds = new HashSet<>();
        for (GraphNode node : nodes) {
            allIds.add(node.getId());
        }
        for (GraphEdge edge : edges) {
            allIds.remove(edge.getTarget().getId());
        }
        if (!allIds.isEmpty()) {
            rootId = allIds.iterator().next();
        }

        if (rootId == null && !nodes.isEmpty()) {
            rootId = nodes.get(0).getId();
        }

        // Create node lookup
        Map<String, GraphNode> nodeMap = new HashMap<>();
        for (GraphNode node : nodes) {
            nodeMap.put(node.getId(), node);
        }

        // Layout tree recursively
        double startY = VERTICAL_SPACING;
        layoutNode(rootId, children, nodeMap, canvasWidth / 2, startY, canvasWidth / 4);
    }

    private double layoutNode(String nodeId, Map<String, List<String>> children,
            Map<String, GraphNode> nodeMap, double x, double y, double subtreeWidth) {
        if (nodeId == null || !nodeMap.containsKey(nodeId)) {
            return 0;
        }

        GraphNode node = nodeMap.get(nodeId);
        node.setX(x - NODE_WIDTH / 2);
        node.setY(y);
        node.setWidth(NODE_WIDTH);
        node.setHeight(NODE_HEIGHT);

        List<String> childIds = children.getOrDefault(nodeId, Collections.emptyList());
        if (childIds.isEmpty()) {
            return NODE_WIDTH;
        }

        // Layout children
        double totalWidth = 0;
        double startX = x - subtreeWidth / 2;

        for (String childId : childIds) {
            double childWidth = layoutNode(childId, children, nodeMap, startX, y + VERTICAL_SPACING, subtreeWidth / childIds.size());
            totalWidth += childWidth + HORIZONTAL_SPACING;
            startX += childWidth + HORIZONTAL_SPACING;
        }

        // Adjust parent position to center children
        if (totalWidth > 0) {
            double childrenCenter = startX - totalWidth + childIds.size() * (HORIZONTAL_SPACING / 2);
            node.setX(childrenCenter - NODE_WIDTH / 2);
        }

        return Math.max(totalWidth, NODE_WIDTH);
    }
}
