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
import adrianmikula.jakartamigration.analysis.persistence.SqliteMigrationAnalysisStore;
import adrianmikula.jakartamigration.coderefactoring.service.CodeRefactoringModule;
import adrianmikula.jakartamigration.coderefactoring.service.RecipeService;
import adrianmikula.jakartamigration.intellij.ui.SimplePlatformsTabComponent;
import adrianmikula.jakartamigration.intellij.ui.ComprehensiveReportsTabComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.Desktop;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        private MigrationPhasesComponent migrationPhasesComponent;
        private AdvancedScansComponent advancedScansComponent;
        private SupportComponent supportComponent;
        private McpServerTabComponent mcpServerTabComponent;
        private RefactorTabComponent refactorTabComponent;
        private HistoryTabComponent historyTabComponent;
        private SimplePlatformsTabComponent platformsTabComponent;
        private RuntimeTabComponent runtimeTabComponent;
        private ComprehensiveReportsTabComponent reportsTabComponent;
        private CodeRefactoringModule refactorModule;
        private RecipeService recipeService;
        private SqliteMigrationAnalysisStore projectStore;
        private JTabbedPane tabbedPane;
        private JPanel toolbarPanel;
        private boolean isPremium;

        public MigrationToolWindowContent(Project project) {
            this.project = project;
            this.analysisService = new MigrationAnalysisService();
            this.store = new CentralMigrationAnalysisStore();

            // Initialize project-specific store
            Path projectPath = Paths.get(project.getBasePath());
            this.projectStore = new SqliteMigrationAnalysisStore(projectPath);
            this.refactorModule = new CodeRefactoringModule(this.store, this.projectStore);
            this.recipeService = this.refactorModule.getRecipeService();
            
            // Initialize advanced scanning service with recipe service
            this.advancedScanningService = new AdvancedScanningService(this.recipeService);

            this.contentPanel = new JPanel(new BorderLayout());

            // Check premium status
            this.isPremium = checkPremiumStatus();
            LOG.info("MigrationToolWindowContent: Constructor called, isPremium=" + isPremium);
            LOG.info("MigrationToolWindowContent: System property jakarta.migration.premium=" +
                    System.getProperty("jakarta.migration.premium"));

            initializeContent();
        }

        public JPanel getContentPanel() {
            return contentPanel;
        }

        /**
         * Check if user has premium license.
         * Uses JetBrains LicensingFacade and our own trial system.
         */
        private boolean checkPremiumStatus() {
            // Use the CheckLicense class which handles both JetBrains licensing
            // and our own trial/premium system
            Boolean licensed = adrianmikula.jakartamigration.intellij.license.CheckLicense.isLicensed();
            LOG.info("MigrationToolWindowContent: CheckLicense.isLicensed() = " + licensed);
            LOG.info("MigrationToolWindowContent: License status: " +
                    adrianmikula.jakartamigration.intellij.license.CheckLicense.getLicenseStatusString());
            
            // Handle null case (LicensingFacade not initialized) - default to trial check
            return licensed != null && licensed;
        }

        private void initializeContent() {
            this.isPremium = checkPremiumStatus();
            LOG.info("initializeContent: Starting, isPremium=" + isPremium);
            contentPanel.removeAll();

            // Toolbar with action buttons
            toolbarPanel = createToolbar();

            tabbedPane = new JTabbedPane();

            // Dashboard tab
            dashboardComponent = new DashboardComponent(project, advancedScanningService, this::handleAnalyzeProject);
            tabbedPane.addTab("Dashboard", dashboardComponent.getPanel());

            // Dependencies tab
            dependenciesComponent = new DependenciesTableComponent(project);
            tabbedPane.addTab("Dependencies", dependenciesComponent.getPanel());

            // Dependency Graph tab
            dependencyGraphComponent = new DependencyGraphComponent(project);
            tabbedPane.addTab("Dependency Graph", dependencyGraphComponent.getPanel());

            // Migration Strategy tab - strategy cards with benefits/risks and migration
            // steps
            migrationPhasesComponent = new MigrationPhasesComponent(project);
            tabbedPane.addTab("Migration Strategy", migrationPhasesComponent.getPanel());

            // Advanced Scans tab - Premium only
            if (isPremium) {
                advancedScansComponent = new AdvancedScansComponent(project, advancedScanningService);
                advancedScansComponent.addScanCompletionListener(() -> {
                    if (dashboardComponent != null) {
                        dashboardComponent.updateAdvancedScanCounts();
                    }
                    
                    // Trigger Maven Central lookup after advanced scan completion
                    // This ensures Jakarta version recommendations are up-to-date
                    if (dependenciesComponent != null) {
                        dependenciesComponent.queryMavenCentralForDependencies();
                    }
                });
                tabbedPane.addTab("Advanced Scans ⭐", advancedScansComponent.getPanel());
                LOG.info("initializeContent: Added PREMIUM Advanced Scans tab");
            } else {
                // Add a locked placeholder for non-premium users
                advancedScansComponent = null;
                tabbedPane.addTab("Advanced Scans 🔒", createPremiumPlaceholderPanel(
                        "Advanced Scans",
                        "Unlock advanced scanning features including JPA, Bean Validation, Servlet/JSP, CDI, and more.",
                        "JPA entity scanning",
                        "Comprehensive annotation analysis",
                        "Servlet/JSP detection",
                        "CDI bean discovery",
                        "Transaction API scanning"));
                LOG.info("initializeContent: Added LOCKED Advanced Scans placeholder tab");
            }

            // Support tab - links to GitHub, LinkedIn, sponsor pages
            supportComponent = new SupportComponent(project, v -> refreshPremiumTabs());
            tabbedPane.addTab("Support", supportComponent.getPanel());

            // AI tab - always visible (formerly MCP Server)
            mcpServerTabComponent = new McpServerTabComponent(project);
            tabbedPane.addTab("AI", mcpServerTabComponent.getPanel());

            // Premium tabs - only available for premium users
            LOG.info("initializeContent: Creating tabs, isPremium=" + isPremium);
            if (isPremium) {
                // Refactor tab (Premium)
                refactorTabComponent = new RefactorTabComponent(project, recipeService);
                tabbedPane.addTab("Refactor ⭐", refactorTabComponent.getPanel());
                LOG.info("initializeContent: Added PREMIUM Refactor tab");

                // History tab (Premium) - shows recipe execution history
                historyTabComponent = new HistoryTabComponent(project, recipeService);
                tabbedPane.addTab("History ⭐", historyTabComponent.getPanel());
                LOG.info("initializeContent: Added PREMIUM History tab");

                // Wire refactor tab to auto-refresh history tab after recipe runs
                final HistoryTabComponent historyRef = historyTabComponent;
                refactorTabComponent.setOnRecipeExecuted(() -> SwingUtilities.invokeLater(historyRef::refreshHistory));

                // Platforms tab (Premium)
                platformsTabComponent = new SimplePlatformsTabComponent(project);
                tabbedPane.addTab("Platforms ⭐", platformsTabComponent.getPanel());
                LOG.info("initializeContent: Added PREMIUM Platforms tab");

                // Reports tab (Premium + Experimental features only)
                System.out.println("DEBUG: MigrationToolWindow - About to check experimental features");
                boolean experimentalEnabled = adrianmikula.jakartamigration.intellij.config.FeatureFlags.getInstance().isExperimentalFeaturesEnabled();
                System.out.println("DEBUG: MigrationToolWindow - experimentalEnabled = " + experimentalEnabled);
                if (experimentalEnabled) {
                    reportsTabComponent = new ComprehensiveReportsTabComponent(project);
                    tabbedPane.addTab("Reports 📊 (Experimental)", reportsTabComponent.getPanel());
                    LOG.info("initializeContent: Added PREMIUM+EXPERIMENTAL Reports tab");
                } else {
                    reportsTabComponent = null;
                    LOG.info("initializeContent: Reports tab hidden (experimental features disabled)");
                }

                // Runtime tab (Premium + Experimental features only)
                if (experimentalEnabled) {
                    runtimeTabComponent = new RuntimeTabComponent(project);
                    tabbedPane.addTab("Runtime ⚡ (Experimental)", runtimeTabComponent.getPanel());
                    LOG.info("initializeContent: Added PREMIUM+EXPERIMENTAL Runtime tab");
                } else {
                    runtimeTabComponent = null;
                    LOG.info("initializeContent: Runtime tab hidden (experimental features disabled)");
                    LOG.info("initializeContent: Runtime and Reports tabs hidden (experimental features disabled)");
                }
            } else {
                refactorTabComponent = null;
                historyTabComponent = null;
                platformsTabComponent = null;

                // Non-premium: show locked placeholders for Refactor and History
                tabbedPane.addTab("Refactor 🔒", createPremiumPlaceholderPanel(
                        "Refactor Tab",
                        "Apply OpenRewrite recipes with one-click refactoring",
                        "One-click code refactoring",
                        "Automatic migration fixes"));
                LOG.info("initializeContent: Added LOCKED Refactor placeholder tab");

                // Platforms tab (Premium)
                tabbedPane.addTab("Platforms 🔒", createPremiumPlaceholderPanel(
                        "Platforms Tab",
                        "Analyze and validate platform compatibility",
                        "Platform detection",
                        "Compatibility analysis"));
                LOG.info("initializeContent: Added LOCKED Platforms placeholder tab");

                // Runtime tab (Beta) - only show for premium users with experimental features enabled
                System.out.println("DEBUG: MigrationToolWindow (Community) - Runtime tab is premium only");
                LOG.info("initializeContent: Runtime tab hidden (premium feature only)");

                // Reports tab (Experimental) - only show for premium users with experimental features enabled
                System.out.println("DEBUG: MigrationToolWindow (Community) - Reports tab is premium only");
                LOG.info("initializeContent: Reports tab hidden (premium feature only)");

                tabbedPane.addTab("History 🔒", createPremiumPlaceholderPanel(
                        "History Tab",
                        "View history of all code changes made via the plugin",
                        "Track migration progress",
                        "Undo reversible changes"));
                LOG.info("initializeContent: Added LOCKED History placeholder tab");
            }

            // Load initial state (empty - wait for user to analyze)
            loadInitialState();

            contentPanel.add(toolbarPanel, BorderLayout.NORTH);
            contentPanel.add(tabbedPane, BorderLayout.CENTER);

            contentPanel.revalidate();
            contentPanel.repaint();
        }

        /**
         * Refreshes tabs when experimental features flag changes
         */
        public void refreshExperimentalTabs() {
            System.out.println("DEBUG: MigrationToolWindow.refreshExperimentalTabs() called");
            boolean experimentalEnabled = adrianmikula.jakartamigration.intellij.config.FeatureFlags.getInstance().isExperimentalFeaturesEnabled();
            System.out.println("DEBUG: MigrationToolWindow - experimentalEnabled = " + experimentalEnabled);
            
            // Refresh Reports tab
            if (reportsTabComponent != null && experimentalEnabled) {
                reportsTabComponent = new ComprehensiveReportsTabComponent(project);
                tabbedPane.setComponentAt(1, reportsTabComponent.getPanel());
                tabbedPane.setTitleAt(1, "Reports 📊 (Experimental)");
                LOG.info("refreshExperimentalTabs: Reports tab added");
            } else if (reportsTabComponent != null && !experimentalEnabled) {
                tabbedPane.removeTabAt(1);
                LOG.info("refreshExperimentalTabs: Reports tab removed");
            }
            
            // Refresh Runtime tab  
            if (runtimeTabComponent != null && experimentalEnabled) {
                runtimeTabComponent = new RuntimeTabComponent(project);
                tabbedPane.setComponentAt(2, runtimeTabComponent.getPanel());
                tabbedPane.setTitleAt(2, "Runtime ⚡ (Experimental)");
                LOG.info("refreshExperimentalTabs: Runtime tab added");
            } else if (runtimeTabComponent != null && !experimentalEnabled) {
                tabbedPane.removeTabAt(2);
                LOG.info("refreshExperimentalTabs: Runtime tab removed");
            }
            
            contentPanel.revalidate();
            contentPanel.repaint();
        }

        public void rebuildUI() {
            System.out.println("DEBUG: rebuildUI() called");
            SwingUtilities.invokeLater(() -> {
                int selectedIndex = tabbedPane != null ? tabbedPane.getSelectedIndex() : -1;
                String selectedTitle = (selectedIndex != -1 && selectedIndex < tabbedPane.getTabCount())
                        ? tabbedPane.getTitleAt(selectedIndex)
                        : null;

                System.out.println("DEBUG: rebuildUI() - calling initializeContent(), isPremium was: " + isPremium);
                initializeContent();

                if (selectedTitle != null) {
                    // Try to restore by title since indexes might change (locked vs unlocked)
                    // Remove icons/stars for comparison
                    String baseTitle = selectedTitle.replace(" ⭐", "").replace(" 🔒", "");
                    for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                        String currentTitle = tabbedPane.getTitleAt(i).replace(" ⭐", "").replace(" 🔒", "");
                        if (currentTitle.equals(baseTitle)) {
                            tabbedPane.setSelectedIndex(i);
                            break;
                        }
                    }
                }
            });
        }

        private JPanel createToolbar() {
            toolbarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
            toolbarPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            // Analyze Project button
            JButton analyzeButton = new JButton("▶ Analyze Project");
            analyzeButton.setToolTipText("Run migration analysis on the current project");
            analyzeButton.addActionListener(this::handleAnalyzeProject);
            System.out.println("DEBUG: Analyze button created, enabled: " + analyzeButton.isEnabled());
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

            // Add license status / upgrade button
            addLicenseButton(toolbarPanel);

            return toolbarPanel;
        }

        /**
         * Creates a premium placeholder panel for locked features.
         */
        private JPanel createPremiumPlaceholderPanel(String featureName, String description,
                String... features) {
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            // Main content
            JPanel contentPanel = new JPanel();
            contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

            // Lock icon and title
            JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JLabel lockIcon = new JLabel("🔒");
            lockIcon.setFont(new Font(lockIcon.getFont().getName(), Font.PLAIN, 24));
            JLabel titleLabel = new JLabel(featureName + " - Premium Feature");
            titleLabel.setFont(new Font(titleLabel.getFont().getName(), Font.BOLD, 18));
            titlePanel.add(lockIcon);
            titlePanel.add(titleLabel);

            // Description
            JLabel descLabel = new JLabel("<html>" + description + "</html>");
            descLabel.setForeground(Color.GRAY);

            // Features list
            JPanel featuresPanel = new JPanel();
            featuresPanel.setLayout(new BoxLayout(featuresPanel, BoxLayout.Y_AXIS));
            featuresPanel.add(Box.createVerticalStrut(10));
            JLabel featuresTitle = new JLabel("Premium features include:");
            featuresTitle.setFont(new Font(featuresTitle.getFont().getName(), Font.BOLD, 14));
            featuresPanel.add(featuresTitle);
            featuresPanel.add(Box.createVerticalStrut(5));

            for (String feature : features) {
                JLabel featureLabel = new JLabel("• " + feature);
                featuresPanel.add(featureLabel);
            }

            // Buttons
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

            JButton upgradeButton = new JButton("⬆ Upgrade to Premium");
            upgradeButton.setFont(upgradeButton.getFont().deriveFont(Font.BOLD));
            upgradeButton.addActionListener(e -> openMarketplace());

            JButton trialButton = new JButton("Start Free Trial");
            trialButton.addActionListener(e -> startTrial());

            buttonPanel.add(upgradeButton);
            buttonPanel.add(trialButton);

            // Add all to content
            contentPanel.add(titlePanel);
            contentPanel.add(Box.createVerticalStrut(10));
            contentPanel.add(descLabel);
            contentPanel.add(featuresPanel);
            contentPanel.add(buttonPanel);

            // Center the content
            JPanel centerPanel = new JPanel(new GridBagLayout());
            centerPanel.add(contentPanel);

            panel.add(centerPanel, BorderLayout.CENTER);

            return panel;
        }

        /**
         * Add license status indicator and upgrade button to toolbar.
         */
        private void addLicenseButton(JPanel toolbarPanel) {
            // Check if premium (for now, check system property)
            boolean isPremium = "true".equals(System.getProperty("jakarta.migration.premium"));
            LOG.info("addLicenseButton: Called, system property premium=" +
                    System.getProperty("jakarta.migration.premium") + ", local isPremium=" + isPremium);

            if (isPremium) {
                // Show premium badge
                JLabel premiumBadge = new JLabel("⭐ PREMIUM");
                premiumBadge.setForeground(new Color(255, 215, 0)); // Gold color
                premiumBadge.setToolTipText("Premium license active");
                toolbarPanel.add(premiumBadge);
            } else {
                // Show upgrade button
                JButton upgradeButton = new JButton("⬆ Upgrade to Premium");
                upgradeButton.setToolTipText("Get premium features: Auto-fixes, one-click refactoring, binary fixes");
                upgradeButton.addActionListener(e -> openMarketplace());
                toolbarPanel.add(upgradeButton);

                // Show trial link
                JButton trialButton = new JButton("Start Free Trial");
                trialButton.setToolTipText("Start a 7-day free trial");
                trialButton.addActionListener(e -> startTrial());
                toolbarPanel.add(trialButton);
            }
        }

        /**
         * Open JetBrains Marketplace to purchase/upgrade.
         */
        private void openMarketplace() {
            try {
                Desktop.getDesktop().browse(new URI("https://plugins.jetbrains.com/plugin/30093-jakarta-migration"));
            } catch (Exception ex) {
                LOG.warn("Failed to open marketplace URL", ex);
                Messages.showInfoMessage(
                        "Please visit: https://plugins.jetbrains.com/plugin/30093-jakarta-migration",
                        "Upgrade to Premium");
            }
        }

        /**
         * Start a free trial (stores in system properties for demo).
         */
        private void startTrial() {
            LOG.info("startTrial: User clicked Start Free Trial button");

            int result = Messages.showYesNoDialog(project,
                    "Start a 7-day free trial of Premium features?\n\n" +
                            "Premium features include:\n" +
                            "• Auto-fixes for migration issues\n" +
                            "• One-click refactoring\n" +
                            "• Binary fixes for JAR files\n" +
                            "• Advanced dependency analysis",
                    "Start Free Trial",
                    Messages.getQuestionIcon());

            if (result == Messages.YES) {
                // Set system property to enable premium (in production, use proper license
                // storage)
                System.setProperty("jakarta.migration.premium", "true");
                System.setProperty("jakarta.migration.trial.end",
                        String.valueOf(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000));

                // Update SupportComponent premium status
                SupportComponent.setPremiumActive(true);

                // Clear license cache to force fresh check
                adrianmikula.jakartamigration.intellij.license.CheckLicense.clearCache();

                LOG.info("startTrial: System properties set - premium=true, trial.end=" +
                        System.getProperty("jakarta.migration.trial.end"));
                LOG.info("startTrial: Calling refreshPremiumUI() to update the UI dynamically");

                // Refresh the UI to show premium features
                rebuildUI();

                Messages.showInfoMessage(
                        "Trial started! You now have 7 days of Premium access.\n\n" +
                                "Premium features are now available!",
                        "Trial Activated");

                LOG.info("startTrial: UI has been refreshed successfully!");
            }
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
            dashboard.setStatus(MigrationStatus.READY);
            dashboard.setLastAnalyzed(Instant.now());

            DependencySummary summary = new DependencySummary();
            summary.setTotalDependencies(0);
            summary.setAffectedDependencies(0);
            summary.setBlockerDependencies(0);
            summary.setMigrableDependencies(0);
            summary.setNoJakartaSupportCount(0);
            summary.setTransitiveDependencies(0);
            dashboard.setDependencySummary(summary);

            // Update dashboard components with new data
            dashboardComponent.setDashboard(dashboard);
            dependenciesComponent.setDependencies(new ArrayList<>());
            migrationPhasesComponent.setDependencies(new ArrayList<>());

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
                    info.setRecommendedGroupId(recommended.groupId());
                    info.setRecommendedArtifactCoordinates(recommended.groupId() + ":" + recommended.artifactId());
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
            long noJakartaSupport = deps.stream()
                    .filter(d -> d.getMigrationStatus() == DependencyMigrationStatus.NO_JAKARTA_VERSION)
                    .count();
            long transitive = deps.stream().filter(d -> d.isTransitive()).count();

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
            summary.setNoJakartaSupportCount((int) noJakartaSupport);
            summary.setTransitiveDependencies((int) transitive);
            dashboard.setDependencySummary(summary);

            // Update dashboard components with new data
            dashboardComponent.setDashboard(dashboard);
            dependenciesComponent.setDependencies(deps);
            migrationPhasesComponent.setDependencies(deps);
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
            dashboard.setStatus(MigrationStatus.NOT_ANALYZED);
            dashboard.setLastAnalyzed(null);

            DependencySummary summary = new DependencySummary();
            summary.setTotalDependencies(0);
            summary.setAffectedDependencies(0);
            summary.setBlockerDependencies(0);
            summary.setMigrableDependencies(0);
            dashboard.setDependencySummary(summary);

            // Update dashboard components with new data
            dashboardComponent.setDashboard(dashboard);
            dependenciesComponent.setDependencies(new ArrayList<>());
            migrationPhasesComponent.setDependencies(new ArrayList<>());
        }

        /**
         * Refresh data from migration-core library
         */
        public void refreshFromLibrary() {
            handleAnalyzeProject(null);
        }
        
        /**
         * Refresh premium tabs when trial is activated or license status changes
         */
        public void refreshPremiumTabs() {
            System.out.println("DEBUG: refreshPremiumTabs() called");
            SwingUtilities.invokeLater(() -> {
                // Note: SimplePlatformsTabComponent doesn't need manual refresh
                // It handles state internally
                System.out.println("DEBUG: Platforms tab uses simplified component - no manual refresh needed");
                // Note: AdvancedScansComponent and ComprehensiveReportsTabComponent 
                // don't have refreshUI() method, so we'll skip them for now
            });
        }
        
        /**
         * Public method to test platforms tab refresh
         * Note: Simplified version doesn't need manual refresh
         */
        public void testPlatformsTabRefresh() {
            System.out.println("DEBUG: testPlatformsTabRefresh() called - simplified component handles refresh internally");
            // SimplePlatformsTabComponent handles state internally
        }
    }
}
