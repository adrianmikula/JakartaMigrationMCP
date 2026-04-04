package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.JakartaMcpRegistrationActivity;
import adrianmikula.jakartamigration.intellij.mcp.JakartaMcpServerProvider;
import adrianmikula.jakartamigration.intellij.model.DependencySummary;
import adrianmikula.jakartamigration.intellij.model.MigrationDashboard;
import adrianmikula.jakartamigration.intellij.service.AdvancedScanningService;
import adrianmikula.jakartamigration.intellij.service.RiskScoringService;
import adrianmikula.jakartamigration.intellij.license.CheckLicense;
import adrianmikula.jakartamigration.intellij.ui.SupportComponent;
import adrianmikula.jakartamigration.intellij.ui.components.RiskGauge;
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
public class DashboardComponent {
    private static final Logger LOG = Logger.getInstance(DashboardComponent.class);

    private final JPanel panel;
    private final Project project;
    private MigrationDashboard dashboard;
    private final Consumer<ActionEvent> onAnalyze;
    private final AdvancedScanningService advancedScanningService;
    private PlatformsTabComponent platformsTabComponent;
    
    // Cache for preventing unnecessary updates
    private Integer lastCalculatedRiskScore = null;

    // UI Components for gauges (top section)
    private JPanel gaugesPanel;
    private JBLabel migrationEffortLabel;
    private RiskGauge migrationRiskGauge;
    
    // UI Components for middle section
    private JPanel summaryPanel;
    private JBLabel scanProgressValue;
    private JBLabel dependenciesFoundValue;
    private JBLabel basicDependenciesValue;
    private JBLabel refactorRecipesValue;
    
    // UI Components for scan results table (bottom section)
    private JPanel scanResultsPanel;
    private JBTable scanResultsTable;
    private DefaultTableModel scanResultsModel;
    
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

    public DashboardComponent(@NotNull Project project, AdvancedScanningService advancedScanningService,
            Consumer<ActionEvent> onAnalyze) {
        this.project = project;
        this.onAnalyze = onAnalyze;
        this.advancedScanningService = advancedScanningService;
        this.panel = new JBPanel<>(new BorderLayout());
        initializeComponent();
        
        // Initialize with current scan results if available
        updateScanResultsTable();
    }
    
    /**
     * Sets the migration dashboard data for this component.
     */
    public void setDashboard(MigrationDashboard dashboard) {
        // Only update if dashboard data actually changed
        if (this.dashboard != null && dashboard != null && 
            this.dashboard.getReadinessScore() == dashboard.getReadinessScore() &&
            this.dashboard.getStatus() == dashboard.getStatus() &&
            Objects.equals(this.dashboard.getLastAnalyzed(), dashboard.getLastAnalyzed())) {
            return; // No actual change, skip update
        }
        
        this.dashboard = dashboard;
        
        // Update all components with new data
        updateGauges();
        updateSummary();
        updateScanResultsTable();
    }
    
    /**
     * Gets the current migration dashboard data.
     */
    public MigrationDashboard getDashboard() {
        return dashboard;
    }

    private void initializeComponent() {
        // Main content panel with vertical layout
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

        // Main dashboard content with multiple sections
        JPanel mainPanel = new JBPanel<>(new BorderLayout());
        
        // Top: Gauges
        gaugesPanel = createGaugesPanel();
        mainPanel.add(gaugesPanel, BorderLayout.NORTH);
        
        // Middle: Summary information
        summaryPanel = createSummaryPanel();
        mainPanel.add(summaryPanel, BorderLayout.CENTER);
        
        // Bottom: Combined results panel (advanced scan + table)
        JPanel bottomPanel = new JBPanel<>(new BorderLayout());
        
        // Scan results table (bottom section)
        scanResultsPanel = createScanResultsPanel();
        bottomPanel.add(scanResultsPanel, BorderLayout.CENTER);
        
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        contentPanel.add(mainPanel, BorderLayout.CENTER);

        // Actions panel
        JPanel actionsPanel = createActionsPanel();

        panel.add(contentPanel, BorderLayout.CENTER);
        panel.add(actionsPanel, BorderLayout.SOUTH);
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
            updateScanResultsTable();
        });
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
            label.setText("0");
            label.setForeground(new Color(40, 167, 69)); // Green for zero
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
            JakartaMcpServerProvider provider = JakartaMcpRegistrationActivity.getServerProvider();

            if (provider != null && provider.isReady()) {
                // MCP is connected and ready
                mcpStatusIndicator.setForeground(new Color(0, 180, 0));
                mcpStatusValue.setText("Connected");
                mcpStatusValue.setForeground(new Color(0, 120, 0));

                int toolCount = provider.getToolCount();
                mcpToolsValue.setText(String.valueOf(toolCount));
                mcpToolsValue.setForeground(new Color(0, 100, 200));

                mcpServerVersionValue.setText(provider.getServerVersion());

                LOG.info("MCP Server Status: Connected with " + toolCount + " tools");
            } else if (provider != null) {
                // MCP provider exists but not ready
                mcpStatusIndicator.setForeground(Color.ORANGE);
                mcpStatusValue.setText("Initializing");
                mcpStatusValue.setForeground(Color.ORANGE);

                mcpToolsValue.setText("-");
                mcpToolsValue.setForeground(Color.GRAY);

                mcpServerVersionValue.setText(provider.getServerVersion());

                LOG.info("MCP Server Status: Provider exists but not ready");
            } else {
                // MCP provider not initialized - check if AI Assistant is available
                mcpStatusIndicator.setForeground(Color.GRAY);
                mcpStatusValue.setText("Not Available");
                mcpStatusValue.setForeground(Color.GRAY);

                mcpToolsValue.setText("-");
                mcpServerVersionValue.setText("1.0.0");

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

    private JPanel createActionsPanel() {
        JPanel actionsPanel = new JBPanel<>(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        actionsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Check if premium is already active
        boolean isPremium = adrianmikula.jakartamigration.intellij.license.CheckLicense.isLicensed();
        
        if (!isPremium) {
            // Add trial button
            JButton trialButton = new JButton("Start Free Trial");
            trialButton.setToolTipText("Start a 7-day free trial of Premium features");
            trialButton.addActionListener(e -> {
                // Trigger trial start via project action
                com.intellij.openapi.actionSystem.AnActionEvent event = null;
                // Use CheckLicense.startTrial() method for consistency
                adrianmikula.jakartamigration.intellij.license.CheckLicense.startTrial();
                SupportComponent.setPremiumActive(true);
                Messages.showInfoMessage(project, 
                    "Free trial started! Premium features are now available.\n\nPlease restart the tool window to see all premium features.",
                    "Trial Started");
            });
            actionsPanel.add(trialButton);
            
            // Add upgrade button
            JButton upgradeButton = new JButton("⬆ Upgrade to Premium");
            upgradeButton.setToolTipText("Get Premium features: Auto-fixes, one-click refactoring, binary fixes");
            upgradeButton.setBackground(new Color(255, 215, 0));
            upgradeButton.setForeground(new Color(80, 60, 0));
            upgradeButton.addActionListener(e -> {
                try {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI("https://plugins.jetbrains.com/plugin/30093-jakarta-migration"));
                } catch (Exception ex) {
                    Messages.showErrorDialog(project, "Could not open URL", "Error");
                }
            });
            actionsPanel.add(upgradeButton);
        } else {
            // Show premium badge
            JLabel premiumBadge = new JLabel("⭐ Premium Active");
            premiumBadge.setForeground(new Color(255, 215, 0));
            premiumBadge.setToolTipText("Premium license active");
            actionsPanel.add(premiumBadge);
        }

        JButton refreshButton = new JButton("↻ Refresh");
        refreshButton.setToolTipText("Refresh analysis results");
        refreshButton.addActionListener(this::handleRefresh);
        actionsPanel.add(refreshButton);

        return actionsPanel;
    }

    
    public JPanel getPanel() {
        return panel;
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

    // ==================== New Dashboard Layout Methods ====================

    /**
     * Creates the top section with two speedometer-style gauges.
     * Indicator 1: Migration Effort
     * Indicator 2: Migration Risk
     */
    private JPanel createGaugesPanel() {
        JPanel panel = new JBPanel<>(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Risk Assessment"),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JPanel gaugesContainer = new JBPanel<>(new FlowLayout(FlowLayout.CENTER, 20, 10));
        
        // Migration Effort Label (Indicator 1) - shows estimated weeks
        JPanel effortPanel = new JBPanel<>(new BorderLayout());
        effortPanel.setPreferredSize(new java.awt.Dimension(200, 150));
        
        JLabel effortTitle = new JBLabel("Migration Effort", SwingConstants.CENTER);
        effortTitle.setFont(effortTitle.getFont().deriveFont(Font.BOLD, 12f));
        effortPanel.add(effortTitle, BorderLayout.NORTH);
        
        migrationEffortLabel = new JBLabel("Calculating...", SwingConstants.CENTER);
        migrationEffortLabel.setFont(migrationEffortLabel.getFont().deriveFont(Font.BOLD, 24f));
        migrationEffortLabel.setForeground(new Color(59, 130, 246)); // Blue color
        effortPanel.add(migrationEffortLabel, BorderLayout.CENTER);
        
        JLabel effortSubtitle = new JBLabel("estimated weeks", SwingConstants.CENTER);
        effortSubtitle.setFont(effortSubtitle.getFont().deriveFont(Font.ITALIC, 10f));
        effortPanel.add(effortSubtitle, BorderLayout.SOUTH);
        
        gaugesContainer.add(effortPanel);
        
        // Migration Risk Gauge (Indicator 2)  
        migrationRiskGauge = new RiskGauge("Migration Risk");
        gaugesContainer.add(migrationRiskGauge);
        
        panel.add(gaugesContainer, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Creates the middle section showing scan progress and summary information.
     * Shows percentage of scans run, dependencies found, and refactor recipes.
     */
    private JPanel createSummaryPanel() {
        JPanel panel = new JBPanel<>(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Migration Summary"),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

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

        // Basic Dependencies Summary
        gbc.gridx = 0; gbc.gridy = 2;
        summaryGrid.add(createKeyLabel("Basic Dependencies:"), gbc);
        gbc.gridx = 1;
        basicDependenciesValue = createValueLabel("-");
        summaryGrid.add(basicDependenciesValue, gbc);

        // Refactor Recipes Available
        gbc.gridx = 0; gbc.gridy = 3;
        summaryGrid.add(createKeyLabel("Refactor Recipes:"), gbc);
        gbc.gridx = 1;
        refactorRecipesValue = createValueLabel("-");
        summaryGrid.add(refactorRecipesValue, gbc);

        panel.add(summaryGrid, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Creates the advanced scan results panel showing detailed scan counts.
     * Shows individual scan type results with color coding.
     */
    private JPanel createAdvancedScanResultsPanel() {
        JPanel panel = new JBPanel<>(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Advanced Scan Results"),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JPanel scanGrid = new JBPanel<>(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Initialize all advanced scan count labels
        int row = 0;
        
        // JPA Scan
        gbc.gridx = 0; gbc.gridy = row;
        scanGrid.add(createKeyLabel("JPA Issues:"), gbc);
        gbc.gridx = 1;
        jpaScanCountValue = createValueLabel("0");
        scanGrid.add(jpaScanCountValue, gbc);
        row++;

        // Bean Validation Scan
        gbc.gridx = 0; gbc.gridy = row;
        scanGrid.add(createKeyLabel("Bean Validation:"), gbc);
        gbc.gridx = 1;
        beanValidationScanCountValue = createValueLabel("0");
        scanGrid.add(beanValidationScanCountValue, gbc);
        row++;

        // Servlet/JSP Scan
        gbc.gridx = 0; gbc.gridy = row;
        scanGrid.add(createKeyLabel("Servlet/JSP:"), gbc);
        gbc.gridx = 1;
        servletJspScanCountValue = createValueLabel("0");
        scanGrid.add(servletJspScanCountValue, gbc);
        row++;

        // CDI Injection Scan
        gbc.gridx = 0; gbc.gridy = row;
        scanGrid.add(createKeyLabel("CDI Injection:"), gbc);
        gbc.gridx = 1;
        cdiInjectionScanCountValue = createValueLabel("0");
        scanGrid.add(cdiInjectionScanCountValue, gbc);
        row++;

        // Build Config Scan
        gbc.gridx = 0; gbc.gridy = row;
        scanGrid.add(createKeyLabel("Build Config:"), gbc);
        gbc.gridx = 1;
        buildConfigScanCountValue = createValueLabel("0");
        scanGrid.add(buildConfigScanCountValue, gbc);
        row++;

        // REST/SOAP Scan
        gbc.gridx = 0; gbc.gridy = row;
        scanGrid.add(createKeyLabel("REST/SOAP:"), gbc);
        gbc.gridx = 1;
        restSoapScanCountValue = createValueLabel("0");
        scanGrid.add(restSoapScanCountValue, gbc);
        row++;

        // Deprecated API Scan
        gbc.gridx = 0; gbc.gridy = row;
        scanGrid.add(createKeyLabel("Deprecated APIs:"), gbc);
        gbc.gridx = 1;
        deprecatedApiScanCountValue = createValueLabel("0");
        scanGrid.add(deprecatedApiScanCountValue, gbc);
        row++;

        // Security API Scan
        gbc.gridx = 0; gbc.gridy = row;
        scanGrid.add(createKeyLabel("Security APIs:"), gbc);
        gbc.gridx = 1;
        securityApiScanCountValue = createValueLabel("0");
        scanGrid.add(securityApiScanCountValue, gbc);
        row++;

        // JMS Messaging Scan
        gbc.gridx = 0; gbc.gridy = row;
        scanGrid.add(createKeyLabel("JMS Messaging:"), gbc);
        gbc.gridx = 1;
        jmsMessagingScanCountValue = createValueLabel("0");
        scanGrid.add(jmsMessagingScanCountValue, gbc);
        row++;

        // Transitive Dependencies Scan
        gbc.gridx = 0; gbc.gridy = row;
        scanGrid.add(createKeyLabel("Transitive Deps:"), gbc);
        gbc.gridx = 1;
        transitiveDependencyScanCountValue = createValueLabel("0");
        scanGrid.add(transitiveDependencyScanCountValue, gbc);
        row++;

        // Config Files Scan
        gbc.gridx = 0; gbc.gridy = row;
        scanGrid.add(createKeyLabel("Config Files:"), gbc);
        gbc.gridx = 1;
        configFileScanCountValue = createValueLabel("0");
        scanGrid.add(configFileScanCountValue, gbc);
        row++;

        // Classloader/Module Scan
        gbc.gridx = 0; gbc.gridy = row;
        scanGrid.add(createKeyLabel("Classloader/Module:"), gbc);
        gbc.gridx = 1;
        classloaderModuleScanCountValue = createValueLabel("0");
        scanGrid.add(classloaderModuleScanCountValue, gbc);
        row++;

        // Logging Metrics Scan
        gbc.gridx = 0; gbc.gridy = row;
        scanGrid.add(createKeyLabel("Logging Metrics:"), gbc);
        gbc.gridx = 1;
        loggingMetricsScanCountValue = createValueLabel("0");
        scanGrid.add(loggingMetricsScanCountValue, gbc);
        row++;

        // Serialization/Cache Scan
        gbc.gridx = 0; gbc.gridy = row;
        scanGrid.add(createKeyLabel("Serialization/Cache:"), gbc);
        gbc.gridx = 1;
        serializationCacheScanCountValue = createValueLabel("0");
        scanGrid.add(serializationCacheScanCountValue, gbc);
        row++;

        // Third-party Libraries Scan
        gbc.gridx = 0; gbc.gridy = row;
        scanGrid.add(createKeyLabel("Third-party Libs:"), gbc);
        gbc.gridx = 1;
        thirdPartyLibScanCountValue = createValueLabel("0");
        scanGrid.add(thirdPartyLibScanCountValue, gbc);
        row++;

        // Total Issues
        gbc.gridx = 0; gbc.gridy = row;
        scanGrid.add(createKeyLabel("Total Issues:"), gbc);
        gbc.gridx = 1;
        totalAdvancedScanCountValue = createValueLabel("0");
        scanGrid.add(totalAdvancedScanCountValue, gbc);

        panel.add(scanGrid, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Creates the bottom section with a table of scan results.
     * Shows scan name, number of items found, and risk level.
     */
    private JPanel createScanResultsPanel() {
        JPanel panel = new JBPanel<>(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Scan Results"),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        // Create table model
        scanResultsModel = new DefaultTableModel();
        scanResultsModel.addColumn("Scan Name");
        scanResultsModel.addColumn("Items Found");
        scanResultsModel.addColumn("Risk Level");

        // Create table
        scanResultsTable = new JBTable(scanResultsModel);
        scanResultsTable.setRowHeight(25);
        scanResultsTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        scanResultsTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        scanResultsTable.getColumnModel().getColumn(2).setPreferredWidth(100);

        // Add scroll pane
        JBScrollPane scrollPane = new JBScrollPane(scanResultsTable);
        scrollPane.setPreferredSize(new Dimension(450, 200));

        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Updates the gauges with current risk scores.
     */
    public void updateGauges() {
        if (dashboard == null) {
            return;
        }

        // Calculate migration effort score (based on number of items to migrate)
        int effortWeeks = calculateEffortWeeks();
        migrationEffortLabel.setText(effortWeeks + " weeks");
        
        // Color code the effort text
        if (effortWeeks <= 4) {
            migrationEffortLabel.setForeground(new Color(40, 167, 69)); // Green - low effort
        } else if (effortWeeks <= 12) {
            migrationEffortLabel.setForeground(new Color(255, 193, 7)); // Yellow - medium effort
        } else {
            migrationEffortLabel.setForeground(new Color(220, 53, 69)); // Red - high effort
        }

        // Calculate migration risk score (using RiskScoringService)
        RiskScoringService riskScoringService = RiskScoringService.getInstance();
        Map<String, List<RiskScoringService.RiskFinding>> scanFindings = new HashMap<>();
        Map<String, Integer> depIssues = new HashMap<>();
        
        // Build dependency issues map
        DependencySummary depSummary = dashboard.getDependencySummary();
        if (depSummary != null) {
            int noSupport = depSummary.getNoJakartaSupportCount() != null ? depSummary.getNoJakartaSupportCount() : 0;
            int affected = depSummary.getAffectedDependencies() != null ? depSummary.getAffectedDependencies() : 0;
            int blockers = depSummary.getBlockerDependencies() != null ? depSummary.getBlockerDependencies() : 0;
            
            if (noSupport > 0) {
                depIssues.put("noJakartaVersion", noSupport * 25);
            }
            if (blockers > 0) {
                depIssues.put("blockedDependency", blockers * 40);
            }
            if (affected > 0) {
                depIssues.put("directDependency", affected * 10);
            }
        }
        
        // Build scan findings from advanced scans
        if (advancedScanningService != null && advancedScanningService.hasCachedResults()) {
            AdvancedScanningService.AdvancedScanSummary summary = advancedScanningService.getCachedSummary();
            if (summary != null) {
                // Create RiskFinding objects for each scan type
                List<RiskScoringService.RiskFinding> jpaFindings = createRiskFindings(summary.getJpaCount(), "jpa");
                List<RiskScoringService.RiskFinding> bvFindings = createRiskFindings(summary.getBeanValidationCount(), "beanValidation");
                List<RiskScoringService.RiskFinding> sjFindings = createRiskFindings(summary.getServletJspCount(), "servletJsp");
                List<RiskScoringService.RiskFinding> cdiFindings = createRiskFindings(summary.getCdiInjectionCount(), "cdiInjection");
                List<RiskScoringService.RiskFinding> bcFindings = createRiskFindings(summary.getBuildConfigCount(), "buildConfig");
                List<RiskScoringService.RiskFinding> rsFindings = createRiskFindings(summary.getRestSoapCount(), "restSoap");
                List<RiskScoringService.RiskFinding> daFindings = createRiskFindings(summary.getDeprecatedApiCount(), "deprecatedApi");
                List<RiskScoringService.RiskFinding> saFindings = createRiskFindings(summary.getSecurityApiCount(), "securityApi");
                List<RiskScoringService.RiskFinding> jmFindings = createRiskFindings(summary.getJmsMessagingCount(), "jmsMessaging");
                List<RiskScoringService.RiskFinding> cfFindings = createRiskFindings(summary.getConfigFileCount(), "configFiles");
                
                scanFindings.put("jpa", jpaFindings);
                scanFindings.put("beanValidation", bvFindings);
                scanFindings.put("servletJsp", sjFindings);
                scanFindings.put("cdiInjection", cdiFindings);
                scanFindings.put("buildConfig", bcFindings);
                scanFindings.put("restSoap", rsFindings);
                scanFindings.put("deprecatedApi", daFindings);
                scanFindings.put("securityApi", saFindings);
                scanFindings.put("jmsMessaging", jmFindings);
                scanFindings.put("configFiles", cfFindings);
            }
        }
        
        // Calculate risk score (with caching to prevent unnecessary recalculations)
        int totalFileCount = getTotalFileCount();
        double platformRiskScore = getPlatformRiskScore();
        RiskScoringService.RiskScore riskScore = riskScoringService.calculateRiskScore(scanFindings, depIssues, totalFileCount, platformRiskScore);
        int newScore = (int) Math.round(riskScore.totalScore());
        
        // Only update gauge if score actually changed
        if (lastCalculatedRiskScore == null || !lastCalculatedRiskScore.equals(newScore)) {
            lastCalculatedRiskScore = newScore;
            migrationRiskGauge.setScore(newScore);
        }
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

            // Update dependencies found
            DependencySummary depSummary = dashboard.getDependencySummary();
            if (depSummary != null) {
                int totalDeps = depSummary.getTotalDependencies();
                int affectedDeps = depSummary.getAffectedDependencies();
                dependenciesFoundValue.setText(affectedDeps + " / " + totalDeps);
                dependenciesFoundValue.setForeground(affectedDeps > 0 ? Color.ORANGE : Color.GREEN);
                
                // Update basic dependencies summary
                int basicDeps = totalDeps - affectedDeps; // Basic = total - affected
                basicDependenciesValue.setText(basicDeps + " compatible");
                basicDependenciesValue.setForeground(basicDeps > 0 ? Color.GREEN : Color.GRAY);
            } else {
                dependenciesFoundValue.setText("-");
                basicDependenciesValue.setText("-");
            }

            // Update refactor recipes (this would need integration with recipe service)
            refactorRecipesValue.setText("Calculating...");
        });
    }

    /**
     * Updates the scan results table with current scan data.
     */
    public void updateScanResultsTable() {
        if (dashboard == null) return;

        SwingUtilities.invokeLater(() -> {
            // Clear existing data
            scanResultsModel.setRowCount(0);

            // Add basic scan results
            addScanResultRow("Basic Analysis", getBasicScanCount(), getRiskLevelForCount(getBasicScanCount()));
            
            // Add advanced scan results if available
            if (advancedScanningService != null && advancedScanningService.hasCachedResults()) {
                AdvancedScanningService.AdvancedScanSummary summary = advancedScanningService.getCachedSummary();
                if (summary != null) {
                    addScanResultRow("JPA Issues", summary.getJpaCount(), getRiskLevelForCount(summary.getJpaCount()));
                    addScanResultRow("Bean Validation", summary.getBeanValidationCount(), getRiskLevelForCount(summary.getBeanValidationCount()));
                    addScanResultRow("Servlet/JSP", summary.getServletJspCount(), getRiskLevelForCount(summary.getServletJspCount()));
                    addScanResultRow("CDI Injection", summary.getCdiInjectionCount(), getRiskLevelForCount(summary.getCdiInjectionCount()));
                    addScanResultRow("Build Config", summary.getBuildConfigCount(), getRiskLevelForCount(summary.getBuildConfigCount()));
                    addScanResultRow("REST/SOAP", summary.getRestSoapCount(), getRiskLevelForCount(summary.getRestSoapCount()));
                    addScanResultRow("Deprecated API", summary.getDeprecatedApiCount(), getRiskLevelForCount(summary.getDeprecatedApiCount()));
                    addScanResultRow("Security API", summary.getSecurityApiCount(), getRiskLevelForCount(summary.getSecurityApiCount()));
                    addScanResultRow("JMS Messaging", summary.getJmsMessagingCount(), getRiskLevelForCount(summary.getJmsMessagingCount()));
                    addScanResultRow("Config Files", summary.getConfigFileCount(), getRiskLevelForCount(summary.getConfigFileCount()));
                    addScanResultRow("Total Advanced", summary.getTotalIssuesFound(), getRiskLevelForCount(summary.getTotalIssuesFound()));
                }
            }
        });
    }

    // ==================== Helper Methods ====================

    private void addScanResultRow(String scanName, int count, String riskLevel) {
        Object[] row = {scanName, count, riskLevel};
        scanResultsModel.addRow(row);
    }

    private int getBasicScanCount() {
        if (dashboard == null || dashboard.getDependencySummary() == null) return 0;
        return dashboard.getDependencySummary().getAffectedDependencies();
    }

    private String getRiskLevelForCount(int count) {
        if (count == 0) return "Low";
        if (count < 10) return "Medium";
        return "High";
    }

    private int calculateEffortWeeks() {
        // Calculate effort in weeks: square root of (risk score * 2)
        // Using the actual risk score from RiskScoringService
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
                    
                    if (noSupport > 0) depIssues.put("noJakartaVersion", noSupport * 25);
                    if (affected > 0) depIssues.put("affectedDependencies", affected * 10);
                    if (blockers > 0) depIssues.put("blockerDependencies", blockers * 50);
                }
                
                // Add basic scan findings
                int basicCount = getBasicScanCount();
                if (basicCount > 0) {
                    scanFindings.put("basic", createRiskFindings(basicCount, "basic"));
                }
                
                RiskScoringService.RiskScore currentScore = riskScoringService.calculateRiskScore(
                    scanFindings, depIssues, getTotalFileCount(), getPlatformRiskScore());
                currentRiskScore = currentScore.totalScore();
            } catch (Exception e) {
                LOG.warn("Could not calculate current risk score for effort calculation: " + e.getMessage());
                currentRiskScore = 25; // Default to medium risk
            }
        }
        
        // Calculate weeks as square root of (risk score * 2)
        int effortWeeks = (int) Math.ceil(Math.sqrt(Math.max(0, currentRiskScore * 2)));
        
        // DEBUG: Log effort calculation
        System.out.println("DEBUG: Effort calculation - Risk Score: " + currentRiskScore + 
                          ", Weeks: " + effortWeeks);
        
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

    private int calculateOverallScanProgress() {
        // Calculate percentage of all available scans that have been run
        // Basic scan (1) + Advanced scans (11) + Platform scans (1) = 13 total
        int totalScans = 1; // Basic scan always available
        int completedScans = 0;
        
        // Check if basic scan is completed
        if (dashboard != null && dashboard.getDependencySummary() != null) {
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

    private void handleRefresh(ActionEvent e) {
        Messages.showInfoMessage(project, "Refreshing analysis results...", "Refresh");
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
}
