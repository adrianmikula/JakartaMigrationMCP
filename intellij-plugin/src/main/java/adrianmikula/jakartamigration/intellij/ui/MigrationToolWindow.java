package adrianmikula.jakartamigration.intellij.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * Main migration tool window from TypeSpec: plugin-components.tsp
 */
public class MigrationToolWindow implements ToolWindowFactory {
    public static final String TOOL_WINDOW_ID = "JakartaMigrationToolWindow";

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        MigrationToolWindowContent content = new MigrationToolWindowContent(project);
        Content tabContent = ContentFactory.getInstance().createContent(content.getContentPanel(), "Jakarta Migration", false);
        toolWindow.getContentManager().addContent(tabContent);
    }

    public static class MigrationToolWindowContent {
        private final JPanel contentPanel;
        private final Project project;

        public MigrationToolWindowContent(Project project) {
            this.project = project;
            this.contentPanel = new JPanel(new BorderLayout());
            initializeContent();
        }

        private void initializeContent() {
            JTabbedPane tabbedPane = new JTabbedPane();
            
            // Dashboard tab
            tabbedPane.addTab("Dashboard", new DashboardComponent(project).getPanel());
            
            // Dependencies tab
            tabbedPane.addTab("Dependencies", new DependenciesTableComponent(project).getPanel());
            
            // Dependency Graph tab
            tabbedPane.addTab("Dependency Graph", new DependencyGraphComponent(project).getPanel());
            
            // Migration Phases tab
            tabbedPane.addTab("Migration Phases", new MigrationPhasesComponent(project).getPanel());
            
            contentPanel.add(tabbedPane, BorderLayout.CENTER);
        }

        public JPanel getContentPanel() {
            return contentPanel;
        }
    }
}