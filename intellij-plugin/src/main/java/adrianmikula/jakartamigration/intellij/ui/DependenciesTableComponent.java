package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus;
import adrianmikula.jakartamigration.intellij.model.RiskLevel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Dependencies table component from TypeSpec: plugin-components.tsp
 */
public class DependenciesTableComponent {
    private final JPanel panel;
    private final Project project;
    private final JBTable table;
    private final DefaultTableModel tableModel;
    private final JTextField searchField;
    private final JComboBox<String> statusFilter;
    private final JCheckBox blockersOnly;
    private List<DependencyInfo> allDependencies;

    public DependenciesTableComponent(Project project) {
        this.project = project;
        this.allDependencies = new ArrayList<>();
        this.panel = new JBPanel<>(new BorderLayout());

        // Table columns from TypeSpec: DependencyTableColumn enum
        String[] columns = {
            "Group ID", "Artifact ID", "Current Version", "Recommended Version",
            "Migration Status", "Is Blocker", "Risk Level", "Migration Impact"
        };

        this.tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        this.table = new JBTable(tableModel);
        this.searchField = new JTextField(20);
        this.statusFilter = new JComboBox<>(new String[]{
            "All", "Compatible", "Needs Upgrade", "No Jakarta Version", "Requires Manual Migration", "Migrated"
        });
        this.blockersOnly = new JCheckBox("Show blockers only");

        initializeComponent();
    }

    private void initializeComponent() {
        // Header with filters
        JPanel headerPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT));
        headerPanel.add(new JLabel("Dependencies Analysis"));

        searchField.setToolTipText("Search dependencies...");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterDependencies();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterDependencies();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterDependencies();
            }
        });
        headerPanel.add(searchField);

        statusFilter.addActionListener(e -> filterDependencies());
        headerPanel.add(statusFilter);

        blockersOnly.addActionListener(e -> filterDependencies());
        headerPanel.add(blockersOnly);

        // Table
        JBScrollPane scrollPane = new JBScrollPane(table);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        // Add mouse listener for double-click navigation
        table.addMouseListener(new MouseInputAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    handleDoubleClick();
                }
            }
        });

        // Actions panel
        JPanel actionsPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT));
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(this::handleRefresh);
        JButton updateButton = new JButton("Update Selected");
        updateButton.addActionListener(this::handleUpdate);
        JButton viewDetailsButton = new JButton("View Details");
        viewDetailsButton.addActionListener(this::handleViewDetails);

        actionsPanel.add(refreshButton);
        actionsPanel.add(updateButton);
        actionsPanel.add(viewDetailsButton);

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(actionsPanel, BorderLayout.SOUTH);
    }

    public JPanel getPanel() {
        return panel;
    }

    public void setDependencies(List<DependencyInfo> dependencies) {
        this.allDependencies = dependencies != null ? dependencies : new ArrayList<>();
        filterDependencies();
    }

    public List<DependencyInfo> getSelectedDependencies() {
        int[] selectedRows = table.getSelectedRows();
        List<DependencyInfo> selected = new ArrayList<>();
        for (int row : selectedRows) {
            int modelRow = table.convertRowIndexToModel(row);
            if (modelRow < allDependencies.size()) {
                selected.add(allDependencies.get(modelRow));
            }
        }
        return selected;
    }

    private void filterDependencies() {
        // Clear and rebuild table with filtered data
        tableModel.setRowCount(0);

        String searchText = searchField.getText().toLowerCase();
        String selectedStatus = (String) statusFilter.getSelectedItem();
        boolean showBlockersOnly = blockersOnly.isSelected();

        for (DependencyInfo dep : allDependencies) {
            // Search filter
            boolean matchesSearch = searchText.isEmpty() ||
                dep.getGroupId().toLowerCase().contains(searchText) ||
                dep.getArtifactId().toLowerCase().contains(searchText) ||
                dep.getCurrentVersion().toLowerCase().contains(searchText);

            // Status filter
            boolean matchesStatus = "All".equals(selectedStatus) ||
                (selectedStatus != null && dep.getMigrationStatus() != null &&
                    dep.getMigrationStatus().getValue().equals(mapStatusToValue(selectedStatus)));

            // Blocker filter
            boolean matchesBlocker = !showBlockersOnly || dep.isBlocker();

            if (matchesSearch && matchesStatus && matchesBlocker) {
                addDependencyRow(dep);
            }
        }
    }

    private String mapStatusToValue(String status) {
        switch (status) {
            case "Compatible": return "COMPATIBLE";
            case "Needs Upgrade": return "NEEDS_UPGRADE";
            case "No Jakarta Version": return "NO_JAKARTA_VERSION";
            case "Requires Manual Migration": return "REQUIRES_MANUAL_MIGRATION";
            case "Migrated": return "MIGRATED";
            default: return null;
        }
    }

    private void addDependencyRow(DependencyInfo dep) {
        tableModel.addRow(new Object[]{
            dep.getGroupId(),
            dep.getArtifactId(),
            dep.getCurrentVersion(),
            dep.getRecommendedVersion() != null ? dep.getRecommendedVersion() : "-",
            dep.getMigrationStatus() != null ? dep.getMigrationStatus().getValue() : "UNKNOWN",
            dep.isBlocker() ? "Yes" : "No",
            dep.getRiskLevel() != null ? dep.getRiskLevel().getValue() : "MEDIUM",
            dep.getMigrationImpact() != null ? dep.getMigrationImpact() : "-"
        });
    }

    private void handleRefresh(ActionEvent e) {
        // Trigger refresh - UI components can listen for this event
        SwingUtilities.invokeLater(() -> {
            Messages.showInfoMessage(project, "Refreshing dependency analysis...", "Refresh");
        });
    }

    private void handleUpdate(ActionEvent e) {
        List<DependencyInfo> selected = getSelectedDependencies();
        if (selected.isEmpty()) {
            Messages.showWarningDialog(project, "Please select dependencies to update.", "No Selection");
            return;
        }

        StringBuilder message = new StringBuilder("Update selected dependencies:\n\n");
        for (DependencyInfo dep : selected) {
            message.append("- ").append(dep.getDisplayName())
                  .append(" -> ").append(dep.getRecommendedVersion() != null ? dep.getRecommendedVersion() : "No Jakarta version")
                   .append("\n");
        }

        int result = Messages.showYesNoDialog(project, message.toString(), "Confirm Updates",
                Messages.getQuestionIcon());

        if (result == Messages.YES) {
            Messages.showInfoMessage(project, "Updates would be applied here.", "Update");
        }
    }

    /**
     * Handle view details action (from button click).
     */
    private void handleViewDetails(ActionEvent e) {
        List<DependencyInfo> selected = getSelectedDependencies();
        if (selected.isEmpty()) {
            Messages.showWarningDialog(project, "Please select a dependency to view details.", "No Selection");
            return;
        }
        showDependencyDetails(selected.get(0));
    }

    /**
     * Show details for a specific dependency.
     */
    public void showDependencyDetails(DependencyInfo dep) {
        String details = String.format("""
            Dependency Details
            ==================

            Group ID: %s
            Artifact ID: %s
            Current Version: %s
            Recommended Version: %s

            Migration Status: %s
            Is Blocker: %s
            Risk Level: %s

            Migration Impact:
            %s
            """,
            dep.getGroupId(),
            dep.getArtifactId(),
            dep.getCurrentVersion(),
            dep.getRecommendedVersion() != null ? dep.getRecommendedVersion() : "N/A",
            dep.getMigrationStatus() != null ? dep.getMigrationStatus().getValue() : "UNKNOWN",
            dep.isBlocker() ? "Yes" : "No",
            dep.getRiskLevel() != null ? dep.getRiskLevel().getValue() : "MEDIUM",
            dep.getMigrationImpact() != null ? dep.getMigrationImpact() : "No impact information available"
        );

        Messages.showInfoMessage(project, details, "Dependency Details - " + dep.getDisplayName());
    }

    /**
     * Handle double-click on table row to show details.
     */
    private void handleDoubleClick() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow >= 0) {
            int modelRow = table.convertRowIndexToModel(selectedRow);
            if (modelRow < allDependencies.size()) {
                DependencyInfo dep = allDependencies.get(modelRow);
                showDependencyDetails(dep);
            }
        }
    }

    /**
     * @deprecated Use setDependencies with DependencyInfo objects instead
     */
    @Deprecated
    public void addDependency(String groupId, String artifactId, String currentVersion,
                             String recommendedVersion, String status, boolean isBlocker,
                             String riskLevel, String impact) {
        // Convert legacy string parameters to DependencyInfo
        DependencyInfo dep = new DependencyInfo();
        dep.setGroupId(groupId);
        dep.setArtifactId(artifactId);
        dep.setCurrentVersion(currentVersion);
        dep.setRecommendedVersion(recommendedVersion);
        dep.setMigrationStatus(mapStringToStatus(status));
        dep.setBlocker(isBlocker);
        dep.setRiskLevel(mapStringToRiskLevel(riskLevel));
        dep.setMigrationImpact(impact);

        allDependencies.add(dep);
        addDependencyRow(dep);
    }

    private DependencyMigrationStatus mapStringToStatus(String status) {
        if (status == null) return null;
        switch (status.toLowerCase()) {
            case "compatible": return DependencyMigrationStatus.COMPATIBLE;
            case "needs upgrade": return DependencyMigrationStatus.NEEDS_UPGRADE;
            case "no jakarta version": return DependencyMigrationStatus.NO_JAKARTA_VERSION;
            case "requires manual migration": return DependencyMigrationStatus.REQUIRES_MANUAL_MIGRATION;
            case "migrated": return DependencyMigrationStatus.MIGRATED;
            default: return DependencyMigrationStatus.COMPATIBLE;
        }
    }

    private RiskLevel mapStringToRiskLevel(String riskLevel) {
        if (riskLevel == null) return RiskLevel.MEDIUM;
        switch (riskLevel.toLowerCase()) {
            case "low": return RiskLevel.LOW;
            case "high": return RiskLevel.HIGH;
            case "critical": return RiskLevel.CRITICAL;
            default: return RiskLevel.MEDIUM;
        }
    }
}
