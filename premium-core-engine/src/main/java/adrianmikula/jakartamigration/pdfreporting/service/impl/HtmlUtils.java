package adrianmikula.jakartamigration.pdfreporting.service.impl;

/**
 * Utility class for HTML operations.
 */
public class HtmlUtils {
    
    /**
     * Escape HTML special characters to prevent XSS.
     */
    public String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }
    
    /**
     * Get the plugin icon SVG for embedding in HTML reports.
     * Returns raw SVG XML for HTML rendering (unescaped for browser display).
     */
    public String getPluginIconSvg() {
        return """
            <svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg" style="display: block;">
                <defs>
                    <linearGradient id="iconGrad1" x1="0%" y1="0%" x2="100%" y2="100%">
                        <stop offset="0%" style="stop-color:#3498db;stop-opacity:1" />
                        <stop offset="100%" style="stop-color:#2c3e50;stop-opacity:1" />
                    </linearGradient>
                </defs>
                <rect x="2" y="2" width="20" height="20" rx="4" fill="url(#iconGrad1)"/>
                <path d="M8 12h8M12 8v8" stroke="white" stroke-width="2" stroke-linecap="round"/>
                <circle cx="12" cy="12" r="6" stroke="white" stroke-width="1.5" fill="none" stroke-dasharray="2,2"/>
            </svg>
            """;
    }
}
