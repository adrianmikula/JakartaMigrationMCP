package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.DependencyTreeResult;
import adrianmikula.jakartamigration.advancedscanning.service.DependencyTreeCommandExecutor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Async implementation of DependencyTreeCommandExecutor with resource management.
 */
@Slf4j
public class DependencyTreeCommandExecutorImpl implements DependencyTreeCommandExecutor {

    private static final int MAX_DEPENDENCIES = 10000;
    private static final int THREAD_POOL_SIZE = 2;
    private final ExecutorService executor;
    private final ObjectMapper objectMapper;
    private final GradleToolingApiExecutor gradleToolingApiExecutor;

    public DependencyTreeCommandExecutorImpl() {
        this.executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        this.objectMapper = new ObjectMapper();
        this.gradleToolingApiExecutor = new GradleToolingApiExecutor();
    }

    @Override
    public CompletableFuture<DependencyTreeResult> executeMavenDependencyTreeAsync(Path pomXmlPath, Set<String> scopes) {
        return CompletableFuture.supplyAsync(() -> {
            Path projectDir = pomXmlPath.getParent();
            // Fast-fail if neither Maven nor Maven wrapper is available
            if (!isMavenAvailableForProject(projectDir)) {
                return DependencyTreeResult.error("mvn command not found");
            }
            if (scopes.size() <= 1) {
                List<String> command = buildMavenCommand(scopes, projectDir);
                return executeCommand(command, projectDir, "mvn dependency:tree",
                    process -> parseMavenJsonOutput(process, scopes));
            }

            // dependency:tree's -Dscope parameter only supports a single scope.
            // Run once per requested scope and merge the results.
            Set<String> seen = new HashSet<>();
            List<DependencyTreeResult.DependencyNode> merged = new ArrayList<>();
            boolean anySuccess = false;
            String lastError = null;
            for (String scope : scopes) {
                Set<String> singleScope = Set.of(scope);
                List<String> command = buildMavenCommand(singleScope, projectDir);
                DependencyTreeResult result = executeCommand(command, projectDir, "mvn dependency:tree",
                    process -> parseMavenJsonOutput(process, singleScope));
                if (result.isSuccess()) {
                    anySuccess = true;
                    for (DependencyTreeResult.DependencyNode node : result.getDependencies()) {
                        if (seen.add(node.getArtifactKey())) {
                            merged.add(node);
                        }
                    }
                } else if (lastError == null) {
                    lastError = result.getErrorMessage();
                }
            }

            if (!anySuccess) {
                return DependencyTreeResult.error(lastError != null ? lastError : "mvn dependency:tree failed for all scopes");
            }
            return new DependencyTreeResult(merged, scopes);
        }, executor);
    }

    @Override
    public CompletableFuture<DependencyTreeResult> executeGradleDependenciesAsync(Path buildFilePath, Set<String> scopes) {
        // Gradle dependency resolution has been migrated to the Gradle Tooling API.
        // The old process-spawning implementation below is deprecated and kept only
        // for reference; the active path delegates to GradleToolingApiExecutor.
        return gradleToolingApiExecutor.executeGradleDependenciesAsync(buildFilePath, scopes);
    }

    private List<String> buildMavenCommand(Set<String> scopes, Path projectDir) {
        // First try to find Maven wrapper
        Optional<Path> mavenWrapper = findMavenWrapper(projectDir);

        String mavenCommand;
        if (mavenWrapper.isPresent()) {
            mavenCommand = mavenWrapper.get().toString();
            log.debug("Using Maven wrapper: {}", mavenCommand);
        } else {
            mavenCommand = "mvn";
            log.debug("Using system Maven: {}", mavenCommand);
        }

        List<String> cmd = new ArrayList<>(List.of(mavenCommand, "dependency:tree", "-DoutputType=json", "-q"));
        // dependency:tree's -Dscope only supports a single scope; omit it for the default all-scopes run.
        if (scopes.size() == 1) cmd.add("-Dscope=" + scopes.iterator().next());
        return cmd;
    }





    @FunctionalInterface
    private interface OutputParser {
        List<DependencyTreeResult.DependencyNode> parse(Process process) throws IOException;
    }

    private DependencyTreeResult executeCommand(List<String> command, Path projectDir, String cmdName, OutputParser parser) {
        if (projectDir == null) return DependencyTreeResult.error("Invalid path: no parent directory");

        Process process = null;
        try {
            String commandStr = String.join(" ", command);
            log.debug("Executing command: {} in directory: {}", commandStr, projectDir);
            
            process = new ProcessBuilder(command).directory(projectDir.toFile()).redirectErrorStream(true).start();
            List<DependencyTreeResult.DependencyNode> deps = parser.parse(process);

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.warn("{} exited with code {} in directory: {}", cmdName, exitCode, projectDir);
                // Note: redirectErrorStream(true) merges stderr into stdout, so getErrorStream() is empty.
                // The parser already consumed stdout via process.getInputStream(). We check the parser's
                // output for diagnostic clues. If we got no dependencies, the exit code itself signals failure.
                if (deps.isEmpty()) {
                    return DependencyTreeResult.error(String.format(
                        "%s exited with code %d in %s. No dependencies were parsed.",
                        cmdName, exitCode, projectDir));
                }
            }

            return deps.isEmpty() ? DependencyTreeResult.empty() : new DependencyTreeResult(deps, Set.of());
        } catch (IOException e) {
            String errorMsg = e.getMessage();
            log.error("Failed to execute {}: {}", cmdName, errorMsg);
            
            // Provide more helpful error messages for common issues, especially missing executables
            String lower = errorMsg == null ? "" : errorMsg.toLowerCase();
            if (errorMsg == null || errorMsg.isEmpty() || lower.contains("cannot run program")
                || lower.contains("no such file or directory") || lower.contains("not found")) {
                String commandName = command.get(0);
                String toolName = cmdName.contains("Maven") ? "Maven" : "Gradle";
                return DependencyTreeResult.error(
                    commandName + " not found. Please install " + toolName + " or ensure a " + toolName +
                    " wrapper (mvnw/mvnw.bat) is available in the project directory.");
            }

            // Fallback: existing specific message for common wrapper issues
            if (errorMsg.contains("Cannot run program") && errorMsg.contains("CreateProcess error=2")) {
                String commandName = command.get(0);
                return DependencyTreeResult.error(String.format(
                    "%s command not found. Please install %s or ensure a %s wrapper (mvnw/mvnw.bat) is available in the project directory.", 
                    commandName, cmdName.contains("Maven") ? "Maven" : "Gradle", cmdName.contains("Maven") ? "Maven" : "Gradle"));
            }
            
            return DependencyTreeResult.error(errorMsg);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return DependencyTreeResult.error("Execution interrupted");
        } finally {
            if (process != null && process.isAlive()) process.destroyForcibly();
        }
    }

    private List<DependencyTreeResult.DependencyNode> parseMavenJsonOutput(Process process, Set<String> scopes) throws IOException {
        List<DependencyTreeResult.DependencyNode> deps = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
            }
            String jsonStr = extractJson(output.toString());
            if (jsonStr == null || jsonStr.isEmpty()) {
                return deps;
            }
            JsonNode root = objectMapper.readTree(jsonStr);
            if (root.isArray()) {
                root.forEach(node -> parseMavenJsonNode(node, deps, 0, null, null));
            } else {
                parseMavenJsonNode(root, deps, 0, null, null);
            }
        }
        return deps;
    }

    /**
     * Extracts a JSON object or array from text that may contain other output.
     * Finds the first '{' or '[' and returns the balanced substring.
     */
    private String extractJson(String text) {
        int start = -1;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{' || c == '[') {
                // Skip bracket constructs like [INFO], [WARN], [ERROR] that appear
                // in Maven log output before the actual JSON
                if (c == '[' && i + 1 < text.length() && Character.isLetter(text.charAt(i + 1))) {
                    continue;
                }
                start = i;
                break;
            }
        }
        if (start == -1) {
            return null;
        }

        boolean inString = false;
        boolean escape = false;
        int braceDepth = 0;
        int bracketDepth = 0;

        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (escape) {
                escape = false;
                continue;
            }
            if (c == '\\') {
                escape = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (!inString) {
                if (c == '{') braceDepth++;
                else if (c == '}') braceDepth--;
                else if (c == '[') bracketDepth++;
                else if (c == ']') bracketDepth--;

                if (braceDepth == 0 && bracketDepth == 0) {
                    return text.substring(start, i + 1);
                }
            }
        }
        return text.substring(start);
    }

    private void parseMavenJsonNode(JsonNode node, List<DependencyTreeResult.DependencyNode> deps, int depth, String parentScope, String parentArtifactKey) {
        if (deps.size() >= MAX_DEPENDENCIES) { log.warn("Max dependency limit reached"); return; }
        if (!node.has("groupId") || !node.has("artifactId")) return;

        String groupId = node.get("groupId").asText();
        String artifactId = node.get("artifactId").asText();
        String artifactKey = groupId + ":" + artifactId;

        String scope = node.has("scope") ? node.get("scope").asText() : (parentScope != null ? parentScope : "compile");
        deps.add(new DependencyTreeResult.DependencyNode(
            groupId,
            artifactId,
            node.has("version") ? node.get("version").asText() : "unknown",
            scope, depth, depth > 0, parentArtifactKey));

        if (node.has("children")) node.get("children").forEach(c -> parseMavenJsonNode(c, deps, depth + 1, scope, artifactKey));
    }



    @Override
    public void shutdown() {
        executor.shutdown();
        try { if (!executor.awaitTermination(5, TimeUnit.SECONDS)) executor.shutdownNow(); }
        catch (InterruptedException e) { executor.shutdownNow(); Thread.currentThread().interrupt(); }
        gradleToolingApiExecutor.shutdown();
    }

    /**
     * Check if Maven is available on the system.
     * Uses a 5-second timeout to avoid hanging.
     */
    public static boolean isMavenAvailable() {
        return isCommandAvailable("mvn", "--version", 5);
    }


    /**
     * Check if Maven is available for a given project directory.
     * Checks system Maven first, then looks for a Maven wrapper.
     */
    private static boolean isMavenAvailableForProject(Path projectDir) {
        if (isMavenAvailable()) return true;
        return findMavenWrapper(projectDir).isPresent();
    }


    /**
     * Find Maven wrapper in the project directory or its parents.
     * @param projectDir The project directory to search from
     * @return Optional Path to the Maven wrapper executable, empty if not found
     */
    private static Optional<Path> findMavenWrapper(Path projectDir) {
        if (projectDir == null) return Optional.empty();
        
        // Check for Maven wrapper in current directory and parent directories
        Path currentDir = projectDir;
        int maxDepth = 10; // Reasonable limit to prevent infinite loops
        int depth = 0;
        while (currentDir != null && depth < maxDepth) {
            // Check Unix-style wrapper
            Path mvnw = currentDir.resolve("mvnw");
            if (Files.exists(mvnw)) {
                log.debug("Found Maven wrapper: {}", mvnw);
                return Optional.of(mvnw);
            }
            
            // Check Windows-style wrapper
            Path mvnwBat = currentDir.resolve("mvnw.bat");
            if (Files.exists(mvnwBat)) {
                log.debug("Found Maven wrapper (Windows): {}", mvnwBat);
                return Optional.of(mvnwBat);
            }
            
            // Move to parent directory
            currentDir = currentDir.getParent();
            depth++;
            // Stop at filesystem root
            if (currentDir != null && currentDir.getNameCount() == 0) break;
        }
        
        return Optional.empty();
    }
    

    /**
     * Check if a command is available and executable.
     * @param command The command to check (e.g., "mvn", "gradle")
     * @param arg A simple argument to test (e.g., "--version")
     * @param timeoutSeconds Maximum seconds to wait
     * @return true if command executed successfully
     */
    private static boolean isCommandAvailable(String command, String arg, int timeoutSeconds) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command, arg);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                log.debug("Command '{}' check timed out after {} seconds", command, timeoutSeconds);
                return false;
            }

            int exitCode = process.exitValue();
            log.debug("Command '{}' check completed with exit code {}", command, exitCode);
            return exitCode == 0;
        } catch (IOException e) {
            log.debug("Command '{}' not available: {}: {}", command, e.getClass().getSimpleName(), e.getMessage());
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
