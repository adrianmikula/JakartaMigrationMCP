package adrianmikula.jakartamigration.pdfreporting.snippet;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Displays migration strategy cards loaded from property files.
 * Shows the 6 available migration strategies with their benefits and risks.
 */
public class MigrationStrategiesSnippet extends BaseHtmlSnippet {

    private static final String PROPERTIES_FILE = "migration-strategies.properties";
    private static final String[] STRATEGY_KEYS = {
        "big_bang",
        "incremental",
        "transform",
        "microservices",
        "adapter",
        "strangler"
    };

    private final Properties properties;

    public MigrationStrategiesSnippet() {
        this.properties = loadProperties();
    }

    @Override
    public String generate() throws SnippetGenerationException {
        if (properties == null || properties.isEmpty()) {
            return generateNoDataMessage();
        }

        StringBuilder strategyCards = new StringBuilder();
        for (String strategyKey : STRATEGY_KEYS) {
            String displayName = properties.getProperty("strategy." + strategyKey + ".displayName");
            String description = properties.getProperty("strategy." + strategyKey + ".description");
            String benefits = properties.getProperty("strategy." + strategyKey + ".benefits");
            String risks = properties.getProperty("strategy." + strategyKey + ".risks");
            String color = properties.getProperty("strategy." + strategyKey + ".color");

            if (displayName != null && description != null) {
                strategyCards.append(generateStrategyCard(
                    strategyKey,
                    displayName,
                    description,
                    benefits,
                    risks,
                    color
                ));
            }
        }

        return safelyFormat("""
            <div class="section migration-strategies">
                <h2>Migration Strategies</h2>
                <p>Available migration approaches for Jakarta EE transition.</p>

                <div class="strategies-grid">
                    %s
                </div>
            </div>
            """,
            strategyCards.toString()
        );
    }

    private String generateStrategyCard(String strategyKey, String displayName, String description,
                                       String benefits, String risks, String color) {
        String colorStyle = color != null ? "border-left: 4px solid rgb(%s);".formatted(color) : "border-left: 4px solid #3498db;";
        String benefitsHtml = formatBulletList(benefits);
        String risksHtml = formatBulletList(risks);

        return safelyFormat("""
            <div class="strategy-card" style="%s">
                <h3>%s</h3>
                <p class="strategy-description">%s</p>
                <div class="strategy-details">
                    <div class="strategy-section">
                        <h4>✓ Benefits</h4>
                        %s
                    </div>
                    <div class="strategy-section">
                        <h4>⚠ Risks</h4>
                        %s
                    </div>
                </div>
            </div>
            """,
            colorStyle,
            escapeHtml(displayName),
            escapeHtml(description),
            benefitsHtml,
            risksHtml
        );
    }

    private String formatBulletList(String text) {
        if (text == null || text.isBlank()) {
            return "<p>No information available</p>";
        }

        // Split by newlines and convert to bullet points
        String[] lines = text.split("\\\\n");
        StringBuilder html = new StringBuilder("<ul>");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("-") || trimmed.startsWith("•")) {
                trimmed = trimmed.substring(1).trim();
            }
            if (!trimmed.isEmpty()) {
                html.append("<li>").append(escapeHtml(trimmed)).append("</li>");
            }
        }
        html.append("</ul>");
        return html.toString();
    }

    private Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (input == null) {
                return null;
            }
            props.load(input);
        } catch (IOException e) {
            return null;
        }
        return props;
    }

    private String generateNoDataMessage() {
        return """
            <div class="section migration-strategies">
                <h2>Migration Strategies</h2>
                <div class="no-data-message">
                    <p>Migration strategy information not available. Could not load strategy properties.</p>
                </div>
            </div>
            """;
    }

    @Override
    public boolean isApplicable() {
        return properties != null && !properties.isEmpty();
    }

    @Override
    public int getOrder() {
        return 50; // Show after Risk Heat Map
    }
}
