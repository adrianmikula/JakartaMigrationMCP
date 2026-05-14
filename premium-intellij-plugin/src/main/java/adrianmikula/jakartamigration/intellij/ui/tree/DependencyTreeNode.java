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
}
