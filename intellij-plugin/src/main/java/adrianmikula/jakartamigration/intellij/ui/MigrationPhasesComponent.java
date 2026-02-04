package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.ui.MigrationStrategyComponent.MigrationStrategy;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Migration phases component from TypeSpec: plugin-components.tsp
 * Updated to include migration strategy selection.
 */
public class MigrationPhasesComponent {
    private final JPanel panel;
    private final Project project;
    private final JBTable phasesTable;
    private final DefaultTableModel tableModel;
    private final MigrationStrategyComponent strategyComponent;
    private MigrationStrategy selectedStrategy;
    private final List<PhaseListener> phaseListeners = new ArrayList<>();

    public interface PhaseListener {
        void onStrategySelected(MigrationStrategy strategy);
        void onPhaseSelected(int phaseIndex);
    }

    public MigrationPhasesComponent(Project project) {
        this.project = project;
        this.panel = new JBPanel<>(new BorderLayout());
        this.strategyComponent = new MigrationStrategyComponent(project);
        
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
        setupStrategyListener();
    }

    private void setupStrategyListener() {
        strategyComponent.addMigrationStrategyListener(strategy -> {
            this.selectedStrategy = strategy;
            for (PhaseListener listener : phaseListeners) {
                listener.onStrategySelected(strategy);
            }
            // Update phases based on selected strategy
            updatePhasesForStrategy(strategy);
        });
    }

    private void updatePhasesForStrategy(MigrationStrategy strategy) {
        // Clear existing phases
        tableModel.setRowCount(0);
        
        // Generate phases based on strategy
        switch (strategy) {
            case BIG_BANG:
                addPhase("phase-1", "Complete Migration", "Ready", 1, 4, "None", 15);
                break;
            case INCREMENTAL:
                addPhase("phase-1", "Dependency Updates", "Ready", 1, 2, "None", 5);
                addPhase("phase-2", "Code Imports", "Pending", 2, 4, "Phase 1", 4);
                addPhase("phase-3", "Configuration", "Pending", 3, 3, "Phase 2", 3);
                addPhase("phase-4", "Testing", "Pending", 4, 3, "Phase 3", 3);
                break;
            case BUILD_TRANSFORMATION:
                addPhase("phase-1", "Recipe Setup", "Ready", 1, 1, "None", 2);
                addPhase("phase-2", "Build Integration", "Pending", 2, 3, "Phase 1", 3);
                addPhase("phase-3", "Transformation", "Pending", 3, 6, "Phase 2", 6);
                addPhase("phase-4", "Verification", "Pending", 4, 4, "Phase 3", 4);
                break;
            case RUNTIME_TRANSFORMATION:
                addPhase("phase-1", "Adapter Setup", "Ready", 1, 2, "None", 4);
                addPhase("phase-2", "Runtime Configuration", "Pending", 2, 3, "Phase 1", 3);
                addPhase("phase-3", "Integration Testing", "Pending", 3, 5, "Phase 2", 5);
                addPhase("phase-4", "Production Rollout", "Pending", 4, 3, "Phase 3", 3);
                break;
        }
    }

    private void initializeComponent() {
        // Strategy selection header
        JPanel strategyHeaderPanel = new JBPanel<>(new BorderLayout());
        strategyHeaderPanel.add(strategyComponent.getPanel(), BorderLayout.CENTER);
        
        // Phases section header
        JPanel phasesHeaderPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT));
        phasesHeaderPanel.add(new JLabel("Migration Strategy"));
        
        // Progress bar
        JProgressBar overallProgress = new JProgressBar(0, 100);
        overallProgress.setStringPainted(true);
        overallProgress.setString("0% Complete");
        phasesHeaderPanel.add(overallProgress);

        // Combined north panel with strategy and phases header
        JPanel northPanel = new JBPanel<>(new BorderLayout());
        northPanel.add(strategyHeaderPanel, BorderLayout.NORTH);
        northPanel.add(phasesHeaderPanel, BorderLayout.CENTER);

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

        panel.add(northPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.SOUTH);
    }

    public JPanel getPanel() {
        return panel;
    }

    public void addPhaseListener(PhaseListener listener) {
        phaseListeners.add(listener);
    }

    public MigrationStrategy getSelectedStrategy() {
        return selectedStrategy;
    }

    public void addPhase(String id, String name, String status, int order, Integer duration, String prerequisites, int taskCount) {
        tableModel.addRow(new Object[]{
            id, name, status, order, 
            duration != null ? duration.toString() : "N/A",
            prerequisites, taskCount + " tasks"
        });
    }

    public void addPhase(String id, String name, String status, int order, Integer duration, String prerequisites) {
        addPhase(id, name, status, order, duration, prerequisites, 0);
    }
}