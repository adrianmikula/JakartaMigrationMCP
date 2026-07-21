package adrianmikula.jakartamigration.intellij.ui.refactor;

import adrianmikula.jakartamigration.config.FeatureFlag;
import adrianmikula.jakartamigration.coderefactoring.service.RecipeService;
import adrianmikula.jakartamigration.intellij.config.FeatureFlags;
import adrianmikula.jakartamigration.intellij.license.CheckLicense;
import adrianmikula.jakartamigration.intellij.ui.components.PremiumUpgradeButton;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * Advanced Refactor component - provides multi-recipe sequence testing functionality.
 * This is a premium-only feature that uses testcontainers for safe refactoring experiments.
 */
public class AdvancedRefactorComponent {
    private static final Logger LOG = Logger.getInstance(AdvancedRefactorComponent.class);

    private final JPanel panel;
    private final Project project;
    private final RecipeService recipeService;
    private final boolean isPremium;
    
    // Sub-components
    private JPanel premiumUpgradePanel;
    private SequenceBuilderPanel sequenceBuilderPanel;
    private ExperimentRunnerPanel experimentRunnerPanel;
    private ExperimentHistoryPanel experimentHistoryPanel;

    public AdvancedRefactorComponent(@NotNull Project project, RecipeService recipeService) {
        this.project = project;
        this.recipeService = recipeService;
        this.isPremium = CheckLicense.isLicensed() != null && CheckLicense.isLicensed();
        this.panel = new JBPanel<>(new BorderLayout());
        
        initializeComponent();
    }

    private void initializeComponent() {
        // Check if experiment engine feature is enabled
        // For now, assume it's enabled since it's a premium feature
        // TODO: Add proper feature flag integration when FeatureFlag.getFeatureKey() is available
        boolean experimentEngineEnabled = true;
        
        if (!experimentEngineEnabled) {
            showFeatureDisabledPanel();
            return;
        }
        
        if (!isPremium) {
            showPremiumUpgradePanel();
            return;
        }
        
        // Premium user with feature enabled - show full UI
        showFullUI();
    }

    private void showFeatureDisabledPanel() {
        JPanel disabledPanel = new JBPanel<>(new BorderLayout());
        disabledPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JBLabel titleLabel = new JBLabel("Experimental Feature");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        
        JBLabel messageLabel = new JBLabel(
            "<html><body style='width: 400px'>" +
            "The Advanced Refactor feature is currently experimental and not enabled.<br><br>" +
            "This feature allows you to test refactor sequences safely using containers.<br><br>" +
            "Enable it in the Dev tab to try it out." +
            "</body></html>"
        );
        
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(titleLabel, BorderLayout.NORTH);
        contentPanel.add(Box.createVerticalStrut(10), BorderLayout.CENTER);
        contentPanel.add(messageLabel, BorderLayout.SOUTH);
        
        disabledPanel.add(contentPanel, BorderLayout.CENTER);
        panel.add(disabledPanel, BorderLayout.CENTER);
    }

    private void showPremiumUpgradePanel() {
        premiumUpgradePanel = PremiumUpgradeButton.createConditionalUpgradePanel(project);
        
        JPanel upgradeContainer = new JBPanel<>(new BorderLayout());
        upgradeContainer.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
        
        JBLabel titleLabel = new JBLabel("Advanced Refactor ⭐");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 20f));
        
        JBLabel descriptionLabel = new JBLabel(
            "<html><body style='width: 500px'>" +
            "<b>Test refactor sequences safely using containers</b><br><br>" +
            "• Build sequences of multiple refactor recipes<br>" +
            "• Run experiments in isolated Docker containers<br>" +
            "• Automatic test verification after each run<br>" +
            "• Track experiment history and compare results<br>" +
            "• Load past experiments for re-editing<br><br>" +
            "Upgrade to Premium to unlock this feature." +
            "</body></html>"
        );
        
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(titleLabel, BorderLayout.NORTH);
        contentPanel.add(Box.createVerticalStrut(20), BorderLayout.CENTER);
        contentPanel.add(descriptionLabel, BorderLayout.SOUTH);
        
        upgradeContainer.add(contentPanel, BorderLayout.CENTER);
        upgradeContainer.add(premiumUpgradePanel, BorderLayout.SOUTH);
        
        panel.add(upgradeContainer, BorderLayout.CENTER);
    }

    private void showFullUI() {
        // Create tabbed pane for the three sub-panels
        JTabbedPane subTabs = new JTabbedPane();
        
        // Sequence Builder tab
        sequenceBuilderPanel = new SequenceBuilderPanel(project, recipeService);
        JBScrollPane builderScroll = new JBScrollPane(sequenceBuilderPanel.getPanel());
        builderScroll.setBorder(null);
        subTabs.addTab("Sequence Builder", builderScroll);
        
        // Experiment Runner tab
        experimentRunnerPanel = new ExperimentRunnerPanel(project);
        JBScrollPane runnerScroll = new JBScrollPane(experimentRunnerPanel.getPanel());
        runnerScroll.setBorder(null);
        subTabs.addTab("Run Experiment", runnerScroll);
        
        // Experiment History tab
        experimentHistoryPanel = new ExperimentHistoryPanel(project);
        JBScrollPane historyScroll = new JBScrollPane(experimentHistoryPanel.getPanel());
        historyScroll.setBorder(null);
        subTabs.addTab("History", historyScroll);
        
        // Wire up cross-panel communication
        wirePanels();
        
        panel.add(subTabs, BorderLayout.CENTER);
    }

    private void wirePanels() {
        // When a sequence is saved in builder, refresh runner's sequence selector
        sequenceBuilderPanel.setOnSequenceSaved(() -> {
            if (experimentRunnerPanel != null) {
                experimentRunnerPanel.refreshSequenceList();
            }
        });
        
        // When history loads a sequence, populate builder
        experimentHistoryPanel.setOnSequenceLoaded(sequence -> {
            if (sequenceBuilderPanel != null) {
                sequenceBuilderPanel.loadSequence(sequence);
                // Switch to builder tab
                JTabbedPane subTabs = (JTabbedPane) panel.getComponent(0);
                subTabs.setSelectedIndex(0);
            }
        });
        
        // When experiment runs, refresh history
        experimentRunnerPanel.setOnExperimentCompleted(() -> {
            if (experimentHistoryPanel != null) {
                experimentHistoryPanel.refreshHistory();
            }
        });
    }

    public JPanel getPanel() {
        return panel;
    }
    
    /**
     * Refresh the component (e.g., after license status changes)
     */
    public void refresh() {
        panel.removeAll();
        initializeComponent();
        panel.revalidate();
        panel.repaint();
    }
}
