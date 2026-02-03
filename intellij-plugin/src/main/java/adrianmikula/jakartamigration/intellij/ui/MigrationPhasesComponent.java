package adrianmikula.jakartamigration.intellij.ui;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Migration phases component from TypeSpec: plugin-components.tsp
 */
public class MigrationPhasesComponent {
    private final JPanel panel;
    private final Project project;
    private final JBTable phasesTable;
    private final DefaultTableModel tableModel;

    public MigrationPhasesComponent(Project project) {
        this.project = project;
        this.panel = new JBPanel<>(new BorderLayout());
        
        // Table columns from TypeSpec: MigrationPhase model
        String[] columns = {
            "Phase", "Name", "Status", "Order", "Duration (hrs)", "Prerequisites", "Tasks"
        };
        
        this.tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        this.phasesTable = new JBTable(tableModel);
        initializeComponent();
    }

    private void initializeComponent() {
        // Header
        JPanel headerPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT));
        headerPanel.add(new JLabel("Migration Phases"));
        
        // Progress bar
        JProgressBar overallProgress = new JProgressBar(0, 100);
        overallProgress.setStringPainted(true);
        overallProgress.setString("0% Complete");
        headerPanel.add(overallProgress);

        // Phases table
        JBScrollPane scrollPane = new JBScrollPane(phasesTable);
        phasesTable.setFillsViewportHeight(true);
        phasesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Actions panel
        JPanel actionsPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT));
        JButton startPhaseButton = new JButton("Start Phase");
        JButton pausePhaseButton = new JButton("Pause Phase");
        JButton skipPhaseButton = new JButton("Skip Phase");
        JButton viewDetailsButton = new JButton("View Details");
        
        actionsPanel.add(startPhaseButton);
        actionsPanel.add(pausePhaseButton);
        actionsPanel.add(skipPhaseButton);
        actionsPanel.add(viewDetailsButton);

        // Status panel
        JPanel statusPanel = new JBPanel<>(new GridLayout(2, 2, 5, 5));
        statusPanel.setBorder(BorderFactory.createTitledBorder("Execution Status"));
        
        statusPanel.add(new JLabel("Current Phase:"));
        statusPanel.add(new JLabel("None"));
        statusPanel.add(new JLabel("Status:"));
        statusPanel.add(new JLabel("Not Started"));

        JPanel bottomPanel = new JBPanel<>(new BorderLayout());
        bottomPanel.add(actionsPanel, BorderLayout.NORTH);
        bottomPanel.add(statusPanel, BorderLayout.CENTER);

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.SOUTH);
    }

    public JPanel getPanel() {
        return panel;
    }

    public void addPhase(String id, String name, String status, int order, 
                        Integer duration, String prerequisites, int taskCount) {
        tableModel.addRow(new Object[]{
            id, name, status, order, 
            duration != null ? duration.toString() : "N/A",
            prerequisites, taskCount + " tasks"
        });
    }
}