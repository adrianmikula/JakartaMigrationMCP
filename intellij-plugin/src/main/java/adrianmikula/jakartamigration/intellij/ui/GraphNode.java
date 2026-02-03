package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.model.RiskLevel;

/**
 * Represents a node in the dependency graph.
 * Each node represents a module or dependency in the project.
 */
public class GraphNode {
    private final String id;
    private final String label;
    private final NodeType type;
    private final RiskLevel riskLevel;
    private double x;
    private double y;
    private double width = 120;
    private double height = 40;

    public enum NodeType {
        MODULE,
        DEPENDENCY,
        ROOT
    }

    public GraphNode(String id, String label, NodeType type, RiskLevel riskLevel) {
        this.id = id;
        this.label = label;
        this.type = type;
        this.riskLevel = riskLevel;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public NodeType getType() {
        return type;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public double getCenterX() {
        return x + width / 2;
    }

    public double getCenterY() {
        return y + height / 2;
    }

    public boolean contains(double px, double py) {
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }
}
