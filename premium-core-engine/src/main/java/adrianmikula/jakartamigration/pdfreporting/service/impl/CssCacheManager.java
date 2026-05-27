package adrianmikula.jakartamigration.pdfreporting.service.impl;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;

/**
 * Manages CSS resource loading and caching to reduce memory overhead.
 * Loads CSS from external resource files and caches them for reuse.
 */
@Slf4j
public class CssCacheManager {
    
    private String cachedCommonStyles;
    private String cachedHeaderStyles;
    private String cachedFooterStyles;
    private String cachedRefactoringStyles;
    
    public CssCacheManager() {
        loadAndCacheCss();
    }
    
    /**
     * Load CSS from external resource files and cache them to reduce memory overhead.
     */
    private void loadAndCacheCss() {
        try {
            cachedCommonStyles = loadCssResource("pdf-reporting/css/common-styles.css");
            cachedHeaderStyles = loadCssResource("pdf-reporting/css/header-styles.css");
            cachedFooterStyles = loadCssResource("pdf-reporting/css/footer-styles.css");
            cachedRefactoringStyles = loadCssResource("pdf-reporting/css/refactoring-styles.css");
            
            log.info("CSS loaded and cached successfully. Common styles: {} bytes, Header styles: {} bytes, Footer styles: {} bytes, Refactoring styles: {} bytes",
                cachedCommonStyles.length(), cachedHeaderStyles.length(), cachedFooterStyles.length(), cachedRefactoringStyles.length());
        } catch (Exception e) {
            log.warn("Failed to load CSS from external files, falling back to inline CSS: {}", e.getMessage());
            // Initialize with empty strings to allow fallback to inline generation
            cachedCommonStyles = "";
            cachedHeaderStyles = "";
            cachedFooterStyles = "";
            cachedRefactoringStyles = "";
        }
    }
    
    /**
     * Load CSS content from a resource file.
     */
    private String loadCssResource(String resourcePath) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            return new String(is.readAllBytes());
        }
    }
    
    public String getCachedCommonStyles() {
        return cachedCommonStyles;
    }
    
    public String getCachedHeaderStyles() {
        return cachedHeaderStyles;
    }
    
    public String getCachedFooterStyles() {
        return cachedFooterStyles;
    }
    
    public String getCachedRefactoringStyles() {
        return cachedRefactoringStyles;
    }
}
