package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus;
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
    private boolean isOrgInternal = false;
    private boolean isTransitive = false;
    private boolean analyzedForMigration = false;
    private DependencyMigrationStatus migrationStatus = null;
    private double x;
    private double y;
    private double width = 120;
    private double height = 40;

    public enum NodeType {
        MODULE,
        DEPENDENCY,
        ROOT,
        ORG_INTERNAL
    }

    public GraphNode(String id, String label, NodeType type, RiskLevel riskLevel) {
        this.id = id;
        this.label = label;
        this.type = type;
        this.riskLevel = riskLevel;
    }

    public GraphNode(String id, String label, NodeType type, RiskLevel riskLevel, boolean isOrgInternal, boolean isTransitive) {
        this.id = id;
        this.label = label;
        this.type = type;
        this.riskLevel = riskLevel;
        this.isOrgInternal = isOrgInternal;
        this.isTransitive = isTransitive;
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

    public boolean isOrgInternal() {
        return isOrgInternal;
    }

    public void setOrgInternal(boolean orgInternal) {
        isOrgInternal = orgInternal;
    }

    public boolean isTransitive() {
        return isTransitive;
    }

    public void setTransitive(boolean transitive) {
        isTransitive = transitive;
    }

    public boolean isAnalyzedForMigration() {
        return analyzedForMigration;
    }

    public void setAnalyzedForMigration(boolean analyzedForMigration) {
        this.analyzedForMigration = analyzedForMigration;
    }

    public DependencyMigrationStatus getMigrationStatus() {
        return migrationStatus;
    }

    public void setMigrationStatus(DependencyMigrationStatus migrationStatus) {
        this.migrationStatus = migrationStatus;
    }
}
