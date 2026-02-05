package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.analysis.persistence.SqliteMigrationAnalysisStore;
import adrianmikula.jakartamigration.analysis.persistence.SqliteMigrationAnalysisStore.StoredRefactoringTask;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.JBOptionPane;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Refactor tab component showing refactoring tasks with auto-fix capabilities.
 * This is a premium feature that requires a license.
 */
public class RefactorTabComponent {
    private static final Logger LOG = Logger.getInstance(RefactorTabComponent.class);
    
    private final JPanel contentPanel;
    private final JBTable tasksTable;
    private final DefaultTableModel tableModel;
    private final JButton applyFixButton;
    private final JButton applyAllButton;
    private final JButton refreshButton;
    private final JLabel statusLabel;
    private final JLabel premiumLabel;
    private final Project project;
    
    private static final String[] COLUMN_NAMES = {
        "Type", "File", "Description", "Priority", "Status"
    };
    
    public RefactorTabComponent(Project project) {
        this.project = project;
        this.contentPanel = new JBPanel<>(new BorderLayout());
        this.tableModel = new DefaultTableModel(COLUMN_NAMES, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.tasksTable = new JBTable(tableModel) {
            @Override
            public TableCellRenderer getCellRenderer(int row, int column) {
                return new TaskCellRenderer();
            }
        };
        this.applyFixButton = new JButton("Apply Fix");
        this.applyAllButton = new JButton("Apply All");
        this.refreshButton = new JButton("Refresh");
        this.statusLabel = new JLabel("No refactoring tasks");
        
        initializeUI();
    }
    
    private void initializeUI() {
        // Toolbar
        JPanel toolbarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        toolbarPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        
        applyFixButton.addActionListener(e -> applySelectedFix());
        applyFixButton.setEnabled(false);
        toolbarPanel.add(applyFixButton);
        
        applyAllButton.addActionListener(e -> applyAllFixes());
        toolbarPanel.add(applyAllButton);
        
        refreshButton.addActionListener(e -> loadRefactoringTasks());
        toolbarPanel.add(refreshButton);
        
        toolbarPanel.add(Box.createHorizontalStrut(10));
        toolbarPanel.add(statusLabel);
        
        toolbarPanel.add(Box.createHorizontalGlue());
        
        // Premium indicator
        premiumLabel = new JLabel("🔒 Premium Feature");
        premiumLabel.setFont(premiumLabel.getFont().deriveFont(Font.BOLD, 11f));
        premiumLabel.setForeground(new Color(150, 100, 50));
        JPanel premiumPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        premiumPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        premiumPanel.setBackground(new Color(255, 248, 220));
        premiumPanel.add(premiumLabel);
        
        // Table with scroll
        JScrollPane scrollPane = new JScrollPane(tasksTable);
        scrollPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        scrollPane.setPreferredSize(new Dimension(600, 300));
        
        // Enable apply fix button when row is selected
        tasksTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                applyFixButton.setEnabled(tasksTable.getSelectedRowCount() > 0);
            }
        });
        
        contentPanel.add(toolbarPanel, BorderLayout.NORTH);
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        contentPanel.add(premiumPanel, BorderLayout.SOUTH);
    }
    
    /**
     * Load refactoring tasks from the database.
     */
    public void loadRefactoringTasks() {
        tableModel.setRowCount(0);
        
        try {
            Path projectPath = getProjectPath();
            if (projectPath == null) {
                statusLabel.setText("No project open");
                return;
            }
            
            try (var store = new SqliteMigrationAnalysisStore(projectPath)) {
                List<StoredRefactoringTask> tasks = store.getRefactoringTasks(projectPath);
                
                if (tasks.isEmpty()) {
                    statusLabel.setText("No refactoring tasks - run analysis first");
                    addEmptyStateRow();
                } else {
                    statusLabel.setText(tasks.size() + " refactoring tasks");
                    for (StoredRefactoringTask task : tasks) {
                        addTaskRow(task);
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to load refactoring tasks", e);
            statusLabel.setText("Error loading tasks");
            addEmptyStateRow();
        }
    }
    
    private void addTaskRow(StoredRefactoringTask task) {
        Object[] row = {
            getTypeIcon(task.taskType()),
            task.filePath(),
            task.description(),
            task.priority(),
            getStatusLabel(task.status())
        };
        tableModel.addRow(row);
    }
    
    private void addEmptyStateRow() {
        Object[] row = {
            "📋",
            "No tasks yet",
            "Run migration analysis to generate refactoring tasks",
            "-",
            "Pending"
        };
        tableModel.addRow(row);
    }
    
    private String getTypeIcon(String taskType) {
        if (taskType == null) return "📋";
        return switch (taskType.toUpperCase()) {
            case "IMPORT_FIX" -> "📥";
            case "XML_NAMESPACE_FIX" -> "📄";
            case "REFLECTION_FIX" -> "🔍";
            case "DEPENDENCY_UPDATE" -> "📦";
            default -> "📋";
        };
    }
    
    private String getStatusLabel(String status) {
        if (status == null) return "⏳ Pending";
        return switch (status.toLowerCase()) {
            case "pending" -> "⏳ Pending";
            case "in_progress" -> "🔄 In Progress";
            case "completed" -> "✅ Completed";
            case "failed" -> "❌ Failed";
            default -> "⏳ Pending";
        };
    }
    
    private void applySelectedFix() {
        int selectedRow = tasksTable.getSelectedRow();
        if (selectedRow == -1) {
            Messages.showWarningDialog(project,
                "Please select a refactoring task to apply",
                "No Selection");
            return;
        }
        
        // Check if it's the empty state row
        String filePath = (String) tableModel.getValueAt(selectedRow, 1);
        if (filePath != null && filePath.contains("No tasks")) {
            Messages.showInfoMessage(project,
                "No refactoring tasks available. Run migration analysis first.",
                "No Tasks");
            return;
        }
        
        showPremiumRequiredDialog();
    }
    
    private void applyAllFixes() {
        int rowCount = tableModel.getRowCount();
        if (rowCount == 0) {
            Messages.showInfoMessage(project,
                "No refactoring tasks to apply",
                "Empty List");
            return;
        }
        
        showPremiumRequiredDialog();
    }
    
    private void showPremiumRequiredDialog() {
        JBOptionPane.showMessageDialog(contentPanel,
            "<html><body><p><b>Premium Feature Required</b></p>" +
            "<p>This feature requires a Premium license.</p>" +
            "<p>Visit the JetBrains Marketplace to purchase a license.</p>" +
            "<p><a href='https://plugins.jetbrains.com/plugin/your-plugin-id'>Get Premium</a></p>" +
            "</body></html>",
            "Premium Feature",
            JBOptionPane.INFORMATION_MESSAGE);
    }
    
    private Path getProjectPath() {
        String basePath = project.getBasePath();
        if (basePath != null) {
            return Paths.get(basePath);
        }
        return null;
    }
    
    /**
     * Task cell renderer for status colors.
     */
    private static class TaskCellRenderer implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            JLabel label = new JLabel(value != null ? value.toString() : "");
            label.setOpaque(true);
            
            if (isSelected) {
                label.setBackground(table.getSelectionBackground());
                label.setForeground(table.getSelectionForeground());
            } else {
                label.setBackground(table.getBackground());
                label.setForeground(table.getForeground());
                
                // Color code by status (column 4)
                if (column == 4 && value != null) {
                    String status = value.toString();
                    if (status.startsWith("✅")) {
                        label.setForeground(new Color(0, 128, 0));
                    } else if (status.startsWith("❌")) {
                        label.setForeground(new Color(200, 0, 0));
                    } else if (status.startsWith("🔄")) {
                        label.setForeground(new Color(0, 100, 200));
                    }
                }
            }
            
            return label;
        }
    }
    
    public JPanel getPanel() {
        return contentPanel;
    }
}
