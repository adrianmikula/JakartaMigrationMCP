package adrianmikula.jakartamigration.pdfreporting.snippet;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * HTML snippet for the File-by-File Action Breakdown section.
 * Displays a detailed table of all javax references organized by priority.
 * 
 * References: docs/spec/html-refactoring-report-requirements.md Section 4
 */
public class FileByFileActionSnippet extends BaseHtmlSnippet {

    private final List<Map<String, Object>> javaxReferences;

    public FileByFileActionSnippet(List<Map<String, Object>> javaxReferences) {
        this.javaxReferences = javaxReferences;
    }

    @Override
    public String getSnippetName() {
        return "File-by-File Action Breakdown";
    }

    @Override
    public String generate() throws SnippetGenerationException {
        StringBuilder html = new StringBuilder();

        html.append("""
            <div class="section">
                <h2>File-by-File Refactoring Actions</h2>
                <p>Detailed breakdown of each file requiring javax to jakarta migration. Actions are grouped by priority to help you focus on high-impact changes first.</p>
                
                <div class="file-actions-container">
            """);

        if (javaxReferences != null && !javaxReferences.isEmpty()) {
            // Sort by priority: HIGH, MEDIUM, LOW
            List<Map<String, Object>> sortedRefs = javaxReferences.stream()
                .sorted(Comparator.comparingInt(this::priorityValue).reversed())
                .toList();

            html.append("""
                    <table class="refactor-table">
                        <thead>
                            <tr>
                                <th>File</th>
                                <th>Line</th>
                                <th>Javax Reference</th>
                                <th>Priority</th>
                                <th>Recipe Available</th>
                            </tr>
                        </thead>
                        <tbody>
                """);

            for (Map<String, Object> ref : sortedRefs) {
                if (ref == null) continue;

                String file = escapeHtml(getString(ref, "file", "Unknown"));
                String line = escapeHtml(getString(ref, "line", "-"));
                String reference = escapeHtml(getString(ref, "reference", "Unknown"));
                String priority = getString(ref, "priority", "MEDIUM").toUpperCase();
                boolean recipeAvailable = Boolean.TRUE.equals(ref.get("recipeAvailable"));

                String priorityClass = switch (priority) {
                    case "HIGH" -> "priority-high";
                    case "MEDIUM" -> "priority-medium";
                    case "LOW" -> "priority-low";
                    default -> "";
                };

                String recipeStatus = recipeAvailable
                    ? "<span class='recipe-available'>Available</span>"
                    : "<span class='recipe-unavailable'>Manual Required</span>";

                html.append(String.format("""
                            <tr class="%s">
                                <td class="file-path">%s</td>
                                <td class="line-number">%s</td>
                                <td class="javax-ref">%s</td>
                                <td class="priority-badge %s">%s</td>
                                <td class="recipe-status">%s</td>
                            </tr>
                    """, priorityClass, file, line, reference, priorityClass, priority, recipeStatus));
            }

            html.append("""
                        </tbody>
                    </table>
            """);
        } else {
            html.append("""
                    <div class="no-actions-message">
                        <div class="message-icon">&#10004;</div>
                        <div class="message-text">
                            <strong>No javax references found!</strong> Your project appears to already be 
                            using Jakarta EE namespaces, or no Java files were scanned.
                        </div>
                    </div>
            """);
        }

        html.append("""
                </div>
            </div>
            """);

        return html.toString();
    }

    @Override
    public boolean isApplicable() {
        return true; // Always show, even if empty (shows success message)
    }

    private int priorityValue(Map<String, Object> ref) {
        if (ref == null) return 0;
        String priority = getString(ref, "priority", "MEDIUM").toUpperCase();
        return switch (priority) {
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            case "LOW" -> 1;
            default -> 0;
        };
    }

    private String getString(Map<String, Object> map, String key, String defaultValue) {
        if (map == null) return defaultValue;
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}
