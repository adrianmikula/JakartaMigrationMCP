/*
 * Copyright © 2026 Adrian Mikula
 *
 * All rights reserved.
 *
 * This software is proprietary and may not be used, copied,
 * modified, or distributed except under the terms of a
 * separate commercial license agreement.
 */
package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Dependency;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Namespace;
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

    public DependencyGraphComponent(Project project) {
        this.project = project;
        this.dependencyGraph = new DependencyGraph();
        this.panel = new JBPanel<>(new BorderLayout());
        this.graphCanvas = new GraphCanvas();
        this.layoutCombo = new JComboBox<>(new String[]{
            "Hierarchical", "Force-Directed", "Circular", "Tree"
        });
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
        // Convert flat list to DependencyGraph
        DependencyGraph graph = new DependencyGraph();
        Set<Artifact> nodes = new HashSet<>();
        Set<Dependency> edges = new HashSet<>();

        for (DependencyInfo dep : deps) {
            // Artifact record requires 5 params: groupId, artifactId, version, scope, transitive
            Artifact artifact = new Artifact(dep.getGroupId(), dep.getArtifactId(), 
                dep.getCurrentVersion(), "compile", dep.isTransitive());
            nodes.add(artifact);
        }

        // Create root node
        Artifact root = new Artifact("root", project.getName(), "1.0", "system", false);
        nodes.add(root);

        // Create edges from root to all dependencies
        for (Artifact artifact : nodes) {
            if (!artifact.equals(root)) {
                edges.add(new Dependency(root, artifact, "compile", false));
            }
        }

        this.dependencyGraph = new DependencyGraph(nodes, edges);
        updateGraphFromDependencyGraph();
    }

    /**
     * Update the graph with the real DependencyGraph from migration-core.
     */
    public void updateGraphFromDependencyGraph(DependencyGraph graph) {
        this.dependencyGraph = graph != null ? graph : new DependencyGraph();
        this.artifactStatusMap = new HashMap<String, DependencyMigrationStatus>();
        updateGraphFromDependencyGraph();
    }
    
    /**
     * Update the graph with the real DependencyGraph and status map.
     */
    public void updateGraphFromDependencyGraph(DependencyGraph graph, Map<String, DependencyMigrationStatus> statusMap) {
        this.dependencyGraph = graph != null ? graph : new DependencyGraph();
        this.artifactStatusMap = statusMap != null ? statusMap : new HashMap<String, DependencyMigrationStatus>();
        updateGraphFromDependencyGraph();
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
            identifierToArtifact.put(artifact.toIdentifier(), artifact);
        }

        // Create root node for the project
        GraphNode root = new GraphNode("root", project.getName(), GraphNode.NodeType.ROOT, RiskLevel.LOW);
        nodes.add(root);
        artifactToNode.put("root:root", root);

        // Create nodes for each artifact in the graph
        for (Artifact artifact : dependencyGraph.getNodes()) {
            String artifactId = artifact.artifactId();
            String groupId = artifact.groupId();
            String id = groupId + ":" + artifactId;

            // Skip the root node (already added)
            if ("root".equals(groupId) && project.getName().equals(artifactId)) {
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
            String toId = dep.to().groupId() + ":" + dep.to().artifactId();
            hasIncomingEdges.add(toId);
        }

        for (Artifact artifact : dependencyGraph.getNodes()) {
            String artifactId = artifact.artifactId();
            String groupId = artifact.groupId();
            String id = groupId + ":" + artifactId;

            // Skip root
            if ("root".equals(groupId) && project.getName().equals(artifactId)) {
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
