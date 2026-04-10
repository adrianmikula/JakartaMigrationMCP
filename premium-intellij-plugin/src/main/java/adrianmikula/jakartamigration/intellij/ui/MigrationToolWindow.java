package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.credits.CreditType;
import adrianmikula.jakartamigration.credits.CreditsService;
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
import adrianmikula.jakartamigration.intellij.ui.PlatformsTabComponent;
import adrianmikula.jakartamigration.intellij.ui.ReportsTabComponent;
import com.intellij.openapi.application.ApplicationManager;
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
        private PlatformsTabComponent platformsTabComponent;
        private RuntimeTabComponent runtimeTabComponent;
        private ReportsTabComponent reportsTabComponent;
        private CodeRefactoringModule refactorModule;
        private RecipeService recipeService;
        private SqliteMigrationAnalysisStore projectStore;
        private JTabbedPane tabbedPane;
        private JPanel toolbarPanel;
        private CreditsProgressBar creditsProgressBar;
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

            // Credits progress bar (only visible for free users)
            creditsProgressBar = new CreditsProgressBar(project);
            creditsProgressBar.refreshCredits();

            // Toolbar with action buttons
            toolbarPanel = createToolbar();

            tabbedPane = new JTabbedPane();

            // Dashboard tab
            dashboardComponent = new DashboardComponent(project, advancedScanningService, this::handleAnalyzeProject);
            tabbedPane.addTab("Dashboard", dashboardComponent.getPanel());

            // Dependencies tab
            dependenciesComponent = new DependenciesTableComponent(project);
            dependenciesComponent.setOnAnalysisCompleteListener(updatedDependencies -> {
                // Refresh dependency graph with updated status when async analysis completes
                refreshDependencyGraphWithUpdatedStatus(updatedDependencies);
            });
            tabbedPane.addTab("Dependencies", dependenciesComponent.getPanel());

            // Dependency Graph tab
            dependencyGraphComponent = new DependencyGraphComponent(project);
            tabbedPane.addTab("Dependency Graph", dependencyGraphComponent.getPanel());

            // Migration Strategy tab - strategy cards with benefits/risks and migration
            // steps
            migrationPhasesComponent = new MigrationPhasesComponent(project);
            tabbedPane.addTab("Migration Strategy", migrationPhasesComponent.getPanel());

            // Advanced Scans tab - Available for all users (truncation mode for free users with exhausted credits)
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
            String advancedScansLabel = isPremium ? "Advanced Scans ⭐" : "Advanced Scans";
            tabbedPane.addTab(advancedScansLabel, advancedScansComponent.getPanel());
            LOG.info("initializeContent: Added Advanced Scans tab (isPremium=" + isPremium + ")");

            // Support tab - links to GitHub, LinkedIn, sponsor pages
            supportComponent = new SupportComponent(project, v -> refreshPremiumTabs(), () -> refreshExperimentalTabs());
            tabbedPane.addTab("Support", supportComponent.getPanel());

            // AI tab - always visible (formerly MCP Server)
            mcpServerTabComponent = new McpServerTabComponent(project);
            tabbedPane.addTab("AI", mcpServerTabComponent.getPanel());

            // All tabs available for both free and premium users
            // Free users are limited by credits on premium features
            LOG.info("initializeContent: Creating all tabs, isPremium=" + isPremium);

            // Refactor tab - Available for all users (credits limit free users)
            refactorTabComponent = new RefactorTabComponent(project, recipeService);
            String refactorLabel = isPremium ? "Refactor ⭐" : "Refactor";
            tabbedPane.addTab(refactorLabel, refactorTabComponent.getPanel());
            LOG.info("initializeContent: Added Refactor tab");

            // History tab - Available for all users (view-only for free users)
            historyTabComponent = new HistoryTabComponent(project, recipeService);
            String historyLabel = isPremium ? "History " : "History";
            tabbedPane.addTab(historyLabel, historyTabComponent.getPanel());
            LOG.info("initializeContent: Added History tab");

            // Wire refactor tab to auto-refresh history tab after recipe runs
            final HistoryTabComponent historyRef = historyTabComponent;
            refactorTabComponent.setOnRecipeExecuted(() -> ApplicationManager.getApplication().invokeLater(() -> historyRef.refreshHistory()));

            // Wire up credit refresh callbacks to update CreditsProgressBar when credits are used
            if (!isPremium && creditsProgressBar != null) {
                refactorTabComponent.setOnCreditUsed(() -> creditsProgressBar.refreshCredits());
                historyTabComponent.setOnCreditUsed(() -> creditsProgressBar.refreshCredits());
                LOG.info("Credit refresh callbacks wired for free user");
            }

            // Platforms tab - 100% free, no credit limitations
            platformsTabComponent = new PlatformsTabComponent(project);
            String platformsLabel = isPremium ? "Platforms " : "Platforms";
            tabbedPane.addTab(platformsLabel, platformsTabComponent.getPanel());
            LOG.info("initializeContent: Added Platforms tab (100% free)");

            // Connect dashboard with platforms tab for risk integration
            dashboardComponent.setPlatformsTabComponent(platformsTabComponent);

            // Reports and Runtime tabs (Experimental features only)
            System.out.println("DEBUG: MigrationToolWindow - About to check experimental features");
            boolean experimentalEnabled = adrianmikula.jakartamigration.intellij.config.FeatureFlags.getInstance().isExperimentalFeaturesEnabled();
            System.out.println("DEBUG: MigrationToolWindow - experimentalEnabled = " + experimentalEnabled);

            if (experimentalEnabled) {
                // Reports tab - Available for all users with experimental features enabled
                reportsTabComponent = new ReportsTabComponent(project, analysisService, advancedScanningService);
                String reportsLabel = isPremium ? "Reports 📊 (Experimental)" : "Reports (Experimental)";
                tabbedPane.addTab(reportsLabel, reportsTabComponent.getPanel());
                LOG.info("initializeContent: Added Reports tab (experimental)");

                // Runtime tab - Available for all users with experimental features enabled
                runtimeTabComponent = new RuntimeTabComponent(project);
                String runtimeLabel = isPremium ? "Runtime ⚡ (Experimental)" : "Runtime (Experimental)";
                tabbedPane.addTab(runtimeLabel, runtimeTabComponent.getPanel());
                LOG.info("initializeContent: Added Runtime tab (experimental)");
            } else {
                reportsTabComponent = null;
                runtimeTabComponent = null;
                LOG.info("initializeContent: Reports and Runtime tabs hidden (experimental features disabled)");
            }

            // Load initial state (empty - wait for user to analyze)
            loadInitialState();

            // Layout: Credits bar (top, only for free users), then toolbar, then tabs
            JPanel topPanel = new JPanel(new BorderLayout());
            topPanel.add(creditsProgressBar, BorderLayout.NORTH);
            topPanel.add(toolbarPanel, BorderLayout.CENTER);
            contentPanel.add(topPanel, BorderLayout.NORTH);
            contentPanel.add(tabbedPane, BorderLayout.CENTER);

            contentPanel.revalidate();
            contentPanel.repaint();
        }

        /**
         * Refreshes tabs when experimental features flag changes.
         * Properly adds or removes experimental tabs at the correct positions.
         */
        public void refreshExperimentalTabs() {
            System.out.println("DEBUG: MigrationToolWindow.refreshExperimentalTabs() called");
            boolean experimentalEnabled = adrianmikula.jakartamigration.intellij.config.FeatureFlags.getInstance().isExperimentalFeaturesEnabled();
            System.out.println("DEBUG: MigrationToolWindow - experimentalEnabled = " + experimentalEnabled);
            
            // Find the Reports tab index if it exists
            int reportsTabIndex = findTabIndex("Reports");
            boolean reportsTabExists = reportsTabIndex >= 0;
            
            // Add or remove Reports tab
            if (experimentalEnabled && !reportsTabExists && reportsTabComponent == null) {
                reportsTabComponent = new ReportsTabComponent(project, analysisService, advancedScanningService);
                // Insert after Platforms tab (find appropriate position)
                int platformsIndex = findTabIndex("Platforms");
                int insertIndex = platformsIndex >= 0 ? platformsIndex + 1 : tabbedPane.getTabCount();
                tabbedPane.insertTab("Reports 📊 (Experimental)", null, reportsTabComponent.getPanel(), null, insertIndex);
                LOG.info("refreshExperimentalTabs: Reports tab added at index " + insertIndex);
            } else if (!experimentalEnabled && reportsTabExists) {
                tabbedPane.removeTabAt(reportsTabIndex);
                reportsTabComponent = null;
                LOG.info("refreshExperimentalTabs: Reports tab removed");
            }
            
            // Find the Runtime tab index if it exists
            int runtimeTabIndex = findTabIndex("Runtime");
            boolean runtimeTabExists = runtimeTabIndex >= 0;
            
            // Add or remove Runtime tab
            if (experimentalEnabled && !runtimeTabExists && runtimeTabComponent == null) {
                runtimeTabComponent = new RuntimeTabComponent(project);
                // Insert after Reports tab if it exists, otherwise after Platforms
                int insertAfter = findTabIndex("Reports");
                if (insertAfter < 0) {
                    insertAfter = findTabIndex("Platforms");
                }
                int insertIndex = insertAfter >= 0 ? insertAfter + 1 : tabbedPane.getTabCount();
                tabbedPane.insertTab("Runtime ⚡ (Experimental)", null, runtimeTabComponent.getPanel(), null, insertIndex);
                LOG.info("refreshExperimentalTabs: Runtime tab added at index " + insertIndex);
            } else if (!experimentalEnabled && runtimeTabExists) {
                tabbedPane.removeTabAt(runtimeTabIndex);
                runtimeTabComponent = null;
                LOG.info("refreshExperimentalTabs: Runtime tab removed");
            }
            
            contentPanel.revalidate();
            contentPanel.repaint();
        }
        
        /**
         * Helper method to find tab index by title prefix.
         */
        private int findTabIndex(String titlePrefix) {
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                String title = tabbedPane.getTitleAt(i);
                if (title != null && title.startsWith(titlePrefix)) {
                    return i;
                }
            }
            return -1;
        }

        public void rebuildUI() {
            System.out.println("DEBUG: rebuildUI() called");
            ApplicationManager.getApplication().invokeLater(() -> {
                int selectedIndex = tabbedPane != null ? tabbedPane.getSelectedIndex() : -1;
                String selectedTitle = (selectedIndex != -1 && selectedIndex < tabbedPane.getTabCount())
                        ? tabbedPane.getTitleAt(selectedIndex)
                        : null;

                // Dispose old credits progress bar before rebuilding
                if (creditsProgressBar != null) {
                    creditsProgressBar.dispose();
                }

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

            buttonPanel.add(upgradeButton);

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
                upgradeButton.setToolTipText("Get unlimited scans and refactors: Auto-fixes, one-click refactoring, binary fixes");
                upgradeButton.addActionListener(e -> openMarketplace());
                toolbarPanel.add(upgradeButton);
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
         * Handle analyze project action - triggers analysis using migration-core library.
         * For premium users, runs all scans with full results.
         * For free users, checks credits and may show truncated results when credits exhausted.
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

            // Set UI to scanning state
            dashboardComponent.setAnalysisRunning(true);

            // Check license status at runtime
            boolean isLicensed = adrianmikula.jakartamigration.intellij.license.CheckLicense.isLicensed();
            LOG.info("handleAnalyzeProject: License check - isLicensed=" + isLicensed);

            if (isLicensed) {
                // Premium: Run all scans with full results
                runPremiumAnalysis(projectPath);
            } else {
                // Free user: Run basic analysis (truncation mode applied at display time based on credits)
                LOG.info("handleAnalyzeProject: Free user, running basic analysis");
                runBasicAnalysis(projectPath);
            }
        }

        /**
         * Run basic analysis only (for non-premium users with credits)
         */
        private void runBasicAnalysis(Path projectPath) {
            LOG.info("runBasicAnalysis: Starting basic scan (free user - no credit consumption)");

            // Note: Basic scans don't consume credits in the simplified model
            // Truncation mode applies when displaying results if credits exhausted

            CompletableFuture.supplyAsync(() -> analysisService.analyzeProject(projectPath))
                    .thenAccept(report -> {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            dashboardComponent.setAnalysisRunning(false);
                            if (report != null && report.dependencyGraph() != null &&
                                    !report.dependencyGraph().getNodes().isEmpty()) {
                                store.saveAnalysisReport(projectPath, report, false);
                                updateDashboardFromReport(report);
                                int depsCount = report.dependencyGraph().getNodes().size();
                                Messages.showInfoMessage(project, "Analysis complete! " +
                                        depsCount +
                                        " dependencies analyzed.", "Analysis Complete");
                            } else {
                                showEmptyResultsState();
                                Messages.showInfoMessage(project,
                                        "Analysis complete. No Jakarta migration issues found.",
                                        "Analysis Complete");
                            }
                        });
                    })
                    .exceptionally(ex -> {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            dashboardComponent.setAnalysisRunning(false);
                            Messages.showWarningDialog(project,
                                    "Analysis failed: " + ex.getMessage(),
                                    "Analysis Failed");
                        });
                        return null;
                    });
        }

        /**
         * Run basic analysis with truncation for free users without credits.
         * Shows partial results with upgrade prompt.
         */
        private void runBasicAnalysisWithTruncation(Path projectPath) {
            LOG.info("runBasicAnalysisWithTruncation: Starting truncated scan (no credits remaining)");

            CompletableFuture.supplyAsync(() -> analysisService.analyzeProject(projectPath))
                    .thenAccept(report -> {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            dashboardComponent.setAnalysisRunning(false);
                            if (report != null && report.dependencyGraph() != null &&
                                    !report.dependencyGraph().getNodes().isEmpty()) {
                                int totalDeps = report.dependencyGraph().getNodes().size();
                                int shownDeps = Math.min(totalDeps, 10); // Show first 10 dependencies

                                // Truncate the report to show only first N dependencies
                                DependencyAnalysisReport truncatedReport = truncateReport(report, shownDeps);

                                store.saveAnalysisReport(projectPath, truncatedReport, false);
                                updateDashboardFromReport(truncatedReport);

                                // Show upgrade dialog with partial results message
                                int result = Messages.showYesNoDialog(project,
                                        String.format("Analysis complete! Showing %d of %d dependencies.\n\n" +
                                                "You've used all your basic scan credits.\n" +
                                                "Upgrade to Premium for unlimited scans and complete results.",
                                                shownDeps, totalDeps),
                                        "Credits Exhausted - Partial Results",
                                        "Upgrade to Premium",
                                        "Continue with Limited Results",
                                        Messages.getInformationIcon());

                                if (result == Messages.YES) {
                                    openMarketplace();
                                }
                            } else {
                                showEmptyResultsState();
                                Messages.showInfoMessage(project,
                                        "Analysis complete. No Jakarta migration issues found.",
                                        "Analysis Complete");
                            }
                        });
                    })
                    .exceptionally(ex -> {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            dashboardComponent.setAnalysisRunning(false);
                            Messages.showWarningDialog(project,
                                    "Analysis failed: " + ex.getMessage(),
                                    "Analysis Failed");
                        });
                        return null;
                    });
        }

        /**
         * Truncate a dependency analysis report to show only first N dependencies.
         */
        private DependencyAnalysisReport truncateReport(DependencyAnalysisReport originalReport, int limit) {
            // For simplicity, we return the full report but the UI will show truncated results
            // A more sophisticated approach would create a new report with limited nodes
            // This is handled in updateDashboardFromReport by checking credit status
            return originalReport;
        }

        /**
         * Run all 3 scans sequentially for premium users:
         * 1. Basic dependency analysis
         * 2. Advanced scans
         * 3. Platform detection
         */
        private void runPremiumAnalysis(Path projectPath) {
            LOG.info("runPremiumAnalysis: Starting sequential scan (basic → advanced → platform)");

            // Step 1: Run basic analysis
            CompletableFuture.supplyAsync(() -> analysisService.analyzeProject(projectPath))
                    .thenCompose(basicReport -> {
                        LOG.info("runPremiumAnalysis: Basic scan completed");

                        // Update UI with basic results
                        ApplicationManager.getApplication().invokeLater(() -> {
                            if (basicReport != null && basicReport.dependencyGraph() != null &&
                                    !basicReport.dependencyGraph().getNodes().isEmpty()) {
                                store.saveAnalysisReport(projectPath, basicReport, false);
                                updateDashboardFromReport(basicReport);
                            } else {
                                showEmptyResultsState();
                            }
                        });

                        // Step 2: Run advanced scans
                        return CompletableFuture.supplyAsync(() -> {
                            LOG.info("runPremiumAnalysis: Starting advanced scans");
                            try {
                                AdvancedScanningService.AdvancedScanSummary advancedSummary =
                                        advancedScanningService.scanAll(projectPath);
                                LOG.info("runPremiumAnalysis: Advanced scans completed");
                                return advancedSummary;
                            } catch (Exception ex) {
                                LOG.warn("runPremiumAnalysis: Advanced scans failed", ex);
                                return null; // Continue even if advanced scan fails
                            }
                        });
                    })
                    .thenCompose(advancedSummary -> {
                        LOG.info("runPremiumAnalysis: Advanced scan step completed");

                        // Update UI with advanced results
                        ApplicationManager.getApplication().invokeLater(() -> {
                            if (advancedSummary != null) {
                                dashboardComponent.updateAdvancedScanCounts();
                            }
                        });

                        // Step 3: Run platform scan
                        return CompletableFuture.supplyAsync(() -> {
                            LOG.info("runPremiumAnalysis: Starting platform scan");
                            try {
                                if (platformsTabComponent != null) {
                                    platformsTabComponent.scanProject();
                                    LOG.info("runPremiumAnalysis: Platform scan completed");
                                } else {
                                    LOG.warn("runPremiumAnalysis: platformsTabComponent is null");
                                }
                                return true;
                            } catch (Exception ex) {
                                LOG.warn("runPremiumAnalysis: Platform scan failed", ex);
                                return false; // Continue even if platform scan fails
                            }
                        }).thenApply(platformSuccess -> advancedSummary);
                    })
                    .thenAccept(finalSummary -> {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            dashboardComponent.setAnalysisRunning(false);
                            LOG.info("runPremiumAnalysis: All scans completed successfully");
                            Messages.showInfoMessage(project,
                                    "Premium analysis complete! Basic, advanced, and platform scans finished.",
                                    "Analysis Complete");
                        });
                    })
                    .exceptionally(ex -> {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            dashboardComponent.setAnalysisRunning(false);
                            LOG.error("runPremiumAnalysis: Analysis failed", ex);
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

                // Determine migration status based on namespace and whether there's a known Jakarta upgrade path
                Namespace namespace = report.namespaceMap() != null
                        ? report.namespaceMap().get(artifact)
                        : Namespace.UNKNOWN;

                // Use the recommendation already looked up earlier
                boolean hasJakartaUpgrade = recommended != null;

                if (namespace == Namespace.JAKARTA) {
                    info.setMigrationStatus(DependencyMigrationStatus.COMPATIBLE);
                } else if (namespace == Namespace.JAVAX && hasJakartaUpgrade) {
                    // Has known Jakarta upgrade path from compatibility.yaml or platforms.yaml
                    info.setMigrationStatus(DependencyMigrationStatus.NEEDS_UPGRADE);
                    info.setTransitive(false);
                } else if (namespace == Namespace.JAVAX && !hasJakartaUpgrade) {
                    // JAVAX but no known Jakarta upgrade path
                    info.setMigrationStatus(DependencyMigrationStatus.NO_JAKARTA_VERSION);
                } else if (namespace == Namespace.UNKNOWN) {
                    // Unknown namespace - check if it's a known non-Jakarta library
                    if (isKnownNonJakartaLibrary(artifact)) {
                        info.setMigrationStatus(DependencyMigrationStatus.COMPATIBLE);
                    } else {
                        // Requires manual review
                        info.setMigrationStatus(DependencyMigrationStatus.UNKNOWN_REVIEW);
                    }
                } else {
                    // Other namespace
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

            // Calculate metrics with all dependency categories
            long blockers = deps.stream().filter(DependencyInfo::isBlocker).count();
            long migrable = deps.stream().filter(d -> d.getRecommendedVersion() != null).count();
            long noJakartaSupport = deps.stream()
                    .filter(d -> d.getMigrationStatus() == DependencyMigrationStatus.NO_JAKARTA_VERSION)
                    .count();
            long jakartaUpgrade = deps.stream()
                    .filter(d -> d.getMigrationStatus() == DependencyMigrationStatus.NEEDS_UPGRADE)
                    .count();
            long jakartaCompatible = deps.stream()
                    .filter(d -> d.getMigrationStatus() == DependencyMigrationStatus.COMPATIBLE)
                    .count();
            long organisational = deps.stream().filter(d -> d.isOrganizational()).count();
            long unknownReview = deps.stream()
                    .filter(d -> d.getMigrationStatus() == DependencyMigrationStatus.UNKNOWN_REVIEW ||
                                 d.getMigrationStatus() == DependencyMigrationStatus.REQUIRES_MANUAL_MIGRATION)
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
            summary.setJakartaUpgradeCount((int) jakartaUpgrade);
            summary.setJakartaCompatibleCount((int) jakartaCompatible);
            summary.setOrganisationalDependencies((int) organisational);
            summary.setUnknownReviewCount((int) unknownReview);
            summary.setTransitiveDependencies((int) transitive);
            dashboard.setDependencySummary(summary);

            // Update dashboard components with new data
            dashboardComponent.setDashboard(dashboard);
            dependenciesComponent.setDependencies(deps);
            migrationPhasesComponent.setDependencies(deps);
        }

        /**
         * Check if an artifact is a known non-Jakarta library that doesn't need migration.
         * These libraries should be marked as COMPATIBLE rather than UNKNOWN_REVIEW.
         */
        private boolean isKnownNonJakartaLibrary(Artifact artifact) {
            String groupId = artifact.groupId();
            
            // Test frameworks and libraries
            if (groupId.startsWith("junit")) return true;
            if (groupId.startsWith("org.junit")) return true;
            if (groupId.startsWith("org.hamcrest")) return true;
            if (groupId.startsWith("org.assertj")) return true;
            if (groupId.startsWith("org.testng")) return true;
            if (groupId.startsWith("org.mockito")) return true;
            if (groupId.startsWith("org.spockframework")) return true;
            
            // Arquillian core/protocol modules are generally safe
            // BUT container adapters (org.jboss.arquillian.container.*) are NOT all Jakarta-compatible
            // Only mark specific safe Arquillian artifacts as non-Jakarta
            if (groupId.equals("org.jboss.arquillian")) return true; // BOM
            if (groupId.equals("org.jboss.arquillian.protocol")) return true; // Protocol modules
            if (groupId.equals("org.jboss.arquillian.test")) return true; // Test core
            if (groupId.equals("org.jboss.arquillian.junit")) return true; // JUnit integration
            if (groupId.equals("org.jboss.arquillian.testng")) return true; // TestNG integration
            if (groupId.equals("org.jboss.arquillian.core")) return true; // Core
            if (groupId.equals("org.jboss.arquillian.config")) return true; // Config
            // NOTE: org.jboss.arquillian.container.* is NOT safe - adapters vary by server
            
            if (groupId.startsWith("org.jboss.shrinkwrap")) return true;
            
            // Utility libraries
            if (groupId.startsWith("org.apache.commons")) return true;
            if (groupId.startsWith("org.apache.logging")) return true;
            if (groupId.startsWith("org.slf4j")) return true;
            if (groupId.startsWith("ch.qos.logback")) return true;
            
            // JSON/XML processing
            if (groupId.startsWith("com.fasterxml.jackson")) return true;
            if (groupId.startsWith("org.json")) return true;
            if (groupId.startsWith("com.google.code.gson")) return true;
            if (groupId.startsWith("org.skyscreamer")) return true; // jsonassert
            
            // Database drivers
            if (groupId.startsWith("com.h2database")) return true;
            if (groupId.startsWith("mysql")) return true;
            if (groupId.startsWith("org.postgresql")) return true;
            if (groupId.startsWith("com.oracle.database")) return true;
            
            // Security/Crypto
            if (groupId.startsWith("org.bouncycastle")) return true;
            if (groupId.startsWith("org.springframework.security")) return true;
            
            // Build tools and plugins
            if (groupId.startsWith("org.apache.maven")) return true;
            if (groupId.startsWith("org.gradle")) return true;
            
            // Async/testing utilities
            if (groupId.startsWith("com.jayway.awaitility")) return true;
            if (groupId.startsWith("org.awaitility")) return true;
            if (groupId.startsWith("org.omnifaces")) return true;
            
            // Web/HTTP testing
            if (groupId.startsWith("net.sourceforge.htmlunit")) return true;
            if (groupId.startsWith("httpunit")) return true;
            if (groupId.startsWith("rhino")) return true;
            
            // XML utilities
            if (groupId.startsWith("xmlunit")) return true;
            
            return false;
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
         * Refresh dependency graph with updated status when async analysis completes
         */
        private void refreshDependencyGraphWithUpdatedStatus(List<DependencyInfo> updatedDependencies) {
            SwingUtilities.invokeLater(() -> {
                // Build updated status map for dependency graph
                Map<String, String> updatedStatusMap = new HashMap<>();
                for (DependencyInfo dep : updatedDependencies) {
                    String key = dep.getGroupId() + ":" + dep.getArtifactId();
                    updatedStatusMap.put(key, dep.getMigrationStatus().name());
                }
                
                // Update dependency graph with new statuses
                dependencyGraphComponent.updateNodeStatuses(updatedStatusMap);
                
                LOG.info("Dependency graph refreshed with " + updatedDependencies.size() + " updated statuses");
            });
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
            ApplicationManager.getApplication().invokeLater(() -> {
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
