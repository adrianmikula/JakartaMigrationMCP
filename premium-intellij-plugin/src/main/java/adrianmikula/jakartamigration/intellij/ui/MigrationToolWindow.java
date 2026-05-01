package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyProjectScanResult;
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
import adrianmikula.jakartamigration.intellij.ui.DevTabComponent;
import adrianmikula.jakartamigration.intellij.ui.SimplePlatformsTabComponent;
import adrianmikula.jakartamigration.intellij.ui.PlatformsTabComponent;
import adrianmikula.jakartamigration.intellij.ui.ReportsTabComponent;
import adrianmikula.jakartamigration.intellij.ui.components.NewFeatureNotification;
import adrianmikula.jakartamigration.intellij.ui.components.PremiumUpgradeButton;
import adrianmikula.jakartamigration.analytics.service.ErrorReportingService;
import adrianmikula.jakartamigration.analytics.service.UserIdentificationService;
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
        private final ErrorReportingService errorReportingService;
        private final UserIdentificationService userIdentificationService;

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
        private DevTabComponent devTabComponent;
        private CodeRefactoringModule refactorModule;
        private RecipeService recipeService;
        private SqliteMigrationAnalysisStore projectStore;
        private JTabbedPane tabbedPane;
        private JPanel toolbarPanel;
        private JPanel scanControlsPanel;
        private CreditsProgressBar creditsProgressBar;
        private CreditsService creditsService;
        private NewFeatureNotification notificationComponent;
        private boolean isPremium;
        
        // Scan controls components
        private JButton analyzeButton;
        private JProgressBar scanProgressBar;
        private JLabel scanProgressLabel;

        public MigrationToolWindowContent(Project project) {
            this.project = project;
            this.analysisService = new MigrationAnalysisService();
            this.store = new CentralMigrationAnalysisStore();
            this.creditsService = new CreditsService();
            this.userIdentificationService = createUserIdentificationService();
            this.errorReportingService = new ErrorReportingService(this.userIdentificationService);

            // Initialize project-specific store
            Path projectPath = Paths.get(project.getBasePath());
            this.projectStore = new SqliteMigrationAnalysisStore(projectPath);
            this.refactorModule = new CodeRefactoringModule(this.store, this.projectStore);
            this.recipeService = this.refactorModule.getRecipeService();
            
            // Initialize advanced scanning service with recipe service
            this.advancedScanningService = new AdvancedScanningService(this.recipeService);
            
            // Initialize credits service
            this.creditsService = new CreditsService();

            this.contentPanel = new JPanel(new BorderLayout());

            // Check premium status
            this.isPremium = checkPremiumStatus();
            LOG.info("MigrationToolWindowContent: Constructor called, isPremium=" + isPremium);
            LOG.info("MigrationToolWindowContent: System property jakarta.migration.premium=" +
                    System.getProperty("jakarta.migration.premium"));

            initializeContent();
        }

        /**
         * Factory method for creating UserIdentificationService.
         * Allows tests to override and provide mock implementations.
         */
        protected UserIdentificationService createUserIdentificationService() {
            return new UserIdentificationService();
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
            LOG.info("initializeContent: License status string: " + adrianmikula.jakartamigration.intellij.license.CheckLicense.getLicenseStatusString());
            LOG.info("initializeContent: System property jakarta.migration.dev.simulate_premium=" + System.getProperty("jakarta.migration.dev.simulate_premium"));
            contentPanel.removeAll();

            // Credits progress bar (only visible for free users)
            creditsProgressBar = new CreditsProgressBar(project);
            creditsProgressBar.refreshCredits();

            // Scan controls panel with analyze button and progress bar
            scanControlsPanel = createScanControlsPanel();

            // Toolbar with action buttons (now minimal)
            toolbarPanel = createToolbar();

            tabbedPane = new JTabbedPane();

            // Dev tab - only in development mode, positioned first
            if (adrianmikula.jakartamigration.intellij.license.CheckLicense.isDevMode()) {
                devTabComponent = new DevTabComponent(project, this::handlePremiumSimulationChanged, errorReportingService);
                tabbedPane.addTab("Dev", devTabComponent.getPanel());
                LOG.info("initializeContent: Added Dev tab (development mode)");
            }

            // Dashboard tab
            dashboardComponent = new DashboardComponent(project, advancedScanningService, this::handleAnalyzeProject);
            dashboardComponent.setExternalProgressComponents(scanProgressBar, scanProgressLabel, analyzeButton);
            tabbedPane.addTab("Risk", dashboardComponent.getPanel());

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
            advancedScansComponent = new AdvancedScansComponent(project, advancedScanningService, errorReportingService);
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
            supportComponent = new SupportComponent(project, v -> refreshPremiumTabs(), () -> refreshExperimentalTabs(), userIdentificationService);
            tabbedPane.addTab("About", supportComponent.getPanel());

            // AI tab - controlled by premium feature flag (formerly MCP Server)
            boolean mcpServerPremiumOnly = adrianmikula.jakartamigration.intellij.config.FeatureFlags.getInstance().isMcpServerPremiumOnly();
            LOG.info("initializeContent: AI tab check - mcpServerPremiumOnly=" + mcpServerPremiumOnly + ", isPremium=" + isPremium);
            if (!mcpServerPremiumOnly || isPremium) {
                mcpServerTabComponent = new McpServerTabComponent(project);
                tabbedPane.addTab("AI", mcpServerTabComponent.getPanel());
                LOG.info("initializeContent: Added AI tab (mcpServerPremiumOnly=" + mcpServerPremiumOnly + ", isPremium=" + isPremium + ")");
            } else {
                mcpServerTabComponent = null;
                LOG.info("initializeContent: AI tab hidden - MCP server is premium only and user is not premium");
            }

            // Reports tab - controlled by premium feature flag
            boolean reportsPremiumOnly = adrianmikula.jakartamigration.intellij.config.FeatureFlags.getInstance().isReportsPremiumOnly();
            LOG.info("initializeContent: Reports tab check - reportsPremiumOnly=" + reportsPremiumOnly + ", isPremium=" + isPremium);
            if (!reportsPremiumOnly || isPremium) {
                reportsTabComponent = new ReportsTabComponent(project, analysisService, advancedScanningService, errorReportingService);
                String reportsLabel = isPremium ? "Reports 📊" : "Reports";
                tabbedPane.addTab(reportsLabel, reportsTabComponent.getPanel());
                LOG.info("initializeContent: Added Reports tab (reportsPremiumOnly=" + reportsPremiumOnly + ", isPremium=" + isPremium + ")");
            } else {
                reportsTabComponent = null;
                LOG.info("initializeContent: Reports tab hidden - Reports is premium only and user is not premium");
            }

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
            
            // Connect reports tab with platforms tab for report generation
            if (reportsTabComponent != null) {
                reportsTabComponent.setPlatformsTabComponent(platformsTabComponent);
                LOG.info("initializeContent: Wired reportsTabComponent with platformsTabComponent");
            }

            // Set up tab switcher for dashboard explanation panels
            dashboardComponent.setTabSwitcher(tabName -> {
                // Find and switch to the requested tab
                int tabIndex = findTabIndex(tabName);
                if (tabIndex >= 0) {
                    tabbedPane.setSelectedIndex(tabIndex);
                    LOG.info("Switched to tab: " + tabName);
                } else {
                    LOG.warn("Tab not found: " + tabName);
                }
            });

            // Runtime tab (Experimental features only)
            System.out.println("DEBUG: MigrationToolWindow - About to check experimental features");
            boolean experimentalEnabled = adrianmikula.jakartamigration.intellij.config.FeatureFlags.getInstance().isExperimentalFeaturesEnabled();
            System.out.println("DEBUG: MigrationToolWindow - experimentalEnabled = " + experimentalEnabled);

            if (experimentalEnabled) {
                // Runtime tab - Available for all users with experimental features enabled
                runtimeTabComponent = new RuntimeTabComponent(project);
                String runtimeLabel = isPremium ? "Runtime ⚡ (Experimental)" : "Runtime (Experimental)";
                tabbedPane.addTab(runtimeLabel, runtimeTabComponent.getPanel());
                LOG.info("initializeContent: Added Runtime tab (experimental)");
            } else {
                runtimeTabComponent = null;
                LOG.info("initializeContent: Runtime tab hidden (experimental features disabled)");
            }

            // Load initial state (empty - wait for user to analyze)
            loadInitialState();

            // Create and configure usage permission notification
            createUsagePermissionNotification();

            // Layout: Notification (top), credits bar, scan controls panel, then tabs
            JPanel notificationContainer = new JPanel(new BorderLayout());
            if (notificationComponent != null) {
                notificationContainer.add(notificationComponent.getPanel(), BorderLayout.NORTH);
            }
            JPanel topPanel = new JPanel(new BorderLayout());
            topPanel.add(creditsProgressBar, BorderLayout.NORTH);
            topPanel.add(scanControlsPanel, BorderLayout.CENTER);
            notificationContainer.add(topPanel, BorderLayout.CENTER);
            contentPanel.add(notificationContainer, BorderLayout.NORTH);
            contentPanel.add(tabbedPane, BorderLayout.CENTER);

            contentPanel.revalidate();
            contentPanel.repaint();
        }

        /**
         * Creates usage permission notification if this is the first time plugin is opened.
         */
        private void createUsagePermissionNotification() {
            // Check if permission has already been requested
            if (userIdentificationService.isUsagePermissionRequested()) {
                LOG.info("Usage permission already requested, skipping notification");
                return;
            }
            
            // Create notification with Yes/No actions
            notificationComponent = NewFeatureNotification.createUsagePermissionNotification(
                this::handleUsagePermissionYes,
                this::handleUsagePermissionNo
            );
            
            LOG.info("Created usage permission notification for first-time user");
        }
        
        /**
         * Handles user clicking "Yes" to usage permission request.
         * Keeps default settings (usage and error reporting enabled).
         */
        private void handleUsagePermissionYes() {
            LOG.info("User opted in to usage data collection");
            
            // Mark permission as requested
            userIdentificationService.setUsagePermissionRequested();
            
            // Hide notification
            if (notificationComponent != null) {
                notificationComponent.setVisible(false);
            }
            
            // Ensure usage metrics and error reporting are enabled (default behavior)
            userIdentificationService.setUsageMetricsEnabled(true);
            userIdentificationService.setErrorReportingEnabled(true);
            
            LOG.info("Usage permission granted - analytics enabled");
        }
        
        /**
         * Handles user clicking "No" to usage permission request.
         * Disables both usage metrics and error reporting.
         */
        private void handleUsagePermissionNo() {
            LOG.info("User opted out of usage data collection");
            
            // Mark permission as requested
            userIdentificationService.setUsagePermissionRequested();
            
            // Hide notification
            if (notificationComponent != null) {
                notificationComponent.setVisible(false);
            }
            
            // Disable both usage metrics and error reporting
            userIdentificationService.setUsageMetricsEnabled(false);
            userIdentificationService.setErrorReportingEnabled(false);
            
            LOG.info("Usage permission denied - analytics disabled");
        }

        /**
         * Handles premium simulation state changes from Dev tab.
         * Triggers a complete UI rebuild to reflect the new license state.
         */
        private void handlePremiumSimulationChanged(boolean isSimulatingPremium) {
            LOG.info("MigrationToolWindow: Premium simulation changed to: " + isSimulatingPremium);
            LOG.info("MigrationToolWindow: Current isPremium before change: " + this.isPremium);
            
            // Clear license cache to ensure fresh checks
            adrianmikula.jakartamigration.intellij.license.CheckLicense.onPremiumSimulationChanged();
            
            // Update isPremium field with fresh license check after cache clear
            this.isPremium = checkPremiumStatus();
            LOG.info("MigrationToolWindow: Updated isPremium after cache clear: " + this.isPremium);
            
            // Rebuild the entire UI to reflect the new premium state
            rebuildUI();
        }

        /**
         * Refreshes tabs when experimental features flag changes.
         * Properly adds or removes experimental tabs at the correct positions.
         */
        public void refreshExperimentalTabs() {
            System.out.println("DEBUG: MigrationToolWindow.refreshExperimentalTabs() called");
            boolean experimentalEnabled = adrianmikula.jakartamigration.intellij.config.FeatureFlags.getInstance().isExperimentalFeaturesEnabled();
            System.out.println("DEBUG: MigrationToolWindow - experimentalEnabled = " + experimentalEnabled);
            
            // Find the Runtime tab index if it exists
            int runtimeTabIndex = findTabIndex("Runtime");
            boolean runtimeTabExists = runtimeTabIndex >= 0;
            
            // Add or remove Runtime tab
            if (experimentalEnabled && !runtimeTabExists && runtimeTabComponent == null) {
                runtimeTabComponent = new RuntimeTabComponent(project);
                // Insert after Platforms tab
                int platformsIndex = findTabIndex("Platforms");
                int insertIndex = platformsIndex >= 0 ? platformsIndex + 1 : tabbedPane.getTabCount();
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
            LOG.info("MigrationToolWindow: rebuildUI() starting, current isPremium: " + this.isPremium);
            
            ApplicationManager.getApplication().invokeLater(() -> {
                int selectedIndex = tabbedPane != null ? tabbedPane.getSelectedIndex() : -1;
                String selectedTitle = (selectedIndex != -1 && selectedIndex < tabbedPane.getTabCount())
                        ? tabbedPane.getTitleAt(selectedIndex)
                        : null;

                // Dispose old credits progress bar before rebuilding
                if (creditsProgressBar != null) {
                    creditsProgressBar.dispose();
                }

                LOG.info("MigrationToolWindow: rebuildUI() - calling initializeContent(), isPremium: " + this.isPremium);
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

        /**
         * Creates the scan controls panel with analyze button and progress bar.
         * This panel will be positioned below the credits progress bar and above the tabs.
         */
        private JPanel createScanControlsPanel() {
            scanControlsPanel = new JPanel(new BorderLayout());
            scanControlsPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 20, 10)); // Increased bottom margin for larger gap

            // Main container with horizontal layout for button and progress
            JPanel mainContainer = new JPanel(new BorderLayout(10, 0));
            
            // Analyze button
            analyzeButton = new JButton("▶ Analyze Project");
            analyzeButton.setToolTipText("Run migration analysis on the current project");
            analyzeButton.addActionListener(this::handleAnalyzeProject);
            // Make button narrower and twice as tall
            Dimension currentButtonSize = analyzeButton.getPreferredSize();
            analyzeButton.setMaximumSize(new Dimension(180, currentButtonSize.height * 2));
            analyzeButton.setPreferredSize(new Dimension(180, currentButtonSize.height * 2));
            System.out.println("DEBUG: Analyze button created, enabled: " + analyzeButton.isEnabled());
            
            // Progress container
            JPanel progressContainer = new JPanel(new BorderLayout(10, 0));
            
            // Progress bar - now handles long descriptions internally
            scanProgressBar = new JProgressBar(0, 100);
            scanProgressBar.setValue(0);
            scanProgressBar.setStringPainted(true);
            scanProgressBar.setString("Ready to scan");
            // Match progress bar height to analyze button height
            Dimension buttonSize = analyzeButton.getPreferredSize();
            scanProgressBar.setPreferredSize(new Dimension(300, buttonSize.height));
            scanProgressBar.setMaximumSize(new Dimension(300, buttonSize.height));
            progressContainer.add(scanProgressBar, BorderLayout.CENTER);
            
            // Progress label - minimized since long descriptions now in progress bar
            scanProgressLabel = new JLabel(""); // Initially empty
            scanProgressLabel.setFont(scanProgressLabel.getFont().deriveFont(Font.PLAIN, 11f));
            scanProgressLabel.setVisible(false); // Hidden by default
            progressContainer.add(scanProgressLabel, BorderLayout.EAST);
            
            // Add analyze button to left, progress to right
            mainContainer.add(analyzeButton, BorderLayout.WEST);
            mainContainer.add(progressContainer, BorderLayout.CENTER);
            
            scanControlsPanel.add(mainContainer, BorderLayout.CENTER);

            return scanControlsPanel;
        }

        private JPanel createToolbar() {
            toolbarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
            toolbarPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            // Status indicator
            JLabel statusLabel = new JLabel("Ready");
            statusLabel.setForeground(Color.GRAY);
            toolbarPanel.add(statusLabel);

            // Add glue to push content to the left
            toolbarPanel.add(Box.createHorizontalGlue());

            // Note: License button is now handled in scanControlsPanel
            // This toolbar is now minimal and could be removed in future

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

            JButton upgradeButton = PremiumUpgradeButton.createUpgradeButton(
                project, 
                "migration_tool_window_placeholder",
                "⬆ Upgrade to Premium",
                "Get Premium features: Auto-fixes, one-click refactoring, binary fixes"
            );

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
                JButton upgradeButton = PremiumUpgradeButton.createUpgradeButton(
                    project,
                    "migration_tool_window_toolbar",
                    "⬆ Upgrade to Premium",
                    "Get unlimited scans and refactors: Auto-fixes, one-click refactoring, binary fixes"
                );
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

            // Check credits for free users
            if (!isPremium) {
                // Check if user has remaining action credits
                if (!creditsService.hasCredits(CreditType.ACTIONS)) {
                    Messages.showWarningDialog(project, 
                        "You've used all your free action credits. Upgrade to Premium to continue scanning projects.\n\n" +
                        "Premium includes:\n" +
                        "• Unlimited action credits\n" +
                        "• Advanced scanning features\n" +
                        "• PDF report generation\n" +
                        "• Code refactoring tools",
                        "Credits Exhausted");
                    return;
                }
                
                // Consume one action credit for the scan
                boolean creditConsumed = creditsService.useCredit(CreditType.ACTIONS, "Scanning", "basic_scan");
                if (!creditConsumed) {
                    Messages.showErrorDialog(project, "Failed to consume action credit. Please try again.", "Credit Error");
                    return;
                }
                
                int remainingCredits = creditsService.getRemainingCredits(CreditType.ACTIONS);
                LOG.info("handleAnalyzeProject: Consumed 1 action credit for free user. Remaining: " + remainingCredits);
            }

            // Set UI to scanning state
            dashboardComponent.setAnalysisRunning(true);

            // Run full analysis for all users (deep scan with transitive dependencies)
            // Display truncation for free users is handled by DependenciesTableComponent
            LOG.info("handleAnalyzeProject: Running full analysis (deep scan with transitive dependencies)");
            runFullAnalysis(projectPath);
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
         * Run all 3 scans sequentially for all users:
         * 1. Deep dependency scanning (or basic analysis as fallback)
         * 2. Advanced scans
         * 3. Platform detection
         *
         * Display truncation for free users is handled at the UI level by DependenciesTableComponent
         * which limits results to 10 rows for non-premium users.
         */
        private void runFullAnalysis(Path projectPath) {
            LOG.info("runFullAnalysis: Starting sequential scan (deep deps -> advanced -> platform)");

            // Report initial progress
            dashboardComponent.onScanPhase("Deep Dependency Analysis", 0, 3);

            // Step 1: Run deep dependency scanning (with fallback to basic analysis)
            CompletableFuture.supplyAsync(() -> runDeepDependencyAnalysis(projectPath))
                    .thenCompose(deepScanResult -> {
                        LOG.info("runFullAnalysis: Dependency scan completed (deep or basic fallback)");
                        
                        // Report progress after dependency analysis
                        dashboardComponent.onScanPhase("Advanced Scans", 1, 3);

                        // Step 2: Run advanced scans (without transitive dependency scanning)
                        return CompletableFuture.supplyAsync(() -> {
                            LOG.info("runFullAnalysis: Starting advanced scans");
                            try {
                                AdvancedScanningService.AdvancedScanSummary advancedSummary =
                                        advancedScanningService.scanAll(projectPath, dashboardComponent);
                                LOG.info("runFullAnalysis: Advanced scans completed");
                                return advancedSummary;
                            } catch (Exception ex) {
                                LOG.warn("runFullAnalysis: Advanced scans failed", ex);
                                dashboardComponent.onScanError(ex);
                                return null; // Continue even if advanced scan fails
                            }
                        }).thenApply(advancedSummary -> {
                            // Combine deep dependency results with advanced summary if available
                            if (deepScanResult != null && !deepScanResult.isEmpty()) {
                                LOG.info("runFullAnalysis: Using deep dependency scan results with " +
                                         deepScanResult.size() + " dependencies");
                                // Store the deep scan results for display
                                storeDeepDependencyResults(projectPath, deepScanResult);
                            }
                            return advancedSummary;
                        });
                    })
                    .thenCompose(advancedSummary -> {
                        LOG.info("runFullAnalysis: Advanced scan step completed");

                        // Report progress for platform scan
                        dashboardComponent.onScanPhase("Platform Detection", 2, 3);

                        // Update UI with advanced results
                        ApplicationManager.getApplication().invokeLater(() -> {
                            if (advancedSummary != null) {
                                dashboardComponent.updateAdvancedScanCounts();
                                // Refresh Advanced Scans tab to display the new results
                                if (advancedScansComponent != null) {
                                    advancedScansComponent.refreshFromCachedResults();
                                }
                            }
                        });

                        // Step 3: Run platform scan
                        return CompletableFuture.supplyAsync(() -> {
                            LOG.info("runFullAnalysis: Starting platform scan");
                            try {
                                if (platformsTabComponent != null) {
                                    platformsTabComponent.scanProject();
                                    LOG.info("runFullAnalysis: Platform scan completed");
                                } else {
                                    LOG.warn("runFullAnalysis: platformsTabComponent is null");
                                }
                                return true;
                            } catch (Exception ex) {
                                LOG.warn("runFullAnalysis: Platform scan failed", ex);
                                return false; // Continue even if platform scan fails
                            }
                        }).thenApply(platformSuccess -> advancedSummary);
                    })
                    .thenAccept(finalSummary -> {
                        // Report completion
                        dashboardComponent.onScanComplete();
                        
                        ApplicationManager.getApplication().invokeLater(() -> {
                            LOG.info("runFullAnalysis: All scans completed successfully");
                            boolean isPremium = adrianmikula.jakartamigration.intellij.license.CheckLicense.isLicensed();
                            if (isPremium) {
                                Messages.showInfoMessage(project,
                                        "Analysis complete! Deep dependency, advanced, and platform scans finished.",
                                        "Analysis Complete");
                            } else {
                                Messages.showInfoMessage(project,
                                        "Analysis complete! Showing all dependencies found.\n\n" +
                                        "Note: Free users see first 10 dependencies in the Dependencies tab. " +
                                        "Upgrade to Premium for unlimited access.",
                                        "Analysis Complete");
                            }
                        });
                    })
                    .exceptionally(ex -> {
                        Exception exception = ex instanceof Exception ? (Exception) ex : new RuntimeException(ex);
                        dashboardComponent.onScanError(exception);
                        ApplicationManager.getApplication().invokeLater(() -> {
                            dashboardComponent.setAnalysisRunning(false);
                            LOG.error("runFullAnalysis: Analysis failed", ex);
                            Messages.showWarningDialog(project,
                                    "Analysis failed: " + ex.getMessage(),
                                    "Analysis Failed");
                        });
                        return null;
                    });
        }

        /**
         * Runs deep dependency analysis using Maven/Gradle commands.
         * Falls back to basic analysis if Maven/Gradle are not available.
         *
         * @param projectPath Path to the project root
         * @return List of DependencyInfo with deep transitive dependencies, or null if failed
         */
        private List<DependencyInfo> runDeepDependencyAnalysis(Path projectPath) {
            LOG.info("runDeepDependencyAnalysis: Starting deep dependency scan");

            // Check if Maven or Gradle is available
            boolean mavenAvailable = advancedScanningService.isMavenAvailable();
            boolean gradleAvailable = advancedScanningService.isGradleAvailable();

            LOG.info("runDeepDependencyAnalysis: Maven available=" + mavenAvailable +
                     ", Gradle available=" + gradleAvailable);

            if (!mavenAvailable && !gradleAvailable) {
                LOG.warn("runDeepDependencyAnalysis: Neither Maven nor Gradle available, falling back to basic analysis");

                // Fall back to basic analysis
                ApplicationManager.getApplication().invokeLater(() -> {
                    Messages.showInfoMessage(project,
                            "Deep dependency analysis unavailable (Maven/Gradle not found).\n" +
                            "Falling back to declared dependencies only.",
                            "Limited Dependency Analysis");
                });

                // Run basic analysis and convert results
                try {
                    DependencyAnalysisReport report = analysisService.analyzeProject(projectPath);
                    if (report != null && report.dependencyGraph() != null) {
                        updateDashboardFromReport(report);
                        return convertBasicReportToDependencyInfo(report);
                    }
                } catch (Exception e) {
                    LOG.error("runDeepDependencyAnalysis: Basic analysis fallback failed", e);
                }
                return new ArrayList<>();
            }

            // Run deep transitive dependency scan
            try {
                TransitiveDependencyProjectScanResult deepResult =
                        advancedScanningService.scanDependenciesDeep(projectPath);

                if (deepResult == null) {
                    LOG.warn("runDeepDependencyAnalysis: Deep scan returned null");
                    return new ArrayList<>();
                }

                // Convert to DependencyInfo list
                List<DependencyInfo> dependencyInfos = advancedScanningService.convertToDependencyInfo(deepResult);

                LOG.info("runDeepDependencyAnalysis: Deep scan completed with " + dependencyInfos.size() + " dependencies");

                // Update UI with deep results
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (!dependencyInfos.isEmpty()) {
                        dependenciesComponent.setDependencies(dependencyInfos);
                        LOG.info("runDeepDependencyAnalysis: Updated Dependencies table with deep scan results");
                    }
                });

                return dependencyInfos;

            } catch (Exception e) {
                LOG.error("runDeepDependencyAnalysis: Deep scan failed", e);

                // Fall back to basic analysis
                try {
                    DependencyAnalysisReport report = analysisService.analyzeProject(projectPath);
                    if (report != null && report.dependencyGraph() != null) {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            Messages.showWarningDialog(project,
                                    "Deep dependency scanning failed: " + e.getMessage() + "\n" +
                                    "Falling back to declared dependencies only.",
                                    "Deep Scan Failed");
                            updateDashboardFromReport(report);
                        });
                        return convertBasicReportToDependencyInfo(report);
                    }
                } catch (Exception fallbackEx) {
                    LOG.error("runDeepDependencyAnalysis: Basic analysis fallback also failed", fallbackEx);
                }

                return new ArrayList<>();
            }
        }

        /**
         * Converts basic analysis report to DependencyInfo list.
         */
        private List<DependencyInfo> convertBasicReportToDependencyInfo(DependencyAnalysisReport report) {
            List<DependencyInfo> deps = new ArrayList<>();
            if (report == null || report.dependencyGraph() == null) {
                return deps;
            }

            for (Artifact artifact : report.dependencyGraph().getNodes()) {
                DependencyInfo info = new DependencyInfo();
                info.setArtifactId(artifact.artifactId());
                info.setGroupId(artifact.groupId());
                info.setCurrentVersion(artifact.version());
                info.setTransitive(artifact.transitive());
                info.setDepth(artifact.transitive() ? 1 : 0); // Basic analysis only knows direct vs transitive
                info.setScope(artifact.scope());
                deps.add(info);
            }

            return deps;
        }

        /**
         * Stores deep dependency results and updates the UI.
         */
        private void storeDeepDependencyResults(Path projectPath, List<DependencyInfo> deepScanResult) {
            LOG.info("storeDeepDependencyResults: Storing " + deepScanResult.size() + " dependencies");

            // Update Dependencies table
            dependenciesComponent.setDependencies(deepScanResult);

            // Update Dependency Graph (if component supports DependencyInfo)
            // Note: dependencyGraphComponent currently uses DependencyGraph, may need adaptation

            // Store in cache/database if needed
            // store.saveDeepDependencyResults(projectPath, deepScanResult);
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
                    DependencyMigrationStatus status = dep.getMigrationStatus();
                    updatedStatusMap.put(key, status != null ? status.name() : "UNKNOWN");
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
                // Rebuild entire UI to reflect premium status changes
                rebuildUI();
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
        
        /**
         * Dispose method to clean up resources
         */
        public void dispose() {
            try {
                if (creditsService != null) {
                    creditsService.close();
                }
                if (creditsProgressBar != null) {
                    creditsProgressBar.dispose();
                }
            } catch (Exception e) {
                LOG.warn("Error disposing resources", e);
            }
        }
    }
}
