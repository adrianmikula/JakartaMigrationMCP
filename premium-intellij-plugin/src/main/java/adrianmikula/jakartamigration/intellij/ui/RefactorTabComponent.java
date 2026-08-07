package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.coderefactoring.service.RecipeService;
import adrianmikula.jakartamigration.intellij.ui.refactor.AdvancedRefactorComponent;
import adrianmikula.jakartamigration.intellij.ui.refactor.BasicRefactorComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * Refactor Tab Component - Main container for refactoring functionality.
 * 
 * This component now contains two sub-tabs:
 * - Basic Refactor: Single-recipe refactoring (available to all users)
 * - Advanced Refactor: Multi-recipe sequence testing (premium feature)
 */
public class RefactorTabComponent {
    private static final Logger LOG = Logger.getInstance(RefactorTabComponent.class);

    private final JPanel panel;
    private final Project project;
    private final RecipeService recipeService;
    
    // Sub-components
    private BasicRefactorComponent basicRefactorComponent;
    private AdvancedRefactorComponent advancedRefactorComponent;
    private JTabbedPane subTabs;

    public RefactorTabComponent(@NotNull Project project, RecipeService recipeService) {
        this.project = project;
        this.recipeService = recipeService;
        this.panel = new JBPanel<>(new BorderLayout());

        initializeComponent();
    }

    public void setOnRecipeExecuted(Runnable callback) {
        if (basicRefactorComponent != null) {
            basicRefactorComponent.setOnRecipeExecuted(callback);
        }
    }

    public void setOnCreditUsed(Runnable callback) {
        if (basicRefactorComponent != null) {
            basicRefactorComponent.setOnCreditUsed(callback);
        }
    }

    private void initializeComponent() {
        subTabs = new JTabbedPane();
        
        // Basic Refactor tab (available to all users)
        basicRefactorComponent = new BasicRefactorComponent(project, recipeService);
        subTabs.addTab("Basic Refactor", basicRefactorComponent.getPanel());
        
        // Advanced Refactor tab (premium feature)
        advancedRefactorComponent = new AdvancedRefactorComponent(project, recipeService);
        boolean isPremium = adrianmikula.jakartamigration.intellij.license.CheckLicense.isLicensed() != null 
            && adrianmikula.jakartamigration.intellij.license.CheckLicense.isLicensed();
        String advancedLabel = isPremium ? "Advanced Refactor ⭐" : "Advanced Refactor 🔒";
        subTabs.addTab(advancedLabel, advancedRefactorComponent.getPanel());
        
        panel.add(subTabs, BorderLayout.CENTER);
    }

    /**
     * Refresh the component (e.g., after license status changes)
     */
    public void refresh() {
        if (advancedRefactorComponent != null) {
            advancedRefactorComponent.refresh();
        }
        
        // Update tab label based on premium status
        boolean isPremium = adrianmikula.jakartamigration.intellij.license.CheckLicense.isLicensed() != null 
            && adrianmikula.jakartamigration.intellij.license.CheckLicense.isLicensed();
        String advancedLabel = isPremium ? "Advanced Refactor ⭐" : "Advanced Refactor 🔒";
        subTabs.setTitleAt(1, advancedLabel);
        
        if (basicRefactorComponent != null) {
            basicRefactorComponent.refreshAllRecipes();
        }
    }

    public JPanel getPanel() {
        return panel;
    }
}
