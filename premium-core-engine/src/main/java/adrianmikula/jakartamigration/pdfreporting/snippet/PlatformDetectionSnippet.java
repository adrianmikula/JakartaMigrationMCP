package adrianmikula.jakartamigration.pdfreporting.snippet;

import adrianmikula.jakartamigration.platforms.model.EnhancedPlatformScanResult;
import adrianmikula.jakartamigration.platforms.model.PlatformDetection;

import java.util.List;
import java.util.Map;

/**
 * Displays platform detection results from EnhancedPlatformScanResult.
 * Shows detected application servers, deployment artifacts, and platform details.
 * Risk scores are calculated by RiskScoringService using risk-score.yaml, not in this snippet.
 */
public class PlatformDetectionSnippet extends BaseHtmlSnippet {

    private final EnhancedPlatformScanResult platformResult;

    public PlatformDetectionSnippet(EnhancedPlatformScanResult platformResult) {
        this.platformResult = platformResult;
    }

    @Override
    public String generate() throws SnippetGenerationException {
        if (platformResult == null) {
            return generateNoDataMessage();
        }

        List<String> detectedPlatforms = platformResult.getDetectedPlatforms();
        if (detectedPlatforms == null || detectedPlatforms.isEmpty()) {
            return generateNoPlatformsDetectedMessage();
        }

        return safelyFormat("""
            <div class="section platform-detection">
                <h2>Platform Detection</h2>
                <p>Detected application servers and platforms from project analysis.</p>

                <div class="platform-grid">
                    <div class="platform-card">
                        <h3>Detected Platforms</h3>
                        %s
                    </div>

                    <div class="platform-card">
                        <h3>Deployment Artifacts</h3>
                        %s
                    </div>
                </div>

                %s
                %s
            </div>
            """,
            generatePlatformList(),
            generateArtifactCounts(),
            generateInferredPlatformsSection(),
            generatePlatformDetailsSection()
        );
    }

    private String generatePlatformList() {
        // Use platform details if available, otherwise fall back to simple platform names
        if (platformResult.hasPlatformDetails()) {
            return generateDetailedPlatformList(platformResult.getDetectedPlatformDetails());
        } else {
            return generateSimplePlatformList(platformResult.getDetectedPlatforms());
        }
    }

    private String generateDetailedPlatformList(List<PlatformDetection> platforms) {
        StringBuilder html = new StringBuilder();
        html.append("<ul class=\"platform-list\">");
        for (PlatformDetection platform : platforms) {
            html.append(String.format("""
                <li class="platform-item">
                    <span class="platform-name">%s %s</span>
                    <span class="platform-compat %s">%s</span>
                </li>
                """,
                escapeHtml(platform.platformName()),
                escapeHtml(platform.detectedVersion()),
                platform.isJakartaCompatible() ? "compatible" : "incompatible",
                platform.isJakartaCompatible() ? "Jakarta Compatible" : "Needs Migration"
            ));
        }
        html.append("</ul>");
        return html.toString();
    }

    private String generateSimplePlatformList(List<String> platforms) {
        StringBuilder html = new StringBuilder();
        html.append("<ul class=\"platform-list\">");
        for (String platform : platforms) {
            html.append(String.format("<li class=\"platform-item\"><span class=\"platform-name\">%s</span></li>%n", escapeHtml(platform)));
        }
        html.append("</ul>");
        return html.toString();
    }

    private String generateArtifactCounts() {
        Map<String, Integer> artifacts = platformResult.getDeploymentArtifacts();
        int warCount = artifacts.getOrDefault("war", 0);
        int earCount = artifacts.getOrDefault("ear", 0);
        int jarCount = artifacts.getOrDefault("jar", 0);

        if (warCount == 0 && earCount == 0 && jarCount == 0) {
            return "<p class=\"no-artifacts\">No deployment artifacts detected</p>";
        }

        StringBuilder html = new StringBuilder();
        html.append("<ul class=\"artifact-list\">");
        if (warCount > 0) {
            html.append(String.format("<li>WAR files: %d</li>%n", warCount));
        }
        if (earCount > 0) {
            html.append(String.format("<li>EAR files: %d</li>%n", earCount));
        }
        if (jarCount > 0) {
            html.append(String.format("<li>JAR files: %d</li>%n", jarCount));
        }
        html.append("</ul>");
        return html.toString();
    }

    private String generateInferredPlatformsSection() {
        if (!platformResult.hasInferredPlatforms()) {
            return "";
        }

        StringBuilder items = new StringBuilder();
        for (String platform : platformResult.getInferredPlatforms()) {
            items.append(String.format("<li>%s</li>%n", escapeHtml(platform)));
        }

        return safelyFormat("""
            <div class="platform-inferred">
                <h4>Inferred Platforms</h4>
                <p class="inferred-note">Platforms inferred from deployment artifacts:</p>
                <ul class="platform-list">
                    %s
                </ul>
            </div>
            """,
            items.toString()
        );
    }

    private String generatePlatformDetailsSection() {
        if (!platformResult.hasPlatformDetails()) {
            return "";
        }

        StringBuilder html = new StringBuilder();
        html.append("<div class=\"platform-details\">");
        html.append("<h4>Platform Compatibility Details</h4>");
        
        for (PlatformDetection platform : platformResult.getDetectedPlatformDetails()) {
            html.append(String.format("""
                <div class="platform-detail-item">
                    <strong>%s</strong>:
                    """, escapeHtml(platform.platformName())));
            
            if (platform.isJakartaCompatible()) {
                html.append(String.format("Jakarta EE Compatible (min version: %s)%n", 
                    escapeHtml(platform.minJakartaVersion())));
            } else {
                html.append("Requires migration to Jakarta EE%n");
            }
            
            if (platform.requirements() != null && !platform.requirements().isEmpty()) {
                html.append("<ul class=\"requirements-list\">");
                for (Map.Entry<String, String> req : platform.requirements().entrySet()) {
                    html.append(String.format("<li>%s: %s</li>%n", 
                        escapeHtml(req.getKey()), escapeHtml(req.getValue())));
                }
                html.append("</ul>");
            }
            
            html.append("</div>");
        }
        
        html.append("</div>");
        return html.toString();
    }

    private String generateNoDataMessage() {
        return """
            <div class="section platform-detection">
                <h2>Platform Detection</h2>
                <div class="no-data-message">
                    <p>No platform scan data available. Run platform detection to identify application servers.</p>
                </div>
            </div>
            """;
    }

    private String generateNoPlatformsDetectedMessage() {
        return """
            <div class="section platform-detection">
                <h2>Platform Detection</h2>
                <div class="no-data-message">
                    <p>No application servers detected. This may be a standalone application or library project.</p>
                </div>
            </div>
            """;
    }

    @Override
    public boolean isApplicable() {
        return platformResult != null && platformResult.getDetectedPlatforms() != null;
    }

    @Override
    public int getOrder() {
        return 42; // After Dependency Matrix (40), before Code Examples (44)
    }
}
