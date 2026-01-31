package adrianmikula.jakartamigration.intellij;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

/**
 * Main action for Jakarta Migration.
 * Shows a dialog to choose between analysis and refactoring.
 */
public class JakartaMigrationAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null)
            return;

        // Check for premium features
        boolean isPremium = "true".equals(System.getProperty("jakarta.migration.premium"));

        String message = "Jakarta Migration Tool\n\n" +
                "Detected project: " + project.getName() + "\n" +
                "License: " + (isPremium ? "PREMIUM" : "COMMUNITY") + "\n\n" +
                "Available tools:";

        String[] options = isPremium
                ? new String[] { "Analyze Readiness", "Apply Auto-Fixes", "Execute Migration Plan", "Close" }
                : new String[] { "Analyze Readiness", "Apply Auto-Fixes (Premium Required)", "Close" };

        int choice = Messages.showDialog(project, message, "Jakarta Migration", options, 0,
                Messages.getInformationIcon());

        if (choice == 0) {
            Messages.showInfoMessage(project, "Readiness analysis started in background...", "Analysis");
        } else if (choice == 1) {
            if (isPremium) {
                Messages.showInfoMessage(project, "Auto-fixes being applied...", "Auto-Fix");
            } else {
                Messages.showWarningDialog(project,
                        "Apply Auto-Fixes requires a PREMIUM license. Please upgrade to unlock this feature.",
                        "Upgrade Required");
            }
        }
    }
}
