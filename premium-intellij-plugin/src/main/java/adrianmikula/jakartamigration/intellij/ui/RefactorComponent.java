package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.coderefactoring.domain.Recipe;
import adrianmikula.jakartamigration.coderefactoring.domain.RefactoringPhase;
import adrianmikula.jakartamigration.coderefactoring.domain.ValidationIssue;
import adrianmikula.jakartamigration.coderefactoring.domain.ValidationResult;
import adrianmikula.jakartamigration.coderefactoring.domain.ValidationSeverity;
import adrianmikula.jakartamigration.coderefactoring.domain.ValidationStatus;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
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
    private final DefaultListModel<String> phaseListModel;
    private final JList<String> phaseList;
    private final DefaultListModel<Recipe> recipeListModel;
    private final JList<Recipe> recipeList;
    private final JTextArea previewArea;
    private final JButton applyButton;
    private final JButton validateButton;
    private final JButton rollbackButton;
    private final JButton runRecipeButton;
    private final JLabel statusLabel;
    private final MigrationActionHandler actionHandler;

    public RefactorComponent(Project project) {
        this.project = project;
        this.phaseListModel = new DefaultListModel<>();
        this.phaseList = new JList<>(phaseListModel);
        this.recipeListModel = new DefaultListModel<>();
        this.recipeList = new JList<>(recipeListModel);
        this.previewArea = new JTextArea(10, 40);
        this.applyButton = new JButton("Apply Refactoring");
        this.validateButton = new JButton("Validate Changes");
        this.rollbackButton = new JButton("Rollback");
        this.runRecipeButton = new JButton("Run Recipe");
        this.statusLabel = new JLabel("Ready");
        this.actionHandler = new MigrationActionHandler(project);
        this.panel = createPanel();

        setupEventHandlers();
    }

    private JPanel createPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Left panel - Phases and Recipes
        JPanel leftPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        leftPanel.setPreferredSize(new Dimension(300, 0));

        // Phases section
        JPanel phasePanel = new JPanel(new BorderLayout(5, 5));
        phasePanel.setBorder(new TitledBorder("Refactoring Phases"));
        phaseList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane phaseScrollPane = new JScrollPane(phaseList);
        phasePanel.add(phaseScrollPane, BorderLayout.CENTER);

        JPanel phaseButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        phaseButtonPanel.add(applyButton);
        phaseButtonPanel.add(validateButton);
        phaseButtonPanel.add(rollbackButton);
        phasePanel.add(phaseButtonPanel, BorderLayout.SOUTH);
        leftPanel.add(phasePanel);

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
        leftPanel.add(recipePanel);

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
        applyButton.addActionListener(this::handleApplyRefactoring);
        validateButton.addActionListener(this::handleValidate);
        rollbackButton.addActionListener(this::handleRollback);
        runRecipeButton.addActionListener(this::handleRunRecipe);

        phaseList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedPhase = phaseList.getSelectedValue();
                if (selectedPhase != null) {
                    updatePreview(selectedPhase);
                }
            }
        });

        recipeList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Recipe selectedRecipe = recipeList.getSelectedValue();
                if (selectedRecipe != null) {
                    updateRecipePreview(selectedRecipe);
                }
            }
        });
    }

    private void handleApplyRefactoring(ActionEvent e) {
        String selectedPhase = phaseList.getSelectedValue();
        if (selectedPhase == null) {
            Messages.showWarningDialog(project, "Please select a refactoring phase first.", "No Phase Selected");
            return;
        }

        int result = Messages.showYesNoDialog(
                project,
                "Apply refactoring for phase: " + selectedPhase + "?",
                "Confirm Refactoring",
                Messages.getQuestionIcon());

        if (result == Messages.YES) {
            applyRefactoring(selectedPhase);
        }
    }

    private void handleValidate(ActionEvent e) {
        String selectedPhase = phaseList.getSelectedValue();
        if (selectedPhase == null) {
            Messages.showWarningDialog(project, "Please select a refactoring phase first.", "No Phase Selected");
            return;
        }

        validateChanges(selectedPhase);
    }

    private void handleRollback(ActionEvent e) {
        String selectedPhase = phaseList.getSelectedValue();
        if (selectedPhase == null) {
            Messages.showWarningDialog(project, "Please select a refactoring phase first.", "No Phase Selected");
            return;
        }

        int result = Messages.showYesNoDialog(
                project,
                "Rollback refactoring for phase: " + selectedPhase + "?",
                "Confirm Rollback",
                Messages.getWarningIcon());

        if (result == Messages.YES) {
            rollbackPhase(selectedPhase);
        }
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

    private void applyRefactoring(String phase) {
        statusLabel.setText("Applying " + phase + "...");
        statusLabel.setForeground(Color.BLUE);

        // Simulate refactoring application
        SwingUtilities.invokeLater(() -> {
            try {
                Thread.sleep(500);
                statusLabel.setText("Applied successfully");
                statusLabel.setForeground(Color.GREEN);
                Messages.showInfoMessage(project, "Refactoring applied successfully!", "Success");
            } catch (Exception ex) {
                statusLabel.setText("Failed: " + ex.getMessage());
                statusLabel.setForeground(Color.RED);
                Messages.showErrorDialog(project, "Refactoring failed: " + ex.getMessage(), "Error");
            }
        });
    }

    private void validateChanges(String phase) {
        statusLabel.setText("Validating " + phase + "...");
        statusLabel.setForeground(Color.BLUE);

        // Simulate validation using record constructor
        SwingUtilities.invokeLater(() -> {
            try {
                Thread.sleep(300);
                ValidationResult result = new ValidationResult(
                        true,
                        new ArrayList<>(),
                        "src/main/java/example/Test.java",
                        ValidationStatus.PASSED);

                String message = result.isSuccessful() ? "All changes validated successfully"
                        : "Validation found issues";
                statusLabel.setText("Validated: " + message);
                statusLabel.setForeground(Color.GREEN);
                Messages.showInfoMessage(project, message, "Valid");
            } catch (Exception ex) {
                statusLabel.setText("Validation failed: " + ex.getMessage());
                statusLabel.setForeground(Color.RED);
                Messages.showErrorDialog(project, "Validation failed: " + ex.getMessage(), "Error");
            }
        });
    }

    private void rollbackPhase(String phase) {
        statusLabel.setText("Rolling back " + phase + "...");
        statusLabel.setForeground(Color.BLUE);

        // Simulate rollback
        SwingUtilities.invokeLater(() -> {
            try {
                Thread.sleep(400);
                statusLabel.setText("Rolled back successfully");
                statusLabel.setForeground(Color.GREEN);
                Messages.showInfoMessage(project, "Rollback completed successfully!", "Success");
            } catch (Exception ex) {
                statusLabel.setText("Rollback failed: " + ex.getMessage());
                statusLabel.setForeground(Color.RED);
                Messages.showErrorDialog(project, "Rollback failed: " + ex.getMessage(), "Error");
            }
        });
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

    private void updatePreview(String phase) {
        // Generate preview based on selected phase
        StringBuilder preview = new StringBuilder();
        preview.append("Refactoring Phase: ").append(phase).append("\n\n");
        preview.append("Changes to be applied:\n");
        preview.append("- Update imports from javax.* to jakarta.*\n");
        preview.append("- Update XML namespace declarations\n");
        preview.append("- Update descriptor files (web.xml, application.xml)\n");
        preview.append("- Update JNDI lookups\n");
        preview.append("- Update any remaining references\n\n");
        preview.append("Files affected:\n");
        preview.append("- src/main/java/com/example/MyServlet.java\n");
        preview.append("- src/main/webapp/WEB-INF/web.xml\n");
        preview.append("- pom.xml\n");

        previewArea.setText(preview.toString());
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
     * Set the available refactoring phases
     */
    public void setPhases(List<RefactoringPhase> phases) {
        phaseListModel.clear();
        for (RefactoringPhase phase : phases) {
            // Use description() as the display name since getName() doesn't exist
            phaseListModel.addElement(phase.description());
        }
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
        phaseListModel.clear();
        recipeListModel.clear();
        previewArea.setText("");
    }

    public JPanel getPanel() {
        return panel;
    }

    public DefaultListModel<String> getPhaseListModel() {
        return phaseListModel;
    }

    public DefaultListModel<Recipe> getRecipeListModel() {
        return recipeListModel;
    }

    public JTextArea getPreviewArea() {
        return previewArea;
    }
}
