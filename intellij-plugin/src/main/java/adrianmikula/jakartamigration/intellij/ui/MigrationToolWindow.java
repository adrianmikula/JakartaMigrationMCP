package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.mcp.AnalyzeMigrationImpactRequest;
import adrianmikula.jakartamigration.intellij.mcp.AnalyzeMigrationImpactResponse;
import adrianmikula.jakartamigration.intellij.mcp.DefaultMcpClientService;
import adrianmikula.jakartamigration.intellij.mcp.McpClientService;
import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus;
import adrianmikula.jakartamigration.intellij.model.DependencySummary;
import adrianmikula.jakartamigration.intellij.model.MigrationDashboard;
import adrianmikula.jakartamigration.intellij.model.MigrationStatus;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Main migration tool window from TypeSpec: plugin-components.tsp
 */
public class MigrationToolWindow implements ToolWindowFactory {
    public static final String TOOL_WINDOW_ID = "JakartaMigrationToolWindow";

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        MigrationToolWindowContent content = new MigrationToolWindowContent(project);
        Content tabContent = ContentFactory.getInstance().createContent(content.getContentPanel(), "Jakarta Migration", false);
        toolWindow.getContentManager().addContent(tabContent);
    }

    public static class MigrationToolWindowContent {
        private final JPanel contentPanel;
        private final Project project;
        private final McpClientService mcpClient;

        // UI Components
        private DashboardComponent dashboardComponent;
        private DependenciesTableComponent dependenciesComponent;
        private DependencyGraphComponent dependencyGraphComponent;
        private MigrationPhasesComponent migrationPhasesComponent;

        public MigrationToolWindowContent(Project project) {
            this.project = project;
            this.mcpClient = new DefaultMcpClientService();
            this.contentPanel = new JPanel(new BorderLayout());
            initializeContent();
        }

        private void initializeContent() {
            // Toolbar with action buttons
            JPanel toolbarPanel = createToolbar();

            JTabbedPane tabbedPane = new JTabbedPane();

            // Dashboard tab
            dashboardComponent = new DashboardComponent(project, this::handleAnalyzeProject);
            tabbedPane.addTab("Dashboard", dashboardComponent.getPanel());

            // Dependencies tab
            dependenciesComponent = new DependenciesTableComponent(project);
            tabbedPane.addTab("Dependencies", dependenciesComponent.getPanel());

            // Dependency Graph tab
            dependencyGraphComponent = new DependencyGraphComponent(project);
            tabbedPane.addTab("Dependency Graph", dependencyGraphComponent.getPanel());

            // Migration Phases tab
            migrationPhasesComponent = new MigrationPhasesComponent(project);
            tabbedPane.addTab("Migration Phases", migrationPhasesComponent.getPanel());

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
         * Handle analyze project action - triggers MCP analysis
         */
        private void handleAnalyzeProject(ActionEvent e) {
            String projectPath = project.getBasePath();
            if (projectPath == null) {
                projectPath = project.getProjectFilePath();
            }

            if (projectPath == null) {
                Messages.showWarningDialog(project, "Cannot determine project path. Please open a project first.", "Analysis Failed");
                return;
            }

            // Show loading state
            dashboardComponent.setStatus(MigrationStatus.IN_PROGRESS);
            dashboardComponent.setReadinessScore(-1); // Show loading

            // Call MCP server asynchronously
            mcpClient.analyzeMigrationImpact(projectPath)
                .thenAccept(response -> {
                    SwingUtilities.invokeLater(() -> {
                        if (response != null && response.getDependencyImpact() != null && 
                            response.getDependencyImpact().getAffectedDependencies() != null &&
                            !response.getDependencyImpact().getAffectedDependencies().isEmpty()) {
                            // Update dashboard with real data
                            updateDashboardFromResponse(response);
                            int depsCount = response.getDependencyImpact().getAffectedDependencies().size();
                            Messages.showInfoMessage(project, "Analysis complete! " + 
                                depsCount + 
                                " dependencies analyzed.", "Analysis Complete");
                        } else {
                            // No affected dependencies - project is ready
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
                            "Could not connect to MCP server. Please ensure the server is running at: " + 
                            mcpClient.getServerUrl() + "\n" +
                            "Error: " + ex.getMessage(), 
                            "Server Unavailable");
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
        }

        /**
         * Update dashboard from MCP response
         */
        private void updateDashboardFromResponse(AnalyzeMigrationImpactResponse response) {
            if (response == null || response.getDependencyImpact() == null) {
                showEmptyResultsState();
                return;
            }

            List<DependencyInfo> deps = response.getDependencyImpact().getAffectedDependencies();
            if (deps == null || deps.isEmpty()) {
                // No affected dependencies - project is ready
                showEmptyResultsState();
                return;
            }

            // Calculate metrics
            long blockers = deps.stream().filter(DependencyInfo::isBlocker).count();
            long migrable = deps.stream().filter(d -> d.getRecommendedVersion() != null).count();
            long compatible = deps.stream()
                .filter(d -> d.getMigrationStatus() == DependencyMigrationStatus.COMPATIBLE)
                .count();

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
         * Refresh data from MCP server
         */
        public void refreshFromMcpServer() {
            handleAnalyzeProject(null);
        }

        public JPanel getContentPanel() {
            return contentPanel;
        }
    }
}
