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

    // UI Components for gauges (top section)
    private JPanel gaugesPanel;
    private RiskGauge migrationEffortGauge;
    private RiskGauge migrationRiskGauge;
    
    // UI Components for middle section
    private JPanel summaryPanel;
    private JBLabel scanProgressValue;
    private JBLabel dependenciesFoundValue;
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

        // Main dashboard content with three sections
        JPanel mainPanel = new JBPanel<>(new BorderLayout());
        
        // Top: Gauges
        gaugesPanel = createGaugesPanel();
        mainPanel.add(gaugesPanel, BorderLayout.NORTH);
        
        // Middle: Summary information
        summaryPanel = createSummaryPanel();
        mainPanel.add(summaryPanel, BorderLayout.CENTER);
        
        // Bottom: Scan results table
        scanResultsPanel = createScanResultsPanel();
        mainPanel.add(scanResultsPanel, BorderLayout.SOUTH);
        
        contentPanel.add(mainPanel, BorderLayout.CENTER);

        // Actions panel
        JPanel actionsPanel = createActionsPanel();

        panel.add(contentPanel, BorderLayout.CENTER);
        panel.add(actionsPanel, BorderLayout.NORTH);
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
            if (dashboard != null) {
                updateRiskScoreWithAdvancedScans(dashboard.getDependencySummary());
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
        label.setText(String.valueOf(count));
        if (count > 0) {
            label.setForeground(new Color(220, 53, 69)); // Red for issues
        } else {
            label.setForeground(new Color(40, 167, 69)); // Green for no issues
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
        label.setText("0");
        label.setForeground(new Color(40, 167, 69)); // Green for zero
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

    private JPanel createMetricsTable() {
        JPanel tablePanel = new JBPanel<>(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.anchor = GridBagConstraints.WEST;

        // Row 1
        gbc.gridx = 0;
        gbc.gridy = 0;
        tablePanel.add(createKeyLabel("Total Dependencies:"), gbc);
        gbc.gridx = 1;
        totalDepsValue = createValueLabel("-");
        tablePanel.add(totalDepsValue, gbc);

        gbc.gridx = 2;
        gbc.gridy = 0;
        tablePanel.add(createKeyLabel("Affected Dependencies:"), gbc);
        gbc.gridx = 3;
        affectedDepsValue = createValueLabel("-");
        tablePanel.add(affectedDepsValue, gbc);

        // Row 2
        gbc.gridx = 0;
        gbc.gridy = 1;
        tablePanel.add(createKeyLabel("No Jakarta Support:"), gbc);
        gbc.gridx = 1;
        noJakartaSupportValue = createValueLabel("-");
        tablePanel.add(noJakartaSupportValue, gbc);

        gbc.gridx = 2;
        gbc.gridy = 1;
        tablePanel.add(createKeyLabel("Transitive Deps:"), gbc);
        gbc.gridx = 3;
        transitiveDepsValue = createValueLabel("-");
        tablePanel.add(transitiveDepsValue, gbc);

        // Row 3
        gbc.gridx = 0;
        gbc.gridy = 2;
        tablePanel.add(createKeyLabel("XML Files Affected:"), gbc);
        gbc.gridx = 1;
        xmlFilesValue = createValueLabel("-");
        tablePanel.add(xmlFilesValue, gbc);

        gbc.gridx = 2;
        gbc.gridy = 2;
        tablePanel.add(createKeyLabel("Migrable:"), gbc);
        gbc.gridx = 3;
        migrableValue = createValueLabel("-");
        tablePanel.add(migrableValue, gbc);

        // Row 4: JPA, Bean Validation
        gbc.gridx = 0;
        gbc.gridy = 3;
        tablePanel.add(createKeyLabel("JPA Issues:"), gbc);
        gbc.gridx = 1;
        jpaScanCountValue = createValueLabel("0");
        tablePanel.add(jpaScanCountValue, gbc);

        gbc.gridx = 2;
        gbc.gridy = 3;
        tablePanel.add(createKeyLabel("Bean Validation:"), gbc);
        gbc.gridx = 3;
        beanValidationScanCountValue = createValueLabel("0");
        tablePanel.add(beanValidationScanCountValue, gbc);

        // Row 5: Servlet/JSP, CDI
        gbc.gridx = 0;
        gbc.gridy = 4;
        tablePanel.add(createKeyLabel("Servlet/JSP:"), gbc);
        gbc.gridx = 1;
        servletJspScanCountValue = createValueLabel("0");
        tablePanel.add(servletJspScanCountValue, gbc);

        gbc.gridx = 2;
        gbc.gridy = 4;
        tablePanel.add(createKeyLabel("CDI Injection:"), gbc);
        gbc.gridx = 3;
        cdiInjectionScanCountValue = createValueLabel("0");
        tablePanel.add(cdiInjectionScanCountValue, gbc);

        // Row 6: Build, REST/SOAP
        gbc.gridx = 0;
        gbc.gridy = 5;
        tablePanel.add(createKeyLabel("Build Config:"), gbc);
        gbc.gridx = 1;
        buildConfigScanCountValue = createValueLabel("0");
        tablePanel.add(buildConfigScanCountValue, gbc);

        gbc.gridx = 2;
        gbc.gridy = 5;
        tablePanel.add(createKeyLabel("REST/SOAP:"), gbc);
        gbc.gridx = 3;
        restSoapScanCountValue = createValueLabel("0");
        tablePanel.add(restSoapScanCountValue, gbc);

        // Row 7: Deprecated, Security
        gbc.gridx = 0;
        gbc.gridy = 6;
        tablePanel.add(createKeyLabel("Deprecated API:"), gbc);
        gbc.gridx = 1;
        deprecatedApiScanCountValue = createValueLabel("0");
        tablePanel.add(deprecatedApiScanCountValue, gbc);

        gbc.gridx = 2;
        gbc.gridy = 6;
        tablePanel.add(createKeyLabel("Security API:"), gbc);
        gbc.gridx = 3;
        securityApiScanCountValue = createValueLabel("0");
        tablePanel.add(securityApiScanCountValue, gbc);

        // Row 8: JMS Messaging, Transitive Dependency
        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(4, 8, 4, 8);
        tablePanel.add(createKeyLabel("JMS Messaging:"), gbc);
        gbc.gridx = 1;
        jmsMessagingScanCountValue = createValueLabel("0");
        tablePanel.add(jmsMessagingScanCountValue, gbc);

        gbc.gridx = 2;
        tablePanel.add(createKeyLabel("Transitive Deps:"), gbc);
        gbc.gridx = 3;
        transitiveDependencyScanCountValue = createValueLabel("0");
        tablePanel.add(transitiveDependencyScanCountValue, gbc);

        // Row 9: Config Files, Classloader/Module
        gbc.gridx = 0;
        gbc.gridy = 9;
        tablePanel.add(createKeyLabel("Config Files:"), gbc);
        gbc.gridx = 1;
        configFileScanCountValue = createValueLabel("0");
        tablePanel.add(configFileScanCountValue, gbc);

        gbc.gridx = 2;
        tablePanel.add(createKeyLabel("Classloader/Module:"), gbc);
        gbc.gridx = 3;
        classloaderModuleScanCountValue = createValueLabel("0");
        tablePanel.add(classloaderModuleScanCountValue, gbc);

        // Row 10: Logging Metrics, Serialization Cache
        gbc.gridx = 0;
        gbc.gridy = 10;
        tablePanel.add(createKeyLabel("Logging/Metrics:"), gbc);
        gbc.gridx = 1;
        loggingMetricsScanCountValue = createValueLabel("0");
        tablePanel.add(loggingMetricsScanCountValue, gbc);

        gbc.gridx = 2;
        tablePanel.add(createKeyLabel("Serialization/Cache:"), gbc);
        gbc.gridx = 3;
        serializationCacheScanCountValue = createValueLabel("0");
        tablePanel.add(serializationCacheScanCountValue, gbc);

// Row 11: Third-Party Libs (spans width)
        gbc.gridx = 0;
        gbc.gridy = 11;
        gbc.gridwidth = 2;
        tablePanel.add(createKeyLabel("Third-Party Libs:"), gbc);
        gbc.gridx = 2;
        thirdPartyLibScanCountValue = createValueLabel("0");
        tablePanel.add(thirdPartyLibScanCountValue, gbc);

        // Row 12: Total Advanced Issues (at bottom of scan counts, spans full width)
        gbc.gridx = 0;
        gbc.gridy = 12;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 4, 8);
        JLabel totalAdvancedLabel = createKeyLabel("Total Advanced Issues:");
        totalAdvancedLabel.setFont(totalAdvancedLabel.getFont().deriveFont(Font.BOLD));
        tablePanel.add(totalAdvancedLabel, gbc);

        gbc.gridx = 2;
        gbc.gridwidth = 2;
        totalAdvancedScanCountValue = createValueLabel("0");
        totalAdvancedScanCountValue.setForeground(new Color(0, 100, 180));
        tablePanel.add(totalAdvancedScanCountValue, gbc);

        // Row 14 - Advanced Scan Progress Bar (spans full width)
        gbc.gridx = 0;
        gbc.gridy = 14;
        gbc.gridwidth = 4;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(12, 8, 4, 8);
        
        JPanel progressPanel = new JBPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        progressPanel.setOpaque(false);
        
        advancedScanProgressLabel = new JBLabel("Advanced Scans:");
        advancedScanProgressLabel.setFont(advancedScanProgressLabel.getFont().deriveFont(Font.PLAIN, 11f));
        progressPanel.add(advancedScanProgressLabel);
        
        advancedScanProgressBar = new JProgressBar(0, 100);
        advancedScanProgressBar.setStringPainted(true);
        advancedScanProgressBar.setValue(0);
        advancedScanProgressBar.setPreferredSize(new Dimension(200, 20));
        progressPanel.add(advancedScanProgressBar);
        
        tablePanel.add(progressPanel, gbc);

        // Row 16 - Last Analyzed spans full width
        gbc.gridx = 0;
        gbc.gridy = 16;
        gbc.insets = new Insets(4, 8, 4, 8);
        JPanel lastAnalyzedPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 10, 0));
        lastAnalyzedPanel.add(new JBLabel("Last Analyzed:"));
        lastAnalyzedValue = createValueLabel("Never");
        lastAnalyzedPanel.add(lastAnalyzedValue);
        tablePanel.add(lastAnalyzedPanel, gbc);

        // Row 17 - Risk Score spans full width
        gbc.gridy = 17;
        gbc.insets = new Insets(8, 8, 4, 8);
        JPanel riskPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 10, 0));
        
        JLabel riskLabel = new JBLabel("Risk Score:");
        riskLabel.setFont(riskLabel.getFont().deriveFont(Font.BOLD, 12f));
        riskPanel.add(riskLabel);
        
        riskScoreValue = new JBLabel("--");
        riskScoreValue.setFont(riskScoreValue.getFont().deriveFont(Font.BOLD, 14f));
        riskPanel.add(riskScoreValue);
        
        riskPanel.add(Box.createHorizontalStrut(20));
        
        JLabel categoryLabel = new JBLabel("Category:");
        categoryLabel.setFont(categoryLabel.getFont().deriveFont(Font.PLAIN, 12f));
        riskPanel.add(categoryLabel);
        
        riskCategoryValue = new JBLabel("--");
        riskCategoryValue.setFont(riskCategoryValue.getFont().deriveFont(Font.BOLD, 14f));
        riskPanel.add(riskCategoryValue);
        
        tablePanel.add(riskPanel, gbc);

        return tablePanel;
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

    private void handleRefresh(ActionEvent e) {
        Messages.showInfoMessage(project, "Refreshing analysis results...", "Refresh");
    }

    public void setDependencySummary(DependencySummary summary) {
        totalDepsValue.setText(String.valueOf(summary.getTotalDependencies()));
        affectedDepsValue.setText(String.valueOf(summary.getAffectedDependencies()));
        
        // Highlight migrable dependencies in green if > 0
        int migrable = summary.getMigrableDependencies();
        migrableValue.setText(String.valueOf(migrable));
        migrableValue.setForeground(migrable > 0 ? new Color(0, 140, 0) : Color.GRAY);

        // Highlight "No Jakarta Support" in red if > 0 (indicates issues)
        int noSupport = summary.getNoJakartaSupportCount() != null ? summary.getNoJakartaSupportCount() : 0;
        noJakartaSupportValue.setText(String.valueOf(noSupport));
        noJakartaSupportValue.setForeground(noSupport > 0 ? Color.RED : Color.GRAY);

        String xmlFiles = summary.getXmlFilesCount() != null ? String.valueOf(summary.getXmlFilesCount()) : "0";
        xmlFilesValue.setText(xmlFiles);

        // Highlight transitive dependencies in red if > 0 (indicates potential issues)
        int transitive = summary.getTransitiveDependencies() != null ? summary.getTransitiveDependencies() : 0;
        transitiveDepsValue.setText(String.valueOf(transitive));
        transitiveDepsValue.setForeground(transitive > 0 ? Color.RED : Color.GRAY);

        // Update risk score based on dependency summary
        updateRiskScore(summary);
    }

    public void setLastAnalyzed(Instant lastAnalyzed) {
        if (lastAnalyzed != null) {
            lastAnalyzedValue.setText(lastAnalyzed.toString());
        } else {
            lastAnalyzedValue.setText("Never");
        }
    }

    /**
     * Updates the risk score display based on dependency analysis and advanced scans.
     * Now includes both basic dependency issues and advanced scan findings with YAML weights.
     */
    public void updateRiskScore(DependencySummary summary) {
        if (summary == null || summary.getTotalDependencies() == null || summary.getTotalDependencies() == 0) {
            riskScoreValue.setText("--");
            riskCategoryValue.setText("--");
            riskScoreValue.setForeground(Color.GRAY);
            riskCategoryValue.setForeground(Color.GRAY);
            return;
        }

        try {
            RiskScoringService riskService = RiskScoringService.getInstance();
            
            // Build dependency issues map
            Map<String, Integer> depIssues = new HashMap<>();
            
            int noSupport = summary.getNoJakartaSupportCount() != null ? summary.getNoJakartaSupportCount() : 0;
            int affected = summary.getAffectedDependencies() != null ? summary.getAffectedDependencies() : 0;
            int blockers = summary.getBlockerDependencies() != null ? summary.getBlockerDependencies() : 0;
            
            if (noSupport > 0) {
                depIssues.put("noJakartaVersion", noSupport * 25);
            }
            if (blockers > 0) {
                depIssues.put("blockedDependency", blockers * 40);
            }
            if (affected > 0) {
                depIssues.put("directDependency", affected * 10);
            }
            
            // Build scan findings from advanced scans
            Map<String, List<RiskScoringService.RiskFinding>> scanFindings = buildScanFindingsFromAdvancedScans();
            
            // Calculate risk score with both scan findings and dependency issues
            RiskScoringService.RiskScore riskScore = riskService.calculateRiskScore(
                scanFindings,
                depIssues
            );
            
            riskScoreValue.setText(String.valueOf(riskScore.totalScore()));
            riskCategoryValue.setText(riskScore.categoryLabel());
            
            // Set color based on category
            try {
                Color categoryColor = Color.decode(riskScore.categoryColor());
                riskCategoryValue.setForeground(categoryColor);
                riskScoreValue.setForeground(categoryColor);
            } catch (Exception e) {
                // Use default color if parsing fails
            }
            
        } catch (Exception e) {
            LOG.warn("Failed to calculate risk score", e);
            riskScoreValue.setText("--");
            riskCategoryValue.setText("--");
        }
    }

    /**
     * Updates the risk score when advanced scans complete.
     * This method recalculates the risk score including advanced scan findings.
     */
    public void updateRiskScoreWithAdvancedScans(DependencySummary summary) {
        // Re-calculate risk score with advanced scan findings
        updateRiskScore(summary);
    }

    /**
     * Builds scan findings from advanced scanning results.
     * Maps advanced scan counts to RiskFinding objects with appropriate risk levels.
     */
    private Map<String, List<RiskScoringService.RiskFinding>> buildScanFindingsFromAdvancedScans() {
        Map<String, List<RiskScoringService.RiskFinding>> scanFindings = new HashMap<>();
        
        // Get advanced scan summary if available
        if (advancedScanningService == null || !advancedScanningService.hasCachedResults()) {
            return scanFindings; // Return empty map if no advanced scans available
        }
        
        AdvancedScanningService.AdvancedScanSummary scanSummary = advancedScanningService.getCachedSummary();
        if (scanSummary == null) {
            return scanFindings;
        }
        
        // Add JPA findings
        if (scanSummary.getJpaCount() > 0) {
            List<RiskScoringService.RiskFinding> jpaFindings = new ArrayList<>();
            jpaFindings.add(new RiskScoringService.RiskFinding(
                "jpa", "entityWithJakartaId", "Jakarta ID annotation usage", "low", scanSummary.getJpaCount()
            ));
            scanFindings.put("jpa", jpaFindings);
        }
        
        // Add Bean Validation findings
        if (scanSummary.getBeanValidationCount() > 0) {
            List<RiskScoringService.RiskFinding> validationFindings = new ArrayList<>();
            validationFindings.add(new RiskScoringService.RiskFinding(
                "beanValidation", "constraintAnnotation", "Validation constraint annotation", "low", scanSummary.getBeanValidationCount()
            ));
            scanFindings.put("beanValidation", validationFindings);
        }
        
        // Add Servlet/JSP findings
        if (scanSummary.getServletJspCount() > 0) {
            List<RiskScoringService.RiskFinding> servletFindings = new ArrayList<>();
            servletFindings.add(new RiskScoringService.RiskFinding(
                "servlet", "javaxServletImport", "javax.servlet import - needs migration", "high", scanSummary.getServletJspCount()
            ));
            scanFindings.put("servlet", servletFindings);
        }
        
        // Add CDI findings
        if (scanSummary.getCdiInjectionCount() > 0) {
            List<RiskScoringService.RiskFinding> cdiFindings = new ArrayList<>();
            cdiFindings.add(new RiskScoringService.RiskFinding(
                "cdi", "cdiBean", "CDI managed bean", "low", scanSummary.getCdiInjectionCount()
            ));
            scanFindings.put("cdi", cdiFindings);
        }
        
        // Add JMS findings
        if (scanSummary.getJmsMessagingCount() > 0) {
            List<RiskScoringService.RiskFinding> jmsFindings = new ArrayList<>();
            jmsFindings.add(new RiskScoringService.RiskFinding(
                "jms", "jmsQueueConnection", "JMS QueueConnection - needs migration", "high", scanSummary.getJmsMessagingCount()
            ));
            scanFindings.put("jms", jmsFindings);
        }
        
        // Add Web Services findings
        if (scanSummary.getRestSoapCount() > 0) {
            List<RiskScoringService.RiskFinding> webserviceFindings = new ArrayList<>();
            webserviceFindings.add(new RiskScoringService.RiskFinding(
                "webservice", "jaxWsEndpoint", "JAX-WS endpoint - needs migration", "high", scanSummary.getRestSoapCount()
            ));
            scanFindings.put("webservice", webserviceFindings);
        }
        
        // Add Serialization/Cache findings (lower weight)
        if (scanSummary.getSerializationCacheCount() > 0) {
            List<RiskScoringService.RiskFinding> serializationFindings = new ArrayList<>();
            serializationFindings.add(new RiskScoringService.RiskFinding(
                "serializationCache", "Serializable", "Java Serialization usage", "low", scanSummary.getSerializationCacheCount()
            ));
            scanFindings.put("serializationCache", serializationFindings);
        }
        
        return scanFindings;
    }

    public void clearMetrics() {
        totalDepsValue.setText("-");
        affectedDepsValue.setText("-");
        noJakartaSupportValue.setText("-");
        noJakartaSupportValue.setForeground(Color.GRAY);
        xmlFilesValue.setText("-");
        transitiveDepsValue.setText("-");
        transitiveDepsValue.setForeground(Color.GRAY);
        migrableValue.setText("-");
        migrableValue.setForeground(Color.GRAY);
        lastAnalyzedValue.setText("Never");
    }

    /**
     * Update the dashboard from a MigrationDashboard object (called by
     * MigrationToolWindow).
     * 
     * @param dashboard The dashboard with data to display
     */
    public void updateDashboard(MigrationDashboard dashboard) {
        if (dashboard == null) {
            return;
        }

        this.dashboard = dashboard;

        // Update dependency summary
        if (dashboard.getDependencySummary() != null) {
            setDependencySummary(dashboard.getDependencySummary());
        }

        // Update last analyzed
        if (dashboard.getLastAnalyzed() != null) {
            setLastAnalyzed(dashboard.getLastAnalyzed());
        } else {
            setLastAnalyzed(null);
        }

        // Update new dashboard components
        updateGauges();
        updateSummary();
        updateScanResultsTable();
    }

    /**
     * Get the current dashboard state.
     * 
     * @return The current dashboard
     */
    public MigrationDashboard getDashboard() {
        return dashboard;
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
        
        // Migration Effort Gauge (Indicator 1)
        migrationEffortGauge = new RiskGauge("Migration Effort");
        gaugesContainer.add(migrationEffortGauge);
        
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

        // Refactor Recipes Available
        gbc.gridx = 0; gbc.gridy = 2;
        summaryGrid.add(createKeyLabel("Refactor Recipes:"), gbc);
        gbc.gridx = 1;
        refactorRecipesValue = createValueLabel("-");
        summaryGrid.add(refactorRecipesValue, gbc);

        panel.add(summaryGrid, BorderLayout.CENTER);
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
        if (dashboard == null) return;

        // Calculate migration effort score (based on number of items to migrate)
        int effortScore = calculateEffortScore();
        migrationEffortGauge.setScore(effortScore);

        // Calculate migration risk score (using RiskScoringService)
        RiskScoringService riskScoringService = RiskScoringService.getInstance();
        int riskScore = riskScoringService.calculateRiskScore(dashboard);
        migrationRiskGauge.setScore(riskScore);
    }

    /**
     * Updates the summary section with current scan data.
     */
    public void updateSummary() {
        if (dashboard == null) return;

        SwingUtilities.invokeLater(() -> {
            // Update scan progress
            int scanProgress = calculateScanProgress();
            scanProgressValue.setText(scanProgress + "%");
            scanProgressValue.setForeground(getProgressColor(scanProgress));

            // Update dependencies found
            DependencySummary depSummary = dashboard.getDependencySummary();
            if (depSummary != null) {
                int totalDeps = depSummary.getTotalDependencies();
                int affectedDeps = depSummary.getAffectedDependencies();
                dependenciesFoundValue.setText(affectedDeps + " / " + totalDeps);
                dependenciesFoundValue.setForeground(affectedDeps > 0 ? Color.ORANGE : Color.GREEN);
            } else {
                dependenciesFoundValue.setText("-");
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
                    addScanResultRow("Total Advanced", summary.getTotalCount(), getRiskLevelForCount(summary.getTotalCount()));
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

    private int calculateEffortScore() {
        // Simple calculation based on number of items to migrate
        int basicCount = getBasicScanCount();
        int advancedCount = 0;
        
        if (advancedScanningService != null && advancedScanningService.hasCachedResults()) {
            AdvancedScanningService.AdvancedScanSummary summary = advancedScanningService.getCachedSummary();
            if (summary != null) {
                advancedCount = summary.getTotalCount();
            }
        }
        
        int totalItems = basicCount + advancedCount;
        
        // Map to 0-100 scale (more items = higher effort)
        return Math.min(100, totalItems * 2); // Rough scaling
    }

    private int calculateScanProgress() {
        // Calculate percentage of available scans that have been run
        int totalScans = 1; // Basic scan always available
        int completedScans = 1; // Basic scan always completed if dashboard exists
        
        if (advancedScanningService != null && advancedScanningService.hasCachedResults()) {
            totalScans += 11; // 11 advanced scan types
            completedScans += 11; // All advanced scans completed
        }
        
        return (completedScans * 100) / totalScans;
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
}
