package adrianmikula.jakartamigration.pdfreporting.snippet;

import java.util.List;
import java.util.Map;

/**
 * HTML snippet for the Manual Migration Tasks section.
 * Lists javax references that require manual developer intervention.
 * 
 * References: docs/spec/html-refactoring-report-requirements.md Section 7
 */
public class ManualTasksSnippet extends BaseHtmlSnippet {

    private final List<Map<String, Object>> javaxReferences;

    public ManualTasksSnippet(List<Map<String, Object>> javaxReferences) {
        this.javaxReferences = javaxReferences;
    }

    @Override
    public String getSnippetName() {
        return "Manual Migration Tasks";
    }

    @Override
    public String generate() throws SnippetGenerationException {
        StringBuilder html = new StringBuilder();

        html.append("""
            <div class="section">
                <h2>Manual Migration Tasks</h2>
                <p>The following javax references require manual intervention as no automated recipe is available. Update these files by hand following the guidance provided for each item.</p>
                
                <div class="manual-tasks-container">
            """);

        List<Map<String, Object>> manualTasks = filterManualTasks();

        if (!manualTasks.isEmpty()) {
            html.append("""
                    <table class="manual-tasks-table">
                        <thead>
                            <tr>
                                <th>File</th>
                                <th>Line</th>
                                <th>Javax Reference</th>
                                <th>Jakarta Equivalent</th>
                                <th>Guidance</th>
                            </tr>
                        </thead>
                        <tbody>
                """);

            for (Map<String, Object> task : manualTasks) {
                String file = escapeHtml(getString(task, "file", "Unknown"));
                String line = escapeHtml(getString(task, "line", "-"));
                String reference = escapeHtml(getString(task, "reference", "Unknown"));
                String jakartaEquivalent = getJakartaEquivalent(reference);
                String guidance = generateGuidance(reference);

                html.append(String.format("""
                            <tr>
                                <td class="task-file">%s</td>
                                <td class="task-line">%s</td>
                                <td class="task-reference">%s</td>
                                <td class="task-equivalent">%s</td>
                                <td class="task-guidance">%s</td>
                            </tr>
                    """, file, line, reference, jakartaEquivalent, guidance));
            }

            html.append("""
                        </tbody>
                    </table>
            """);
        } else {
            html.append("""
                    <div class="no-tasks-message">
                        <div class="message-icon">&#10004;</div>
                        <div class="message-text">
                            <strong>Great news!</strong> All detected javax references can be handled by OpenRewrite recipes. 
                            No manual migration tasks are required. Execute the recipes in the section above to complete the migration.
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
        // Always applicable - shows "no tasks" message if everything is automated
        return true;
    }

    private List<Map<String, Object>> filterManualTasks() {
        if (javaxReferences == null) {
            return List.of();
        }

        return javaxReferences.stream()
            .filter(ref -> {
                if (ref == null) return false;
                Object recipeAvailable = ref.get("recipeAvailable");
                return recipeAvailable == null ||
                       Boolean.FALSE.equals(recipeAvailable) ||
                       "false".equalsIgnoreCase(String.valueOf(recipeAvailable));
            })
            .toList();
    }

    private String getString(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private String getJakartaEquivalent(String javaxReference) {
        if (javaxReference == null) return "jakarta.* equivalent";

        // Common javax to jakarta mappings using standard if-else for Java compatibility
        if (javaxReference.startsWith("javax.servlet.")) {
            return javaxReference.replace("javax.servlet.", "jakarta.servlet.");
        }
        if (javaxReference.startsWith("javax.persistence.")) {
            return javaxReference.replace("javax.persistence.", "jakarta.persistence.");
        }
        if (javaxReference.startsWith("javax.inject.")) {
            return javaxReference.replace("javax.inject.", "jakarta.inject.");
        }
        if (javaxReference.startsWith("javax.ws.rs.")) {
            return javaxReference.replace("javax.ws.rs.", "jakarta.ws.rs.");
        }
        if (javaxReference.startsWith("javax.annotation.")) {
            return javaxReference.replace("javax.annotation.", "jakarta.annotation.");
        }
        if (javaxReference.startsWith("javax.ejb.")) {
            return javaxReference.replace("javax.ejb.", "jakarta.ejb.");
        }
        if (javaxReference.startsWith("javax.jms.")) {
            return javaxReference.replace("javax.jms.", "jakarta.jms.");
        }
        if (javaxReference.startsWith("javax.mail.")) {
            return javaxReference.replace("javax.mail.", "jakarta.mail.");
        }
        if (javaxReference.startsWith("javax.transaction.")) {
            return javaxReference.replace("javax.transaction.", "jakarta.transaction.");
        }
        if (javaxReference.startsWith("javax.security.")) {
            return javaxReference.replace("javax.security.", "jakarta.security.");
        }
        if (javaxReference.startsWith("javax.json.")) {
            return javaxReference.replace("javax.json.", "jakarta.json.");
        }
        if (javaxReference.startsWith("javax.websocket.")) {
            return javaxReference.replace("javax.websocket.", "jakarta.websocket.");
        }
        if (javaxReference.startsWith("javax.faces.")) {
            return javaxReference.replace("javax.faces.", "jakarta.faces.");
        }
        if (javaxReference.startsWith("javax.batch.")) {
            return javaxReference.replace("javax.batch.", "jakarta.batch.");
        }
        if (javaxReference.startsWith("javax.decorator.")) {
            return javaxReference.replace("javax.decorator.", "jakarta.decorator.");
        }
        if (javaxReference.startsWith("javax.enterprise.")) {
            return javaxReference.replace("javax.enterprise.", "jakarta.enterprise.");
        }
        return "jakarta.* equivalent (see migration guide)";
    }

    private String generateGuidance(String javaxReference) {
        if (javaxReference == null) {
            return "Update import statement and any type references.";
        }

        if (javaxReference.contains("Inject") || javaxReference.contains("Resource")) {
            return "Update import and verify dependency injection still works with your CDI implementation.";
        }
        if (javaxReference.contains("Entity") || javaxReference.contains("Table") || javaxReference.contains("Column")) {
            return "Update JPA annotations. Verify persistence.xml references jakarta.persistence namespace.";
        }
        if (javaxReference.contains("HttpServlet") || javaxReference.contains("Filter")) {
            return "Update servlet classes. Check web.xml for jakarta.servlet namespace.";
        }
        if (javaxReference.contains("Path") || javaxReference.contains("GET") || javaxReference.contains("POST")) {
            return "Update JAX-RS annotations. Verify REST endpoints register correctly.";
        }
        if (javaxReference.contains("PostConstruct") || javaxReference.contains("PreDestroy")) {
            return "Update lifecycle annotations. Ensure annotation is on jakarta.annotation package.";
        }

        return "Update import statement and any type references in code.";
    }
}
