/*
 * Copyright © 2026 Adrian Mikula
 *
 * All rights reserved.
 *
 * This software is proprietary and may not be used, copied,
 * modified, or distributed except under the terms of a
 * separate commercial license agreement.
 */
package adrianmikula.jakartamigration.intellij;

import adrianmikula.jakartamigration.intellij.mcp.JakartaMcpServerProvider;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Registers the Jakarta Migration MCP server with IntelliJ AI Assistant.
 * 
 * This class implements ProjectActivity to ensure the MCP server is registered
 * when a project is opened while the plugin is enabled. The AI Assistant
 * can then discover and invoke the registered MCP tools.
 * 
 * The MCP server tools are exposed through the JakartaMcpServerProvider class,
 * which provides tool definitions with metadata required by IntelliJ.
 */
public class JakartaMcpRegistrationActivity implements ProjectActivity {

    private static final Logger LOG = Logger.getInstance(JakartaMcpRegistrationActivity.class);
    private static JakartaMcpServerProvider serverProvider;

    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        LOG.info("Initializing Jakarta Migration MCP for project: " + project.getName());

        try {
            // Initialize the MCP server provider
            if (serverProvider == null) {
                serverProvider = new JakartaMcpServerProvider();
                serverProvider.initialize();
            }

            // Log registration details
            LOG.info("Jakarta Migration MCP Server Provider initialized:");
            LOG.info("  - Server ID: " + serverProvider.getServerId());
            LOG.info("  - Server Name: " + serverProvider.getServerName());
            LOG.info("  - Server Version: " + serverProvider.getServerVersion());
            LOG.info("  - Tools Registered: " + serverProvider.getToolCount());
            LOG.info("  - Ready: " + serverProvider.isReady());

            // Log available tools for debugging
            if (serverProvider.isReady()) {
                LOG.info("Available MCP tools:");
                serverProvider.getTools().forEach(tool -> 
                    LOG.info("  - " + tool.getName() + ": " + tool.getDescription().substring(0, Math.min(80, tool.getDescription().length())) + "...")
                );
            }

        } catch (Exception e) {
            LOG.error("Failed to initialize Jakarta Migration MCP: " + e.getMessage(), e);
        }

        return Unit.INSTANCE;
    }

    /**
     * Get the singleton server provider instance.
     * @return The MCP server provider
     */
    public static JakartaMcpServerProvider getServerProvider() {
        return serverProvider;
    }

    /**
     * Check if the MCP server is registered and ready.
     * @return true if MCP is available
     */
    public static boolean isMcpReady() {
        return serverProvider != null && serverProvider.isReady();
    }
}
