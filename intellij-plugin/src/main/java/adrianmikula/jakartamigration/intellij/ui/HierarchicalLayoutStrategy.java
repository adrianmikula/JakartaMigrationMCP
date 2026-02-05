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
 * Hierarchical layout strategy that positions nodes in layers.
 * Nodes with no incoming edges are placed at the top.
 */
public class HierarchicalLayoutStrategy implements GraphLayoutStrategy {
    private static final int NODE_WIDTH = 120;
    private static final int NODE_HEIGHT = 40;
    private static final int HORIZONTAL_SPACING = 50;
    private static final int VERTICAL_SPACING = 60;

    @Override
    public void layout(List<GraphNode> nodes, List<GraphEdge> edges, int canvasWidth, int canvasHeight) {
        if (nodes.isEmpty()) return;

        // Build adjacency list for dependencies
        Map<String, List<GraphNode>> dependents = new HashMap<>();
        Map<String, Integer> incomingCount = new HashMap<>();

        for (GraphNode node : nodes) {
            dependents.put(node.getId(), new ArrayList<>());
            incomingCount.put(node.getId(), 0);
        }

        for (GraphEdge edge : edges) {
            String targetId = edge.getTarget().getId();
            dependents.computeIfAbsent(edge.getSource().getId(), k -> new ArrayList<>()).add(edge.getTarget());
            incomingCount.merge(targetId, 1, Integer::sum);
        }

        // Assign nodes to layers based on longest path from root
        Map<String, Integer> nodeLayer = new HashMap<>();
        Queue<GraphNode> queue = new LinkedList<>();

        // Find root nodes (no incoming edges)
        for (GraphNode node : nodes) {
            if (incomingCount.get(node.getId()) == 0) {
                nodeLayer.put(node.getId(), 0);
                queue.offer(node);
            }
        }

        // BFS to assign layers
        while (!queue.isEmpty()) {
            GraphNode current = queue.poll();
            int currentLayer = nodeLayer.get(current.getId());

            for (GraphNode dependent : dependents.getOrDefault(current.getId(), Collections.emptyList())) {
                int newLayer = currentLayer + 1;
                int existingLayer = nodeLayer.getOrDefault(dependent.getId(), 0);
                if (newLayer > existingLayer) {
                    nodeLayer.put(dependent.getId(), newLayer);
                }
                if (!queue.contains(dependent)) {
                    queue.offer(dependent);
                }
            }
        }

        // Group nodes by layer
        Map<Integer, List<GraphNode>> layers = new TreeMap<>();
        for (GraphNode node : nodes) {
            int layer = nodeLayer.getOrDefault(node.getId(), 0);
            layers.computeIfAbsent(layer, k -> new ArrayList<>()).add(node);
        }

        // Position nodes
        int startY = VERTICAL_SPACING;
        for (Map.Entry<Integer, List<GraphNode>> entry : layers.entrySet()) {
            int layer = entry.getKey();
            List<GraphNode> layerNodes = entry.getValue();

            // Sort nodes to reduce edge crossings
            layerNodes.sort((a, b) -> {
                int aOut = dependents.getOrDefault(a.getId(), Collections.emptyList()).size();
                int bOut = dependents.getOrDefault(b.getId(), Collections.emptyList()).size();
                return Integer.compare(bOut, aOut);
            });

            int layerWidth = layerNodes.size() * NODE_WIDTH + (layerNodes.size() - 1) * HORIZONTAL_SPACING;
            int startX = Math.max(HORIZONTAL_SPACING, (canvasWidth - layerWidth) / 2);

            for (int i = 0; i < layerNodes.size(); i++) {
                GraphNode node = layerNodes.get(i);
                node.setX(startX + i * (NODE_WIDTH + HORIZONTAL_SPACING));
                node.setY(startY + layer * VERTICAL_SPACING);
                node.setWidth(NODE_WIDTH);
                node.setHeight(NODE_HEIGHT);
            }
        }
    }
}
