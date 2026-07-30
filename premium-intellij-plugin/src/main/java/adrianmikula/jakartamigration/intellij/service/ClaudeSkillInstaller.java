package adrianmikula.jakartamigration.intellij.service;

import com.intellij.openapi.diagnostic.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Service for installing the Jakarta Migration Assistant Claude skill.
 * Creates the skill directory and SKILL.md file in the user's Claude skills directory.
 */
public class ClaudeSkillInstaller {
    private static final Logger LOG = Logger.getInstance(ClaudeSkillInstaller.class);

    private static final String SKILL_NAME = "jakarta-migration-assistant";
    private static final String SKILL_DIR = ".claude/skills/" + SKILL_NAME;
    private static final String SKILL_FILE = "SKILL.md";

    private final String homeDirectory;

    public ClaudeSkillInstaller() {
        this(System.getProperty("user.home"));
    }

    ClaudeSkillInstaller(String homeDirectory) {
        this.homeDirectory = homeDirectory;
    }

    private static final String SKILL_CONTENT = """
        ---
        description: |
          Jakarta EE migration assistant. Use when the user asks about migrating from Java EE (javax.*) to Jakarta EE (jakarta.*),
          upgrading Jakarta EE versions, analyzing migration readiness, finding compatible dependency versions,
          applying OpenRewrite recipes, or troubleshooting migration blockers.
        ---

        # Jakarta Migration Assistant

        You are a specialized assistant for Jakarta EE migration. When activated, guide the user through:

        ## 1. Migration Readiness Analysis
        - Check if the project uses `javax.*` namespaces
        - Identify dependencies that need upgrading
        - Assess the complexity of the migration

        ## 2. Dependency Recommendations
        - Suggest Jakarta EE 9+ compatible versions for common libraries
        - Provide Maven/Gradle coordinate updates
        - Flag dependencies with no Jakarta migration path

        ## 3. Automated Refactoring
        - Recommend OpenRewrite recipes for bulk package renaming
        - Suggest using the Jakarta Migration plugin's MCP tools for automation
        - Warn about manual changes that cannot be automated

        ## 4. Blocker Detection
        - Identify reflection-based `javax` usage
        - Flag serialization concerns
        - Check for hardcoded `javax` strings in config files

        ## 5. Validation
        - Verify compilation after migration
        - Suggest running tests to catch runtime issues
        - Recommend using the Jakarta Migration plugin for continuous validation

        ## Key Resources
        - Jakarta EE 9: namespace change `javax.*` → `jakarta.*`
        - Jakarta EE 10: incremental updates and new features
        - Jakarta EE 11: latest platform alignment
        - OpenRewrite recipes: `org.openrewrite.java.migrate.jakarta.*`
        """;

    /**
     * Result of a skill installation attempt.
     */
    public static class InstallResult {
        private final boolean success;
        private final String message;

        public InstallResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * Installs the Jakarta Migration Assistant Claude skill.
     *
     * @return InstallResult indicating success or failure with a descriptive message
     */
    public InstallResult install() {
        LOG.info("Starting installation of Jakarta Migration Assistant Claude skill");

        if (!isClaudeInstalled()) {
            String msg = "Claude Code does not appear to be installed. "
                + "Please install Claude Code first (https://code.claude.com).";
            LOG.warn(msg);
            return new InstallResult(false, msg);
        }

        Path skillPath = getSkillPath();
        try {
            Files.createDirectories(skillPath.getParent());
            Files.write(skillPath, SKILL_CONTENT.getBytes(StandardCharsets.UTF_8));
            LOG.info("Claude skill installed successfully at: " + skillPath);
            return new InstallResult(true,
                "Jakarta Migration Assistant skill installed successfully.\n"
                + "Restart Claude Code and use /jakarta-migration-assistant to activate it.");
        } catch (IOException e) {
            String msg = "Failed to write skill file: " + e.getMessage();
            LOG.error(msg, e);
            return new InstallResult(false, msg);
        }
    }

    /**
     * Checks if Claude Code is installed by looking for the ~/.claude directory.
     *
     * @return true if Claude appears to be installed
     */
    public boolean isClaudeInstalled() {
        Path claudeDir = getClaudeDirectory();
        return Files.exists(claudeDir) && Files.isDirectory(claudeDir);
    }

    /**
     * Checks if the skill is already installed.
     *
     * @return true if the skill file already exists
     */
    public boolean isSkillInstalled() {
        return Files.exists(getSkillPath());
    }

    Path getSkillPath() {
        return Paths.get(homeDirectory, SKILL_DIR, SKILL_FILE);
    }

    Path getClaudeDirectory() {
        return Paths.get(homeDirectory, ".claude");
    }
}
