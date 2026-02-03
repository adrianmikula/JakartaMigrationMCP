package adrianmikula.jakartamigration.intellij;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Registers the Jakarta Migration MCP server with IntelliJ AI Assistant.
 */
public class JakartaMcpRegistrationActivity implements ProjectActivity {

    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        // Log initialization
        System.out.println("Initializing Jakarta Migration MCP for project: " + project.getName());

        // In a real implementation, we would:
        // 1. Locate the MCP server JAR (likely bundled with the plugin)
        // 2. Register it with the AI Assistant's McpServerManager

        // For demo purposes, we will assume the AI Assistant can discover servers
        // provided by plugins via the McpServerProvider extension point.

        return Unit.INSTANCE;
    }
}
