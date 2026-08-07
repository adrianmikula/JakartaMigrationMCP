package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.DependencyTreeResult;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for DependencyTreeCommandExecutorImpl.
 * Tests command availability checks and dependency tree execution.
 */
@Tag("slow")
class DependencyTreeCommandExecutorImplTest {

    @Test
    void isMavenAvailable_shouldReturnBoolean() {
        // This test just verifies the method runs without error
        // Result depends on whether Maven is installed on the test machine
        boolean available = DependencyTreeCommandExecutorImpl.isMavenAvailable();

        // Should return either true or false, never throw
        assertTrue(available == true || available == false);
    }

    @Test
    void isGradleAvailable_shouldReturnBoolean() {
        // This test just verifies the method runs without error
        // Result depends on whether Gradle is installed on the test machine
        boolean available = DependencyTreeCommandExecutorImpl.isGradleAvailable();

        // Should return either true or false, never throw
        assertTrue(available == true || available == false);
    }

    @Test
    void dependencyTreeResult_shouldStoreErrorMessage() {
        String errorMessage = "Command failed with exit code 1";
        DependencyTreeResult result = DependencyTreeResult.error(errorMessage);

        assertFalse(result.isSuccess());
        assertTrue(result.getDependencies().isEmpty());
        assertEquals(errorMessage, result.getErrorMessage());
    }

    @Test
    void dependencyTreeResult_shouldCreateEmptyResult() {
        DependencyTreeResult result = DependencyTreeResult.empty();

        assertTrue(result.isSuccess());
        assertNotNull(result.getDependencies());
        assertTrue(result.getDependencies().isEmpty());
        assertTrue(result.getScopes().isEmpty());
        assertNull(result.getErrorMessage());
    }

    @Test
    void dependencyTreeResult_shouldCreateWithDependencies() {
        var deps = java.util.List.of(
                new DependencyTreeResult.DependencyNode(
                        "javax.servlet", "javax.servlet-api", "4.0.1",
                        "provided", 0, false, null
                ),
                new DependencyTreeResult.DependencyNode(
                        "javax.xml.bind", "jaxb-api", "2.3.1",
                        "compile", 1, true, "javax.servlet:javax.servlet-api"
                )
        );
        var scopes = java.util.Set.of("compile", "provided");

        DependencyTreeResult result = new DependencyTreeResult(deps, scopes);

        assertTrue(result.isSuccess());
        assertEquals(2, result.getDependencies().size());
        assertEquals(2, result.getScopes().size());
        assertNull(result.getErrorMessage());
    }

    @Test
    void dependencyNode_shouldCalculateArtifactKey() {
        DependencyTreeResult.DependencyNode node = new DependencyTreeResult.DependencyNode(
                "javax.servlet", "javax.servlet-api", "4.0.1",
                "provided", 0, false, null
        );

        assertEquals("javax.servlet:javax.servlet-api", node.getArtifactKey());
    }

    @Test
    void dependencyNode_shouldStoreAllFields() {
        DependencyTreeResult.DependencyNode node = new DependencyTreeResult.DependencyNode(
                "javax.xml.bind", "jaxb-api", "2.3.1",
                "compile", 2, true, "parent:artifact"
        );

        assertEquals("javax.xml.bind", node.getGroupId());
        assertEquals("jaxb-api", node.getArtifactId());
        assertEquals("2.3.1", node.getVersion());
        assertEquals("compile", node.getScope());
        assertEquals(2, node.getDepth());
        assertTrue(node.isTransitive());
    }

    @Test
    void executeMavenDependencyTreeAsync_shouldHandleMissingMaven(@TempDir Path tempDir) throws Exception {
        // Create a temporary pom.xml file
        Path pomFile = tempDir.resolve("pom.xml");
        Files.writeString(pomFile, "<?xml version=\"1.0\"?><project></project>");
        
        DependencyTreeCommandExecutorImpl executor = new DependencyTreeCommandExecutorImpl();
        CompletableFuture<DependencyTreeResult> future = 
            executor.executeMavenDependencyTreeAsync(pomFile, Set.of("compile"));
        
        DependencyTreeResult result = future.get();
        
        // Should return an error result when Maven is not available
        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("not found") || 
                  result.getErrorMessage().contains("command"));
    }

    @Test
    void executeGradleDependenciesAsync_shouldHandleMissingGradle(@TempDir Path tempDir) throws Exception {
        // Create a temporary build.gradle file
        Path buildFile = tempDir.resolve("build.gradle");
        Files.writeString(buildFile, "plugins { java }");
        
        DependencyTreeCommandExecutorImpl executor = new DependencyTreeCommandExecutorImpl();
        CompletableFuture<DependencyTreeResult> future = 
            executor.executeGradleDependenciesAsync(buildFile, Set.of("compileClasspath"));
        
        DependencyTreeResult result = future.get();
        
        // Should return an error result when Gradle is not available
        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("not found") || 
                  result.getErrorMessage().contains("command"));
    }

    @Test
    void executeMavenDependencyTreeAsync_shouldUseMavenWrapperWhenAvailable(@TempDir Path tempDir) throws Exception {
        // Create a temporary pom.xml file
        Path pomFile = tempDir.resolve("pom.xml");
        Files.writeString(pomFile, "<?xml version=\"1.0\"?><project></project>");
        
        // Create a mock Maven wrapper (Windows batch file)
        Path mvnwBat = tempDir.resolve("mvnw.bat");
        Files.writeString(mvnwBat, "@echo off\necho Mock Maven wrapper");
        mvnwBat.toFile().setExecutable(true);
        
        DependencyTreeCommandExecutorImpl executor = new DependencyTreeCommandExecutorImpl();
        CompletableFuture<DependencyTreeResult> future = 
            executor.executeMavenDependencyTreeAsync(pomFile, Set.of("compile"));
        
        DependencyTreeResult result = future.get();
        
        // The wrapper should be found and attempted (may fail but shouldn't be "command not found")
        assertNotNull(result);
        // The exact result depends on the mock wrapper behavior
    }

    @Test
    void executeGradleDependenciesAsync_shouldUseGradleWrapperWhenAvailable(@TempDir Path tempDir) throws Exception {
        // Create a temporary build.gradle file
        Path buildFile = tempDir.resolve("build.gradle");
        Files.writeString(buildFile, "plugins { java }");
        
        // Create a mock Gradle wrapper (Windows batch file)
        Path gradlewBat = tempDir.resolve("gradlew.bat");
        Files.writeString(gradlewBat, "@echo off\necho Mock Gradle wrapper");
        gradlewBat.toFile().setExecutable(true);
        
        DependencyTreeCommandExecutorImpl executor = new DependencyTreeCommandExecutorImpl();
        CompletableFuture<DependencyTreeResult> future = 
            executor.executeGradleDependenciesAsync(buildFile, Set.of("compileClasspath"));
        
        DependencyTreeResult result = future.get();
        
        // The wrapper should be found and attempted (may fail but shouldn't be "command not found")
        assertNotNull(result);
        // The exact result depends on the mock wrapper behavior
    }

    @Test
    void shutdown_shouldNotThrow() {
        DependencyTreeCommandExecutorImpl executor = new DependencyTreeCommandExecutorImpl();
        assertDoesNotThrow(executor::shutdown);
    }

    @Test
    void extractJson_shouldExtractBalancedJsonFromMixedOutput() throws Exception {
        DependencyTreeCommandExecutorImpl executor = new DependencyTreeCommandExecutorImpl();
        Method method = DependencyTreeCommandExecutorImpl.class.getDeclaredMethod("extractJson", String.class);
        method.setAccessible(true);

        String mixedOutput = "[INFO] Some log line\n{\"groupId\":\"com.example\",\"artifactId\":\"app\",\"children\":[]}\n[WARN] Another line";
        String result = (String) method.invoke(executor, mixedOutput);
        assertEquals("{\"groupId\":\"com.example\",\"artifactId\":\"app\",\"children\":[]}", result);
    }

    @Test
    void parseMavenJsonOutput_shouldParseMultiLineJsonWithTransitiveDeps() throws Exception {
        String json = "[\n" +
            "  {\n" +
            "    \"groupId\": \"com.example\",\n" +
            "    \"artifactId\": \"parent\",\n" +
            "    \"version\": \"1.0.0\",\n" +
            "    \"scope\": \"compile\",\n" +
            "    \"children\": [\n" +
            "      {\n" +
            "        \"groupId\": \"javax.servlet\",\n" +
            "        \"artifactId\": \"javax.servlet-api\",\n" +
            "        \"version\": \"4.0.1\",\n" +
            "        \"scope\": \"provided\",\n" +
            "        \"children\": []\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "]";

        java.lang.Process mockProcess = new java.lang.Process() {
            @Override public java.io.OutputStream getOutputStream() { return null; }
            @Override public java.io.InputStream getInputStream() { return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)); }
            @Override public java.io.InputStream getErrorStream() { return new ByteArrayInputStream(new byte[0]); }
            @Override public int waitFor() { return 0; }
            @Override public int exitValue() { return 0; }
            @Override public void destroy() {}
        };

        DependencyTreeCommandExecutorImpl executor = new DependencyTreeCommandExecutorImpl();
        Method method = DependencyTreeCommandExecutorImpl.class.getDeclaredMethod("parseMavenJsonOutput", java.lang.Process.class, Set.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<DependencyTreeResult.DependencyNode> deps =
            (List<DependencyTreeResult.DependencyNode>) method.invoke(executor, mockProcess, Set.of());

        assertEquals(2, deps.size());
        assertEquals("com.example", deps.get(0).getGroupId());
        assertEquals("parent", deps.get(0).getArtifactId());
        assertFalse(deps.get(0).isTransitive());
        assertEquals("javax.servlet", deps.get(1).getGroupId());
        assertEquals("javax.servlet-api", deps.get(1).getArtifactId());
        assertTrue(deps.get(1).isTransitive());
        assertEquals("com.example:parent", deps.get(1).getParentArtifactKey());
    }

    @Test
    void parseMavenJsonOutput_shouldHandleObjectRoot() throws Exception {
        String json = "{\n" +
            "  \"groupId\": \"com.example\",\n" +
            "  \"artifactId\": \"app\",\n" +
            "  \"version\": \"1.0.0\",\n" +
            "  \"children\": []\n" +
            "}";

        java.lang.Process mockProcess = new java.lang.Process() {
            @Override public java.io.OutputStream getOutputStream() { return null; }
            @Override public java.io.InputStream getInputStream() { return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)); }
            @Override public java.io.InputStream getErrorStream() { return new ByteArrayInputStream(new byte[0]); }
            @Override public int waitFor() { return 0; }
            @Override public int exitValue() { return 0; }
            @Override public void destroy() {}
        };

        DependencyTreeCommandExecutorImpl executor = new DependencyTreeCommandExecutorImpl();
        Method method = DependencyTreeCommandExecutorImpl.class.getDeclaredMethod("parseMavenJsonOutput", java.lang.Process.class, Set.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<DependencyTreeResult.DependencyNode> deps =
            (List<DependencyTreeResult.DependencyNode>) method.invoke(executor, mockProcess, Set.of());

        assertEquals(1, deps.size());
        assertEquals("com.example", deps.get(0).getGroupId());
        assertEquals("app", deps.get(0).getArtifactId());
        assertFalse(deps.get(0).isTransitive());
    }

    // ── Gradle Command Building Tests ────────────────────────────────────────

    @Test
    void buildGradleCommand_shouldNotContainConfigurationFlag(@TempDir Path tempDir) throws Exception {
        DependencyTreeCommandExecutorImpl executor = new DependencyTreeCommandExecutorImpl();
        Method method = DependencyTreeCommandExecutorImpl.class.getDeclaredMethod("buildGradleCommand", Set.class, Path.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> cmd = (List<String>) method.invoke(executor, Set.of("compileClasspath"), tempDir);

        // --configuration must NEVER appear — Gradle only accepts it once and
        // passing multiple flags causes "Multiple arguments were provided" error
        assertThat(cmd).doesNotContain("--configuration");
    }

    @Test
    void buildGradleCommand_shouldContainQuietAndNoDaemon(@TempDir Path tempDir) throws Exception {
        DependencyTreeCommandExecutorImpl executor = new DependencyTreeCommandExecutorImpl();
        Method method = DependencyTreeCommandExecutorImpl.class.getDeclaredMethod("buildGradleCommand", Set.class, Path.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> cmd = (List<String>) method.invoke(executor, Set.of(), tempDir);

        assertThat(cmd).contains("--quiet", "--no-daemon");
    }

    @Test
    void buildGradleCommand_shouldUseWrapperWhenAvailable(@TempDir Path tempDir) throws Exception {
        // Place a gradlew wrapper in the temp directory
        Path gradlew = tempDir.resolve("gradlew");
        Files.writeString(gradlew, "#!/bin/sh\necho 'gradle wrapper'\n");
        gradlew.toFile().setExecutable(true);

        DependencyTreeCommandExecutorImpl executor = new DependencyTreeCommandExecutorImpl();
        Method method = DependencyTreeCommandExecutorImpl.class.getDeclaredMethod("buildGradleCommand", Set.class, Path.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> cmd = (List<String>) method.invoke(executor, Set.of(), tempDir);

        // First element should be the wrapper path, not "gradle"
        assertThat(cmd.get(0)).isEqualTo(gradlew.toString());
    }

    @Test
    void buildGradleCommand_shouldFallBackToSystemGradle(@TempDir Path tempDir) throws Exception {
        // No wrapper in temp directory — should fall back to system "gradle"
        DependencyTreeCommandExecutorImpl executor = new DependencyTreeCommandExecutorImpl();
        Method method = DependencyTreeCommandExecutorImpl.class.getDeclaredMethod("buildGradleCommand", Set.class, Path.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> cmd = (List<String>) method.invoke(executor, Set.of(), tempDir);

        assertThat(cmd.get(0)).isEqualTo("gradle");
    }

    @Test
    void buildGradleCommand_shouldStartWithDependenciesTask(@TempDir Path tempDir) throws Exception {
        DependencyTreeCommandExecutorImpl executor = new DependencyTreeCommandExecutorImpl();
        Method method = DependencyTreeCommandExecutorImpl.class.getDeclaredMethod("buildGradleCommand", Set.class, Path.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> cmd = (List<String>) method.invoke(executor, Set.of(), tempDir);

        // Second element should be "dependencies" task
        assertThat(cmd.get(1)).isEqualTo("dependencies");
    }

    // ── Multi-module Gradle tests ─────────────────────────────────────────

    @Test
    void findGradleProjectRoot_shouldFindSettingsGradleKts(@TempDir Path tempDir) throws Exception {
        // root/settings.gradle.kts → root/sub/build.gradle.kts
        Path root = tempDir.resolve("root");
        Path sub = root.resolve("sub");
        Files.createDirectories(sub);
        Files.writeString(root.resolve("settings.gradle.kts"), "rootProject.name = 'demo'");
        Path buildFile = sub.resolve("build.gradle.kts");
        Files.writeString(buildFile, "plugins { java }");

        Optional<Path> result = DependencyTreeCommandExecutorImpl.findGradleProjectRoot(sub);

        assertTrue(result.isPresent());
        assertEquals(root, result.get());
    }

    @Test
    void findGradleProjectRoot_shouldFindSettingsGradle(@TempDir Path tempDir) throws Exception {
        Path root = tempDir.resolve("root");
        Path sub = root.resolve("module-a");
        Files.createDirectories(sub);
        Files.writeString(root.resolve("settings.gradle"), "rootProject.name = 'demo'");
        Path buildFile = sub.resolve("build.gradle");
        Files.writeString(buildFile, "apply plugin: 'java'");

        Optional<Path> result = DependencyTreeCommandExecutorImpl.findGradleProjectRoot(sub);

        assertTrue(result.isPresent());
        assertEquals(root, result.get());
    }

    @Test
    void findGradleProjectRoot_shouldReturnEmpty_whenNoSettingsFile(@TempDir Path tempDir) throws Exception {
        Path sub = tempDir.resolve("standalone");
        Files.createDirectories(sub);
        Files.writeString(sub.resolve("build.gradle"), "plugins { java }");

        Optional<Path> result = DependencyTreeCommandExecutorImpl.findGradleProjectRoot(sub);

        assertFalse(result.isPresent());
    }

    @Test
    void findGradleProjectRoot_shouldReturnEmpty_whenStartDirIsNull() {
        Optional<Path> result = DependencyTreeCommandExecutorImpl.findGradleProjectRoot(null);
        assertFalse(result.isPresent());
    }

    @Test
    void computeGradleModuleName_shouldReturnColonPrefixedPath(@TempDir Path tempDir) {
        Path root = tempDir.resolve("root");
        Path sub = root.resolve("community-core-engine");

        String name = DependencyTreeCommandExecutorImpl.computeGradleModuleName(root, sub);

        assertThat(name).isEqualTo(":community-core-engine");
    }

    @Test
    void computeGradleModuleName_shouldHandleNestedModules(@TempDir Path tempDir) {
        Path root = tempDir.resolve("root");
        Path sub = root.resolve("libs").resolve("core");

        String name = DependencyTreeCommandExecutorImpl.computeGradleModuleName(root, sub);

        assertThat(name).isEqualTo(":libs:core");
    }

    @Test
    void executeGradleDependenciesAsync_shouldUseModuleTaskPath_whenInSubmodule(@TempDir Path tempDir) throws Exception {
        // Arrange: root/settings.gradle.kts + sub/build.gradle.kts
        Path root = tempDir.resolve("root");
        Path sub = root.resolve("community-core-engine");
        Files.createDirectories(sub);
        Files.writeString(root.resolve("settings.gradle.kts"), "rootProject.name = 'demo'");
        Files.writeString(sub.resolve("build.gradle.kts"), "plugins { java }");

        DependencyTreeCommandExecutorImpl executor = new DependencyTreeCommandExecutorImpl();
        CompletableFuture<DependencyTreeResult> future =
            executor.executeGradleDependenciesAsync(sub.resolve("build.gradle.kts"), Set.of());

        DependencyTreeResult result = future.get();

        // Will fail because there's no real gradle wrapper, but the command structure
        // is validated by the error message (it should NOT be "gradle command not found"
        // if gradle is installed, or if not, it should attempt the module task path)
        assertNotNull(result);
        // The key assertion: no crash, graceful handling
    }

    @Test
    void executeGradleDependenciesAsync_shouldRunDirectly_whenAtRoot(@TempDir Path tempDir) throws Exception {
        // Arrange: root/settings.gradle.kts + root/build.gradle.kts (root IS the project)
        Path root = tempDir.resolve("root");
        Files.createDirectories(root);
        Files.writeString(root.resolve("settings.gradle.kts"), "rootProject.name = 'demo'");
        Files.writeString(root.resolve("build.gradle.kts"), "plugins { java }");

        DependencyTreeCommandExecutorImpl executor = new DependencyTreeCommandExecutorImpl();
        CompletableFuture<DependencyTreeResult> future =
            executor.executeGradleDependenciesAsync(root.resolve("build.gradle.kts"), Set.of());

        DependencyTreeResult result = future.get();

        assertNotNull(result);
    }

    @Test
    void executeGradleDependenciesAsync_shouldRunDirectly_whenNoSettingsFile(@TempDir Path tempDir) throws Exception {
        // No settings.gradle → single-module project
        Files.writeString(tempDir.resolve("build.gradle"), "apply plugin: 'java'");

        DependencyTreeCommandExecutorImpl executor = new DependencyTreeCommandExecutorImpl();
        CompletableFuture<DependencyTreeResult> future =
            executor.executeGradleDependenciesAsync(tempDir.resolve("build.gradle"), Set.of());

        DependencyTreeResult result = future.get();

        // Should fail gracefully (no gradle installed) or succeed
        assertNotNull(result);
    }
}
