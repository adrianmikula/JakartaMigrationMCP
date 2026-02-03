package adrianmikula.jakartamigration.intellij;

import adrianmikula.jakartamigration.intellij.mcp.AnalyzeMigrationImpactResponse;
import adrianmikula.jakartamigration.intellij.mcp.DefaultMcpClientService;
import adrianmikula.jakartamigration.intellij.mcp.McpClientService;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Main action for Jakarta Migration.
 * Shows a dialog to choose between analysis and refactoring.
 * Connects directly to the MCP server for real operations.
 */
public class JakartaMigrationAction extends AnAction {
    private final McpClientService mcpClient;

    public JakartaMigrationAction() {
        this.mcpClient = new DefaultMcpClientService();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null)
            return;

        // Check for premium features
        boolean isPremium = "true".equals(System.getProperty("jakarta.migration.premium"));

        String serverUrl = mcpClient.getServerUrl();
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
        String projectPath = project.getBasePath();
        if (projectPath == null) {
            projectPath = project.getProjectFilePath();
        }

        if (projectPath == null) {
            Messages.showWarningDialog(project, "Cannot determine project path. Please open a project first.", "Analysis Failed");
            return;
        }

        Messages.showInfoMessage(project, "Analysis started. Results will appear in the Jakarta Migration tool window...", "Analysis Started");

        mcpClient.analyzeMigrationImpact(projectPath)
            .thenAccept(response -> {
                if (response != null && response.getDependencyImpact() != null &&
                    response.getDependencyImpact().getAffectedDependencies() != null) {
                    int depsCount = response.getDependencyImpact().getAffectedDependencies().size();
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
                    "Analysis failed: " + ex.getMessage() + "\n\n" +
                    "Please ensure the MCP server is running at: " + mcpClient.getServerUrl(),
                    "Analysis Failed");
                return null;
            });
    }

    private void performAutoFix(Project project) {
        // Check if MCP server supports auto-fix operations
        mcpClient.isServerAvailable()
            .thenAccept(available -> {
                if (available) {
                    Messages.showInfoMessage(project,
                        "Auto-fix feature would be executed here.\n\n" +
                        "This requires the MCP server to support auto-fix operations.",
                        "Auto-Fix");
                } else {
                    Messages.showWarningDialog(project,
                        "Cannot connect to MCP server at: " + mcpClient.getServerUrl() + "\n\n" +
                        "Please ensure the server is running before using auto-fix.",
                        "Server Unavailable");
                }
            })
            .exceptionally(ex -> {
                Messages.showWarningDialog(project,
                    "Connection error: " + ex.getMessage(),
                    "Connection Failed");
                return null;
            });
    }
}
