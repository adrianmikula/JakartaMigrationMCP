package adrianmikula.jakartamigration.intellij.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ClaudeSkillInstaller.
 * Fast unit tests that do not require the IntelliJ platform.
 */
@Tag("fast")
class ClaudeSkillInstallerTest {

    @Test
    void isClaudeInstalledReturnsFalseWhenDirectoryMissing(@TempDir Path tempDir) {
        ClaudeSkillInstaller installer = new ClaudeSkillInstaller(tempDir.toString());
        assertThat(installer.isClaudeInstalled()).isFalse();
    }

    @Test
    void isClaudeInstalledReturnsTrueWhenDirectoryExists(@TempDir Path tempDir) throws Exception {
        Files.createDirectories(tempDir.resolve(".claude"));
        ClaudeSkillInstaller installer = new ClaudeSkillInstaller(tempDir.toString());
        assertThat(installer.isClaudeInstalled()).isTrue();
    }

    @Test
    void isSkillInstalledReturnsFalseWhenSkillMissing(@TempDir Path tempDir) throws Exception {
        Files.createDirectories(tempDir.resolve(".claude"));
        ClaudeSkillInstaller installer = new ClaudeSkillInstaller(tempDir.toString());
        assertThat(installer.isSkillInstalled()).isFalse();
    }

    @Test
    void isSkillInstalledReturnsTrueWhenSkillExists(@TempDir Path tempDir) throws Exception {
        Files.createDirectories(tempDir.resolve(".claude/skills/jakarta-migration-assistant"));
        Files.write(tempDir.resolve(".claude/skills/jakarta-migration-assistant/SKILL.md"), "test".getBytes());
        ClaudeSkillInstaller installer = new ClaudeSkillInstaller(tempDir.toString());
        assertThat(installer.isSkillInstalled()).isTrue();
    }

    @Test
    void installFailsWhenClaudeNotInstalled(@TempDir Path tempDir) {
        ClaudeSkillInstaller installer = new ClaudeSkillInstaller(tempDir.toString());
        ClaudeSkillInstaller.InstallResult result = installer.install();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("Claude Code does not appear to be installed");
    }

    @Test
    void installSucceedsWhenClaudeIsInstalled(@TempDir Path tempDir) throws Exception {
        Files.createDirectories(tempDir.resolve(".claude"));
        ClaudeSkillInstaller installer = new ClaudeSkillInstaller(tempDir.toString());
        ClaudeSkillInstaller.InstallResult result = installer.install();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).contains("installed successfully");
        assertThat(tempDir.resolve(".claude/skills/jakarta-migration-assistant/SKILL.md")).exists();
    }

    @Test
    void installOverwritesExistingSkill(@TempDir Path tempDir) throws Exception {
        Path skillDir = tempDir.resolve(".claude/skills/jakarta-migration-assistant");
        Files.createDirectories(skillDir);
        Files.write(skillDir.resolve("SKILL.md"), "old content".getBytes());

        ClaudeSkillInstaller installer = new ClaudeSkillInstaller(tempDir.toString());
        ClaudeSkillInstaller.InstallResult result = installer.install();

        assertThat(result.isSuccess()).isTrue();
        String written = Files.readString(skillDir.resolve("SKILL.md"));
        assertThat(written).contains("Jakarta Migration Assistant");
        assertThat(written).doesNotContain("old content");
    }

    @Test
    void getSkillPathReturnsCorrectPath(@TempDir Path tempDir) {
        ClaudeSkillInstaller installer = new ClaudeSkillInstaller(tempDir.toString());
        Path expected = tempDir.resolve(".claude/skills/jakarta-migration-assistant/SKILL.md");
        assertThat(installer.getSkillPath()).isEqualTo(expected);
    }

    @Test
    void getClaudeDirectoryReturnsCorrectPath(@TempDir Path tempDir) {
        ClaudeSkillInstaller installer = new ClaudeSkillInstaller(tempDir.toString());
        Path expected = tempDir.resolve(".claude");
        assertThat(installer.getClaudeDirectory()).isEqualTo(expected);
    }
}
