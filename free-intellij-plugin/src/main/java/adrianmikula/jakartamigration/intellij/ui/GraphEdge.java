package adrianmikula.jakartamigration.intellij.ui;

/**
 * Represents an edge (connection) between two nodes in the dependency graph.
 */
public class GraphEdge {
    private final GraphNode source;
    private final GraphNode target;
    private final EdgeType type;
    private final boolean isCriticalPath;

    public enum EdgeType {
        DEPENDENCY,
        TRANSITIVE,
        COMPILES_WITH,
        RUNTIME_DEPENDENCY
    }

    public GraphEdge(GraphNode source, GraphNode target, EdgeType type, boolean isCriticalPath) {
        this.source = source;
        this.target = target;
        this.type = type;
        this.isCriticalPath = isCriticalPath;
    }

    public GraphNode getSource() {
        return source;
    }

    public GraphNode getTarget() {
        return target;
    }

    public EdgeType getType() {
        return type;
    }

    public boolean isCriticalPath() {
        return isCriticalPath;
    }

    public GraphNode getOtherNode(GraphNode node) {
        if (source.equals(node)) {
            return target;
        } else if (target.equals(node)) {
            return source;
        }
        return null;
    }
}
