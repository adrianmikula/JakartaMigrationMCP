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
 * Force-directed layout strategy that simulates physical forces.
 * Repulsive forces between nodes, attractive forces along edges.
 */
public class ForceDirectedLayoutStrategy implements GraphLayoutStrategy {
    private static final double REPULSION_STRENGTH = 1000;
    private static final double ATTRACTION_STRENGTH = 0.01;
    private static final double DAMPING = 0.85;
    private static final int MAX_ITERATIONS = 200;
    private static final double NODE_WIDTH = 120;
    private static final double NODE_HEIGHT = 40;

    @Override
    public void layout(List<GraphNode> nodes, List<GraphEdge> edges, int canvasWidth, int canvasHeight) {
        if (nodes.isEmpty()) return;

        // Initialize positions in a grid
        initializePositions(nodes, canvasWidth, canvasHeight);

        // Run force simulation
        for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
            Map<String, Double> forcesX = new HashMap<>();
            Map<String, Double> forcesY = new HashMap<>();

            // Initialize forces
            for (GraphNode node : nodes) {
                forcesX.put(node.getId(), 0.0);
                forcesY.put(node.getId(), 0.0);
            }

            // Calculate repulsive forces between all nodes
            for (int i = 0; i < nodes.size(); i++) {
                for (int j = i + 1; j < nodes.size(); j++) {
                    GraphNode n1 = nodes.get(i);
                    GraphNode n2 = nodes.get(j);
                    calculateRepulsiveForce(n1, n2, forcesX, forcesY);
                }
            }

            // Calculate attractive forces along edges
            for (GraphEdge edge : edges) {
                calculateAttractiveForce(edge, forcesX, forcesY);
            }

            // Apply forces with damping
            double maxForce = 0;
            for (GraphNode node : nodes) {
                double fx = forcesX.get(node.getId());
                double fy = forcesY.get(node.getId());

                node.setX(node.getX() + fx * DAMPING);
                node.setY(node.getY() + fy * DAMPING);

                maxForce = Math.max(maxForce, Math.abs(fx) + Math.abs(fy));
            }

            // Stop early if converged
            if (maxForce < 0.5) break;
        }

        // Center the layout
        centerLayout(nodes, canvasWidth, canvasHeight);
    }

    private void initializePositions(List<GraphNode> nodes, int canvasWidth, int canvasHeight) {
        int cols = (int) Math.ceil(Math.sqrt(nodes.size() * 1.5));
        int startX = canvasWidth / (cols + 1);
        int startY = canvasHeight / ((nodes.size() / cols) + 1);

        for (int i = 0; i < nodes.size(); i++) {
            GraphNode node = nodes.get(i);
            int col = i % cols;
            int row = i / cols;
            node.setX(startX + col * 200);
            node.setY(startY + row * 100);
            node.setWidth(NODE_WIDTH);
            node.setHeight(NODE_HEIGHT);
        }
    }

    private void calculateRepulsiveForce(GraphNode n1, GraphNode n2,
            Map<String, Double> forcesX, Map<String, Double> forcesY) {
        double dx = n2.getCenterX() - n1.getCenterX();
        double dy = n2.getCenterY() - n1.getCenterY();
        double distSq = dx * dx + dy * dy;
        if (distSq < 1) distSq = 1;

        double dist = Math.sqrt(distSq);
        double force = REPULSION_STRENGTH / distSq;

        double fx = (dx / dist) * force;
        double fy = (dy / dist) * force;

        forcesX.put(n1.getId(), forcesX.get(n1.getId()) - fx);
        forcesY.put(n1.getId(), forcesY.get(n1.getId()) - fy);
        forcesX.put(n2.getId(), forcesX.get(n2.getId()) + fx);
        forcesY.put(n2.getId(), forcesY.get(n2.getId()) + fy);
    }

    private void calculateAttractiveForce(GraphEdge edge,
            Map<String, Double> forcesX, Map<String, Double> forcesY) {
        GraphNode source = edge.getSource();
        GraphNode target = edge.getTarget();

        double dx = target.getCenterX() - source.getCenterX();
        double dy = target.getCenterY() - source.getCenterY();
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < 1) dist = 1;

        double force = dist * ATTRACTION_STRENGTH;

        double fx = (dx / dist) * force;
        double fy = (dy / dist) * force;

        forcesX.put(source.getId(), forcesX.get(source.getId()) + fx);
        forcesY.put(source.getId(), forcesY.get(source.getId()) + fy);
        forcesX.put(target.getId(), forcesX.get(target.getId()) - fx);
        forcesY.put(target.getId(), forcesY.get(target.getId()) - fy);
    }

    private void centerLayout(List<GraphNode> nodes, int canvasWidth, int canvasHeight) {
        // Calculate center of mass
        double avgX = 0, avgY = 0;
        for (GraphNode node : nodes) {
            avgX += node.getCenterX();
            avgY += node.getCenterY();
        }
        avgX /= nodes.size();
        avgY /= nodes.size();

        // Shift to center of canvas
        double shiftX = (canvasWidth / 2) - avgX;
        double shiftY = (canvasHeight / 2) - avgY;

        for (GraphNode node : nodes) {
            node.setX(node.getX() + shiftX);
            node.setY(node.getY() + shiftY);
        }
    }
}
