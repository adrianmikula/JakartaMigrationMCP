package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.JakartaMcpRegistrationActivity;
import adrianmikula.jakartamigration.intellij.mcp.JakartaMcpServerProvider;
import adrianmikula.jakartamigration.intellij.model.DependencySummary;
import adrianmikula.jakartamigration.intellij.model.MigrationDashboard;
import adrianmikula.jakartamigration.intellij.model.MigrationStatus;
import adrianmikula.jakartamigration.intellij.service.AdvancedScanningService;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.Instant;
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

    // UI Components for key:value table
    private JPanel metricsTablePanel;
    private JBLabel totalDepsValue;
    private JBLabel affectedDepsValue;
    private JBLabel noJakartaSupportValue;
    private JBLabel xmlFilesValue;
    private JBLabel transitiveDepsValue;
    private JBLabel migrableValue;
    private JBLabel lastAnalyzedValue;
    private JBLabel statusIndicator;

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
    private JBLabel totalAdvancedScanCountValue;

    // Status indicator panel
    private JPanel statusPanel;

    public DashboardComponent(@NotNull Project project, AdvancedScanningService advancedScanningService,
            Consumer<ActionEvent> onAnalyze) {
        this.project = project;
        this.onAnalyze = onAnalyze;
        this.advancedScanningService = advancedScanningService;
        this.panel = new JBPanel<>(new BorderLayout());
        initializeComponent();
    }

    private void initializeComponent() {
        // Content - Key:Value Table
        JPanel contentPanel = new JBPanel<>(new BorderLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = new JLabel("Migration Summary", SwingConstants.LEFT);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        contentPanel.add(titleLabel, BorderLayout.NORTH);

        metricsTablePanel = createMetricsTable();
        contentPanel.add(metricsTablePanel, BorderLayout.CENTER);

        // Actions panel
        JPanel actionsPanel = createActionsPanel();

        // MCP Server Status Panel (at the bottom)
        JPanel mcpStatusFooter = createMcpStatusPanel();

        panel.add(contentPanel, BorderLayout.CENTER);
        panel.add(actionsPanel, BorderLayout.NORTH);
        panel.add(mcpStatusFooter, BorderLayout.SOUTH);

        // Update MCP status after initialization
        updateMcpServerStatus();
    }

    /**
     * Updates the advanced scan counts from cached results.
     * Should be called when the dashboard is shown or after scans complete.
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
            jpaScanCountValue.setText(String.valueOf(summary.getJpaCount()));
            beanValidationScanCountValue.setText(String.valueOf(summary.getBeanValidationCount()));
            servletJspScanCountValue.setText(String.valueOf(summary.getServletJspCount()));
            cdiInjectionScanCountValue.setText(String.valueOf(summary.getCdiInjectionCount()));
            buildConfigScanCountValue.setText(String.valueOf(summary.getBuildConfigCount()));
            restSoapScanCountValue.setText(String.valueOf(summary.getRestSoapCount()));
            deprecatedApiScanCountValue.setText(String.valueOf(summary.getDeprecatedApiCount()));
            securityApiScanCountValue.setText(String.valueOf(summary.getSecurityApiCount()));
            jmsMessagingScanCountValue.setText(String.valueOf(summary.getJmsMessagingCount()));
            transitiveDependencyScanCountValue.setText(String.valueOf(summary.getTransitiveDependencyCount()));
            configFileScanCountValue.setText(String.valueOf(summary.getConfigFileCount()));
            classloaderModuleScanCountValue.setText(String.valueOf(summary.getClassloaderModuleCount()));
            totalAdvancedScanCountValue.setText(String.valueOf(summary.getTotalIssuesFound()));
        });
    }

    /**
     * Resets all advanced scan counts to zero.
     */
    private void resetAdvancedScanCounts() {
        SwingUtilities.invokeLater(() -> {
            jpaScanCountValue.setText("0");
            beanValidationScanCountValue.setText("0");
            servletJspScanCountValue.setText("0");
            cdiInjectionScanCountValue.setText("0");
            buildConfigScanCountValue.setText("0");
            restSoapScanCountValue.setText("0");
            deprecatedApiScanCountValue.setText("0");
            securityApiScanCountValue.setText("0");
            jmsMessagingScanCountValue.setText("0");
            transitiveDependencyScanCountValue.setText("0");
            configFileScanCountValue.setText("0");
            classloaderModuleScanCountValue.setText("0");
            totalAdvancedScanCountValue.setText("0");
        });
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

        // Row 8: Total Advanced Issues (Spans width)
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 2;
        JLabel totalAdvancedLabel = createKeyLabel("Total Advanced Issues:");
        totalAdvancedLabel.setFont(totalAdvancedLabel.getFont().deriveFont(Font.BOLD));
        tablePanel.add(totalAdvancedLabel, gbc);

        gbc.gridx = 2;
        gbc.gridwidth = 2;
        totalAdvancedScanCountValue = createValueLabel("0");
        totalAdvancedScanCountValue.setForeground(new Color(0, 100, 180));
        tablePanel.add(totalAdvancedScanCountValue, gbc);

        // Row 10: JMS Messaging, Transitive Dependency (Hidden/Extra)
        // Note: These are initialized and updated even if not currently in the main 2x4
        // grid
        jmsMessagingScanCountValue = createValueLabel("0");
        transitiveDependencyScanCountValue = createValueLabel("0");
        configFileScanCountValue = createValueLabel("0");
        classloaderModuleScanCountValue = createValueLabel("0");

        // Row 9 - Status indicator spans full width
        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.gridwidth = 4;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(12, 8, 4, 8);
        statusPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 10, 0));
        statusIndicator = new JBLabel("●");
        statusIndicator.setFont(statusIndicator.getFont().deriveFont(Font.BOLD, 14f));
        statusPanel.add(statusIndicator);
        statusPanel.add(new JBLabel("Jakarta Status Indicator"));
        tablePanel.add(statusPanel, gbc);

        // Row 10 - Last Analyzed spans full width
        gbc.gridy = 9;
        gbc.insets = new Insets(4, 8, 4, 8);
        JPanel lastAnalyzedPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 10, 0));
        lastAnalyzedPanel.add(new JBLabel("Last Analyzed:"));
        lastAnalyzedValue = createValueLabel("Never");
        lastAnalyzedPanel.add(lastAnalyzedValue);
        tablePanel.add(lastAnalyzedPanel, gbc);

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

        JButton refreshButton = new JButton("Refresh");
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
        migrableValue.setText(String.valueOf(summary.getMigrableDependencies()));

        // Task 6 Fixes: correctly populate remaining fields if available
        noJakartaSupportValue.setText(
                summary.getNoJakartaSupportCount() != null ? String.valueOf(summary.getNoJakartaSupportCount()) : "-");
        xmlFilesValue.setText(summary.getXmlFilesCount() != null ? String.valueOf(summary.getXmlFilesCount()) : "-");
        transitiveDepsValue.setText(
                summary.getTransitiveDependencies() != null ? String.valueOf(summary.getTransitiveDependencies())
                        : "-");
    }

    public void setLastAnalyzed(Instant lastAnalyzed) {
        if (lastAnalyzed != null) {
            lastAnalyzedValue.setText(lastAnalyzed.toString());
        } else {
            lastAnalyzedValue.setText("Never");
        }
    }

    public void setStatusAndColor(MigrationStatus status, Color color) {
        statusIndicator.setForeground(color);
        dashboard = new MigrationDashboard();
        dashboard.setStatus(status);
    }

    public void clearMetrics() {
        totalDepsValue.setText("-");
        affectedDepsValue.setText("-");
        noJakartaSupportValue.setText("-");
        xmlFilesValue.setText("-");
        transitiveDepsValue.setText("-");
        migrableValue.setText("-");
        lastAnalyzedValue.setText("Never");
        statusIndicator.setForeground(Color.GRAY);
    }

    /**
     * Set the migration status (called by MigrationToolWindow).
     * 
     * @param status The new status
     */
    public void setStatus(MigrationStatus status) {
        if (status != null) {
            Color color = getStatusColor(status);
            statusIndicator.setForeground(color);
            dashboard = new MigrationDashboard();
            dashboard.setStatus(status);
        }
    }

    /**
     * Get the color for a given status.
     */
    private Color getStatusColor(MigrationStatus status) {
        return switch (status) {
            case READY -> new Color(0, 140, 0);
            case IN_PROGRESS -> Color.BLUE;
            case HAS_BLOCKERS -> new Color(200, 140, 0);
            case FAILED -> Color.RED;
            default -> Color.GRAY;
        };
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

        // Update status
        if (dashboard.getStatus() != null) {
            setStatus(dashboard.getStatus());
        }

        // Update last analyzed
        if (dashboard.getLastAnalyzed() != null) {
            setLastAnalyzed(dashboard.getLastAnalyzed());
        } else {
            setLastAnalyzed(null);
        }
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

    public JBLabel getStatusIndicator() {
        return statusIndicator;
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

    public JBLabel getDeprecatedApiScanCountValue() {
        return deprecatedApiScanCountValue;
    }

    public JBLabel getSecurityApiScanCountValue() {
        return securityApiScanCountValue;
    }

    public JBLabel getTotalAdvancedScanCountValue() {
        return totalAdvancedScanCountValue;
    }
}
