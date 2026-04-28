package adrianmikula.jakartamigration.pdfreporting.snippet;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Assembles HTML snippets into a complete report.
 * Handles snippet ordering, error recovery, and final HTML structure.
 */
@Slf4j
public class ReportAssembler {
    
    /**
     * Assemble a list of HTML snippets into a complete HTML report.
     * 
     * @param snippets List of HTML snippets to include
     * @param reportTitle Title for the HTML document
     * @return Complete HTML report
     */
    public String assembleReport(List<HtmlSnippet> snippets, String reportTitle) {
        // Sort snippets by order
        List<HtmlSnippet> sortedSnippets = snippets.stream()
            .filter(HtmlSnippet::isApplicable)
            .sorted((s1, s2) -> Integer.compare(s1.getOrder(), s2.getOrder()))
            .collect(Collectors.toList());
        
        StringBuilder html = new StringBuilder();
        
        // HTML document structure (no DOCTYPE for XML compatibility)
        html.append("<html>\n");
        html.append("<head>\n");
        html.append(generateHeadSection(reportTitle));
        html.append("</head>\n");
        html.append("<body>\n");
        html.append(generateBodyStart());
        
        // Generate each snippet with error handling
        for (HtmlSnippet snippet : sortedSnippets) {
            try {
                String snippetHtml = snippet.generate();
                if (snippetHtml != null && !snippetHtml.trim().isEmpty()) {
                    html.append(snippetHtml);
                    html.append("\n");
                }
            } catch (SnippetGenerationException e) {
                log.error("Failed to generate snippet: {}", snippet.getSnippetName(), e);
                html.append(generateErrorSnippet(snippet.getSnippetName(), e.getMessage()));
            } catch (Exception e) {
                log.error("Unexpected error generating snippet: {}", snippet.getSnippetName(), e);
                html.append(generateErrorSnippet(snippet.getSnippetName(), "Unexpected error: " + e.getMessage()));
            }
        }
        
        html.append(generateBodyEnd());
        html.append("</body>\n");
        html.append("</html>\n");
        
        return html.toString();
    }
    
    /**
     * Generate the HTML head section with common styles and metadata.
     * 
     * @param reportTitle The report title
     * @return HTML head section
     */
    private String generateHeadSection(String reportTitle) {
        return String.format("""
            <title>%s</title>
            <meta charset="UTF-8"></meta>
            <meta name="viewport" content="width=device-width, initial-scale=1.0"></meta>
            <style>
                body {
                    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                    margin: 0;
                    padding: 40px;
                    line-height: 1.6;
                    color: #333;
                    background: #f8f9fa;
                }
                .container {
                    max-width: 1200px;
                    margin: 0 auto;
                    background: white;
                    padding: 40px;
                    border-radius: 8px;
                    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
                }
                .header {
                    text-align: center;
                    margin-bottom: 40px;
                    border-bottom: 2px solid #e9ecef;
                    padding-bottom: 20px;
                }
                .header h1 {
                    color: #2c3e50;
                    margin: 0;
                    font-size: 2.5em;
                    font-weight: 300;
                }
                .header h2 {
                    color: #34495e;
                    font-size: 1.8em;
                    margin: 10px 0;
                    font-weight: 400;
                }
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
                .warning-box {
                    background: #fff3cd;
                    border: 1px solid #ffeaa7;
                    border-radius: 4px;
                    padding: 15px;
                    margin: 20px 0;
                }
                .warning-box h3 {
                    color: #856404;
                    margin-top: 0;
                }
                .dependency-matrix-table {
                    width: 100%%;
                    border-collapse: collapse;
                    margin: 20px 0;
                    font-size: 0.85em;
                    background: white;
                    border-radius: 8px;
                    overflow: hidden;
                    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
                }
                .dependency-matrix-table th {
                    background: linear-gradient(135deg, #2c3e50 0%%, #34495e 100%%);
                    color: white;
                    font-weight: bold;
                    padding: 15px 12px;
                    text-align: left;
                    border-bottom: 2px solid #3498db;
                }
                .dependency-matrix-table td {
                    padding: 12px;
                    text-align: left;
                    border-bottom: 1px solid #e1e8ed;
                    vertical-align: top;
                }
                .dependency-matrix-table tr:hover {
                    background-color: #f8f9fa;
                }
                .compatible { color: #27ae60; font-weight: bold; }
                .needs-update { color: #f39c12; font-weight: bold; }
                .incompatible { color: #e74c3c; font-weight: bold; }
                .unknown-status { color: #95a5a6; font-weight: bold; }
                .compatibility-summary {
                    margin: 30px 0;
                    padding: 20px;
                    background: #f8f9fa;
                    border-radius: 8px;
                    border: 1px solid #e9ecef;
                }
                .guidelines-grid {
                    display: grid;
                    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
                    gap: 20px;
                    margin: 20px 0;
                }
                .guideline-card {
                    background: white;
                    padding: 20px;
                    border-radius: 8px;
                    border: 1px solid #e1e8ed;
                    border-left: 4px solid #3498db;
                }
                .guideline-card h4 {
                    margin: 0 0 10px 0;
                    color: #2c3e50;
                }
                .code-example-section {
                    margin: 40px 0;
                    padding: 30px;
                    background: #f8f9fa;
                    border-radius: 8px;
                    border: 1px solid #e9ecef;
                }
                .code-comparison {
                    display: grid;
                    grid-template-columns: 1fr 1fr;
                    gap: 20px;
                    margin: 20px 0;
                }
                .before-code, .after-code {
                    background: white;
                    padding: 20px;
                    border-radius: 8px;
                    border: 1px solid #e1e8ed;
                }
                .before-code {
                    border-left: 4px solid #e74c3c;
                }
                .after-code {
                    border-left: 4px solid #27ae60;
                }
                .code-block {
                    background: #2c3e50;
                    color: #ecf0f1;
                    padding: 15px;
                    border-radius: 4px;
                    font-family: 'Courier New', monospace;
                    font-size: 0.85em;
                    overflow-x: auto;
                    white-space: pre-wrap;
                }
                .migration-note {
                    margin: 20px 0;
                    padding: 20px;
                    background: #e8f5e8;
                    border-radius: 8px;
                    border-left: 4px solid #f39c12;
                }
                .risk-matrix-container {
                    margin: 30px 0;
                }
                .risk-overview-grid {
                    display: grid;
                    grid-template-columns: 2fr 1fr;
                    gap: 30px;
                    margin: 20px 0;
                }
                .risk-score-card {
                    background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                    color: white;
                    padding: 30px;
                    border-radius: 12px;
                    text-align: center;
                }
                .risk-dial {
                    width: 120px;
                    height: 120px;
                    border-radius: 50%%;
                    background: rgba(255, 255, 255, 0.2);
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    font-size: 2em;
                    font-weight: bold;
                    margin: 0 auto 20px;
                }
                .risk-level {
                    margin-top: 20px;
                }
                .risk-level-text {
                    font-size: 1.5em;
                    font-weight: bold;
                    text-transform: uppercase;
                }
                .factor-grid {
                    display: grid;
                    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                    gap: 15px;
                    margin: 20px 0;
                }
                .factor-item {
                    display: flex;
                    align-items: center;
                    gap: 10px;
                    padding: 10px;
                    background: white;
                    border-radius: 6px;
                    border: 1px solid #e1e8ed;
                }
                .factor-bar {
                    flex: 1;
                    height: 8px;
                    background: #e9ecef;
                    border-radius: 4px;
                    overflow: hidden;
                }
                .factor-fill {
                    height: 100%%;
                    background: linear-gradient(90deg, #3498db, #2c3e50);
                    transition: width 0.3s ease;
                }
                .module-risk-grid {
                    display: grid;
                    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
                    gap: 20px;
                    margin: 20px 0;
                }
                .module-risk-card {
                    background: white;
                    border-radius: 8px;
                    overflow: hidden;
                    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
                }
                .module-header {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    padding: 20px;
                    background: #f8f9fa;
                    border-bottom: 1px solid #e9ecef;
                }
                .module-risk-score {
                    font-size: 1.5em;
                    font-weight: bold;
                }
                .low { color: #27ae60; }
                .medium { color: #f39c12; }
                .high { color: #e74c3c; }
                .phase-grid {
                    display: grid;
                    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
                    gap: 20px;
                    margin: 20px 0;
                }
                .trend-phase {
                    background: white;
                    border-radius: 8px;
                    padding: 20px;
                    text-align: center;
                    border: 1px solid #e1e8ed;
                    min-height: 200px;
                    display: flex;
                    flex-direction: column;
                }
                .trend-phase h4 {
                    margin: 0 0 15px 0;
                    color: #2c3e50;
                }
                .trend-phase p {
                    margin: 15px 0 0 0;
                    color: #7f8c8d;
                    font-size: 0.9em;
                }
                .phase-risk {
                    margin-top: 15px;
                    height: 120px;
                    border-radius: 10px;
                    position: relative;
                    overflow: hidden;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                }
                .risk-label {
                    color: white;
                    font-weight: bold;
                    font-size: 0.9em;
                    text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.3);
                    z-index: 1;
                }
                .high-risk { background: linear-gradient(180deg, #e74c3c, #c0392b); }
                .medium-risk { background: linear-gradient(180deg, #f39c12, #e67e22); }
                .low-risk { background: linear-gradient(180deg, #27ae60, #229954); }
                .legend-color {
                    width: 20px;
                    height: 20px;
                    border-radius: 4px;
                    display: inline-block;
                }
                .trend-legend {
                    display: flex;
                    justify-content: center;
                    gap: 30px;
                    margin-bottom: 20px;
                    padding: 10px;
                    background: #f8f9fa;
                    border-radius: 8px;
                }
                .legend-item {
                    display: flex;
                    align-items: center;
                    gap: 8px;
                    font-size: 0.9em;
                    color: #555;
                }
                .trend-grid {
                    display: grid;
                    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                    gap: 20px;
                    margin-top: 20px;
                }
                .strategies-grid {
                    display: grid;
                    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
                    gap: 20px;
                    margin: 20px 0;
                }
                .strategy-card {
                    background: white;
                    border-radius: 8px;
                    padding: 25px;
                    border: 1px solid #e1e8ed;
                    border-left: 4px solid #3498db;
                }
                .strategy-effectiveness {
                    font-weight: bold;
                    margin-bottom: 15px;
                    padding: 5px 10px;
                    border-radius: 4px;
                    font-size: 0.9em;
                }
                .high { background: #d4edda; color: #155724; }
                .medium { background: #fff3cd; color: #856404; }
                .timeline-grid {
                    display: grid;
                    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                    gap: 15px;
                    margin: 20px 0;
                }
                .resource-allocation {
                    margin: 30px 0;
                }
                .resource-breakdown {
                    display: grid;
                    grid-template-columns: 1fr 1fr;
                    gap: 30px;
                }
                .resource-role-item {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    padding: 15px;
                    background: white;
                    border-radius: 6px;
                    border: 1px solid #e1e8ed;
                    margin-bottom: 10px;
                }
                .milestone-complete { color: #27ae60; }
                .milestone-pending { color: #f39c12; }
                .snippet-error {
                    background: #ffebee;
                    border: 1px solid #f44336;
                    padding: 10px;
                    margin: 10px 0;
                    border-radius: 4px;
                }
                .snippet-error h4 {
                    color: #d32f2f;
                    margin: 0 0 10px 0;
                }
                @media print {
                    body { padding: 20px; }
                    .container { box-shadow: none; }
                }
            </style>
            """, escapeHtml(reportTitle));
    }
    
    /**
     * Generate the body start section with container.
     * 
     * @return Body start HTML
     */
    private String generateBodyStart() {
        return """
            <div class="container">
            """;
    }
    
    /**
     * Generate the body end section.
     * 
     * @return Body end HTML
     */
    private String generateBodyEnd() {
        return """
            </div>
            """;
    }
    
    /**
     * Generate an error snippet when snippet generation fails.
     * 
     * @param snippetName Name of the failed snippet
     * @param errorMessage Error message
     * @return Error snippet HTML
     */
    private String generateErrorSnippet(String snippetName, String errorMessage) {
        return String.format("""
            <div class="snippet-error">
                <h4>⚠️ Error in %s</h4>
                <p>Unable to generate section content: %s</p>
            </div>
            """, escapeHtml(snippetName), escapeHtml(errorMessage));
    }
    
    /**
     * Escape HTML content for safe display.
     * 
     * @param content The content to escape
     * @return Escaped content
     */
    private String escapeHtml(String content) {
        if (content == null) return "";
        return content.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}
