package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;

import adrianmikula.jakartamigration.credits.CreditType;
import adrianmikula.jakartamigration.credits.CreditsService;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Reusable component for displaying refactoring recipes for a selected dependency.
 * Extracted from DependenciesTableComponent for reuse in tree view.
 */
public class RecipesPanelComponent {
    private final JPanel panel;
    private final Project project;
    private JLabel selectedDependencyLabel;
    private JList<String> recipeList;
    private DefaultListModel<String> recipeListModel;
    private JButton applyRecipeButton;
    private final CreditsService creditsService;
    private JLabel premiumHint;
    private boolean isPremiumUser = false;

    public RecipesPanelComponent(Project project) {
        this.project = project;
        this.creditsService = new CreditsService();
        this.panel = new JBPanel<>(new BorderLayout());
        initializeComponent();
    }

    private void initializeComponent() {
        panel.setBorder(new TitledBorder("Refactoring Recipes"));
        panel.setPreferredSize(new Dimension(0, 150));
        
        selectedDependencyLabel = new JLabel("Select a dependency to see available recipes");
        selectedDependencyLabel.setBorder(new EmptyBorder(5, 5, 5, 5));
        panel.add(selectedDependencyLabel, BorderLayout.NORTH);
        
        recipeListModel = new DefaultListModel<>();
        recipeList = new JList<>(recipeListModel);
        recipeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        JBScrollPane recipeScrollPane = new JBScrollPane(recipeList);
        panel.add(recipeScrollPane, BorderLayout.CENTER);
        
        JPanel recipeActionsPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT));
        applyRecipeButton = new JButton("Apply Recipe");
        applyRecipeButton.setEnabled(false);
        applyRecipeButton.addActionListener(this::handleApplyRecipe);
        recipeActionsPanel.add(applyRecipeButton);
        
        premiumHint = new JLabel(getHintText());
        premiumHint.setForeground(Color.GRAY);
        recipeActionsPanel.add(premiumHint);
        
        panel.add(recipeActionsPanel, BorderLayout.SOUTH);
        
        // Initially hide the recipes panel
        panel.setVisible(false);
    }

    /**
     * Update the recipes panel with information for the selected dependency.
     */
    public void updateForDependency(DependencyInfo dep) {
        if (dep == null) {
            panel.setVisible(false);
            return;
        }
        
        // Show the recipes panel
        panel.setVisible(true);
        
        // Update the label
        String recommendedCoords = dep.getRecommendedArtifactCoordinates();
        if (recommendedCoords != null && !recommendedCoords.isEmpty() && !recommendedCoords.equals("-")) {
            selectedDependencyLabel.setText("Selected: " + dep.getDisplayName() + " → " + recommendedCoords);
        } else {
            selectedDependencyLabel.setText("Selected: " + dep.getDisplayName());
        }
        
        // Update recipe list
        recipeListModel.clear();
        
        String associatedRecipe = dep.getAssociatedRecipeName();
        if (associatedRecipe != null && !associatedRecipe.isEmpty()) {
            recipeListModel.addElement(associatedRecipe);
            recipeList.setSelectedIndex(0);
            applyRecipeButton.setEnabled(true);
        } else if (dep.getRecommendedVersion() != null && !dep.getRecommendedVersion().equals("-")) {
            // Add generic upgrade recipe if there's a recommended version
            String genericRecipe = "Upgrade to Jakarta: " + dep.getRecommendedArtifactCoordinates();
            recipeListModel.addElement(genericRecipe);
            recipeList.setSelectedIndex(0);
            applyRecipeButton.setEnabled(true);
        } else {
            recipeListModel.addElement("No recipes available");
            applyRecipeButton.setEnabled(false);
        }
    }

    /**
     * Hide the recipes panel.
     */
    public void hide() {
        panel.setVisible(false);
    }

    private void handleApplyRecipe(ActionEvent e) {
        String selectedRecipe = recipeList.getSelectedValue();
        
        if (selectedRecipe == null || selectedRecipe.contains("No recipes")) {
            return;
        }
        
        if (!isPremiumUser) {
            if (!creditsService.hasCredits(CreditType.ACTIONS)) {
                Messages.showWarningDialog(project,
                        "No free action credits left. Upgrade to Premium for unlimited access.",
                        "Credits Exhausted");
                return;
            }
            boolean creditUsed = creditsService.useCredit(CreditType.ACTIONS, "Dependencies", "apply_recipe");
            if (!creditUsed) {
                Messages.showWarningDialog(project,
                        "Failed to use action credit. Please try again.",
                        "Credit Error");
                return;
            }
        }

        // Note: This is a placeholder - actual recipe application would be handled by the caller
        Messages.showInfoMessage(project,
                "Recipe application would be triggered here.\nRecipe: " + selectedRecipe,
                "Recipe Application");
    }

    public void setPremiumUser(boolean isPremium) {
        this.isPremiumUser = isPremium;
        if (premiumHint != null) {
            premiumHint.setText(getHintText());
        }
    }

    private String getHintText() {
        return isPremiumUser ? "Premium feature" : "1 action credit";
    }

    public JPanel getPanel() {
        return panel;
    }

    public JButton getApplyRecipeButton() {
        return applyRecipeButton;
    }
}
