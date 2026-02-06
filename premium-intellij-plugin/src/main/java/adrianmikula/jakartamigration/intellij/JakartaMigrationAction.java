package adrianmikula.jakartamigration.intellij;

import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.intellij.service.MigrationAnalysisService;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Main action for Jakarta Migration.
 * Shows a dialog to choose between analysis and refactoring.
 * Connects directly to the migration-core library for real operations.
 */
public class JakartaMigrationAction extends AnAction {
    private final MigrationAnalysisService analysisService;

    public JakartaMigrationAction() {
        this.analysisService = new MigrationAnalysisService();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null)
            return;

        // Check for premium features
        boolean isPremium = "true".equals(System.getProperty("jakarta.migration.premium"));

        String serverUrl = "(direct library call)";
        String message = "Jakarta Migration Tool\n\n" +
                "Detected project: " + project.getName() + "\n" +
                "License: " + (isPremium ? "PREMIUM" : "COMMUNITY") + "\n" +
                "MCP Server: " + serverUrl + "\n\n" +
                "Available actions:";

        String[] options = isPremium
                ? new String[] { "Analyze Readiness", "Apply Auto-Fixes", "Execute Migration Plan", "Close" }
                : new String[] { "Analyze Readiness", "Apply Auto-Fixes (Premium Required)", "Close" };

        int choice = Messages.showDialog(project, message, "Jakarta Migration", options, 0,
                Messages.getInformationIcon());

        if (choice == 0) {
            performAnalysis(project);
        } else if (choice == 1) {
            if (isPremium) {
                performAutoFix(project);
            } else {
                Messages.showWarningDialog(project,
                        "Apply Auto-Fixes requires a PREMIUM license. Please upgrade to unlock this feature.",
                        "Upgrade Required");
            }
        }
    }

    private void performAnalysis(Project project) {
        String projectPathStr = project.getBasePath();
        if (projectPathStr == null) {
            projectPathStr = project.getProjectFilePath();
        }

        if (projectPathStr == null) {
            Messages.showWarningDialog(project, "Cannot determine project path. Please open a project first.", "Analysis Failed");
            return;
        }

        Messages.showInfoMessage(project, "Analysis started. Results will appear in the Jakarta Migration tool window...", "Analysis Started");

        final Path projectPath = Path.of(projectPathStr);

        // Run analysis directly using the migration-core library
        CompletableFuture.supplyAsync(() -> analysisService.analyzeProject(projectPath))
            .thenAccept(report -> {
                if (report != null && report.dependencyGraph() != null) {
                    int depsCount = report.dependencyGraph().getNodes().size();
                    Messages.showInfoMessage(project,
                        "Analysis complete! " + depsCount + " dependencies analyzed.\n\n" +
                        "Open the Jakarta Migration tool window to view results.",
                        "Analysis Complete");
                } else {
                    Messages.showInfoMessage(project,
                        "Analysis complete. No Jakarta migration issues found.\n\n" +
                        "Your project appears to be ready for Jakarta EE migration!",
                        "Analysis Complete");
                }
            })
            .exceptionally(ex -> {
                Messages.showWarningDialog(project,
                    "Analysis failed: " + ex.getMessage(),
                    "Analysis Failed");
                return null;
            });
    }

    private void performAutoFix(Project project) {
        Messages.showInfoMessage(project,
            "Auto-fix feature would be executed here.\n\n" +
            "This feature uses the migration-core library directly.",
            "Auto-Fix");
    }
}
