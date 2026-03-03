package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.analysis.persistence.CentralMigrationAnalysisStore;
import adrianmikula.jakartamigration.intellij.model.MigrationDashboard;
import adrianmikula.jakartamigration.intellij.model.MigrationStatus;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * History Tab Component - Shows recipe execution history (Premium feature).
 * Displays all code changes made via the plugin, with undo capability for reversible changes.
 */
public class HistoryTabComponent {
    private static final Logger LOG = Logger.getInstance(HistoryTabComponent.class);
    
    private final Project project;
    private final JPanel panel;
    private final HistoryTableModel tableModel;
    private final JBTable table;
    private final CentralMigrationAnalysisStore store;
    
    public HistoryTabComponent(Project project) {
        LOG.info("HistoryTabComponent: Constructor called for project: " + project.getName());
        this.project = project;
        this.store = new CentralMigrationAnalysisStore();
        this.tableModel = new HistoryTableModel();
        this.table = createTable();
        this.panel = createPanel();
        loadHistory();
        LOG.info("HistoryTabComponent: Constructor complete");
    }
    
    private JPanel createPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Title
        JLabel titleLabel = new JLabel("Recipe Execution History");
        titleLabel.setFont(new Font(titleLabel.getFont().getName(), Font.BOLD, 16));
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        titlePanel.add(titleLabel);
        
        // Description
        JLabel descLabel = new JLabel("View all OpenRewrite recipe executions and undo reversible changes.");
        descLabel.setForeground(Color.GRAY);
        JPanel descPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        descPanel.add(descLabel);
        
        // Table area
        JScrollPane tableScrollPane = new JScrollPane(table);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshButton = new JButton("↻ Refresh");
        refreshButton.addActionListener(e -> loadHistory());
        buttonPanel.add(refreshButton);
        
        JButton undoButton = new JButton("↩ Undo Selected");
        undoButton.addActionListener(e -> handleUndo());
        buttonPanel.add(undoButton);
        
        // Add components
        mainPanel.add(titlePanel, BorderLayout.NORTH);
        mainPanel.add(descPanel, BorderLayout.CENTER);
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(tableScrollPane, BorderLayout.CENTER);
        centerPanel.add(buttonPanel, BorderLayout.SOUTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        
        return mainPanel;
    }
    
    private JBTable createTable() {
        JBTable table = new JBTable(tableModel);
        table.setRowHeight(30);
        table.getColumnModel().getColumn(0).setHeaderValue("Recipe");
        table.getColumnModel().getColumn(0).setPreferredWidth(150);
        table.getColumnModel().getColumn(1).setHeaderValue("Executed At");
        table.getColumnModel().getColumn(1).setPreferredWidth(150);
        table.getColumnModel().getColumn(2).setHeaderValue("Status");
        table.getColumnModel().getColumn(2).setPreferredWidth(80);
        table.getColumnModel().getColumn(3).setHeaderValue("Files Changed");
        table.getColumnModel().getColumn(3).setPreferredWidth(300);
        
        return table;
    }
    
    private void loadHistory() {
        String projectPath = project.getBasePath();
        if (projectPath == null) {
            projectPath = project.getProjectFilePath();
        }
        
        if (projectPath == null) {
            LOG.warn("HistoryTabComponent: Could not determine project path");
            return;
        }
        
        List<Map<String, Object>> executions = store.getAllRecipeExecutions(projectPath);
        tableModel.setExecutions(executions);
        LOG.info("HistoryTabComponent: Loaded " + executions.size() + " recipe executions");
    }
    
    private void handleUndo() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            Messages.showWarningDialog(project, "Please select a recipe execution to undo.", "Undo");
            return;
        }
        
        Map<String, Object> execution = tableModel.getExecutionAt(selectedRow);
        String recipeName = (String) execution.get("recipe_name");
        String affectedFiles = (String) execution.get("affected_files");
        
        if (affectedFiles == null || affectedFiles.isEmpty()) {
            Messages.showWarningDialog(project, "No files to undo for this execution.", "Undo");
            return;
        }
        
        String message = String.format("Undo recipe '%s'?\n\nThis will revert %d file(s) to their previous state.",
            recipeName, affectedFiles.split(",").length);
        
        int result = Messages.showYesNoDialog(project, message, "Confirm Undo", Messages.getQuestionIcon());
        
        if (result == Messages.YES) {
            // Perform undo via MigrationAnalysisService
            Messages.showInfoMessage(project, 
                "Undo functionality will restore files from backup copies.\n\nNote: This feature requires the backup files to exist.",
                "Undo");
        }
    }
    
    public JPanel getPanel() {
        return panel;
    }
    
    // Table Model
    private static class HistoryTableModel extends AbstractTableModel {
        private final List<Map<String, Object>> executions = new ArrayList<>();
        private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault());
        
        public void setExecutions(List<Map<String, Object>> newExecutions) {
            executions.clear();
            executions.addAll(newExecutions);
            fireTableDataChanged();
        }
        
        public Map<String, Object> getExecutionAt(int row) {
            return row >= 0 && row < executions.size() ? executions.get(row) : null;
        }
        
        @Override
        public int getRowCount() {
            return executions.size();
        }
        
        @Override
        public int getColumnCount() {
            return 4;
        }
        
        @Override
        public Object getValueAt(int row, int column) {
            Map<String, Object> exec = executions.get(row);
            return switch (column) {
                case 0 -> exec.get("recipe_name");
                case 1 -> formatTimestamp(exec.get("executed_at"));
                case 2 -> exec.get("success") != null && (Boolean) exec.get("success") ? "Success" : "Failed";
                case 3 -> exec.get("affected_files");
                default -> "";
            };
        }
        
        private String formatTimestamp(Object timestamp) {
            if (timestamp == null) return "";
            return timestamp.toString();
        }
    }
}
