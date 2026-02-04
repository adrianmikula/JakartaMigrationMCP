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
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Subtask table component for migration phases.
 * Displays subtasks with action buttons for automatable tasks.
 */
public class SubtaskTableComponent {
    private final JPanel panel;
    private final JBTable table;
    private final DefaultTableModel tableModel;
    private final Project project;
    private final List<SubtaskItem> subtasks = new ArrayList<>();

    // Automation types that have core library support
    private static final String AUTOMATION_OPEN_REWRITE = "open-rewrite";
    private static final String AUTOMATION_BINARY_SCAN = "binary-scan";
    private static final String AUTOMATION_DEPENDENCY_UPDATE = "dependency-update";

    public SubtaskTableComponent(Project project) {
        this.project = project;
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

        public SubtaskItem(String name, String description, DependencyInfo dependency, String automationType) {
            this.name = name;
            this.description = description;
            this.dependency = dependency;
            this.automationType = automationType;
            this.completed = false;
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
        public DependencyInfo getDependency() { return dependency; }
        public String getAutomationType() { return automationType; }
        public boolean isCompleted() { return completed; }
        public void setCompleted(boolean completed) { this.completed = completed; }
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
            JPanel panel = new JPanel(new BorderLayout(5, 0));
            panel.setOpaque(true);

            if (isSelected) {
                panel.setBackground(table.getSelectionBackground());
            } else {
                panel.setBackground(table.getBackground());
            }

            switch (table.getColumnName(column)) {
                case "Status":
                    JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
                    statusPanel.setOpaque(true);
                    statusPanel.setBackground(panel.getBackground());

                    JPanel dot = new JPanel();
                    dot.setPreferredSize(new Dimension(10, 10));
                    dot.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
                    dot.setBackground(subtask.isCompleted() ? new Color(40, 167, 69) : new Color(108, 117, 125));

                    statusPanel.add(dot);
                    panel.add(statusPanel, BorderLayout.WEST);
                    break;

                case "Subtask":
                    JLabel nameLabel = new JLabel(subtask.getName());
                    nameLabel.setOpaque(true);
                    nameLabel.setBackground(panel.getBackground());
                    panel.add(nameLabel, BorderLayout.CENTER);
                    break;

                case "Dependency":
                    JLabel depLabel = new JLabel(subtask.getDependencyName());
                    depLabel.setOpaque(true);
                    depLabel.setBackground(panel.getBackground());
                    if (subtask.getDependency() != null) {
                        depLabel.setToolTipText(subtask.getDependency().getGroupId() + ":" +
                            subtask.getDependency().getArtifactId() + " v" + subtask.getDependency().getCurrentVersion());
                    }
                    panel.add(depLabel, BorderLayout.CENTER);
                    break;

                case "Action":
                    if (subtask.hasAutomation()) {
                        JButton actionButton = createActionButton(subtask);
                        panel.add(actionButton, BorderLayout.EAST);
                    } else {
                        JLabel noActionLabel = new JLabel("-");
                        noActionLabel.setHorizontalAlignment(SwingConstants.CENTER);
                        panel.add(noActionLabel, BorderLayout.CENTER);
                    }
                    break;
            }

            return panel;
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
        if (subtask == null) return;

        switch (subtask.getAutomationType()) {
            case AUTOMATION_OPEN_REWRITE:
                handleOpenRewriteAction(subtask);
                break;
            case AUTOMATION_BINARY_SCAN:
                handleBinaryScanAction(subtask);
                break;
            case AUTOMATION_DEPENDENCY_UPDATE:
                handleDependencyUpdateAction(subtask);
                break;
            default:
                Messages.showInfoMessage(project, "Action: " + subtask.getName(), "Subtask Action");
        }
    }

    private void handleOpenRewriteAction(SubtaskItem subtask) {
        String message = String.format("""
            Run OpenRewrite Refactoring

            Task: %s
            %s

            This will automatically refactor javax.* imports to jakarta.*
            in the selected scope.
            """,
            subtask.getName(),
            subtask.getDependency() != null ? "Dependency: " + subtask.getDependencyName() : "");

        int result = Messages.showYesNoDialog(project, message, "OpenRewrite Migration",
                Messages.getQuestionIcon());

        if (result == Messages.YES) {
            subtask.setCompleted(true);
            refreshTable();
            Messages.showInfoMessage(project, "OpenRewrite refactoring started...", "OpenRewrite");
        }
    }

    private void handleBinaryScanAction(SubtaskItem subtask) {
        String message = String.format("""
            Scan Binary Dependency

            Task: %s
            Dependency: %s

            This will scan the binary JAR for Jakarta EE compatibility issues.
            """,
            subtask.getName(),
            subtask.getDependency() != null ? subtask.getDependencyName() : "N/A");

        Messages.showInfoMessage(project, message, "Binary Scan");
    }

    private void handleDependencyUpdateAction(SubtaskItem subtask) {
        if (subtask.getDependency() == null) return;

        String message = String.format("""
            Update Dependency

            Update: %s
            Current: %s
            Recommended: %s

            This will update the dependency version in your build file.
            """,
            subtask.getDependencyName(),
            subtask.getDependency().getCurrentVersion(),
            subtask.getDependency().getRecommendedVersion() != null ?
                subtask.getDependency().getRecommendedVersion() : "N/A");

        int result = Messages.showYesNoDialog(project, message, "Update Dependency",
                Messages.getQuestionIcon());

        if (result == Messages.YES) {
            subtask.setCompleted(true);
            refreshTable();
            Messages.showInfoMessage(project, "Dependency update started...", "Update");
        }
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
