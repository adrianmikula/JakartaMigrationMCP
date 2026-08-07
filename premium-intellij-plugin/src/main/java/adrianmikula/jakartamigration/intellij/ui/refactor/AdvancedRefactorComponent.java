package adrianmikula.jakartamigration.intellij.ui.refactor;

import adrianmikula.jakartamigration.config.FeatureFlag;
import adrianmikula.jakartamigration.coderefactoring.service.RecipeService;
import adrianmikula.jakartamigration.experiment.domain.MigrationSequence;
import adrianmikula.jakartamigration.intellij.config.FeatureFlags;
import adrianmikula.jakartamigration.intellij.license.CheckLicense;
import adrianmikula.jakartamigration.intellij.service.ExperimentService;
import adrianmikula.jakartamigration.intellij.ui.components.PremiumUpgradeButton;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

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
    private ExperimentResultsPanel experimentResultsPanel;
    private ExperimentHistoryPanel experimentHistoryPanel;
    private JTabbedPane subTabs;
    private ExperimentService experimentService;

    public AdvancedRefactorComponent(@NotNull Project project, RecipeService recipeService) {
        this.project = project;
        this.recipeService = recipeService;
        this.isPremium = CheckLicense.isLicensed() != null && CheckLicense.isLicensed();
        this.panel = new JBPanel<>(new BorderLayout());
        
        initializeComponent();
    }

    private void initializeComponent() {
        boolean experimentEngineEnabled = FeatureFlags.getInstance().isEnabled("experimentEngine");
        
        if (!experimentEngineEnabled) {
            showFeatureDisabledPanel();
            return;
        }
        
        if (!isPremium) {
            showPremiumUpgradePanel();
            return;
        }
        
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
        subTabs = new JTabbedPane();

        Path projectRoot = Path.of(project.getBasePath());
        experimentService = new ExperimentService(projectRoot, recipeService);

        // Sequence Builder tab
        sequenceBuilderPanel = new SequenceBuilderPanel(project, recipeService);
        subTabs.addTab("Sequence Builder", sequenceBuilderPanel.getPanel());

        // Experiment Results tab
        experimentResultsPanel = new ExperimentResultsPanel(project, experimentService);
        subTabs.addTab("Experiment Results", experimentResultsPanel.getPanel());

        // Experiment History tab
        experimentHistoryPanel = new ExperimentHistoryPanel(project, experimentService);
        subTabs.addTab("History", experimentHistoryPanel.getPanel());

        wirePanels();

        panel.add(subTabs, BorderLayout.CENTER);
    }

    private void wirePanels() {
        // When Run Experiment is clicked in builder, run and switch to Results tab
        sequenceBuilderPanel.setOnRunExperiment(() -> {
            MigrationSequence sequence = sequenceBuilderPanel.getCurrentSequence();
            if (sequence == null) {
                JOptionPane.showMessageDialog(panel,
                    "Please name the sequence and add at least one recipe.",
                    "Cannot Run Experiment", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                experimentService.saveSequence(sequence);
            } catch (Exception e) {
                LOG.error("Failed to save sequence before running experiment", e);
                JOptionPane.showMessageDialog(panel,
                    "Failed to save sequence: " + e.getMessage(),
                    "Experiment Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            experimentResultsPanel.runExperiment(sequence, Optional.empty());
            subTabs.setSelectedIndex(1);
        });

        // When a sequence is saved in builder, refresh history
        sequenceBuilderPanel.setOnSequenceSaved(() -> {
            if (experimentHistoryPanel != null) {
                experimentHistoryPanel.refreshHistory();
            }
        });

        // When history item is viewed, show results in Results tab
        experimentHistoryPanel.setOnExperimentSelected(result -> {
            MigrationSequence sequence = new MigrationSequence(
                result.sequenceName(), "", List.of(), null, List.of());
            experimentResultsPanel.showResults(result, sequence);
            subTabs.setSelectedIndex(1);
        });

        // When history loads a sequence, populate builder
        experimentHistoryPanel.setOnSequenceLoaded(sequence -> {
            if (sequenceBuilderPanel != null) {
                sequenceBuilderPanel.loadSequence(sequence);
                subTabs.setSelectedIndex(0);
            }
        });

        // When experiment completes, refresh history
        experimentResultsPanel.setOnExperimentCompleted(result -> {
            ApplicationManager.getApplication().invokeLater(() -> {
                if (experimentHistoryPanel != null) {
                    experimentHistoryPanel.refreshHistory();
                }
            });
        });

        // When Re-run is clicked, run experiment again
        experimentResultsPanel.setOnRerun(sequence -> {
            experimentResultsPanel.runExperiment(sequence, Optional.empty());
        });

        // When Clone is clicked, load cloned sequence in Builder
        experimentResultsPanel.setOnClone(sequence -> {
            sequenceBuilderPanel.loadSequence(sequence);
            subTabs.setSelectedIndex(0);
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
