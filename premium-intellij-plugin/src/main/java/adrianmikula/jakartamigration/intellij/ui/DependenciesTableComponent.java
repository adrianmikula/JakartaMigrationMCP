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
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Dependencies table component with colored status indicators and dependency
 * type column.
 * Updated to show:
 * - Colored status dot (green/yellow/red)
 * - Dependency type (Direct/Transitive)
 * - Jakarta Equivalent columns
 * - Bottom panel for refactoring recipes (premium feature)
 */
public class DependenciesTableComponent {
    private final JPanel panel;
    private final Project project;
    private final JBTable table;
    private final DefaultTableModel tableModel;
    private final JTextField searchField;
    private final JComboBox<String> statusFilter;
    private final JCheckBox transitiveFilter;
    private final JCheckBox organizationalFilter;
    private List<DependencyInfo> allDependencies;
    
    // Bottom panel for recipes
    private JPanel recipesPanel;
    private JList<String> recipeList;
    private DefaultListModel<String> recipeListModel;
    private JButton applyRecipeButton;
    private JLabel selectedDependencyLabel;
    private boolean isPremiumUser = false;

    // Status colors
    private static final Color STATUS_COMPATIBLE = new Color(40, 167, 69); // Green
    private static final Color STATUS_NEEDS_UPGRADE = new Color(255, 193, 7); // Yellow
    private static final Color STATUS_NO_JAKARTA = new Color(220, 53, 69); // Red
    private static final Color STATUS_UNKNOWN = new Color(108, 117, 125); // Gray

    public DependenciesTableComponent(Project project) {
        this.project = project;
        this.allDependencies = new ArrayList<>();
        this.panel = new JBPanel<>(new BorderLayout());

        // Columns with Jakarta Equivalent information
        String[] columns = {
                "Group ID",
                "Artifact ID",
                "Current Version",
                "Jakarta Equivalent",
                "Recommended Version",
                "Compatibility Status",
                "Dependency Type", // Direct or Transitive
                "Status" // Colored dot indicator
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
        this.statusFilter = new JComboBox<>(new String[] {
                "All", "Compatible", "Needs Upgrade", "No Jakarta Version"
        });
        this.transitiveFilter = new JCheckBox("Show Transitive Only", false);
        this.organizationalFilter = new JCheckBox("Show All Organisational Artifacts", false);

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
            // Status column is now at index 7
            if (column == 7 && value instanceof DependencyInfo) {
                DependencyInfo dep = (DependencyInfo) value;
                JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
                panel.setOpaque(true);

                // Set background for selection
                if (isSelected) {
                    panel.setBackground(table.getSelectionBackground());
                } else {
                    if (dep.isOrganizational()) {
                        panel.setBackground(new Color(230, 240, 255)); // Light blue tint
                    } else {
                        panel.setBackground(table.getBackground());
                    }
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
                        ? dep.getMigrationStatus().getValue()
                        : "UNKNOWN");
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
                // Determine if this row is organizational (check status column at index 7)
                boolean isOrg = false;
                Object depObj = table.getModel().getValueAt(row, 7);
                if (depObj instanceof DependencyInfo) {
                    isOrg = ((DependencyInfo) depObj).isOrganizational();
                }

                if (isOrg) {
                    label.setBackground(new Color(230, 240, 255)); // Light blue for organizational
                } else {
                    label.setBackground(table.getBackground());
                }
            }
            label.setHorizontalAlignment(SwingConstants.LEFT);

            return label;
        }

        private static Color getStatusColor(DependencyMigrationStatus status) {
            if (status == null)
                return STATUS_UNKNOWN;
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

        organizationalFilter.addActionListener(e -> filterDependencies());
        headerPanel.add(organizationalFilter);

        // Table
        JBScrollPane scrollPane = new JBScrollPane(table);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // Set column widths (7 columns now)
        table.getColumnModel().getColumn(0).setPreferredWidth(150); // Group ID
        table.getColumnModel().getColumn(1).setPreferredWidth(150); // Artifact ID
        table.getColumnModel().getColumn(2).setPreferredWidth(90); // Current Version
        table.getColumnModel().getColumn(3).setPreferredWidth(150); // Jakarta Equivalent
        table.getColumnModel().getColumn(4).setPreferredWidth(90); // Recommended Version
        table.getColumnModel().getColumn(5).setPreferredWidth(100); // Compatibility Status
        table.getColumnModel().getColumn(6).setPreferredWidth(80); // Dependency Type
        table.getColumnModel().getColumn(7).setPreferredWidth(100); // Status

        // Add mouse listener for double-click navigation
        table.addMouseListener(new MouseInputAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    handleDoubleClick();
                }
            }
        });

        // Add selection listener to update recipes panel
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateRecipesPanel();
            }
        });

        // Initialize bottom panel for recipes
        initializeRecipesPanel();

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

        // Create center panel with table and recipes
        JPanel centerPanel = new JBPanel<>(new BorderLayout());
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        centerPanel.add(recipesPanel, BorderLayout.SOUTH);

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(centerPanel, BorderLayout.CENTER);
        panel.add(actionsPanel, BorderLayout.SOUTH);
    }

    private void initializeRecipesPanel() {
        recipesPanel = new JBPanel<>(new BorderLayout());
        recipesPanel.setBorder(new TitledBorder("Refactoring Recipes"));
        recipesPanel.setPreferredSize(new Dimension(0, 150));
        
        selectedDependencyLabel = new JLabel("Select a dependency to see available recipes");
        selectedDependencyLabel.setBorder(new EmptyBorder(5, 5, 5, 5));
        recipesPanel.add(selectedDependencyLabel, BorderLayout.NORTH);
        
        recipeListModel = new DefaultListModel<>();
        recipeList = new JList<>(recipeListModel);
        recipeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        JBScrollPane recipeScrollPane = new JBScrollPane(recipeList);
        recipesPanel.add(recipeScrollPane, BorderLayout.CENTER);
        
        JPanel recipeActionsPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT));
        applyRecipeButton = new JButton("Apply Recipe");
        applyRecipeButton.setEnabled(false);
        applyRecipeButton.addActionListener(this::handleApplyRecipe);
        recipeActionsPanel.add(applyRecipeButton);
        
        JLabel premiumHint = new JLabel("(Premium feature)");
        premiumHint.setForeground(Color.GRAY);
        recipeActionsPanel.add(premiumHint);
        
        recipesPanel.add(recipeActionsPanel, BorderLayout.SOUTH);
        
        // Initially hide the recipes panel
        recipesPanel.setVisible(false);
    }

    private void updateRecipesPanel() {
        List<DependencyInfo> selected = getSelectedDependencies();
        
        if (selected.isEmpty()) {
            recipesPanel.setVisible(false);
            return;
        }
        
        DependencyInfo dep = selected.get(0);
        
        // Show the recipes panel
        recipesPanel.setVisible(true);
        
        // Update the label
        String recommendedCoords = dep.getRecommendedArtifactCoordinates();
        if (recommendedCoords != null && !recommendedCoords.isEmpty() && !recommendedCoords.equals("-")) {
            selectedDependencyLabel.setText("Selected: " + dep.getDisplayName() + " → " + recommendedCoords);
        } else {
            selectedDependencyLabel.setText("Selected: " + dep.getDisplayName());
        }
        
        // Update recipe list
        recipeListModel.clear();
        
        String associatedRecipe = dep.getAssociatedRecipeName();
        if (associatedRecipe != null && !associatedRecipe.isEmpty()) {
            recipeListModel.addElement(associatedRecipe);
            recipeList.setSelectedIndex(0);
            applyRecipeButton.setEnabled(true);
        } else if (dep.getRecommendedVersion() != null && !dep.getRecommendedVersion().equals("-")) {
            // Add generic upgrade recipe if there's a recommended version
            String genericRecipe = "Upgrade to Jakarta: " + dep.getRecommendedArtifactId();
            recipeListModel.addElement(genericRecipe);
            recipeList.setSelectedIndex(0);
            applyRecipeButton.setEnabled(true);
        } else {
            recipeListModel.addElement("No recipes available");
            applyRecipeButton.setEnabled(false);
        }
    }

    private void handleApplyRecipe(ActionEvent e) {
        List<DependencyInfo> selected = getSelectedDependencies();
        if (selected.isEmpty()) {
            return;
        }
        
        DependencyInfo dep = selected.get(0);
        String selectedRecipe = recipeList.getSelectedValue();
        
        if (selectedRecipe == null || selectedRecipe.contains("No recipes")) {
            return;
        }
        
        if (!isPremiumUser) {
            Messages.showWarningDialog(project, 
                    "Applying recipes requires a Premium license.\nPlease upgrade to Premium to use this feature.", 
                    "Premium Feature");
            return;
        }
        
        int result = Messages.showYesNoDialog(project, 
                "Apply recipe '" + selectedRecipe + "' to migrate " + dep.getDisplayName() + "?",
                "Confirm Recipe Application",
                Messages.getQuestionIcon());
        
        if (result == Messages.YES) {
            // This would trigger the recipe execution
            // The actual implementation would call the refactoring service
            Messages.showInfoMessage(project, 
                    "Recipe application would be triggered here.\nThis will run the refactoring to migrate the dependency.", 
                    "Apply Recipe");
        }
    }

    public void setPremiumUser(boolean isPremium) {
        this.isPremiumUser = isPremium;
    }

    public boolean isPremiumUser() {
        return isPremiumUser;
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
        boolean showOrganizationalOnly = organizationalFilter.isSelected();

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

            // Organizational filter
            boolean matchesOrganizational = !showOrganizationalOnly || dep.isOrganizational();

            if (matchesSearch && matchesStatus && matchesTransitive && matchesOrganizational) {
                addDependencyRow(dep);
            }
        }
    }

    private String mapStatusToValue(String status) {
        if (status == null)
            return null;
        switch (status) {
            case "Compatible":
                return "COMPATIBLE";
            case "Needs Upgrade":
                return "NEEDS_UPGRADE";
            case "No Jakarta Version":
                return "NO_JAKARTA_VERSION";
            default:
                return null;
        }
    }

    private void addDependencyRow(DependencyInfo dep) {
        // Determine dependency type
        String dependencyType = dep.isTransitive() ? "Transitive" : "Direct";
        
        // Jakarta Equivalent
        String jakartaEquivalent = dep.getRecommendedArtifactId() != null 
                ? dep.getRecommendedArtifactCoordinates() 
                : "-";
        
        // Compatibility Status
        String compatibilityStatus = dep.getJakartaCompatibilityStatus() != null 
                ? dep.getJakartaCompatibilityStatus() 
                : (dep.getRecommendedVersion() != null && !dep.getRecommendedVersion().equals("-") 
                        ? "Compatible" : "Unknown");

        // Add row with all 8 columns
        tableModel.addRow(new Object[] {
                dep.getGroupId(),
                dep.getArtifactId(),
                dep.getCurrentVersion(),
                jakartaEquivalent,
                dep.getRecommendedVersion() != null ? dep.getRecommendedVersion() : "-",
                compatibilityStatus,
                dependencyType,
                dep // Full object for status column (last column)
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
                    .append(" -> ")
                    .append(dep.getRecommendedVersion() != null ? dep.getRecommendedVersion() : "No Jakarta version")
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
                dep.isTransitive() ? "Transitive" : "Direct");

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
        dep.setTransitive(isBlocker); // Reusing isBlocker for transitive

        allDependencies.add(dep);
        addDependencyRow(dep);
    }

    private DependencyMigrationStatus mapStringToStatus(String status) {
        if (status == null)
            return null;
        switch (status.toLowerCase()) {
            case "compatible":
                return DependencyMigrationStatus.COMPATIBLE;
            case "needs upgrade":
                return DependencyMigrationStatus.NEEDS_UPGRADE;
            case "no jakarta version":
                return DependencyMigrationStatus.NO_JAKARTA_VERSION;
            case "requires manual migration":
                return DependencyMigrationStatus.REQUIRES_MANUAL_MIGRATION;
            case "migrated":
                return DependencyMigrationStatus.MIGRATED;
            default:
                return DependencyMigrationStatus.COMPATIBLE;
        }
    }

    public DefaultTableModel getTableModel() {
        return tableModel;
    }

    public JTextField getSearchField() {
        return searchField;
    }

    public JComboBox<String> getStatusFilter() {
        return statusFilter;
    }

    public JCheckBox getTransitiveFilter() {
        return transitiveFilter;
    }
}
