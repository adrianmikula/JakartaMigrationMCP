package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.JakartaMcpRegistrationActivity;
import adrianmikula.jakartamigration.intellij.mcp.JakartaMcpServerProvider;
import adrianmikula.jakartamigration.intellij.model.DependencySummary;
import adrianmikula.jakartamigration.intellij.model.MigrationDashboard;
import adrianmikula.jakartamigration.intellij.service.AdvancedScanningService;
import adrianmikula.jakartamigration.platforms.config.RiskScoringConfig;
import adrianmikula.jakartamigration.risk.RiskScoringService;
import adrianmikula.jakartamigration.risk.EnhancedTestCoverageAnalysisService;
import adrianmikula.jakartamigration.credits.CreditsService;
import adrianmikula.jakartamigration.credits.CreditType;
import adrianmikula.jakartamigration.credits.FreemiumConfig;
import adrianmikula.jakartamigration.intellij.license.CheckLicense;
import adrianmikula.jakartamigration.intellij.ui.SupportComponent;
import adrianmikula.jakartamigration.intellij.ui.components.TruncationHelper;
import adrianmikula.jakartamigration.intellij.ui.components.RiskGauge;
import adrianmikula.jakartamigration.intellij.ui.components.PremiumUpgradeButton;
import adrianmikula.jakartamigration.intellij.ui.components.ConfidenceGauge;
import adrianmikula.jakartamigration.intellij.ui.components.EffortGauge;
import adrianmikula.jakartamigration.intellij.ui.components.CombinedConfidenceGauge;
import adrianmikula.jakartamigration.platforms.model.EnhancedPlatformScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Dashboard component with text-based key:value table and MCP server status.
 * Shows migration summary and AI assistant integration status.
 */
public class DashboardComponent implements ScanProgressListener {
    private static final Logger LOG = Logger.getInstance(DashboardComponent.class);

    private final JPanel panel;
    private final Project project;
    private MigrationDashboard dashboard;
    private final Consumer<ActionEvent> onAnalyze;
    private final AdvancedScanningService advancedScanningService;
    private final CreditsService creditsService;
    private final TruncationHelper truncationHelper;
    private final EnhancedTestCoverageAnalysisService enhancedTestCoverageService;
    private PlatformsTabComponent platformsTabComponent;
    
    // Cache for preventing unnecessary updates
    private Integer lastCalculatedRiskScore = null;

    // UI Components for gauges (top section)
    private JPanel gaugesPanel;
    private CombinedConfidenceGauge confidenceGauge;
    private RiskGauge migrationRiskGauge;
    private EffortGauge effortScoreGauge;

    // Explanation panels for grid layout (column 2)
    private JPanel riskExplanationPanel;
    private JPanel effortExplanationPanel;
    private JPanel confidenceExplanationPanel;

    // Clickable bullet labels for explanations
    private JLabel riskDirectDepsLabel;
    private JLabel riskTransitiveLabel;
    private JLabel riskPlatformsLabel;
    private JLabel riskSourceIssuesLabel;
    private JLabel riskConfigIssuesLabel;

    private JLabel effortRecipesLabel;
    private JLabel effortOrgDepsLabel;
    
    private JLabel effortProjectSizeLabel;

    private JLabel confidenceScansLabel;
    private JLabel confidenceUnknownDepsLabel;
    
    // Validation confidence labels
    private JLabel validationUnitTestCoverageLabel;
    private JLabel validationIntegrationTestsLabel;
    private JLabel validationCriticalModulesLabel;

    

    // Tab switcher callback for navigation
    private Consumer<String> tabSwitcher;

    // Default values for calculations (previously configured via sliders)
    private static final int DEFAULT_TEAM_SIZE = 5;
    private static final int DEFAULT_TEST_COVERAGE = 50;
    
    // UI Components for progress section (middle)
    private JPanel progressPanel;
    private JProgressBar mainScanProgressBar;
    private JBLabel mainScanProgressLabel;
    
    // UI Components for results section with sub-panels
    private JPanel resultsPanel;
    private JPanel basicResultsPanel;
    private JPanel platformResultsPanel;
    private JPanel advancedResultsPanel;
    private JBLabel scanProgressValue;
    private JBLabel dependenciesFoundValue;
    private JBLabel basicDependenciesValue;
    private JBLabel detectedPlatformsValue;
    
    // Comprehensive Summary Components
    private JBLabel totalBasicIssuesValue;
    private JBLabel totalAdvancedIssuesValue;
    private JBLabel totalPlatformArtifactsValue;
    private JBLabel grandTotalValue;
    private JBLabel deploymentWarCountValue;
    private JBLabel deploymentEarCountValue;
    private JBLabel deploymentJarCountValue;
    private JBLabel totalDeploymentCountValue;
    
    // New Dependency Count Components
    private JBLabel organisationalDepsValue;
    private JBLabel noJakartaEquivalentValue;
    private JBLabel jakartaUpgradeValue;
    private JBLabel jakartaCompatibleValue;
    private JBLabel unknownReviewValue;
    private JBLabel transitiveDepsValue;
    
    // Project Size Component
    private JBLabel projectSizeValue;
    
    // UI Components for scan results table (removed - was integrated into basicResultsPanel)
    // Note: scan results now displayed directly in basicResultsPanel as count labels
    
    // Legacy components for backward compatibility
    private JPanel metricsTablePanel;

    // MCP Server Status components
    private JPanel mcpStatusPanel;
    private JBLabel mcpStatusIndicator;
    private JBLabel mcpStatusValue;
    private JBLabel mcpToolsValue;
    private JBLabel mcpServerVersionValue;

// Advanced Scan Counts components (Premium)
    private JBLabel jpaScanCountValue;
    private JBLabel beanValidationScanCountValue;
    private JBLabel servletJspScanCountValue;
    private JBLabel cdiInjectionScanCountValue;
    private JBLabel buildConfigScanCountValue;
    private JBLabel restSoapScanCountValue;
    private JBLabel deprecatedApiScanCountValue;
    private JBLabel securityApiScanCountValue;
    private JBLabel jmsMessagingScanCountValue;
    private JBLabel transitiveDependencyScanCountValue;
    private JBLabel configFileScanCountValue;
    private JBLabel classloaderModuleScanCountValue;
    private JBLabel loggingMetricsScanCountValue;
    private JBLabel serializationCacheScanCountValue;
    private JBLabel thirdPartyLibScanCountValue;
    private JBLabel totalAdvancedScanCountValue;

// Status indicator panel
    private JPanel statusPanel;
    
    // Progress bar for advanced scans
    private JProgressBar advancedScanProgressBar;
    private JLabel advancedScanProgressLabel;

    // Analyse button - instance field for external control
    private JButton analyseButton;
    
    // External progress components (for updates from MigrationToolWindow)
    private JProgressBar externalProgressBar;
    private JLabel externalProgressLabel;
    private JButton externalAnalyzeButton;

    public DashboardComponent(@NotNull Project project, AdvancedScanningService advancedScanningService,
            Consumer<ActionEvent> onAnalyze) {
        this.project = project;
        this.onAnalyze = onAnalyze;
        this.advancedScanningService = advancedScanningService;
        this.creditsService = new CreditsService();
        this.truncationHelper = new TruncationHelper();
        this.enhancedTestCoverageService = EnhancedTestCoverageAnalysisService.getInstance();
        this.panel = new JBPanel<>(new BorderLayout());
        initializeComponent();
    }
    
    /**
     * Sets external UI components for progress updates.
     * This allows MigrationToolWindow to control the progress bar and analyze button.
     */
    public void setExternalProgressComponents(JProgressBar progressBar, JLabel progressLabel, JButton analyzeButton) {
        this.externalProgressBar = progressBar;
        this.externalProgressLabel = progressLabel;
        this.externalAnalyzeButton = analyzeButton;
    }
    
    /**
     * Sets the migration dashboard data for this component.
     */
    public void setDashboard(MigrationDashboard dashboard) {
        this.dashboard = dashboard;

        // Update all components with new data
        updateGauges();
        updateSummary();
    }
    
    /**
     * Gets the current migration dashboard data.
     */
    public MigrationDashboard getDashboard() {
        return dashboard;
    }

    private void initializeComponent() {
        // Main content panel with vertical layout - wrapped in scroll pane
        JPanel contentPanel = new JBPanel<>(new BorderLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Title with version
        JPanel titlePanel = new JBPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel titleLabel = new JLabel("Migration Dashboard", SwingConstants.LEFT);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        titlePanel.add(titleLabel);

        // Add version - show we're using the latest build
        JLabel versionLabel = new JLabel("(timestamp build)");
        versionLabel.setForeground(new Color(100, 100, 100));
        versionLabel.setFont(versionLabel.getFont().deriveFont(Font.ITALIC, 10f));
        titlePanel.add(versionLabel);
        contentPanel.add(titlePanel, BorderLayout.NORTH);

        // Main dashboard content with multiple sections using BoxLayout for vertical stacking
        JPanel mainPanel = new JBPanel<>();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        // Top: Gauges (Risk Assessment)
        gaugesPanel = createGaugesPanel();
        gaugesPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(gaugesPanel);

        // Bottom: Results Panel (Comprehensive Scan Summary with sub-panels)
        JPanel resultsPanel = createResultsPanel();
        resultsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(resultsPanel);

        contentPanel.add(mainPanel, BorderLayout.CENTER);

        // Wrap content in scroll pane for vertical scrolling
        JBScrollPane scrollPane = new JBScrollPane(contentPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        panel.add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Updates the advanced scan counts from cached results.
     * Should be called when the dashboard is shown or after scans complete.
     * Now also recalculates and refreshes the risk score with advanced scan findings.
     */
    public void updateAdvancedScanCounts() {
        if (advancedScanningService == null || !advancedScanningService.hasCachedResults()) {
            // No cached results, initialize with zeros
            resetAdvancedScanCounts();
            return;
        }

        AdvancedScanningService.AdvancedScanSummary summary = advancedScanningService.getCachedSummary();
        if (summary == null) {
            resetAdvancedScanCounts();
            return;
        }

        SwingUtilities.invokeLater(() -> {
            updateScanCountWithColor(jpaScanCountValue, summary.getJpaCount());
            updateScanCountWithColor(beanValidationScanCountValue, summary.getBeanValidationCount());
            updateScanCountWithColor(servletJspScanCountValue, summary.getServletJspCount());
            updateScanCountWithColor(cdiInjectionScanCountValue, summary.getCdiInjectionCount());
            updateScanCountWithColor(buildConfigScanCountValue, summary.getBuildConfigCount());
            updateScanCountWithColor(restSoapScanCountValue, summary.getRestSoapCount());
            updateScanCountWithColor(deprecatedApiScanCountValue, summary.getDeprecatedApiCount());
            updateScanCountWithColor(securityApiScanCountValue, summary.getSecurityApiCount());
            updateScanCountWithColor(jmsMessagingScanCountValue, summary.getJmsMessagingCount());
            updateScanCountWithColor(transitiveDependencyScanCountValue, summary.getTransitiveDependencyCount());
            updateScanCountWithColor(configFileScanCountValue, summary.getConfigFileCount());
            updateScanCountWithColor(classloaderModuleScanCountValue, summary.getClassloaderModuleCount());
            updateScanCountWithColor(loggingMetricsScanCountValue, summary.getLoggingMetricsCount());
            updateScanCountWithColor(serializationCacheScanCountValue, summary.getSerializationCacheCount());
            updateScanCountWithColor(thirdPartyLibScanCountValue, summary.getThirdPartyLibCount());
            updateScanCountWithColor(totalAdvancedScanCountValue, summary.getTotalIssuesFound());
            
            // Re-calculate risk score with advanced scan findings
            // This ensures the dashboard UI refreshes with the updated risk score
            // Note: updateGauges() already handles risk score calculation
            if (dashboard != null) {
                // Risk score is now calculated in updateGauges() method
            }
            
            // Update new dashboard components with advanced scan data
            updateGauges();
            updateSummary();
        });
    }
    
    /**
     * Helper method to get detected platforms from platforms tab component
     */
    private List<String> getDetectedPlatformsFromTab() {
        try {
            if (platformsTabComponent != null) {
                // This would need to be implemented based on the actual platforms tab
                // For now, return empty list - the actual platform detection
                // happens in the PlatformsTabComponent itself
                return new ArrayList<>();
            }
            return new ArrayList<>();
        } catch (Exception e) {
            LOG.warn("Could not get detected platforms: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Updates the progress bar for advanced scans.
     * @param completed Number of scans completed
     * @param total Total number of scans
     */
    public void updateAdvancedScanProgress(int completed, int total) {
        SwingUtilities.invokeLater(() -> {
            if (advancedScanProgressBar != null && total > 0) {
                int percentage = (completed * 100) / total;
                advancedScanProgressBar.setValue(percentage);
                advancedScanProgressBar.setString(completed + " / " + total + " scans");
            }
        });
    }

    private void updateScanCountWithColor(JBLabel label, int count) {
        if (label != null) {
            label.setText(String.valueOf(count));
            if (count > 0) {
                label.setForeground(new Color(220, 53, 69)); // Red for issues
            } else {
                label.setForeground(new Color(40, 167, 69)); // Green for no issues
            }
        }
    }

    /**
     * Resets all advanced scan counts to zero.
     */
private void resetAdvancedScanCounts() {
        SwingUtilities.invokeLater(() -> {
            resetScanCountToZero(jpaScanCountValue);
            resetScanCountToZero(beanValidationScanCountValue);
            resetScanCountToZero(servletJspScanCountValue);
            resetScanCountToZero(cdiInjectionScanCountValue);
            resetScanCountToZero(buildConfigScanCountValue);
            resetScanCountToZero(restSoapScanCountValue);
            resetScanCountToZero(deprecatedApiScanCountValue);
            resetScanCountToZero(securityApiScanCountValue);
            resetScanCountToZero(jmsMessagingScanCountValue);
            resetScanCountToZero(transitiveDependencyScanCountValue);
            resetScanCountToZero(configFileScanCountValue);
            resetScanCountToZero(classloaderModuleScanCountValue);
            resetScanCountToZero(loggingMetricsScanCountValue);
            resetScanCountToZero(serializationCacheScanCountValue);
            resetScanCountToZero(thirdPartyLibScanCountValue);
            resetScanCountToZero(totalAdvancedScanCountValue);
        });
    }

    private void resetScanCountToZero(JBLabel label) {
        if (label != null) {
            label.setText("?");
            label.setForeground(Color.GRAY); // Grey for unscanned
        }
    }

    private JPanel createMcpStatusPanel() {
        mcpStatusPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 10, 5));
        mcpStatusPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        mcpStatusPanel.setBackground(new Color(245, 245, 250));

        // Title
        JLabel mcpTitleLabel = new JLabel("MCP Server:");
        mcpTitleLabel.setFont(mcpTitleLabel.getFont().deriveFont(Font.BOLD, 11f));
        mcpStatusPanel.add(mcpTitleLabel);

        // Status indicator dot
        mcpStatusIndicator = new JBLabel("●");
        mcpStatusIndicator.setFont(mcpStatusIndicator.getFont().deriveFont(Font.BOLD, 14f));
        mcpStatusIndicator.setForeground(Color.GRAY);
        mcpStatusPanel.add(mcpStatusIndicator);

        // Status value
        mcpStatusValue = new JBLabel("Checking...");
        mcpStatusValue.setFont(mcpStatusValue.getFont().deriveFont(Font.PLAIN, 11f));
        mcpStatusPanel.add(mcpStatusValue);

        // Separator
        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        separator.setPreferredSize(new Dimension(1, 20));
        separator.setForeground(Color.LIGHT_GRAY);
        mcpStatusPanel.add(separator);

        // Tools count
        JLabel toolsLabel = new JLabel("AI Tools:");
        toolsLabel.setFont(toolsLabel.getFont().deriveFont(Font.PLAIN, 11f));
        mcpStatusPanel.add(toolsLabel);

        mcpToolsValue = new JBLabel("-");
        mcpToolsValue.setFont(mcpToolsValue.getFont().deriveFont(Font.BOLD, 11f));
        mcpStatusPanel.add(mcpToolsValue);

        // Separator
        JSeparator separator2 = new JSeparator(SwingConstants.VERTICAL);
        separator2.setPreferredSize(new Dimension(1, 20));
        separator2.setForeground(Color.LIGHT_GRAY);
        mcpStatusPanel.add(separator2);

        // Server version
        JLabel versionLabel = new JLabel("Version:");
        versionLabel.setFont(versionLabel.getFont().deriveFont(Font.PLAIN, 11f));
        mcpStatusPanel.add(versionLabel);

        mcpServerVersionValue = new JBLabel("-");
        mcpServerVersionValue.setFont(mcpServerVersionValue.getFont().deriveFont(Font.PLAIN, 11f));
        mcpStatusPanel.add(mcpServerVersionValue);

        return mcpStatusPanel;
    }

    /**
     * Updates the MCP server status display.
     * Called on initialization and can be called to refresh.
     */
    public void updateMcpServerStatus() {
        SwingUtilities.invokeLater(() -> {
            // Check if MCP server is premium-only and user is not premium
            boolean mcpServerPremiumOnly = adrianmikula.jakartamigration.intellij.config.FeatureFlags.getInstance().isMcpServerPremiumOnly();
            boolean isPremium = adrianmikula.jakartamigration.intellij.license.CheckLicense.isLicensed();
            
            if (mcpServerPremiumOnly && !isPremium) {
                // MCP server is premium-only but user is not premium
                if (mcpStatusIndicator != null) {
                    mcpStatusIndicator.setForeground(new Color(255, 140, 0)); // Orange for premium required
                }
                if (mcpStatusValue != null) {
                    mcpStatusValue.setText("Premium Only");
                    mcpStatusValue.setForeground(new Color(255, 140, 0));
                }
                if (mcpToolsValue != null) {
                    mcpToolsValue.setText("🔒");
                    mcpToolsValue.setForeground(new Color(255, 140, 0));
                }
                if (mcpServerVersionValue != null) {
                    mcpServerVersionValue.setText("-");
                }
                LOG.info("MCP Server Status: Premium Only - user is not premium and MCP server is premium-only feature");
                return;
            }

            JakartaMcpServerProvider provider = JakartaMcpRegistrationActivity.getServerProvider();

            if (provider != null && provider.isReady()) {
                // MCP is connected and ready
                if (mcpStatusIndicator != null) {
                    mcpStatusIndicator.setForeground(new Color(0, 180, 0));
                }
                if (mcpStatusValue != null) {
                    mcpStatusValue.setText("Connected");
                    mcpStatusValue.setForeground(new Color(0, 120, 0));
                }

                int toolCount = provider.getToolCount();
                if (mcpToolsValue != null) {
                    mcpToolsValue.setText(String.valueOf(toolCount));
                    mcpToolsValue.setForeground(new Color(0, 100, 200));
                }

                if (mcpServerVersionValue != null) {
                    mcpServerVersionValue.setText(provider.getServerVersion());
                }

                LOG.info("MCP Server Status: Connected with " + toolCount + " tools");
            } else if (provider != null) {
                // MCP provider exists but not ready
                if (mcpStatusIndicator != null) {
                    mcpStatusIndicator.setForeground(Color.ORANGE);
                }
                if (mcpStatusValue != null) {
                    mcpStatusValue.setText("Initializing");
                    mcpStatusValue.setForeground(Color.ORANGE);
                }

                if (mcpToolsValue != null) {
                    mcpToolsValue.setText("-");
                    mcpToolsValue.setForeground(Color.GRAY);
                }

                if (mcpServerVersionValue != null) {
                    mcpServerVersionValue.setText(provider.getServerVersion());
                }

                LOG.info("MCP Server Status: Provider exists but not ready");
            } else {
                // MCP provider not initialized - check if AI Assistant is available
                if (mcpStatusIndicator != null) {
                    mcpStatusIndicator.setForeground(Color.GRAY);
                }
                if (mcpStatusValue != null) {
                    mcpStatusValue.setText("Not Available");
                    mcpStatusValue.setForeground(Color.GRAY);
                }

                if (mcpToolsValue != null) {
                    mcpToolsValue.setText("-");
                }
                if (mcpServerVersionValue != null) {
                    mcpServerVersionValue.setText("1.0.0");
                }

                LOG.info("MCP Server Status: Provider not initialized - AI Assistant may not be active");
            }
        });
    }

    /**
     * Gets the current MCP server status as a string.
     * 
     * @return "Connected", "Not Ready", or "Not Initialized"
     */
    public String getMcpStatus() {
        JakartaMcpServerProvider provider = JakartaMcpRegistrationActivity.getServerProvider();
        if (provider != null && provider.isReady()) {
            return "Connected";
        } else if (provider != null) {
            return "Not Ready";
        } else {
            return "Not Initialized";
        }
    }

    /**
     * Gets the number of loaded MCP tools.
     * 
     * @return Number of tools, or 0 if not ready
     */
    public int getMcpToolCount() {
        JakartaMcpServerProvider provider = JakartaMcpRegistrationActivity.getServerProvider();
        if (provider != null && provider.isReady()) {
            return provider.getToolCount();
        }
        return 0;
    }

    private JBLabel createKeyLabel(String text) {
        JBLabel label = new JBLabel(text, SwingConstants.LEFT);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 12f));
        return label;
    }

    private JBLabel createValueLabel(String text) {
        JBLabel label = new JBLabel(text, SwingConstants.LEFT);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
        return label;
    }

    /**
     * Creates a titled border with transparent border lines.
     * Keeps the title text but makes the border itself invisible.
     */
    private javax.swing.border.Border createTransparentTitledBorder(String title) {
        // Create an empty border (no visible lines)
        javax.swing.border.Border emptyBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);
        // Create a titled border with the empty border
        javax.swing.border.TitledBorder titledBorder = BorderFactory.createTitledBorder(emptyBorder, title);
        titledBorder.setTitleFont(titledBorder.getTitleFont().deriveFont(Font.BOLD, 12f));
        return titledBorder;
    }

    
    public JPanel getPanel() {
        return panel;
    }

    /**
     * Sets the analysis running state - disables/enables button and updates progress UI.
     * Should be called on EDT (Event Dispatch Thread) via SwingUtilities.invokeLater.
     *
     * @param running true if analysis is starting, false when complete
     */
    public void setAnalysisRunning(boolean running) {
        SwingUtilities.invokeLater(() -> {
            // Update internal analyze button (if it exists)
            if (analyseButton != null) {
                analyseButton.setEnabled(!running);
                if (running) {
                    analyseButton.setText("Scanning...");
                    analyseButton.setBackground(new Color(156, 163, 175)); // Gray when disabled
                } else {
                    analyseButton.setText("Analyse");
                    analyseButton.setBackground(new Color(59, 130, 246)); // Blue when enabled
                }
            }
            
            // Update external analyze button (from MigrationToolWindow)
            if (externalAnalyzeButton != null) {
                externalAnalyzeButton.setEnabled(!running);
                if (running) {
                    externalAnalyzeButton.setText("Scanning...");
                    externalAnalyzeButton.setBackground(new Color(156, 163, 175)); // Gray when disabled
                } else {
                    externalAnalyzeButton.setText("▶ Analyze Project");
                    externalAnalyzeButton.setBackground(new Color(59, 130, 246)); // Blue when enabled
                }
            }
            
            // Update internal progress bar (if it exists)
            if (mainScanProgressBar != null) {
                mainScanProgressBar.setIndeterminate(running);
                if (running) {
                    mainScanProgressBar.setString("Scanning in progress... Please wait");
                    mainScanProgressLabel.setText(""); // Clear external label since text is now in progress bar
                } else {
                    mainScanProgressBar.setValue(100);
                    mainScanProgressBar.setString("Scan complete");
                    mainScanProgressLabel.setText(""); // Clear external label since text is now in progress bar
                }
            }
            
            // Update external progress bar (from MigrationToolWindow)
            if (externalProgressBar != null) {
                externalProgressBar.setIndeterminate(running);
                if (running) {
                    externalProgressBar.setString("Scanning in progress... Please wait");
                    if (externalProgressLabel != null) {
                        externalProgressLabel.setText(""); // Clear external label since text is now in progress bar
                    }
                } else {
                    externalProgressBar.setValue(100);
                    externalProgressBar.setString("Scan complete");
                    if (externalProgressLabel != null) {
                        externalProgressLabel.setText(""); // Clear external label since text is now in progress bar
                    }
                }
            }
        });
    }

    public JBLabel getJpaScanCountValue() {
        return jpaScanCountValue;
    }

    public JBLabel getBeanValidationScanCountValue() {
        return beanValidationScanCountValue;
    }

    public JBLabel getServletJspScanCountValue() {
        return servletJspScanCountValue;
    }

    public JBLabel getMcpStatusValue() {
        return mcpStatusValue;
    }

    public JBLabel getCdiInjectionScanCountValue() {
        return cdiInjectionScanCountValue;
    }

    public JBLabel getBuildConfigScanCountValue() {
        return buildConfigScanCountValue;
    }

    public JBLabel getRestSoapScanCountValue() {
        return restSoapScanCountValue;
    }

    // ==================== ScanProgressListener Implementation ====================

    @Override
    public void onScanPhase(String phase, int completed, int total) {
        SwingUtilities.invokeLater(() -> {
            // Update internal progress bar (if it exists)
            if (mainScanProgressBar != null && mainScanProgressLabel != null) {
                if (total > 0) {
                    // Show determinate progress
                    mainScanProgressBar.setIndeterminate(false);
                    int percentage = (completed * 100) / total;
                    mainScanProgressBar.setValue(percentage);
                    mainScanProgressBar.setString(phase + " (" + completed + "/" + total + ") - " + phase + " in progress...");
                    mainScanProgressLabel.setText(""); // Clear external label since text is now in progress bar
                } else {
                    // Show indeterminate progress for unknown total
                    mainScanProgressBar.setIndeterminate(true);
                    mainScanProgressBar.setString(phase + " in progress...");
                    mainScanProgressLabel.setText(""); // Clear external label since text is now in progress bar
                }
            }
            
            // Update external progress bar (from MigrationToolWindow)
            if (externalProgressBar != null && externalProgressLabel != null) {
                if (total > 0) {
                    // Show determinate progress
                    externalProgressBar.setIndeterminate(false);
                    int percentage = (completed * 100) / total;
                    externalProgressBar.setValue(percentage);
                    externalProgressBar.setString(phase + " (" + completed + "/" + total + ") - " + phase + " in progress...");
                    externalProgressLabel.setText(""); // Clear external label since text is now in progress bar
                } else {
                    // Show indeterminate progress for unknown total
                    externalProgressBar.setIndeterminate(true);
                    externalProgressBar.setString(phase + " in progress...");
                    externalProgressLabel.setText(""); // Clear external label since text is now in progress bar
                }
            }
        });
    }

    @Override
    public void onScanComplete() {
        SwingUtilities.invokeLater(() -> {
            LOG.info("DashboardComponent: Scan completed, updating UI");
            
            // Set analysis running to false to re-enable the button
            setAnalysisRunning(false);
            
            // Update all dashboard components with latest data
            updateGauges();
            updateSummary();
            updateAdvancedScanCounts();
            
            // Update MCP server status
            updateMcpServerStatus();
            
            LOG.info("DashboardComponent: UI update completed");
        });
    }

    @Override
    public void onScanError(Exception error) {
        SwingUtilities.invokeLater(() -> {
            LOG.error("DashboardComponent: Scan failed", error);
            
            // Set analysis running to false to re-enable the button
            setAnalysisRunning(false);
            
            // Show error in progress bar instead of external label
            if (mainScanProgressBar != null) {
                mainScanProgressBar.setIndeterminate(false);
                mainScanProgressBar.setValue(0);
                mainScanProgressBar.setString("Scan failed - Error");
            }
            if (mainScanProgressLabel != null) {
                mainScanProgressLabel.setText(""); // Clear external label since error is now in progress bar
            }
            
            // Show error in external progress bar
            if (externalProgressBar != null) {
                externalProgressBar.setIndeterminate(false);
                externalProgressBar.setValue(0);
                externalProgressBar.setString("Scan failed - Error");
            }
            if (externalProgressLabel != null) {
                externalProgressLabel.setText(""); // Clear external label since error is now in progress bar
            }
            
            // Log the error for debugging
            LOG.error("Scan error details:", error);
        });
    }

    @Override
    public void onSubScanComplete(String scanType, int resultCount) {
        SwingUtilities.invokeLater(() -> {
            // Update specific scan count if the label exists
            switch (scanType) {
                case "JPA":
                    if (jpaScanCountValue != null) {
                        updateScanCountWithColor(jpaScanCountValue, resultCount);
                    }
                    break;
                case "Bean Validation":
                    if (beanValidationScanCountValue != null) {
                        updateScanCountWithColor(beanValidationScanCountValue, resultCount);
                    }
                    break;
                case "Servlet/JSP":
                    if (servletJspScanCountValue != null) {
                        updateScanCountWithColor(servletJspScanCountValue, resultCount);
                    }
                    break;
                case "CDI Injection":
                    if (cdiInjectionScanCountValue != null) {
                        updateScanCountWithColor(cdiInjectionScanCountValue, resultCount);
                    }
                    break;
                case "Build Config":
                    if (buildConfigScanCountValue != null) {
                        updateScanCountWithColor(buildConfigScanCountValue, resultCount);
                    }
                    break;
                case "REST/SOAP":
                    if (restSoapScanCountValue != null) {
                        updateScanCountWithColor(restSoapScanCountValue, resultCount);
                    }
                    break;
                default:
                    // For other scan types, just log the completion
                    LOG.info("Sub-scan completed: " + scanType + " with " + resultCount + " results");
                    break;
            }
        });
    }

    // ==================== New Dashboard Layout Methods ====================

    /**
     * Creates the top section with a 2×3 grid layout.
     * Column 1: Three speedometer-style gauges (one per row)
     * Column 2: Explanatory breakdown panels with clickable bullet links
     *
     * Row 1: Migration Risk Gauge + Risk Breakdown
     * Row 2: Migration Effort Gauge + Effort Breakdown
     * Row 3: Confidence Score Gauge + Confidence Breakdown
     */
    private JPanel createGaugesPanel() {
        JPanel panel = new JBPanel<>(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                createTransparentTitledBorder("Risk Assessment"),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        // Main grid container: 2 columns × 4 rows
        JPanel gridContainer = new JBPanel<>(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 15, 10, 15);
        gbc.fill = GridBagConstraints.NONE;

        // Row 1: Migration Risk
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 0.5;
        gbc.anchor = GridBagConstraints.CENTER;
        migrationRiskGauge = new RiskGauge("Migration Risk");
        gridContainer.add(migrationRiskGauge, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        riskExplanationPanel = createRiskExplanationPanel();
        gridContainer.add(riskExplanationPanel, gbc);

        // Row 2: Migration Effort
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        effortScoreGauge = new EffortGauge("Migration Effort");
        gridContainer.add(effortScoreGauge, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        effortExplanationPanel = createEffortExplanationPanel();
        gridContainer.add(effortExplanationPanel, gbc);

        // Row 3: Confidence Score
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        confidenceGauge = new CombinedConfidenceGauge("Confidence");
        gridContainer.add(confidenceGauge, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        confidenceExplanationPanel = createCombinedConfidenceExplanationPanel();
        gridContainer.add(confidenceExplanationPanel, gbc);

        // Row 4: Validation Confidence
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.CENTER;
        

        panel.add(gridContainer, BorderLayout.CENTER);

        // Slider panel for effort estimation inputs
        JPanel slidersPanel = createSlidersPanel();
        panel.add(slidersPanel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Creates the project info panel showing project metadata.
     */
    private JPanel createSlidersPanel() {
        JPanel panel = new JBPanel<>(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 5, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.anchor = GridBagConstraints.WEST;

        // Project Size
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel projectSizeLabel = new JLabel("Project Size:");
        projectSizeLabel.setFont(projectSizeLabel.getFont().deriveFont(Font.PLAIN, 11f));
        panel.add(projectSizeLabel, gbc);

        projectSizeValue = createValueLabel("-");
        gbc.gridx = 1;
        panel.add(projectSizeValue, gbc);

        return panel;
    }

    /**
     * Creates the risk explanation panel with clickable bullet links.
     * Shows breakdown of factors contributing to risk score.
     */
    private JPanel createRiskExplanationPanel() {
        JPanel panel = new JBPanel<>(new GridLayout(5, 1, 2, 2));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // Create clickable bullet labels with initial values
        riskDirectDepsLabel = createClickableBullet("Direct dependencies needing upgrade", "Dependencies", 0);
        riskTransitiveLabel = createClickableBullet("Transitive dependency issues", "Dependencies", 0);
        riskPlatformsLabel = createClickableBullet("Platforms needing upgrade", "Platforms", 0);
        riskSourceIssuesLabel = createClickableBullet("Source code issues", "Advanced Scans", 0);
        riskConfigIssuesLabel = createClickableBullet("Config/non-source issues", "Advanced Scans", 0);

        panel.add(riskDirectDepsLabel);
        panel.add(riskTransitiveLabel);
        panel.add(riskPlatformsLabel);
        panel.add(riskSourceIssuesLabel);
        panel.add(riskConfigIssuesLabel);

        return panel;
    }

    /**
     * Creates the effort explanation panel with clickable bullet links.
     * Shows breakdown of factors contributing to effort score.
     */
    private JPanel createEffortExplanationPanel() {
        JPanel panel = new JBPanel<>(new GridLayout(3, 1, 2, 2));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // Create clickable bullet labels with initial values
        effortRecipesLabel = createClickableBullet("Refactors with recipes", "Refactor", 0);
        effortProjectSizeLabel = createClickableBullet("Project complexity (files)", "Dependencies", 0);
        effortOrgDepsLabel = createClickableBullet("Organisational dependencies", "Dependencies", 0);

        panel.add(effortRecipesLabel);
        panel.add(effortProjectSizeLabel);
        panel.add(effortOrgDepsLabel);

        return panel;
    }

    /**
     * Creates the confidence explanation panel with clickable bullet links.
     * Shows breakdown of factors contributing to confidence score.
     */
    private JPanel createCombinedConfidenceExplanationPanel() {
        JPanel panel = new JBPanel<>(new GridLayout(2, 1, 2, 2));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // Create clickable bullet labels with initial values
        confidenceScansLabel = createClickableBullet("Scans completed", "Advanced Scans", 0);
        confidenceUnknownDepsLabel = createClickableBullet("Unknown/review dependencies", "Dependencies", 0);

        panel.add(confidenceScansLabel);
        panel.add(confidenceUnknownDepsLabel);

        return panel;
    }

    

    /**
     * Creates a clickable bullet label that navigates to a specific tab when clicked.
     *
     * @param labelText Base text for the label (value will be appended)
     * @param targetTab Tab to switch to when clicked
     * @param initialValue Initial value to display
     * @return Configured JLabel with click listener
     */
    private JLabel createClickableBullet(String labelText, String targetTab, int initialValue) {
        JLabel label = new JLabel(formatBulletText(labelText, initialValue));
        label.setCursor(new Cursor(Cursor.HAND_CURSOR));
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 11f));

        // Add click listener for tab navigation
        label.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (tabSwitcher != null) {
                    tabSwitcher.accept(targetTab);
                }
            }

            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                label.setText("<html><u>" + formatBulletText(labelText, extractValue(label.getText())) + "</u></html>");
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                label.setText(formatBulletText(labelText, extractValue(label.getText())));
            }
        });

        return label;
    }

    /**
     * Formats bullet text with proper HTML styling.
     */
    private String formatBulletText(String labelText, int value) {
        return "<html>&bull; " + labelText + ": " + value + "</html>";
    }

    /**
     * Extracts the numeric value from formatted bullet text.
     */
    private int extractValue(String text) {
        if (text == null) return 0;
        // Remove HTML tags and extract the number after the colon
        String plainText = text.replaceAll("<[^>]*>", "");
        String[] parts = plainText.split(":");
        if (parts.length > 1) {
            try {
                return Integer.parseInt(parts[1].trim().replaceAll("\\D", ""));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * Updates a clickable bullet label with new value and color based on severity.
     *
     * @param label The label to update
     * @param baseText The base text (without value)
     * @param value The value to display
     * @param color The color to apply based on severity
     */
    private void updateBulletLabel(JLabel label, String baseText, int value, Color color) {
        String text = formatBulletText(baseText, value);
        label.setText(text);
        label.setForeground(color);
    }

    /**
     * Determines color based on value and thresholds.
     * Returns appropriate color for severity level.
     *
     * @param value The metric value
     * @param thresholds Array of 3 threshold values: [green/yellow, yellow/orange, orange/red]
     * @param isPositiveMetric If true, higher values are good (colors inverted)
     * @return Color based on severity
     */
    private Color getColorForMetric(int value, int[] thresholds, boolean isPositiveMetric) {
        // Define colors
        Color green = new Color(40, 167, 69);
        Color yellow = new Color(255, 193, 7);
        Color orange = new Color(255, 165, 0);
        Color red = new Color(220, 53, 69);

        if (isPositiveMetric) {
            // Higher is better (e.g., test coverage)
            if (value >= thresholds[2]) return green;
            if (value >= thresholds[1]) return yellow;
            if (value >= thresholds[0]) return orange;
            return red;
        } else {
            // Lower is better (e.g., issues count)
            if (value <= thresholds[0]) return green;
            if (value <= thresholds[1]) return yellow;
            if (value <= thresholds[2]) return orange;
            return red;
        }
    }

    /**
     * Creates the results panel container with basic, platform, and advanced sub-panels.
     */
    private JPanel createResultsPanel() {
        JPanel panel = new JBPanel<>(new GridBagLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                createTransparentTitledBorder("Comprehensive Scan Summary"),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        // Top section: Basic and Platform panels side by side
        JPanel topSection = new JBPanel<>(new GridBagLayout());
        GridBagConstraints topGbc = new GridBagConstraints();
        topGbc.insets = new Insets(0, 0, 0, 5);
        topGbc.fill = GridBagConstraints.BOTH;
        topGbc.anchor = GridBagConstraints.NORTHWEST;

        // Basic Results (takes more space)
        basicResultsPanel = createBasicResultsPanel();
        topGbc.gridx = 0;
        topGbc.gridy = 0;
        topGbc.weightx = 0.6;
        topGbc.weighty = 1.0;
        topSection.add(basicResultsPanel, topGbc);

        // Platform Results
        platformResultsPanel = createPlatformResultsPanel();
        topGbc.gridx = 1;
        topGbc.weightx = 0.4;
        topGbc.insets = new Insets(0, 5, 0, 0);
        topSection.add(platformResultsPanel, topGbc);

        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 0.5;
        panel.add(topSection, gbc);

        // Bottom section: Advanced Results
        advancedResultsPanel = createAdvancedResultsPanel();
        gbc.gridy = 1;
        gbc.weighty = 0.5;
        gbc.insets = new Insets(10, 5, 5, 5);
        panel.add(advancedResultsPanel, gbc);

        return panel;
    }

    /**
     * Creates the basic results panel showing dependency counts and scan results table.
     */
    private JPanel createBasicResultsPanel() {
        JPanel panel = new JBPanel<>(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                createTransparentTitledBorder("Basic Scan Results"),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        panel.setMinimumSize(new Dimension(300, 200));

        // Top: Summary counts
        JPanel summaryGrid = new JBPanel<>(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Scan Progress
        gbc.gridx = 0; gbc.gridy = 0;
        summaryGrid.add(createKeyLabel("Scan Progress:"), gbc);
        gbc.gridx = 1;
        scanProgressValue = createValueLabel("0%");
        summaryGrid.add(scanProgressValue, gbc);

        // Dependencies Found
        gbc.gridx = 0; gbc.gridy = 1;
        summaryGrid.add(createKeyLabel("Dependencies Found:"), gbc);
        gbc.gridx = 1;
        dependenciesFoundValue = createValueLabel("-");
        summaryGrid.add(dependenciesFoundValue, gbc);

        // Basic Dependencies
        gbc.gridx = 0; gbc.gridy = 2;
        summaryGrid.add(createKeyLabel("Basic Dependencies:"), gbc);
        gbc.gridx = 1;
        basicDependenciesValue = createValueLabel("-");
        summaryGrid.add(basicDependenciesValue, gbc);

        // Total Basic Issues
        gbc.gridx = 0; gbc.gridy = 3;
        JBLabel totalLabel = createKeyLabel("Total Basic Issues:");
        totalLabel.setFont(totalLabel.getFont().deriveFont(Font.BOLD));
        summaryGrid.add(totalLabel, gbc);
        gbc.gridx = 1;
        totalBasicIssuesValue = createValueLabel("0");
        totalBasicIssuesValue.setFont(totalBasicIssuesValue.getFont().deriveFont(Font.BOLD));
        summaryGrid.add(totalBasicIssuesValue, gbc);

        // Organisational Dependencies
        gbc.gridx = 0; gbc.gridy = 4;
        summaryGrid.add(createKeyLabel("Organisational Deps:"), gbc);
        gbc.gridx = 1;
        organisationalDepsValue = createValueLabel("0");
        summaryGrid.add(organisationalDepsValue, gbc);

        // No Jakarta Equivalent
        gbc.gridx = 0; gbc.gridy = 5;
        summaryGrid.add(createKeyLabel("No Jakarta Equivalent:"), gbc);
        gbc.gridx = 1;
        noJakartaEquivalentValue = createValueLabel("0");
        summaryGrid.add(noJakartaEquivalentValue, gbc);

        // Jakarta Upgrade
        gbc.gridx = 0; gbc.gridy = 6;
        summaryGrid.add(createKeyLabel("Jakarta Upgrade:"), gbc);
        gbc.gridx = 1;
        jakartaUpgradeValue = createValueLabel("0");
        summaryGrid.add(jakartaUpgradeValue, gbc);

        // Jakarta Compatible
        gbc.gridx = 0; gbc.gridy = 7;
        summaryGrid.add(createKeyLabel("Jakarta Compatible:"), gbc);
        gbc.gridx = 1;
        jakartaCompatibleValue = createValueLabel("0");
        summaryGrid.add(jakartaCompatibleValue, gbc);

        // Unknown/Review
        gbc.gridx = 0; gbc.gridy = 8;
        summaryGrid.add(createKeyLabel("Unknown/Review:"), gbc);
        gbc.gridx = 1;
        unknownReviewValue = createValueLabel("0");
        summaryGrid.add(unknownReviewValue, gbc);

        // Transitive Dependencies
        gbc.gridx = 0; gbc.gridy = 9;
        summaryGrid.add(createKeyLabel("Transitive Dependencies:"), gbc);
        gbc.gridx = 1;
        transitiveDepsValue = createValueLabel("0");
        summaryGrid.add(transitiveDepsValue, gbc);

        // Separator before grand total
        gbc.gridx = 0; gbc.gridy = 10; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 5, 5, 5);
        JSeparator separator = new JSeparator();
        separator.setForeground(Color.GRAY);
        summaryGrid.add(separator, gbc);
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Grand Total across all scan types
        gbc.gridx = 0; gbc.gridy = 12;
        JBLabel grandTotalLabel = createKeyLabel("GRAND TOTAL:");
        grandTotalLabel.setFont(grandTotalLabel.getFont().deriveFont(Font.BOLD, 13f));
        grandTotalLabel.setForeground(new Color(0, 100, 200));
        summaryGrid.add(grandTotalLabel, gbc);
        gbc.gridx = 1;
        grandTotalValue = createValueLabel("0");
        grandTotalValue.setFont(grandTotalValue.getFont().deriveFont(Font.BOLD, 13f));
        grandTotalValue.setForeground(new Color(0, 100, 200));
        summaryGrid.add(grandTotalValue, gbc);

        panel.add(summaryGrid, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Creates the platform results panel showing deployment artifact counts.
     */
    private JPanel createPlatformResultsPanel() {
        JPanel panel = new JBPanel<>(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                createTransparentTitledBorder("Platform Scan Results"),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        panel.setMinimumSize(new Dimension(200, 200));

        JPanel platformGrid = new JBPanel<>(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.CENTER;

        int row = 0;

        // Detected Platforms
        gbc.gridx = 0; gbc.gridy = row;
        platformGrid.add(createKeyLabel("Detected Platforms:"), gbc);
        gbc.gridx = 1;
        detectedPlatformsValue = createValueLabel("-");
        platformGrid.add(detectedPlatformsValue, gbc);
        row++;

        // WAR Files
        gbc.gridx = 0; gbc.gridy = row;
        platformGrid.add(createKeyLabel("WAR Files:"), gbc);
        gbc.gridx = 1;
        deploymentWarCountValue = createValueLabel("0");
        platformGrid.add(deploymentWarCountValue, gbc);
        row++;

        // EAR Files
        gbc.gridx = 0; gbc.gridy = row;
        platformGrid.add(createKeyLabel("EAR Files:"), gbc);
        gbc.gridx = 1;
        deploymentEarCountValue = createValueLabel("0");
        platformGrid.add(deploymentEarCountValue, gbc);
        row++;

        // JAR Files
        gbc.gridx = 0; gbc.gridy = row;
        platformGrid.add(createKeyLabel("JAR Files:"), gbc);
        gbc.gridx = 1;
        deploymentJarCountValue = createValueLabel("0");
        platformGrid.add(deploymentJarCountValue, gbc);
        row++;

        // Total Artifacts
        gbc.gridx = 0; gbc.gridy = row;
        JBLabel totalArtifactsLabel = createKeyLabel("Total Artifacts:");
        totalArtifactsLabel.setFont(totalArtifactsLabel.getFont().deriveFont(Font.BOLD));
        platformGrid.add(totalArtifactsLabel, gbc);
        gbc.gridx = 1;
        totalDeploymentCountValue = createValueLabel("0");
        totalDeploymentCountValue.setFont(totalDeploymentCountValue.getFont().deriveFont(Font.BOLD));
        platformGrid.add(totalDeploymentCountValue, gbc);
        row++;

        // Separator
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JSeparator separator = new JSeparator();
        separator.setForeground(Color.LIGHT_GRAY);
        platformGrid.add(separator, gbc);
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        row++;

        // Total Platform Artifacts (sum for grand total calculation)
        gbc.gridx = 0; gbc.gridy = row;
        JBLabel platformTotalLabel = createKeyLabel("Platform Issues:");
        platformTotalLabel.setFont(platformTotalLabel.getFont().deriveFont(Font.BOLD));
        platformGrid.add(platformTotalLabel, gbc);
        gbc.gridx = 1;
        totalPlatformArtifactsValue = createValueLabel("0");
        totalPlatformArtifactsValue.setFont(totalPlatformArtifactsValue.getFont().deriveFont(Font.BOLD));
        platformGrid.add(totalPlatformArtifactsValue, gbc);

        panel.add(platformGrid, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Creates the advanced results panel showing all 14 scan type counts.
     */
    private JPanel createAdvancedResultsPanel() {
        JPanel panel = new JBPanel<>(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                createTransparentTitledBorder("Advanced Scan Results"),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        panel.setMinimumSize(new Dimension(300, 150));

        // Use 3-column grid for compact layout
        JPanel scanGrid = new JBPanel<>(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 10, 3, 10);
        gbc.anchor = GridBagConstraints.WEST;

        int[] row = {0};
        int[] col = {0};
        final int MAX_COLS = 3;

        // Helper method reference for adding scan rows
        class ScanRowHelper {
            void add(String labelText, JBLabel labelValue) {
                gbc.gridx = col[0] * 2;
                gbc.gridy = row[0];
                scanGrid.add(createKeyLabel(labelText), gbc);
                gbc.gridx = col[0] * 2 + 1;
                scanGrid.add(labelValue, gbc);
                col[0]++;
                if (col[0] >= MAX_COLS) {
                    col[0] = 0;
                    row[0]++;
                }
            }
        }
        ScanRowHelper addScanRow = new ScanRowHelper();

        // Initialize all advanced scan count labels and add to grid
        jpaScanCountValue = createValueLabel("?");
        addScanRow.add("JPA:", jpaScanCountValue);

        beanValidationScanCountValue = createValueLabel("?");
        addScanRow.add("Bean Validation:", beanValidationScanCountValue);

        servletJspScanCountValue = createValueLabel("?");
        addScanRow.add("Servlet/JSP:", servletJspScanCountValue);

        cdiInjectionScanCountValue = createValueLabel("?");
        addScanRow.add("CDI Injection:", cdiInjectionScanCountValue);

        buildConfigScanCountValue = createValueLabel("?");
        addScanRow.add("Build Config:", buildConfigScanCountValue);

        restSoapScanCountValue = createValueLabel("?");
        addScanRow.add("REST/SOAP:", restSoapScanCountValue);

        deprecatedApiScanCountValue = createValueLabel("?");
        addScanRow.add("Deprecated API:", deprecatedApiScanCountValue);

        securityApiScanCountValue = createValueLabel("?");
        addScanRow.add("Security API:", securityApiScanCountValue);

        jmsMessagingScanCountValue = createValueLabel("?");
        addScanRow.add("JMS Messaging:", jmsMessagingScanCountValue);

        transitiveDependencyScanCountValue = createValueLabel("?");
        addScanRow.add("Transitive Deps:", transitiveDependencyScanCountValue);

        configFileScanCountValue = createValueLabel("?");
        addScanRow.add("Config Files:", configFileScanCountValue);

        classloaderModuleScanCountValue = createValueLabel("?");
        addScanRow.add("Classloader:", classloaderModuleScanCountValue);

        loggingMetricsScanCountValue = createValueLabel("?");
        addScanRow.add("Logging/Metrics:", loggingMetricsScanCountValue);

        serializationCacheScanCountValue = createValueLabel("?");
        addScanRow.add("Serialization:", serializationCacheScanCountValue);

        thirdPartyLibScanCountValue = createValueLabel("?");
        addScanRow.add("Third-Party:", thirdPartyLibScanCountValue);

        // Total Advanced Issues (span across columns)
        if (col[0] > 0) {
            row[0]++; // Move to next row if we're in the middle of a row
        }
        gbc.gridx = 0; gbc.gridy = row[0]; gbc.gridwidth = MAX_COLS * 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JSeparator separator = new JSeparator();
        separator.setForeground(Color.GRAY);
        scanGrid.add(separator, gbc);
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        row[0]++;

        // Total row - Total Advanced Issues
        gbc.gridx = 0; gbc.gridy = row[0]; gbc.gridwidth = 2;
        JBLabel totalLabel = createKeyLabel("Total Advanced Issues:");
        totalLabel.setFont(totalLabel.getFont().deriveFont(Font.BOLD, 12f));
        scanGrid.add(totalLabel, gbc);
        gbc.gridx = 2; gbc.gridwidth = 1;
        totalAdvancedScanCountValue = createValueLabel("?");
        totalAdvancedScanCountValue.setFont(totalAdvancedScanCountValue.getFont().deriveFont(Font.BOLD, 12f));
        scanGrid.add(totalAdvancedScanCountValue, gbc);

        panel.add(scanGrid, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Updates the gauges with current risk scores and confidence score.
     */
    public void updateGauges() {
        if (dashboard == null) {
            return;
        }

        // Calculate confidence score (percentage of dependencies with known Jakarta status)
        int confidenceScore = calculateConfidenceScore();
        confidenceGauge.setScore(confidenceScore);

        // Calculate migration risk score (using RiskScoringService)
        RiskScoringService riskScoringService = RiskScoringService.getInstance();
        Map<String, List<RiskScoringService.RiskFinding>> scanFindings = new HashMap<>();
        Map<String, Integer> depIssues = new HashMap<>();
        
        // Build dependency issues map (excluding jakarta-compatible dependencies)
        DependencySummary depSummary = dashboard.getDependencySummary();
        if (depSummary != null) {
            int noSupport = depSummary.getNoJakartaSupportCount() != null ? depSummary.getNoJakartaSupportCount() : 0;
            int blockers = depSummary.getBlockerDependencies() != null ? depSummary.getBlockerDependencies() : 0;
            int affected = depSummary.getAffectedDependencies() != null ? depSummary.getAffectedDependencies() : 0;
            int transitiveDeps = depSummary.getTransitiveDependencies() != null ? depSummary.getTransitiveDependencies() : 0;
            int jakartaUpgrade = depSummary.getJakartaUpgradeCount() != null ? depSummary.getJakartaUpgradeCount() : 0;

            // Merge blocker and no_jakarta_upgrade into single category
            int mergedNoUpgrade = noSupport + blockers;
            if (mergedNoUpgrade > 0) {
                depIssues.put("noJakartaUpgrade", mergedNoUpgrade * 25);
            }
            if (affected > 0) {
                depIssues.put("directDependency", affected * 10);
            }
            if (transitiveDeps > 0) {
                depIssues.put("transitiveDependency", (int) Math.round(transitiveDeps * 0.1));
            }
        }
        
        // Note: Scan findings excluded from risk calculation per new formula
        // Calculate risk score without scan findings and validation confidence
        int totalFileCount = getTotalFileCount();
        int testFileCount = getTestFileCount();
        double platformRiskScore = getPlatformRiskScore();
        // Estimate integration tests and critical modules (simplified for now)
        int integrationTestCount = estimateIntegrationTestCount();
        int criticalModulesTested = estimateCriticalModulesTested();
        
        // Pass empty scan findings to exclude them from calculation
        RiskScoringService.RiskScore riskScore = riskScoringService.calculateRiskScore(
            new HashMap<>(), // Empty scan findings - excluded from risk calculation
            depIssues, 
            totalFileCount, 
            platformRiskScore, 
            testFileCount, 
            integrationTestCount, 
            criticalModulesTested
        );
        int newScore = (int) Math.round(riskScore.totalScore());
        
        // Only update gauge if score actually changed
        if (lastCalculatedRiskScore == null || !lastCalculatedRiskScore.equals(newScore)) {
            lastCalculatedRiskScore = newScore;
            migrationRiskGauge.setScore(newScore);
        }

        // Calculate migration effort score (combining automation potential and test coverage)
        int effortScore = calculateEffortScore();
        effortScoreGauge.setScore(effortScore);

        // Calculate enhanced validation confidence using test coverage analysis
        int validationConfidenceScore = calculateEnhancedValidationConfidence(riskScore);
        

        // Update explanation panels with current data and colors
        updateRiskExplanation();
        updateEffortExplanation();
        updateConfidenceExplanation();
        
    }

    /**
     * Calculates enhanced validation confidence using comprehensive test coverage analysis.
     */
    private int calculateEnhancedValidationConfidence(RiskScoringService.RiskScore riskScore) {
        try {
            if (project != null && project.getBasePath() != null) {
                // Gather migration issues for correlation analysis
                Map<String, List<String>> migrationIssues = collectMigrationIssues();

                // Perform enhanced test coverage analysis
                var analysis = enhancedTestCoverageService.analyzeTestCoverage(
                    project.getBasePath(), migrationIssues);

                // Return the validation confidence score
                return (int) Math.round(analysis.validationConfidenceScore);
            }
        } catch (Exception e) {
            LOG.warn("Failed to calculate enhanced validation confidence, falling back to basic score", e);
        }

        // Fallback to basic score from risk calculation
        Integer basicScore = riskScore.componentScores().get("validationConfidence");
        return basicScore != null ? basicScore : 50;
    }

    /**
     * Collects migration issues from the current dashboard data for correlation analysis.
     */
    private Map<String, List<String>> collectMigrationIssues() {
        Map<String, List<String>> issues = new HashMap<>();

        if (dashboard != null && dashboard.getDependencySummary() != null) {
            DependencySummary depSummary = dashboard.getDependencySummary();
            List<String> mainIssues = new ArrayList<>();

            if (depSummary.getNoJakartaSupportCount() != null && depSummary.getNoJakartaSupportCount() > 0) {
                mainIssues.add("javax.servlet usage - no Jakarta equivalent");
            }
            if (depSummary.getBlockerDependencies() != null && depSummary.getBlockerDependencies() > 0) {
                mainIssues.add("blocked dependencies requiring manual intervention");
            }
            if (depSummary.getAffectedDependencies() != null && depSummary.getAffectedDependencies() > 0) {
                mainIssues.add("dependencies needing javax to jakarta migration");
            }

            if (!mainIssues.isEmpty()) {
                issues.put("main", mainIssues);
            }
        }

        return issues;
    }

    /**
     * Updates the risk explanation panel with current data and severity colors.
     */
    private void updateRiskExplanation() {
        if (dashboard == null) return;

        DependencySummary depSummary = dashboard.getDependencySummary();
        if (depSummary == null) return;

        // Get values for risk factors
        int affectedDeps = depSummary.getAffectedDependencies() != null ? depSummary.getAffectedDependencies() : 0;
        int transitiveDeps = depSummary.getTransitiveDependencies() != null ? depSummary.getTransitiveDependencies() : 0;
        int platformsNeedingUpgrade = getPlatformsNeedingUpgradeCount();
        int sourceIssues = getSourceCodeIssuesCount();
        int configIssues = getConfigIssuesCount();

        // Update labels with color coding (thresholds: [green, yellow, orange])
        updateBulletLabel(riskDirectDepsLabel, "Direct dependencies needing upgrade", affectedDeps,
            getColorForMetric(affectedDeps, new int[]{0, 5, 10}, false));
        updateBulletLabel(riskTransitiveLabel, "Transitive dependency issues", transitiveDeps,
            getColorForMetric(transitiveDeps, new int[]{0, 10, 20}, false));
        updateBulletLabel(riskPlatformsLabel, "Platforms needing upgrade", platformsNeedingUpgrade,
            getColorForMetric(platformsNeedingUpgrade, new int[]{0, 1, 2}, false));
        updateBulletLabel(riskSourceIssuesLabel, "Source code issues", sourceIssues,
            getColorForMetric(sourceIssues, new int[]{0, 5, 15}, false));
        updateBulletLabel(riskConfigIssuesLabel, "Config/non-source issues", configIssues,
            getColorForMetric(configIssues, new int[]{0, 3, 8}, false));
    }

    /**
     * Updates the effort explanation panel with current data and severity colors.
     */
    private void updateEffortExplanation() {
        if (dashboard == null) return;

        DependencySummary depSummary = dashboard.getDependencySummary();

        // Get values for effort factors
        int recipesWithMatches = getRecipesWithMatchesCount();
        int projectFiles = getTotalFileCount();
        int orgDeps = depSummary != null && depSummary.getOrganisationalDependencies() != null
            ? depSummary.getOrganisationalDependencies() : 0;

        // Update labels with color coding
        updateBulletLabel(effortRecipesLabel, "Refactors with recipes", recipesWithMatches,
            getColorForMetric(recipesWithMatches, new int[]{0, 5, 10}, false));
        updateBulletLabel(effortProjectSizeLabel, "Project complexity (files)", projectFiles,
            getColorForMetric(projectFiles, new int[]{100, 1000, 5000}, true));
        updateBulletLabel(effortOrgDepsLabel, "Organisational dependencies", orgDeps,
            getColorForMetric(orgDeps, new int[]{0, 3, 8}, false));
    }

    /**
     * Updates the confidence explanation panel with current data and severity colors.
     */
    private void updateConfidenceExplanation() {
        if (dashboard == null) return;

        DependencySummary depSummary = dashboard.getDependencySummary();

        // Get values for confidence factors
        int totalDeps = depSummary != null && depSummary.getTotalDependencies() != null
            ? depSummary.getTotalDependencies() : 0;
        
        // Calculate dependency knowledge percentage
        int knownPercentage = 0;
        if (totalDeps > 0) {
            int jakartaCompatible = depSummary.getJakartaCompatibleCount() != null ? depSummary.getJakartaCompatibleCount() : 0;
            int jakartaUpgrade = depSummary.getJakartaUpgradeCount() != null ? depSummary.getJakartaUpgradeCount() : 0;
            int noJakartaSupport = depSummary.getNoJakartaSupportCount() != null ? depSummary.getNoJakartaSupportCount() : 0;
            int knownDependencies = jakartaCompatible + jakartaUpgrade + noJakartaSupport;
            knownPercentage = (int) Math.round((knownDependencies * 100.0) / totalDeps);
        }
        
        // Calculate test coverage percentage
        int testCoveragePercentage = 50; // Default fallback
        try {
            if (project != null && project.getBasePath() != null) {
                Map<String, List<String>> migrationIssues = collectMigrationIssues();
                var analysis = enhancedTestCoverageService.analyzeTestCoverage(
                    project.getBasePath(), migrationIssues);
                testCoveragePercentage = (int) Math.round(analysis.validationConfidenceScore);
            }
        } catch (Exception e) {
            // Use fallback value on error
        }

        // Update labels with color coding
        updateBulletLabel(confidenceScansLabel, "Dependencies with known status", knownPercentage,
            getColorForMetric(knownPercentage, new int[]{50, 70, 90}, true));
        updateBulletLabel(confidenceUnknownDepsLabel, "Test coverage for migration risk", testCoveragePercentage,
            getColorForMetric(testCoveragePercentage, new int[]{50, 70, 90}, true));
    }

    private void updateValidationConfidenceExplanation() {
        if (dashboard == null) return;

        try {
            // Use enhanced test coverage analysis if available
            if (project != null && project.getBasePath() != null) {
                Map<String, List<String>> migrationIssues = collectMigrationIssues();
                var analysis = enhancedTestCoverageService.analyzeTestCoverage(
                    project.getBasePath(), migrationIssues);

                // Update with detailed analysis results
                updateValidationConfidenceFromAnalysis(analysis);
                return;
            }
        } catch (Exception e) {
            LOG.warn("Failed to use enhanced validation confidence analysis, falling back to basic", e);
        }

        // Fallback to basic calculation
        updateBasicValidationConfidenceExplanation();
    }

    /**
     * Updates validation confidence explanation using enhanced analysis results.
     */
    private void updateValidationConfidenceFromAnalysis(
            EnhancedTestCoverageAnalysisService.EnhancedTestCoverageAnalysis analysis) {

        var metrics = analysis.detailedMetrics;

        // Unit test coverage (effective coverage excluding mocked tests)
        float effectiveUnitCoverage = Math.max(0, metrics.unitTestCoverage - metrics.mockedTestCoverage);
        boolean lowUnitCoverage = effectiveUnitCoverage < 70.0f;

        // Integration/component/E2E coverage (most valuable for migration)
        float realIntegrationCoverage = metrics.integrationTestCoverage +
                                      metrics.componentTestCoverage +
                                      metrics.endToEndTestCoverage;
        boolean lowRealIntegrationCoverage = realIntegrationCoverage < 20.0f; // Stricter threshold

        // Migration risk coverage specifically
        boolean lowMigrationRiskCoverage = metrics.migrationRiskCoverage < 30.0f;

        // Update labels with enhanced information
        String unitText = String.format("Effective unit test coverage: %.1f%% (excl. mocks)", effectiveUnitCoverage);
        updateBulletLabel(validationUnitTestCoverageLabel, unitText, lowUnitCoverage ? 1 : 0,
            lowUnitCoverage ? Color.RED : Color.GREEN);

        String integrationText = String.format("Real integration coverage: %.1f%% (valuable for migration)", realIntegrationCoverage);
        updateBulletLabel(validationIntegrationTestsLabel, integrationText, lowRealIntegrationCoverage ? 1 : 0,
            lowRealIntegrationCoverage ? new Color(255, 165, 0) : Color.GREEN); // Orange for medium priority

        String criticalText = String.format("Migration-risk test coverage: %.1f%%", metrics.migrationRiskCoverage);
        updateBulletLabel(validationCriticalModulesLabel, criticalText, lowMigrationRiskCoverage ? 1 : 0,
            lowMigrationRiskCoverage ? Color.RED : Color.GREEN);

        // Show critical risk zones if any exist
        if (!analysis.criticalRiskZones.isEmpty()) {
            LOG.info("Found " + analysis.criticalRiskZones.size() + " critical risk zones with low test coverage");
            // Could add additional UI indicators here
        }
    }

    /**
     * Fallback method for basic validation confidence explanation.
     */
    private void updateBasicValidationConfidenceExplanation() {
        // Calculate test coverage metrics
        double testCoverage = calculateTestCoverageEstimate();
        int integrationTestCount = estimateIntegrationTestCount();
        int criticalModulesTested = estimateCriticalModulesTested();
        int totalFiles = getTotalFileCount();

        // Unit test coverage below threshold (threshold 70%)
        int unitTestCoverageValue = (int) Math.round(testCoverage);
        boolean belowThreshold = unitTestCoverageValue < 70;

        // Limited integration tests (threshold: at least 5% of files)
        int integrationTestPercentage = totalFiles > 0 ? (integrationTestCount * 100) / totalFiles : 0;
        boolean limitedIntegration = integrationTestPercentage < 5;

        // Critical modules lack coverage (threshold: at least 50% tested)
        int criticalModulesPercentage = totalFiles > 0 ? (criticalModulesTested * 100) / Math.max(totalFiles / 10, 1) : 0;
        boolean criticalModulesLackCoverage = criticalModulesPercentage < 50;

        // Update labels with color coding (red if issue exists, green if ok)
        updateBulletLabel(validationUnitTestCoverageLabel, "Unit test coverage below 70% threshold", belowThreshold ? 1 : 0,
            belowThreshold ? Color.RED : Color.GREEN);
        updateBulletLabel(validationIntegrationTestsLabel, "Integration tests < 5% of files", limitedIntegration ? 1 : 0,
            limitedIntegration ? Color.RED : Color.GREEN);
        updateBulletLabel(validationCriticalModulesLabel, "Critical modules < 50% tested", criticalModulesLackCoverage ? 1 : 0,
            criticalModulesLackCoverage ? Color.RED : Color.GREEN);
    }

    /**
     * Gets the count of platforms needing upgrade.
     */
    private int getPlatformsNeedingUpgradeCount() {
        // This would typically come from the platforms tab component
        // For now, return 0 or get from platformsTabComponent if available
        if (platformsTabComponent != null) {
            // This is a placeholder - actual implementation would depend on
            // what platformsTabComponent exposes
            return 0;
        }
        return 0;
    }

    /**
     * Gets the count of source code issues from advanced scans.
     */
    private int getSourceCodeIssuesCount() {
        if (advancedScanningService == null || !advancedScanningService.hasCachedResults()) {
            return 0;
        }
        AdvancedScanningService.AdvancedScanSummary summary = advancedScanningService.getCachedSummary();
        if (summary == null) return 0;

        // Sum all source code related scan issues
        return summary.getJpaCount() + summary.getBeanValidationCount() + summary.getServletJspCount()
            + summary.getCdiInjectionCount() + summary.getRestSoapCount() + summary.getDeprecatedApiCount()
            + summary.getSecurityApiCount() + summary.getJmsMessagingCount();
    }

    /**
     * Gets the count of config/non-source issues from advanced scans.
     */
    private int getConfigIssuesCount() {
        if (advancedScanningService == null || !advancedScanningService.hasCachedResults()) {
            return 0;
        }
        AdvancedScanningService.AdvancedScanSummary summary = advancedScanningService.getCachedSummary();
        if (summary == null) return 0;

        // Sum all config related scan issues
        return summary.getBuildConfigCount() + summary.getConfigFileCount();
    }

    /**
     * Gets the count of issues that have matching refactor recipes.
     */
    private int getRecipesWithMatchesCount() {
        if (advancedScanningService == null || !advancedScanningService.hasCachedResults()) {
            return 0;
        }
        AdvancedScanningService.AdvancedScanSummary summary = advancedScanningService.getCachedSummary();
        if (summary == null) return 0;

        return getIssuesWithMatchingRecipes(summary);
    }

    /**
     * Updates the summary section with current scan data.
     */
    public void updateSummary() {
        if (dashboard == null) return;

        SwingUtilities.invokeLater(() -> {
            // Update scan progress (including basic, advanced, and platform scans)
            int scanProgress = calculateOverallScanProgress();
            scanProgressValue.setText(scanProgress + "%");
            scanProgressValue.setForeground(getProgressColor(scanProgress));

            // Update main progress bar at top of dashboard
            if (mainScanProgressBar != null) {
                mainScanProgressBar.setValue(scanProgress);
                mainScanProgressBar.setString(scanProgress + "%");
            }
            if (mainScanProgressLabel != null) {
                if (scanProgress >= 100) {
                    mainScanProgressLabel.setText("Complete");
                } else if (scanProgress > 0) {
                    mainScanProgressLabel.setText("Scanning...");
                } else {
                    mainScanProgressLabel.setText("Ready to scan");
                }
            }

            // Update project size (total file count)
            int totalFiles = getTotalFileCount();
            projectSizeValue.setText(String.valueOf(totalFiles));
            projectSizeValue.setForeground(totalFiles > 0 ? new Color(100, 100, 200) : Color.GRAY);

            // Update dependencies found
            DependencySummary depSummary = dashboard.getDependencySummary();
            int totalBasicIssues = 0;
            if (depSummary != null) {
                int totalDeps = depSummary.getTotalDependencies();
                int affectedDeps = depSummary.getAffectedDependencies();
                dependenciesFoundValue.setText(affectedDeps + " / " + totalDeps);
                dependenciesFoundValue.setForeground(affectedDeps > 0 ? Color.ORANGE : Color.GREEN);
                
                // Update basic dependencies summary
                int basicDeps = totalDeps - affectedDeps; // Basic = total - affected
                basicDependenciesValue.setText(basicDeps + " compatible");
                basicDependenciesValue.setForeground(basicDeps > 0 ? Color.GREEN : Color.GRAY);
                
                // Calculate total basic scan issues (affected dependencies)
                totalBasicIssues = affectedDeps;
                
                // Update the 5 new dependency count labels
                int orgDeps = depSummary.getOrganisationalDependencies() != null ? depSummary.getOrganisationalDependencies() : 0;
                int noJakartaEquiv = depSummary.getNoJakartaSupportCount() != null ? depSummary.getNoJakartaSupportCount() : 0;
                int jakartaUpgrade = depSummary.getJakartaUpgradeCount() != null ? depSummary.getJakartaUpgradeCount() : 0;
                int jakartaCompatible = depSummary.getJakartaCompatibleCount() != null ? depSummary.getJakartaCompatibleCount() : 0;
                int unknownReview = depSummary.getUnknownReviewCount() != null ? depSummary.getUnknownReviewCount() : 0;
                int transitiveDeps = depSummary.getTransitiveDependencies() != null ? depSummary.getTransitiveDependencies() : 0;
                
                organisationalDepsValue.setText(String.valueOf(orgDeps));
                noJakartaEquivalentValue.setText(String.valueOf(noJakartaEquiv));
                jakartaUpgradeValue.setText(String.valueOf(jakartaUpgrade));
                jakartaCompatibleValue.setText(String.valueOf(jakartaCompatible));
                unknownReviewValue.setText(String.valueOf(unknownReview));
                transitiveDepsValue.setText(String.valueOf(transitiveDeps));
                
                // Apply color coding based on values
                organisationalDepsValue.setForeground(orgDeps > 0 ? Color.ORANGE : Color.GREEN);
                noJakartaEquivalentValue.setForeground(noJakartaEquiv > 0 ? Color.RED : Color.GREEN);
                jakartaUpgradeValue.setForeground(jakartaUpgrade > 0 ? Color.ORANGE : Color.GREEN);
                jakartaCompatibleValue.setForeground(jakartaCompatible > 0 ? Color.GREEN : Color.GRAY);
                unknownReviewValue.setForeground(unknownReview > 0 ? Color.ORANGE : Color.GREEN);
                transitiveDepsValue.setForeground(transitiveDeps > 0 ? Color.ORANGE : Color.GREEN);
            } else {
                dependenciesFoundValue.setText("-");
                basicDependenciesValue.setText("-");
                organisationalDepsValue.setText("0");
                noJakartaEquivalentValue.setText("0");
                jakartaUpgradeValue.setText("0");
                jakartaCompatibleValue.setText("0");
                unknownReviewValue.setText("0");
                transitiveDepsValue.setText("0");
                projectSizeValue.setText("-");
                projectSizeValue.setForeground(Color.GRAY);
            }
            
            // Update comprehensive summary - Basic Issues
            updateScanCountWithColor(totalBasicIssuesValue, totalBasicIssues);

            // Update advanced scan totals
            int totalAdvancedIssues = 0;
            if (advancedScanningService != null && advancedScanningService.hasCachedResults()) {
                AdvancedScanningService.AdvancedScanSummary summary = advancedScanningService.getCachedSummary();
                if (summary != null) {
                    totalAdvancedIssues = summary.getTotalIssuesFound();
                }
            }
            updateScanCountWithColor(totalAdvancedIssuesValue, totalAdvancedIssues);
            
            // Update platform artifact totals
            int totalPlatformArtifacts = 0;
            int warCount = 0;
            int earCount = 0;
            int jarCount = 0;
            int totalDeploymentCount = 0;
            
            if (platformsTabComponent != null) {
                EnhancedPlatformScanResult platformResult = platformsTabComponent.getCurrentScanResult();
                if (platformResult != null) {
                    warCount = platformResult.getWarCount();
                    earCount = platformResult.getEarCount();
                    jarCount = platformResult.getJarCount();
                    totalDeploymentCount = platformResult.getTotalDeploymentCount();
                    // Platform artifacts count as the total deployment artifacts
                    totalPlatformArtifacts = totalDeploymentCount;
                }
            }
            
            // Update platform artifact labels
            updateScanCountWithColor(totalPlatformArtifactsValue, totalPlatformArtifacts);
            updateScanCountWithColor(deploymentWarCountValue, warCount);
            updateScanCountWithColor(deploymentEarCountValue, earCount);
            updateScanCountWithColor(deploymentJarCountValue, jarCount);
            updateScanCountWithColor(totalDeploymentCountValue, totalDeploymentCount);
            
            // Calculate and update GRAND TOTAL
            int grandTotal = totalBasicIssues + totalAdvancedIssues + totalPlatformArtifacts;
            if (grandTotalValue != null) {
                grandTotalValue.setText(String.valueOf(grandTotal));
                if (grandTotal > 0) {
                    grandTotalValue.setForeground(new Color(220, 53, 69)); // Red for issues
                } else {
                    grandTotalValue.setForeground(new Color(40, 167, 69)); // Green for no issues
                }
            }
            
            // Update detected platforms from platform scanning
            if (detectedPlatformsValue != null && platformsTabComponent != null) {
                try {
                    // Get platform list from platforms tab component
                    List<String> detectedPlatforms = getDetectedPlatformsFromTab();
                    if (detectedPlatforms != null && !detectedPlatforms.isEmpty()) {
                        String platformsText = String.join(", ", detectedPlatforms);
                        detectedPlatformsValue.setText(platformsText);
                        detectedPlatformsValue.setForeground(Color.BLUE);
                    } else {
                        detectedPlatformsValue.setText("No platforms detected");
                        detectedPlatformsValue.setForeground(Color.GRAY);
                    }
                } catch (Exception e) {
                    LOG.warn("Could not update detected platforms: " + e.getMessage());
                    if (detectedPlatformsValue != null) {
                        detectedPlatformsValue.setText("-");
                        detectedPlatformsValue.setForeground(Color.GRAY);
                    }
                }
            } else {
                if (detectedPlatformsValue != null) {
                    detectedPlatformsValue.setText("-");
                    detectedPlatformsValue.setForeground(Color.GRAY);
                }
            }
        });
    }

    // ==================== Helper Methods ====================

    private int getBasicScanCount() {
        if (dashboard == null || dashboard.getDependencySummary() == null) return 0;
        return dashboard.getDependencySummary().getAffectedDependencies();
    }

    /**
     * Calculates the confidence score (0-100) based on the percentage of dependencies
     * with known Jakarta compatibility status vs unknown status.
     *
     * Known statuses: COMPATIBLE, NEEDS_UPGRADE, NO_JAKARTA_VERSION, REQUIRES_MANUAL_MIGRATION, MIGRATED
     * Unknown statuses: UNKNOWN_REVIEW, UNKNOWN
     *
     * Includes deep transitive dependencies in the calculation.
     *
     * @return Confidence score from 0 to 100
     */
    private int calculateConfidenceScore() {
        if (dashboard == null || dashboard.getDependencySummary() == null) {
            return 0;
        }

        // Calculate dependency knowledge score (50% weight)
        DependencySummary depSummary = dashboard.getDependencySummary();
        int totalDependencies = depSummary.getTotalDependencies() != null ? depSummary.getTotalDependencies() : 0;
        
        int dependencyScore = 0;
        if (totalDependencies > 0) {
            int jakartaCompatible = depSummary.getJakartaCompatibleCount() != null ? depSummary.getJakartaCompatibleCount() : 0;
            int jakartaUpgrade = depSummary.getJakartaUpgradeCount() != null ? depSummary.getJakartaUpgradeCount() : 0;
            int noJakartaSupport = depSummary.getNoJakartaSupportCount() != null ? depSummary.getNoJakartaSupportCount() : 0;
            int knownDependencies = jakartaCompatible + jakartaUpgrade + noJakartaSupport;
            dependencyScore = (int) Math.round((knownDependencies * 100.0) / totalDependencies);
        }
        
        // Calculate test coverage score (50% weight)
        int testCoverageScore = 50; // Default fallback
        try {
            if (project != null && project.getBasePath() != null) {
                Map<String, List<String>> migrationIssues = collectMigrationIssues();
                var analysis = enhancedTestCoverageService.analyzeTestCoverage(
                    project.getBasePath(), migrationIssues);
                testCoverageScore = (int) Math.round(analysis.validationConfidenceScore);
            }
        } catch (Exception e) {
            // Use fallback value on error
        }
        
        // Combine scores: 50% dependency knowledge + 50% test coverage
        int combinedScore = (int) Math.round((dependencyScore * 0.5) + (testCoverageScore * 0.5));
        
        // Ensure score is within 0-100 range
        return Math.max(0, Math.min(100, combinedScore));
    }

    /**
     * Calculates the migration effort score (0-100) based on four factors:
     * 1. Scan findings (25%): logarithmic scale based on total scan findings
     * 2. Jakarta dependencies to upgrade (25%): jakartaUpgrade count
     * 3. Automation potential (25%): percentage of scan issues that have matching refactor recipes
     * 4. Project size (25%): total file count
     *
     * Lower score = easier migration (fewer findings, better automation, smaller project)
     * Higher score = harder migration (more findings, less automation, larger project)
     *
     * Weights are loaded from risk-scoring.yaml configuration.
     *
     * @return Effort score from 0 to 100
     */
    private int calculateEffortScore() {
        if (dashboard == null) {
            return 0;
        }

        // Load weights from configuration
        RiskScoringService riskScoringService = RiskScoringService.getInstance();
        RiskScoringConfig config = riskScoringService.getRiskScoringConfig();
        
        // Check for conditional weighting based on major version changes
        boolean hasJavaMajorVersionChange = hasJavaMajorVersionChange();
        boolean hasAppserverPlatformChange = hasAppserverPlatformChange();
        
        // Base weights from YAML
        double scanFindingsWeight = 0.20;
        double jakartaUpgradeWeight = 0.20;
        double dockerfilesWeight = 0.15;
        double cicdScriptsWeight = 0.15;
        double automationWeight = 0.15;
        double projectSizeWeight = 0.15;
        
        // Apply conditional weighting if major version changes detected
        if (hasJavaMajorVersionChange || hasAppserverPlatformChange) {
            // Use conditional weights (5% each for Docker and CI/CD scripts)
            dockerfilesWeight = 0.05;
            cicdScriptsWeight = 0.05;
            // Reduce other weights to accommodate the conditional weights
            automationWeight = 0.175; // (0.20 - 0.05 - 0.05) = 0.10
            projectSizeWeight = 0.175; // (0.20 - 0.05 - 0.05) = 0.10
        } else {
            // Normal case: use 0 weight for Docker and CI/CD scripts
            dockerfilesWeight = 0.0;
            cicdScriptsWeight = 0.0;
            // Use normal weights for other factors
            automationWeight = 0.20;
            projectSizeWeight = 0.20;
        }

        // Calculate scan findings score (logarithmic scale)
        int scanFindingsScore = calculateScanFindingsScore();

        // Calculate jakarta upgrade dependencies score
        int jakartaUpgradeScore = calculateJakartaUpgradeScore();
        
        // Calculate Docker files score
        int dockerfilesScore = calculateDockerfilesScore();
        
        // Calculate CI/CD scripts score
        int cicdScriptsScore = calculateCicdScriptsScore();

        // Calculate automation score (percentage of issues WITHOUT recipe matches)
        int automationScore = calculateAutomationScore();

        // Calculate project size score (larger projects = higher effort)
        int projectSizeScore = calculateProjectSizeScore(10000); // Use fixed threshold

        // Combine scores using new weights
        int combinedScore = (int) Math.round(
            (scanFindingsScore * scanFindingsWeight) +
            (jakartaUpgradeScore * jakartaUpgradeWeight) +
            (dockerfilesScore * dockerfilesWeight) +
            (cicdScriptsScore * cicdScriptsWeight) +
            (automationScore * automationWeight) +
            (projectSizeScore * projectSizeWeight)
        );

        // Ensure score is within 0-100 range
        return Math.max(0, Math.min(100, combinedScore));
    }

    /**
     * Calculates the organisational dependencies score based on the count of
     * organisational/internal dependencies. More organisational dependencies
     * means higher effort (harder migration).
     *
     * @param maxThreshold Maximum threshold for capping the score
     * @return Organisational dependencies score from 0 to 100
     */
    private int calculateOrganisationalDepsScore(int maxThreshold) {
        if (dashboard == null || dashboard.getDependencySummary() == null) {
            return 0;
        }

        DependencySummary depSummary = dashboard.getDependencySummary();
        int orgDeps = depSummary.getOrganisationalDependencies() != null
            ? depSummary.getOrganisationalDependencies()
            : 0;

        if (orgDeps == 0) {
            return 0; // No organisational dependencies = low effort
        }

        // Calculate score: proportional to org deps count, capped at threshold
        // Score = min(orgDeps / maxThreshold, 1.0) * 100
        double ratio = Math.min(orgDeps / (double) maxThreshold, 1.0);
        return (int) Math.round(ratio * 100);
    }

    /**
     * Calculates the project size score based on the total number of files in the project.
     * Larger projects mean higher effort (more files to migrate).
     *
     * @param maxThreshold Maximum threshold for capping the score
     * @return Project size score from 0 to 100
     */
    private int calculateProjectSizeScore(int maxThreshold) {
        if (dashboard == null || dashboard.getDependencySummary() == null) {
            return 0;
        }

        DependencySummary depSummary = dashboard.getDependencySummary();
        int totalFiles = depSummary.getTotalDependencies() != null
            ? depSummary.getTotalDependencies()
            : 0;

        if (totalFiles == 0) {
            return 0; // No files = low effort
        }

        // Calculate score: proportional to file count, capped at threshold
        // Score = min(totalFiles / maxThreshold, 1.0) * 100
        double ratio = Math.min(totalFiles / (double) maxThreshold, 1.0);
        return (int) Math.round(ratio * 100);
    }

    /**
     * Calculates the automation score based on how many scan issues have matching refactor recipes.
     * Higher score = fewer issues can be automated (more manual effort required)
     * Lower score = more issues can be automated (less manual effort required)
     *
     * @return Automation score from 0 to 100
     */
    private int calculateAutomationScore() {
        if (advancedScanningService == null || !advancedScanningService.hasCachedResults()) {
            // No scan results available - assume worst case (all manual)
            return 100;
        }

        AdvancedScanningService.AdvancedScanSummary summary = advancedScanningService.getCachedSummary();
        if (summary == null) {
            return 100;
        }

        // Calculate total issues found across all scan types
        int totalIssues = summary.getTotalIssuesFound();
        if (totalIssues == 0) {
            // No issues found = easy migration (low effort)
            return 0;
        }

        // Get recipe recommendations for the scan results
        int issuesWithRecipes = getIssuesWithMatchingRecipes(summary);

        // Calculate automation score: percentage of issues WITHOUT recipe matches
        // Higher percentage = more manual work = higher effort score
        int issuesWithoutRecipes = totalIssues - issuesWithRecipes;
        int automationScore = (int) Math.round((issuesWithoutRecipes * 100.0) / totalIssues);

        return Math.max(0, Math.min(100, automationScore));
    }

    /**
     * Calculates the scan findings score using logarithmic scale.
     * More scan findings = higher effort, but with diminishing returns.
     *
     * @return Scan findings score from 0 to 100
     */
    private int calculateScanFindingsScore() {
        if (advancedScanningService == null || !advancedScanningService.hasCachedResults()) {
            return 0; // No scan results = no effort from findings
        }

        AdvancedScanningService.AdvancedScanSummary summary = advancedScanningService.getCachedSummary();
        if (summary == null) {
            return 0;
        }

        // Calculate total scan findings
        int totalFindings = summary.getJpaCount() + 
                           summary.getBeanValidationCount() + 
                           summary.getServletJspCount() + 
                           summary.getCdiInjectionCount() + 
                           summary.getBuildConfigCount() + 
                           summary.getRestSoapCount() + 
                           summary.getDeprecatedApiCount() + 
                           summary.getSecurityApiCount() + 
                           summary.getJmsMessagingCount() + 
                           summary.getConfigFileCount();

        if (totalFindings <= 0) {
            return 0; // No findings = no effort
        }

        // Logarithmic scale: log10(totalFindings + 1) / logDivisor * 100
        // Using logDivisor of 3.0 (log10(1000) ≈ 3) from YAML config
        double logDivisor = 3.0;
        double logScore = Math.log10(totalFindings + 1) / logDivisor * 100.0;
        
        return Math.max(0, Math.min(100, (int) Math.round(logScore)));
    }

    /**
     * Calculates the jakarta upgrade dependencies score.
     * More dependencies needing upgrade = higher effort.
     *
     * @return Jakarta upgrade score from 0 to 100
     */
    private int calculateJakartaUpgradeScore() {
        if (dashboard == null || dashboard.getDependencySummary() == null) {
            return 0;
        }

        DependencySummary depSummary = dashboard.getDependencySummary();
        int jakartaUpgrade = depSummary.getJakartaUpgradeCount() != null 
            ? depSummary.getJakartaUpgradeCount() 
            : 0;

        if (jakartaUpgrade <= 0) {
            return 0; // No jakarta upgrades needed = no effort
        }

        // Score proportional to jakarta upgrade count, capped at 100
        // Using a reasonable threshold where 50+ jakarta upgrades = max effort
        int maxThreshold = 50;
        double ratio = Math.min(jakartaUpgrade / (double) maxThreshold, 1.0);
        return (int) Math.round(ratio * 100);
    }

    /**
     * Gets the count of scan issues that have matching refactor recipes.
     * Uses the ScanRecipeRecommendationService to determine which scan types have applicable recipes.
     *
     * @param summary The advanced scan summary containing issue counts by type
     * @return Number of issues that have matching recipes
     */
    
    /**
     * Checks if there's a major Java version change required (e.g., Java 8 to 11+).
     * This is a simplified check - in a real implementation, this would check
     * actual Java versions and project configuration.
     *
     * @return true if major Java version change is detected
     */
    private boolean hasJavaMajorVersionChange() {
        // Simplified check - in real implementation, this would analyze
        // actual Java versions and project configuration
        // For now, return false as a placeholder
        return false;
    }

    /**
     * Checks if there's a major appserver platform change required.
     * This is a simplified check - in a real implementation, this would
     * analyze the actual application server configuration.
     *
     * @return true if major appserver platform change is detected
     */
    private boolean hasAppserverPlatformChange() {
        // Simplified check - in real implementation, this would analyze
        // actual application server configuration
        // For now, return false as a placeholder
        return false;
    }
    private int getIssuesWithMatchingRecipes(AdvancedScanningService.AdvancedScanSummary summary) {
        if (summary == null) {
            return 0;
        }

        int issuesWithRecipes = 0;

        // Map of scan types to their recipe availability
        // These scan types have matching recipes in ScanRecipeRecommendationServiceImpl.SCAN_TO_RECIPE_MAPPING
        Map<String, Integer> scanTypeCounts = new HashMap<>();
        scanTypeCounts.put("jpa", summary.getJpaCount());
        scanTypeCounts.put("beanValidation", summary.getBeanValidationCount());
        scanTypeCounts.put("servletJsp", summary.getServletJspCount());
        scanTypeCounts.put("cdiInjection", summary.getCdiInjectionCount());
        scanTypeCounts.put("restSoap", summary.getRestSoapCount());
        scanTypeCounts.put("securityApi", summary.getSecurityApiCount());
        scanTypeCounts.put("jmsMessaging", summary.getJmsMessagingCount());
        scanTypeCounts.put("buildConfig", summary.getBuildConfigCount());
        scanTypeCounts.put("configFiles", summary.getConfigFileCount());
        scanTypeCounts.put("deprecatedApi", summary.getDeprecatedApiCount());
        scanTypeCounts.put("transitiveDependency", summary.getTransitiveDependencyCount());

        // Scan types with matching recipes (from ScanRecipeRecommendationServiceImpl.SCAN_TO_RECIPE_MAPPING)
        // These scan types have recipes available, so their issues can be automated
        String[] scanTypesWithRecipes = {
            "jpa", "beanValidation", "servletJsp", "cdiInjection", "restSoap",
            "securityApi", "jmsMessaging", "buildConfig", "configFiles", "deprecatedApi"
        };

        // Count issues that have matching recipes
        for (String scanType : scanTypesWithRecipes) {
            Integer count = scanTypeCounts.get(scanType);
            if (count != null && count > 0) {
                issuesWithRecipes += count;
            }
        }

        // For transitive dependencies and other scans without direct recipes,
        // we don't count them as having recipe matches

        return issuesWithRecipes;
    }

    private int calculateEffortWeeks() {
        // Calculate effort in weeks using the new exponential formula from RiskScoringService
        // Uses default team size (5) and customer count (1) - can be enhanced with UI inputs later
        RiskScoringService riskScoringService = RiskScoringService.getInstance();
        double currentRiskScore = 0;

        if (riskScoringService != null) {
            try {
                // Get current scan findings for effort calculation
                Map<String, List<RiskScoringService.RiskFinding>> scanFindings = new HashMap<>();
                Map<String, Integer> depIssues = new HashMap<>();

                // Build dependency issues map from dashboard
                DependencySummary depSummary = dashboard != null ? dashboard.getDependencySummary() : null;
                if (depSummary != null) {
                    int noSupport = depSummary.getNoJakartaSupportCount() != null ? depSummary.getNoJakartaSupportCount() : 0;
                    int affected = depSummary.getAffectedDependencies() != null ? depSummary.getAffectedDependencies() : 0;
                    int blockers = depSummary.getBlockerDependencies() != null ? depSummary.getBlockerDependencies() : 0;
                    int transitiveDeps = depSummary.getTransitiveDependencies() != null ? depSummary.getTransitiveDependencies() : 0;

                    if (noSupport > 0) depIssues.put("noJakartaVersion", noSupport * 25);
                    if (affected > 0) depIssues.put("affectedDependencies", affected * 10);
                    if (blockers > 0) depIssues.put("blockerDependencies", blockers * 50);
                    if (transitiveDeps > 0) depIssues.put("transitiveDependency", (int) Math.round(transitiveDeps * 0.1));
                }

                // Add basic scan findings
                int basicCount = getBasicScanCount();
                if (basicCount > 0) {
                    scanFindings.put("basic", createRiskFindings(basicCount, "basic"));
                }

                // Calculate risk score with test coverage parameters
                int testFileCount = getTestFileCount();
                int integrationTestCount = estimateIntegrationTestCount();
                int criticalModulesTested = estimateCriticalModulesTested();
                RiskScoringService.RiskScore currentScore = riskScoringService.calculateRiskScore(
                    scanFindings, depIssues, getTotalFileCount(), getPlatformRiskScore(),
                    testFileCount, integrationTestCount, criticalModulesTested);
                currentRiskScore = currentScore.totalScore();
            } catch (Exception e) {
                LOG.warn("Could not calculate current risk score for effort calculation: " + e.getMessage());
                currentRiskScore = 25; // Default to medium risk
            }
        }

        // Use default team size (slider removed), default to 1 environment
        int teamSize = DEFAULT_TEAM_SIZE;

        // Calculate test coverage estimate
        double testCoverage = calculateTestCoverageEstimate();

        // Apply coverage factor: lower coverage = higher effort (more testing needed)
        // 100% coverage = 1.0x multiplier, 0% coverage = 2.0x multiplier
        double coverageFactor = 2.0 - (testCoverage / 100.0);

        // Use new exponential formula with team size only (environments parameter removed)
        int baseEffortWeeks = riskScoringService.calculateMigrationTimeWeeks(currentRiskScore, teamSize, 1);

        // Apply coverage factor to effort
        int effortWeeks = (int) Math.ceil(baseEffortWeeks * coverageFactor);

        return effortWeeks;
    }
    
    private void addCurrentFindings(Map<String, List<RiskScoringService.RiskFinding>> scanFindings, Map<String, Integer> depIssues) {
        // This method is currently not implemented due to missing dashboard methods
        // Using simplified effort calculation based on basic scan count only
    }

    private List<RiskScoringService.RiskFinding> createRiskFindings(int count, String scanType) {
        List<RiskScoringService.RiskFinding> findings = new ArrayList<>();
        if (count > 0) {
            // Use the base weight from YAML configuration
            double baseWeight = getBaseWeightForScanType(scanType);
            
            // Calculate score based on count and base weight from YAML
            // Apply the minScanThreshold and maxFindingsPerScan limits
            double minThreshold = getMinScanThreshold();
            int maxFindings = getMaxFindingsPerScan();
            
            // Limit the count to maxFindingsPerScan
            int effectiveCount = Math.min(count, maxFindings);
            
            // Calculate score using base weight from YAML
            double score = effectiveCount * baseWeight;
            
            // Apply minimum threshold
            if (score < minThreshold) {
                score = 0;
            }
            
            // Determine risk level based on score and YAML configuration
            String riskLevel = determineRiskLevel(score, scanType);
            
            findings.add(new RiskScoringService.RiskFinding(
                scanType,
                scanType + "_issues",
                scanType + " issues found: " + count,
                riskLevel,
                (int) Math.round(score)
            ));
        }
        return findings;
    }
    
    private double getBaseWeightForScanType(String scanType) {
        try {
            // Use reasonable defaults based on YAML configuration
            switch (scanType) {
                case "basic": return 0.1;
                case "jpa": return 0.1;
                case "beanValidation": return 0.1;
                case "servletJsp": return 1.0;
                case "cdiInjection": return 0.1;
                case "buildConfig": return 0.1;
                case "restSoap": return 0.5;
                case "jmsMessaging": return 1.0;
                case "configFiles": return 0.1;
                case "deprecatedApi": return 1.0;
                case "securityApi": return 1.0;
                default: return 0.1;
            }
        } catch (Exception e) {
            LOG.warn("Could not get base weight for scan type: " + scanType + ", using default");
            return 0.1;
        }
    }
    
    private double getMinScanThreshold() {
        try {
            // Get from YAML configuration
            return 0.5; // From riskCalculation.minScanThreshold
        } catch (Exception e) {
            return 0.5; // Default
        }
    }
    
    private int getMaxFindingsPerScan() {
        try {
            // Get from YAML configuration
            return 20; // From riskCalculation.maxFindingsPerScan
        } catch (Exception e) {
            return 20; // Default
        }
    }
    
    private String determineRiskLevel(double score, String scanType) {
        // Use YAML-based risk level determination
        if (score >= 5.0) {
            return "high";
        } else if (score >= 2.0) {
            return "medium";
        } else {
            return "low";
        }
    }
    
    /**
     * Gets the total file count for the project to calculate complexity score.
     */
    private int getTotalFileCount() {
        try {
            if (project != null && project.getBasePath() != null) {
                java.nio.file.Path projectPath = java.nio.file.Paths.get(project.getBasePath());
                return countFilesRecursively(projectPath);
            }
        } catch (Exception e) {
            LOG.warn("Could not count project files: " + e.getMessage());
        }
        return 0;
    }
    
    /**
     * Recursively counts files in the project directory.
     */
    private int countFilesRecursively(java.nio.file.Path path) {
        try {
            return java.nio.file.Files.walk(path)
                    .filter(java.nio.file.Files::isRegularFile)
                    .filter(p -> {
                        String fileName = p.getFileName().toString();
                        // Count only source files and configuration files
                        return fileName.endsWith(".java") || fileName.endsWith(".xml") ||
                               fileName.endsWith(".properties") || fileName.endsWith(".yml") ||
                               fileName.endsWith(".yaml") || fileName.endsWith(".kt") ||
                               fileName.endsWith(".scala") || fileName.endsWith(".groovy");
                    })
                    .mapToInt(p -> 1)
                    .limit(10000) // Cap at 10000 files for performance
                    .sum();
        } catch (Exception e) {
            LOG.warn("Error counting files: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Gets the test file count for the project.
     * Test files are identified by common test naming patterns and locations.
     */
    private int getTestFileCount() {
        try {
            if (project != null && project.getBasePath() != null) {
                java.nio.file.Path projectPath = java.nio.file.Paths.get(project.getBasePath());
                return countTestFilesRecursively(projectPath);
            }
        } catch (Exception e) {
            LOG.warn("Could not count test files: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Recursively counts test files in the project directory.
     * Identifies test files by:
     * - Files in test directories (src/test, test/, tests/, __tests__/)
     * - Files with test naming patterns (Test*.java, *Test.java, *Tests.java, *IT.java)
     */
    private int countTestFilesRecursively(java.nio.file.Path path) {
        try {
            return java.nio.file.Files.walk(path)
                    .filter(java.nio.file.Files::isRegularFile)
                    .filter(p -> {
                        String fileName = p.getFileName().toString().toLowerCase();
                        String pathStr = p.toString().toLowerCase();

                        // Check if in test directory
                        boolean inTestDir = pathStr.contains("/test/") ||
                                          pathStr.contains("/tests/") ||
                                          pathStr.contains("/__tests__/") ||
                                          pathStr.contains("/src/test/") ||
                                          pathStr.contains("/src/it/") ||
                                          pathStr.contains("/integration-test/");

                        // Check test file naming patterns
                        boolean isTestFile = fileName.endsWith("test.java") ||
                                             fileName.endsWith("tests.java") ||
                                             fileName.endsWith("it.java") ||
                                             fileName.startsWith("test") && fileName.endsWith(".java") ||
                                             fileName.matches(".*test[0-9]*\\.java");

                        // Kotlin test files
                        boolean isKotlinTest = fileName.endsWith("test.kt") ||
                                               fileName.endsWith("tests.kt") ||
                                               fileName.endsWith("ittest.kt") ||
                                               fileName.endsWith("spec.kt");

                        return (inTestDir && (fileName.endsWith(".java") || fileName.endsWith(".kt"))) ||
                               isTestFile || isKotlinTest;
                    })
                    .mapToInt(p -> 1)
                    .limit(5000) // Cap at 5000 test files for performance
                    .sum();
        } catch (Exception e) {
            LOG.warn("Error counting test files: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Calculates estimated test coverage percentage based on test file ratio.
     * Formula: coverage = (testFiles * 1000) / totalFiles, capped at 100%
     *
     * Examples:
     * - 10:1 ratio (10% test files) = 100% coverage
     * - 100:1 ratio (1% test files) = 10% coverage
     */
    private double calculateTestCoverageEstimate() {
        int totalFiles = getTotalFileCount();
        int testFiles = getTestFileCount();

        if (totalFiles == 0) {
            return 0.0;
        }

        // Formula: (testFiles / totalFiles) * 1000 gives coverage percentage
        // 10:1 ratio (10% are tests) -> 100% coverage
        // 100:1 ratio (1% are tests) -> 10% coverage
        double coverage = (testFiles * 1000.0) / totalFiles;

        // Cap at 100%
        return Math.min(coverage, 100.0);
    }

    /**
     * Estimates the number of integration test files.
     * For now, assumes 20% of test files are integration tests.
     */
    private int estimateIntegrationTestCount() {
        int testFiles = getTestFileCount();
        return (int) Math.round(testFiles * 0.2);
    }

    /**
     * Estimates the number of critical modules with test coverage.
     * For now, assumes 30% of modules are critical and tested.
     */
    private int estimateCriticalModulesTested() {
        int totalFiles = getTotalFileCount();
        // Assume modules are roughly 50 files each
        int estimatedModules = Math.max(totalFiles / 50, 1);
        return (int) Math.round(estimatedModules * 0.3);
    }
    
    /**
     * Gets the platform risk score based on platform compatibility and deployment artifacts.
     */
    private double getPlatformRiskScore() {
        try {
            if (platformsTabComponent != null) {
                return platformsTabComponent.getCurrentPlatformRiskScore();
            }
            // Default low risk if no platforms tab component available
            return 1.0;
        } catch (Exception e) {
            LOG.warn("Could not calculate platform risk: " + e.getMessage());
            return 1.0; // Default low risk
        }
    }

    /**
     * Sets the platforms tab component for integration
     */
    public void setPlatformsTabComponent(PlatformsTabComponent platformsTabComponent) {
        this.platformsTabComponent = platformsTabComponent;
        LOG.info("DashboardComponent: PlatformsTabComponent set for risk integration");
    }

    /**
     * Sets the tab switcher callback for navigation from explanation bullets.
     *
     * @param tabSwitcher Consumer that takes a tab name and switches to that tab
     */
    public void setTabSwitcher(Consumer<String> tabSwitcher) {
        this.tabSwitcher = tabSwitcher;
    }

    private int calculateOverallScanProgress() {
        // Calculate percentage of all available scans that have been run
        // Basic scan (1) + Advanced scans (11) + Platform scans (1) = 13 total
        int totalScans = 1; // Basic scan always available
        int completedScans = 0;
        
        // Check if basic scan is completed - only count as completed if it has actual dependencies
        if (dashboard != null && dashboard.getDependencySummary() != null 
            && dashboard.getDependencySummary().getTotalDependencies() > 0) {
            completedScans += 1; // Basic scan completed
        }
        
        // Check advanced scans
        if (advancedScanningService != null && advancedScanningService.hasCachedResults()) {
            totalScans += 11; // 11 advanced scan types
            completedScans += 11; // All advanced scans completed
        } else {
            totalScans += 11; // Advanced scans available but not completed
        }
        
        // Check platform scans (simplified - assume 1 platform scan type)
        // For now, we'll consider platform scans completed if advanced scans are done
        if (advancedScanningService != null && advancedScanningService.hasCachedResults()) {
            totalScans += 1; // Platform scan available
            completedScans += 1; // Platform scan completed
        } else {
            totalScans += 1; // Platform scan available but not completed
        }
        
        return totalScans > 0 ? (completedScans * 100) / totalScans : 0;
    }

    private Color getProgressColor(int progress) {
        if (progress >= 100) return new Color(40, 167, 69); // Green
        if (progress >= 50) return new Color(255, 193, 7); // Yellow
        return new Color(220, 53, 69); // Red
    }

    public JBLabel getDeprecatedApiScanCountValue() {
        return deprecatedApiScanCountValue;
    }

public JBLabel getSecurityApiScanCountValue() {
        return securityApiScanCountValue;
    }

    public JBLabel getJmsMessagingScanCountValue() {
        return jmsMessagingScanCountValue;
    }

    public JBLabel getTransitiveDependencyScanCountValue() {
        return transitiveDependencyScanCountValue;
    }

    public JBLabel getConfigFileScanCountValue() {
        return configFileScanCountValue;
    }

    public JBLabel getClassloaderModuleScanCountValue() {
        return classloaderModuleScanCountValue;
    }

    public JBLabel getLoggingMetricsScanCountValue() {
        return loggingMetricsScanCountValue;
    }

    public JBLabel getSerializationCacheScanCountValue() {
        return serializationCacheScanCountValue;
    }

    public JBLabel getThirdPartyLibScanCountValue() {
        return thirdPartyLibScanCountValue;
    }

    public JBLabel getTotalAdvancedScanCountValue() {
        return totalAdvancedScanCountValue;
    }

    // ==================== Truncation Helper Delegation ====================

    /**
     * Checks if truncation mode should be applied.
     * Delegates to TruncationHelper for consistent behavior across UI tabs.
     * @return true if results should be truncated (free users see truncated results)
     */
    private boolean shouldTruncateResults() {
        return truncationHelper.shouldTruncateResults();
    }

    /**
     * Formats a count display with truncation indicator if needed.
     * Delegates to TruncationHelper for consistent behavior across UI tabs.
     * @param actualCount the actual number of results
     * @return formatted string like "5" or "10 of 25" when truncated
     */
    private String formatTruncatedCount(int actualCount) {
        return truncationHelper.formatTruncatedCount(actualCount);
    }

    /**
     * Calculates Docker files score based on the number of Docker-related files found.
     * @return Docker files score from 0 to 100
     */
    private int calculateDockerfilesScore() {
        if (advancedScanningService == null || !advancedScanningService.hasCachedResults()) {
            return 0; // No scan results = no Docker files detected
        }

        try {
            // Get comprehensive scan results to extract Docker file information
            ComprehensiveScanResults scanResults = advancedScanningService.getLastScanResults();
            if (scanResults == null) {
                return 0;
            }

            // Count Docker-related files (Dockerfile, docker-compose.yml, etc.)
            int dockerFileCount = 0;
            // This would be populated from actual scan results
            // For now, using a placeholder calculation based on available data
            
            // Score proportional to Docker file count, capped at 100
            // Using a threshold where 10+ Docker files = max effort
            int maxThreshold = 10;
            double ratio = Math.min(dockerFileCount / (double) maxThreshold, 1.0);
            return (int) Math.round(ratio * 100);
        } catch (Exception e) {
            LOG.warn("Could not calculate Docker files score: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Calculates CI/CD scripts score based on the number of CI/CD configuration files found.
     * @return CI/CD scripts score from 0 to 100
     */
    private int calculateCicdScriptsScore() {
        if (advancedScanningService == null || !advancedScanningService.hasCachedResults()) {
            return 0; // No scan results = no CI/CD scripts detected
        }

        try {
            // Get comprehensive scan results to extract CI/CD script information
            ComprehensiveScanResults scanResults = advancedScanningService.getLastScanResults();
            if (scanResults == null) {
                return 0;
            }

            // Count CI/CD-related files (.github/workflows, Jenkinsfile, azure-pipelines.yml, etc.)
            int cicdScriptCount = 0;
            // This would be populated from actual scan results
            // For now, using a placeholder calculation based on available data
            
            // Score proportional to CI/CD script count, capped at 100
            // Using a threshold where 15+ CI/CD scripts = max effort
            int maxThreshold = 15;
            double ratio = Math.min(cicdScriptCount / (double) maxThreshold, 1.0);
            return (int) Math.round(ratio * 100);
        } catch (Exception e) {
            LOG.warn("Could not calculate CI/CD scripts score: " + e.getMessage());
            return 0;
        }
    }
}
