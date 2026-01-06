package com.bugbounty.jakartamigration.coderefactoring.service;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for downloading and caching external tools.
 * Handles downloading the Apache Tomcat Jakarta EE Migration Tool.
 */
@Slf4j
public class ToolDownloader {
    
    private static final String APACHE_TOMCAT_MIGRATION_REPO = "apache/tomcat-jakartaee-migration";
    private static final String APACHE_TOMCAT_MIGRATION_JAR_PATTERN = "jakartaee-migration-.*-shaded\\.jar";
    private static final String GITHUB_API_BASE = "https://api.github.com/repos/";
    private static final String GITHUB_RELEASES_BASE = "https://github.com/";
    
    /**
     * Downloads the Apache Tomcat migration tool if not already cached.
     *
     * @return Path to the downloaded/cached tool JAR
     * @throws IOException if download fails
     */
    public Path downloadApacheTomcatMigrationTool() throws IOException {
        Path cacheDir = getCacheDirectory();
        Path cachedJar = findCachedJar(cacheDir);
        
        if (cachedJar != null && Files.exists(cachedJar)) {
            log.info("Using cached Apache Tomcat migration tool: {}", cachedJar);
            return cachedJar;
        }
        
        log.info("Apache Tomcat migration tool not found in cache, downloading...");
        
        // Get latest release info
        String latestVersion = getLatestReleaseVersion();
        String downloadUrl = getDownloadUrl(latestVersion);
        
        // Download to cache directory
        Path downloadPath = cacheDir.resolve("jakartaee-migration-" + latestVersion + "-shaded.jar");
        downloadFile(downloadUrl, downloadPath);
        
        log.info("Apache Tomcat migration tool downloaded successfully to: {}", downloadPath);
        return downloadPath;
    }
    
    /**
     * Gets the cache directory for tools.
     * Uses platform-specific cache directories.
     */
    private Path getCacheDirectory() throws IOException {
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
        
        Files.createDirectories(cacheDir);
        return cacheDir;
    }
    
    /**
     * Finds an existing cached JAR file.
     */
    private Path findCachedJar(Path cacheDir) {
        if (!Files.exists(cacheDir)) {
            return null;
        }
        
        try {
            return Files.list(cacheDir)
                .filter(path -> path.getFileName().toString().matches(APACHE_TOMCAT_MIGRATION_JAR_PATTERN))
                .filter(Files::isRegularFile)
                .findFirst()
                .orElse(null);
        } catch (IOException e) {
            log.warn("Failed to search cache directory: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Gets the latest release version from GitHub API.
     */
    private String getLatestReleaseVersion() throws IOException {
        String apiUrl = GITHUB_API_BASE + APACHE_TOMCAT_MIGRATION_REPO + "/releases/latest";
        
        try {
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            
            // Follow redirects
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || 
                responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                String redirectUrl = connection.getHeaderField("Location");
                connection = (HttpURLConnection) new URL(redirectUrl).openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/json");
            }
            
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("Failed to get latest release: HTTP " + connection.getResponseCode());
            }
            
            try (InputStream is = connection.getInputStream()) {
                byte[] buffer = new byte[8192];
                StringBuilder response = new StringBuilder();
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    response.append(new String(buffer, 0, bytesRead));
                }
                
                String responseStr = response.toString();
                // Parse JSON to get tag_name
                Pattern versionPattern = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
                Matcher matcher = versionPattern.matcher(responseStr);
                if (matcher.find()) {
                    String version = matcher.group(1);
                    // Remove 'v' prefix if present
                    return version.startsWith("v") ? version.substring(1) : version;
                }
            }
            
            throw new IOException("Could not parse version from GitHub API response");
            
        } catch (Exception e) {
            log.warn("Failed to get latest version from GitHub API, using default: {}", e.getMessage());
            // Fallback to a known version
            return "1.0.0";
        }
    }
    
    /**
     * Gets the download URL for a specific version.
     */
    private String getDownloadUrl(String version) {
        // GitHub releases URL format
        return GITHUB_RELEASES_BASE + APACHE_TOMCAT_MIGRATION_REPO + 
               "/releases/download/v" + version + "/jakartaee-migration-" + version + "-shaded.jar";
    }
    
    /**
     * Downloads a file from a URL to a local path.
     */
    private void downloadFile(String urlString, Path destination) throws IOException {
        log.info("Downloading from: {}", urlString);
        
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(30000); // 30 seconds
        connection.setReadTimeout(60000); // 60 seconds
        connection.setInstanceFollowRedirects(true);
        
        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("Failed to download file: HTTP " + responseCode + " from " + urlString);
        }
        
        long contentLength = connection.getContentLengthLong();
        log.info("Downloading {} bytes...", contentLength > 0 ? contentLength : "unknown size");
        
        try (InputStream inputStream = new BufferedInputStream(connection.getInputStream());
             FileOutputStream outputStream = new FileOutputStream(destination.toFile())) {
            
            byte[] buffer = new byte[8192];
            long totalBytesRead = 0;
            int bytesRead;
            
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
                
                if (contentLength > 0 && totalBytesRead % (1024 * 1024) == 0) {
                    int percent = (int) ((totalBytesRead * 100) / contentLength);
                    log.debug("Download progress: {}%", percent);
                }
            }
        }
        
        log.info("Download completed: {} bytes", Files.size(destination));
    }
    
    /**
     * Gets the cache directory path (for external access).
     */
    public Path getCacheDirectoryPath() throws IOException {
        return getCacheDirectory();
    }
}

