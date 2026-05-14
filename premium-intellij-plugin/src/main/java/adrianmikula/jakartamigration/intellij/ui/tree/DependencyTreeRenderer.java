package adrianmikula.jakartamigration.intellij.ui.tree;

import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus;
import adrianmikula.jakartamigration.intellij.ui.DependencyStatusColors;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

/**
 * Custom cell renderer for dependency tree nodes.
 * Provides color-coded icons based on migration status.
 */
public class DependencyTreeRenderer extends DefaultTreeCellRenderer {

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
                                                  boolean expanded, boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

        if (value instanceof DependencyTreeNode) {
            DependencyTreeNode node = (DependencyTreeNode) value;
            DependencyInfo dep = node.getDependency();

            // Set text
            setText(node.getDisplayText());

            // Only set custom icon for leaf nodes (nodes without children)
            // Parent nodes use default tree icons to preserve expand/collapse handles
            if (!node.hasChildDependencies()) {
                setIcon(getStatusIcon(dep.getMigrationStatus()));
            }

            // Set tooltip
            setToolTipText(buildTooltip(dep));

            // Set text color based on status
            setForeground(getStatusColor(dep.getMigrationStatus()));

            // Highlight organizational artifacts
            if (dep.isOrganizational()) {
                setBackground(new Color(230, 240, 255));
                setOpaque(true);
            }
        }

        return this;
    }

    /**
     * Get icon for a given migration status.
     */
    private Icon getStatusIcon(DependencyMigrationStatus status) {
        // For now, use default icons. In a full implementation,
        // you would load custom icons for each status.
        if (status == DependencyMigrationStatus.COMPATIBLE) {
            return UIManager.getIcon("Tree.leafIcon");
        } else if (status == DependencyMigrationStatus.NEEDS_UPGRADE || 
                   status == DependencyMigrationStatus.REQUIRES_MANUAL_MIGRATION) {
            return UIManager.getIcon("Tree.openIcon");
        } else if (status == DependencyMigrationStatus.NO_JAKARTA_VERSION) {
            return UIManager.getIcon("Tree.closedIcon");
        }
        return UIManager.getIcon("Tree.leafIcon");
    }

    /**
     * Get color for a given migration status - uses shared DependencyStatusColors utility.
     */
    private Color getStatusColor(DependencyMigrationStatus status) {
        return DependencyStatusColors.getStatusColor(status);
    }

    /**
     * Build tooltip text for a dependency.
     */
    private String buildTooltip(DependencyInfo dep) {
        StringBuilder tooltip = new StringBuilder();
        tooltip.append("<html>");
        tooltip.append("<b>").append(dep.getDisplayName()).append("</b><br>");
        tooltip.append("Version: ").append(dep.getCurrentVersion()).append("<br>");
        tooltip.append("Scope: ").append(dep.getScope()).append("<br>");
        
        if (dep.getMigrationStatus() != null) {
            tooltip.append("Status: ").append(dep.getMigrationStatus().getValue()).append("<br>");
        }
        
        if (dep.getRecommendedArtifactCoordinates() != null && 
            !dep.getRecommendedArtifactCoordinates().isEmpty()) {
            tooltip.append("Jakarta Equivalent: ").append(dep.getRecommendedArtifactCoordinates());
        }
        
        tooltip.append("</html>");
        return tooltip.toString();
    }
}
