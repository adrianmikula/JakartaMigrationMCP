package adrianmikula.jakartamigration.intellij.ui;

import java.util.*;

/**
 * Force-directed layout strategy that simulates physical forces.
 * Repulsive forces between nodes, attractive forces along edges.
 * 
 * <h2>Calibration Approach</h2>
 * This implementation uses industry-standard techniques from multiple force-directed graph algorithms
 * to dynamically tune optimal distances for different sized graphs:
 * 
 * <ul>
 *   <li><b>Area-based scaling</b> (Fruchterman-Reingold): optimal distance k = sqrt(canvasArea / nodeCount)
 *       - Directly relates spacing to available canvas space rather than fixed baseline
 *       - More intuitive and predictable than inverse square root scaling
 *   
 *   <li><b>Adaptive temperature cooling</b> (Simulated Annealing/D3.js): 
 *       - Initial temperature = 10% of canvas width, decays by 5% per iteration
 *       - Allows larger movements early for exploration, smaller movements later for refinement
 *       - Prevents premature convergence to local minima
 *   
 *   <li><b>Degree-dependent repulsion</b> (ForceAtlas2/Gephi):
 *       - Repulsion scales with sqrt((degree1 + 1) * (degree2 + 1))
 *       - Highly connected nodes repel more strongly, reducing visual clutter
 *       - Particularly effective for power-law networks with many leaf nodes
 *   
 *   <li><b>Gravity force</b> (ForceAtlas2):
 *       - Attracts all nodes toward canvas center with strength 0.1
 *       - Prevents disconnected components from drifting to edges
 *       - Keeps sparse graphs cohesive
 *   
 *   <li><b>Improved convergence detection</b>:
 *       - Tracks energy change over iterations with minimum 50-iteration threshold
 *       - Stops when relative energy change < 1% or temperature < 0.5
 *       - Prevents both premature stopping and unnecessary iterations
 * </ul>
 * 
 * <h2>Parameter Tuning</h2>
 * Key parameters are configured for balanced behavior across graph sizes:
 * - COOLING_RATE: 0.95 (temperature decay per iteration)
 * - INITIAL_TEMPERATURE_RATIO: 0.1 (initial temp as fraction of canvas width)
 * - GRAVITY_STRENGTH: 0.1 (prevents component drift)
 * - Minimum node sizes: 60px width, 25px height, 40px separation
 * 
 * @see ADR-0065 (Dynamic Graph Spacing and Standard Panning)
 * @see spec/plugin-components.tsp GraphLayout.nodeSpacing
 * @see <a href="https://journals.plos.org/plosone/article?id=10.1371/journal.pone.0098679">ForceAtlas2 Paper</a>
 * @see <a href="https://d3js.org/d3-force">D3.js Force Simulation</a>
 */
public class ForceDirectedLayoutStrategy implements GraphLayoutStrategy {
    // Single scaling parameter that affects all distances
    private static final double ATTRACTION_STRENGTH = 0.01;
    private static final int BASE_MAX_ITERATIONS = 500;
    private static final double COOLING_RATE = 0.95; // Temperature decay per iteration
    private static final double INITIAL_TEMPERATURE_RATIO = 0.1; // Initial temp as fraction of canvas width

    // Minimum sizes to ensure readability
    private static final double MIN_NODE_WIDTH = 60;
    private static final double MIN_NODE_HEIGHT = 25;
    private static final double MIN_MIN_SEPARATION = 40;
    private static final double GRAVITY_STRENGTH = 0.1; // Prevents disconnected components from drifting

    @Override
    public void layout(List<GraphNode> nodes, List<GraphEdge> edges, int canvasWidth, int canvasHeight) {
        if (nodes.isEmpty()) return;

        int nodeCount = nodes.size();

        // Calculate optimal distance using area-based formula from Fruchterman-Reingold
        // k = sqrt(canvasArea / nodeCount)
        // This directly relates optimal spacing to available canvas space
        double optimalDistance = calculateOptimalDistance(nodeCount, canvasWidth, canvasHeight);

        // Apply optimal distance to all parameters with minimum thresholds
        double minSeparation = Math.max(optimalDistance * 0.8, MIN_MIN_SEPARATION);
        double nodeWidth = Math.max(optimalDistance * 1.2, MIN_NODE_WIDTH);
        double nodeHeight = Math.max(optimalDistance * 0.4, MIN_NODE_HEIGHT);
        double repulsionStrength = optimalDistance * optimalDistance * 0.5;

        // Adaptive iteration count: more nodes need more iterations, but capping for performance
        int maxIterations = calculateMaxIterations(nodeCount);

        // Initialize positions in a grid with scaled spacing
        initializePositions(nodes, canvasWidth, canvasHeight, nodeWidth, nodeHeight);

        // Calculate node degrees for degree-dependent repulsion
        Map<String, Integer> nodeDegrees = calculateNodeDegrees(nodes, edges);

        // Initialize temperature for adaptive cooling
        double temperature = canvasWidth * INITIAL_TEMPERATURE_RATIO;
        double previousEnergy = Double.MAX_VALUE;
        double convergenceThreshold = 0.01;
        int minIterations = 50; // Minimum iterations before checking convergence

        // Run force simulation with dynamic parameters
        for (int iter = 0; iter < maxIterations; iter++) {
            Map<String, Double> forcesX = new HashMap<>();
            Map<String, Double> forcesY = new HashMap<>();

            // Initialize forces
            for (GraphNode node : nodes) {
                forcesX.put(node.getId(), 0.0);
                forcesY.put(node.getId(), 0.0);
            }

            // Calculate repulsive forces between all nodes with degree-dependent scaling
            for (int i = 0; i < nodes.size(); i++) {
                for (int j = i + 1; j < nodes.size(); j++) {
                    GraphNode n1 = nodes.get(i);
                    GraphNode n2 = nodes.get(j);
                    int degree1 = nodeDegrees.getOrDefault(n1.getId(), 0);
                    int degree2 = nodeDegrees.getOrDefault(n2.getId(), 0);
                    calculateRepulsiveForce(n1, n2, forcesX, forcesY, minSeparation, repulsionStrength, degree1, degree2);
                }
            }

            // Calculate attractive forces along edges
            for (GraphEdge edge : edges) {
                calculateAttractiveForce(edge, forcesX, forcesY);
            }

            // Apply gravity force to prevent disconnected components from drifting
            calculateGravityForce(nodes, forcesX, forcesY, canvasWidth, canvasHeight);

            // Apply forces with temperature and velocity decay
            double maxForce = 0;
            double currentEnergy = 0;
            for (GraphNode node : nodes) {
                double fx = forcesX.get(node.getId());
                double fy = forcesY.get(node.getId());

                // Limit displacement by temperature (simulated annealing)
                double displacement = Math.min(Math.sqrt(fx * fx + fy * fy), temperature);
                double scale = displacement > 0 ? displacement / Math.sqrt(fx * fx + fy * fy) : 0;

                node.setX(node.getX() + fx * scale);
                node.setY(node.getY() + fy * scale);

                maxForce = Math.max(maxForce, Math.abs(fx) + Math.abs(fy));
                currentEnergy += Math.abs(fx) + Math.abs(fy);
            }

            // Cool down temperature
            temperature *= COOLING_RATE;

            // Improved convergence detection: track energy change
            if (iter >= minIterations) {
                double energyChange = Math.abs(currentEnergy - previousEnergy);
                double relativeChange = energyChange / (previousEnergy + 1e-10);
                
                // Stop if converged (energy change is small) or temperature too low
                if (relativeChange < convergenceThreshold || temperature < 0.5) {
                    break;
                }
            }
            
            previousEnergy = currentEnergy;
        }

        // Center the layout
        centerLayout(nodes, canvasWidth, canvasHeight);
    }

    /**
     * Calculate optimal distance using area-based formula from Fruchterman-Reingold.
     * k = sqrt(canvasArea / nodeCount)
     * This directly relates optimal spacing to available canvas space.
     * @param nodeCount Number of nodes in the graph
     * @param canvasWidth Width of the canvas
     * @param canvasHeight Height of the canvas
     * @return Optimal distance between nodes
     */
    private double calculateOptimalDistance(int nodeCount, int canvasWidth, int canvasHeight) {
        double canvasArea = canvasWidth * canvasHeight;
        return Math.sqrt(canvasArea / nodeCount);
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
            double minSeparation, double repulsionStrength, int degree1, int degree2) {
        double dx = n2.getCenterX() - n1.getCenterX();
        double dy = n2.getCenterY() - n1.getCenterY();
        double distSq = dx * dx + dy * dy;

        // Use gentler repulsive force calculation with better falloff
        double minDistSq = minSeparation * minSeparation;
        if (distSq < minDistSq) {
            distSq = minDistSq;
        }

        double dist = Math.sqrt(distSq);

        // Degree-dependent repulsion: higher degree nodes repel more strongly
        // Inspired by ForceAtlas2 to reduce visual clutter from leaf nodes
        double degreeFactor = Math.sqrt((degree1 + 1) * (degree2 + 1));

        // Exponential falloff: force ~ exp(-dist / (3 * separation))
        // This creates a "soft" repulsion that fades smoothly
        double force = repulsionStrength * degreeFactor * Math.exp(-dist / (3 * minSeparation));

        // Additional linear force for very close nodes to prevent overlap
        if (dist < minSeparation * 1.2) {
            force += repulsionStrength * degreeFactor * 0.3 * (minSeparation * 1.2 - dist) / minSeparation;
        }

        double fx = (dx / dist) * force;
        double fy = (dy / dist) * force;

        forcesX.put(n1.getId(), forcesX.get(n1.getId()) - fx);
        forcesY.put(n1.getId(), forcesY.get(n1.getId()) - fy);
        forcesX.put(n2.getId(), forcesX.get(n2.getId()) + fx);
        forcesY.put(n2.getId(), forcesY.get(n2.getId()) + fy);
    }

    /**
     * Calculate the degree (number of edges) for each node.
     * Used for degree-dependent repulsion to reduce visual clutter from leaf nodes.
     */
    private Map<String, Integer> calculateNodeDegrees(List<GraphNode> nodes, List<GraphEdge> edges) {
        Map<String, Integer> degrees = new HashMap<>();
        
        // Initialize all nodes with degree 0
        for (GraphNode node : nodes) {
            degrees.put(node.getId(), 0);
        }
        
        // Count edges for each node
        for (GraphEdge edge : edges) {
            String sourceId = edge.getSource().getId();
            String targetId = edge.getTarget().getId();
            degrees.put(sourceId, degrees.getOrDefault(sourceId, 0) + 1);
            degrees.put(targetId, degrees.getOrDefault(targetId, 0) + 1);
        }
        
        return degrees;
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

    /**
     * Calculate gravity force to prevent disconnected components from drifting.
     * Attracts all nodes toward the center of the canvas.
     * Inspired by ForceAtlas2 gravity implementation.
     */
    private void calculateGravityForce(List<GraphNode> nodes,
            Map<String, Double> forcesX, Map<String, Double> forcesY,
            int canvasWidth, int canvasHeight) {
        double centerX = canvasWidth / 2.0;
        double centerY = canvasHeight / 2.0;

        for (GraphNode node : nodes) {
            double dx = centerX - node.getCenterX();
            double dy = centerY - node.getCenterY();
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist < 1) dist = 1;

            // Gravity force increases with distance from center
            double force = GRAVITY_STRENGTH * dist;

            double fx = (dx / dist) * force;
            double fy = (dy / dist) * force;

            forcesX.put(node.getId(), forcesX.get(node.getId()) + fx);
            forcesY.put(node.getId(), forcesY.get(node.getId()) + fy);
        }
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
