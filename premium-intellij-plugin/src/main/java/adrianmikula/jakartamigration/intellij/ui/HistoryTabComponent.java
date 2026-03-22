package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.coderefactoring.domain.RecipeExecutionHistory;
import adrianmikula.jakartamigration.coderefactoring.service.RecipeService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Tab component for viewing recipe execution history and performing undos (Premium).
 * Req: History rows are refreshed automatically after a recipe is run from the refactor tab.
 * Req: Action column shows "Apply" for apply actions, not "Undo".
 * Req: Write-safe context used for all UI updates (ApplicationManager.invokeLater).
 */
public class HistoryTabComponent {
    private static final Logger LOG = Logger.getInstance(HistoryTabComponent.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final JPanel panel;
    private final Project project;
    private final RecipeService recipeService;
    private JBTable historyTable;
    private DefaultTableModel tableModel;

    public HistoryTabComponent(@NotNull Project project, RecipeService recipeService) {
        this.project = project;
        this.recipeService = recipeService;
        this.panel = new JBPanel<>(new BorderLayout());
        initializeComponent();
    }

    private void initializeComponent() {
        String[] columnNames = { "ID", "Recipe", "Status", "Date Applied", "Files Changed", "Action" };
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        historyTable = new JBTable(tableModel);
        historyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyTable.getTableHeader().setReorderingAllowed(false);

        JBScrollPane scrollPane = new JBScrollPane(historyTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refreshButton = new JButton("↻ Refresh History");
        refreshButton.addActionListener(e -> refreshHistory());
        actionsPanel.add(refreshButton);

        JButton undoButton = new JButton("↶ Undo Selected");
        undoButton.addActionListener(e -> handleUndo());
        actionsPanel.add(undoButton);

        panel.add(actionsPanel, BorderLayout.SOUTH);

        refreshHistory();
    }

    public void refreshHistory() {
        if (recipeService == null) {
            LOG.warn("RecipeService is null, cannot load history");
            return;
        }

        Path projectPath = Paths.get(project.getBasePath());
        List<RecipeExecutionHistory> history = recipeService.getHistory(projectPath);
        LOG.info("Loading " + history.size() + " history records");

        tableModel.setRowCount(0);
        for (RecipeExecutionHistory h : history) {
            // Bug fix: Action column shows "Applied" for apply actions, "Undo" for undo actions
            String action;
            if (h.isUndo()) {
                action = "Undo";
            } else if (h.isSuccess()) {
                action = "Applied";
            } else {
                action = "-";
            }

            tableModel.addRow(new Object[] {
                    h.getId(),
                    h.getRecipeName(),
                    h.isSuccess() ? (h.isUndo() ? "Undone" : "Success") : "Failed",
                    DATE_FORMATTER.format(h.getExecutedAt()),
                    h.getAffectedFiles().size(),
                    action
            });
        }
    }

    private void handleUndo() {
        int selectedRow = historyTable.getSelectedRow();
        if (selectedRow == -1) {
            // Use ApplicationManager.invokeLater for write-safe context
            ApplicationManager.getApplication().invokeLater(() ->
                Messages.showWarningDialog(project, "Please select an execution to undo.", "No Selection"));
            return;
        }

        Long executionId = (Long) tableModel.getValueAt(selectedRow, 0);
        String recipeName = (String) tableModel.getValueAt(selectedRow, 1);
        String status = (String) tableModel.getValueAt(selectedRow, 2);

        if ("Undone".equals(status)) {
            ApplicationManager.getApplication().invokeLater(() ->
                Messages.showErrorDialog(project, "This action has already been undone.", "Already Undone"));
            return;
        }

        if ("Failed".equals(status)) {
            ApplicationManager.getApplication().invokeLater(() ->
                Messages.showErrorDialog(project, "Cannot undo a failed execution.", "Cannot Undo"));
            return;
        }

        int confirm = Messages.showYesNoDialog(project,
                "Undo changes made by '" + recipeName + "' (ID: " + executionId + ")?\n\n" +
                        "This will restore files to their previous state.",
                "Confirm Undo",
                Messages.getQuestionIcon());

        if (confirm == Messages.YES) {
            LOG.info("Undoing execution: " + executionId);
            Path projectPath = Paths.get(project.getBasePath());

            CompletableFuture.supplyAsync(() -> recipeService.undoRecipe(executionId, projectPath))
                    .thenAccept(result -> {
                        // Use ApplicationManager.invokeLater for write-safe context
                        ApplicationManager.getApplication().invokeLater(() -> {
                            if (result.success()) {
                                Messages.showInfoMessage(project,
                                        "Successfully undone '" + recipeName + "'.\n" +
                                                "Restored " + result.filesChanged() + " files.",
                                        "Undo Successful");
                                refreshHistory();
                            } else {
                                Messages.showErrorDialog(project,
                                        "Failed to undo: " + result.errorMessage(),
                                        "Undo Failed");
                            }
                        });
                    });
        }
    }

    public JPanel getPanel() {
        return panel;
    }
}
