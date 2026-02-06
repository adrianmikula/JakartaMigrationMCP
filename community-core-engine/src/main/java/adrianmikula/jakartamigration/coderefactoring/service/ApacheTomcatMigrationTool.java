package adrianmikula.jakartamigration.coderefactoring.service;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Service for invoking the Apache Tomcat Jakarta EE Migration Tool.
 * 
 * IMPORTANT: This tool works on COMPILED JAR/WAR files (bytecode), NOT source code.
 * 
 * Use Cases for AI Agents Working on J2EE/Jakarta Migration:
 * 
 * 1. COMPATIBILITY TESTING:
 *    - Migrate a compiled JAR/WAR to Jakarta namespace
 *    - Run the migrated application to test compatibility
 *    - Identify libraries that fail after migration and need replacement
 *    - Discover runtime issues before migrating source code
 * 
 * 2. BYTECODE DIFF & VALIDATION:
 *    - Compare bytecode before/after migration
 *    - Cross-check against our bytecode analysis to ensure completeness
 *    - Verify all javax.* references were migrated (including in string constants)
 *    - Validate that nothing was missed in source code analysis
 * 
 * 3. THIRD-PARTY LIBRARY ASSESSMENT:
 *    - Test if third-party JARs are Jakarta-compatible
 *    - Identify which dependencies need Jakarta-compatible alternatives
 *    - Check for binary incompatibilities in library bytecode
 * 
 * What This Tool Migrates (in compiled bytecode):
 * - Package references: javax.* → jakarta.* in class files
 * - String constants: javax.* references in string literals
 * - Embedded config files: web.xml, persistence.xml, faces-config.xml, etc.
 * - JSP files and TLD files within WAR files
 * - All Java EE 8 packages to Jakarta EE 9 equivalents
 * 
 * What This Tool Does NOT Do:
 * - Does NOT work on Java source code files (.java)
 * - Does NOT modify your original source code
 * - Does NOT update Maven/Gradle dependencies (only bytecode)
 * 
 * Note: This tool removes cryptographic signatures from JAR files as bytecode changes
 * invalidate signatures. This is expected and necessary behavior.
 * 
 * @see <a href="https://github.com/apache/tomcat-jakartaee-migration">Apache Tomcat Migration Tool</a>
 */
@Slf4j
public class ApacheTomcatMigrationTool {
    
    private static final long DEFAULT_TIMEOUT_SECONDS = 300; // 5 minutes
    
    private Path toolJarPath;  // Lazy initialization - only resolved when needed
    private final long timeoutSeconds;
    
    /**
     * Creates a new instance with default tool location.
     * The tool JAR will be found/downloaded lazily on first use, not at construction time.
     * This prevents startup delays and failures if the tool is not available.
     */
    public ApacheTomcatMigrationTool() {
        this.toolJarPath = null;  // Will be resolved lazily when migrate() is called
        this.timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
    }
    
    /**
     * Creates a new instance with a specific tool JAR path.
     *
     * @param toolJarPath Path to the jakartaee-migration-*-shaded.jar file
     */
    public ApacheTomcatMigrationTool(Path toolJarPath) {
        if (toolJarPath == null || !Files.exists(toolJarPath)) {
            throw new IllegalArgumentException("Tool JAR path must exist: " + toolJarPath);
        }
        this.toolJarPath = toolJarPath;  // Explicitly provided, use immediately
        this.timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
    }
    
    /**
     * Creates a new instance with a specific tool JAR path and timeout.
     *
     * @param toolJarPath Path to the jakartaee-migration-*-shaded.jar file
     * @param timeoutSeconds Timeout in seconds for the migration process
     */
    public ApacheTomcatMigrationTool(Path toolJarPath, long timeoutSeconds) {
        if (toolJarPath == null || !Files.exists(toolJarPath)) {
            throw new IllegalArgumentException("Tool JAR path must exist: " + toolJarPath);
        }
        this.toolJarPath = toolJarPath;  // Explicitly provided, use immediately
        this.timeoutSeconds = timeoutSeconds;
    }
    
    /**
     * Migrates a source file or directory to Jakarta EE using the Apache Tomcat migration tool.
     *
     * @param sourcePath Path to the source file, directory, or archive
     * @param destinationPath Path where the migrated content will be written
     * @return Migration result with success status and output
     * @throws IOException if the migration process fails
     */
    public MigrationResult migrate(Path sourcePath, Path destinationPath) throws IOException {
        if (sourcePath == null || !Files.exists(sourcePath)) {
            throw new IllegalArgumentException("Source path must exist: " + sourcePath);
        }
        if (destinationPath == null) {
            throw new IllegalArgumentException("Destination path cannot be null");
        }
        
        // Lazy initialization: find/download tool only when actually needed
        if (toolJarPath == null) {
            toolJarPath = findToolJar();
        }
        
        if (toolJarPath == null) {
            throw new IllegalStateException(
                "Apache Tomcat migration tool JAR not found. " +
                "Please set JAKARTA_MIGRATION_TOOL_PATH environment variable or " +
                "download from https://tomcat.apache.org/download-migration.cgi"
            );
        }
        
        log.info("Starting Apache Tomcat migration: {} -> {}", sourcePath, destinationPath);
        log.info("Using migration tool: {}", toolJarPath);
        
        // Build command: java -jar jakartaee-migration-*-shaded.jar <source> <destination>
        List<String> command = new ArrayList<>();
        command.add("java");
        command.add("-jar");
        command.add(toolJarPath.toAbsolutePath().toString());
        command.add(sourcePath.toAbsolutePath().toString());
        command.add(destinationPath.toAbsolutePath().toString());
        
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(false);
        
        List<String> stdout = new ArrayList<>();
        List<String> stderr = new ArrayList<>();
        int exitCode = -1;
        boolean timedOut = false;
        
        Process process = null;
        long startTime = System.currentTimeMillis();
        
        try {
            process = processBuilder.start();
            
            // Capture stdout
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stdout.add(line);
                    log.debug("Migration tool stdout: {}", line);
                }
            }
            
            // Capture stderr
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stderr.add(line);
                    log.warn("Migration tool stderr: {}", line);
                }
            }
            
            // Wait for process with timeout
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                timedOut = true;
                log.error("Migration tool timed out after {} seconds", timeoutSeconds);
            } else {
                exitCode = process.exitValue();
                log.info("Migration tool completed with exit code: {}", exitCode);
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (process != null) {
                process.destroyForcibly();
            }
            throw new IOException("Migration process interrupted", e);
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
        
        long duration = System.currentTimeMillis() - startTime;
        boolean success = exitCode == 0 && !timedOut && Files.exists(destinationPath);
        
        if (success) {
            log.info("Migration completed successfully in {} ms", duration);
        } else {
            log.error("Migration failed. Exit code: {}, Timed out: {}", exitCode, timedOut);
        }
        
        return new MigrationResult(
            success,
            exitCode,
            timedOut,
            stdout,
            stderr,
            duration
        );
    }
    
    /**
     * Checks if the migration tool is available.
     * This will trigger lazy initialization if the tool hasn't been found yet.
     *
     * @return true if the tool JAR is found and accessible
     */
    public boolean isAvailable() {
        if (toolJarPath == null) {
            toolJarPath = findToolJar();
        }
        return toolJarPath != null && Files.exists(toolJarPath);
    }
    
    /**
     * Gets the path to the migration tool JAR.
     * This will trigger lazy initialization if the tool hasn't been found yet.
     *
     * @return Path to the tool JAR, or null if not found
     */
    public Path getToolJarPath() {
        if (toolJarPath == null) {
            toolJarPath = findToolJar();
        }
        return toolJarPath;
    }
    
    /**
     * Finds the migration tool JAR in the system, or downloads it if not found.
     * This is called lazily on first use, not at construction time, to prevent startup delays.
     * 
     * Checks:
     * 1. Cache directory (checks for existing cached tool, downloads only if missing)
     * 2. Downloads directly from Apache website (no environment variables required)
     * 3. Common installation locations (for manually installed tools)
     *
     * @return Path to the tool JAR, or null if not found and download fails
     */
    private Path findToolJar() {
        // Check cache directory first (without downloading)
        Path cacheDir = getCacheDirectory();
        if (cacheDir != null) {
            Path cachedJar = findCachedJar(cacheDir);
            if (cachedJar != null && Files.exists(cachedJar)) {
                log.info("Using cached Apache Tomcat migration tool: {}", cachedJar);
                return cachedJar;
            }
        }
        
        // Download directly from Apache website (lazy download on demand)
        // No environment variables required - downloads from official Apache sources
        try {
            log.info("Apache Tomcat migration tool not found in cache, downloading from Apache website...");
            ToolDownloader downloader = new ToolDownloader();
            Path downloadedJar = downloader.downloadApacheTomcatMigrationTool();
            if (downloadedJar != null && Files.exists(downloadedJar)) {
                log.info("Apache Tomcat migration tool downloaded successfully from Apache website: {}", downloadedJar);
                return downloadedJar;
            }
        } catch (IOException e) {
            log.warn("Failed to download migration tool from Apache website: {}", e.getMessage());
            // Continue to check other locations
        }
        
        // Check common installation locations (for manually installed tools)
        String[] commonPaths = {
            System.getProperty("user.home") + "/.local/share/jakartaee-migration",
            "/usr/share/java",
            "/opt/jakartaee-migration",
            "."
        };
        
        for (String commonPath : commonPaths) {
            Path dir = Paths.get(commonPath);
            if (Files.exists(dir) && Files.isDirectory(dir)) {
                try {
                    Path found = Files.list(dir)
                        .filter(path -> path.getFileName().toString().matches("jakartaee-migration-.*-shaded\\.jar"))
                        .filter(Files::isRegularFile)
                        .findFirst()
                        .orElse(null);
                    
                    if (found != null) {
                        log.info("Found migration tool at: {}", found);
                        return found;
                    }
                } catch (IOException e) {
                    // Continue searching
                }
            }
        }
        
        log.error("Apache Tomcat migration tool JAR not found and download from Apache website failed. " +
                 "You can manually download from https://tomcat.apache.org/download-migration.cgi " +
                 "or set JAKARTA_MIGRATION_TOOL_PATH environment variable to point to the tool JAR");
        
        return null;
    }
    
    /**
     * Gets the cache directory for tools (without creating it).
     * Returns null if the directory doesn't exist.
     */
    private Path getCacheDirectory() {
        try {
            String osName = System.getProperty("os.name", "").toLowerCase();
            Path cacheDir;
            
            if (osName.contains("win")) {
                // Windows: %USERPROFILE%\AppData\Local\jakarta-migration-tools
                String userHome = System.getProperty("user.home");
                cacheDir = Paths.get(userHome, "AppData", "Local", "jakarta-migration-tools");
            } else {
                // Linux/macOS: ~/.cache/jakarta-migration-tools
                String userHome = System.getProperty("user.home");
                cacheDir = Paths.get(userHome, ".cache", "jakarta-migration-tools");
            }
            
            // Only return if directory exists (don't create it here)
            return Files.exists(cacheDir) ? cacheDir : null;
        } catch (Exception e) {
            log.debug("Could not determine cache directory: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Finds an existing cached JAR file in the cache directory.
     */
    private Path findCachedJar(Path cacheDir) {
        if (cacheDir == null || !Files.exists(cacheDir)) {
            return null;
        }
        
        try {
            return Files.list(cacheDir)
                .filter(path -> path.getFileName().toString().matches("jakartaee-migration-.*-shaded\\.jar"))
                .filter(Files::isRegularFile)
                .findFirst()
                .orElse(null);
        } catch (IOException e) {
            log.debug("Failed to search cache directory: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Result of a migration operation.
     */
    public record MigrationResult(
        boolean success,
        int exitCode,
        boolean timedOut,
        List<String> stdout,
        List<String> stderr,
        long durationMs
    ) {
        /**
         * Returns a summary message of the migration result.
         */
        public String getSummary() {
            if (success) {
                return String.format("Migration completed successfully in %d ms", durationMs);
            } else if (timedOut) {
                return "Migration timed out";
            } else {
                return String.format("Migration failed with exit code %d", exitCode);
            }
        }
    }
}

