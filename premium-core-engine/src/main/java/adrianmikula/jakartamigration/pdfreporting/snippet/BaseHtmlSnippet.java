package adrianmikula.jakartamigration.pdfreporting.snippet;

import lombok.extern.slf4j.Slf4j;

/**
 * Base class for HTML snippets with common functionality.
 * Provides safe formatting and error handling.
 */
@Slf4j
public abstract class BaseHtmlSnippet implements HtmlSnippet {
    
    /**
     * Safely format a string template with arguments.
     * Catches formatting exceptions and provides fallback content.
     * 
     * @param template The format template
     * @param args The format arguments
     * @return Formatted string or fallback content on error
     */
    protected String safelyFormat(String template, Object... args) {
        try {
            // Escape stray '%' characters to avoid format parsing errors
            String safeTemplate = template.replaceAll("%(?!s)", "%%");
            return safeTemplate.formatted(args);
        } catch (Exception e) {
            log.error("Formatting error in snippet {}: template='{}', args={}", 
                getSnippetName(), template, args, e);
            return generateFallbackContent(template, args);
        }
    }
    
    /**
     * Generate fallback content when formatting fails.
     * 
     * @param template The original template
     * @param args The original arguments
     * @return Fallback HTML content
     */
    protected String generateFallbackContent(String template, Object... args) {
        return String.format("""
            <div class="snippet-error" style="background: #ffebee; border: 1px solid #f44336; padding: 10px; margin: 10px 0; border-radius: 4px;">
                <h4 style="color: #d32f2f; margin: 0 0 10px 0;">⚠️ Formatting Error</h4>
                <p style="margin: 0; color: #666;">Unable to render %s section due to formatting error.</p>
                <details style="margin-top: 10px;">
                    <summary style="cursor: pointer; color: #666;">Technical Details</summary>
                    <pre style="background: #f5f5f5; padding: 10px; margin: 10px 0; border-radius: 4px; overflow: auto; font-size: 12px;">Template: %s
Arguments: %s
Error: %s</pre>
                </details>
            </div>
            """, getSnippetName(), escapeHtml(template), formatArgs(args), "Formatting failed");
    }
    
    /**
     * Escape HTML content for safe display.
     * This method should be used for all dynamic content inserted into HTML.
     * 
     * @param content The content to escape
     * @return Escaped content
     */
    protected String escapeHtml(String content) {
        if (content == null) return "null";
        return content.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
    
    /**
     * Escape HTML content and preserve basic formatting for display.
     * Use this for content that should maintain some readability while being safe.
     * 
     * @param content The content to escape
     * @return Escaped content with preserved formatting
     */
    protected String escapeHtmlWithFormatting(String content) {
        if (content == null) return "null";
        String escaped = escapeHtml(content);
        // Preserve line breaks
        return escaped.replace("\\n", "<br>").replace("\\r\\n", "<br>");
    }
    
    /**
     * Safely format content with automatic HTML escaping.
     * This method ensures all arguments are properly escaped before insertion.
     * 
     * @param template The format template with %s placeholders
     * @param args The arguments to escape and insert
     * @return Formatted and escaped HTML content
     */
    protected String safelyFormatWithEscaping(String template, Object... args) {
        try {
            // Escape all arguments before formatting
            Object[] escapedArgs = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                if (args[i] != null) {
                    escapedArgs[i] = escapeHtml(args[i].toString());
                } else {
                    escapedArgs[i] = "null";
                }
            }
            return template.formatted(escapedArgs);
        } catch (Exception e) {
            log.error("Formatting error in snippet {}: template='{}', args={}", 
                getSnippetName(), template, args, e);
            return generateFallbackContent(template, args);
        }
    }
    
    /**
     * Format arguments for display in error messages.
     * 
     * @param args The arguments to format
     * @return Formatted string representation
     */
    private String formatArgs(Object[] args) {
        if (args == null || args.length == 0) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(args[i] != null ? args[i].toString() : "null");
        }
        sb.append("]");
        return sb.toString();
    }
    
    /**
     * Get common CSS styles used across snippets.
     * 
     * @return CSS style string
     */
    protected String getCommonStyles() {
        return """
            .section {
                margin: 30px 0;
            }
            .section h2 {
                color: #2c3e50;
                border-bottom: 2px solid #3498db;
                padding-bottom: 10px;
            }
            .metrics-grid {
                display: grid;
                grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                gap: 20px;
                margin: 20px 0;
            }
            .metric-card {
                background: #f8f9fa;
                padding: 20px;
                border-radius: 8px;
                text-align: center;
                border-left: 4px solid #3498db;
            }
            .metric-value {
                font-size: 2em;
                font-weight: bold;
                color: #2c3e50;
            }
            .metric-label {
                color: #7f8c8d;
                margin-top: 5px;
            }
            """;
    }
}
