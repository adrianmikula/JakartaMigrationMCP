package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import adrianmikula.jakartamigration.intellij.model.DependencyTreeBuilder;
import adrianmikula.jakartamigration.intellij.ui.tree.DependencyTreeNode;
import adrianmikula.jakartamigration.intellij.ui.tree.DependencyTreeRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Tree component for displaying dependencies in a hierarchical structure.
 * Experimental feature added as a new tab alongside the flat table view.
 */
public class DependenciesTreeComponent extends AbstractDependencyUIComponent {
    private final JPanel panel;
    private final JTree tree;
    private final JCheckBox transitiveFilter;
    private final JCheckBox organizationalFilter;
    private final JCheckBox mergeDuplicatesFilter;
    private final RecipesPanelComponent recipesPanel;
    private final DependencyTreeBuilder treeBuilder;
    
    private List<DependencyInfo> allDependencies;
    private List<DependencyInfo> filteredDependencies;

    public DependenciesTreeComponent(Project project) {
        this.allDependencies = new ArrayList<>();
        this.filteredDependencies = new ArrayList<>();
        this.treeBuilder = new DependencyTreeBuilder();
        this.panel = new JBPanel<>(new BorderLayout());

        this.tree = new JTree();
        this.transitiveFilter = new JCheckBox("Hide Transitive Dependencies", false);
        this.organizationalFilter = new JCheckBox("Show All Organisational Artifacts", false);
        this.mergeDuplicatesFilter = new JCheckBox("Merge Duplicates", false);
        this.recipesPanel = new RecipesPanelComponent(project);

        initializeComponent();
    }

    private void initializeComponent() {
        // Header panel with filters
        JPanel headerPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT));
        headerPanel.add(new JLabel("Dependencies Tree (Experimental)"));
        
        transitiveFilter.addActionListener(e -> applyFilters());
        headerPanel.add(transitiveFilter);
        
        organizationalFilter.addActionListener(e -> applyFilters());
        headerPanel.add(organizationalFilter);
        
        mergeDuplicatesFilter.addActionListener(e -> applyFilters());
        headerPanel.add(mergeDuplicatesFilter);

        // Tree setup
        tree.setCellRenderer(new DependencyTreeRenderer());
        tree.setRootVisible(false);
        tree.setSelectionModel(new DefaultTreeSelectionModel());
        tree.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tree.setToggleClickCount(1); // Enable single-click expand/collapse
        
        JBScrollPane scrollPane = new JBScrollPane(tree);
        tree.setShowsRootHandles(true);

        // Add selection listener to update recipes panel
        tree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                updateRecipesPanel();
            }
        });

        // Actions panel for expand/collapse
        JPanel actionsPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT));
        JButton expandAllButton = new JButton("Expand All");
        expandAllButton.addActionListener(e -> expandAll());
        JButton collapseAllButton = new JButton("Collapse All");
        collapseAllButton.addActionListener(e -> collapseAll());
        
        actionsPanel.add(expandAllButton);
        actionsPanel.add(collapseAllButton);

        // Layout
        JPanel centerPanel = new JBPanel<>(new BorderLayout());
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        centerPanel.add(recipesPanel.getPanel(), BorderLayout.SOUTH);

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(centerPanel, BorderLayout.CENTER);
        panel.add(actionsPanel, BorderLayout.SOUTH);
    }

    /**
     * Set the dependencies to display in the tree.
     */
    @Override
    public void setDependencies(List<DependencyInfo> dependencies) {
        this.allDependencies = dependencies != null ? dependencies : new ArrayList<>();
        applyFilters();
    }

    @Override
    public void clearDependencies() {
        setDependencies(new ArrayList<>());
    }

    /**
     * Apply filters to the dependency list and rebuild the tree.
     */
    private void applyFilters() {
        filteredDependencies = new ArrayList<>();
        
        boolean hideTransitive = transitiveFilter.isSelected();
        boolean showOrganizational = organizationalFilter.isSelected();
        boolean mergeDuplicates = mergeDuplicatesFilter.isSelected();

        for (DependencyInfo dep : allDependencies) {
            // Transitive filter
            if (hideTransitive && dep.isTransitive()) {
                continue;
            }
            
            // Organizational filter
            if (!showOrganizational && dep.isOrganizational()) {
                continue;
            }
            
            filteredDependencies.add(dep);
        }

        rebuildTree(mergeDuplicates);
    }

    /**
     * Rebuild the tree from filtered dependencies.
     */
    private void rebuildTree(boolean mergeDuplicates) {
        // Build tree structure
        List<DependencyInfo> treeRoots = treeBuilder.buildTree(filteredDependencies, mergeDuplicates);
        
        // Create tree nodes
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Dependencies");
        for (DependencyInfo dep : treeRoots) {
            DependencyTreeNode node = new DependencyTreeNode(dep);
            node.buildChildNodes();
            root.add(node);
        }
        
        // Update tree model
        tree.setModel(new javax.swing.tree.DefaultTreeModel(root));
        
        // Expand first level
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

    /**
     * Update recipes panel based on selected tree node.
     */
    private void updateRecipesPanel() {
        TreePath selectedPath = tree.getSelectionPath();
        if (selectedPath == null) {
            recipesPanel.hide();
            return;
        }
        
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
        if (selectedNode instanceof DependencyTreeNode) {
            DependencyTreeNode depNode = (DependencyTreeNode) selectedNode;
            recipesPanel.updateForDependency(depNode.getDependency());
        } else {
            recipesPanel.hide();
        }
    }

    /**
     * Expand all tree nodes.
     */
    private void expandAll() {
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

    /**
     * Collapse all tree nodes.
     */
    private void collapseAll() {
        for (int i = tree.getRowCount() - 1; i >= 0; i--) {
            tree.collapseRow(i);
        }
    }

    public void setPremiumUser(boolean isPremium) {
        recipesPanel.setPremiumUser(isPremium);
    }

    public JPanel getPanel() {
        return panel;
    }

    public JCheckBox getTransitiveFilter() {
        return transitiveFilter;
    }

    public JCheckBox getOrganizationalFilter() {
        return organizationalFilter;
    }

    public JCheckBox getMergeDuplicatesFilter() {
        return mergeDuplicatesFilter;
    }
}
