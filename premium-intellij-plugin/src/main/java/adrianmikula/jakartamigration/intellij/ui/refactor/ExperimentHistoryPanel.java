package adrianmikula.jakartamigration.intellij.ui.refactor;

import adrianmikula.jakartamigration.experiment.domain.ExperimentResult;
import adrianmikula.jakartamigration.experiment.domain.ExperimentStatus;
import adrianmikula.jakartamigration.experiment.domain.MigrationSequence;
import adrianmikula.jakartamigration.intellij.service.ExperimentService;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Panel for viewing experiment history.
 * Shows past refactor experiments with success/failure status and allows loading sequences for re-editing.
 */
public class ExperimentHistoryPanel {
    private static final Logger LOG = Logger.getInstance(ExperimentHistoryPanel.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JPanel panel;
    private final Project project;
    private final ExperimentService experimentService;
    
    // UI Components
    private JBTable historyTable;
    private HistoryTableModel tableModel;
    private JComboBox<String> statusFilter;
    private JBTextArea detailArea;
    
    // Callbacks
    private Consumer<MigrationSequence> onSequenceLoaded;
    private Consumer<ExperimentResult> onExperimentSelected;

    public ExperimentHistoryPanel(@NotNull Project project, ExperimentService experimentService) {
        this.project = project;
        this.experimentService = experimentService;
        this.panel = new JBPanel<>(new BorderLayout());
        
        initializeComponent();
    }

    private void initializeComponent() {
        // Filter panel
        JPanel filterPanel = createFilterPanel();
        
        // Table panel
        JPanel tablePanel = createTablePanel();
        
        // Detail panel
        JPanel detailPanel = createDetailPanel();
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tablePanel, detailPanel);
        splitPane.setDividerLocation(300);
        splitPane.setResizeWeight(0.6);
        
        JPanel mainPanel = new JBPanel<>(new BorderLayout());
        mainPanel.add(filterPanel, BorderLayout.NORTH);
        mainPanel.add(splitPane, BorderLayout.CENTER);
        
        panel.add(mainPanel, BorderLayout.CENTER);
        
        // Load history
        refreshHistory();
    }

    private JPanel createFilterPanel() {
        JPanel panel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        panel.add(new JBLabel("Filter by status:"));
        
        statusFilter = new JComboBox<>(new String[]{"All", "Success", "Failed", "Running"});
        statusFilter.addActionListener(e -> filterHistory());
        panel.add(statusFilter);
        
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshHistory());
        panel.add(refreshButton);
        
        return panel;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JBPanel<>(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY),
            "Experiment History",
            TitledBorder.LEFT,
            TitledBorder.TOP
        ));
        
        tableModel = new HistoryTableModel();
        historyTable = new JBTable(tableModel);
        
        // Custom renderer for status column
        historyTable.getColumnModel().getColumn(2).setCellRenderer(new StatusCellRenderer());
        
        // Set column widths
        historyTable.getColumnModel().getColumn(0).setPreferredWidth(150); // Date
        historyTable.getColumnModel().getColumn(1).setPreferredWidth(200); // Sequence Name
        historyTable.getColumnModel().getColumn(2).setPreferredWidth(80);  // Status
        historyTable.getColumnModel().getColumn(3).setPreferredWidth(100); // Files Modified
        historyTable.getColumnModel().getColumn(4).setPreferredWidth(100); // Test Results
        
        JBScrollPane scrollPane = new JBScrollPane(historyTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Double-click shows results in Experiment Results tab
        historyTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    fireExperimentSelected();
                }
            }
        });

        // Action buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton loadButton = new JButton("Load Sequence");
        JButton viewDetailsButton = new JButton("View Details");
        
        loadButton.addActionListener(e -> loadSelectedSequence());
        viewDetailsButton.addActionListener(e -> fireExperimentSelected());
        
        buttonPanel.add(loadButton);
        buttonPanel.add(viewDetailsButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }

    private JPanel createDetailPanel() {
        JPanel panel = new JBPanel<>(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY),
            "Experiment Details",
            TitledBorder.LEFT,
            TitledBorder.TOP
        ));
        
        detailArea = new JBTextArea();
        detailArea.setEditable(false);
        detailArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        
        JBScrollPane scrollPane = new JBScrollPane(detailArea);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }

    public void refreshHistory() {
        try {
            List<ExperimentResult> history = experimentService.getHistory(Optional.empty(), Optional.empty(), 50);
            tableModel.setHistory(history);
        } catch (Exception e) {
            LOG.error("Failed to load experiment history", e);
            tableModel.setHistory(List.of());
        }
    }

    private void filterHistory() {
        String selectedStatus = (String) statusFilter.getSelectedItem();
        Optional<ExperimentStatus> statusFilterOpt = switch (selectedStatus) {
            case "Success" -> Optional.of(ExperimentStatus.SUCCESS);
            case "Failed" -> Optional.of(ExperimentStatus.FAILED);
            case "Running" -> Optional.of(ExperimentStatus.RUNNING);
            default -> Optional.empty();
        };
        
        try {
            List<ExperimentResult> history = experimentService.getHistory(Optional.empty(), statusFilterOpt, 50);
            tableModel.setHistory(history);
        } catch (Exception e) {
            LOG.error("Failed to filter experiment history", e);
            tableModel.setHistory(List.of());
        }
    }

    private void loadSelectedSequence() {
        int selectedRow = historyTable.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }
        
        ExperimentResult result = tableModel.getExperimentAt(selectedRow);
        
        try {
            var sequenceOpt = experimentService.getSequenceByResult(result.runId());
            if (onSequenceLoaded != null) {
                sequenceOpt.ifPresent(onSequenceLoaded);
            }
        } catch (Exception e) {
            LOG.error("Failed to load sequence for experiment: " + result.runId(), e);
        }
    }

    private void viewSelectedDetails() {
        int selectedRow = historyTable.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }
        
        ExperimentResult result = tableModel.getExperimentAt(selectedRow);
        displayDetails(result);
    }

    private void displayDetails(ExperimentResult result) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("Experiment ID: ").append(result.runId()).append("\n");
        sb.append("Sequence: ").append(result.sequenceName()).append("\n");
        sb.append("Status: ").append(result.status()).append("\n");
        sb.append("Started: ").append(formatInstant(result.startedAt())).append("\n");
        sb.append("Finished: ").append(formatInstant(result.finishedAt())).append("\n");
        sb.append("Duration: ").append(
            java.time.Duration.between(result.startedAt(), result.finishedAt()).toSeconds()
        ).append(" seconds\n");
        sb.append("\n");
        
        if (result.diffSummary() != null) {
            sb.append("Summary: ").append(result.diffSummary()).append("\n");
            sb.append("\n");
        }
        
        if (!result.stepResults().isEmpty()) {
            sb.append("Step Results:\n");
            for (int i = 0; i < result.stepResults().size(); i++) {
                var step = result.stepResults().get(i);
                sb.append("  Step ").append(i + 1).append(": ");
                sb.append(step.success() ? "✓ Success" : "✗ Failed");
                sb.append(" (").append(step.filesChanged()).append(" files changed)\n");
                if (step.message() != null) {
                    sb.append("    Message: ").append(step.message()).append("\n");
                }
            }
            sb.append("\n");
        }
        
        if (result.testOutcome() != null) {
            sb.append("Test Results:\n");
            sb.append("  Total: ").append(result.testOutcome().total()).append("\n");
            sb.append("  Passed: ").append(result.testOutcome().passed()).append("\n");
            sb.append("  Failed: ").append(result.testOutcome().failed()).append("\n");
            sb.append("  Skipped: ").append(result.testOutcome().skipped()).append("\n");
            sb.append("\n");
        }
        
        if (result.errorMessage() != null) {
            sb.append("Error: ").append(result.errorMessage()).append("\n");
        }
        
        detailArea.setText(sb.toString());
    }

    private String formatInstant(Instant instant) {
        return FORMATTER.format(instant.atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
    }

    public void setOnSequenceLoaded(Consumer<MigrationSequence> callback) {
        this.onSequenceLoaded = callback;
    }

    public void setOnExperimentSelected(Consumer<ExperimentResult> callback) {
        this.onExperimentSelected = callback;
    }

    private void fireExperimentSelected() {
        int selectedRow = historyTable.getSelectedRow();
        if (selectedRow < 0 || onExperimentSelected == null) {
            return;
        }
        ExperimentResult result = tableModel.getExperimentAt(selectedRow);
        onExperimentSelected.accept(result);
    }

    public JPanel getPanel() {
        return panel;
    }

    /**
     * Table model for experiment history
     */
    private static class HistoryTableModel extends AbstractTableModel {
        private List<ExperimentResult> history = new ArrayList<>();
        private final String[] columnNames = {"Date", "Sequence Name", "Status", "Files Modified", "Test Results"};

        public void setHistory(List<ExperimentResult> history) {
            this.history = history;
            fireTableDataChanged();
        }

        public ExperimentResult getExperimentAt(int row) {
            return history.get(row);
        }

        @Override
        public int getRowCount() {
            return history.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            ExperimentResult result = history.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> FORMATTER.format(result.startedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
                case 1 -> result.sequenceName();
                case 2 -> result.status();
                case 3 -> String.valueOf(result.stepResults().stream().mapToInt(s -> s.filesChanged()).sum());
                case 4 -> result.testOutcome() != null 
                    ? String.format("%d/%d passed", result.testOutcome().passed(), result.testOutcome().total())
                    : "N/A";
                default -> "";
            };
        }
    }

    /**
     * Cell renderer for status column with color coding
     */
    private static class StatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, 
                boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            if (value instanceof ExperimentStatus status) {
                setText(status.toString());
                setForeground(switch (status) {
                    case SUCCESS -> new Color(0, 100, 0);
                    case FAILED -> Color.RED;
                    case RUNNING -> new Color(255, 140, 0);
                    default -> Color.BLACK;
                });
            }
            
            return this;
        }
    }
}
