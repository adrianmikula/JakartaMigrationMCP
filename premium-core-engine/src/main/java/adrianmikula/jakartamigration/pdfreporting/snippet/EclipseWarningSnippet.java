package adrianmikula.jakartamigration.pdfreporting.snippet;

/**
 * Warning snippet for Eclipse-based projects without build files.
 * Shows when dependency analysis couldn't be performed due to missing Maven/Gradle files.
 */
public class EclipseWarningSnippet extends BaseHtmlSnippet {
    
    private final boolean hasDependencyGraph;
    
    public EclipseWarningSnippet(boolean hasDependencyGraph) {
        this.hasDependencyGraph = hasDependencyGraph;
    }
    
    @Override
    public String generate() throws SnippetGenerationException {
        if (!isApplicable()) {
            return ""; // Return empty if not applicable
        }
        
        return safelyFormat("""
            <div class="warning-box">
                <h3>⚠️ Eclipse Project Detected</h3>
                <p>This appears to be an Eclipse-based project without Maven or Gradle build files. 
                Dependency analysis requires pom.xml, build.gradle, or build.gradle.kts files.</p>
                <p><strong>Recommendations:</strong></p>
                <ul>
                    <li>Consider migrating to Maven or Gradle for comprehensive dependency analysis</li>
                    <li>Add pom.xml or build.gradle files to enable full dependency scanning</li>
                    <li>Run a complete project analysis after build system migration</li>
                </ul>
                <p>The report below includes available scan data only.</p>
            </div>
            """);
    }
    
    @Override
    public boolean isApplicable() {
        // Only show this warning when there's no dependency graph data
        return !hasDependencyGraph;
    }
    
    @Override
    public int getOrder() {
        return 20; // Show after header, before other content
    }
}
