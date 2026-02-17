package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.coderefactoring.domain.Recipe;
import adrianmikula.jakartamigration.coderefactoring.domain.SafetyLevel;
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
import java.util.ArrayList;
import java.util.List;

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
    
    public RefactorTabComponent(Project project) {
        this.project = project;
        this.analysisService = new MigrationAnalysisService();
        this.panel = createPanel();
        this.tableModel = new RecipeTableModel();
        this.table = createTable();
        loadRecipes();
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
        
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(tableScrollPane, BorderLayout.CENTER);
        
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
        table.getColumnModel().getColumn(1).setPreferredWidth(300);
        table.getColumnModel().getColumn(2).setHeaderValue("Safety");
        table.getColumnModel().getColumn(2).setPreferredWidth(80);
        table.getColumnModel().getColumn(3).setHeaderValue("Action");
        table.getColumnModel().getColumn(3).setPreferredWidth(100);
        
        // Set up button column renderer
        table.getColumnModel().getColumn(3).setCellRenderer(new ButtonRenderer());
        table.getColumnModel().getColumn(3).setCellEditor(new ButtonEditor(new JCheckBox()));
        
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
    
    private void handleRefresh(ActionEvent e) {
        loadRecipes();
        Messages.showInfoMessage(project, "Recipes refreshed.", "Refresh Complete");
    }
    
    private void handleApplyRecipe(int row) {
        Recipe recipe = tableModel.getRecipeAt(row);
        if (recipe == null) return;
        
        String message = String.format("Apply recipe '%s'?\n\n%s\n\nThis will modify source files in your project.", 
            recipe.name(), recipe.description());
        
        int result = Messages.showYesNoDialog(project, message, "Confirm Refactor", Messages.getQuestionIcon());
        
        if (result == Messages.YES) {
            // TODO: Trigger refactoring via CodeRefactoringModule
            Messages.showInfoMessage(project, 
                "Refactoring triggered for: " + recipe.name() + "\n\n(This feature requires Premium license)", 
                "Refactor");
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
            return 4;
        }
        
        @Override
        public Object getValueAt(int row, int column) {
            Recipe recipe = recipes.get(row);
            return switch (column) {
                case 0 -> recipe.name();
                case 1 -> recipe.description();
                case 2 -> recipe.safety().toString();
                case 3 -> "Apply";
                default -> "";
            };
        }
        
        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 3; // Only button column is editable
        }
    }
    
    // Button Renderer
    private static class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setText("Apply");
            setEnabled(true);
            return this;
        }
    }
    
    // Button Editor
    private static class ButtonEditor extends DefaultCellEditor {
        private final JButton button;
        private int selectedRow;
        
        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton("Apply");
            button.setOpaque(true);
            button.addActionListener(e -> {
                fireEditingStopped();
                // This would trigger the apply action - handled by parent
            });
        }
        
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            selectedRow = row;
            return button;
        }
        
        @Override
        public Object getCellEditorValue() {
            return "Apply";
        }
    }
}
