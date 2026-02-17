package adrianmikula.jakartamigration.coderefactoring.service;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Service for downloading and caching external tools.
 * Handles downloading the Apache Tomcat Jakarta EE Migration Tool.
 */
@Slf4j
public class ToolDownloader {
    
    private static final String APACHE_TOMCAT_MIGRATION_JAR_PATTERN = "jakartaee-migration-.*-shaded\\.jar";
    
    // Direct download URLs from Apache website (trying multiple sources for reliability)
    private static final String[] APACHE_DOWNLOAD_URLS = {
        // Primary: Apache archive (official distribution)
        "https://archive.apache.org/dist/tomcat/tomcat-10/v10.1.20/bin/extras/jakartaee-migration-1.0.0-shaded.jar",
        // Fallback: GitHub releases (Apache hosts releases here)
        "https://github.com/apache/tomcat-jakartaee-migration/releases/download/v1.0.0/jakartaee-migration-1.0.0-shaded.jar",
        // Alternative: Direct from Apache Tomcat extras
        "https://dlcdn.apache.org/tomcat/tomcat-10/v10.1.20/bin/extras/jakartaee-migration-1.0.0-shaded.jar"
    };
    
    private static final String DEFAULT_VERSION = "1.0.0";
    
    /**
     * Downloads the Apache Tomcat migration tool if not already cached.
     * Downloads directly from Apache website - no environment variables required.
     *
     * @return Path to the downloaded/cached tool JAR
     * @throws IOException if download fails from all sources
     */
    public Path downloadApacheTomcatMigrationTool() throws IOException {
        Path cacheDir = getCacheDirectory();
        Path cachedJar = findCachedJar(cacheDir);
        
        if (cachedJar != null && Files.exists(cachedJar)) {
            log.info("Using cached Apache Tomcat migration tool: {}", cachedJar);
            return cachedJar;
        }
        
        log.info("Apache Tomcat migration tool not found in cache, downloading from Apache website...");
        
        // Try multiple download URLs (Apache archive, GitHub releases, etc.)
        IOException lastException = null;
        Path downloadPath = cacheDir.resolve("jakartaee-migration-" + DEFAULT_VERSION + "-shaded.jar");
        
        for (String downloadUrl : APACHE_DOWNLOAD_URLS) {
            try {
                log.info("Attempting to download from: {}", downloadUrl);
                downloadFile(downloadUrl, downloadPath);
                log.info("Apache Tomcat migration tool downloaded successfully from Apache website: {}", downloadPath);
                return downloadPath;
            } catch (IOException e) {
                log.warn("Failed to download from {}: {}", downloadUrl, e.getMessage());
                lastException = e;
                // Try next URL
            }
        }
        
        // All download attempts failed
        throw new IOException("Failed to download Apache Tomcat migration tool from all sources. " +
                            "Last error: " + (lastException != null ? lastException.getMessage() : "Unknown error"));
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
     * Downloads a file from a URL to a local path.
     */
    private void downloadFile(String urlString, Path destination) throws IOException {
        log.info("Downloading from: {}", urlString);
        
        URL url = URI.create(urlString).toURL();
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
             java.io.OutputStream outputStream = Files.newOutputStream(destination)) {
            
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

