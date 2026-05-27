package adrianmikula.jakartamigration.pdfreporting.service.impl;

/**
 * Generator for CSS styles used in HTML reports.
 * Provides inline CSS fallback when external CSS loading fails.
 */
public class ReportCssGenerator {
    
    private final CssCacheManager cssCacheManager;
    
    public ReportCssGenerator() {
        this.cssCacheManager = new CssCacheManager();
    }
    
    /**
     * Get shared header styles for reports.
     * Returns cached CSS if available, otherwise generates inline fallback.
     */
    public String getSharedHeaderStyles() {
        // Return cached CSS if available, otherwise generate inline (fallback)
        if (cssCacheManager.getCachedHeaderStyles() != null && !cssCacheManager.getCachedHeaderStyles().isEmpty()) {
            return cssCacheManager.getCachedHeaderStyles();
        }
        
        // Fallback to inline generation if CSS loading failed
        return """
            /* Shared Header Styles */
            .report-header {
                display: flex;
                justify-content: space-between;
                align-items: center;
                padding: 20px 0;
                border-bottom: 3px solid #2c3e50;
                margin-bottom: 40px;
                background: linear-gradient(135deg, #f8f9fa 0%%, #e9ecef 100%%);
                border-radius: 8px 8px 0 0;
            }
            
            .header-left {
                flex: 0 0 auto;
                display: flex;
                align-items: center;
            }
            
            .plugin-icon {
                width: 32px;
                height: 32px;
                display: flex;
                align-items: center;
                justify-content: center;
            }
            
            .plugin-icon svg {
                width: 100%%;
                height: 100%;
                max-width: 32px;
                max-height: 32px;
            }
            
            .header-center {
                flex: 1 1 auto;
                text-align: center;
                padding: 0 20px;
            }
            
            .report-title {
                color: #2c3e50;
                font-size: 2.2em;
                margin: 0;
                font-weight: 300;
                line-height: 1.2;
            }
            
            .project-name {
                color: #34495e;
                font-size: 1.4em;
                margin: 8px 0 4px 0;
                font-weight: 400;
            }
            
            .report-type {
                color: #7f8c8d;
                font-size: 1.1em;
                font-style: italic;
                margin: 0;
            }
            
            .header-right {
                flex: 0 0 auto;
                text-align: right;
            }
            
            .timestamp {
                color: #7f8c8d;
                font-size: 0.9em;
                font-weight: 500;
            }
            
            .plugin-name {
                color: #2c3e50;
                font-size: 0.95em;
                font-weight: 600;
                margin-left: 10px;
                max-width: 120px;
                line-height: 1.2;
            }
            """;
    }
    
    /**
     * Get shared footer styles for reports.
     * Returns cached CSS if available, otherwise generates inline fallback.
     */
    public String getSharedFooterStyles() {
        // Return cached CSS if available, otherwise generate inline (fallback)
        if (cssCacheManager.getCachedFooterStyles() != null && !cssCacheManager.getCachedFooterStyles().isEmpty()) {
            return cssCacheManager.getCachedFooterStyles();
        }
        
        // Fallback to inline generation if CSS loading failed
        return """
            /* Shared Footer Styles */
            .report-footer {
                display: flex;
                justify-content: space-between;
                align-items: center;
                padding: 20px 0;
                border-top: 2px solid #e1e8ed;
                margin-top: 60px;
                background: #f8f9fa;
                border-radius: 0 0 8px 8px;
                position: relative;
            }
            
            .footer-left {
                flex: 0 0 auto;
                display: flex;
                align-items: center;
            }
            
            .plugin-icon-footer {
                width: 24px;
                height: 24px;
                display: flex;
                align-items: center;
                justify-content: center;
                margin-right: 8px;
            }
            
            .plugin-icon-footer svg {
                width: 100%%;
                height: 100%;
                max-width: 24px;
                max-height: 24px;
            }
            
            .plugin-info {
                color: #2c3e50;
                font-size: 0.9em;
                font-weight: 600;
            }
            
            .footer-center {
                flex: 1 1 auto;
                text-align: center;
            }
            
            .page-info {
                color: #7f8c8d;
                font-size: 0.85em;
                font-weight: 500;
            }
            
            .footer-right {
                flex: 0 0 auto;
                text-align: right;
            }
            
            .report-type-footer {
                color: #7f8c8d;
                font-size: 0.85em;
                font-weight: 500;
                text-transform: uppercase;
                letter-spacing: 0.5px;
            }
            
            /* Print-specific styles for PDF generation */
            @media print {
                .report-header {
                    position: fixed;
                    top: 0;
                    left: 0;
                    right: 0;
                    height: auto;
                    z-index: 1000;
                }
                
                .report-footer {
                    position: fixed;
                    bottom: 0;
                    left: 0;
                    right: 0;
                    height: auto;
                    z-index: 1000;
                }
                
                body {
                    margin-top: 120px;
                    margin-bottom: 80px;
                }
            }
            """;
    }
    
    /**
     * Fallback inline common styles when external CSS loading fails.
     */
    public String getInlineCommonStyles() {
        return """
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
            .section {
                margin: 40px 0;
                padding: 30px;
                border: 1px solid #e1e8ed;
                border-radius: 8px;
                background: #ffffff;
            }
            .section h2 {
                color: #2c3e50;
                border-bottom: 2px solid #3498db;
                padding-bottom: 15px;
                margin-top: 0;
                font-size: 1.8em;
            }
            .metrics-grid {
                display: grid;
                grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
                gap: 20px;
                margin: 30px 0;
            }
            .metric-card {
                background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                color: white;
                padding: 25px;
                border-radius: 12px;
                text-align: center;
                box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
            }
            .metric-value {
                font-size: 2.5em;
                font-weight: bold;
                margin-bottom: 10px;
            }
            .metric-label {
                font-size: 1.1em;
                opacity: 0.9;
            }
            .dependency-table {
                width: 100%%;
                border-collapse: collapse;
                margin: 20px 0;
                font-size: 0.9em;
            }
            .dependency-table th,
            .dependency-table td {
                padding: 12px 15px;
                text-align: left;
                border-bottom: 1px solid #ddd;
            }
            .dependency-table th {
                background-color: #34495e;
                color: white;
                font-weight: bold;
            }
            .dependency-table tr:hover {
                background-color: #f5f5f5;
            }
            .compatible { color: #27ae60; font-weight: bold; }
            .incompatible { color: #e74c3c; font-weight: bold; }
            .blocker-list {
                list-style: none;
                padding: 0;
            }
            .blocker-item {
                background: #fff3cd;
                border: 1px solid #ffeaa7;
                border-left: 4px solid #e17055;
                padding: 15px;
                margin: 10px 0;
                border-radius: 4px;
            }
            .blocker-high { border-left-color: #e74c3c; background: #fdf2f2; }
            .blocker-medium { border-left-color: #f39c12; background: #fef9e7; }
            .blocker-low { border-left-color: #27ae60; background: #e8f8f5; }
            .timeline {
                position: relative;
                padding: 20px 0;
            }
            .timeline-item {
                padding: 20px;
                margin: 20px 0;
                background: #f8f9fa;
                border-radius: 8px;
                border-left: 4px solid #3498db;
            }
            .timeline-phase {
                font-weight: bold;
                color: #2c3e50;
                font-size: 1.2em;
                margin-bottom: 10px;
            }
            .timeline-duration {
                color: #7f8c8d;
                font-style: italic;
                margin-bottom: 10px;
            }
            .strategy-table-container {
                margin: 20px 0;
                overflow-x: auto;
            }
            .strategy-comparison-table {
                width: 100%%;
                border-collapse: collapse;
                font-size: 0.85em;
                background: white;
                border-radius: 8px;
                overflow: hidden;
                box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
            }
            .strategy-comparison-table th {
                background: linear-gradient(135deg, #2c3e50 0%%, #34495e 100%%);
                color: white;
                font-weight: bold;
                padding: 15px 12px;
                text-align: left;
                border-bottom: 2px solid #3498db;
                font-size: 0.9em;
            }
            .strategy-comparison-table td {
                padding: 12px;
                border-bottom: 1px solid #e1e8ed;
                vertical-align: top;
                line-height: 1.4;
            }
            .strategy-row:hover {
                background-color: #f8f9fa;
            }
            .strategy-row:nth-child(even) {
                background-color: #fafbfc;
            }
            .strategy-row:nth-child(even):hover {
                background-color: #f1f3f4;
            }
            .strategy-name {
                font-weight: bold;
                min-width: 120px;
            }
            .strategy-indicator {
                width: 12px;
                height: 12px;
                border-radius: 50%%;
                display: inline-block;
                margin-right: 8px;
                border: 1px solid rgba(0, 0, 0, 0.2);
                vertical-align: middle;
            }
            .strategy-description {
                min-width: 150px;
                font-style: italic;
                color: #555;
            }
            .strategy-benefits {
                color: #27ae60;
                font-size: 0.9em;
            }
            .strategy-risks {
                color: #e74c3c;
                font-size: 0.9em;
            }
            .strategy-phases {
                font-size: 0.8em;
                color: #666;
            }
            .strategy-use-case {
                font-weight: 500;
                color: #2c3e50;
                font-size: 0.9em;
            }
            .risk-dial {
                display: inline-block;
                width: 120px;
                height: 120px;
                border-radius: 50%%;
                text-align: center;
                line-height: 120px;
                font-size: 1.5em;
                font-weight: bold;
                margin: 20px;
                color: white;
            }
            .risk-low { background: #27ae60; }
            .risk-medium { background: #f39c12; }
            .risk-high { background: #e74c3c; }
            .risk-critical { background: #8e44ad; }
            @media print {
                body { padding: 20px; }
                .container { box-shadow: none; }
                .strategy-comparison-table { font-size: 0.75em; }
                .strategy-comparison-table th { padding: 10px 8px; }
                .strategy-comparison-table td { padding: 8px; }
            }
            """;
    }
    
    /**
     * Fallback inline refactoring styles when external CSS loading fails.
     */
    public String getInlineRefactoringStyles() {
        return """
            .section h2 {
                color: #2c3e50;
                border-bottom: 2px solid #e74c3c;
                padding-bottom: 15px;
                margin-top: 0;
                font-size: 1.8em;
            }
            .metric-card {
                background: linear-gradient(135deg, #e74c3c 0%%, #c0392b 100%%);
                color: white;
                padding: 25px;
                border-radius: 12px;
                text-align: center;
                box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
            }
            .refactor-table {
                width: 100%%;
                border-collapse: collapse;
                margin: 20px 0;
                font-size: 0.9em;
            }
            .refactor-table th,
            .refactor-table td {
                padding: 12px 15px;
                text-align: left;
                border-bottom: 1px solid #ddd;
            }
            .refactor-table th {
                background-color: #c0392b;
                color: white;
                font-weight: bold;
            }
            .refactor-table tr:hover {
                background-color: #f5f5f5;
            }
            .priority-high { background: #fdf2f2; color: #e74c3c; font-weight: bold; }
            .priority-medium { background: #fef9e7; color: #f39c12; font-weight: bold; }
            .priority-low { background: #e8f8f5; color: #27ae60; font-weight: bold; }
            .recipe-available { color: #27ae60; font-weight: bold; }
            .recipe-unavailable { color: #e74c3c; font-weight: bold; }
            .code-example {
                background: #2c3e50;
                color: #ecf0f1;
                padding: 20px;
                border-radius: 8px;
                font-family: 'Courier New', monospace;
                margin: 15px 0;
                overflow-x: auto;
            }
            .action-steps {
                background: #ecf0f1;
                padding: 20px;
                border-radius: 8px;
                margin: 20px 0;
            }
            .action-steps ol {
                margin: 0;
                padding-left: 20px;
            }
            .action-steps li {
                margin: 10px 0;
            }
            .footer {
                margin-top: 60px;
                padding-top: 30px;
                border-top: 1px solid #e1e8ed;
                text-align: center;
                color: #7f8c8d;
                font-size: 0.9em;
            }
            """;
    }
}
