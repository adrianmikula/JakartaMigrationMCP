package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Coordinator class for managing updates to all dependency UI components.
 * Ensures consistent updates across all components and prevents missed updates.
 */
public class DependencyUIManager {
    
    private final DependenciesTableComponent dependenciesComponent;
    private final DependenciesTreeComponent dependenciesTreeComponent;
    private final DependencyGraphComponent dependencyGraphComponent;
    
    /**
     * Constructor with all dependency UI components.
     * 
     * @param dependenciesComponent The dependencies table component
     * @param dependenciesTreeComponent The dependencies tree component
     * @param dependencyGraphComponent The dependency graph component
     */
    public DependencyUIManager(
            DependenciesTableComponent dependenciesComponent,
            DependenciesTreeComponent dependenciesTreeComponent,
            DependencyGraphComponent dependencyGraphComponent) {
        this.dependenciesComponent = dependenciesComponent;
        this.dependenciesTreeComponent = dependenciesTreeComponent;
        this.dependencyGraphComponent = dependencyGraphComponent;
    }
    
    /**
     * Update all components with a list of dependencies.
     * This updates the table, tree, and graph components.
     * 
     * @param dependencies List of dependency information
     */
    public void updateAllDependencies(List<DependencyInfo> dependencies) {
        List<DependencyInfo> deps = dependencies != null ? dependencies : new ArrayList<>();
        dependenciesComponent.setDependencies(deps);
        dependenciesTreeComponent.setDependencies(deps);
        dependencyGraphComponent.setDependencies(deps);
    }
    
    /**
     * Update the graph component with a DependencyGraph.
     * 
     * @param graph The dependency graph
     */
    public void updateDependencyGraph(DependencyGraph graph) {
        dependencyGraphComponent.updateDependencyGraph(graph);
    }
    
    /**
     * Update the graph component with a DependencyGraph and status map.
     * 
     * @param graph The dependency graph
     * @param statusMap Map of artifact IDs to migration statuses
     */
    public void updateDependencyGraph(DependencyGraph graph, Map<String, DependencyMigrationStatus> statusMap) {
        dependencyGraphComponent.updateDependencyGraph(graph, statusMap);
    }
    
    /**
     * Update node statuses in the graph component.
     * 
     * @param statusMap Map of artifact IDs to status strings
     */
    public void updateNodeStatuses(Map<String, String> statusMap) {
        dependencyGraphComponent.updateNodeStatuses(statusMap);
    }
    
    /**
     * Clear all dependency components.
     */
    public void clearAllDependencies() {
        dependenciesComponent.clearDependencies();
        dependenciesTreeComponent.clearDependencies();
        dependencyGraphComponent.clearDependencies();
    }
    
    /**
     * Get the dependencies table component.
     * 
     * @return The dependencies table component
     */
    public DependenciesTableComponent getDependenciesComponent() {
        return dependenciesComponent;
    }
    
    /**
     * Get the dependencies tree component.
     * 
     * @return The dependencies tree component
     */
    public DependenciesTreeComponent getDependenciesTreeComponent() {
        return dependenciesTreeComponent;
    }
    
    /**
     * Get the dependency graph component.
     * 
     * @return The dependency graph component
     */
    public DependencyGraphComponent getDependencyGraphComponent() {
        return dependencyGraphComponent;
    }
}
