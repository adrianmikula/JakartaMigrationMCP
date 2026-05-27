package adrianmikula.jakartamigration.intellij.ui.tree;

import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Tree node for dependency hierarchy.
 * Wraps DependencyInfo in a DefaultMutableTreeNode for use with Swing JTree.
 */
public class DependencyTreeNode extends DefaultMutableTreeNode {

    private final DependencyInfo dependency;

    public DependencyTreeNode(DependencyInfo dependency) {
        super(dependency);
        this.dependency = dependency;
    }

    /**
     * Get the dependency info associated with this node.
     */
    public DependencyInfo getDependency() {
        return dependency;
    }

    /**
     * Get the display text for this node.
     */
    public String getDisplayText() {
        return dependency.getDisplayName();
    }

    /**
     * Check if this node has children.
     */
    public boolean hasChildDependencies() {
        return dependency.getChildren() != null && !dependency.getChildren().isEmpty();
    }

    /**
     * Build child nodes from the dependency's children list.
     */
    public void buildChildNodes() {
        if (dependency.getChildren() != null) {
            for (DependencyInfo childDep : dependency.getChildren()) {
                DependencyTreeNode childNode = new DependencyTreeNode(childDep);
                childNode.buildChildNodes();
                add(childNode);
            }
        }
    }

    @Override
    public String toString() {
        return getDisplayText();
    }

    /**
     * Recursively filter this node and its children.
     * If this node is transitive, returns null (indicating it should be removed).
     * If this node is not transitive, filters its children recursively.
     *
     * @return The filtered node, or null if this node should be removed
     */
    public DependencyTreeNode filterTransitive() {
        if (dependency.isTransitive()) {
            // Remove this node and all its children
            return null;
        }

        // This node is not transitive, keep it and filter its children
        DependencyTreeNode filteredNode = new DependencyTreeNode(dependency);

        if (dependency.getChildren() != null) {
            for (DependencyInfo childDep : dependency.getChildren()) {
                DependencyTreeNode childNode = new DependencyTreeNode(childDep);
                DependencyTreeNode filteredChild = childNode.filterTransitive();
                if (filteredChild != null) {
                    filteredNode.add(filteredChild);
                }
            }
        }

        return filteredNode;
    }
}
