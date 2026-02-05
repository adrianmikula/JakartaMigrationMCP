/*
 * Copyright © 2026 Adrian Mikula
 *
 * All rights reserved.
 *
 * This software is proprietary and may not be used, copied,
 * modified, or distributed except under the terms of a
 * separate commercial license agreement.
 */
package adrianmikula.jakartamigration.intellij.ui;

import java.util.*;

/**
 * Circular layout strategy that positions nodes in a circle.
 */
public class CircularLayoutStrategy implements GraphLayoutStrategy {
    private static final double NODE_WIDTH = 100;
    private static final double NODE_HEIGHT = 30;
    private static final double RADIUS_SCALE = 80;

    @Override
    public void layout(List<GraphNode> nodes, List<GraphEdge> edges, int canvasWidth, int canvasHeight) {
        if (nodes.isEmpty()) return;

        int centerX = canvasWidth / 2;
        int centerY = canvasHeight / 2;
        double radius = Math.min(canvasWidth, canvasHeight) / 2 - 100;

        // Calculate radius based on number of nodes
        radius = Math.max(radius, nodes.size() * RADIUS_SCALE / (2 * Math.PI));

        // Position root node in center if it exists
        GraphNode rootNode = null;
        List<GraphNode> otherNodes = new ArrayList<>();

        for (GraphNode node : nodes) {
            if (node.getType() == GraphNode.NodeType.ROOT) {
                rootNode = node;
            } else {
                otherNodes.add(node);
            }
        }

        if (rootNode != null) {
            rootNode.setX(centerX - NODE_WIDTH / 2);
            rootNode.setY(centerY - NODE_HEIGHT / 2);
            rootNode.setWidth(NODE_WIDTH);
            rootNode.setHeight(NODE_HEIGHT);
        }

        // Position other nodes in a circle
        double angleStep = 2 * Math.PI / otherNodes.size();
        double startAngle = -Math.PI / 2; // Start from top

        for (int i = 0; i < otherNodes.size(); i++) {
            GraphNode node = otherNodes.get(i);
            double angle = startAngle + i * angleStep;

            int x = (int) (centerX + radius * Math.cos(angle) - NODE_WIDTH / 2);
            int y = (int) (centerY + radius * Math.sin(angle) - NODE_HEIGHT / 2);

            node.setX(x);
            node.setY(y);
            node.setWidth(NODE_WIDTH);
            node.setHeight(NODE_HEIGHT);
        }
    }
}
