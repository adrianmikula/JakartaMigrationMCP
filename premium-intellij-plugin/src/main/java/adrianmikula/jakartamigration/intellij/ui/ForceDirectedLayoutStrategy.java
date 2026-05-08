package adrianmikula.jakartamigration.intellij.ui;

import java.util.*;

/**
 * Force-directed layout strategy that simulates physical forces.
 * Repulsive forces between nodes, attractive forces along edges.
 * Node spacing automatically adjusts based on total node count to maintain
 * consistent visual density for graphs of any size.
 *
 * @see ADR-0065 (Dynamic Graph Spacing and Standard Panning)
 * @see spec/plugin-components.tsp GraphLayout.nodeSpacing
 */
public class ForceDirectedLayoutStrategy implements GraphLayoutStrategy {
    // Base constants optimized for ~50 nodes (medium graph)
    private static final double BASE_REPULSION_STRENGTH = 2500;
    private static final double ATTRACTION_STRENGTH = 0.01;
    private static final double DAMPING = 0.85;
    private static final int BASE_MAX_ITERATIONS = 500;
    private static final double BASE_NODE_WIDTH = 120;
    private static final double BASE_NODE_HEIGHT = 40;
    private static final double BASE_MIN_SEPARATION = 80;

    // Minimum sizes to ensure readability
    private static final double MIN_NODE_WIDTH = 60;
    private static final double MIN_NODE_HEIGHT = 25;
    private static final double MIN_MIN_SEPARATION = 40;

    @Override
    public void layout(List<GraphNode> nodes, List<GraphEdge> edges, int canvasWidth, int canvasHeight) {
        if (nodes.isEmpty()) return;

        int nodeCount = nodes.size();

        // Calculate dynamic scaling factor based on node count
        // Implements spec: GraphLayout.nodeSpacing auto-adjusts for consistent visual density
        // See spec/plugin-components.tsp - GraphLayout model with configurable nodeSpacing
        // This implementation auto-calculates optimal spacing based on graph size
        double scaleFactor = calculateScaleFactor(nodeCount);

        // Apply scaling to all parameters
        double minSeparation = Math.max(BASE_MIN_SEPARATION * scaleFactor, MIN_MIN_SEPARATION);
        double nodeWidth = Math.max(BASE_NODE_WIDTH * scaleFactor, MIN_NODE_WIDTH);
        double nodeHeight = Math.max(BASE_NODE_HEIGHT * scaleFactor, MIN_NODE_HEIGHT);
        double repulsionStrength = BASE_REPULSION_STRENGTH * scaleFactor;

        // Adaptive iteration count: more nodes need more iterations, but capping for performance
        int maxIterations = calculateMaxIterations(nodeCount);

        // Initialize positions in a grid with scaled spacing
        initializePositions(nodes, canvasWidth, canvasHeight, nodeWidth, nodeHeight);

        // Run force simulation with dynamic parameters
        for (int iter = 0; iter < maxIterations; iter++) {
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
                    calculateRepulsiveForce(n1, n2, forcesX, forcesY, minSeparation, repulsionStrength);
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

    /**
     * Calculate scale factor using inverse square root: sqrt(BASE / count).
     * This ensures consistent visual density across different graph sizes.
     * Examples: 10 nodes → 2.24x, 50 nodes → 1.0x, 200 nodes → 0.5x, 1000 nodes → 0.22x
     */
    private double calculateScaleFactor(int nodeCount) {
        final int OPTIMAL_NODE_COUNT = 50; // Base case: optimized for this size
        return Math.sqrt(Math.max(1.0, (double) OPTIMAL_NODE_COUNT / nodeCount));
    }

    /**
     * Adaptive iteration count: larger graphs need more iterations to converge,
     * but we cap it to avoid excessive CPU usage on very large graphs.
     */
    private int calculateMaxIterations(int nodeCount) {
        // Base 500 + additional for large graphs, logarithmically scaled
        int extraIterations = (int) Math.ceil(Math.log10(Math.max(2, nodeCount / 50.0)) * 100);
        return Math.min(BASE_MAX_ITERATIONS + extraIterations, 800);
    }

    private void initializePositions(List<GraphNode> nodes, int canvasWidth, int canvasHeight,
                                      double nodeWidth, double nodeHeight) {
        int cols = (int) Math.ceil(Math.sqrt(nodes.size() * 1.5));
        int startX = canvasWidth / (cols + 1);
        int startY = canvasHeight / ((nodes.size() / cols) + 1);

        // Grid spacing should match expected separation to avoid initial explosion
        double gridSpacingX = nodeWidth * 1.5;
        double gridSpacingY = nodeHeight * 1.5;

        for (int i = 0; i < nodes.size(); i++) {
            GraphNode node = nodes.get(i);
            int col = i % cols;
            int row = i / cols;
            node.setX(startX + col * gridSpacingX);
            node.setY(startY + row * gridSpacingY);
            node.setWidth(nodeWidth);
            node.setHeight(nodeHeight);
        }
    }

    private void calculateRepulsiveForce(GraphNode n1, GraphNode n2,
            Map<String, Double> forcesX, Map<String, Double> forcesY,
            double minSeparation, double repulsionStrength) {
        double dx = n2.getCenterX() - n1.getCenterX();
        double dy = n2.getCenterY() - n1.getCenterY();
        double distSq = dx * dx + dy * dy;

        // Use gentler repulsive force calculation with better falloff
        double minDistSq = minSeparation * minSeparation;
        if (distSq < minDistSq) {
            distSq = minDistSq;
        }

        double dist = Math.sqrt(distSq);

        // Exponential falloff: force ~ exp(-dist / (3 * separation))
        // This creates a "soft" repulsion that fades smoothly
        double force = repulsionStrength * Math.exp(-dist / (3 * minSeparation));

        // Additional linear force for very close nodes to prevent overlap
        if (dist < minSeparation * 1.2) {
            force += repulsionStrength * 0.3 * (minSeparation * 1.2 - dist) / minSeparation;
        }

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
