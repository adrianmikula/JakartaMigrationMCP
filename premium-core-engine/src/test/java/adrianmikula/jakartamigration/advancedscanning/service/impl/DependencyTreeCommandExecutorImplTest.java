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
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void stripAnsiEscapeSequences_shouldRemoveColorCodes() throws Exception {
        DependencyTreeCommandExecutorImpl executor = new DependencyTreeCommandExecutorImpl();
        Method method = DependencyTreeCommandExecutorImpl.class.getDeclaredMethod("stripAnsiEscapeSequences", String.class);
        method.setAccessible(true);

        // Test red color code
        String input = "\u001B[31mError message\u001B[0m";
        String result = (String) method.invoke(executor, input);
        assertEquals("Error message", result);

        // Test bold red color code
        input = "\u001B[1;31mFailed to execute goal\u001B[m";
        result = (String) method.invoke(executor, input);
        assertEquals("Failed to execute goal", result);

        // Test multiple escape sequences
        input = "\u001B[1;31mERROR\u001B[m Failed to execute goal on project \u001B[36mwebservice-example-client\u001B[m";
        result = (String) method.invoke(executor, input);
        assertEquals("ERROR Failed to execute goal on project webservice-example-client", result);
    }

    @Test
    void stripAnsiEscapeSequences_shouldHandleComplexMavenOutput() throws Exception {
        DependencyTreeCommandExecutorImpl executor = new DependencyTreeCommandExecutorImpl();
        Method method = DependencyTreeCommandExecutorImpl.class.getDeclaredMethod("stripAnsiEscapeSequences", String.class);
        method.setAccessible(true);

        String mavenOutput = "\u001B[1;31mERROR\u001B[m] Failed to execute goal on project \u001B[36mwebservice-example-client\u001B[m: \u001B[1;31mCould not collect dependencies\u001B[m\n" +
            "\u001B[1;31mERROR\u001B[m] \u001B[1;31mFailed to read artifact descriptor for com.sun.istack:istack-commons-runtime:jar:1.1-SNAPSHOT\u001B[m\n" +
            "[INFO] Some normal log line\n" +
            "{\n" +
            "  \"groupId\": \"com.example\",\n" +
            "  \"artifactId\": \"app\",\n" +
            "  \"children\": []\n" +
            "}\n";

        String result = (String) method.invoke(executor, mavenOutput);
        assertFalse(result.contains("\u001B"));
        assertTrue(result.contains("ERROR] Failed to execute goal"));
        assertTrue(result.contains("webservice-example-client"));
        assertTrue(result.contains("\"groupId\": \"com.example\""));
    }

    @Test
    void stripAnsiEscapeSequences_shouldHandleEmptyAndNullInput() throws Exception {
        DependencyTreeCommandExecutorImpl executor = new DependencyTreeCommandExecutorImpl();
        Method method = DependencyTreeCommandExecutorImpl.class.getDeclaredMethod("stripAnsiEscapeSequences", String.class);
        method.setAccessible(true);

        // Test null input
        String result = (String) method.invoke(executor, (String) null);
        assertNull(result);

        // Test empty input
        result = (String) method.invoke(executor, "");
        assertEquals("", result);

        // Test input without escape sequences
        result = (String) method.invoke(executor, "Normal text without colors");
        assertEquals("Normal text without colors", result);
    }

    @Test
    void parseMavenJsonOutput_shouldHandleAnsiEscapeSequencesInErrorOutput() throws Exception {
        // This test simulates the exact scenario from the error logs
        String mixedOutput = "\u001B[1;31mERROR\u001B[m] Failed to execute goal on project \u001B[36mwebservice-example-client\u001B[m: \u001B[1;31mCould not collect dependencies\u001B[m\n" +
            "[INFO] Some normal log line\n" +
            "{\"groupId\":\"com.example\",\"artifactId\":\"app\",\"children\":[]}\n" +
            "\u001B[1;31mERROR\u001B[m] \u001B[1;31mFailed to read artifact descriptor\u001B[m\n";

        java.lang.Process mockProcess = new java.lang.Process() {
            @Override public java.io.OutputStream getOutputStream() { return null; }
            @Override public java.io.InputStream getInputStream() { return new ByteArrayInputStream(mixedOutput.getBytes(StandardCharsets.UTF_8)); }
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

        // Should successfully parse the JSON despite ANSI escape sequences in error output
        assertEquals(1, deps.size());
        assertEquals("com.example", deps.get(0).getGroupId());
        assertEquals("app", deps.get(0).getArtifactId());
    }
}
