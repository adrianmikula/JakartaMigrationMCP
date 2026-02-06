package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Canvas component for rendering the dependency graph.
 * Supports pan, zoom, and node selection.
 */
public class GraphCanvas extends JPanel {
    private final List<GraphNode> nodes = new ArrayList<>();
    private final List<GraphEdge> edges = new ArrayList<>();
    private GraphLayoutStrategy layoutStrategy = new HierarchicalLayoutStrategy();
    private double scale = 1.0;
    private double offsetX = 0;
    private double offsetY = 0;
    private GraphNode selectedNode = null;
    private boolean showCriticalPath = true;
    private boolean showLabels = true;

    // Mouse interaction state
    private Point lastMousePos = null;
    private boolean isDragging = false;

    public GraphCanvas() {
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createLineBorder(Color.GRAY));

        // Mouse listeners for pan and zoom
        addMouseWheelListener(e -> handleMouseWheel(e));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleMousePressed(e);
            }
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    resetView();
                }
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                handleMouseDragged(e);
            }
        });
    }

    public void setNodes(List<GraphNode> nodes) {
        this.nodes.clear();
        this.nodes.addAll(nodes);
        applyLayout();
        repaint();
    }

    public void setEdges(List<GraphEdge> edges) {
        this.edges.clear();
        this.edges.addAll(edges);
        repaint();
    }

    public void setLayoutStrategy(GraphLayoutStrategy strategy) {
        this.layoutStrategy = strategy;
        applyLayout();
        repaint();
    }

    public void setShowCriticalPath(boolean show) {
        this.showCriticalPath = show;
        repaint();
    }

    public void setShowLabels(boolean show) {
        this.showLabels = show;
        repaint();
    }

    public void zoomIn() {
        scale = Math.min(scale * 1.2, 5.0);
        repaint();
    }

    public void zoomOut() {
        scale = Math.max(scale / 1.2, 0.2);
        repaint();
    }

    public void resetView() {
        scale = 1.0;
        offsetX = 0;
        offsetY = 0;
        repaint();
    }

    public GraphNode getSelectedNode() {
        return selectedNode;
    }

    public void clearSelection() {
        selectedNode = null;
        repaint();
    }

    private void applyLayout() {
        if (nodes.isEmpty()) return;
        layoutStrategy.layout(nodes, edges, getWidth(), getHeight());
    }

    private void handleMouseWheel(MouseWheelEvent e) {
        if (e.isControlDown()) {
            // Zoom with control + scroll
            if (e.getWheelRotation() < 0) {
                zoomIn();
            } else {
                zoomOut();
            }
        } else {
            // Pan with scroll
            offsetY += e.getWheelRotation() * 20;
            repaint();
        }
    }

    private void handleMousePressed(MouseEvent e) {
        lastMousePos = e.getPoint();
        // Check if clicking on a node
        Point worldPos = screenToWorld(e.getX(), e.getY());
        selectedNode = null;
        for (GraphNode node : nodes) {
            if (node.contains(worldPos.x, worldPos.y)) {
                selectedNode = node;
                break;
            }
        }
        repaint();
    }

    private void handleMouseDragged(MouseEvent e) {
        if (lastMousePos != null && !SwingUtilities.isLeftMouseButton(e)) {
            // Pan
            offsetX += e.getX() - lastMousePos.x;
            offsetY += e.getY() - lastMousePos.y;
            repaint();
        }
        lastMousePos = e.getPoint();
    }

    private Point screenToWorld(int screenX, int screenY) {
        int worldX = (int) ((screenX - offsetX) / scale);
        int worldY = (int) ((screenY - offsetY) / scale);
        return new Point(worldX, worldY);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        // Apply transformation
        g2d.translate(offsetX, offsetY);
        g2d.scale(scale, scale);

        // Draw edges
        drawEdges(g2d);

        // Draw nodes
        drawNodes(g2d);

        g2d.dispose();
    }

    private void drawEdges(Graphics2D g2d) {
        g2d.setStroke(new BasicStroke(showCriticalPath ? 2.0f : 1.0f));

        for (GraphEdge edge : edges) {
            if (!showCriticalPath && edge.isCriticalPath()) continue;

            Color edgeColor = edge.isCriticalPath() ? new Color(220, 53, 69) : Color.GRAY;
            if (edge.getType() == GraphEdge.EdgeType.TRANSITIVE) {
                edgeColor = new Color(108, 117, 125);
            } else if (edge.getType() == GraphEdge.EdgeType.RUNTIME_DEPENDENCY) {
                edgeColor = new Color(40, 167, 69);
            }

            g2d.setColor(edgeColor);
            drawArrowLine(g2d,
                (int) edge.getSource().getCenterX(),
                (int) edge.getSource().getCenterY(),
                (int) edge.getTarget().getCenterX(),
                (int) edge.getTarget().getCenterY(),
                10, 5
            );
        }
    }

    private void drawArrowLine(Graphics2D g2d, int x1, int y1, int x2, int y2, int arrowLength, int arrowWidth) {
        double angle = Math.atan2(y2 - y1, x2 - x1);
        int dx = x2 - x1;
        int dy = y2 - y1;
        double length = Math.sqrt(dx * dx + dy * dy);

        // Shorten the line to not overlap with nodes
        double shortenAmount = 25;
        double shortenRatio = Math.max(0, (length - shortenAmount) / length);

        int endX = (int) (x1 + dx * shortenRatio);
        int endY = (int) (y1 + dy * shortenRatio);
        int startX = (int) (x1 + dx * (1 - shortenRatio));
        int startY = (int) (y1 + dy * (1 - shortenRatio));

        g2d.drawLine(startX, startY, endX, endY);

        // Draw arrow head
        int arrowX1 = (int) (endX - arrowLength * Math.cos(angle - Math.PI / 6));
        int arrowY1 = (int) (endY - arrowLength * Math.sin(angle - Math.PI / 6));
        int arrowX2 = (int) (endX - arrowLength * Math.cos(angle + Math.PI / 6));
        int arrowY2 = (int) (endY - arrowLength * Math.sin(angle + Math.PI / 6));

        int[] xPoints = {endX, arrowX1, arrowX2};
        int[] yPoints = {endY, arrowY1, arrowY2};
        g2d.fillPolygon(xPoints, yPoints, 3);
    }

    private void drawNodes(Graphics2D g2d) {
        for (GraphNode node : nodes) {
            Color bgColor;

            // Check if this is an org internal dependency - use purple for org
            if (node.isOrgInternal()) {
                bgColor = new Color(156, 39, 176); // Bright purple for org internal
            } else {
                // Use migration status colors (same as dependencies table)
                DependencyMigrationStatus status = node.getMigrationStatus();
                if (status == null) {
                    // Root node or unknown status - use gray
                    bgColor = new Color(108, 117, 125);
                } else {
                    switch (status) {
                        case COMPATIBLE:
                            bgColor = new Color(40, 167, 69);  // Green
                            break;
                        case NEEDS_UPGRADE:
                        case REQUIRES_MANUAL_MIGRATION:
                            bgColor = new Color(255, 193, 7);  // Yellow
                            break;
                        case NO_JAKARTA_VERSION:
                            bgColor = new Color(220, 53, 69);  // Red
                            break;
                        case MIGRATED:
                            bgColor = new Color(23, 162, 184);  // Cyan
                            break;
                        default:
                            bgColor = new Color(108, 117, 125);  // Gray for unknown
                    }
                }
            }

            // Set stroke - dashed for transitive dependencies
            if (node.equals(selectedNode)) {
                g2d.setColor(Color.BLUE);
                g2d.setStroke(new BasicStroke(3));
            } else if (node.isTransitive()) {
                g2d.setColor(bgColor);
                g2d.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{8, 4}, 0));
            } else {
                g2d.setColor(bgColor);
                g2d.setStroke(new BasicStroke(1));
            }

            int x = (int) node.getX();
            int y = (int) node.getY();
            int w = (int) node.getWidth();
            int h = (int) node.getHeight();

            // Draw rounded rectangle
            g2d.drawRoundRect(x, y, w, h, 8, 8);

            // Fill with lighter color
            g2d.setColor(new Color(
                Math.min(255, bgColor.getRed() + 50),
                Math.min(255, bgColor.getGreen() + 50),
                Math.min(255, bgColor.getBlue() + 50)
            ));
            g2d.fillRoundRect(x + 1, y + 1, w - 2, h - 2, 7, 7);

            // Draw org internal indicator (small dot in corner)
            if (node.isOrgInternal()) {
                g2d.setColor(node.isAnalyzedForMigration() ?
                    new Color(255, 255, 0) : new Color(255, 100, 100));
                g2d.fillOval(x + w - 12, y + 4, 8, 8);
            }

            // Draw transitive indicator (small "T" in corner)
            if (node.isTransitive() && !node.isOrgInternal()) {
                g2d.setColor(new Color(108, 117, 125));
                g2d.setFont(g2d.getFont().deriveFont(Font.BOLD, 9f));
                g2d.drawString("T", x + w - 12, y + h - 4);
            }

            // Draw label
            if (showLabels) {
                g2d.setColor(Color.WHITE);
                g2d.setFont(g2d.getFont().deriveFont(Font.BOLD, 11f));
                FontMetrics fm = g2d.getFontMetrics();
                String label = node.getLabel();
                if (fm.stringWidth(label) > w - 10) {
                    // Truncate label
                    while (fm.stringWidth(label + "...") > w - 10 && label.length() > 0) {
                        label = label.substring(0, label.length() - 1);
                    }
                    label += "...";
                }
                int textX = x + (w - fm.stringWidth(label)) / 2;
                int textY = y + (h + fm.getAscent()) / 2 - 2;
                g2d.drawString(label, textX, textY);
            }
        }
    }
}
