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
import java.util.List;

/**
 * Dependency graph component from TypeSpec: plugin-components.tsp
 * Displays module dependency graph with interactive visualization.
 */
public class DependencyGraphComponent {
    private final JPanel panel;
    private final Project project;
    private final GraphCanvas graphCanvas;
    private final JComboBox<String> layoutCombo;
    private final JCheckBox showCriticalPathCheck;
    private final JCheckBox showLabelsCheck;
    private List<DependencyInfo> dependencies;

    public DependencyGraphComponent(Project project) {
        this.project = project;
        this.dependencies = new ArrayList<>();
        this.panel = new JBPanel<>(new BorderLayout());
        this.graphCanvas = new GraphCanvas();
        this.layoutCombo = new JComboBox<>(new String[]{
            "Hierarchical", "Force-Directed", "Circular", "Tree"
        });
        this.showCriticalPathCheck = new JCheckBox("Highlight Critical Path", true);
        this.showLabelsCheck = new JCheckBox("Show Labels", true);

        initializeComponent();
        loadSampleData();
    }

    private void initializeComponent() {
        // Header with graph controls
        JPanel headerPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT));
        headerPanel.add(new JLabel("Module Dependency Graph"));

        headerPanel.add(new JLabel("Layout:"));
        layoutCombo.addActionListener(this::handleLayoutChange);
        headerPanel.add(layoutCombo);

        showCriticalPathCheck.addActionListener(this::handleShowCriticalPath);
        headerPanel.add(showCriticalPathCheck);

        showLabelsCheck.setSelected(true);
        showLabelsCheck.addActionListener(this::handleShowLabels);
        headerPanel.add(showLabelsCheck);

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

    private void handleShowCriticalPath(ActionEvent e) {
        graphCanvas.setShowCriticalPath(showCriticalPathCheck.isSelected());
    }

    private void handleShowLabels(ActionEvent e) {
        graphCanvas.setShowLabels(showLabelsCheck.isSelected());
    }

    private void handleLoadDependencies(ActionEvent e) {
        if (dependencies.isEmpty()) {
            Messages.showInfoMessage(project, "No dependencies loaded. Using sample data.", "Info");
        }
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

        // Create root node
        GraphNode root = new GraphNode("root", project.getName(), GraphNode.NodeType.ROOT, RiskLevel.LOW);
        nodes.add(root);

        // Create nodes for each dependency
        for (int i = 0; i < dependencies.size(); i++) {
            DependencyInfo dep = dependencies.get(i);
            String id = dep.getGroupId() + ":" + dep.getArtifactId();
            GraphNode.NodeType type = dep.isBlocker() ? GraphNode.NodeType.DEPENDENCY : GraphNode.NodeType.MODULE;
            RiskLevel riskLevel = dep.getRiskLevel() != null ? dep.getRiskLevel() : RiskLevel.LOW;

            GraphNode node = new GraphNode(id, dep.getArtifactId(), type, riskLevel);
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
                    prevDep.getRiskLevel() != null ? prevDep.getRiskLevel() : RiskLevel.LOW
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
     * Load sample data for demonstration.
     */
    public void loadSampleData() {
        List<DependencyInfo> sampleDeps = new ArrayList<>();

        sampleDeps.add(new DependencyInfo(
            "javax.servlet", "javax.servlet-api", "4.0.1",
            "jakarta.servlet-api:5.0.0",
            DependencyMigrationStatus.REQUIRES_MANUAL_MIGRATION,
            true, RiskLevel.CRITICAL, "High impact - core API"
        ));

        sampleDeps.add(new DependencyInfo(
            "javax.persistence", "javax.persistence-api", "2.2.3",
            "jakarta.persistence:jakarta.persistence-api:3.0.0",
            DependencyMigrationStatus.NEEDS_UPGRADE,
            false, RiskLevel.HIGH, "Medium impact"
        ));

        sampleDeps.add(new DependencyInfo(
            "javax.annotation", "javax.annotation-api", "1.3.2",
            "jakarta.annotation:jakarta.annotation-api:2.0.0",
            DependencyMigrationStatus.COMPATIBLE,
            false, RiskLevel.LOW, "Low impact"
        ));

        sampleDeps.add(new DependencyInfo(
            "org.springframework", "spring-web", "5.3.0",
            "6.0.0 (requires Jakarta EE 9)",
            DependencyMigrationStatus.NO_JAKARTA_VERSION,
            true, RiskLevel.CRITICAL, "Major upgrade needed"
        ));

        sampleDeps.add(new DependencyInfo(
            "org.hibernate", "hibernate-core", "5.4.0",
            "6.0.0 (requires Jakarta EE 9)",
            DependencyMigrationStatus.REQUIRES_MANUAL_MIGRATION,
            true, RiskLevel.HIGH, "Major changes"
        ));

        updateGraph(sampleDeps);
    }

    public JPanel getPanel() {
        return panel;
    }
}
