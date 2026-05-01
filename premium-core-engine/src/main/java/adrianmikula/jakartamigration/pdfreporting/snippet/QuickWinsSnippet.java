package adrianmikula.jakartamigration.pdfreporting.snippet;

import adrianmikula.jakartamigration.advancedscanning.domain.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Highlights "quick win" migrations - simple javax→jakarta renames with low complexity.
 * Helps developers build confidence by tackling easy changes first.
 */
public class QuickWinsSnippet extends BaseHtmlSnippet {

    private final ComprehensiveScanResults scanResults;

    public QuickWinsSnippet(ComprehensiveScanResults scanResults) {
        this.scanResults = scanResults;
    }

    @Override
    public String generate() throws SnippetGenerationException {
        List<QuickWin> quickWins = collectQuickWins();

        if (quickWins.isEmpty()) {
            return generateNoQuickWinsMessage();
        }

        return safelyFormat("""
            <div class="section quick-wins">
                <h2>Quick Wins - Easy Migrations First</h2>
                <p>These findings are simple javax→jakarta renames with direct equivalents. Start here to build momentum!</p>

                <div class="quick-wins-summary">
                    <span class="summary-stat quick-win-count">%d Quick Wins Found</span>
                    <span class="summary-stat">Estimated Time: %d minutes</span>
                </div>

                <div class="quick-wins-grid">
                    %s
                </div>

                <div class="quick-win-strategy">
                    <h4>Strategy: Tackle These First</h4>
                    <ol>
                        <li><strong>Batch Import Changes:</strong> Use IDE "Replace in Files" for javax.→jakarta. imports</li>
                        <li><strong>Verify Compilation:</strong> Build after each group to catch issues early</li>
                        <li><strong>Run Tests:</strong> Ensure tests pass before moving to complex items</li>
                    </ol>
                </div>
            </div>
            """,
            quickWins.size(),
            estimateQuickWinTime(quickWins.size()),
            generateQuickWinCards(quickWins)
        );
    }

    private List<QuickWin> collectQuickWins() {
        List<QuickWin> wins = new ArrayList<>();

        // Collect from JPA findings
        collectJpaQuickWins(wins);

        // Collect from CDI findings
        collectCdiQuickWins(wins);

        // Collect from Servlet findings (if available)
        collectServletQuickWins(wins);

        return wins;
    }

    private void collectJpaQuickWins(List<QuickWin> wins) {
        if (scanResults == null || scanResults.jpaResults() == null) return;

        Map<String, Object> jpaMap = scanResults.jpaResults();
        JpaProjectScanResult jpaResult = extractJpaResult(jpaMap);
        if (jpaResult == null) return;

        for (JpaScanResult fileResult : jpaResult.fileResults()) {
            for (JpaAnnotationUsage usage : fileResult.annotations()) {
                // Quick win: has Jakarta equivalent and is a simple annotation rename
                if (usage.hasJakartaEquivalent() && isSimpleRename(usage.annotationName())) {
                    wins.add(new QuickWin(
                        "JPA",
                        fileResult.filePath().toString(),
                        usage.lineNumber(),
                        usage.annotationName(),
                        usage.jakartaEquivalent(),
                        "Annotation",
                        extractSimpleChange(usage.annotationName(), usage.jakartaEquivalent())
                    ));
                }
            }
        }
    }

    private void collectCdiQuickWins(List<QuickWin> wins) {
        if (scanResults == null) return;
        // CDI results may be stored differently - check available maps
        Map<String, Object> cdiMap = scanResults.beanValidationResults();
        if (cdiMap == null) return;

        for (Object value : cdiMap.values()) {
            if (value instanceof CdiInjectionProjectScanResult cdiResult) {
                for (CdiInjectionScanResult fileResult : cdiResult.fileResults()) {
                    for (CdiInjectionUsage usage : fileResult.usages()) {
                        if (usage.hasJakartaEquivalent() && isSimpleRename(usage.className())) {
                            wins.add(new QuickWin(
                                "CDI",
                                fileResult.filePath().toString(),
                                usage.lineNumber(),
                                usage.className(),
                                usage.jakartaEquivalent(),
                                usage.usageType(),
                                extractSimpleChange(usage.className(), usage.jakartaEquivalent())
                            ));
                        }
                    }
                }
            }
        }
    }

    private void collectServletQuickWins(List<QuickWin> wins) {
        if (scanResults == null || scanResults.servletJspResults() == null) return;

        Map<String, Object> servletMap = scanResults.servletJspResults();
        for (Object value : servletMap.values()) {
            if (value instanceof ServletJspProjectScanResult servletResult) {
                for (ServletJspScanResult fileResult : servletResult.fileResults()) {
                    for (ServletJspUsage usage : fileResult.usages()) {
                        if (usage.hasJakartaEquivalent() && isSimpleRename(usage.className())) {
                            wins.add(new QuickWin(
                                "Servlet",
                                fileResult.filePath().toString(),
                                usage.lineNumber(),
                                usage.className(),
                                usage.jakartaEquivalent(),
                                "Import",
                                extractSimpleChange(usage.className(), usage.jakartaEquivalent())
                            ));
                        }
                    }
                }
            }
        }
    }

    private boolean isSimpleRename(String javaxName) {
        // Simple renames are direct javax.→jakarta. package changes
        // without complex API changes
        String lower = javaxName.toLowerCase();
        return lower.startsWith("javax.persistence") ||
               lower.startsWith("javax.inject") ||
               lower.startsWith("javax.servlet") ||
               lower.startsWith("javax.validation") ||
               lower.startsWith("javax.ws.rs"); // JAX-RS
    }

    private String extractSimpleChange(String from, String to) {
        // Show the simple package change
        return from.replace("javax.", "jakarta.") + " → " + to;
    }

    private String generateQuickWinCards(List<QuickWin> wins) {
        StringBuilder cards = new StringBuilder();

        // Limit to first 12 quick wins to avoid overwhelming
        List<QuickWin> displayWins = wins.size() > 12 ? wins.subList(0, 12) : wins;

        for (QuickWin win : displayWins) {
            cards.append(String.format("""
                <div class="quick-win-card">
                    <div class="quick-win-header">
                        <span class="category-badge category-%s">%s</span>
                        <span class="line-ref">Line %d</span>
                    </div>
                    <div class="quick-win-file">%s</div>
                    <div class="quick-win-change">
                        <span class="change-from">%s</span>
                        <span class="change-arrow">→</span>
                        <span class="change-to">%s</span>
                    </div>
                    <div class="quick-win-type">Type: %s</div>
                </div>
                """,
                win.category().toLowerCase(),
                win.category(),
                win.lineNumber(),
                escapeHtml(win.filePath()),
                escapeHtml(win.from()),
                escapeHtml(win.to()),
                escapeHtml(win.type())
            ));
        }

        if (wins.size() > 12) {
            cards.append(String.format("""
                <div class="quick-win-more">
                    <p>... and %d more quick wins. See detailed findings sections for complete list.</p>
                </div>
                """, wins.size() - 12));
        }

        return cards.toString();
    }

    private int estimateQuickWinTime(int count) {
        // Estimate: 30 seconds per quick win for batch operations
        return Math.max(5, count * 30 / 60);
    }

    private JpaProjectScanResult extractJpaResult(Map<String, Object> jpaMap) {
        for (Object value : jpaMap.values()) {
            if (value instanceof JpaProjectScanResult) {
                return (JpaProjectScanResult) value;
            }
        }
        return null;
    }

    private String generateNoQuickWinsMessage() {
        return """
            <div class="section quick-wins">
                <h2>Quick Wins - Easy Migrations First</h2>
                <div class="no-quick-wins">
                    <p>No simple javax→jakarta renames detected. All migrations appear to require more complex changes.</p>
                    <p>Review the detailed findings sections for migration guidance on complex items.</p>
                </div>
            </div>
            """;
    }

    @Override
    public boolean isApplicable() {
        return scanResults != null && !collectQuickWins().isEmpty();
    }

    @Override
    public int getOrder() {
        return 25; // Right after header, before coverage
    }

    /**
     * Simple record for quick win data
     */
    private record QuickWin(
        String category,
        String filePath,
        int lineNumber,
        String from,
        String to,
        String type,
        String change
    ) {}
}
