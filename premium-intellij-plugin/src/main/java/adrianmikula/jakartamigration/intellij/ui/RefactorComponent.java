package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.coderefactoring.domain.Recipe;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * Refactor tab component for code refactoring operations.
 * Provides UI for applying automated refactorings and viewing refactoring
 * results.
 */
public class RefactorComponent {
    private static final Logger LOG = Logger.getInstance(RefactorComponent.class);

    private final JPanel panel;
    private final Project project;
    private final DefaultListModel<Recipe> recipeListModel;
    private final JList<Recipe> recipeList;
    private final JTextArea previewArea;
    private final JButton runRecipeButton;
    private final JLabel statusLabel;
    private final MigrationActionHandler actionHandler;

    public RefactorComponent(Project project) {
        this.project = project;
        this.recipeListModel = new DefaultListModel<>();
        this.recipeList = new JList<>(recipeListModel);
        this.previewArea = new JTextArea(10, 40);
        this.runRecipeButton = new JButton("Run Recipe");
        this.statusLabel = new JLabel("Ready");
        this.actionHandler = new MigrationActionHandler(project);
        this.panel = createPanel();

        setupEventHandlers();
    }

    private JPanel createPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Left panel - Recipes
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setPreferredSize(new Dimension(300, 0));

        // Recipes section
        JPanel recipePanel = new JPanel(new BorderLayout(5, 5));
        recipePanel.setBorder(new TitledBorder("Migration Recipes"));
        recipeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Define how recipes are displayed in the list
        recipeList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Recipe) {
                    setText(((Recipe) value).name());
                }
                return this;
            }
        });

        JScrollPane recipeScrollPane = new JScrollPane(recipeList);
        recipePanel.add(recipeScrollPane, BorderLayout.CENTER);

        JPanel recipeButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        recipeButtonPanel.add(runRecipeButton);
        recipePanel.add(recipeButtonPanel, BorderLayout.SOUTH);
        leftPanel.add(recipePanel, BorderLayout.CENTER);

        // Right panel - Preview
        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
        rightPanel.setBorder(new TitledBorder("Details Preview"));

        previewArea.setEditable(false);
        previewArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane previewScrollPane = new JScrollPane(previewArea);
        rightPanel.add(previewScrollPane, BorderLayout.CENTER);

        // Status panel
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(new JLabel("Status:"));
        statusPanel.add(statusLabel);
        rightPanel.add(statusPanel, BorderLayout.SOUTH);

        // Split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setResizeWeight(0.4);
        mainPanel.add(splitPane, BorderLayout.CENTER);

        return mainPanel;
    }

    private void setupEventHandlers() {
        runRecipeButton.addActionListener(this::handleRunRecipe);

        recipeList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Recipe selectedRecipe = recipeList.getSelectedValue();
                if (selectedRecipe != null) {
                    updateRecipePreview(selectedRecipe);
                }
            }
        });
    }

    private void handleRunRecipe(ActionEvent e) {
        Recipe selectedRecipe = recipeList.getSelectedValue();
        if (selectedRecipe == null) {
            Messages.showWarningDialog(project, "Please select a migration recipe first.", "No Recipe Selected");
            return;
        }

        int result = Messages.showYesNoDialog(
                project,
                "Execute OpenRewrite recipe: " + selectedRecipe.name() + "?\n\n" +
                        "Description: " + selectedRecipe.description(),
                "Confirm Recipe Execution",
                Messages.getQuestionIcon());

        if (result == Messages.YES) {
            runRecipe(selectedRecipe);
        }
    }

    private void runRecipe(Recipe recipe) {
        statusLabel.setText("Executing " + recipe.name() + "...");
        statusLabel.setForeground(Color.BLUE);

        actionHandler.handleRecipeExecution(recipe, (success, message) -> {
            SwingUtilities.invokeLater(() -> {
                if (success) {
                    statusLabel.setText("Recipe executed successfully");
                    statusLabel.setForeground(Color.GREEN);
                    Messages.showInfoMessage(project, message, "Success");
                } else {
                    statusLabel.setText("Execution failed");
                    statusLabel.setForeground(Color.RED);
                    Messages.showErrorDialog(project, message, "Error");
                }
            });
        });
    }

    private void updateRecipePreview(Recipe recipe) {
        StringBuilder preview = new StringBuilder();
        preview.append("OpenRewrite Recipe: ").append(recipe.name()).append("\n\n");
        preview.append("Description:\n").append(recipe.description()).append("\n\n");
        preview.append("Pattern:\n").append(recipe.pattern()).append("\n\n");
        preview.append("Safety Level: ").append(recipe.safety()).append("\n");
        preview.append("Reversible: ").append(recipe.reversible() ? "Yes" : "No").append("\n\n");

        preview.append("This recipe will perform automated code transformations using OpenRewrite engine.");

        previewArea.setText(preview.toString());
    }

    /**
     * Set the available migration recipes
     */
    public void setRecipes(List<Recipe> recipes) {
        recipeListModel.clear();
        for (Recipe recipe : recipes) {
            recipeListModel.addElement(recipe);
        }
    }

    /**
     * Clear all phases and recipes
     */
    public void clearAll() {
        recipeListModel.clear();
        previewArea.setText("");
    }

    public JPanel getPanel() {
        return panel;
    }

    public DefaultListModel<Recipe> getRecipeListModel() {
        return recipeListModel;
    }

    public JTextArea getPreviewArea() {
        return previewArea;
    }
}
