package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.dependencyanalysis.domain.*;
import adrianmikula.jakartamigration.intellij.model.DependencySummary;
import adrianmikula.jakartamigration.intellij.model.MigrationDashboard;
import adrianmikula.jakartamigration.intellij.model.MigrationStatus;
import adrianmikula.jakartamigration.intellij.service.MigrationAnalysisService;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

/**
 * Dashboard component with text-based key:value table and migration analysis buttons.
 * Connects directly to the migration-core library for Jakarta migration analysis.
 */
public class DashboardComponent {
    private static final Logger LOG = Logger.getInstance(DashboardComponent.class);
    
    private final JPanel panel;
    private final Project project;
    private MigrationDashboard dashboard;
    private final Consumer<ActionEvent> onAnalyze;
    private final MigrationAnalysisService analysisService;

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

    // Migration Analysis buttons
    private JButton analyzeReadinessBtn;
    private JButton detectBlockersBtn;
    private JButton recommendVersionsBtn;
    private JButton analyzeImpactBtn;

    // Status indicator panel
    private JPanel statusPanel;

    public DashboardComponent(@NotNull Project project, Consumer<ActionEvent> onAnalyze) {
        this.project = project;
        this.onAnalyze = onAnalyze;
        this.analysisService = new MigrationAnalysisService();
        this.panel = new JBPanel<>(new BorderLayout());
        initializeComponent();
    }

    private void initializeComponent() {
        // Migration Analysis Button Bar (along the top)
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

        JLabel toolsLabel = new JLabel("Migration Analysis:");
        toolsLabel.setFont(toolsLabel.getFont().deriveFont(Font.BOLD, 11f));
        buttonBar.add(toolsLabel);

        // Free Analysis Tools
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

        // Full Analysis (Main analysis)
        analyzeImpactBtn = createToolButton("Analyze Impact", () -> handleToolAction("analyzeImpact"));
        buttonBar.add(analyzeImpactBtn);

        return buttonBar;
    }

    private JButton createToolButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.setToolTipText("Run " + text + " analysis");
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

        JButton refreshButton = new JButton("Refresh");
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

        LOG.info("Executing analysis: " + toolName + " for project: " + projectPath);
        setStatusAndColor(MigrationStatus.IN_PROGRESS, Color.BLUE);

        // Make effectively final for use in SwingWorker
        final String effectiveProjectPath = projectPath;
        final Path path = Paths.get(effectiveProjectPath);

        // Run analysis in background thread to avoid blocking UI
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    switch (toolName) {
                        case "analyzeReadiness":
                            executeAnalyzeReadiness(path);
                            break;
                        case "detectBlockers":
                            executeDetectBlockers(path);
                            break;
                        case "recommendVersions":
                            executeRecommendVersions(path);
                            break;
                        case "analyzeImpact":
                            if (onAnalyze != null) {
                                onAnalyze.accept(null);
                            }
                            break;
                        default:
                            SwingUtilities.invokeLater(() -> {
                                setStatusAndColor(MigrationStatus.NOT_ANALYZED, Color.GRAY);
                                Messages.showInfoMessage(project,
                                    "Analysis: " + toolName + "\n\nThis feature would run the " + toolName + " analysis.",
                                    "Analysis: " + toolName);
                            });
                    }
                } catch (Exception e) {
                    LOG.error("Analysis failed: " + e.getMessage(), e);
                    SwingUtilities.invokeLater(() -> {
                        setStatusAndColor(MigrationStatus.FAILED, Color.RED);
                        Messages.showWarningDialog(project, "Error: " + e.getMessage(), "Analysis Failed");
                    });
                }
                return null;
            }
            
            @Override
            protected void done() {
                setButtonsEnabled(true);
            }
        }.execute();
    }

    private void executeAnalyzeReadiness(Path projectPath) {
        LOG.info("Running readiness analysis on: " + projectPath);
        
        DependencyAnalysisReport report = analysisService.analyzeProject(projectPath);
        DependencyGraph graph = report.dependencyGraph();
        int totalDeps = graph.getNodes().size();
        int affectedDeps = countAffectedDependencies(report);
        int blockers = report.blockers().size();
        int score = calculateReadinessScore(report);
        
        SwingUtilities.invokeLater(() -> {
            setReadinessScore(score);
            setDependencySummary(totalDeps, affectedDeps, blockers);
            
            MigrationStatus status;
            if (blockers > 0) {
                status = MigrationStatus.HAS_BLOCKERS;
            } else if (score >= 80) {
                status = MigrationStatus.READY;
            } else {
                status = MigrationStatus.IN_PROGRESS;
            }
            setStatusAndColor(status, getScoreColor(score));
        });
        
        LOG.info("Readiness analysis complete: score=" + score + ", total=" + totalDeps + ", affected=" + affectedDeps + ", blockers=" + blockers);
    }
    
    private int countAffectedDependencies(DependencyAnalysisReport report) {
        int count = 0;
        NamespaceCompatibilityMap namespaceMap = report.namespaceMap();
        for (Artifact artifact : report.dependencyGraph().getNodes()) {
            Namespace namespace = namespaceMap.get(artifact);
            if (namespace != null && namespace != Namespace.JAKARTA) {
                count++;
            }
        }
        return count;
    }
    
    private int calculateReadinessScore(DependencyAnalysisReport report) {
        int total = report.dependencyGraph().getNodes().size();
        if (total == 0) {
            return 100;
        }
        
        int jakartaCount = 0;
        NamespaceCompatibilityMap namespaceMap = report.namespaceMap();
        for (Artifact artifact : report.dependencyGraph().getNodes()) {
            Namespace namespace = namespaceMap.get(artifact);
            if (namespace == Namespace.JAKARTA) {
                jakartaCount++;
            }
        }
        
        return (int) ((double) jakartaCount / total * 100);
    }

    private void executeDetectBlockers(Path projectPath) {
        LOG.info("Detecting blockers in: " + projectPath);
        
        List<Blocker> blockers = analysisService.detectBlockers(projectPath);
        int count = blockers.size();
        
        LOG.info("Detected " + count + " blockers");
        
        SwingUtilities.invokeLater(() -> {
            if (blockersValue != null) {
                blockersValue.setText(String.valueOf(count));
            }
            
            if (count > 0) {
                setStatusAndColor(MigrationStatus.HAS_BLOCKERS, Color.RED);
                
                StringBuilder msg = new StringBuilder("Found " + count + " migration blocker(s):\n\n");
                for (int i = 0; i < Math.min(5, blockers.size()); i++) {
                    Blocker blocker = blockers.get(i);
                    Artifact artifact = blocker.artifact();
                    msg.append("- ").append(artifact.groupId()).append(":").append(artifact.artifactId()).append("\n");
                    msg.append("  Reason: ").append(blocker.reason()).append("\n\n");
                }
                if (blockers.size() > 5) {
                    msg.append("... and ").append(blockers.size() - 5).append(" more");
                }
                Messages.showInfoMessage(project, msg.toString(), "Blockers Detected");
            } else {
                setStatusAndColor(MigrationStatus.READY, Color.GREEN.darker());
                Messages.showInfoMessage(project, 
                    "No migration blockers detected.", 
                    "Blockers Check Complete");
            }
        });
    }

    private void executeRecommendVersions(Path projectPath) {
        LOG.info("Recommending versions for: " + projectPath);
        
        List<VersionRecommendation> recommendations = analysisService.recommendVersions(projectPath);
        int count = recommendations.size();
        
        LOG.info("Found " + count + " version recommendations");
        
        SwingUtilities.invokeLater(() -> {
            if (count > 0) {
                StringBuilder msg = new StringBuilder("Found " + count + " version recommendation(s):\n\n");
                for (int i = 0; i < Math.min(5, recommendations.size()); i++) {
                    VersionRecommendation rec = recommendations.get(i);
                    Artifact current = rec.currentArtifact();
                    Artifact recommended = rec.recommendedArtifact();
                    msg.append("- ").append(current.groupId()).append(":").append(current.artifactId())
                       .append(" -> ").append(recommended.version()).append("\n");
                    String path = rec.migrationPath();
                    if (path != null && !path.isEmpty()) {
                        msg.append("  Path: ").append(path).append("\n");
                    }
                    msg.append("\n");
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
        });
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
                if (readinessScoreValue != null && dashboard.getReadinessScore() != null) {
                    readinessScoreValue.setText(dashboard.getReadinessScore() + "%");
                    updateReadinessColor(dashboard.getReadinessScore());
                }

                if (statusValue != null && dashboard.getStatus() != null) {
                    statusValue.setText(dashboard.getStatus().getValue());
                    updateStatusColor(dashboard.getStatus());
                }

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

                if (noJakartaSupportValue != null) noJakartaSupportValue.setText("-");
                if (xmlFilesValue != null) xmlFilesValue.setText("-");
                if (transitiveDepsValue != null) transitiveDepsValue.setText("-");

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
