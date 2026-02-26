package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyAnalysisReport;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Namespace;
import adrianmikula.jakartamigration.dependencyanalysis.domain.VersionRecommendation;
import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus;
import adrianmikula.jakartamigration.intellij.model.DependencySummary;
import adrianmikula.jakartamigration.intellij.model.MigrationDashboard;
import adrianmikula.jakartamigration.intellij.model.MigrationStatus;
import adrianmikula.jakartamigration.intellij.service.AdvancedScanningService;
import adrianmikula.jakartamigration.intellij.service.MigrationAnalysisService;
import adrianmikula.jakartamigration.analysis.persistence.CentralMigrationAnalysisStore;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Main migration tool window from TypeSpec: plugin-components.tsp
 */
public class MigrationToolWindow implements ToolWindowFactory {
    public static final String TOOL_WINDOW_ID = "JakartaMigrationToolWindow";

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        MigrationToolWindowContent content = new MigrationToolWindowContent(project);
        Content tabContent = ContentFactory.getInstance().createContent(content.getContentPanel(), "Jakarta Migration",
                false);
        toolWindow.getContentManager().addContent(tabContent);
    }

    public static class MigrationToolWindowContent {
        private static final Logger LOG = Logger.getInstance(MigrationToolWindowContent.class);

        private final JPanel contentPanel;
        private final AdvancedScanningService advancedScanningService;
        private final Project project;
        private final MigrationAnalysisService analysisService;
        private final CentralMigrationAnalysisStore store;

        // UI Components
        private DashboardComponent dashboardComponent;
        private DependenciesTableComponent dependenciesComponent;
        private DependencyGraphComponent dependencyGraphComponent;
        private RefactorComponent refactorComponent;
        private RuntimeComponent runtimeComponent;
        private AdvancedScansComponent advancedScansComponent;
        private MigrationStrategyComponent strategyComponent;
        private SupportComponent supportComponent;

        public MigrationToolWindowContent(Project project) {
            this.project = project;
            this.analysisService = new MigrationAnalysisService();
            this.advancedScanningService = new AdvancedScanningService();
            this.store = new CentralMigrationAnalysisStore();
            this.contentPanel = new JPanel(new BorderLayout());
            initializeContent();
        }

        private void initializeContent() {
            // Toolbar with action buttons
            JPanel toolbarPanel = createToolbar();

            JTabbedPane tabbedPane = new JTabbedPane();

            // Dashboard tab
            dashboardComponent = new DashboardComponent(project, advancedScanningService, this::handleAnalyzeProject);
            tabbedPane.addTab("Dashboard", dashboardComponent.getPanel());

            // Dependencies tab
            dependenciesComponent = new DependenciesTableComponent(project);
            tabbedPane.addTab("Dependencies", dependenciesComponent.getPanel());

            // Dependency Graph tab
            dependencyGraphComponent = new DependencyGraphComponent(project);
            tabbedPane.addTab("Dependency Graph", dependencyGraphComponent.getPanel());

            // Migration Strategy tab
            strategyComponent = new MigrationStrategyComponent(project);
            tabbedPane.addTab("Migration Strategy", strategyComponent.getPanel());

            // Refactor tab (Premium)
            refactorComponent = new RefactorComponent(project);
            refactorComponent.setRecipes(List.of(
                    adrianmikula.jakartamigration.coderefactoring.domain.Recipe.jakartaNamespaceRecipe(),
                    adrianmikula.jakartamigration.coderefactoring.domain.Recipe.persistenceXmlRecipe(),
                    adrianmikula.jakartamigration.coderefactoring.domain.Recipe.webXmlRecipe(),
                    adrianmikula.jakartamigration.coderefactoring.domain.Recipe.jpaRecipe(),
                    adrianmikula.jakartamigration.coderefactoring.domain.Recipe.beanValidationRecipe(),
                    adrianmikula.jakartamigration.coderefactoring.domain.Recipe.servletRecipe(),
                    adrianmikula.jakartamigration.coderefactoring.domain.Recipe.cdiRecipe(),
                    adrianmikula.jakartamigration.coderefactoring.domain.Recipe.restRecipe(),
                    adrianmikula.jakartamigration.coderefactoring.domain.Recipe.soapRecipe()));
            tabbedPane.addTab("Refactor", refactorComponent.getPanel());

            // Runtime tab (Premium)
            runtimeComponent = new RuntimeComponent(project);
            tabbedPane.addTab("Runtime", runtimeComponent.getPanel());

            // Advanced Scans tab (Premium) - JPA, Bean Validation, Servlet/JSP
            advancedScansComponent = new AdvancedScansComponent(project, advancedScanningService);
            advancedScansComponent.addScanCompletionListener(() -> {
                if (dashboardComponent != null) {
                    dashboardComponent.updateAdvancedScanCounts();
                }
            });
            tabbedPane.addTab("Advanced Scans", advancedScansComponent.getPanel());

            // Support tab - Links to GitHub, LinkedIn, Sponsor pages
            supportComponent = new SupportComponent(project);
            tabbedPane.addTab("Support", supportComponent.getPanel());

            // Add tab change listener to update dashboard scan counts when switching to
            // dashboard tab
            tabbedPane.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    int selectedIndex = tabbedPane.getSelectedIndex();
                    // Dashboard is the first tab (index 0)
                    if (selectedIndex == 0 && dashboardComponent != null) {
                        dashboardComponent.updateAdvancedScanCounts();
                    }
                }
            });

            // Load initial state (empty - wait for user to analyze)
            loadInitialState();

            contentPanel.add(toolbarPanel, BorderLayout.NORTH);
            contentPanel.add(tabbedPane, BorderLayout.CENTER);
        }

        private JPanel createToolbar() {
            JPanel toolbarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
            toolbarPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            // Analyze Project button
            JButton analyzeButton = new JButton("▶ Analyze Project");
            analyzeButton.setToolTipText("Run migration analysis on the current project");
            analyzeButton.addActionListener(this::handleAnalyzeProject);
            toolbarPanel.add(analyzeButton);

            // Refresh button
            JButton refreshButton = new JButton("↻ Refresh");
            refreshButton.setToolTipText("Refresh analysis results");
            refreshButton.addActionListener(e -> handleAnalyzeProject(null));
            toolbarPanel.add(refreshButton);

            // Status indicator
            JLabel statusLabel = new JLabel("Ready");
            statusLabel.setForeground(Color.GRAY);
            toolbarPanel.add(statusLabel);

            // Add glue to push content to the left
            toolbarPanel.add(Box.createHorizontalGlue());

            return toolbarPanel;
        }

        /**
         * Handle analyze project action - triggers analysis using migration-core
         * library
         */
        private void handleAnalyzeProject(java.awt.event.ActionEvent e) {
            String projectPathStr = project.getBasePath();
            if (projectPathStr == null) {
                projectPathStr = project.getProjectFilePath();
            }

            if (projectPathStr == null) {
                Messages.showWarningDialog(project, "Cannot determine project path. Please open a project first.",
                        "Analysis Failed");
                return;
            }

            final Path projectPath = Path.of(projectPathStr);

            // Show loading state
            dashboardComponent.setStatus(MigrationStatus.IN_PROGRESS);
            dashboardComponent.setReadinessScore(-1); // Show loading

            // Run analysis directly using the migration-core library
            CompletableFuture.supplyAsync(() -> analysisService.analyzeProject(projectPath))
                    .thenAccept(report -> {
                        SwingUtilities.invokeLater(() -> {
                            if (report != null && report.dependencyGraph() != null &&
                                    !report.dependencyGraph().getNodes().isEmpty()) {
                                // Save to database
                                store.saveAnalysisReport(projectPath, report, false);
                                // Update dashboard with real data
                                updateDashboardFromReport(report);
                                int depsCount = report.dependencyGraph().getNodes().size();
                                Messages.showInfoMessage(project, "Analysis complete! " +
                                        depsCount +
                                        " dependencies analyzed.", "Analysis Complete");
                            } else {
                                // No dependencies found - project is ready
                                showEmptyResultsState();
                                Messages.showInfoMessage(project,
                                        "Analysis complete. No Jakarta migration issues found.",
                                        "Analysis Complete");
                            }
                        });
                    })
                    .exceptionally(ex -> {
                        SwingUtilities.invokeLater(() -> {
                            dashboardComponent.setStatus(MigrationStatus.FAILED);
                            Messages.showWarningDialog(project,
                                    "Analysis failed: " + ex.getMessage(),
                                    "Analysis Failed");
                        });
                        return null;
                    });
        }

        /**
         * Show empty results state when no issues are found
         */
        private void showEmptyResultsState() {
            MigrationDashboard dashboard = new MigrationDashboard();
            dashboard.setReadinessScore(100);
            dashboard.setStatus(MigrationStatus.READY);
            dashboard.setLastAnalyzed(Instant.now());

            DependencySummary summary = new DependencySummary();
            summary.setTotalDependencies(0);
            summary.setAffectedDependencies(0);
            summary.setBlockerDependencies(0);
            summary.setMigrableDependencies(0);
            dashboard.setDependencySummary(summary);

            dashboardComponent.updateDashboard(dashboard);
            dependenciesComponent.setDependencies(new ArrayList<>());

            // Clear the dependency graph
            dependencyGraphComponent.updateGraphFromDependencyGraph(new DependencyGraph());
        }

        /**
         * Update dashboard from migration analysis report
         */
        private void updateDashboardFromReport(DependencyAnalysisReport report) {
            LOG.info("updateDashboardFromReport: processing report with " +
                    (report == null ? "null" : "non-null") + " report");

            if (report == null || report.dependencyGraph() == null) {
                LOG.info("updateDashboardFromReport: null report or dependencyGraph, showing empty results");
                showEmptyResultsState();
                return;
            }

            DependencyGraph graph = report.dependencyGraph();
            Set<Artifact> nodes = graph.getNodes();

            LOG.info("updateDashboardFromReport: found " + (nodes == null ? "null" : nodes.size())
                    + " dependency nodes");

            if (nodes == null || nodes.isEmpty()) {
                // No dependencies found - project is ready
                showEmptyResultsState();
                return;
            }

            // Build a set of root packages to determine organizational namespace
            Set<String> rootGroupIds = new HashSet<>();
            Set<Artifact> targetNodes = new HashSet<>();
            for (adrianmikula.jakartamigration.dependencyanalysis.domain.Dependency edge : graph.getEdges()) {
                targetNodes.add(edge.to());
            }

            for (Artifact node : nodes) {
                if (!targetNodes.contains(node)) {
                    rootGroupIds.add(node.groupId());
                }
            }

            // Convert Artifact objects to DependencyInfo
            List<DependencyInfo> deps = new ArrayList<>();

            // Build a map of recommendations by current artifact
            Map<String, Artifact> recommendationMap = new HashMap<>();
            if (report.recommendations() != null) {
                for (VersionRecommendation rec : report.recommendations()) {
                    if (rec.currentArtifact() != null) {
                        recommendationMap.put(
                                rec.currentArtifact().groupId() + ":" + rec.currentArtifact().artifactId(),
                                rec.recommendedArtifact());
                    }
                }
            }

            // Build a set of org namespace patterns from the project
            Set<String> orgPatterns = new HashSet<>();
            for (String root : rootGroupIds) {
                orgPatterns.add(root);
                orgPatterns.add(root + ".*");
            }

            for (Artifact artifact : nodes) {
                DependencyInfo info = new DependencyInfo();
                info.setArtifactId(artifact.artifactId());
                info.setGroupId(artifact.groupId());
                info.setCurrentVersion(artifact.version());
                info.setTransitive(artifact.transitive());

                // Check if it's an organizational artifact
                boolean isOrg = false;
                for (String rootGroup : rootGroupIds) {
                    if (artifact.groupId().startsWith(rootGroup) || rootGroup.startsWith(artifact.groupId())) {
                        isOrg = true;
                        break;
                    }
                }
                info.setOrganizational(isOrg);

                // Check if there's a recommendation for this artifact
                String artifactKey = artifact.groupId() + ":" + artifact.artifactId();
                Artifact recommended = recommendationMap.get(artifactKey);
                if (recommended != null) {
                    info.setRecommendedVersion(recommended.version());
                }

                // Determine migration status based on namespace from the report's namespace map
                Namespace namespace = report.namespaceMap() != null
                        ? report.namespaceMap().get(artifact)
                        : Namespace.UNKNOWN;

                if (namespace == Namespace.JAKARTA) {
                    info.setMigrationStatus(DependencyMigrationStatus.COMPATIBLE);
                } else if (namespace == Namespace.JAVAX) {
                    // Check if there's a Jakarta equivalent
                    info.setMigrationStatus(DependencyMigrationStatus.NEEDS_UPGRADE);
                    info.setTransitive(false);
                } else {
                    info.setMigrationStatus(DependencyMigrationStatus.NO_JAKARTA_VERSION);
                }

                // Collect org namespace patterns from JAVAX dependencies
                if (namespace == Namespace.JAVAX) {
                    String groupId = artifact.groupId();
                    // Extract org pattern from groupId (e.g., "javax.servlet" -> "javax.*")
                    int lastDot = groupId.lastIndexOf('.');
                    if (lastDot > 0) {
                        orgPatterns.add(groupId.substring(0, lastDot) + ".*");
                    } else {
                        orgPatterns.add(groupId);
                    }
                }

                deps.add(info);
            }

            // Build status map for the dependency graph
            Map<String, DependencyMigrationStatus> statusMap = new HashMap<>();
            for (DependencyInfo info : deps) {
                statusMap.put(info.getGroupId() + ":" + info.getArtifactId(), info.getMigrationStatus());
            }

            // Update the dependency graph with real relationships and status
            dependencyGraphComponent.updateGraphFromDependencyGraph(report.dependencyGraph(), statusMap);

            // Set org namespace patterns for the dependency graph component
            dependencyGraphComponent.setOrgNamespacePatterns(orgPatterns);

            LOG.info("updateDashboardFromReport: converted " + deps.size() + " dependencies, updating UI");

            // Calculate metrics
            long blockers = deps.stream().filter(DependencyInfo::isBlocker).count();
            long migrable = deps.stream().filter(d -> d.getRecommendedVersion() != null).count();

            int score = calculateReadinessScore(deps);

            // Update dashboard
            MigrationDashboard dashboard = new MigrationDashboard();
            dashboard.setReadinessScore(score);
            dashboard.setStatus(blockers > 0 ? MigrationStatus.HAS_BLOCKERS : MigrationStatus.READY);
            dashboard.setLastAnalyzed(Instant.now());

            DependencySummary summary = new DependencySummary();
            summary.setTotalDependencies(deps.size());
            summary.setAffectedDependencies(deps.size());
            summary.setBlockerDependencies((int) blockers);
            summary.setMigrableDependencies((int) migrable);
            dashboard.setDependencySummary(summary);

            dashboardComponent.updateDashboard(dashboard);
            dependenciesComponent.setDependencies(deps);

            // Also update the dependency graph with real relationships
            if (report.dependencyGraph() != null) {
                dependencyGraphComponent.updateGraphFromDependencyGraph(report.dependencyGraph());
            }
        }

        private int calculateReadinessScore(List<DependencyInfo> deps) {
            if (deps == null || deps.isEmpty()) {
                return 100;
            }

            long compatible = deps.stream()
                    .filter(d -> d.getMigrationStatus() == DependencyMigrationStatus.COMPATIBLE)
                    .count();
            long hasVersion = deps.stream()
                    .filter(d -> d.getRecommendedVersion() != null)
                    .count();

            return (int) ((compatible + hasVersion * 0.7) * 100 / deps.size());
        }

        private void loadInitialState() {
            String projectPathStr = project.getBasePath();
            if (projectPathStr == null) {
                projectPathStr = project.getProjectFilePath();
            }
            if (projectPathStr != null) {
                Path projectPath = Path.of(projectPathStr);
                DependencyAnalysisReport report = store.getLatestAnalysisReport(projectPath);
                if (report != null) {
                    updateDashboardFromReport(report);
                    return;
                }
            }

            // Set initial empty state - wait for user to analyze
            MigrationDashboard dashboard = new MigrationDashboard();
            dashboard.setReadinessScore(0);
            dashboard.setStatus(MigrationStatus.NOT_ANALYZED);
            dashboard.setLastAnalyzed(null);

            DependencySummary summary = new DependencySummary();
            summary.setTotalDependencies(0);
            summary.setAffectedDependencies(0);
            summary.setBlockerDependencies(0);
            summary.setMigrableDependencies(0);
            dashboard.setDependencySummary(summary);

            dashboardComponent.updateDashboard(dashboard);
            dependenciesComponent.setDependencies(new ArrayList<>());
        }

        /**
         * Refresh data from migration-core library
         */
        public void refreshFromLibrary() {
            handleAnalyzeProject(null);
        }

        public JPanel getContentPanel() {
            return contentPanel;
        }
    }
}
