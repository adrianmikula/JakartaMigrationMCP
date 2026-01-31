package integration.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for MCP Server using STDIO transport.
 * 
 * This test treats the MCP server as a "black box" and communicates via
 * JSON-RPC
 * over stdin/stdout, just as Claude or Cursor would in a real-world scenario.
 * 
 * Strategy: "The Out-of-Process Mock"
 * - Spins up the MCP server as a sub-process
 * - Communicates via JSON-RPC over stdin/stdout
 * - Verifies JSON-RPC response contracts
 */
class McpServerStdioIntegrationTest {

    private Process serverProcess;
    private BufferedReader serverOutput;
    private BufferedReader serverError;
    private PrintWriter serverInput;
    private ObjectMapper objectMapper;
    private JsonNode initializeResponse;
    private final java.util.concurrent.BlockingQueue<String> stdoutQueue = new java.util.concurrent.LinkedBlockingQueue<>();
    private int requestId = 1;

    @BeforeEach
    void setup() throws IOException {
        // 1. Build the JAR if it doesn't exist
        File jarFile = new File("build/libs/jakarta-migration-mcp-1.0.0-SNAPSHOT.jar");
        if (!jarFile.exists()) {
            throw new IllegalStateException(
                    "JAR file not found. Please run 'gradlew bootJar' first: " + jarFile.getAbsolutePath());
        }

        // 2. Start the server as a sub-process
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            javaBin += ".exe";
        }

        ProcessBuilder processBuilder = new ProcessBuilder(
                javaBin,
                "-jar",
                jarFile.getAbsolutePath(),
                "--spring.profiles.active=mcp-stdio");

        System.err.println("Running command: " + String.join(" ", processBuilder.command()));

        processBuilder.environment().put("MCP_TRANSPORT", "stdio");

        this.serverProcess = processBuilder.start();
        this.serverOutput = new BufferedReader(
                new InputStreamReader(serverProcess.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
        this.serverError = new BufferedReader(
                new InputStreamReader(serverProcess.getErrorStream(), java.nio.charset.StandardCharsets.UTF_8));
        this.serverInput = new PrintWriter(
                new OutputStreamWriter(serverProcess.getOutputStream(), java.nio.charset.StandardCharsets.UTF_8), true);
        this.objectMapper = new ObjectMapper();

        // Start stdout and stderr reader threads
        startStdoutReader();
        startStderrReader();

        // Wait for server to be ready
        waitForServerStart();

        // 3. Initialize the MCP connection
        sendRequest("initialize", Map.of(
                "protocolVersion", "2024-11-05",
                "capabilities", Map.of(),
                "clientInfo", Map.of("name", "test-client", "version", "1.0.0")));

        // Wait for initialization response and store it
        this.initializeResponse = readResponse();
        assertThat(initializeResponse.has("result")).isTrue();
    }

    private final java.util.concurrent.CountDownLatch serverReadyLatch = new java.util.concurrent.CountDownLatch(1);

    private void startStdoutReader() {
        Thread thread = new Thread(() -> {
            try {
                String line;
                while ((line = serverOutput.readLine()) != null) {
                    System.err.println("[STDOUT-RAW] " + line); // LOG EVERY LINE
                    stdoutQueue.offer(line);
                }
            } catch (IOException e) {
                // Ignore if process is closing
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void startStderrReader() {
        Thread thread = new Thread(() -> {
            try {
                String line;
                while ((line = serverError.readLine()) != null) {
                    System.err.println("[SERVER-LOG] " + line);
                    if (line.contains("Enable completions capabilities")) {
                        serverReadyLatch.countDown();
                    }
                }
            } catch (IOException e) {
                // Ignore if process is closing
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void waitForServerStart() throws IOException {
        System.err.println("Waiting for server to signal readiness...");
        try {
            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < 30000) {
                if (serverReadyLatch.await(1, TimeUnit.SECONDS)) {
                    System.err.println("Server ready signal received.");
                    return;
                }
                if (!serverProcess.isAlive()) {
                    int exitCode = serverProcess.exitValue();
                    throw new IOException("Server process died prematurely with exit code: " + exitCode);
                }
            }
            throw new IOException("Server failed to start within 30s timeout. Check logs above.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for server start");
        }
    }

    @Test
    void testServerInitialization() throws IOException {
        // Verify server info is available from initialize response stored in setup
        assertThat(initializeResponse.has("result")).isTrue();
        JsonNode result = initializeResponse.get("result");
        assertThat(result.has("serverInfo")).isTrue();
        JsonNode serverInfo = result.get("serverInfo");
        assertThat(serverInfo.get("name").asText()).isEqualTo("jakarta-migration-mcp");
        assertThat(serverInfo.has("version")).isTrue();
    }

    @Test
    void testListTools() throws IOException {
        // Verify all tools are listed correctly
        sendRequest("tools/list", Map.of());
        JsonNode response = readResponse();

        assertThat(response.has("result")).isTrue();
        JsonNode result = response.get("result");
        assertThat(result.has("tools")).isTrue();
        JsonNode tools = result.get("tools");
        assertThat(tools.isArray()).isTrue();
        assertThat(tools.size()).isGreaterThan(0);

        // Verify Jakarta migration tools are present
        boolean hasAnalyzeReadiness = false;
        boolean hasDetectBlockers = false;
        boolean hasRecommendVersions = false;
        boolean hasCreatePlan = false;
        boolean hasVerifyRuntime = false;
        boolean hasCheckEnv = false;

        for (JsonNode tool : tools) {
            String name = tool.get("name").asText();
            if ("analyzeJakartaReadiness".equals(name))
                hasAnalyzeReadiness = true;
            if ("detectBlockers".equals(name))
                hasDetectBlockers = true;
            if ("recommendVersions".equals(name))
                hasRecommendVersions = true;
            if ("createMigrationPlan".equals(name))
                hasCreatePlan = true;
            if ("verifyRuntime".equals(name))
                hasVerifyRuntime = true;
            if ("check_env".equals(name))
                hasCheckEnv = true;

            // Verify tool descriptions are present (LLMs rely on these!)
            assertThat(tool.has("name")).isTrue();
            assertThat(tool.has("description")).isTrue();
            assertThat(tool.get("description").asText()).isNotEmpty();
        }

        assertThat(hasAnalyzeReadiness).isTrue();
        assertThat(hasDetectBlockers).isTrue();
        assertThat(hasRecommendVersions).isTrue();
        assertThat(hasCreatePlan).isTrue();
        assertThat(hasVerifyRuntime).isTrue();
        assertThat(hasCheckEnv).isTrue();
    }

    @Test
    void testCheckEnvTool() throws IOException {
        // Test the simple sentinel tool
        sendRequest("tools/call", Map.of(
                "name", "check_env",
                "arguments", Map.of("name", "PATH")));

        JsonNode response = readResponse();
        assertThat(response.has("result")).isTrue();
        JsonNode result = response.get("result");
        assertThat(result.has("content")).isTrue();
        JsonNode content = result.get("content");
        assertThat(content.isArray()).isTrue();
        assertThat(content.size()).isGreaterThan(0);

        String text = content.get(0).get("text").asText();
        assertThat(text).isNotNull();
        // Should either say "Defined:" or "Missing:"
        assertThat(text).matches("(Defined|Missing):.*");
    }

    @Test
    void testAnalyzeJakartaReadinessTool() throws IOException {
        // Use a test project path (create a minimal test project)
        Path testProject = createTestProject();

        try {
            sendRequest("tools/call", Map.of(
                    "name", "analyzeJakartaReadiness",
                    "arguments", Map.of("projectPath", testProject.toString())));

            JsonNode response = readResponse();
            assertThat(response.has("result")).isTrue();
            JsonNode result = response.get("result");
            assertThat(result.has("content")).isTrue();

            String text = result.get("content").get(0).get("text").asText();
            assertThat(text).isNotNull();
            // Should return JSON with status field
            assertThat(text).contains("\"status\"");

        } finally {
            // Cleanup
            deleteTestProject(testProject);
        }
    }

    @Test
    void testDetectBlockersTool() throws IOException {
        Path testProject = createTestProject();

        try {
            sendRequest("tools/call", Map.of(
                    "name", "detectBlockers",
                    "arguments", Map.of("projectPath", testProject.toString())));

            JsonNode response = readResponse();
            assertThat(response.has("result")).isTrue();
            JsonNode result = response.get("result");
            assertThat(result.has("content")).isTrue();

            String text = result.get("content").get(0).get("text").asText();
            assertThat(text).isNotNull();
            assertThat(text).contains("\"status\"");

        } finally {
            deleteTestProject(testProject);
        }
    }

    @Test
    void testRecommendVersionsTool() throws IOException {
        Path testProject = createTestProject();

        try {
            sendRequest("tools/call", Map.of(
                    "name", "recommendVersions",
                    "arguments", Map.of("projectPath", testProject.toString())));

            JsonNode response = readResponse();
            assertThat(response.has("result")).isTrue();
            JsonNode result = response.get("result");
            assertThat(result.has("content")).isTrue();

            String text = result.get("content").get(0).get("text").asText();
            assertThat(text).isNotNull();
            assertThat(text).contains("\"status\"");

        } finally {
            deleteTestProject(testProject);
        }
    }

    @Test
    void testCreateMigrationPlanTool() throws IOException {
        Path testProject = createTestProject();

        try {
            sendRequest("tools/call", Map.of(
                    "name", "createMigrationPlan",
                    "arguments", Map.of("projectPath", testProject.toString())));

            JsonNode response = readResponse();
            assertThat(response.has("result")).isTrue();
            JsonNode result = response.get("result");
            assertThat(result.has("content")).isTrue();

            String text = result.get("content").get(0).get("text").asText();
            assertThat(text).isNotNull();
            assertThat(text).contains("\"status\"");

        } finally {
            deleteTestProject(testProject);
        }
    }

    @Test
    void testToolInputSchemaValidation() throws IOException {
        // Verify that tool input schemas are valid
        sendRequest("tools/list", Map.of());
        JsonNode response = readResponse();
        JsonNode tools = response.get("result").get("tools");

        for (JsonNode tool : tools) {
            assertThat(tool.has("inputSchema")).isTrue();
            JsonNode schema = tool.get("inputSchema");
            assertThat(schema.has("type")).isTrue();
        }
    }

    @Test
    void testInvalidToolCall() throws IOException {
        // Verify error handling for invalid tool calls
        sendRequest("tools/call", Map.of(
                "name", "nonexistent_tool",
                "arguments", Map.of()));

        JsonNode response = readResponse();
        // Should have an error
        assertThat(response.has("error")).isTrue();
    }

    // Helper methods for JSON-RPC communication

    private void sendRequest(String method, Map<String, Object> params) throws IOException {
        Map<String, Object> request = Map.of(
                "jsonrpc", "2.0",
                "id", requestId++,
                "method", method,
                "params", params);

        String json = objectMapper.writeValueAsString(request);
        serverInput.println(json);
        serverInput.flush();
    }

    private JsonNode readResponse() throws IOException {
        try {
            String line;
            while ((line = stdoutQueue.poll(30, TimeUnit.SECONDS)) != null) {
                line = line.trim();
                if (line.isEmpty())
                    continue;
                if (line.startsWith("{")) {
                    try {
                        return objectMapper.readTree(line);
                    } catch (Exception e) {
                        System.err.println("[WARN] Failed to parse JSON from stdout: " + line);
                    }
                } else {
                    System.err.println("[STDOUT] " + line);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while reading response");
        }
        throw new IOException("Server closed connection or timeout reached");
    }

    @AfterEach
    void tearDown() {
        if (serverInput != null) {
            serverInput.close();
        }
        if (serverOutput != null) {
            try {
                serverOutput.close();
            } catch (IOException e) {
                // Ignore
            }
        }
        if (serverProcess != null) {
            serverProcess.destroy();
            try {
                serverProcess.waitFor(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (serverProcess.isAlive()) {
                serverProcess.destroyForcibly();
            }
        }
    }

    /**
     * Create a minimal test project for testing Jakarta migration tools.
     */
    private Path createTestProject() {
        try {
            Path testProject = Paths.get(System.getProperty("java.io.tmpdir"),
                    "mcp-test-project-" + System.currentTimeMillis());
            testProject.toFile().mkdirs();

            // Create a minimal pom.xml
            Path pomXml = testProject.resolve("pom.xml");
            Files.writeString(pomXml, """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0"
                             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                             xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>com.test</groupId>
                        <artifactId>test-project</artifactId>
                        <version>1.0.0</version>
                        <dependencies>
                            <dependency>
                                <groupId>javax.servlet</groupId>
                                <artifactId>javax.servlet-api</artifactId>
                                <version>4.0.1</version>
                            </dependency>
                        </dependencies>
                    </project>
                    """);

            return testProject;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test project", e);
        }
    }

    private void deleteTestProject(Path testProject) {
        try {
            if (testProject != null && testProject.toFile().exists()) {
                deleteRecursively(testProject.toFile());
            }
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }
}
