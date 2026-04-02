package adrianmikula.jakartamigration.intellij.integration;

import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Core utility class for managing example projects in integration tests.
 * Handles downloading, extracting, and cleanup of example projects from examples.yaml.
 */
@Slf4j
public class ExampleProjectManager {
    
    private static final String EXAMPLES_YAML_PATH = "/examples.yaml";
    private static final String CACHE_DIR_NAME = ".downloads";
    private static final String EXTRACT_DIR_NAME = "extracted-projects";
    
    private final Path cacheDir;
    private final Path extractDir;
    private final Map<String, Map<String, Object>> examplesData;
    
    public ExampleProjectManager(Path tempDir) throws IOException {
        this.cacheDir = tempDir.resolve(CACHE_DIR_NAME);
        this.extractDir = tempDir.resolve(EXTRACT_DIR_NAME);
        
        Files.createDirectories(cacheDir);
        Files.createDirectories(extractDir);
        
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
        log.info("Getting example project: {} (type: {})", exampleName, exampleType);
        
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
        
        // Check if already extracted
        Path extractedDir = extractDir.resolve(safeProjectName);
        if (isProjectExtracted(extractedDir)) {
            log.info("Project {} already extracted, reusing", safeProjectName);
            return extractedDir;
        }
        
        // Download if not cached
        Path zipFile = cacheDir.resolve(safeProjectName + ".zip");
        if (!Files.exists(zipFile)) {
            log.info("Downloading project from {}", url);
            downloadProject(url, zipFile);
        } else {
            log.info("Using cached ZIP file for {}", safeProjectName);
        }
        
        // Extract project
        log.info("Extracting {} to {}", zipFile, extractedDir);
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
            log.warn("Project path {} is not within extract directory, skipping cleanup", projectPath);
            return;
        }
        
        try {
            log.info("Cleaning up extracted project: {}", projectPath);
            deleteDirectory(projectPath);
        } catch (IOException e) {
            log.error("Failed to cleanup extracted project: " + projectPath, e);
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
                        log.error("Failed to delete directory: " + path, e);
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
            
            if (value instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> examples = (List<Map<String, Object>>) value;
                available.put(type, examples);
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
        if (typeData instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> examples = (List<Map<String, Object>>) typeData;
            
            return examples.stream()
                .filter(example -> exampleName.equals(example.get("name")))
                .findFirst()
                .orElse(null);
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
            
            // Convert to the expected structure
            Map<String, Map<String, Object>> result = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (entry.getValue() instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> list = (List<Map<String, Object>>) entry.getValue();
                    result.put(entry.getKey(), Map.of("examples", list));
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
        String zipUrl = convertToZipUrl(urlString);
        
        log.info("Downloading from: {}", zipUrl);
        
        URL url = new URL(zipUrl);
        try (var inputStream = url.openStream()) {
            Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
        }
        
        log.info("Downloaded to: {}", targetFile);
    }
    
    /**
     * Converts GitHub repository URL to ZIP download URL.
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
                return "https://github.com/" + owner + "/" + repo + "/archive/refs/heads/main.zip";
            }
        }
        throw new IllegalArgumentException("Unsupported repository URL: " + repoUrl);
    }
    
    /**
     * Extracts ZIP file to target directory.
     */
    private void extractProject(Path zipFile, Path targetDir) throws IOException {
        // Clean target directory first
        if (Files.exists(targetDir)) {
            deleteDirectory(targetDir);
        }
        Files.createDirectories(targetDir);
        
        try (var zipInputStream = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                Path entryPath = targetDir.resolve(entry.getName());
                
                // Normalize path to prevent zip slip
                entryPath = entryPath.normalize();
                if (!entryPath.startsWith(targetDir)) {
                    log.warn("Skipping potentially malicious entry: {}", entry.getName());
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
        
        log.info("Extracted to: {}", targetDir);
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
                        log.error("Failed to delete file: " + path, e);
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
            log.error("Failed to get cache stats", e);
        }
        
        return stats;
    }
}
