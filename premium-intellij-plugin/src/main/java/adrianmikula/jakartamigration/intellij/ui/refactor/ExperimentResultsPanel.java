package adrianmikula.jakartamigration.intellij.ui.refactor;

import adrianmikula.jakartamigration.experiment.domain.ExperimentResult;
import adrianmikula.jakartamigration.experiment.domain.ExperimentStatus;
import adrianmikula.jakartamigration.experiment.domain.MigrationSequence;
import adrianmikula.jakartamigration.intellij.service.ExperimentService;
import adrianmikula.jakartamigration.intellij.util.NotificationHelper;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Panel for displaying experiment results.
 * Shows current/past experiment run status and results with Re-run and Clone actions.
 */
public class ExperimentResultsPanel {
    private static final Logger LOG = Logger.getInstance(ExperimentResultsPanel.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JPanel panel;
    private final Project project;
    private final ExperimentService experimentService;

    private JBLabel statusLabel;
    private JProgressBar progressBar;
    private JBTextArea resultsArea;
    private JButton rerunButton;
    private JButton cloneButton;

    private MigrationSequence currentSequence;
    private ExperimentResult currentResult;

    private Consumer<MigrationSequence> onRerun;
    private Consumer<MigrationSequence> onClone;
    private Consumer<ExperimentResult> onExperimentCompleted;

    public ExperimentResultsPanel(@NotNull Project project, ExperimentService experimentService) {
        this.project = project;
        this.experimentService = experimentService;
        this.panel = new JBPanel<>(new BorderLayout());
        initializeComponent();
    }

    private void initializeComponent() {
        JPanel headerPanel = createHeaderPanel();
        JPanel resultsPanel = createResultsPanel();

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(resultsPanel, BorderLayout.CENTER);

        showEmptyState();
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JBPanel<>(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel statusPanel = new JBPanel<>(new BorderLayout());
        statusLabel = new JBLabel("No experiment selected");
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD, 14f));
        statusPanel.add(statusLabel, BorderLayout.WEST);

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        statusPanel.add(progressBar, BorderLayout.CENTER);

        header.add(statusPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rerunButton = new JButton("Re-run Experiment");
        cloneButton = new JButton("Clone Experiment");

        rerunButton.addActionListener(e -> handleRerun());
        cloneButton.addActionListener(e -> handleClone());

        rerunButton.setEnabled(false);
        cloneButton.setEnabled(false);

        buttonPanel.add(rerunButton);
        buttonPanel.add(cloneButton);

        header.add(buttonPanel, BorderLayout.EAST);

        return header;
    }

    private JPanel createResultsPanel() {
        JPanel results = new JBPanel<>(new BorderLayout());
        results.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY),
            "Experiment Results",
            TitledBorder.LEFT,
            TitledBorder.TOP
        ));

        resultsArea = new JBTextArea();
        resultsArea.setEditable(false);
        resultsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JBScrollPane scrollPane = new JBScrollPane(resultsArea);
        results.add(scrollPane, BorderLayout.CENTER);

        return results;
    }

    private void showEmptyState() {
        statusLabel.setText("No experiment selected");
        statusLabel.setForeground(Color.GRAY);
        resultsArea.setText("Run an experiment from the Sequence Builder, or select one from History.");
        rerunButton.setEnabled(false);
        cloneButton.setEnabled(false);
    }

    public void runExperiment(MigrationSequence sequence, Optional<String> gitRef) {
        this.currentSequence = sequence;
        this.currentResult = null;

        statusLabel.setText("Running experiment: " + sequence.name() + "...");
        statusLabel.setForeground(new Color(255, 140, 0));
        progressBar.setVisible(true);
        rerunButton.setEnabled(false);
        cloneButton.setEnabled(false);

        resultsArea.setText("Starting experiment...\n" +
            "Sequence: " + sequence.name() + "\n" +
            "Steps: " + sequence.steps().size() + "\n" +
            (gitRef.isPresent() ? "Git Ref: " + gitRef.get() + "\n" : "Using current HEAD\n") +
            "\nThis may take several minutes...");

        CompletableFuture.supplyAsync(() -> {
            try {
                return experimentService.runExperiment(sequence.name(), gitRef);
            } catch (Exception e) {
                LOG.error("Experiment execution failed", e);
                
                // Check for Docker-related errors and show notification
                if (isDockerError(e)) {
                    SwingUtilities.invokeLater(() -> {
                        NotificationHelper.showError(
                            project,
                            "Docker Environment Error",
                            "Could not find a valid Docker environment. Please ensure Docker is running and you have the necessary permissions. See https://java.testcontainers.org/on_failure.html for more details."
                        );
                    });
                }
                
                return new ExperimentResult(
                    "failed-" + System.currentTimeMillis(),
                    sequence.name(),
                    Instant.now(),
                    Instant.now(),
                    ExperimentStatus.FAILED,
                    List.of(),
                    null,
                    null,
                    e.getMessage()
                );
            }
        }).thenAccept(result -> SwingUtilities.invokeLater(() -> {
            this.currentResult = result;
            displayResults(result);
            if (result.status() == ExperimentStatus.FAILED && isDockerError(result.errorMessage())) {
                NotificationHelper.showError(
                    project,
                    "Docker Environment Error",
                    "Could not find a valid Docker environment. Please ensure Docker is running and you have the necessary permissions. See https://java.testcontainers.org/on_failure.html for more details."
                );
            }
            if (onExperimentCompleted != null) {
                onExperimentCompleted.accept(result);
            }
        })).exceptionally(throwable -> {
            LOG.error("Unexpected error in experiment UI update", throwable);
            SwingUtilities.invokeLater(() -> {
                progressBar.setVisible(false);
                statusLabel.setText("Experiment failed unexpectedly");
                statusLabel.setForeground(Color.RED);
                resultsArea.setText("An unexpected error occurred: " + throwable.getMessage());
                rerunButton.setEnabled(currentSequence != null);
                cloneButton.setEnabled(currentSequence != null);
            });
            return null;
        });
    }

    public void showResults(ExperimentResult result, MigrationSequence sequence) {
        this.currentResult = result;
        this.currentSequence = sequence;
        displayResults(result);
    }

    private void displayResults(ExperimentResult result) {
        progressBar.setVisible(false);
        rerunButton.setEnabled(currentSequence != null);
        cloneButton.setEnabled(currentSequence != null);

        if (result.status() == ExperimentStatus.SUCCESS) {
            statusLabel.setText("Experiment completed successfully");
            statusLabel.setForeground(new Color(0, 100, 0));
        } else if (result.status() == ExperimentStatus.FAILED) {
            statusLabel.setText("Experiment failed");
            statusLabel.setForeground(Color.RED);
        } else {
            statusLabel.setText("Experiment: " + result.status());
            statusLabel.setForeground(Color.GRAY);
        }

        resultsArea.setText(formatResults(result));
    }

    private String formatResults(ExperimentResult result) {
        StringBuilder sb = new StringBuilder();

        sb.append("Experiment ID: ").append(result.runId()).append("\n");
        sb.append("Sequence: ").append(result.sequenceName()).append("\n");
        sb.append("Status: ").append(result.status()).append("\n");
        sb.append("Started: ").append(FORMATTER.format(result.startedAt().atZone(ZoneOffset.UTC))).append("\n");
        sb.append("Finished: ").append(FORMATTER.format(result.finishedAt().atZone(ZoneOffset.UTC))).append("\n");
        sb.append("Duration: ").append(
            Duration.between(result.startedAt(), result.finishedAt()).toSeconds()
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
                sb.append(step.success() ? "\u2713 Success" : "\u2717 Failed");
                sb.append(" (").append(step.filesChanged()).append(" files changed)\n");
                if (step.message() != null) {
                    sb.append("    Message: ").append(step.message()).append("\n");
                }
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

        return sb.toString();
    }

    private void handleRerun() {
        if (currentSequence != null && onRerun != null) {
            onRerun.accept(currentSequence);
        }
    }

    private void handleClone() {
        if (currentSequence != null && onClone != null) {
            MigrationSequence cloned = new MigrationSequence(
                "Copy of " + currentSequence.name(),
                currentSequence.description(),
                currentSequence.steps(),
                null,
                currentSequence.tags()
            );
            onClone.accept(cloned);
        }
    }

    public void setOnRerun(Consumer<MigrationSequence> callback) {
        this.onRerun = callback;
    }

    public void setOnClone(Consumer<MigrationSequence> callback) {
        this.onClone = callback;
    }

    public void setOnExperimentCompleted(Consumer<ExperimentResult> callback) {
        this.onExperimentCompleted = callback;
    }

    public JPanel getPanel() {
        return panel;
    }

    /**
     * Checks if an exception is related to Docker environment issues.
     */
    private boolean isDockerError(Exception e) {
        return e != null && isDockerError(e.getMessage());
    }

    /**
     * Checks if a message is related to Docker environment issues.
     */
    private boolean isDockerError(String message) {
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase();
        return message.contains("Could not find a valid Docker environment") ||
               lower.contains("docker environment") ||
               (lower.contains("docker") && lower.contains("permission"));
    }
}
