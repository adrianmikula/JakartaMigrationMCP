package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.model.MigrationDashboard;
import adrianmikula.jakartamigration.intellij.model.MigrationStatus;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;

import javax.swing.*;
import java.awt.*;

/**
 * Dashboard component from TypeSpec: plugin-components.tsp
 */
public class DashboardComponent {
    private final JPanel panel;
    private final Project project;
    private MigrationDashboard dashboard;

    public DashboardComponent(Project project) {
        this.project = project;
        this.panel = new JBPanel<>(new BorderLayout());
        initializeComponent();
    }

    private void initializeComponent() {
        // Header
        JPanel headerPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT));
        headerPanel.add(new JBLabel("Migration Dashboard", SwingConstants.LEFT));
        
        // Content
        JPanel contentPanel = new JBPanel<>(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Readiness Score
        gbc.gridx = 0; gbc.gridy = 0;
        contentPanel.add(new JBLabel("Readiness Score:"), gbc);
        gbc.gridx = 1;
        JBLabel readinessLabel = new JBLabel("Not analyzed");
        contentPanel.add(readinessLabel, gbc);

        // Status
        gbc.gridx = 0; gbc.gridy = 1;
        contentPanel.add(new JBLabel("Status:"), gbc);
        gbc.gridx = 1;
        JBLabel statusLabel = new JBLabel(MigrationStatus.NOT_ANALYZED.getValue());
        contentPanel.add(statusLabel, gbc);

        // Dependencies Summary
        gbc.gridx = 0; gbc.gridy = 2;
        contentPanel.add(new JBLabel("Total Dependencies:"), gbc);
        gbc.gridx = 1;
        JBLabel totalDepsLabel = new JBLabel("0");
        contentPanel.add(totalDepsLabel, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        contentPanel.add(new JBLabel("Affected Dependencies:"), gbc);
        gbc.gridx = 1;
        JBLabel affectedDepsLabel = new JBLabel("0");
        contentPanel.add(affectedDepsLabel, gbc);

        // Actions
        JPanel actionsPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT));
        JButton refreshButton = new JButton("Refresh Analysis");
        JButton startMigrationButton = new JButton("Start Migration");
        actionsPanel.add(refreshButton);
        actionsPanel.add(startMigrationButton);

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(contentPanel, BorderLayout.CENTER);
        panel.add(actionsPanel, BorderLayout.SOUTH);
    }

    public JPanel getPanel() {
        return panel;
    }

    public void updateDashboard(MigrationDashboard dashboard) {
        this.dashboard = dashboard;
        // Update UI components with dashboard data
        SwingUtilities.invokeLater(() -> {
            // Update labels with dashboard data
        });
    }
}