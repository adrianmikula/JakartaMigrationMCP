package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus;
import adrianmikula.jakartamigration.intellij.model.RiskLevel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Dependency graph component from TypeSpec: plugin-components.tsp
 * Displays module dependency graph with interactive visualization.
 */
public class DependencyGraphComponent {
    private final JPanel panel;
    private final Project project;
    private final GraphCanvas graphCanvas;
    private final JComboBox<String> layoutCombo;
    private final JCheckBox showNonOrgDependenciesCheck;
    private final JCheckBox transitiveOnlyCheck;
    private List<DependencyInfo> dependencies;
    private Set<String> orgNamespacePatterns = new HashSet<>();

    public DependencyGraphComponent(Project project) {
        this.project = project;
        this.dependencies = new ArrayList<>();
        this.panel = new JBPanel<>(new BorderLayout());
        this.graphCanvas = new GraphCanvas();
        this.layoutCombo = new JComboBox<>(new String[]{
            "Hierarchical", "Force-Directed", "Circular", "Tree"
        });
        this.showNonOrgDependenciesCheck = new JCheckBox("Show Non-Org Dependencies", true);
        this.transitiveOnlyCheck = new JCheckBox("Transitive Only", false);

        initializeComponent();
    }

    private void initializeComponent() {
        // Header with graph controls
        JPanel headerPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT));
        headerPanel.add(new JLabel("Module Dependency Graph"));

        headerPanel.add(new JLabel("Layout:"));
        layoutCombo.addActionListener(this::handleLayoutChange);
        headerPanel.add(layoutCombo);

        showNonOrgDependenciesCheck.addActionListener(this::handleShowNonOrgDependencies);
        headerPanel.add(showNonOrgDependenciesCheck);

        transitiveOnlyCheck.addActionListener(this::handleTransitiveFilter);
        headerPanel.add(transitiveOnlyCheck);

        // Graph visualization area
        JPanel graphPanel = new JBPanel<>(new BorderLayout());
        graphPanel.setBorder(BorderFactory.createTitledBorder("Dependency Graph"));

        JBScrollPane scrollPane = new JBScrollPane(graphCanvas);
        scrollPane.setPreferredSize(new Dimension(600, 400));
        graphPanel.add(scrollPane, BorderLayout.CENTER);

        // Controls panel
        JPanel controlsPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT));
        JButton zoomInButton = new JButton("Zoom In");
        JButton zoomOutButton = new JButton("Zoom Out");
        JButton resetViewButton = new JButton("Reset View");
        JButton loadDataButton = new JButton("Load Dependencies");

        zoomInButton.addActionListener(e -> graphCanvas.zoomIn());
        zoomOutButton.addActionListener(e -> graphCanvas.zoomOut());
        resetViewButton.addActionListener(e -> graphCanvas.resetView());
        loadDataButton.addActionListener(this::handleLoadDependencies);

        controlsPanel.add(zoomInButton);
        controlsPanel.add(zoomOutButton);
        controlsPanel.add(resetViewButton);
        controlsPanel.add(loadDataButton);

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(graphPanel, BorderLayout.CENTER);
        panel.add(controlsPanel, BorderLayout.SOUTH);
    }

    private void handleLayoutChange(ActionEvent e) {
        String selected = (String) layoutCombo.getSelectedItem();
        if ("Hierarchical".equals(selected)) {
            graphCanvas.setLayoutStrategy(new HierarchicalLayoutStrategy());
        } else if ("Force-Directed".equals(selected)) {
            graphCanvas.setLayoutStrategy(new ForceDirectedLayoutStrategy());
        } else if ("Circular".equals(selected)) {
            graphCanvas.setLayoutStrategy(new CircularLayoutStrategy());
        } else if ("Tree".equals(selected)) {
            graphCanvas.setLayoutStrategy(new TreeLayoutStrategy());
        }
    }

    private void handleShowNonOrgDependencies(ActionEvent e) {
        updateGraphFromDependencies();
    }

    private void handleTransitiveFilter(ActionEvent e) {
        updateGraphFromDependencies();
    }

    private void handleLoadDependencies(ActionEvent e) {
        if (dependencies.isEmpty()) {
            Messages.showInfoMessage(project, "No dependencies loaded. Run 'Analyze Readiness' first to load dependency data.", "Info");
        }
        updateGraphFromDependencies();
    }

    /**
     * Set organization namespace patterns for internal dependency detection.
     */
    public void setOrgNamespacePatterns(Set<String> patterns) {
        this.orgNamespacePatterns = patterns != null ? patterns : new HashSet<>();
        updateGraphFromDependencies();
    }

    /**
     * Update the graph with dependency data.
     */
    public void updateGraph(List<DependencyInfo> deps) {
        this.dependencies = deps != null ? deps : new ArrayList<>();
        updateGraphFromDependencies();
    }

    private void updateGraphFromDependencies() {
        List<GraphNode> nodes = new ArrayList<>();
        List<GraphEdge> edges = new ArrayList<>();
        boolean showNonOrgDeps = showNonOrgDependenciesCheck.isSelected();
        boolean showTransitiveOnly = transitiveOnlyCheck.isSelected();

        // Create root node
        GraphNode root = new GraphNode("root", project.getName(), GraphNode.NodeType.ROOT, RiskLevel.LOW);
        nodes.add(root);

        // Create nodes for each dependency
        for (int i = 0; i < dependencies.size(); i++) {
            DependencyInfo dep = dependencies.get(i);
            String id = dep.getGroupId() + ":" + dep.getArtifactId();
            GraphNode.NodeType type = dep.isBlocker() ? GraphNode.NodeType.DEPENDENCY : GraphNode.NodeType.MODULE;
            RiskLevel riskLevel = RiskLevel.LOW;
            boolean isTransitive = dep.isTransitive();

            // Check if this is an org internal dependency
            boolean isOrgInternal = isOrgDependency(dep.getGroupId());

            // If org internal and not showing non-org, skip
            if (isOrgInternal && !showNonOrgDeps) {
                continue;
            }

            // If transitive-only filter is on and this is not transitive, skip
            if (showTransitiveOnly && !isTransitive) {
                continue;
            }

            // Create node with transitive flag for visual styling
            GraphNode node = new GraphNode(id, dep.getArtifactId(), type, riskLevel, isOrgInternal, isTransitive);
            nodes.add(node);

            // Create edge from root to this node
            edges.add(new GraphEdge(root, node, GraphEdge.EdgeType.DEPENDENCY, dep.isBlocker()));

            // Create edges between dependencies (simplified - connect to previous)
            if (i > 0) {
                DependencyInfo prevDep = dependencies.get(i - 1);
                GraphNode prevNode = new GraphNode(
                    prevDep.getGroupId() + ":" + prevDep.getArtifactId(),
                    prevDep.getArtifactId(),
                    GraphNode.NodeType.MODULE,
                    RiskLevel.LOW
                );
                if (!nodes.contains(prevNode)) {
                    // Find existing node
                    for (GraphNode existing : nodes) {
                        if (existing.getId().equals(prevNode.getId())) {
                            prevNode = existing;
                            break;
                        }
                    }
                }
                edges.add(new GraphEdge(node, prevNode, GraphEdge.EdgeType.TRANSITIVE, false));
            }
        }

        graphCanvas.setNodes(nodes);
        graphCanvas.setEdges(edges);
    }

    /**
     * Check if a dependency belongs to the organization based on namespace patterns.
     */
    private boolean isOrgDependency(String groupId) {
        for (String pattern : orgNamespacePatterns) {
            if (matchPattern(pattern, groupId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Match a namespace pattern (supports wildcards like com.myorg.*).
     */
    private boolean matchPattern(String pattern, String groupId) {
        if (pattern.endsWith(".*")) {
            String prefix = pattern.substring(0, pattern.length() - 2);
            return groupId.equals(prefix) || groupId.startsWith(prefix + ".");
        }
        return groupId.equals(pattern);
    }

    public JPanel getPanel() {
        return panel;
    }
}
