package adrianmikula.jakartamigration.pdfreporting.snippet;

import adrianmikula.jakartamigration.platforms.model.PlatformScanResult;
import adrianmikula.jakartamigration.platforms.model.PlatformDetection;

import java.util.List;

/**
 * Displays platform detection results from PlatformScanResult.
 * Shows detected application servers and platform risk score.
 */
public class PlatformDetectionSnippet extends BaseHtmlSnippet {

    private final PlatformScanResult platformResult;

    public PlatformDetectionSnippet(PlatformScanResult platformResult) {
        this.platformResult = platformResult;
    }

    @Override
    public String generate() throws SnippetGenerationException {
        if (platformResult == null) {
            return generateNoDataMessage();
        }

        List<PlatformDetection> detectedPlatforms = platformResult.detectedPlatforms();
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
                        <ul class="platform-list">
                            %s
                        </ul>
                    </div>

                    <div class="platform-card">
                        <h3>Platform Risk Score</h3>
                        <div class="risk-score-display">
                            <span class="risk-score-value %s">%d</span>
                            <span class="risk-score-label">Total Risk</span>
                        </div>
                    </div>
                </div>

                %s
            </div>
            """,
            generatePlatformList(detectedPlatforms),
            getRiskClass(platformResult.totalRiskScore()),
            platformResult.totalRiskScore(),
            generateRecommendationsSection()
        );
    }

    private String generatePlatformList(List<PlatformDetection> platforms) {
        StringBuilder html = new StringBuilder();
        for (PlatformDetection platform : platforms) {
            html.append(String.format("""
                <li class=\"platform-item\">
                    <span class=\"platform-name\">%s %s</span>
                    <span class=\"platform-compat %s\">%s</span>
                </li>
                """,
                escapeHtml(platform.platformName()),
                escapeHtml(platform.detectedVersion()),
                platform.isJakartaCompatible() ? "compatible" : "incompatible",
                platform.isJakartaCompatible() ? "Jakarta Compatible" : "Needs Migration"
            ));
        }
        return html.toString();
    }

    private String generateRecommendationsSection() {
        List<String> recommendations = platformResult.recommendations();
        if (recommendations == null || recommendations.isEmpty()) {
            return "";
        }

        StringBuilder items = new StringBuilder();
        for (String rec : recommendations) {
            items.append(String.format("<li>%s</li>%n", escapeHtml(rec)));
        }

        return safelyFormat("""
            <div class="platform-recommendations">
                <h4>Platform Recommendations</h4>
                <ul class="recommendation-list">
                    %s
                </ul>
            </div>
            """,
            items.toString()
        );
    }

    private String getRiskClass(int score) {
        if (score < 30) return "low";
        if (score < 60) return "medium";
        if (score < 80) return "high";
        return "critical";
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
        return platformResult != null && platformResult.detectedPlatforms() != null;
    }

    @Override
    public int getOrder() {
        return 42; // After Dependency Matrix (40), before Code Examples (44)
    }
}
