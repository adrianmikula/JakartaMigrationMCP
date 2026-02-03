package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.model.DependencySummary;
import adrianmikula.jakartamigration.intellij.model.MigrationDashboard;
import adrianmikula.jakartamigration.intellij.model.MigrationStatus;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.Instant;

/**
 * Dashboard component from TypeSpec: plugin-components.tsp
 */
public class DashboardComponent {
    private final JPanel panel;
    private final Project project;
    private MigrationDashboard dashboard;

    // UI Labels
    private JBLabel readinessLabel;
    private JBLabel statusLabel;
    private JBLabel totalDepsLabel;
    private JBLabel affectedDepsLabel;
    private JBLabel blockerDepsLabel;
    private JBLabel lastAnalyzedLabel;

    public DashboardComponent(Project project) {
        this.project = project;
        this.panel = new JBPanel<>(new BorderLayout());
        initializeComponent();
    }

    private void initializeComponent() {
        // Header
        JPanel headerPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT));
        headerPanel.add(new JBLabel("Migration Dashboard", SwingConstants.LEFT));

        // Content - Summary Cards
        JPanel contentPanel = new JBPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        // Readiness Score Card
        JPanel readinessCard = createCard("Readiness Score", "Not analyzed", Color.BLUE);
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 0.25;
        contentPanel.add(readinessCard, gbc);

        // Status Card
        JPanel statusCard = createCard("Status", MigrationStatus.NOT_ANALYZED.getValue(), Color.GRAY);
        gbc.gridx = 1; gbc.gridy = 0;
        contentPanel.add(statusCard, gbc);

        // Dependencies Summary
        JPanel depsCard = createCard("Total Dependencies", "0", Color.DARK_GRAY);
        gbc.gridx = 2; gbc.gridy = 0;
        contentPanel.add(depsCard, gbc);

        // Affected Dependencies Card
        JPanel affectedCard = createCard("Affected", "0", Color.ORANGE);
        gbc.gridx = 0; gbc.gridy = 1;
        contentPanel.add(affectedCard, gbc);

        // Blocker Dependencies Card
        JPanel blockerCard = createCard("Blockers", "0", Color.RED);
        gbc.gridx = 1; gbc.gridy = 1;
        contentPanel.add(blockerCard, gbc);

        // Last Analyzed Card
        JPanel lastAnalyzedCard = createCard("Last Analyzed", "Never", Color.DARK_GRAY);
        gbc.gridx = 2; gbc.gridy = 1;
        contentPanel.add(lastAnalyzedCard, gbc);

        // Actions
        JPanel actionsPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT));
        JButton refreshButton = new JButton("Refresh Analysis");
        refreshButton.addActionListener(this::handleRefresh);
        JButton startMigrationButton = new JButton("Start Migration");
        startMigrationButton.addActionListener(this::handleStartMigration);

        actionsPanel.add(refreshButton);
        actionsPanel.add(startMigrationButton);

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(contentPanel, BorderLayout.CENTER);
        panel.add(actionsPanel, BorderLayout.SOUTH);
    }

    private JPanel createCard(String title, String value, Color accentColor) {
        JPanel card = new JBPanel<>(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accentColor, 2),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JBLabel titleLabel = new JBLabel(title, SwingConstants.CENTER);
        titleLabel.setForeground(accentColor);

        JBLabel valueLabel = new JBLabel(value, SwingConstants.CENTER);
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 24f));

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        // Store reference for updates
        if ("Readiness Score".equals(title)) {
            readinessLabel = valueLabel;
        } else if ("Status".equals(title)) {
            statusLabel = valueLabel;
        } else if ("Total Dependencies".equals(title)) {
            totalDepsLabel = valueLabel;
        } else if ("Affected".equals(title)) {
            affectedDepsLabel = valueLabel;
        } else if ("Blockers".equals(title)) {
            blockerDepsLabel = valueLabel;
        } else if ("Last Analyzed".equals(title)) {
            lastAnalyzedLabel = valueLabel;
        }

        return card;
    }

    public JPanel getPanel() {
        return panel;
    }

    public void updateDashboard(MigrationDashboard dashboard) {
        this.dashboard = dashboard;
        SwingUtilities.invokeLater(() -> {
            if (dashboard != null) {
                // Update readiness score
                if (readinessLabel != null && dashboard.getReadinessScore() != null) {
                    readinessLabel.setText(dashboard.getReadinessScore() + "%");
                    updateReadinessColor(dashboard.getReadinessScore());
                }

                // Update status
                if (statusLabel != null && dashboard.getStatus() != null) {
                    statusLabel.setText(dashboard.getStatus().getValue());
                    updateStatusColor(dashboard.getStatus());
                }

                // Update dependency summary
                if (totalDepsLabel != null && dashboard.getDependencySummary() != null) {
                    DependencySummary summary = dashboard.getDependencySummary();
                    totalDepsLabel.setText(String.valueOf(summary.getTotalDependencies()));
                    if (affectedDepsLabel != null) {
                        affectedDepsLabel.setText(String.valueOf(summary.getAffectedDependencies()));
                    }
                    if (blockerDepsLabel != null) {
                        blockerDepsLabel.setText(String.valueOf(summary.getBlockerDependencies()));
                    }
                }

                // Update last analyzed
                if (lastAnalyzedLabel != null) {
                    if (dashboard.getLastAnalyzed() != null) {
                        lastAnalyzedLabel.setText(formatTimestamp(dashboard.getLastAnalyzed()));
                    } else {
                        lastAnalyzedLabel.setText("Never");
                    }
                }
            }
        });
    }

    public void setReadinessScore(int score) {
        if (readinessLabel != null) {
            readinessLabel.setText(score + "%");
            updateReadinessColor(score);
        }
    }

    public void setStatus(MigrationStatus status) {
        if (statusLabel != null) {
            statusLabel.setText(status.getValue());
            updateStatusColor(status);
        }
    }

    public void setDependencySummary(int total, int affected, int blockers) {
        if (totalDepsLabel != null) totalDepsLabel.setText(String.valueOf(total));
        if (affectedDepsLabel != null) affectedDepsLabel.setText(String.valueOf(affected));
        if (blockerDepsLabel != null) blockerDepsLabel.setText(String.valueOf(blockers));
    }

    public void setLastAnalyzed(Instant timestamp) {
        if (lastAnalyzedLabel != null) {
            lastAnalyzedLabel.setText(timestamp != null ? formatTimestamp(timestamp) : "Never");
        }
    }

    private void updateReadinessColor(int score) {
        if (readinessLabel != null) {
            if (score >= 80) {
                readinessLabel.setForeground(Color.GREEN.darker());
            } else if (score >= 50) {
                readinessLabel.setForeground(Color.ORANGE);
            } else {
                readinessLabel.setForeground(Color.RED);
            }
        }
    }

    private void updateStatusColor(MigrationStatus status) {
        if (statusLabel != null) {
            switch (status) {
                case READY -> statusLabel.setForeground(Color.GREEN.darker());
                case HAS_BLOCKERS -> statusLabel.setForeground(Color.RED);
                case IN_PROGRESS -> statusLabel.setForeground(Color.BLUE);
                case COMPLETED -> statusLabel.setForeground(Color.GREEN.darker());
                case FAILED -> statusLabel.setForeground(Color.RED);
                default -> statusLabel.setForeground(Color.GRAY);
            }
        }
    }

    private String formatTimestamp(Instant timestamp) {
        return timestamp.toString().replace("T", " ").substring(0, 19);
    }

    private void handleRefresh(ActionEvent e) {
        Messages.showInfoMessage(project, "Refreshing migration analysis...", "Refresh Analysis");
    }

    private void handleStartMigration(ActionEvent e) {
        Messages.showInfoMessage(project, "Migration wizard would start here.", "Start Migration");
    }
}
