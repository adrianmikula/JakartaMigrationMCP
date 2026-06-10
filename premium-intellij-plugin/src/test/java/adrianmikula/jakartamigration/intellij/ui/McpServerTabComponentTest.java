package adrianmikula.jakartamigration.intellij.ui;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for McpServerTabComponent, specifically the Claude Skill installation feature.
 * Spec: install-claude-skill-button-895efa.md
 */
public class McpServerTabComponentTest extends BasePlatformTestCase {

    private String originalUserHome;
    private Path tempHome;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        originalUserHome = System.getProperty("user.home");
        tempHome = Files.createTempDirectory("mcp-server-test-home");
        System.setProperty("user.home", tempHome.toString());
    }

    @Override
    public void tearDown() {
        try {
            System.setProperty("user.home", originalUserHome);
            deleteRecursively(tempHome);
        } catch (Exception e) {
            // Ignore cleanup errors
        }
        try {
            super.tearDown();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void testComponentCreatesPanelWithClaudeSkillSection() {
        McpServerTabComponent component = new McpServerTabComponent(getProject());
        assertThat(component).isNotNull();
        assertThat(component.getPanel()).isNotNull();
    }

    public void testInstallClaudeSkillWhenClaudeDirExists() throws Exception {
        // Create .claude directory to simulate Claude Code installation
        Path claudeDir = tempHome.resolve(".claude");
        Files.createDirectories(claudeDir);

        McpServerTabComponent component = new McpServerTabComponent(getProject());
        component.installClaudeSkill();

        Path expectedSkillFile = claudeDir.resolve("skills/jakarta-migration-assistant.md");
        assertThat(expectedSkillFile).exists();
        assertThat(Files.readString(expectedSkillFile))
            .contains("Jakarta Migration Analyzer");
    }

    public void testInstallClaudeSkillCreatesSkillsDir() throws Exception {
        Path claudeDir = tempHome.resolve(".claude");
        Files.createDirectories(claudeDir);

        McpServerTabComponent component = new McpServerTabComponent(getProject());
        component.installClaudeSkill();

        Path skillsDir = claudeDir.resolve("skills");
        assertThat(skillsDir).exists();
    }

    public void testInstallClaudeSkillWhenClaudeDirMissing() throws Exception {
        // Ensure .claude does NOT exist
        Path claudeDir = tempHome.resolve(".claude");
        assertThat(claudeDir).doesNotExist();

        McpServerTabComponent component = new McpServerTabComponent(getProject());
        component.installClaudeSkill();

        // No skill file should be created
        Path expectedSkillFile = claudeDir.resolve("skills/jakarta-migration-assistant.md");
        assertThat(expectedSkillFile).doesNotExist();
    }

    public void testInstallClaudeSkillOverwritesExistingFile() throws Exception {
        Path claudeDir = tempHome.resolve(".claude");
        Path skillsDir = claudeDir.resolve("skills");
        Files.createDirectories(skillsDir);

        Path existingFile = skillsDir.resolve("jakarta-migration-assistant.md");
        Files.writeString(existingFile, "old content");

        McpServerTabComponent component = new McpServerTabComponent(getProject());
        component.installClaudeSkill();

        assertThat(existingFile).exists();
        assertThat(Files.readString(existingFile))
            .contains("Jakarta Migration Analyzer");
        assertThat(Files.readString(existingFile))
            .doesNotContain("old content");
    }

    private void deleteRecursively(Path path) throws Exception {
        if (!Files.exists(path)) {
            return;
        }
        if (Files.isDirectory(path)) {
            try (var entries = Files.list(path)) {
                for (Path entry : entries.toList()) {
                    deleteRecursively(entry);
                }
            }
        }
        Files.delete(path);
    }
}
