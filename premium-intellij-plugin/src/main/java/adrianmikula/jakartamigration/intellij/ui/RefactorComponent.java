package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.analysis.persistence.CentralMigrationAnalysisStore;
import adrianmikula.jakartamigration.coderefactoring.domain.Recipe;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Refactor tab component for code refactoring operations.
 * Provides UI for applying automated refactorings and viewing refactoring
 * results.
 */
public class RefactorComponent {
    private static final Logger LOG = Logger.getInstance(RefactorComponent.class);

    private final JPanel panel;
    private final Project project;
    private final CentralMigrationAnalysisStore store;
    private final DefaultListModel<Recipe> recipeListModel;
    private final JList<Recipe> recipeList;
    private final JTextArea previewArea;
    private final JTextArea resultsArea;
    private final JButton runRecipeButton;
    private final JLabel statusLabel;
    private final MigrationActionHandler actionHandler;

    public RefactorComponent(Project project, CentralMigrationAnalysisStore store) {
        this.project = project;
        this.store = store;
        this.recipeListModel = new DefaultListModel<>();
        this.recipeList = new JList<>(recipeListModel);
        this.previewArea = new JTextArea(10, 40);
        this.resultsArea = new JTextArea(10, 40);
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
        previewArea.setLineWrap(true);
        previewArea.setWrapStyleWord(true);
        previewArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane previewScrollPane = new JScrollPane(previewArea);

        // Results section
        JPanel resultsPanel = new JPanel(new BorderLayout(5, 5));
        resultsPanel.setBorder(new TitledBorder("Refactoring Results"));
        resultsArea.setEditable(false);
        resultsArea.setLineWrap(true);
        resultsArea.setWrapStyleWord(true);
        resultsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane resultsScrollPane = new JScrollPane(resultsArea);
        resultsPanel.add(resultsScrollPane, BorderLayout.CENTER);

        // Split right panel
        JSplitPane rightSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, previewScrollPane, resultsPanel);
        rightSplitPane.setResizeWeight(0.5);
        rightPanel.add(rightSplitPane, BorderLayout.CENTER);

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
                    loadRecipeHistory(selectedRecipe);
                }
            }
        });
    }

    private void loadRecipeHistory(Recipe recipe) {
        if (store == null)
            return;

        String repoPath = getRepositoryPath();
        if (repoPath == null) {
            resultsArea.setText("Cannot determine project path. Please ensure a project is open.");
            return;
        }

        List<Map<String, Object>> history = store.getRecipeExecutions(repoPath, recipe.name());
        if (history.isEmpty()) {
            resultsArea.setText("No execution history found for this recipe.\n\nClick 'Run Recipe' to execute a migration recipe.");
            return;
        }

        StringBuilder results = new StringBuilder();
        results.append("Execution History for ").append(recipe.name()).append(":\n\n");

        for (Map<String, Object> entry : history) {
            results.append("Date: ").append(entry.get("executed_at")).append("\n");
            results.append("Status: ").append((boolean) entry.get("success") ? "SUCCESS" : "FAILED").append("\n");
            results.append("Message: ").append(entry.get("message")).append("\n");

            String affectedFiles = (String) entry.get("affected_files");
            if (affectedFiles != null && !affectedFiles.isEmpty()) {
                results.append("Files modified:\n");
                for (String file : affectedFiles.split(",")) {
                    results.append("  - ").append(file).append("\n");
                }
            } else {
                results.append("Files modified: None\n");
            }
            results.append("--------------------------------------------------\n\n");
        }

        resultsArea.setText(results.toString());
        resultsArea.setCaretPosition(0);
    }

    private String getRepositoryPath() {
        String path = project.getBasePath();
        if (path == null || path.isEmpty()) {
            path = project.getProjectFilePath();
        }
        if (path == null || path.isEmpty()) {
            path = project.getName();
        }
        return path;
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

        String repoPath = getRepositoryPath();

        actionHandler.handleRecipeExecution(recipe, (success, message) -> {
            SwingUtilities.invokeLater(() -> {
                if (success) {
                    statusLabel.setText("Recipe executed successfully");
                    statusLabel.setForeground(Color.GREEN);
                    Messages.showInfoMessage(project, message, "Success");

                    // Save execution to DB
                    if (store != null) {
                        store.saveRecipeExecution(repoPath, recipe.name(), true, message,
                                Collections.emptyList());
                        loadRecipeHistory(recipe);
                    }
                } else {
                    statusLabel.setText("Execution failed");
                    statusLabel.setForeground(Color.RED);
                    Messages.showErrorDialog(project, message, "Error");

                    if (store != null) {
                        store.saveRecipeExecution(repoPath, recipe.name(), false, message,
                                Collections.emptyList());
                        loadRecipeHistory(recipe);
                    }
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
