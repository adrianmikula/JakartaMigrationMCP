package adrianmikula.jakartamigration.mcp;

import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * MCP Tools for Jakarta Migration.
 * 
 * This class provides MCP tools under Apache License 2.0.
 * All tools are FREE and available without any license requirement.
 * 
 * The MCP server is fully open source and community-driven.
 * 
 * Free Tools (Apache 2.0):
 * - analyzeJakartaReadiness: Analyzes project for Jakarta migration readiness
 * - detectBlockers: Identifies migration blockers
 * - recommendVersions: Recommends Jakarta-compatible versions
 * - createMigrationPlan: Creates comprehensive migration plans
 * - analyzeMigrationImpact: Full migration impact analysis
 * - verifyRuntime: Runtime verification
 * - applyAutoFixes: Automatic code fixes
 * - executeMigrationPlan: One-click migration execution
 * 
 * Uses Spring AI 1.0.0 MCP Server annotations to expose tools via the MCP protocol.
 */
@Component
@Slf4j
public class JakartaMigrationTools {

    private final CommunityMigrationTools communityTools;

    public JakartaMigrationTools(CommunityMigrationTools communityTools) {
        this.communityTools = communityTools;
    }

    /**
     * Analyzes a Java project for Jakarta migration readiness.
     * FREE TOOL - Licensed under Apache License 2.0
     */
    @McpTool(name = "analyzeJakartaReadiness", description = "Analyzes a Java project for Jakarta migration readiness. Returns a JSON report with readiness score, blockers, and recommendations.")
    public String analyzeJakartaReadiness(
            @McpToolParam(description = "Path to the project root directory", required = true) String projectPath) {
        return communityTools.analyzeJakartaReadiness(projectPath);
    }

    /**
     * Detects blockers that prevent Jakarta migration.
     * FREE TOOL - Licensed under Apache License 2.0
     */
    @McpTool(name = "detectBlockers", description = "Detects blockers that prevent Jakarta migration. Returns a JSON list of blockers with types, reasons, and mitigation strategies.")
    public String detectBlockers(
            @McpToolParam(description = "Path to the project root directory", required = true) String projectPath) {
        return communityTools.detectBlockers(projectPath);
    }

    /**
     * Recommends Jakarta-compatible versions for dependencies.
     * FREE TOOL - Licensed under Apache License 2.0
     */
    @McpTool(name = "recommendVersions", description = "Recommends Jakarta-compatible versions for project dependencies. Returns a JSON list of version recommendations with migration paths and compatibility scores.")
    public String recommendVersions(
            @McpToolParam(description = "Path to the project root directory", required = true) String projectPath) {
        return communityTools.recommendVersions(projectPath);
    }

    /**
     * Creates a migration plan for Jakarta migration.
     * FREE TOOL - Licensed under Apache License 2.0
     */
    @McpTool(name = "createMigrationPlan", description = "Creates a comprehensive migration plan for Jakarta migration. Returns a JSON plan with phases, estimated duration, and risk assessment.")
    public String createMigrationPlan(
            @McpToolParam(description = "Path to the project root directory", required = true) String projectPath) {
        return communityTools.createMigrationPlan(projectPath);
    }

    /**
     * Analyzes full migration impact combining dependency analysis and source code scanning.
     * FREE TOOL - Licensed under Apache License 2.0
     */
    @McpTool(name = "analyzeMigrationImpact", description = "Analyzes full migration impact combining dependency analysis and source code scanning. Returns a comprehensive summary with file counts, import counts, blockers, and estimated effort.")
    public String analyzeMigrationImpact(
            @McpToolParam(description = "Path to the project root directory", required = true) String projectPath) {
        return communityTools.analyzeMigrationImpact(projectPath);
    }

    /**
     * Verifies runtime execution of a migrated application.
     * FREE TOOL - Licensed under Apache License 2.0
     */
    @McpTool(name = "verifyRuntime", description = "Verifies runtime execution of a migrated Jakarta application. Returns a JSON result with execution status, errors, and metrics.")
    public String verifyRuntime(
            @McpToolParam(description = "Path to the JAR file to execute", required = true) String jarPath,
            @McpToolParam(description = "Optional timeout in seconds (default: 30)", required = false) Integer timeoutSeconds) {
        return communityTools.verifyRuntime(jarPath, timeoutSeconds);
    }

    /**
     * Applies automatic fixes to Jakarta migration issues.
     * FREE TOOL - Licensed under Apache License 2.0
     */
    @McpTool(name = "applyAutoFixes", description = "Applies automatic Jakarta migration fixes to source files. Updates imports, XML namespaces, and configurations. Returns a JSON result with refactored files and statistics.")
    public String applyAutoFixes(
            @McpToolParam(description = "Path to the project root directory", required = true) String projectPath,
            @McpToolParam(description = "Optional flag for dry run (default: false)", required = false) Boolean dryRun) {
        return communityTools.applyAutoFixes(projectPath, dryRun);
    }

    /**
     * Executes a comprehensive Jakarta migration plan.
     * FREE TOOL - Licensed under Apache License 2.0
     */
    @McpTool(name = "executeMigrationPlan", description = "Executes a complete Jakarta migration plan in phases. Applies all refactoring recipes according to the plan. Returns a JSON session result.")
    public String executeMigrationPlan(
            @McpToolParam(description = "Path to the project root directory", required = true) String projectPath) {
        return communityTools.executeMigrationPlan(projectPath);
    }
}
