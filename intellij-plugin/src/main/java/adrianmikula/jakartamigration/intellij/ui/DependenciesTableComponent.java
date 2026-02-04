package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Dependencies table component with colored status indicators and dependency type column.
 * Updated to show:
 * - Colored status dot (green/yellow/red)
 * - Dependency type (Direct/Transitive)
 * - Simplified columns (Group ID, Artifact ID, Version, Recommended Version, Status, Type)
 */
public class DependenciesTableComponent {
    private final JPanel panel;
    private final Project project;
    private final JBTable table;
    private final DefaultTableModel tableModel;
    private final JTextField searchField;
    private final JComboBox<String> statusFilter;
    private final JCheckBox transitiveFilter;
    private List<DependencyInfo> allDependencies;

    // Status colors
    private static final Color STATUS_COMPATIBLE = new Color(40, 167, 69);    // Green
    private static final Color STATUS_NEEDS_UPGRADE = new Color(255, 193, 7); // Yellow
    private static final Color STATUS_NO_JAKARTA = new Color(220, 53, 69);   // Red
    private static final Color STATUS_UNKNOWN = new Color(108, 117, 125);     // Gray

    public DependenciesTableComponent(Project project) {
        this.project = project;
        this.allDependencies = new ArrayList<>();
        this.panel = new JBPanel<>(new BorderLayout());

        // Simplified columns - removed Risk Level, Migration Impact, Is Blocker
        // Status column moved to the right
        String[] columns = {
            "Group ID",
            "Artifact ID",
            "Current Version",
            "Recommended Version",
            "Dependency Type",  // Direct or Transitive
            "Status"            // Colored dot indicator (moved to right)
        };

        this.tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        this.table = new JBTable(tableModel) {
            @Override
            public TableCellRenderer getCellRenderer(int row, int column) {
                return new StatusCellRenderer();
            }
        };

        this.searchField = new JTextField(20);
        this.statusFilter = new JComboBox<>(new String[]{
            "All", "Compatible", "Needs Upgrade", "No Jakarta Version"
        });
        this.transitiveFilter = new JCheckBox("Show Transitive Only", false);

        initializeComponent();
    }

    /**
     * Custom cell renderer for status column with colored dot indicator.
     */
    private static class StatusCellRenderer implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            if (column == 5 && value instanceof DependencyInfo) {
                DependencyInfo dep = (DependencyInfo) value;
                JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
                panel.setOpaque(true);

                // Set background for selection
                if (isSelected) {
                    panel.setBackground(table.getSelectionBackground());
                } else {
                    panel.setBackground(table.getBackground());
                }

                // Add dotted border for transitive dependencies
                if (dep.isTransitive()) {
                    panel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createDashedBorder(Color.GRAY),
                        new EmptyBorder(5, 2, 5, 2)));
                } else {
                    panel.setBorder(new EmptyBorder(5, 0, 5, 0));
                }

                // Colored status dot
                JPanel dot = new JPanel();
                dot.setPreferredSize(new Dimension(12, 12));
                dot.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));

                Color statusColor = getStatusColor(dep.getMigrationStatus());
                dot.setBackground(statusColor);

                JLabel label = new JLabel(dep.getMigrationStatus() != null
                    ? dep.getMigrationStatus().getValue() : "UNKNOWN");
                label.setFont(table.getFont());

                // Add transitive indicator text
                if (dep.isTransitive()) {
                    label.setText(label.getText() + " (Transitive)");
                    label.setFont(label.getFont().deriveFont(Font.ITALIC));
                }

                panel.add(dot);
                panel.add(label);
                return panel;
            }

            // Default rendering for other columns - truncate text
            JLabel label = new JLabel(value != null ? value.toString() : "");
            label.setOpaque(true);
            if (isSelected) {
                label.setBackground(table.getSelectionBackground());
            } else {
                label.setBackground(table.getBackground());
            }
            label.setHorizontalAlignment(SwingConstants.LEFT);

            return label;
        }

        private static Color getStatusColor(DependencyMigrationStatus status) {
            if (status == null) return STATUS_UNKNOWN;
            switch (status) {
                case COMPATIBLE:
                    return STATUS_COMPATIBLE;
                case NEEDS_UPGRADE:
                case REQUIRES_MANUAL_MIGRATION:
                    return STATUS_NEEDS_UPGRADE;
                case NO_JAKARTA_VERSION:
                    return STATUS_NO_JAKARTA;
                default:
                    return STATUS_UNKNOWN;
            }
        }
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

        transitiveFilter.addActionListener(e -> filterDependencies());
        headerPanel.add(transitiveFilter);

        // Table
        JBScrollPane scrollPane = new JBScrollPane(table);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // Set column widths (status is now at column 5)
        table.getColumnModel().getColumn(0).setPreferredWidth(180); // Group ID
        table.getColumnModel().getColumn(1).setPreferredWidth(180); // Artifact ID
        table.getColumnModel().getColumn(2).setPreferredWidth(100); // Current Version
        table.getColumnModel().getColumn(3).setPreferredWidth(100); // Recommended
        table.getColumnModel().getColumn(4).setPreferredWidth(80);  // Dependency Type
        table.getColumnModel().getColumn(5).setPreferredWidth(120); // Status

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
        boolean showTransitiveOnly = transitiveFilter.isSelected();

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

            // Transitive filter
            boolean matchesTransitive = !showTransitiveOnly || dep.isTransitive();

            if (matchesSearch && matchesStatus && matchesTransitive) {
                addDependencyRow(dep);
            }
        }
    }

    private String mapStatusToValue(String status) {
        if (status == null) return null;
        switch (status) {
            case "Compatible": return "COMPATIBLE";
            case "Needs Upgrade": return "NEEDS_UPGRADE";
            case "No Jakarta Version": return "NO_JAKARTA_VERSION";
            default: return null;
        }
    }

    private void addDependencyRow(DependencyInfo dep) {
        // Determine dependency type
        String dependencyType = dep.isTransitive() ? "Transitive" : "Direct";

        // Pass the full DependencyInfo object to the status column (column 5)
        tableModel.addRow(new Object[]{
            dep.getGroupId(),
            dep.getArtifactId(),
            dep.getCurrentVersion(),
            dep.getRecommendedVersion() != null ? dep.getRecommendedVersion() : "-",
            dependencyType,
            dep  // Full object for status column (last column)
        });
    }

    private void handleRefresh(ActionEvent e) {
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

    private void handleViewDetails(ActionEvent e) {
        List<DependencyInfo> selected = getSelectedDependencies();
        if (selected.isEmpty()) {
            Messages.showWarningDialog(project, "Please select a dependency to view details.", "No Selection");
            return;
        }
        showDependencyDetails(selected.get(0));
    }

    public void showDependencyDetails(DependencyInfo dep) {
        String details = String.format("""
            Dependency Details
            ==================

            Group ID: %s
            Artifact ID: %s
            Current Version: %s
            Recommended Version: %s

            Migration Status: %s
            Dependency Type: %s

            Actions:
            • Update to recommended version
            • View source code
            • Exclude from analysis
            """,
            dep.getGroupId(),
            dep.getArtifactId(),
            dep.getCurrentVersion(),
            dep.getRecommendedVersion() != null ? dep.getRecommendedVersion() : "N/A",
            dep.getMigrationStatus() != null ? dep.getMigrationStatus().getValue() : "UNKNOWN",
            dep.isTransitive() ? "Transitive" : "Direct"
        );

        Messages.showInfoMessage(project, details, "Dependency Details - " + dep.getDisplayName());
    }

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
        dep.setTransitive(isBlocker);  // Reusing isBlocker for transitive

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
}
