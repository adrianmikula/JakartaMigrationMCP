package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * Subtask table component for migration phases.
 * Displays subtasks with action buttons for automatable tasks.
 * Connected to real MCP server or core migration library.
 */
public class SubtaskTableComponent {
    private static final Logger LOG = Logger.getInstance(SubtaskTableComponent.class);

    private final JPanel panel;
    private final JBTable table;
    private final DefaultTableModel tableModel;
    private final Project project;
    private final List<SubtaskItem> subtasks = new ArrayList<>();
    private MigrationActionHandler actionHandler;

    // Automation types that have core library support
    private static final String AUTOMATION_OPEN_REWRITE = "open-rewrite";
    private static final String AUTOMATION_BINARY_SCAN = "binary-scan";
    private static final String AUTOMATION_DEPENDENCY_UPDATE = "dependency-update";

    public SubtaskTableComponent(Project project) {
        this.project = project;
        this.actionHandler = new MigrationActionHandler(project);
        this.panel = new JBPanel<>(new BorderLayout());

        String[] columns = {
            "Status",
            "Subtask",
            "Dependency",
            "Action"
        };

        this.tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // No editing - use clickable buttons
            }
        };

        this.table = new JBTable(tableModel) {
            @Override
            public TableCellRenderer getCellRenderer(int row, int column) {
                return new SubtaskCellRenderer();
            }
        };

        // Add mouse listener for action column clicks
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row >= 0 && row < subtasks.size() && col == 3) {
                    SubtaskItem subtask = subtasks.get(row);
                    if (subtask.hasAutomation()) {
                        handleAction(subtask);
                    }
                }
            }
        });

        initializeComponent();
    }

    /**
     * Subtask item with automation type indicator.
     */
    public static class SubtaskItem {
        private final String name;
        private final String description;
        private final DependencyInfo dependency;
        private final String automationType; // null if no automation available
        private boolean completed;
        private boolean inProgress;

        public SubtaskItem(String name, String description, DependencyInfo dependency, String automationType) {
            this.name = name;
            this.description = description;
            this.dependency = dependency;
            this.automationType = automationType;
            this.completed = false;
            this.inProgress = false;
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
        public DependencyInfo getDependency() { return dependency; }
        public String getAutomationType() { return automationType; }
        public boolean isCompleted() { return completed; }
        public boolean isInProgress() { return inProgress; }
        public void setCompleted(boolean completed) { this.completed = completed; }
        public void setInProgress(boolean inProgress) { this.inProgress = inProgress; }
        public boolean hasAutomation() { return automationType != null; }
        public String getDependencyName() {
            return dependency != null ? dependency.getArtifactId() : "";
        }
    }

    /**
     * Custom cell renderer for subtask rows.
     */
    private class SubtaskCellRenderer implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            if (row < 0 || row >= subtasks.size()) {
                return new JLabel("");
            }

            SubtaskItem subtask = subtasks.get(row);
            JPanel cellPanel = new JPanel(new BorderLayout(5, 0));
            cellPanel.setOpaque(true);

            if (isSelected) {
                cellPanel.setBackground(table.getSelectionBackground());
            } else {
                cellPanel.setBackground(table.getBackground());
            }

            String columnName = table.getColumnName(column);
            switch (columnName) {
                case "Status":
                    JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
                    statusPanel.setOpaque(true);
                    statusPanel.setBackground(cellPanel.getBackground());

                    JPanel dot = new JPanel();
                    dot.setPreferredSize(new Dimension(10, 10));
                    dot.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
                    
                    if (subtask.isCompleted()) {
                        dot.setBackground(new Color(40, 167, 69)); // Green - completed
                    } else if (subtask.isInProgress()) {
                        dot.setBackground(new Color(255, 193, 7)); // Yellow - in progress
                    } else {
                        dot.setBackground(new Color(108, 117, 125)); // Gray - pending
                    }

                    statusPanel.add(dot);
                    cellPanel.add(statusPanel, BorderLayout.WEST);
                    break;

                case "Subtask":
                    JLabel nameLabel = new JLabel(subtask.getName());
                    nameLabel.setOpaque(true);
                    nameLabel.setBackground(cellPanel.getBackground());
                    cellPanel.add(nameLabel, BorderLayout.CENTER);
                    break;

                case "Dependency":
                    JLabel depLabel = new JLabel(subtask.getDependencyName());
                    depLabel.setOpaque(true);
                    depLabel.setBackground(cellPanel.getBackground());
                    if (subtask.getDependency() != null) {
                        depLabel.setToolTipText(subtask.getDependency().getGroupId() + ":" +
                            subtask.getDependency().getArtifactId() + " v" + subtask.getDependency().getCurrentVersion());
                    }
                    cellPanel.add(depLabel, BorderLayout.CENTER);
                    break;

                case "Action":
                    if (subtask.hasAutomation()) {
                        JButton actionButton = createActionButton(subtask);
                        cellPanel.add(actionButton, BorderLayout.EAST);
                    } else {
                        JLabel noActionLabel = new JLabel("-");
                        noActionLabel.setHorizontalAlignment(SwingConstants.CENTER);
                        cellPanel.add(noActionLabel, BorderLayout.CENTER);
                    }
                    break;
            }

            return cellPanel;
        }

        private JButton createActionButton(SubtaskItem subtask) {
            String buttonText;
            String tooltip;

            switch (subtask.getAutomationType()) {
                case AUTOMATION_OPEN_REWRITE:
                    buttonText = "Run OpenRewrite";
                    tooltip = "Apply automated refactoring using OpenRewrite";
                    break;
                case AUTOMATION_BINARY_SCAN:
                    buttonText = "Scan Binary";
                    tooltip = "Scan binary dependency for compatibility issues";
                    break;
                case AUTOMATION_DEPENDENCY_UPDATE:
                    buttonText = "Update";
                    tooltip = "Automatically update to recommended version";
                    break;
                default:
                    buttonText = "Run";
                    tooltip = "Run automated task";
            }

            JButton button = new JButton(buttonText);
            button.setToolTipText(tooltip);
            button.setFont(button.getFont().deriveFont(Font.BOLD));
            button.addActionListener(e -> handleAction(subtask));
            return button;
        }
    }

    private void handleAction(SubtaskItem subtask) {
        if (subtask == null || !subtask.hasAutomation()) return;

        subtask.setInProgress(true);
        refreshTable();

        switch (subtask.getAutomationType()) {
            case AUTOMATION_OPEN_REWRITE:
                actionHandler.handleOpenRewriteAction(subtask, this::handleActionComplete);
                break;
            case AUTOMATION_BINARY_SCAN:
                actionHandler.handleBinaryScanAction(subtask, this::handleActionComplete);
                break;
            case AUTOMATION_DEPENDENCY_UPDATE:
                actionHandler.handleDependencyUpdateAction(subtask, this::handleActionComplete);
                break;
            default:
                subtask.setInProgress(false);
                Messages.showInfoMessage(project, "Action: " + subtask.getName(), "Subtask Action");
        }
    }

    private void handleActionComplete(SubtaskItem subtask, String message) {
        SwingUtilities.invokeLater(() -> {
            subtask.setInProgress(false);
            if (message != null && !message.contains("successfully") && !message.contains("updated successfully")) {
                Messages.showMessageDialog(project, message, "Action Failed", Messages.getWarningIcon());
            } else if (message != null) {
                Messages.showInfoMessage(project, message, "Action Complete");
            }
            refreshTable();
        });
    }

    private void initializeComponent() {
        table.setRowHeight(35);
        table.getColumn("Status").setPreferredWidth(60);
        table.getColumn("Status").setMaxWidth(80);
        table.getColumn("Subtask").setPreferredWidth(200);
        table.getColumn("Dependency").setPreferredWidth(150);
        table.getColumn("Action").setPreferredWidth(100);
        table.getColumn("Action").setMaxWidth(120);

        JBScrollPane scrollPane = new JBScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(500, 150));
        scrollPane.setBorder(BorderFactory.createTitledBorder("Subtasks"));

        panel.add(scrollPane, BorderLayout.CENTER);
    }

    public void setSubtasks(List<SubtaskItem> newSubtasks) {
        subtasks.clear();
        if (newSubtasks != null) {
            subtasks.addAll(newSubtasks);
        }
        refreshTable();
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (SubtaskItem subtask : subtasks) {
            tableModel.addRow(new Object[]{
                subtask,
                subtask.getName(),
                subtask.getDependencyName(),
                subtask.hasAutomation() ? "Run" : "-"
            });
        }
    }

    public JPanel getPanel() {
        return panel;
    }

    /**
     * Create subtasks from phase definition and dependencies.
     */
    public static List<SubtaskItem> createSubtasks(
            String[] phaseSubtasks,
            List<DependencyInfo> dependencies,
            DependencyMigrationStatus targetStatus) {

        List<SubtaskItem> items = new ArrayList<>();

        for (String task : phaseSubtasks) {
            String automationType = determineAutomationType(task);
            items.add(new SubtaskItem(task, "", null, automationType));
        }

        if (dependencies != null && !dependencies.isEmpty()) {
            List<DependencyInfo> matching = dependencies.stream()
                .filter(d -> d.getMigrationStatus() == targetStatus)
                .toList();

            for (DependencyInfo dep : matching.stream().limit(10).toList()) {
                String task = String.format("Migrate %s:%s",
                    dep.getGroupId(), dep.getArtifactId());
                items.add(new SubtaskItem(task, "", dep, AUTOMATION_DEPENDENCY_UPDATE));
            }
        }

        return items;
    }

    private static String determineAutomationType(String task) {
        String lower = task.toLowerCase();
        if (lower.contains("import") || lower.contains("refactor") || lower.contains("rewrite")) {
            return AUTOMATION_OPEN_REWRITE;
        } else if (lower.contains("binary") || lower.contains("scan") || lower.contains("analyze")) {
            return AUTOMATION_BINARY_SCAN;
        } else if (lower.contains("update") || lower.contains("upgrade") || lower.contains("dependency")) {
            return AUTOMATION_DEPENDENCY_UPDATE;
        }
        return null;
    }
}
