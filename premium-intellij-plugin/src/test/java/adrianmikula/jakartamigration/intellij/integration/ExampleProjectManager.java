package adrianmikula.jakartamigration.intellij.integration;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.logging.Logger;

/**
 * Core utility class for managing example projects in integration tests.
 * Handles downloading, extracting, and cleanup of example projects from examples.yaml.
 */
public class ExampleProjectManager {
    
    private static final Logger log = Logger.getLogger(ExampleProjectManager.class.getName());
    private static final String EXAMPLES_YAML_PATH = "/examples.yaml";
    private static final String CACHE_DIR_NAME = ".downloads";
    private static final String EXTRACT_DIR_NAME = "extracted-projects";
    
    private final Path cacheDir;
    private final Path extractDir;
    private final Map<String, Map<String, Object>> examplesData;
    
    public ExampleProjectManager(Path tempDir) throws IOException {
        log.info("Initializing ExampleProjectManager with temp dir: " + tempDir);
        
        // Use consistent cache directory name for easier access
        Path cacheDir = tempDir.resolve("jakarta-migration/.downloads");
        Path extractDir = tempDir.resolve("jakarta-migration/extracted-projects");
        
        Files.createDirectories(cacheDir);
        Files.createDirectories(extractDir);
        
        this.cacheDir = cacheDir;
        this.extractDir = extractDir;
        
        log.info("Cache directory: " + this.cacheDir);
        log.info("Extract directory: " + this.extractDir);
        
        // Load examples.yaml
        this.examplesData = loadExamplesYaml();
    }
    
    /**
     * Gets an example project by name and type, downloading if necessary.
     * 
     * @param exampleName The name of the example from examples.yaml
     * @param exampleType The type category (application_servers, javax_packages, etc.)
     * @return Path to the extracted project directory
     * @throws IOException If download/extraction fails
     */
    public Path getExampleProject(String exampleName, String exampleType) throws IOException {
        log.info("Getting example project: " + exampleName + " (type: " + exampleType + ")");
        
        // Find the example in YAML data
        Map<String, Object> example = findExample(exampleName, exampleType);
        if (example == null) {
            throw new IllegalArgumentException("Example not found: " + exampleName + " in type: " + exampleType);
        }
        
        String url = (String) example.get("url");
        if (url == null || url.contains("?/?")) {
            throw new IllegalArgumentException("Invalid or placeholder URL for example: " + exampleName);
        }
        
        // Generate safe project name
        String safeProjectName = generateSafeProjectName(exampleName, exampleType);
        log.info("Generated safe project name: " + safeProjectName);
        
        // Check if already extracted
        Path extractedDir = extractDir.resolve(safeProjectName);
        log.info("Extracted directory path: " + extractedDir);
        if (isProjectExtracted(extractedDir)) {
            log.info("Project " + safeProjectName + " already extracted, reusing");
            return extractedDir;
        }
        
        // Download if not cached
        Path zipFile = cacheDir.resolve(safeProjectName + ".zip");
        log.info("ZIP file path: " + zipFile);
        if (!Files.exists(zipFile)) {
            log.info("Downloading project from " + url);
            downloadProject(url, zipFile);
        } else {
            log.info("Using cached ZIP file for " + safeProjectName);
        }
        
        // Extract project
        log.info("Extracting " + zipFile + " to " + extractedDir);
        extractProject(zipFile, extractedDir);
        
        return extractedDir;
    }
    
    /**
     * Cleans up the extracted project directory but retains cached ZIP files.
     * 
     * @param projectPath The path to the extracted project to clean up
     * @throws IOException If cleanup fails
     */
    public void cleanupExtractedProject(Path projectPath) throws IOException {
        if (projectPath == null || !Files.exists(projectPath)) {
            return;
        }
        
        // Only clean if it's within our extract directory
        if (!projectPath.startsWith(extractDir)) {
            log.warning("Project path " + projectPath + " is not within extract directory, skipping cleanup");
            return;
        }
        
        try {
            log.info("Cleaning up extracted project: " + projectPath);
            deleteDirectory(projectPath);
        } catch (IOException e) {
            log.severe("Failed to cleanup extracted project: " + projectPath + " - " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Cleans up all extracted projects but retains cached ZIP files.
     * 
     * @throws IOException If cleanup fails
     */
    public void cleanupAllExtractedProjects() throws IOException {
        log.info("Cleaning up all extracted projects");
        
        if (Files.exists(extractDir)) {
            try (var stream = Files.list(extractDir)) {
                stream.forEach(path -> {
                    try {
                        deleteDirectory(path);
                    } catch (IOException e) {
                        log.severe("Failed to delete directory: " + path + " - " + e.getMessage());
                    }
                });
            }
        }
    }
    
    /**
     * Gets information about available examples.
     */
    public Map<String, List<Map<String, Object>>> getAvailableExamples() {
        Map<String, List<Map<String, Object>>> available = new LinkedHashMap<>();
        
        for (Map.Entry<String, Map<String, Object>> entry : examplesData.entrySet()) {
            String type = entry.getKey();
            Object value = entry.getValue();
            
            // Check if the value is a List (project_complexity, etc.) or a Map with "examples" key
            if (value instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> examples = (List<Map<String, Object>>) value;
                available.put(type, examples);
            } else if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> valueMap = (Map<String, Object>) value;
                if (valueMap.containsKey("examples")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> examples = (List<Map<String, Object>>) valueMap.get("examples");
                    available.put(type, examples);
                }
            }
        }
        
        return available;
    }
    
    /**
     * Checks if a project has been properly extracted.
     */
    private boolean isProjectExtracted(Path projectDir) {
        if (!Files.exists(projectDir) || !Files.isDirectory(projectDir)) {
            return false;
        }
        
        // Check for common project indicators
        return Files.exists(projectDir.resolve("pom.xml")) ||
               Files.exists(projectDir.resolve("build.gradle")) ||
               Files.exists(projectDir.resolve("build.gradle.kts")) ||
               Files.exists(projectDir.resolve("src"));
    }
    
    /**
     * Finds an example in the YAML data by name and type.
     */
    private Map<String, Object> findExample(String exampleName, String exampleType) {
        Map<String, Object> typeData = examplesData.get(exampleType);
        if (typeData != null) {
            // Get the examples list from the wrapper map
            Object examplesObj = typeData.get("examples");
            if (examplesObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> examples = (List<Map<String, Object>>) examplesObj;
                
                return examples.stream()
                    .filter(example -> exampleName.equals(example.get("name")))
                    .findFirst()
                    .orElse(null);
            }
        }
        return null;
    }
    
    /**
     * Generates a safe project name for file system use.
     */
    private String generateSafeProjectName(String exampleName, String exampleType) {
        // Replace spaces and special characters with underscores
        String safeName = exampleName.toLowerCase()
            .replaceAll("[^a-zA-Z0-9]", "_")
            .replaceAll("_+", "_")
            .replaceAll("^_|_$", "");
        
        return exampleType + "_" + safeName;
    }
    
    /**
     * Loads the examples.yaml file.
     */
    private Map<String, Map<String, Object>> loadExamplesYaml() throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(EXAMPLES_YAML_PATH)) {
            if (inputStream == null) {
                throw new IOException("examples.yaml not found in classpath");
            }
            
            Yaml yaml = new Yaml();
            @SuppressWarnings("unchecked")
            Map<String, Object> data = yaml.load(inputStream);
            
            // Convert to the expected structure - keep lists as they are for getAvailableExamples()
            Map<String, Map<String, Object>> result = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (entry.getValue() instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> list = (List<Map<String, Object>>) entry.getValue();
                    // Store the list in a wrapper Map to maintain the Map<String, Map<String, Object>> structure
                    result.put(entry.getKey(), Map.of("examples", list));
                } else {
                    // Handle non-list entries if any
                    result.put(entry.getKey(), Map.of("data", entry.getValue()));
                }
            }
            
            return result;
        } catch (Exception e) {
            throw new IOException("Failed to load examples.yaml", e);
        }
    }
    
    /**
     * Downloads a project from URL to ZIP file.
     */
    private void downloadProject(String urlString, Path targetFile) throws IOException {
        log.info("Downloading from: " + urlString);
        
        if (urlString.startsWith("file://")) {
            // Handle local file URLs
            String sourcePath = urlString.substring(7); // Remove "file://"
            // Convert forward slashes to backslashes for Windows paths
            sourcePath = sourcePath.replace('/', '\\');
            Path sourceDir = Paths.get(sourcePath);
            
            if (!Files.exists(sourceDir)) {
                log.info("Source directory does not exist: " + sourcePath + ", creating minimal test project");
                // Create a minimal test project on the fly
                sourceDir = createMinimalTestProject(targetFile.getParent().resolve("test-project"));
            }
            
            // Create a ZIP file from the local directory
            createZipFromDirectory(sourceDir, targetFile);
        } else {
            // Handle remote URLs (GitHub, etc.)
            String zipUrl = convertToZipUrl(urlString);
            URL url = new URL(zipUrl);
            try (var inputStream = url.openStream()) {
                Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        
        log.info("Downloaded to: " + targetFile);
    }
    
    /**
     * Converts GitHub repository URL to ZIP download URL.
     * Tries both 'main' and 'master' branches to handle different repository configurations.
     */
    private String convertToZipUrl(String repoUrl) {
        if (repoUrl.contains("github.com")) {
            // Extract owner and repo name
            String[] parts = repoUrl.split("/");
            if (parts.length >= 5) {
                String owner = parts[3];
                String repo = parts[4];
                // Remove .git if present
                repo = repo.replace(".git", "");
                
                // Try main branch first, then master as fallback
                String mainUrl = "https://github.com/" + owner + "/" + repo + "/archive/refs/heads/main.zip";
                String masterUrl = "https://github.com/" + owner + "/" + repo + "/archive/refs/heads/master.zip";
                
                // Check which branch exists by trying to access the URL
                try {
                    HttpURLConnection connection = (HttpURLConnection) new URL(mainUrl).openConnection();
                    connection.setRequestMethod("HEAD");
                    connection.setConnectTimeout(5000);
                    connection.connect();
                    int responseCode = connection.getResponseCode();
                    if (responseCode == 200) {
                        return mainUrl;
                    }
                } catch (Exception e) {
                    // Main branch doesn't exist, try master
                }
                
                return masterUrl;
            }
        }
        throw new IllegalArgumentException("Unsupported repository URL: " + repoUrl);
    }
    
    /**
     * Extracts ZIP file to target directory.
     * If ZIP contains a single top-level directory, extracts its contents directly.
     */
    private void extractProject(Path zipFile, Path targetDir) throws IOException {
        // Clean target directory first
        if (Files.exists(targetDir)) {
            deleteDirectory(targetDir);
        }
        Files.createDirectories(targetDir);
        
        // First pass: check if ZIP has a single top-level directory
        String topLevelDir = null;
        try (var zipInputStream = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    String[] parts = entry.getName().split("/");
                    if (parts.length > 1) {
                        if (topLevelDir == null) {
                            topLevelDir = parts[0];
                        } else if (!topLevelDir.equals(parts[0])) {
                            // Multiple top-level directories found, don't flatten
                            topLevelDir = null;
                            break;
                        }
                    }
                }
            }
        }
        
        log.info("ZIP extraction mode: " + (topLevelDir != null ? "flatten single top-level directory" : "preserve structure"));
        
        // Second pass: extract with optional flattening
        try (var zipInputStream = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                String entryName = entry.getName();
                
                // If we have a single top-level directory, remove it from the path
                if (topLevelDir != null && entryName.startsWith(topLevelDir + "/")) {
                    entryName = entryName.substring(topLevelDir.length() + 1);
                }
                
                // Skip empty entries
                if (entryName.isEmpty()) {
                    continue;
                }
                
                Path entryPath = targetDir.resolve(entryName);
                
                // Normalize path to prevent zip slip
                entryPath = entryPath.normalize();
                if (!entryPath.startsWith(targetDir)) {
                    log.warning("Skipping potentially malicious entry: " + entry.getName());
                    continue;
                }
                
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    Files.copy(zipInputStream, entryPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        
        log.info("Extracted to: " + targetDir);
    }
    
    /**
     * Recursively deletes a directory.
     */
    private void deleteDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        
        try (var stream = Files.walk(directory)) {
            stream.sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        log.severe("Failed to delete file: " + path + " - " + e.getMessage());
                    }
                });
        }
    }
    
    /**
     * Gets cache statistics.
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Count cached ZIP files
            long zipCount = 0;
            long zipSize = 0;
            if (Files.exists(cacheDir)) {
                try (var stream = Files.list(cacheDir)) {
                    var paths = stream.toList();
                    zipCount = paths.size();
                    zipSize = paths.stream()
                        .mapToLong(path -> {
                            try {
                                return Files.size(path);
                            } catch (IOException e) {
                                return 0;
                            }
                        })
                        .sum();
                }
            }
            
            // Count extracted projects
            long extractedCount = 0;
            if (Files.exists(extractDir)) {
                try (var stream = Files.list(extractDir)) {
                    extractedCount = stream.count();
                }
            }
            
            stats.put("cachedZipFiles", zipCount);
            stats.put("cacheSizeBytes", zipSize);
            stats.put("extractedProjects", extractedCount);
            stats.put("cacheDir", cacheDir.toString());
            stats.put("extractDir", extractDir.toString());
            
        } catch (IOException e) {
            log.severe("Failed to get cache stats - " + e.getMessage());
        }
        
        return stats;
    }
    
    /**
     * Creates a ZIP file from a local directory.
     */
    private void createZipFromDirectory(Path sourceDir, Path targetZip) throws IOException {
        try (var zipOutputStream = new java.util.zip.ZipOutputStream(Files.newOutputStream(targetZip))) {
            Files.walk(sourceDir)
                .filter(path -> !Files.isDirectory(path))
                .forEach(path -> {
                    try {
                        String entryName = sourceDir.relativize(path).toString().replace('\\', '/');
                        var zipEntry = new java.util.zip.ZipEntry(entryName);
                        zipOutputStream.putNextEntry(zipEntry);
                        Files.copy(path, zipOutputStream);
                        zipOutputStream.closeEntry();
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to add file to ZIP: " + path, e);
                    }
                });
        }
    }
    
    /**
     * Creates a minimal test project with javax packages for integration testing.
     */
    private Path createMinimalTestProject(Path projectDir) throws IOException {
        Files.createDirectories(projectDir);
        
        // Create pom.xml
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
                     http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>test-project</artifactId>
                <version>1.0.0</version>
                <dependencies>
                    <dependency>
                        <groupId>javax.servlet</groupId>
                        <artifactId>javax.servlet-api</artifactId>
                        <version>4.0.1</version>
                    </dependency>
                    <dependency>
                        <groupId>javax.persistence</groupId>
                        <artifactId>javax.persistence-api</artifactId>
                        <version>2.2</version>
                    </dependency>
                    <dependency>
                        <groupId>javax.validation</groupId>
                        <artifactId>validation-api</artifactId>
                        <version>2.0.1.Final</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        
        Files.write(projectDir.resolve("pom.xml"), pomContent.getBytes());
        
        // Create src/main/java directory structure
        Path srcDir = projectDir.resolve("src/main/java/com/example");
        Files.createDirectories(srcDir);
        
        // Create a simple Java file with javax imports
        String javaContent = """
            package com.example;
            
            import javax.servlet.ServletException;
            import javax.servlet.http.HttpServlet;
            import javax.persistence.Entity;
            import javax.persistence.Id;
            import javax.validation.constraints.NotNull;
            
            @Entity
            public class TestServlet extends HttpServlet {
                @Id
                private Long id;
                
                @NotNull
                private String name;
                
                public Long getId() { return id; }
                public void setId(Long id) { this.id = id; }
                public String getName() { return name; }
                public void setName(String name) { this.name = name; }
            }
            """;
        
        Files.write(srcDir.resolve("TestServlet.java"), javaContent.getBytes());
        
        log.info("Created minimal test project at: " + projectDir);
        return projectDir;
    }
}
