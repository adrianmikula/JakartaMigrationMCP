package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Dependency;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Namespace;
import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus;
import adrianmikula.jakartamigration.intellij.model.RiskLevel;
import adrianmikula.jakartamigration.intellij.license.CheckLicense;
import adrianmikula.jakartamigration.intellij.ui.UIColors;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;

/**
 * Dependency graph component that displays module dependency graph with interactive visualization.
 * Uses the real DependencyGraph from migration-core library.
 */
public class DependencyGraphComponent {
    private final JPanel panel;
    private final Project project;
    private final GraphCanvas graphCanvas;
    private final JComboBox<String> layoutCombo;
    private final JCheckBox organisationalDependenciesCheck;
    private final JCheckBox directDependenciesCheck;
    private final JCheckBox transitiveDependenciesCheck;
    private DependencyGraph dependencyGraph;
    private Set<String> orgNamespacePatterns = new HashSet<>();
    private Map<String, DependencyMigrationStatus> artifactStatusMap = new HashMap<>();
    private Set<String> directDependencyIds = new HashSet<>();
    private List<DependencyInfo> lastFlatDependencies = new ArrayList<>();

    public DependencyGraphComponent(Project project) {
        this.project = project;
        this.dependencyGraph = new DependencyGraph();
        this.panel = new JBPanel<>(new BorderLayout());
        this.graphCanvas = new GraphCanvas();
        
        // Check if user is premium
        boolean isPremium = CheckLicense.isLicensed();
        
        // Setup layout dropdown based on license status
        if (isPremium) {
            // Premium users get all layout options
            this.layoutCombo = new JComboBox<>(new String[]{
                "Hierarchical", "Circular", "Tree", "Force-Directed"
            });
        } else {
            // Freemium users get only Hierarchical with premium message
            this.layoutCombo = new JComboBox<>(new String[]{
                "Hierarchical", "Other layouts (Premium only)"
            });
        }
        
        this.organisationalDependenciesCheck = new JCheckBox("Organisational Dependencies", true);
        this.directDependenciesCheck = new JCheckBox("Direct Dependencies", true);
        this.transitiveDependenciesCheck = new JCheckBox("Transitive Dependencies", true);

        initializeComponent();
    }

    private void initializeComponent() {
        // Header with graph controls
        JPanel headerPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT));
        headerPanel.add(new JLabel("Module Dependency Graph"));

        headerPanel.add(new JLabel("Layout:"));
        layoutCombo.addActionListener(this::handleLayoutChange);
        headerPanel.add(layoutCombo);

        organisationalDependenciesCheck.addActionListener(this::handleFilterChange);
        headerPanel.add(organisationalDependenciesCheck);

        directDependenciesCheck.addActionListener(this::handleFilterChange);
        headerPanel.add(directDependenciesCheck);

        transitiveDependenciesCheck.addActionListener(this::handleFilterChange);
        headerPanel.add(transitiveDependenciesCheck);

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

        // Legend panel
        JPanel legendPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 15, 5));
        legendPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        legendPanel.setBackground(UIColors.PANEL_BACKGROUND_ALT);

        // Compatible (Green)
        JPanel legendItem1 = createLegendItem(new Color(40, 167, 69), "Jakarta Compatible");
        legendPanel.add(legendItem1);

        // Needs Upgrade (Yellow)
        JPanel legendItem2 = createLegendItem(new Color(255, 193, 7), "Needs Upgrade");
        legendPanel.add(legendItem2);

        // No Jakarta Version (Red)
        JPanel legendItem3 = createLegendItem(new Color(220, 53, 69), "No Jakarta Version");
        legendPanel.add(legendItem3);

        // Organisational - Thicker border indicator
        JPanel legendItem4 = createLegendItemWithBorder(new Color(108, 117, 125), "Organisational (thicker border)");
        legendPanel.add(legendItem4);

        // Combine controls and legend in a wrapper panel
        JPanel bottomPanel = new JBPanel<>(new BorderLayout());
        bottomPanel.add(controlsPanel, BorderLayout.NORTH);
        bottomPanel.add(legendPanel, BorderLayout.SOUTH);

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(graphPanel, BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.SOUTH);
    }

    private JPanel createLegendItem(Color color, String label) {
        JPanel itemPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 5, 0));
        itemPanel.setOpaque(false);

        JPanel colorBox = new JPanel();
        colorBox.setBackground(color);
        colorBox.setPreferredSize(new Dimension(16, 16));
        colorBox.setBorder(BorderFactory.createLineBorder(UIColors.BORDER));

        JLabel labelText = new JLabel(label);
        labelText.setFont(labelText.getFont().deriveFont(Font.PLAIN, 11f));

        itemPanel.add(colorBox);
        itemPanel.add(labelText);

        return itemPanel;
    }

    private JPanel createLegendItemWithBorder(Color color, String label) {
        JPanel itemPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 5, 0));
        itemPanel.setOpaque(false);

        JPanel colorBox = new JPanel();
        colorBox.setBackground(color);
        colorBox.setPreferredSize(new Dimension(16, 16));
        colorBox.setBorder(BorderFactory.createLineBorder(UIColors.BORDER, 3));

        JLabel labelText = new JLabel(label);
        labelText.setFont(labelText.getFont().deriveFont(Font.PLAIN, 11f));

        itemPanel.add(colorBox);
        itemPanel.add(labelText);

        return itemPanel;
    }

    private void handleLayoutChange(ActionEvent e) {
        String selected = (String) layoutCombo.getSelectedItem();
        
        // Check if user is trying to access premium layouts
        if ("Other layouts (Premium only)".equals(selected)) {
            boolean isPremium = CheckLicense.isLicensed();
            if (!isPremium) {
                Messages.showInfoMessage(
                    project,
                    "Other graph layouts (Circular, Tree, Force-Directed) are available with Premium subscription.\n\n" +
                    "Upgrade to Premium to unlock all layout options:\n" +
                    "• Circular layout - ideal for moderate dependency counts\n" +
                    "• Tree layout - perfect for hierarchical structures\n" +
                    "• Force-Directed layout - best for large, complex graphs\n\n" +
                    "Hierarchical layout remains available for all users.",
                    "Premium Feature");
                // Reset to Hierarchical
                layoutCombo.setSelectedItem("Hierarchical");
                graphCanvas.setLayoutStrategy(new HierarchicalLayoutStrategy());
                return;
            }
        }
        
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

    private void handleFilterChange(ActionEvent e) {
        updateGraphFromDependencyGraph();
    }

    private void handleLoadDependencies(ActionEvent e) {
        if (dependencyGraph == null || dependencyGraph.nodeCount() == 0) {
            Messages.showInfoMessage(project, "No dependencies loaded. Run 'Analyze Readiness' first to load dependency data.", "Info");
        }
        updateGraphFromDependencyGraph();
    }

    /**
     * Set organization namespace patterns for internal dependency detection.
     */
    public void setOrgNamespacePatterns(Set<String> patterns) {
        this.orgNamespacePatterns = patterns != null ? patterns : new HashSet<>();
        updateGraphFromDependencyGraph();
    }

    /**
     * Update the graph with dependency data from a flat list (legacy method).
     */
    public void updateGraph(List<DependencyInfo> deps) {
        // Handle null or empty dependency list
        if (deps == null) {
            deps = new ArrayList<>();
        }
        this.lastFlatDependencies = new ArrayList<>(deps);

        // Convert flat list to DependencyGraph
        Set<Artifact> nodes = new HashSet<>();
        Set<Dependency> edges = new HashSet<>();

        for (DependencyInfo dep : deps) {
            // Artifact record requires 5 params: groupId, artifactId, version, scope, transitive
            // Skip if required fields are null
            if (dep == null || dep.getGroupId() == null || dep.getArtifactId() == null) {
                continue;
            }
            String version = dep.getCurrentVersion() != null ? dep.getCurrentVersion() : "unknown";
            Artifact artifact = new Artifact(dep.getGroupId(), dep.getArtifactId(),
                version, "compile", dep.isTransitive());
            nodes.add(artifact);
        }

        // Create the dependency graph without a root node
        // updateGraphFromDependencyGraph() will create the root node in the canvas
        this.dependencyGraph = new DependencyGraph(nodes, edges);
        updateGraphFromDependencyGraph();
    }

    /**
     * Set dependencies (for DependencyUIManager compatibility).
     */
    public void setDependencies(List<DependencyInfo> dependencies) {
        updateGraph(dependencies);
    }

    /**
     * Clear dependencies (for DependencyUIManager compatibility).
     */
    public void clearDependencies() {
        updateGraph(new ArrayList<>());
    }

    /**
     * Fallback to the last known flat dependency list when the DependencyGraph is empty.
     * Preserves the provided status map so colours remain accurate.
     */
    private void fallbackToFlatDependencies(Map<String, DependencyMigrationStatus> statusMap) {
        Set<Artifact> nodes = new HashSet<>();
        for (DependencyInfo dep : lastFlatDependencies) {
            if (dep == null || dep.getGroupId() == null || dep.getArtifactId() == null) {
                continue;
            }
            String version = dep.getCurrentVersion() != null ? dep.getCurrentVersion() : "unknown";
            nodes.add(new Artifact(dep.getGroupId(), dep.getArtifactId(), version, "compile", dep.isTransitive()));
        }
        this.dependencyGraph = new DependencyGraph(nodes, new HashSet<>());
        this.artifactStatusMap = statusMap != null ? statusMap : new HashMap<>();
        updateGraphFromDependencyGraph();
    }

    private boolean isEmptyGraph(DependencyGraph graph) {
        return graph == null || graph.getNodes() == null || graph.getNodes().isEmpty();
    }

    /**
     * Update the graph with the real DependencyGraph from migration-core.
     */
    public void updateDependencyGraph(DependencyGraph graph) {
        if (isEmptyGraph(graph) && !lastFlatDependencies.isEmpty()) {
            fallbackToFlatDependencies(new HashMap<>());
            return;
        }
        this.dependencyGraph = graph != null ? graph : new DependencyGraph();
        this.artifactStatusMap = new HashMap<>();
        updateGraphFromDependencyGraph();
    }

    /**
     * Update the graph with the real DependencyGraph and status map.
     */
    public void updateDependencyGraph(DependencyGraph graph, Map<String, DependencyMigrationStatus> statusMap) {
        if (isEmptyGraph(graph) && !lastFlatDependencies.isEmpty()) {
            fallbackToFlatDependencies(statusMap);
            return;
        }
        this.dependencyGraph = graph != null ? graph : new DependencyGraph();
        this.artifactStatusMap = statusMap != null ? statusMap : new HashMap<>();
        updateGraphFromDependencyGraph();
    }

    /**
     * Update the graph with the real DependencyGraph from migration-core.
     */
    public void updateGraphFromDependencyGraph(DependencyGraph graph) {
        if (isEmptyGraph(graph) && !lastFlatDependencies.isEmpty()) {
            fallbackToFlatDependencies(new HashMap<>());
            return;
        }
        this.dependencyGraph = graph != null ? graph : new DependencyGraph();
        this.artifactStatusMap = new HashMap<>();
        updateGraphFromDependencyGraph();
    }

    /**
     * Update the graph with the real DependencyGraph and status map.
     */
    public void updateGraphFromDependencyGraph(DependencyGraph graph, Map<String, DependencyMigrationStatus> statusMap) {
        if (isEmptyGraph(graph) && !lastFlatDependencies.isEmpty()) {
            fallbackToFlatDependencies(statusMap);
            return;
        }
        this.dependencyGraph = graph != null ? graph : new DependencyGraph();
        this.artifactStatusMap = statusMap != null ? statusMap : new HashMap<>();
        updateGraphFromDependencyGraph();
    }

    /**
     * Update node statuses from a string-based status map and refresh the graph.
     * Used to sync status updates from DependenciesTableComponent async analysis.
     */
    public void updateNodeStatuses(Map<String, String> statusMap) {
        if (statusMap == null || statusMap.isEmpty()) {
            return;
        }
        
        // Convert string status to enum and update artifactStatusMap
        for (Map.Entry<String, String> entry : statusMap.entrySet()) {
            try {
                DependencyMigrationStatus status = DependencyMigrationStatus.valueOf(entry.getValue());
                artifactStatusMap.put(entry.getKey(), status);
            } catch (IllegalArgumentException e) {
                // Ignore invalid status values
            }
        }
        
        // Refresh the graph with updated statuses
        if (dependencyGraph != null) {
            updateGraphFromDependencyGraph();
        }
    }

    private void updateGraphFromDependencyGraph() {
        if (dependencyGraph == null) {
            return;
        }

        List<GraphNode> nodes = new ArrayList<>();
        List<GraphEdge> edges = new ArrayList<>();
        boolean showOrganisationalDeps = organisationalDependenciesCheck.isSelected();
        boolean showDirectDeps = directDependenciesCheck.isSelected();
        boolean showTransitiveDeps = transitiveDependenciesCheck.isSelected();

        // Map from Artifact to GraphNode for quick lookup
        Map<String, GraphNode> artifactToNode = new HashMap<>();
        
        // Also track nodes by toIdentifier() for edge lookup
        Map<String, Artifact> identifierToArtifact = new HashMap<>();
        for (Artifact artifact : dependencyGraph.getNodes()) {
            // Skip artifacts with null groupId or artifactId
            if (artifact.groupId() == null || artifact.artifactId() == null) {
                continue;
            }
            identifierToArtifact.put(artifact.toIdentifier(), artifact);
        }

        // Create root node for the project
        String projectName = project.getName() != null ? project.getName() : "project";
        GraphNode root = new GraphNode("root:root", "root", GraphNode.NodeType.ROOT, RiskLevel.LOW);
        nodes.add(root);
        artifactToNode.put("root:root", root);

        // Create nodes for each artifact in the graph
        for (Artifact artifact : dependencyGraph.getNodes()) {
            String artifactId = artifact.artifactId();
            String groupId = artifact.groupId();

            // Skip artifacts with null groupId or artifactId
            if (groupId == null || artifactId == null) {
                continue;
            }

            String id = groupId + ":" + artifactId;

            // Skip the canvas root node (already added above)
            if ("root:root".equals(id)) {
                continue;
            }

            GraphNode.NodeType type = GraphNode.NodeType.DEPENDENCY;
            RiskLevel riskLevel = RiskLevel.LOW;
            boolean isTransitive = artifact.transitive();

            // Check if this is an org internal dependency
            boolean isOrgInternal = isOrgDependency(groupId);

            // Filter based on checkbox states
            // If org internal and not showing organisational deps, skip
            if (isOrgInternal && !showOrganisationalDeps) {
                continue;
            }
            
            // If transitive and not showing transitive deps, skip
            if (isTransitive && !showTransitiveDeps) {
                continue;
            }
            
            // If NOT transitive (direct) and not showing direct deps, skip
            if (!isTransitive && !showDirectDeps) {
                continue;
            }

            // Create node with transitive flag for visual styling
            GraphNode node = new GraphNode(id, artifactId, type, riskLevel, isOrgInternal, isTransitive);
            
            // Set migration status from the status map
            DependencyMigrationStatus status = artifactStatusMap.get(id);
            if (status != null) {
                node.setMigrationStatus(status);
            } else {
                node.setMigrationStatus(DependencyMigrationStatus.NO_JAKARTA_VERSION);
            }
            
            nodes.add(node);
            artifactToNode.put(id, node);
        }

        // Create edges from the real DependencyGraph
        for (Dependency dep : dependencyGraph.getEdges()) {
            // Skip dependencies with null from/to artifacts
            if (dep.from() == null || dep.to() == null) {
                continue;
            }
            if (dep.from().groupId() == null || dep.from().artifactId() == null) {
                continue;
            }
            if (dep.to().groupId() == null || dep.to().artifactId() == null) {
                continue;
            }

            String fromId = dep.from().groupId() + ":" + dep.from().artifactId();
            String toId = dep.to().groupId() + ":" + dep.to().artifactId();

            GraphNode fromNode = artifactToNode.get(fromId);
            GraphNode toNode = artifactToNode.get(toId);

            if (fromNode != null && toNode != null) {
                GraphEdge.EdgeType edgeType = dep.optional() ? GraphEdge.EdgeType.TRANSITIVE : GraphEdge.EdgeType.DEPENDENCY;
                edges.add(new GraphEdge(fromNode, toNode, edgeType, dep.optional()));
            }
        }

        // Add edges from root to all direct dependencies (those with no incoming edges)
        Set<String> hasIncomingEdges = new HashSet<>();
        for (Dependency dep : dependencyGraph.getEdges()) {
            // Skip dependencies with null to artifact
            if (dep.to() == null || dep.to().groupId() == null || dep.to().artifactId() == null) {
                continue;
            }
            String toId = dep.to().groupId() + ":" + dep.to().artifactId();
            hasIncomingEdges.add(toId);
        }

        for (Artifact artifact : dependencyGraph.getNodes()) {
            String artifactId = artifact.artifactId();
            String groupId = artifact.groupId();

            // Skip artifacts with null groupId or artifactId
            if (groupId == null || artifactId == null) {
                continue;
            }

            String id = groupId + ":" + artifactId;

            // Skip canvas root node (already added above)
            // But allow "root:project" and other root:* nodes from the graph
            if ("root:root".equals(id)) {
                continue;
            }

            // Skip if already has incoming edges (not a direct dependency)
            if (hasIncomingEdges.contains(id)) {
                continue;
            }

            // Skip if filtered out
            GraphNode node = artifactToNode.get(id);
            if (node == null) {
                continue;
            }

            // Add edge from root
            edges.add(new GraphEdge(root, node, GraphEdge.EdgeType.DEPENDENCY, false));
        }

        graphCanvas.setNodesAndEdges(nodes, edges);
        
        // Auto-select optimal layout based on dependency count
        selectOptimalLayout(nodes.size());
    }
    
    /**
     * Auto-select the best layout based on the number of dependencies
     * Requirements:
     * - 5 or less: tree mode
     * - 5 to 25: circular mode
     * - 25 to 100: force-directed mode
     * - more than 100: hierarchical mode (force-directed is too expensive for large graphs)
     */
    private void selectOptimalLayout(int nodeCount) {
        String optimalLayout;
        if (nodeCount <= 5) {
            optimalLayout = "Tree";
        } else if (nodeCount <= 25) {
            optimalLayout = "Circular";
        } else if (nodeCount <= 100) {
            optimalLayout = "Force-Directed";
        } else {
            optimalLayout = "Hierarchical";
        }
        
        layoutCombo.setSelectedItem(optimalLayout);
        handleLayoutChange(null); // Apply the selected layout
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
