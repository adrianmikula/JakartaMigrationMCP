package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus;

import javax.swing.*;
import java.util.List;
import java.util.Map;

/**
 * Abstract base class for dependency UI components.
 * Defines common interface for updating and clearing dependency data.
 */
public abstract class AbstractDependencyUIComponent {
    
    /**
     * Set the dependencies to display in this component.
     * 
     * @param dependencies List of dependency information
     */
    public abstract void setDependencies(List<DependencyInfo> dependencies);
    
    /**
     * Clear all dependencies from this component.
     */
    public abstract void clearDependencies();
    
    /**
     * Get the UI panel for this component.
     * 
     * @return The JPanel containing the component's UI
     */
    public abstract JPanel getPanel();
    
    /**
     * Update the component with a DependencyGraph (optional, for graph-specific components).
     * Default implementation does nothing.
     * 
     * @param graph The dependency graph
     */
    public void updateDependencyGraph(DependencyGraph graph) {
        // Default implementation - components that don't support graphs can ignore this
    }
    
    /**
     * Update the component with a DependencyGraph and status map (optional, for graph-specific components).
     * Default implementation does nothing.
     * 
     * @param graph The dependency graph
     * @param statusMap Map of artifact IDs to migration statuses
     */
    public void updateDependencyGraph(DependencyGraph graph, Map<String, DependencyMigrationStatus> statusMap) {
        // Default implementation - components that don't support graphs can ignore this
    }
    
    /**
     * Update node statuses in the component (optional, for graph-specific components).
     * Default implementation does nothing.
     * 
     * @param statusMap Map of artifact IDs to status strings
     */
    public void updateNodeStatuses(Map<String, String> statusMap) {
        // Default implementation - components that don't support status updates can ignore this
    }
}
