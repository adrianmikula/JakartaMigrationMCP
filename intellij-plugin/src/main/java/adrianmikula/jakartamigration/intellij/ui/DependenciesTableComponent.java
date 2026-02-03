package adrianmikula.jakartamigration.intellij.ui;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Dependencies table component from TypeSpec: plugin-components.tsp
 */
public class DependenciesTableComponent {
    private final JPanel panel;
    private final Project project;
    private final JBTable table;
    private final DefaultTableModel tableModel;

    public DependenciesTableComponent(Project project) {
        this.project = project;
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
        initializeComponent();
    }

    private void initializeComponent() {
        // Header with filters
        JPanel headerPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT));
        headerPanel.add(new JLabel("Dependencies Analysis"));
        
        JTextField searchField = new JTextField(20);
        searchField.setToolTipText("Search dependencies...");
        headerPanel.add(searchField);
        
        JComboBox<String> statusFilter = new JComboBox<>(new String[]{
            "All", "Compatible", "Needs Upgrade", "No Jakarta Version", "Requires Manual Migration"
        });
        headerPanel.add(statusFilter);
        
        JCheckBox blockersOnly = new JCheckBox("Show blockers only");
        headerPanel.add(blockersOnly);

        // Table
        JBScrollPane scrollPane = new JBScrollPane(table);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // Actions panel
        JPanel actionsPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT));
        JButton updateButton = new JButton("Update Selected");
        JButton viewDetailsButton = new JButton("View Details");
        actionsPanel.add(updateButton);
        actionsPanel.add(viewDetailsButton);

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(actionsPanel, BorderLayout.SOUTH);
    }

    public JPanel getPanel() {
        return panel;
    }

    public void addDependency(String groupId, String artifactId, String currentVersion, 
                             String recommendedVersion, String status, boolean isBlocker, 
                             String riskLevel, String impact) {
        tableModel.addRow(new Object[]{
            groupId, artifactId, currentVersion, recommendedVersion,
            status, isBlocker ? "Yes" : "No", riskLevel, impact
        });
    }
}