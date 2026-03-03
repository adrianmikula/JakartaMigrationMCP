package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.coderefactoring.domain.Recipe;
import adrianmikula.jakartamigration.coderefactoring.domain.SafetyLevel;
import adrianmikula.jakartamigration.coderefactoring.domain.RollbackResult;
import adrianmikula.jakartamigration.intellij.service.MigrationAnalysisService;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.table.JBTable;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Refactor Tab Component - Displays available OpenRewrite recipes with Apply buttons.
 */
public class RefactorTabComponent {
    private static final Logger LOG = Logger.getInstance(RefactorTabComponent.class);
    
    private final Project project;
    private final MigrationAnalysisService analysisService;
    private final JPanel panel;
    private final RecipeTableModel tableModel;
    private final JTable table;
    private final JTextArea resultsArea;
    private final JButton undoButton;
    private Recipe selectedRecipe;
    
    // Track undo info per recipe (for future history tab)
    private final Map<String, List<String>> recipeToChangedFiles = new HashMap<>();
    private String lastAppliedRecipeName;
    private List<String> lastChangedFiles;
    
    public RefactorTabComponent(Project project) {
        LOG.info("RefactorTabComponent: Constructor called for project: " + project.getName());
        this.project = project;
        this.analysisService = new MigrationAnalysisService();
        LOG.info("RefactorTabComponent: MigrationAnalysisService created");
        this.lastAppliedRecipeName = null;
        this.lastChangedFiles = new ArrayList<>();
        this.undoButton = new JButton("↩ Undo Last");
        this.undoButton.setEnabled(false);
        this.undoButton.addActionListener(this::handleUndo);
        // Create table model and table BEFORE creating the panel (so scroll pane has a valid table)
        this.tableModel = new RecipeTableModel();
        this.table = createTable();
        this.resultsArea = createResultsArea();
        LOG.info("RefactorTabComponent: Table created");
        this.panel = createPanel();
        LOG.info("RefactorTabComponent: About to load recipes...");
        loadRecipes();
        LOG.info("RefactorTabComponent: Constructor complete");
    }
    
    private JTextArea createResultsArea() {
        JTextArea area = new JTextArea(10, 50);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setEditable(false);
        area.setText("No execution history found for this recipe.\n\nSelect a recipe and click Apply to see results here.");
        area.setBackground(new Color(245, 245, 245));
        return area;
    }
    
    private JPanel createPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Title
        JLabel titleLabel = new JLabel("OpenRewrite Recipes");
        titleLabel.setFont(new Font(titleLabel.getFont().getName(), Font.BOLD, 16));
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        titlePanel.add(titleLabel);
        
        // Description
        JLabel descLabel = new JLabel("Select a recipe below to apply Jakarta migration fixes to your code.");
        descLabel.setForeground(Color.GRAY);
        JPanel descPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        descPanel.add(descLabel);
        
        // Table area
        JScrollPane tableScrollPane = new JScrollPane(table);
        
        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshButton = new JButton("↻ Refresh Recipes");
        refreshButton.addActionListener(this::handleRefresh);
        buttonPanel.add(refreshButton);
        buttonPanel.add(undoButton);
        
        // Info label
        JLabel infoLabel = new JLabel("💡 Tip: Recipes with HIGH safety level are recommended for automated migration.");
        infoLabel.setForeground(new Color(100, 100, 100));
        infoLabel.setFont(infoLabel.getFont().deriveFont(Font.ITALIC));
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        infoPanel.add(infoLabel);
        
// Add components
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(titlePanel);
        topPanel.add(descPanel);
        
        // Results panel
        JPanel resultsPanel = new JPanel(new BorderLayout());
        resultsPanel.setBorder(BorderFactory.createTitledBorder("Refactoring Results"));
        resultsPanel.add(new JScrollPane(resultsArea), BorderLayout.CENTER);
        
        // Split pane for table and results
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setTopComponent(tableScrollPane);
        splitPane.setBottomComponent(resultsPanel);
        splitPane.setDividerLocation(300);
        
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(splitPane, BorderLayout.CENTER);
        
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.add(buttonPanel);
        bottomPanel.add(infoPanel);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        return mainPanel;
    }
    
    private JTable createTable() {
        JTable table = new JTable(tableModel);
        table.setRowHeight(40);
        table.getColumnModel().getColumn(0).setHeaderValue("Recipe Name");
        table.getColumnModel().getColumn(0).setPreferredWidth(150);
        table.getColumnModel().getColumn(1).setHeaderValue("Description");
        table.getColumnModel().getColumn(1).setPreferredWidth(250);
        table.getColumnModel().getColumn(2).setHeaderValue("Safety");
        table.getColumnModel().getColumn(2).setPreferredWidth(80);
        table.getColumnModel().getColumn(3).setHeaderValue("Apply");
        table.getColumnModel().getColumn(3).setPreferredWidth(80);
        table.getColumnModel().getColumn(4).setHeaderValue("Undo");
        table.getColumnModel().getColumn(4).setPreferredWidth(80);
        
        // Set up Apply button column
        table.getColumnModel().getColumn(3).setCellRenderer(new ButtonRenderer("Apply", false));
        table.getColumnModel().getColumn(3).setCellEditor(new ButtonEditor(new JCheckBox(), this::handleApplyRecipe, false));
        
        // Set up Undo button column
        table.getColumnModel().getColumn(4).setCellRenderer(new ButtonRenderer("Undo", true));
        table.getColumnModel().getColumn(4).setCellEditor(new ButtonEditor(new JCheckBox(), this::handleRowUndo, true));
        
        return table;
    }
    
    private void loadRecipes() {
        List<Recipe> recipes = analysisService.getAvailableRecipes();
        LOG.info("loadRecipes: Found " + recipes.size() + " recipes");
        for (Recipe recipe : recipes) {
            LOG.info("loadRecipes: - " + recipe.name() + ": " + recipe.description());
        }
        tableModel.setRecipes(recipes);
    }
    
    private boolean hasUndoForRecipe(String recipeName) {
        List<String> files = recipeToChangedFiles.get(recipeName);
        return files != null && !files.isEmpty();
    }
    
    private void handleRowUndo(int row) {
        Recipe recipe = tableModel.getRecipeAt(row);
        if (recipe == null) return;
        
        List<String> changedFiles = recipeToChangedFiles.get(recipe.name());
        if (changedFiles == null || changedFiles.isEmpty()) {
            Messages.showWarningDialog(project, "No changes to undo for recipe '" + recipe.name() + "'.", "Undo");
            return;
        }
        
        String message = String.format("Undo recipe '%s'?\n\nThis will revert %d file(s) to their previous state.",
            recipe.name(), changedFiles.size());
        
        int confirmResult = Messages.showYesNoDialog(project, message, "Confirm Undo", Messages.getQuestionIcon());
        
        if (confirmResult == Messages.YES) {
            handleUndoForRecipe(recipe.name(), changedFiles);
        }
    }
    
    private void handleUndoForRecipe(String recipeName, List<String> filesToUndo) {
        int undoneCount = 0;
        StringBuilder sb = new StringBuilder();
        
        for (String filePath : filesToUndo) {
            try {
                Path path = Path.of(filePath);
                if (Files.exists(path)) {
                    Path backupPath = path.resolveSibling(path.getFileName() + ".bak");
                    if (Files.exists(backupPath)) {
                        Files.copy(backupPath, path, StandardCopyOption.REPLACE_EXISTING);
                        Files.delete(backupPath);
                        undoneCount++;
                        sb.append("Reverted: ").append(filePath).append("\n");
                    }
                }
            } catch (Exception e) {
                sb.append("Failed to revert: ").append(filePath).append(" - ").append(e.getMessage()).append("\n");
            }
        }
        
        recipeToChangedFiles.remove(recipeName);
        
        if (recipeToChangedFiles.isEmpty()) {
            undoButton.setEnabled(false);
            lastAppliedRecipeName = null;
            lastChangedFiles.clear();
        }
        
        tableModel.fireTableDataChanged();
        
        sb.insert(0, "Undo completed for recipe '" + recipeName + "'.\n\nReverted " + undoneCount + " file(s).\n\n");
        resultsArea.setText(sb.toString());
        
        Messages.showInfoMessage(project, sb.toString(), "Undo Complete");
    }
    
    private void handleRefresh(ActionEvent e) {
        loadRecipes();
        Messages.showInfoMessage(project, "Recipes refreshed.", "Refresh Complete");
    }
    
    private void handleUndo(ActionEvent e) {
        if (lastAppliedRecipeName == null || lastChangedFiles == null || lastChangedFiles.isEmpty()) {
            Messages.showWarningDialog(project, "No changes to undo.", "Undo");
            return;
        }
        
        String message = String.format("Undo recipe '%s'?\n\nThis will revert %d file(s) to their previous state.",
            lastAppliedRecipeName, lastChangedFiles.size());
        
        int confirmResult = Messages.showYesNoDialog(project, message, "Confirm Undo", Messages.getQuestionIcon());
        
        if (confirmResult == Messages.YES) {
            int undoneCount = 0;
            StringBuilder sb = new StringBuilder();
            
            for (String filePath : lastChangedFiles) {
                try {
                    Path path = Path.of(filePath);
                    Path backupPath = Path.of(filePath + ".bak");
                    
                    if (Files.exists(backupPath)) {
                        Files.copy(backupPath, path, StandardCopyOption.REPLACE_EXISTING);
                        Files.delete(backupPath);
                        undoneCount++;
                        sb.append("Restored: ").append(filePath).append("\n");
                    } else {
                        sb.append("Warning: No backup found for: ").append(filePath).append("\n");
                    }
                } catch (Exception ex) {
                    LOG.error("Failed to undo file: " + filePath, ex);
                    sb.append("Error: ").append(filePath).append(" - ").append(ex.getMessage()).append("\n");
                }
            }
            
            // Clear the undo state
            undoButton.setEnabled(false);
            lastAppliedRecipeName = null;
            lastChangedFiles.clear();
            
            // Update results area
            StringBuilder results = new StringBuilder();
            results.append("═══════════════════════════════════════════════════════════\n");
            results.append("UNDO RESULTS\n");
            results.append("═══════════════════════════════════════════════════════════\n\n");
            results.append("Recipe: ").append(lastAppliedRecipeName != null ? lastAppliedRecipeName : "Unknown").append("\n");
            results.append("Files Undone: ").append(undoneCount).append("\n\n");
            results.append(sb);
            results.append("\n═══════════════════════════════════════════════════════════\n");
            resultsArea.setText(results.toString());
            
            Messages.showInfoMessage(project, 
                "Undo complete. " + undoneCount + " file(s) restored.", "Undo Complete");
        }
    }
    
private void handleApplyRecipe(int row) {
        Recipe recipe = tableModel.getRecipeAt(row);
        if (recipe == null) return;
        
        this.selectedRecipe = recipe;
        
        String message = String.format("Apply recipe '%s'?\n\n%s\n\nThis will modify source files in your project.", 
            recipe.name(), recipe.description());
        
        int dialogResult = Messages.showYesNoDialog(project, message, "Confirm Refactor", Messages.getQuestionIcon());
        
        if (dialogResult == Messages.YES) {
            // Get project base path
            Path projectPath = project.getBasePath() != null ? Path.of(project.getBasePath()) : null;
            
            if (projectPath == null) {
                Messages.showErrorDialog(project, "Unable to determine project path.", "Error");
                return;
            }
            
            // Apply the recipe via the analysis service
            MigrationAnalysisService.RecipeApplicationResult result = analysisService.applyRecipe(recipe.name(), projectPath);
            
            if (result.success()) {
                // Track the applied recipe for undo functionality
                this.lastAppliedRecipeName = recipe.name();
                this.lastChangedFiles = result.changedFilePaths() != null ? new ArrayList<>(result.changedFilePaths()) : new ArrayList<>();
                this.undoButton.setEnabled(!this.lastChangedFiles.isEmpty());
                
                // Track per-recipe for undo buttons in table
                if (result.changedFilePaths() != null && !result.changedFilePaths().isEmpty()) {
                    recipeToChangedFiles.put(recipe.name(), new ArrayList<>(result.changedFilePaths()));
                }
                
                // Refresh table to update undo button states
                tableModel.fireTableDataChanged();
                
                // Update results area with recipe details and file changes
                StringBuilder sb = new StringBuilder();
                sb.append("═══════════════════════════════════════════════════════════\n");
                sb.append("REFACTORING RESULTS\n");
                sb.append("═══════════════════════════════════════════════════════════\n\n");
                sb.append("Recipe: ").append(recipe.name()).append("\n");
                sb.append("Description: ").append(recipe.description()).append("\n");
                sb.append("Safety Level: ").append(recipe.safety()).append("\n\n");
                sb.append("Status: SUCCESS\n\n");
                sb.append("Files Processed: ").append(result.filesProcessed()).append("\n");
                sb.append("Files Changed: ").append(result.filesChanged()).append("\n\n");
                
                if (result.filesChanged() > 0) {
                    sb.append("Changed Files:\n");
                    for (String filePath : result.changedFilePaths()) {
                        sb.append("  - ").append(filePath).append("\n");
                    }
                    sb.append("\nUse the 'Undo Last' button to revert these changes.\n");
                } else {
                    sb.append("No files required changes.\n");
                }
                
                sb.append("\nPlease review the changes.\n");
                sb.append("═══════════════════════════════════════════════════════════\n");
                resultsArea.setText(sb.toString());
                
                Messages.showInfoMessage(project, 
                    "Recipe '" + recipe.name() + "' applied successfully!\n\nFiles processed: " + result.filesProcessed() + "\nFiles changed: " + result.filesChanged(), 
                    "Refactor Complete");
            } else {
                // Update results area with error
                StringBuilder sb = new StringBuilder();
                sb.append("═══════════════════════════════════════════════════════════\n");
                sb.append("REFACTORING RESULTS\n");
                sb.append("═══════════════════════════════════════════════════════════\n\n");
                sb.append("Recipe: ").append(recipe.name()).append("\n");
                sb.append("Description: ").append(recipe.description()).append("\n");
                sb.append("Safety Level: ").append(recipe.safety()).append("\n\n");
                sb.append("Status: FAILED\n\n");
                sb.append("Error: ").append(result.errorMessage()).append("\n");
                sb.append("═══════════════════════════════════════════════════════════\n");
                resultsArea.setText(sb.toString());
                
                Messages.showErrorDialog(project, 
                    "Failed to apply recipe '" + recipe.name() + "'.\n\nError: " + result.errorMessage(), 
                    "Refactor Failed");
            }
        }
    }
    
    public JPanel getPanel() {
        return panel;
    }
    
    // Table Model
    private static class RecipeTableModel extends AbstractTableModel {
        private final List<Recipe> recipes = new ArrayList<>();
        
        public void setRecipes(List<Recipe> newRecipes) {
            recipes.clear();
            recipes.addAll(newRecipes);
            fireTableDataChanged();
        }
        
        public Recipe getRecipeAt(int row) {
            return row >= 0 && row < recipes.size() ? recipes.get(row) : null;
        }
        
        @Override
        public int getRowCount() {
            return recipes.size();
        }
        
        @Override
        public int getColumnCount() {
            return 5;
        }
        
        @Override
        public Object getValueAt(int row, int column) {
            Recipe recipe = recipes.get(row);
            return switch (column) {
                case 0 -> recipe.name();
                case 1 -> recipe.description();
                case 2 -> recipe.safety().toString();
                case 3 -> "Apply";
                case 4 -> "Undo";
                default -> "";
            };
        }
        
        @Override
        public boolean isCellEditable(int row, int column) {
            return column >= 3; // Button columns are editable
        }
    }
    
    // Button Renderer - now takes button text and enabled state
    private class ButtonRenderer extends JButton implements TableCellRenderer {
        private final String buttonText;
        private final boolean checkUndoState;
        
        public ButtonRenderer(String text, boolean checkUndo) {
            this.buttonText = text;
            this.checkUndoState = checkUndo;
            setOpaque(true);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setText(buttonText);
            
            // For undo column, check if there's undo state for this recipe
            if (checkUndoState && buttonText.equals("Undo")) {
                Recipe recipe = tableModel.getRecipeAt(row);
                boolean hasUndo = recipe != null && hasUndoForRecipe(recipe.name());
                setEnabled(hasUndo);
            } else {
                setEnabled(true);
            }
            
            return this;
        }
    }
    
    // Button Editor
    private class ButtonEditor extends DefaultCellEditor {
        private final JButton button;
        private int selectedRow;
        private final Consumer<Integer> callback;
        private final String buttonText;
        private final boolean isUndoColumn;
        
        public ButtonEditor(JCheckBox checkBox, Consumer<Integer> callback, boolean isUndo) {
            super(checkBox);
            this.callback = callback;
            this.isUndoColumn = isUndo;
            this.buttonText = isUndo ? "Undo" : "Apply";
            button = new JButton(buttonText);
            button.setOpaque(true);
            button.addActionListener(e -> {
                fireEditingStopped();
                if (callback != null) {
                    callback.accept(selectedRow);
                }
            });
        }
        
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int selectedRow) {
            this.selectedRow = selectedRow;
            
            // For undo column, check if there's undo state for this recipe
            if (isUndoColumn) {
                Recipe recipe = tableModel.getRecipeAt(selectedRow);
                boolean hasUndo = recipe != null && hasUndoForRecipe(recipe.name());
                button.setEnabled(hasUndo);
            } else {
                button.setEnabled(true);
            }
            
            return button;
        }
        
        @Override
        public Object getCellEditorValue() {
            return buttonText;
        }
    }
}
