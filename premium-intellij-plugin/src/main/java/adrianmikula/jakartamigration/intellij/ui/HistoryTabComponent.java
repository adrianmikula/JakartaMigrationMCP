package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.coderefactoring.domain.RecipeExecutionHistory;
import adrianmikula.jakartamigration.coderefactoring.service.RecipeService;
import adrianmikula.jakartamigration.credits.CreditType;
import adrianmikula.jakartamigration.credits.CreditsService;
import adrianmikula.jakartamigration.intellij.license.CheckLicense;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.Desktop;
import java.net.URI;
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
    private final CreditsService creditsService;
    private JBTable historyTable;
    private DefaultTableModel tableModel;
    private JButton undoButton;

    // Callback to notify UI to refresh credits display
    private Runnable onCreditUsed;

    public HistoryTabComponent(@NotNull Project project, RecipeService recipeService) {
        this.project = project;
        this.recipeService = recipeService;
        this.creditsService = new CreditsService();
        this.panel = new JBPanel<>(new BorderLayout());
        initializeComponent();
    }

    public void setOnCreditUsed(Runnable callback) {
        this.onCreditUsed = callback;
    }

    private void notifyCreditUsed() {
        if (onCreditUsed != null) {
            onCreditUsed.run();
        }
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
        
        // Apply custom cell renderer for color coding
        HistoryTableCellRenderer renderer = new HistoryTableCellRenderer();
        for (int i = 0; i < historyTable.getColumnCount(); i++) {
            historyTable.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        JBScrollPane scrollPane = new JBScrollPane(historyTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton undoButton = new JButton("↶ Undo Selected");
        undoButton.addActionListener(e -> handleUndo());
        this.undoButton = undoButton;
        actionsPanel.add(undoButton);

        panel.add(actionsPanel, BorderLayout.SOUTH);

        // Add selection listener to enable/disable undo button based on selection
        historyTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateUndoButtonState();
            }
        });

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
        
        updateUndoButtonState();
    }

    /**
     * Updates the enabled state of the undo button based on the current selection.
     * Undo button should be disabled when:
     * - No row is selected
     * - Selected row is a failed execution
     * - Selected row is already an undo action
     * Undo button should be enabled when:
     * - Selected row is a successful "Applied" action
     */
    private void updateUndoButtonState() {
        int selectedRow = historyTable.getSelectedRow();
        boolean canUndo = false;

        if (selectedRow != -1 && tableModel.getRowCount() > 0) {
            String status = (String) tableModel.getValueAt(selectedRow, 2);
            String action = (String) tableModel.getValueAt(selectedRow, 5);
            
            // Enable undo only for successful "Applied" actions
            canUndo = "Success".equals(status) && "Applied".equals(action);
        }

        undoButton.setEnabled(canUndo);
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

        // Check credits for free users (undo consumes 1 action credit)
        boolean isPremium = CheckLicense.isLicensed() != null && CheckLicense.isLicensed();
        if (!isPremium) {
            if (!creditsService.hasCredits(CreditType.ACTIONS)) {
                int result = Messages.showYesNoDialog(project,
                        "You've used all your action credits.\n\n" +
                        "Undo operations require credits. Upgrade to Premium for unlimited undo.",
                        "Credits Exhausted",
                        "Upgrade to Premium",
                        "Cancel",
                        Messages.getWarningIcon());
                if (result == Messages.YES) {
                    openMarketplace();
                }
                return;
            }
        }

        int confirm = Messages.showYesNoDialog(project,
                "Undo changes made by '" + recipeName + "' (ID: " + executionId + ")?\n\n" +
                        "This will restore files to their previous state.",
                "Confirm Undo",
                Messages.getQuestionIcon());

        if (confirm == Messages.YES) {
            // Use credit if free tier
            if (!isPremium) {
                boolean creditUsed = creditsService.useCredit(CreditType.ACTIONS);
                if (creditUsed) {
                    LOG.info("Credit deducted for undo in History tab. Remaining: " +
                        creditsService.getRemainingCredits(CreditType.ACTIONS));
                    notifyCreditUsed(); // Refresh UI
                } else {
                    LOG.warn("Failed to deduct credit for undo in History tab");
                }
            }
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

    /**
     * Opens the JetBrains Marketplace for upgrade.
     */
    private void openMarketplace() {
        try {
            Desktop.getDesktop().browse(new URI("https://plugins.jetbrains.com/plugin/30093-jakarta-migration"));
        } catch (Exception ex) {
            LOG.warn("Failed to open marketplace URL", ex);
            Messages.showInfoMessage(
                project,
                "Please visit: https://plugins.jetbrains.com/plugin/30093-jakarta-migration",
                "Upgrade to Premium"
            );
        }
    }
}

/**
 * Custom cell renderer for the history table to add color coding:
 * - Green for successful actions
 * - Red for failed actions  
 * - Yellow for undo actions
 */
class HistoryTableCellRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        
        if (!isSelected) {
            // Get the status from column 2 (Status column)
            String status = (String) table.getValueAt(row, 2);
            String action = (String) table.getValueAt(row, 5); // Action column
            
            if ("Failed".equals(status)) {
                // Red for failed actions
                component.setForeground(new Color(220, 53, 69)); // Bootstrap danger red
                component.setBackground(new Color(248, 215, 218)); // Light red background
            } else if ("Undo".equals(action)) {
                // Yellow for undo actions
                component.setForeground(new Color(255, 193, 7)); // Bootstrap warning yellow
                component.setBackground(new Color(255, 248, 225)); // Light yellow background
            } else if ("Success".equals(status)) {
                // Green for successful actions
                component.setForeground(new Color(25, 135, 84)); // Bootstrap success green
                component.setBackground(new Color(223, 240, 216)); // Light green background
            } else if ("Undone".equals(status)) {
                // Light gray for undone actions
                component.setForeground(new Color(108, 117, 125)); // Gray
                component.setBackground(new Color(248, 249, 250)); // Light gray background
            } else {
                // Default colors
                component.setForeground(table.getForeground());
                component.setBackground(table.getBackground());
            }
        }
        
        return component;
    }
}
