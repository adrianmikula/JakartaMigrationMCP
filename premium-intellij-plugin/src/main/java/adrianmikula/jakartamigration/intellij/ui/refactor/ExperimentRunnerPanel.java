package adrianmikula.jakartamigration.intellij.ui.refactor;

import adrianmikula.jakartamigration.experiment.domain.ExperimentResult;
import adrianmikula.jakartamigration.experiment.domain.ExperimentStatus;
import adrianmikula.jakartamigration.experiment.domain.MigrationSequence;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Panel for running refactor experiments using testcontainers.
 */
public class ExperimentRunnerPanel {
    private static final Logger LOG = Logger.getInstance(ExperimentRunnerPanel.class);

    private final JPanel panel;
    private final Project project;
    
    // UI Components
    private JComboBox<String> sequenceSelector;
    private JBTextField gitRefField;
    private JButton runButton;
    private JProgressBar progressBar;
    private JBLabel statusLabel;
    private JBTextArea resultsArea;
    
    // Callbacks
    private Runnable onExperimentCompleted;

    public ExperimentRunnerPanel(@NotNull Project project) {
        this.project = project;
        this.panel = new JBPanel<>(new BorderLayout());
        
        initializeComponent();
    }

    private void initializeComponent() {
        // Configuration panel
        JPanel configPanel = createConfigPanel();
        
        // Results panel
        JPanel resultsPanel = createResultsPanel();
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, configPanel, resultsPanel);
        splitPane.setDividerLocation(200);
        splitPane.setResizeWeight(0.3);
        
        panel.add(splitPane, BorderLayout.CENTER);
        
        // Load sequences
        refreshSequenceList();
    }

    private JPanel createConfigPanel() {
        JPanel panel = new JBPanel<>(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY),
            "Experiment Configuration",
            TitledBorder.LEFT,
            TitledBorder.TOP
        ));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Sequence selector
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.1;
        panel.add(new JBLabel("Sequence:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 0.9;
        sequenceSelector = new JComboBox<>();
        panel.add(sequenceSelector, gbc);
        
        // Git ref (optional)
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.1;
        panel.add(new JBLabel("Git Ref (optional):"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 0.9;
        gitRefField = new JBTextField(30);
        gitRefField.setToolTipText("Leave empty to use current HEAD, or specify a branch/tag/commit");
        panel.add(gitRefField, gbc);
        
        // Run button
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        runButton = new JButton("Run Experiment");
        runButton.addActionListener(e -> runExperiment());
        panel.add(runButton, gbc);
        
        // Progress bar
        gbc.gridy = 3;
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        panel.add(progressBar, gbc);
        
        // Status label
        gbc.gridy = 4;
        statusLabel = new JBLabel("");
        statusLabel.setForeground(Color.GRAY);
        panel.add(statusLabel, gbc);
        
        return panel;
    }

    private JPanel createResultsPanel() {
        JPanel panel = new JBPanel<>(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY),
            "Experiment Results",
            TitledBorder.LEFT,
            TitledBorder.TOP
        ));
        
        resultsArea = new JBTextArea();
        resultsArea.setEditable(false);
        resultsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        
        JBScrollPane scrollPane = new JBScrollPane(resultsArea);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }

    public void refreshSequenceList() {
        sequenceSelector.removeAllItems();
        
        // TODO: Load sequences from SequenceService
        // For now, add placeholder
        sequenceSelector.addItem("Select a sequence...");
        // sequenceService.listSequences().forEach(seq -> sequenceSelector.addItem(seq.name()));
    }

    private void runExperiment() {
        String sequenceName = (String) sequenceSelector.getSelectedItem();
        if (sequenceName == null || sequenceName.equals("Select a sequence...")) {
            Messages.showWarningDialog(project, "Please select a sequence to run.", "No Sequence Selected");
            return;
        }
        
        String gitRef = gitRefField.getText().trim();
        Optional<String> gitRefOpt = gitRef.isEmpty() ? Optional.empty() : Optional.of(gitRef);
        
        int confirm = Messages.showYesNoDialog(project,
                "Run experiment '" + sequenceName + "'?\n\n" +
                "This will create a temporary copy of your project and run the refactor sequence in a container.\n" +
                (gitRefOpt.isPresent() ? "Using git ref: " + gitRefOpt.get() + "\n" : "Using current HEAD\n") +
                "The process may take several minutes.",
                "Run Experiment",
                Messages.getQuestionIcon());
        
        if (confirm == Messages.YES) {
            executeExperiment(sequenceName, gitRefOpt);
        }
    }

    private void executeExperiment(String sequenceName, Optional<String> gitRef) {
        setRunning(true);
        statusLabel.setText("Starting experiment...");
        
        CompletableFuture.supplyAsync(() -> {
            try {
                // TODO: Integrate with ExperimentService
                // ExperimentService experimentService = new ExperimentService(project);
                // return experimentService.runExperiment(sequenceName, projectPath, gitRef);
                
                // Placeholder - simulate execution
                Thread.sleep(2000);
                return new ExperimentResult(
                    "test-run-id",
                    sequenceName,
                    java.time.Instant.now(),
                    java.time.Instant.now(),
                    ExperimentStatus.SUCCESS,
                    List.of(),
                    null,
                    "0 files modified, 0 test failures",
                    null
                );
            } catch (Exception e) {
                LOG.error("Experiment execution failed", e);
                return new ExperimentResult(
                    "failed-run-id",
                    sequenceName,
                    java.time.Instant.now(),
                    java.time.Instant.now(),
                    ExperimentStatus.FAILED,
                    List.of(),
                    null,
                    null,
                    e.getMessage()
                );
            }
        }).thenAccept(result -> {
            SwingUtilities.invokeLater(() -> {
                setRunning(false);
                displayResults(result);
                
                if (result.status() == ExperimentStatus.SUCCESS) {
                    statusLabel.setText("Experiment completed successfully");
                    statusLabel.setForeground(new Color(0, 100, 0));
                } else {
                    statusLabel.setText("Experiment failed");
                    statusLabel.setForeground(Color.RED);
                }
                
                if (onExperimentCompleted != null) {
                    onExperimentCompleted.run();
                }
            });
        });
    }

    private void setRunning(boolean running) {
        runButton.setEnabled(!running);
        progressBar.setVisible(running);
        sequenceSelector.setEnabled(!running);
        gitRefField.setEnabled(!running);
    }

    private void displayResults(ExperimentResult result) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("Experiment ID: ").append(result.runId()).append("\n");
        sb.append("Sequence: ").append(result.sequenceName()).append("\n");
        sb.append("Status: ").append(result.status()).append("\n");
        sb.append("Started: ").append(result.startedAt()).append("\n");
        sb.append("Finished: ").append(result.finishedAt()).append("\n");
        sb.append("Duration: ").append(
            java.time.Duration.between(result.startedAt(), result.finishedAt()).toSeconds()
        ).append(" seconds\n");
        sb.append("\n");
        
        if (result.diffSummary() != null) {
            sb.append("Summary: ").append(result.diffSummary()).append("\n");
            sb.append("\n");
        }
        
        if (!result.stepResults().isEmpty()) {
            sb.append("Step Results:\n");
            for (int i = 0; i < result.stepResults().size(); i++) {
                var step = result.stepResults().get(i);
                sb.append("  Step ").append(i + 1).append(": ");
                sb.append(step.success() ? "✓ Success" : "✗ Failed");
                sb.append(" (").append(step.filesChanged()).append(" files changed)\n");
            }
            sb.append("\n");
        }
        
        if (result.testOutcome() != null) {
            sb.append("Test Results:\n");
            sb.append("  Total: ").append(result.testOutcome().total()).append("\n");
            sb.append("  Passed: ").append(result.testOutcome().passed()).append("\n");
            sb.append("  Failed: ").append(result.testOutcome().failed()).append("\n");
            sb.append("  Skipped: ").append(result.testOutcome().skipped()).append("\n");
            sb.append("\n");
        }
        
        if (result.errorMessage() != null) {
            sb.append("Error: ").append(result.errorMessage()).append("\n");
        }
        
        resultsArea.setText(sb.toString());
    }

    public void setOnExperimentCompleted(Runnable callback) {
        this.onExperimentCompleted = callback;
    }

    public JPanel getPanel() {
        return panel;
    }
}
