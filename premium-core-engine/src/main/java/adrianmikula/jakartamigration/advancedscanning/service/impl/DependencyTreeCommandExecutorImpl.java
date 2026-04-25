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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Async implementation of DependencyTreeCommandExecutor with resource management.
 */
@Slf4j
public class DependencyTreeCommandExecutorImpl implements DependencyTreeCommandExecutor {

    private static final int MAX_DEPENDENCIES = 10000;
    private static final int THREAD_POOL_SIZE = 2;
    private static final Pattern DEP_PATTERN = Pattern.compile("^([^\\s:]+):([^\\s:]+):([^\\s]+)");

    private final ExecutorService executor;
    private final ObjectMapper objectMapper;

    public DependencyTreeCommandExecutorImpl() {
        this.executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public CompletableFuture<DependencyTreeResult> executeMavenDependencyTreeAsync(Path pomXmlPath, Set<String> scopes) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> command = buildMavenCommand(scopes, pomXmlPath.getParent());
            return executeCommand(command, pomXmlPath.getParent(), "mvn dependency:tree",
                process -> parseMavenJsonOutput(process, scopes));
        }, executor);
    }

    @Override
    public CompletableFuture<DependencyTreeResult> executeGradleDependenciesAsync(Path buildFilePath, Set<String> scopes) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> command = buildGradleCommand(scopes, buildFilePath.getParent());
            return executeCommand(command, buildFilePath.getParent(), "gradle dependencies",
                process -> parseGradleOutput(process, scopes));
        }, executor);
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
        if (!scopes.isEmpty()) cmd.add("-Dscope=" + String.join(",", scopes));
        return cmd;
    }

    private List<String> buildGradleCommand(Set<String> scopes, Path projectDir) {
        // First try to find Gradle wrapper
        Optional<Path> gradleWrapper = findGradleWrapper(projectDir);
        
        String gradleCommand;
        if (gradleWrapper.isPresent()) {
            gradleCommand = gradleWrapper.get().toString();
            log.debug("Using Gradle wrapper: {}", gradleCommand);
        } else {
            gradleCommand = "gradle";
            log.debug("Using system Gradle: {}", gradleCommand);
        }
        
        List<String> cmd = new ArrayList<>(List.of(gradleCommand, "dependencies", "--quiet", "--no-daemon"));
        scopes.forEach(s -> { cmd.add("--configuration"); cmd.add(s); });
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
                // Read error output for better diagnostics
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    StringBuilder errorOutput = new StringBuilder();
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorOutput.append(line).append("\n");
                    }
                    String errorMsg = errorOutput.toString().trim();
                    if (!errorMsg.isEmpty()) {
                        log.debug("Command error output: {}", errorMsg);
                        // Check for common error patterns
                        if (errorMsg.contains("not recognized") || errorMsg.contains("not found") || errorMsg.contains("cannot find the file")) {
                            return DependencyTreeResult.error(String.format(
                                "Command '%s' not found. Please install %s or ensure %s wrapper is available in project directory.", 
                                command.get(0), cmdName.contains("Maven") ? "Maven" : "Gradle", cmdName.contains("Maven") ? "Maven" : "Gradle"));
                        }
                    }
                }
            }

            return deps.isEmpty() ? DependencyTreeResult.empty() : new DependencyTreeResult(deps, Set.of());
        } catch (IOException e) {
            String errorMsg = e.getMessage();
            log.error("Failed to execute {}: {}", cmdName, errorMsg);
            
            // Provide more helpful error messages for common issues
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
            StringBuilder json = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().startsWith("{") || line.trim().startsWith("[")) json.append(line);
            }
            if (json.length() > 0) parseMavenJsonNode(objectMapper.readTree(json.toString()), deps, 0, null);
        }
        return deps;
    }

    private void parseMavenJsonNode(JsonNode node, List<DependencyTreeResult.DependencyNode> deps, int depth, String parentScope) {
        if (deps.size() >= MAX_DEPENDENCIES) { log.warn("Max dependency limit reached"); return; }
        if (!node.has("groupId") || !node.has("artifactId")) return;

        String scope = node.has("scope") ? node.get("scope").asText() : (parentScope != null ? parentScope : "compile");
        deps.add(new DependencyTreeResult.DependencyNode(
            node.get("groupId").asText(),
            node.get("artifactId").asText(),
            node.has("version") ? node.get("version").asText() : "unknown",
            scope, depth, depth > 0));

        if (node.has("children")) node.get("children").forEach(c -> parseMavenJsonNode(c, deps, depth + 1, scope));
    }

    private List<DependencyTreeResult.DependencyNode> parseGradleOutput(Process process, Set<String> scopes) throws IOException {
        List<DependencyTreeResult.DependencyNode> deps = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line, scope = "compile";
            while ((line = reader.readLine()) != null && deps.size() < MAX_DEPENDENCIES) {
                if (line.trim().isEmpty() || line.startsWith("\\")) continue;
                if (line.endsWith("Classpath") || line.endsWith("Configuration")) {
                    scope = line.trim().split(" ")[0]; continue;
                }
                parseGradleLine(line, scope).ifPresent(deps::add);
            }
        }
        return deps;
    }

    private Optional<DependencyTreeResult.DependencyNode> parseGradleLine(String line, String scope) {
        int depth = 0;
        String trimmed = line;
        while (trimmed.startsWith("\\") || trimmed.startsWith("+")) {
            depth++; trimmed = trimmed.substring(1).trim();
        }
        Matcher m = DEP_PATTERN.matcher(trimmed);
        if (!m.find()) return Optional.empty();
        return Optional.of(new DependencyTreeResult.DependencyNode(
            m.group(1), m.group(2), m.group(3), scope, depth, depth > 0));
    }

    @Override
    public void shutdown() {
        executor.shutdown();
        try { if (!executor.awaitTermination(5, TimeUnit.SECONDS)) executor.shutdownNow(); }
        catch (InterruptedException e) { executor.shutdownNow(); Thread.currentThread().interrupt(); }
    }

    /**
     * Check if Maven is available on the system.
     * Uses a 5-second timeout to avoid hanging.
     */
    public static boolean isMavenAvailable() {
        return isCommandAvailable("mvn", "--version", 5);
    }

    /**
     * Check if Gradle is available on the system.
     * Uses a 5-second timeout to avoid hanging.
     */
    public static boolean isGradleAvailable() {
        return isCommandAvailable("gradle", "--version", 5);
    }

    /**
     * Find Maven wrapper in the project directory or its parents.
     * @param projectDir The project directory to search from
     * @return Optional Path to the Maven wrapper executable, empty if not found
     */
    private Optional<Path> findMavenWrapper(Path projectDir) {
        if (projectDir == null) return Optional.empty();
        
        // Check for Maven wrapper in current directory and parent directories
        Path currentDir = projectDir;
        while (currentDir != null) {
            // Check Unix-style wrapper
            Path mvnw = currentDir.resolve("mvnw");
            if (Files.isExecutable(mvnw)) {
                log.debug("Found Maven wrapper: {}", mvnw);
                return Optional.of(mvnw);
            }
            
            // Check Windows-style wrapper
            Path mvnwBat = currentDir.resolve("mvnw.bat");
            if (Files.isExecutable(mvnwBat)) {
                log.debug("Found Maven wrapper (Windows): {}", mvnwBat);
                return Optional.of(mvnwBat);
            }
            
            // Move to parent directory
            currentDir = currentDir.getParent();
            // Stop at filesystem root or after reasonable depth
            if (currentDir == null || currentDir.getNameCount() < 2) break;
        }
        
        return Optional.empty();
    }
    
    /**
     * Find Gradle wrapper in the project directory or its parents.
     * @param projectDir The project directory to search from
     * @return Optional Path to the Gradle wrapper executable, empty if not found
     */
    private Optional<Path> findGradleWrapper(Path projectDir) {
        if (projectDir == null) return Optional.empty();
        
        // Check for Gradle wrapper in current directory and parent directories
        Path currentDir = projectDir;
        while (currentDir != null) {
            // Check Unix-style wrapper
            Path gradlew = currentDir.resolve("gradlew");
            if (Files.isExecutable(gradlew)) {
                log.debug("Found Gradle wrapper: {}", gradlew);
                return Optional.of(gradlew);
            }
            
            // Check Windows-style wrapper
            Path gradlewBat = currentDir.resolve("gradlew.bat");
            if (Files.isExecutable(gradlewBat)) {
                log.debug("Found Gradle wrapper (Windows): {}", gradlewBat);
                return Optional.of(gradlewBat);
            }
            
            // Move to parent directory
            currentDir = currentDir.getParent();
            // Stop at filesystem root or after reasonable depth
            if (currentDir == null || currentDir.getNameCount() < 2) break;
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
            log.debug("Command '{}' not available: {}", command, e.getMessage());
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
