package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.mcp.AnalyzeMigrationImpactResponse;
import adrianmikula.jakartamigration.intellij.mcp.DefaultMcpClientService;
import adrianmikula.jakartamigration.intellij.mcp.McpClientService;
import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import adrianmikula.jakartamigration.intellij.model.DependencySummary;
import adrianmikula.jakartamigration.intellij.model.MigrationDashboard;
import adrianmikula.jakartamigration.intellij.model.MigrationStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

/**
 * Dashboard component with text-based key:value table and MCP tool buttons.
 * Connects directly to the real MCP server for Jakarta migration analysis.
 */
public class DashboardComponent {
    private static final Logger LOG = LoggerFactory.getLogger(DashboardComponent.class);
    
    private final JPanel panel;
    private final Project project;
    private MigrationDashboard dashboard;
    private final Consumer<ActionEvent> onAnalyze;
    private final McpClientService mcpClient;
    private final ObjectMapper objectMapper;

    // UI Components for key:value table
    private JPanel metricsTablePanel;
    private JBLabel readinessScoreValue;
    private JBLabel totalDepsValue;
    private JBLabel affectedDepsValue;
    private JBLabel noJakartaSupportValue;
    private JBLabel xmlFilesValue;
    private JBLabel transitiveDepsValue;
    private JBLabel blockersValue;
    private JBLabel migrableValue;
    private JBLabel lastAnalyzedValue;
    private JBLabel statusValue;
    private JBLabel statusIndicator;

    // MCP Tool buttons
    private JButton analyzeReadinessBtn;
    private JButton detectBlockersBtn;
    private JButton recommendVersionsBtn;
    private JButton analyzeImpactBtn;

    // Status indicator panel
    private JPanel statusPanel;

    public DashboardComponent(@NotNull Project project, Consumer<ActionEvent> onAnalyze) {
        this.project = project;
        this.onAnalyze = onAnalyze;
        this.mcpClient = new DefaultMcpClientService();
        this.objectMapper = new ObjectMapper();
        this.panel = new JBPanel<>(new BorderLayout());
        initializeComponent();
    }

    private void initializeComponent() {
        // MCP Tool Button Bar (along the top)
        JPanel toolButtonBar = createToolButtonBar();

        // Content - Key:Value Table (borderless, vertical)
        JPanel contentPanel = new JBPanel<>(new BorderLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = new JLabel("Migration Summary", SwingConstants.LEFT);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        contentPanel.add(titleLabel, BorderLayout.NORTH);

        metricsTablePanel = createMetricsTable();
        contentPanel.add(metricsTablePanel, BorderLayout.CENTER);

        // Actions panel
        JPanel actionsPanel = createActionsPanel();

        panel.add(toolButtonBar, BorderLayout.NORTH);
        panel.add(contentPanel, BorderLayout.CENTER);
        panel.add(actionsPanel, BorderLayout.SOUTH);
    }

    private JPanel createToolButtonBar() {
        JPanel buttonBar = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 5, 5));
        buttonBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));

        JLabel toolsLabel = new JLabel("MCP Tools:");
        toolsLabel.setFont(toolsLabel.getFont().deriveFont(Font.BOLD, 11f));
        buttonBar.add(toolsLabel);

        // FREE Tools
        analyzeReadinessBtn = createToolButton("Analyze Readiness", () -> handleToolAction("analyzeReadiness"));
        buttonBar.add(analyzeReadinessBtn);

        detectBlockersBtn = createToolButton("Detect Blockers", () -> handleToolAction("detectBlockers"));
        buttonBar.add(detectBlockersBtn);

        recommendVersionsBtn = createToolButton("Recommend Versions", () -> handleToolAction("recommendVersions"));
        buttonBar.add(recommendVersionsBtn);

        // Separator
        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        separator.setPreferredSize(new Dimension(2, 24));
        buttonBar.add(separator);

        // Analyze Impact (Main analysis)
        analyzeImpactBtn = createToolButton("Analyze Impact ⭐", () -> handleToolAction("analyzeImpact"));
        buttonBar.add(analyzeImpactBtn);

        return buttonBar;
    }

    private JButton createToolButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.setToolTipText("Run " + text + " MCP tool");
        button.addActionListener(e -> {
            setButtonsEnabled(false);
            action.run();
        });
        button.setFont(button.getFont().deriveFont(Font.PLAIN, 11f));
        button.setMargin(new Insets(4, 8, 4, 8));
        return button;
    }

    private void setButtonsEnabled(boolean enabled) {
        SwingUtilities.invokeLater(() -> {
            analyzeReadinessBtn.setEnabled(enabled);
            detectBlockersBtn.setEnabled(enabled);
            recommendVersionsBtn.setEnabled(enabled);
            analyzeImpactBtn.setEnabled(enabled);
        });
    }

    private JPanel createMetricsTable() {
        JPanel tablePanel = new JBPanel<>(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.anchor = GridBagConstraints.WEST;

        // Row 1
        gbc.gridx = 0; gbc.gridy = 0;
        tablePanel.add(createKeyLabel("Readiness Score:"), gbc);
        gbc.gridx = 1;
        readinessScoreValue = createValueLabel("-");
        tablePanel.add(readinessScoreValue, gbc);

        gbc.gridx = 2; gbc.gridy = 0;
        tablePanel.add(createKeyLabel("Total Dependencies:"), gbc);
        gbc.gridx = 3;
        totalDepsValue = createValueLabel("-");
        tablePanel.add(totalDepsValue, gbc);

        // Row 2
        gbc.gridx = 0; gbc.gridy = 1;
        tablePanel.add(createKeyLabel("Affected Dependencies:"), gbc);
        gbc.gridx = 1;
        affectedDepsValue = createValueLabel("-");
        tablePanel.add(affectedDepsValue, gbc);

        gbc.gridx = 2; gbc.gridy = 1;
        tablePanel.add(createKeyLabel("No Jakarta Support:"), gbc);
        gbc.gridx = 3;
        noJakartaSupportValue = createValueLabel("-");
        tablePanel.add(noJakartaSupportValue, gbc);

        // Row 3
        gbc.gridx = 0; gbc.gridy = 2;
        tablePanel.add(createKeyLabel("XML Files Affected:"), gbc);
        gbc.gridx = 1;
        xmlFilesValue = createValueLabel("-");
        tablePanel.add(xmlFilesValue, gbc);

        gbc.gridx = 2; gbc.gridy = 2;
        tablePanel.add(createKeyLabel("Transitive Deps:"), gbc);
        gbc.gridx = 3;
        transitiveDepsValue = createValueLabel("-");
        tablePanel.add(transitiveDepsValue, gbc);

        // Row 4
        gbc.gridx = 0; gbc.gridy = 3;
        tablePanel.add(createKeyLabel("Blockers:"), gbc);
        gbc.gridx = 1;
        blockersValue = createValueLabel("-");
        tablePanel.add(blockersValue, gbc);

        gbc.gridx = 2; gbc.gridy = 3;
        tablePanel.add(createKeyLabel("Migrable:"), gbc);
        gbc.gridx = 3;
        migrableValue = createValueLabel("-");
        tablePanel.add(migrableValue, gbc);

        // Row 5 - Status spans full width
        gbc.gridx = 0; gbc.gridy = 4;
        gbc.gridwidth = 4;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(12, 8, 4, 8);
        statusPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 10, 0));
        statusIndicator = new JBLabel("●");
        statusIndicator.setFont(statusIndicator.getFont().deriveFont(Font.BOLD, 14f));
        statusPanel.add(statusIndicator);
        statusPanel.add(new JBLabel("Status:"));
        statusValue = createValueLabel("Not Analyzed");
        statusValue.setFont(statusValue.getFont().deriveFont(Font.BOLD));
        statusPanel.add(statusValue);
        tablePanel.add(statusPanel, gbc);

        // Row 6 - Last Analyzed spans full width
        gbc.gridy = 5;
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

        JButton analyzeButton = new JButton("▶ Analyze Project");
        analyzeButton.setToolTipText("Run full migration analysis on this project");
        analyzeButton.addActionListener(this::handleAnalyze);
        actionsPanel.add(analyzeButton);

        JButton refreshButton = new JButton("↻ Refresh");
        refreshButton.setToolTipText("Refresh analysis results");
        refreshButton.addActionListener(this::handleRefresh);
        actionsPanel.add(refreshButton);

        return actionsPanel;
    }

    private void handleToolAction(String toolName) {
        String projectPath = project.getBasePath();
        if (projectPath == null) {
            projectPath = project.getProjectFilePath();
        }

        if (projectPath == null) {
            setStatusAndColor(MigrationStatus.NOT_ANALYZED, Color.GRAY);
            Messages.showWarningDialog(project, "Cannot determine project path. Please open a project first.", "Analysis Failed");
            setButtonsEnabled(true);
            return;
        }

        LOG.info("Executing MCP tool: {} for project: {}", toolName, projectPath);
        setStatusAndColor(MigrationStatus.IN_PROGRESS, Color.BLUE);

        switch (toolName) {
            case "analyzeReadiness":
                executeAnalyzeReadiness(projectPath);
                break;

            case "detectBlockers":
                executeDetectBlockers(projectPath);
                break;

            case "recommendVersions":
                executeRecommendVersions(projectPath);
                break;

            case "analyzeImpact":
                // Trigger the main analyze action which handles the full analysis
                if (onAnalyze != null) {
                    onAnalyze.accept(null);
                }
                setButtonsEnabled(true);
                break;

            default:
                setStatusAndColor(MigrationStatus.NOT_ANALYZED, Color.GRAY);
                Messages.showInfoMessage(project,
                    "MCP Tool: " + toolName + "\n\nThis feature would invoke the " + toolName + " MCP tool.",
                    "MCP Tool: " + toolName);
                setButtonsEnabled(true);
        }
    }

    private void executeAnalyzeReadiness(String projectPath) {
        mcpClient.analyzeReadiness(projectPath)
            .thenAccept(responseJson -> SwingUtilities.invokeLater(() -> {
                try {
                    LOG.debug("Analyze readiness response: {}", responseJson);
                    
                    // Parse the JSON response
                    JsonNode root = objectMapper.readTree(responseJson);
                    
                    // Check for error
                    if (root.has("error")) {
                        String errorMsg = root.path("error").asText();
                        LOG.error("Analyze readiness error: {}", errorMsg);
                        setStatusAndColor(MigrationStatus.FAILED, Color.RED);
                        Messages.showWarningDialog(project, "Error: " + errorMsg, "Analysis Failed");
                        setButtonsEnabled(true);
                        return;
                    }
                    
                    // Extract readiness data
                    int score = extractReadinessScore(root);
                    int totalDeps = extractTotalDependencies(root);
                    int affectedDeps = extractAffectedDependencies(root);
                    int blockers = extractBlockerCount(root);
                    
                    // Update UI
                    setReadinessScore(score);
                    setDependencySummary(totalDeps, affectedDeps, blockers);
                    setStatusAndColor(score >= 80 ? MigrationStatus.READY : 
                                     blockers > 0 ? MigrationStatus.HAS_BLOCKERS : 
                                     MigrationStatus.IN_PROGRESS, 
                                     getScoreColor(score));
                    
                    LOG.info("Readiness analysis complete: score={}, total={}, affected={}, blockers={}", 
                             score, totalDeps, affectedDeps, blockers);
                    
                } catch (Exception e) {
                    LOG.error("Failed to parse readiness response: {}", e.getMessage(), e);
                    setStatusAndColor(MigrationStatus.FAILED, Color.RED);
                    Messages.showWarningDialog(project, "Failed to parse response: " + e.getMessage(), "Parse Error");
                }
                setButtonsEnabled(true);
            }))
            .exceptionally(ex -> {
                LOG.error("Analyze readiness failed: {}", ex.getMessage(), ex);
                SwingUtilities.invokeLater(() -> {
                    setStatusAndColor(MigrationStatus.FAILED, Color.RED);
                    Messages.showWarningDialog(project, "Error: " + ex.getMessage(), "Connection Failed");
                    setButtonsEnabled(true);
                });
                return null;
            });
    }

    private void executeDetectBlockers(String projectPath) {
        mcpClient.detectBlockers(projectPath)
            .thenAccept(blockers -> SwingUtilities.invokeLater(() -> {
                try {
                    int count = blockers != null ? blockers.size() : 0;
                    LOG.info("Detected {} blockers", count);
                    
                    // Update blockers count
                    if (blockersValue != null) {
                        blockersValue.setText(String.valueOf(count));
                    }
                    
                    // Update status based on blockers
                    if (count > 0) {
                        setStatusAndColor(MigrationStatus.HAS_BLOCKERS, Color.RED);
                        Messages.showInfoMessage(project, 
                            "Found " + count + " migration blocker(s).\n\nRun 'Analyze Readiness' for details.", 
                            "Blockers Detected");
                    } else {
                        setStatusAndColor(MigrationStatus.READY, Color.GREEN.darker());
                        Messages.showInfoMessage(project, 
                            "No migration blockers detected.", 
                            "Blockers Check Complete");
                    }
                    
                } catch (Exception e) {
                    LOG.error("Failed to process blockers: {}", e.getMessage(), e);
                    setStatusAndColor(MigrationStatus.FAILED, Color.RED);
                }
                setButtonsEnabled(true);
            }))
            .exceptionally(ex -> {
                LOG.error("Detect blockers failed: {}", ex.getMessage(), ex);
                SwingUtilities.invokeLater(() -> {
                    setStatusAndColor(MigrationStatus.FAILED, Color.RED);
                    Messages.showWarningDialog(project, "Error: " + ex.getMessage(), "Tool Failed");
                    setButtonsEnabled(true);
                });
                return null;
            });
    }

    private void executeRecommendVersions(String projectPath) {
        mcpClient.recommendVersions(projectPath)
            .thenAccept(recommendations -> SwingUtilities.invokeLater(() -> {
                try {
                    int count = recommendations != null ? recommendations.size() : 0;
                    LOG.info("Found {} version recommendations", count);
                    
                    // Show recommendations in a dialog
                    if (count > 0) {
                        StringBuilder msg = new StringBuilder("Found " + count + " version recommendation(s):\n\n");
                        for (int i = 0; i < Math.min(5, recommendations.size()); i++) {
                            DependencyInfo dep = recommendations.get(i);
                            msg.append("• ").append(dep.getGroupId()).append(":").append(dep.getArtifactId())
                               .append(" → ").append(dep.getRecommendedVersion()).append("\n");
                        }
                        if (recommendations.size() > 5) {
                            msg.append("... and ").append(recommendations.size() - 5).append(" more");
                        }
                        Messages.showInfoMessage(project, msg.toString(), "Version Recommendations");
                    } else {
                        Messages.showInfoMessage(project, 
                            "No version recommendations needed.\nAll dependencies are already Jakarta-compatible.", 
                            "Version Recommendations");
                    }
                    
                } catch (Exception e) {
                    LOG.error("Failed to process recommendations: {}", e.getMessage(), e);
                    setStatusAndColor(MigrationStatus.FAILED, Color.RED);
                }
                setButtonsEnabled(true);
            }))
            .exceptionally(ex -> {
                LOG.error("Recommend versions failed: {}", ex.getMessage(), ex);
                SwingUtilities.invokeLater(() -> {
                    setStatusAndColor(MigrationStatus.FAILED, Color.RED);
                    Messages.showWarningDialog(project, "Error: " + ex.getMessage(), "Tool Failed");
                    setButtonsEnabled(true);
                });
                return null;
            });
    }

    private int extractReadinessScore(JsonNode root) {
        try {
            if (root.has("readinessScore")) {
                return root.get("readinessScore").asInt();
            }
            if (root.has("score")) {
                return root.get("score").asInt();
            }
            // Try to calculate from other fields
            int total = root.has("totalDependencies") ? root.get("totalDependencies").asInt() : 0;
            int affected = root.has("affectedDependencies") ? root.get("affectedDependencies").asInt() : 0;
            if (total > 0) {
                return Math.round(((float)(total - affected) / total) * 100);
            }
        } catch (Exception e) {
            LOG.debug("Could not extract readiness score: {}", e.getMessage());
        }
        return 0;
    }

    private int extractTotalDependencies(JsonNode root) {
        try {
            if (root.has("totalDependencies")) {
                return root.get("totalDependencies").asInt();
            }
            if (root.has("total")) {
                return root.get("total").asInt();
            }
        } catch (Exception e) {
            LOG.debug("Could not extract total dependencies: {}", e.getMessage());
        }
        return 0;
    }

    private int extractAffectedDependencies(JsonNode root) {
        try {
            if (root.has("affectedDependencies")) {
                return root.get("affectedDependencies").asInt();
            }
            if (root.has("affected")) {
                return root.get("affected").asInt();
            }
        } catch (Exception e) {
            LOG.debug("Could not extract affected dependencies: {}", e.getMessage());
        }
        return 0;
    }

    private int extractBlockerCount(JsonNode root) {
        try {
            if (root.has("blockerCount") || root.has("blockers")) {
                JsonNode blockers = root.has("blockerCount") ? root.get("blockerCount") : root.get("blockers");
                return blockers.isArray() ? blockers.size() : blockers.asInt();
            }
        } catch (Exception e) {
            LOG.debug("Could not extract blocker count: {}", e.getMessage());
        }
        return 0;
    }

    private Color getScoreColor(int score) {
        if (score >= 80) {
            return Color.GREEN.darker();
        } else if (score >= 50) {
            return Color.ORANGE;
        } else {
            return Color.RED;
        }
    }

    public JPanel getPanel() {
        return panel;
    }

    public void updateDashboard(MigrationDashboard dashboard) {
        this.dashboard = dashboard;
        SwingUtilities.invokeLater(() -> {
            if (dashboard != null) {
                // Update readiness score
                if (readinessScoreValue != null && dashboard.getReadinessScore() != null) {
                    readinessScoreValue.setText(dashboard.getReadinessScore() + "%");
                    updateReadinessColor(dashboard.getReadinessScore());
                }

                // Update status
                if (statusValue != null && dashboard.getStatus() != null) {
                    statusValue.setText(dashboard.getStatus().getValue());
                    updateStatusColor(dashboard.getStatus());
                }

                // Update dependency summary
                if (totalDepsValue != null && dashboard.getDependencySummary() != null) {
                    DependencySummary summary = dashboard.getDependencySummary();
                    totalDepsValue.setText(String.valueOf(summary.getTotalDependencies()));
                    if (affectedDepsValue != null) {
                        affectedDepsValue.setText(String.valueOf(summary.getAffectedDependencies()));
                    }
                    if (blockersValue != null) {
                        blockersValue.setText(String.valueOf(summary.getBlockerDependencies()));
                    }
                    if (migrableValue != null) {
                        migrableValue.setText(String.valueOf(summary.getMigrableDependencies()));
                    }
                }

                // Initialize additional metrics with "-" for now
                if (noJakartaSupportValue != null) noJakartaSupportValue.setText("-");
                if (xmlFilesValue != null) xmlFilesValue.setText("-");
                if (transitiveDepsValue != null) transitiveDepsValue.setText("-");

                // Update last analyzed
                if (lastAnalyzedValue != null) {
                    if (dashboard.getLastAnalyzed() != null) {
                        lastAnalyzedValue.setText(formatTimestamp(dashboard.getLastAnalyzed()));
                    } else {
                        lastAnalyzedValue.setText("Never");
                    }
                }
            }
        });
    }

    public void setReadinessScore(int score) {
        if (readinessScoreValue != null) {
            readinessScoreValue.setText(score + "%");
            updateReadinessColor(score);
        }
    }

    public void setStatus(MigrationStatus status) {
        if (statusValue != null) {
            statusValue.setText(status.getValue());
            updateStatusColor(status);
        }
    }

    public void setStatusAndColor(MigrationStatus status, Color color) {
        if (statusValue != null) {
            statusValue.setText(status.getValue());
            statusValue.setForeground(color);
        }
        if (statusIndicator != null) {
            statusIndicator.setForeground(color);
        }
    }

    public void setDependencySummary(int total, int affected, int blockers) {
        if (totalDepsValue != null) totalDepsValue.setText(String.valueOf(total));
        if (affectedDepsValue != null) affectedDepsValue.setText(String.valueOf(affected));
        if (blockersValue != null) blockersValue.setText(String.valueOf(blockers));
        if (migrableValue != null) migrableValue.setText(String.valueOf(Math.max(0, total - affected)));
    }

    public void setLastAnalyzed(Instant timestamp) {
        if (lastAnalyzedValue != null) {
            lastAnalyzedValue.setText(timestamp != null ? formatTimestamp(timestamp) : "Never");
        }
    }

    private void updateReadinessColor(int score) {
        if (readinessScoreValue != null) {
            readinessScoreValue.setForeground(getScoreColor(score));
        }
    }

    private void updateStatusColor(MigrationStatus status) {
        if (statusValue != null) {
            Color color = switch (status) {
                case READY -> Color.GREEN.darker();
                case HAS_BLOCKERS -> Color.RED;
                case IN_PROGRESS -> Color.BLUE;
                case COMPLETED -> Color.GREEN.darker();
                case FAILED -> Color.RED;
                default -> Color.GRAY;
            };
            statusValue.setForeground(color);
            if (statusIndicator != null) {
                statusIndicator.setForeground(color);
            }
        }
    }

    private String formatTimestamp(Instant timestamp) {
        return timestamp.toString().replace("T", " ").substring(0, 19);
    }

    private void handleAnalyze(ActionEvent e) {
        if (onAnalyze != null) {
            onAnalyze.accept(e);
        }
    }

    private void handleRefresh(ActionEvent e) {
        if (onAnalyze != null) {
            onAnalyze.accept(e);
        }
    }
}
