package adrianmikula.jakartamigration.intellij.ui;

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
            JTabbedPane tabbedPane = new JTabbedPane();

            // Dashboard tab
            dashboardComponent = new DashboardComponent(project);
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

            // Load initial data
            loadInitialData();

            contentPanel.add(tabbedPane, BorderLayout.CENTER);
        }

        private void loadInitialData() {
            // Load mock data for MVP demonstration
            loadMockDashboardData();
            loadMockDependencies();
        }

        private void loadMockDashboardData() {
            // Create a mock dashboard for demonstration
            MigrationDashboard dashboard = new MigrationDashboard();
            dashboard.setReadinessScore(65);
            dashboard.setStatus(MigrationStatus.HAS_BLOCKERS);
            dashboard.setLastAnalyzed(Instant.now());

            DependencySummary summary = new DependencySummary();
            summary.setTotalDependencies(42);
            summary.setAffectedDependencies(18);
            summary.setBlockerDependencies(3);
            summary.setMigrableDependencies(15);
            dashboard.setDependencySummary(summary);

            dashboardComponent.updateDashboard(dashboard);
        }

        private void loadMockDependencies() {
            List<DependencyInfo> dependencies = new ArrayList<>();

            // Blockers
            dependencies.add(new DependencyInfo(
                "javax.xml.bind", "jaxb-api", "2.3.1", null,
                DependencyMigrationStatus.NO_JAKARTA_VERSION, true,
                adrianmikula.jakartamigration.intellij.model.RiskLevel.CRITICAL,
                "No Jakarta equivalent - requires alternative"
            ));

            dependencies.add(new DependencyInfo(
                "javax.activation", "javax.activation-api", "1.2.0", "jakarta.activation:jakarta.activation-api:2.3.1",
                DependencyMigrationStatus.NEEDS_UPGRADE, true,
                adrianmikula.jakartamigration.intellij.model.RiskLevel.HIGH,
                "Upgrade to Jakarta Activation 2.3"
            ));

            dependencies.add(new DependencyInfo(
                "org.glassfish.jaxb", "jaxb-runtime", "2.3.1", "org.glassfish.jaxb:jaxb-runtime:3.0.2",
                DependencyMigrationStatus.NEEDS_UPGRADE, true,
                adrianmikula.jakartamigration.intellij.model.RiskLevel.MEDIUM,
                "Update to Jakarta XML Binding 3.0"
            ));

            // Needs upgrade
            dependencies.add(new DependencyInfo(
                "org.springframework", "spring-beans", "5.3.27", "6.0.9",
                DependencyMigrationStatus.NEEDS_UPGRADE, false,
                adrianmikula.jakartamigration.intellij.model.RiskLevel.HIGH,
                "Required for Spring Framework 6.0 migration"
            ));

            dependencies.add(new DependencyInfo(
                "org.springframework", "spring-core", "5.3.27", "6.0.9",
                DependencyMigrationStatus.NEEDS_UPGRADE, false,
                adrianmikula.jakartamigration.intellij.model.RiskLevel.HIGH,
                "Required for Spring Framework 6.0 migration"
            ));

            dependencies.add(new DependencyInfo(
                "org.springframework", "spring-web", "5.3.27", "6.0.9",
                DependencyMigrationStatus.NEEDS_UPGRADE, false,
                adrianmikula.jakartamigration.intellij.model.RiskLevel.MEDIUM,
                "Update for Jakarta Servlet 5.0 compatibility"
            ));

            dependencies.add(new DependencyInfo(
                "org.hibernate", "hibernate-core", "5.6.15.Final", "6.2.0.Final",
                DependencyMigrationStatus.NEEDS_UPGRADE, false,
                adrianmikula.jakartamigration.intellij.model.RiskLevel.CRITICAL,
                "Major version upgrade - significant changes"
            ));

            // Compatible
            dependencies.add(new DependencyInfo(
                "jakarta.servlet", "jakarta.servlet-api", "5.0.0", null,
                DependencyMigrationStatus.COMPATIBLE, false,
                adrianmikula.jakartamigration.intellij.model.RiskLevel.LOW,
                "Already using Jakarta EE 9+ compatible version"
            ));

            dependencies.add(new DependencyInfo(
                "jakarta.validation", "jakarta.validation-api", "3.0.2", null,
                DependencyMigrationStatus.COMPATIBLE, false,
                adrianmikula.jakartamigration.intellij.model.RiskLevel.LOW,
                "Already using Jakarta EE compatible version"
            ));

            dependencies.add(new DependencyInfo(
                "org.apache.commons", "commons-lang3", "3.12.0", null,
                DependencyMigrationStatus.COMPATIBLE, false,
                adrianmikula.jakartamigration.intellij.model.RiskLevel.LOW,
                "No javax dependencies"
            ));

            dependenciesComponent.setDependencies(dependencies);
        }

        /**
         * Refresh data from MCP server
         */
        public void refreshFromMcpServer() {
            String projectPath = project.getBasePath();
            if (projectPath == null) {
                projectPath = project.getProjectFilePath();
            }

            if (projectPath == null) {
                Messages.showWarningDialog(project, "Cannot determine project path", "Refresh Failed");
                return;
            }

            // Show loading state
            dashboardComponent.setStatus(MigrationStatus.IN_PROGRESS);

            // Call MCP server asynchronously
            CompletableFuture.supplyAsync(() -> mcpClient.analyzeMigrationImpact(projectPath))
                .thenAccept(response -> {
                    SwingUtilities.invokeLater(() -> {
                        if (response != null && response.getDependencyImpact() != null) {
                            // Update dashboard
                            MigrationDashboard dashboard = new MigrationDashboard();
                            dashboard.setReadinessScore(calculateReadinessScore(response));
                            dashboard.setStatus(determineStatus(response));
                            dashboard.setLastAnalyzed(Instant.now());

                            DependencySummary summary = new DependencySummary();
                            List<DependencyInfo> deps = response.getDependencyImpact().getAffectedDependencies();
                            summary.setTotalDependencies(deps.size());
                            summary.setAffectedDependencies(deps.size());
                            summary.setBlockerDependencies((int) deps.stream().filter(DependencyInfo::isBlocker).count());
                            summary.setMigrableDependencies((int) deps.stream().filter(d -> d.getRecommendedVersion() != null).count());
                            dashboard.setDependencySummary(summary);

                            dashboardComponent.updateDashboard(dashboard);
                            dependenciesComponent.setDependencies(deps);
                        }
                    });
                })
                .exceptionally(ex -> {
                    SwingUtilities.invokeLater(() -> {
                        dashboardComponent.setStatus(MigrationStatus.FAILED);
                        Messages.showErrorDialog(project, "Failed to connect to MCP server: " + ex.getMessage(), "Connection Failed");
                    });
                    return null;
                });
        }

        private int calculateReadinessScore(AnalyzeMigrationImpactResponse response) {
            if (response == null || response.getDependencyImpact() == null) {
                return 0;
            }
            List<DependencyInfo> deps = response.getDependencyImpact().getAffectedDependencies();
            if (deps == null || deps.isEmpty()) {
                return 100;
            }

            long compatible = deps.stream().filter(d -> d.getMigrationStatus() == DependencyMigrationStatus.COMPATIBLE).count();
            long hasVersion = deps.stream().filter(d -> d.getRecommendedVersion() != null).count();

            return (int) ((compatible + hasVersion * 0.7) * 100 / deps.size());
        }

        private MigrationStatus determineStatus(AnalyzeMigrationImpactResponse response) {
            if (response == null || response.getDependencyImpact() == null) {
                return MigrationStatus.NOT_ANALYZED;
            }
            List<DependencyInfo> deps = response.getDependencyImpact().getAffectedDependencies();
            if (deps == null || deps.isEmpty()) {
                return MigrationStatus.READY;
            }

            boolean hasBlockers = deps.stream().anyMatch(DependencyInfo::isBlocker);
            return hasBlockers ? MigrationStatus.HAS_BLOCKERS : MigrationStatus.READY;
        }

        public JPanel getContentPanel() {
            return contentPanel;
        }
    }
}
