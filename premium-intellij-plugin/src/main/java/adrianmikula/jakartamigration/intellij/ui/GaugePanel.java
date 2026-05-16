package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.model.DependencySummary;
import adrianmikula.jakartamigration.intellij.model.MigrationDashboard;
import adrianmikula.jakartamigration.intellij.service.AdvancedScanningService;
import adrianmikula.jakartamigration.risk.RiskScoringService;
import adrianmikula.jakartamigration.risk.EnhancedTestCoverageAnalysisService;
import adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults;
import adrianmikula.jakartamigration.intellij.ui.components.RiskGauge;
import adrianmikula.jakartamigration.intellij.ui.components.EffortGauge;
import adrianmikula.jakartamigration.intellij.ui.components.CombinedConfidenceGauge;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Panel containing risk assessment gauges and explanation panels.
 * Displays Migration Risk, Migration Effort, and Confidence gauges with detailed breakdowns.
 */
public class GaugePanel {
    private static final Logger LOG = Logger.getInstance(GaugePanel.class);

    private final Project project;
    private final EnhancedTestCoverageAnalysisService enhancedTestCoverageService;
    private final AdvancedScanningService advancedScanningService;
    private final Consumer<String> tabSwitcher;
    private PlatformsTabComponent platformsTabComponent;

    // UI Components for gauges
    private JPanel panel;
    private CombinedConfidenceGauge confidenceGauge;
    private RiskGauge migrationRiskGauge;
    private EffortGauge effortScoreGauge;

    // Explanation panels
    private JPanel riskExplanationPanel;
    private JPanel effortExplanationPanel;
    private JPanel confidenceExplanationPanel;

    // Clickable bullet labels for explanations
    private JLabel riskDirectDepsLabel;
    private JLabel riskTransitiveLabel;
    private JLabel riskPlatformsLabel;
    private JLabel riskSourceIssuesLabel;
    private JLabel riskConfigIssuesLabel;

    private JLabel effortRecipesLabel;
    private JLabel effortOrgDepsLabel;
    private JLabel effortProjectSizeLabel;

    private JLabel confidenceScansLabel;
    private JLabel confidenceUnknownDepsLabel;

    // Project size label
    private JBLabel projectSizeValue;

    // Cache for preventing unnecessary updates
    private Integer lastCalculatedRiskScore = null;

    // Current dashboard data
    private MigrationDashboard dashboard;

    public GaugePanel(@NotNull Project project, AdvancedScanningService advancedScanningService, Consumer<String> tabSwitcher) {
        this.project = project;
        this.advancedScanningService = advancedScanningService;
        this.tabSwitcher = tabSwitcher;
        this.enhancedTestCoverageService = EnhancedTestCoverageAnalysisService.getInstance();
        this.panel = createGaugesPanel();
    }

    /**
     * Creates the top section with a 2×3 grid layout.
     * Column 1: Three speedometer-style gauges (one per row)
     * Column 2: Explanatory breakdown panels with clickable bullet links
     *
     * Row 1: Migration Risk Gauge + Risk Breakdown
     * Row 2: Migration Effort Gauge + Effort Breakdown
     * Row 3: Confidence Score Gauge + Confidence Breakdown
     */
    private JPanel createGaugesPanel() {
        JPanel panel = new JBPanel<>(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                createTransparentTitledBorder("Risk Assessment"),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        // Main grid container: 2 columns × 4 rows
        JPanel gridContainer = new JBPanel<>(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 15, 10, 15);
        gbc.fill = GridBagConstraints.NONE;

        // Row 1: Migration Risk
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 0.5;
        gbc.anchor = GridBagConstraints.CENTER;
        migrationRiskGauge = new RiskGauge("Migration Risk");
        gridContainer.add(migrationRiskGauge, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        riskExplanationPanel = createRiskExplanationPanel();
        gridContainer.add(riskExplanationPanel, gbc);

        // Row 2: Migration Effort
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        effortScoreGauge = new EffortGauge("Migration Effort");
        gridContainer.add(effortScoreGauge, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        effortExplanationPanel = createEffortExplanationPanel();
        gridContainer.add(effortExplanationPanel, gbc);

        // Row 3: Confidence Score
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        confidenceGauge = new CombinedConfidenceGauge("Confidence");
        gridContainer.add(confidenceGauge, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        confidenceExplanationPanel = createCombinedConfidenceExplanationPanel();
        gridContainer.add(confidenceExplanationPanel, gbc);

        panel.add(gridContainer, BorderLayout.CENTER);

        // Slider panel for effort estimation inputs
        JPanel slidersPanel = createSlidersPanel();
        panel.add(slidersPanel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Creates the project info panel showing project metadata.
     */
    private JPanel createSlidersPanel() {
        JPanel panel = new JBPanel<>(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 5, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.anchor = GridBagConstraints.WEST;

        // Project Size
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel projectSizeLabel = new JLabel("Project Size:");
        projectSizeLabel.setFont(projectSizeLabel.getFont().deriveFont(Font.PLAIN, 11f));
        panel.add(projectSizeLabel, gbc);

        projectSizeValue = createValueLabel("-");
        gbc.gridx = 1;
        panel.add(projectSizeValue, gbc);

        return panel;
    }

    /**
     * Creates the risk explanation panel with clickable bullet links.
     * Shows breakdown of factors contributing to risk score.
     */
    private JPanel createRiskExplanationPanel() {
        JPanel panel = new JBPanel<>(new GridLayout(5, 1, 2, 2));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // Create clickable bullet labels with initial values
        riskDirectDepsLabel = createClickableBullet("Direct dependencies needing upgrade", "Dependencies", 0);
        riskTransitiveLabel = createClickableBullet("Transitive dependency issues", "Dependencies", 0);
        riskPlatformsLabel = createClickableBullet("Platforms needing upgrade", "Platforms", 0);
        riskSourceIssuesLabel = createClickableBullet("Source code issues", "Source Scans", 0);
        riskConfigIssuesLabel = createClickableBullet("Config/non-source issues", "Source Scans", 0);

        panel.add(riskDirectDepsLabel);
        panel.add(riskTransitiveLabel);
        panel.add(riskPlatformsLabel);
        panel.add(riskSourceIssuesLabel);
        panel.add(riskConfigIssuesLabel);

        return panel;
    }

    /**
     * Creates the effort explanation panel with clickable bullet links.
     * Shows breakdown of factors contributing to effort score.
     */
    private JPanel createEffortExplanationPanel() {
        JPanel panel = new JBPanel<>(new GridLayout(3, 1, 2, 2));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // Create clickable bullet labels with initial values
        effortRecipesLabel = createClickableBullet("Refactors with recipes", "Refactor", 0);
        effortProjectSizeLabel = createClickableBullet("Project complexity (files)", "Dependencies", 0);
        effortOrgDepsLabel = createClickableBullet("Organisational dependencies", "Dependencies", 0);

        panel.add(effortRecipesLabel);
        panel.add(effortProjectSizeLabel);
        panel.add(effortOrgDepsLabel);

        return panel;
    }

    /**
     * Creates the confidence explanation panel with clickable bullet links.
     * Shows breakdown of factors contributing to confidence score.
     */
    private JPanel createCombinedConfidenceExplanationPanel() {
        JPanel panel = new JBPanel<>(new GridLayout(2, 1, 2, 2));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // Create clickable bullet labels with initial values
        confidenceScansLabel = createClickableBullet("Scans completed", "Source Scans", 0);
        confidenceUnknownDepsLabel = createClickableBullet("Unknown/review dependencies", "Dependencies", 0);

        panel.add(confidenceScansLabel);
        panel.add(confidenceUnknownDepsLabel);

        return panel;
    }

    /**
     * Creates a clickable bullet label that navigates to a specific tab when clicked.
     *
     * @param labelText Base text for the label (value will be appended)
     * @param targetTab Tab to switch to when clicked
     * @param initialValue Initial value to display
     * @return Configured JLabel with click listener
     */
    private JLabel createClickableBullet(String labelText, String targetTab, int initialValue) {
        JLabel label = new JLabel(formatBulletText(labelText, initialValue));
        label.setCursor(new Cursor(Cursor.HAND_CURSOR));
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 11f));

        // Add click listener for tab navigation
        label.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (tabSwitcher != null) {
                    tabSwitcher.accept(targetTab);
                }
            }

            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                label.setText("<html><u>" + formatBulletText(labelText, extractValue(label.getText())) + "</u></html>");
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                label.setText(formatBulletText(labelText, extractValue(label.getText())));
            }
        });

        return label;
    }

    /**
     * Formats bullet text with proper HTML styling.
     */
    private String formatBulletText(String labelText, int value) {
        return "<html>&bull; " + labelText + ": " + value + "</html>";
    }

    /**
     * Extracts the numeric value from formatted bullet text.
     */
    private int extractValue(String text) {
        if (text == null) return 0;
        // Remove HTML tags and extract the number after the colon
        String plainText = text.replaceAll("<[^>]*>", "");
        String[] parts = plainText.split(":");
        if (parts.length > 1) {
            try {
                return Integer.parseInt(parts[1].trim().replaceAll("\\D", ""));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * Updates a clickable bullet label with new value and color based on severity.
     *
     * @param label The label to update
     * @param baseText The base text (without value)
     * @param value The value to display
     * @param color The color to apply based on severity
     */
    private void updateBulletLabel(JLabel label, String baseText, int value, Color color) {
        String text = formatBulletText(baseText, value);
        label.setText(text);
        label.setForeground(color);
    }

    /**
     * Determines color based on value and thresholds.
     * Returns appropriate color for severity level.
     *
     * @param value The metric value
     * @param thresholds Array of 3 threshold values: [green/yellow, yellow/orange, orange/red]
     * @param isPositiveMetric If true, higher values are good (colors inverted)
     * @return Color based on severity
     */
    private Color getColorForMetric(int value, int[] thresholds, boolean isPositiveMetric) {
        // Define colors
        Color green = new Color(40, 167, 69);
        Color yellow = new Color(255, 193, 7);
        Color orange = new Color(255, 165, 0);
        Color red = new Color(220, 53, 69);

        if (isPositiveMetric) {
            // Higher is better (e.g., test coverage)
            if (value >= thresholds[2]) return green;
            if (value >= thresholds[1]) return yellow;
            if (value >= thresholds[0]) return orange;
            return red;
        } else {
            // Lower is better (e.g., issues count)
            if (value <= thresholds[0]) return green;
            if (value <= thresholds[1]) return yellow;
            if (value <= thresholds[2]) return orange;
            return red;
        }
    }

    /**
     * Creates a titled border with transparent border lines.
     * Keeps the title text but makes the border itself invisible.
     */
    private javax.swing.border.Border createTransparentTitledBorder(String title) {
        // Create an empty border (no visible lines)
        javax.swing.border.Border emptyBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);
        // Create a titled border with the empty border
        javax.swing.border.TitledBorder titledBorder = BorderFactory.createTitledBorder(emptyBorder, title);
        titledBorder.setTitleFont(titledBorder.getTitleFont().deriveFont(Font.BOLD, 12f));
        return titledBorder;
    }


    private JBLabel createValueLabel(String text) {
        JBLabel label = new JBLabel(text, SwingConstants.LEFT);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
        return label;
    }

    /**
     * Gets the main panel for this component.
     */
    public JPanel getPanel() {
        return panel;
    }

    /**
     * Sets the platforms tab component for integration.
     */
    public void setPlatformsTabComponent(PlatformsTabComponent platformsTabComponent) {
        this.platformsTabComponent = platformsTabComponent;
    }

    /**
     * Sets the migration dashboard data for this component.
     */
    public void setDashboard(MigrationDashboard dashboard) {
        this.dashboard = dashboard;
        updateGauges();
    }

    /**
     * Updates the gauges with current risk scores and confidence score.
     */
    public void updateGauges() {
        if (dashboard == null) {
            return;
        }

        // Calculate confidence score (percentage of dependencies with known Jakarta status)
        int confidenceScore = calculateConfidenceScore();
        confidenceGauge.setScore(confidenceScore);

        // Calculate migration risk score (using RiskScoringService)
        RiskScoringService riskScoringService = RiskScoringService.getInstance();
        Map<String, Integer> depIssues = new HashMap<>();
        
        // Build dependency issues map (excluding jakarta-compatible dependencies)
        DependencySummary depSummary = dashboard.getDependencySummary();
        if (depSummary != null) {
            int noSupport = depSummary.getNoJakartaSupportCount() != null ? depSummary.getNoJakartaSupportCount() : 0;
            int blockers = depSummary.getBlockerDependencies() != null ? depSummary.getBlockerDependencies() : 0;
            int affected = depSummary.getAffectedDependencies() != null ? depSummary.getAffectedDependencies() : 0;
            int transitiveDeps = depSummary.getTransitiveDependencies() != null ? depSummary.getTransitiveDependencies() : 0;

            // Merge blocker and no_jakarta_upgrade into single category
            int mergedNoUpgrade = noSupport + blockers;
            if (mergedNoUpgrade > 0) {
                depIssues.put("noJakartaUpgrade", mergedNoUpgrade * 25);
            }
            if (affected > 0) {
                depIssues.put("directDependency", affected * 10);
            }
            if (transitiveDeps > 0) {
                depIssues.put("transitiveDependency", (int) Math.round(transitiveDeps * 0.1));
            }
        }
        
        // Note: Scan findings excluded from risk calculation per new formula
        // Calculate risk score without scan findings and validation confidence
        int totalFileCount = getTotalFileCount();
        int testFileCount = getTestFileCount();
        double platformRiskScore = getPlatformRiskScore();
        // Estimate integration tests and critical modules (simplified for now)
        int integrationTestCount = estimateIntegrationTestCount();
        int criticalModulesTested = estimateCriticalModulesTested();
        
        // Pass empty scan findings to exclude them from calculation
        RiskScoringService.RiskScore riskScore = riskScoringService.calculateRiskScore(
            new HashMap<>(), // Empty scan findings - excluded from risk calculation
            depIssues,
            totalFileCount,
            platformRiskScore,
            testFileCount,
            integrationTestCount,
            criticalModulesTested
        );
        int newScore = (int) Math.round(riskScore.totalScore());
        
        // Only update gauge if score actually changed
        if (lastCalculatedRiskScore == null || !lastCalculatedRiskScore.equals(newScore)) {
            lastCalculatedRiskScore = newScore;
            migrationRiskGauge.setScore(newScore);
        }

        // Calculate migration effort score (combining automation potential and test coverage)
        int effortScore = calculateEffortScore();
        effortScoreGauge.setScore(effortScore);

        // Calculate enhanced validation confidence using test coverage analysis
        calculateEnhancedValidationConfidence(riskScore);

        // Update explanation panels with current data and colors
        updateRiskExplanation();
        updateEffortExplanation();
        updateConfidenceExplanation();
        
    }

    /**
     * Calculates enhanced validation confidence using comprehensive test coverage analysis.
     */
    private int calculateEnhancedValidationConfidence(RiskScoringService.RiskScore riskScore) {
        try {
            if (project != null && project.getBasePath() != null) {
                // Gather migration issues for correlation analysis
                Map<String, List<String>> migrationIssues = collectMigrationIssues();

                // Perform enhanced test coverage analysis
                var analysis = enhancedTestCoverageService.analyzeTestCoverage(
                    project.getBasePath(), migrationIssues);

                // Return the validation confidence score
                return (int) Math.round(analysis.validationConfidenceScore);
            }
        } catch (Exception e) {
            LOG.warn("Failed to calculate enhanced validation confidence, falling back to basic score", e);
        }

        // Fallback to basic score from risk calculation
        Integer basicScore = riskScore.componentScores().get("validationConfidence");
        return basicScore != null ? basicScore : 50;
    }

    /**
     * Collects migration issues from the current dashboard data for correlation analysis.
     */
    private Map<String, List<String>> collectMigrationIssues() {
        Map<String, List<String>> issues = new HashMap<>();

        if (dashboard != null && dashboard.getDependencySummary() != null) {
            DependencySummary depSummary = dashboard.getDependencySummary();
            List<String> mainIssues = new ArrayList<>();

            if (depSummary.getNoJakartaSupportCount() != null && depSummary.getNoJakartaSupportCount() > 0) {
                mainIssues.add("javax.servlet usage - no Jakarta equivalent");
            }
            if (depSummary.getBlockerDependencies() != null && depSummary.getBlockerDependencies() > 0) {
                mainIssues.add("blocked dependencies requiring manual intervention");
            }
            if (depSummary.getAffectedDependencies() != null && depSummary.getAffectedDependencies() > 0) {
                mainIssues.add("dependencies needing javax to jakarta migration");
            }

            if (!mainIssues.isEmpty()) {
                issues.put("main", mainIssues);
            }
        }

        return issues;
    }

    /**
     * Updates the risk explanation panel with current data and severity colors.
     */
    private void updateRiskExplanation() {
        if (dashboard == null) return;

        DependencySummary depSummary = dashboard.getDependencySummary();
        if (depSummary == null) return;

        // Get values for risk factors
        int affectedDeps = depSummary.getAffectedDependencies() != null ? depSummary.getAffectedDependencies() : 0;
        int transitiveDeps = depSummary.getTransitiveDependencies() != null ? depSummary.getTransitiveDependencies() : 0;
        int platformsNeedingUpgrade = getPlatformsNeedingUpgradeCount();
        int sourceIssues = getSourceCodeIssuesCount();
        int configIssues = getConfigIssuesCount();

        // Update labels with color coding (thresholds: [green, yellow, orange])
        updateBulletLabel(riskDirectDepsLabel, "Direct dependencies needing upgrade", affectedDeps,
            getColorForMetric(affectedDeps, new int[]{0, 5, 10}, false));
        updateBulletLabel(riskTransitiveLabel, "Transitive dependency issues", transitiveDeps,
            getColorForMetric(transitiveDeps, new int[]{0, 10, 20}, false));
        updateBulletLabel(riskPlatformsLabel, "Platforms needing upgrade", platformsNeedingUpgrade,
            getColorForMetric(platformsNeedingUpgrade, new int[]{0, 1, 2}, false));
        updateBulletLabel(riskSourceIssuesLabel, "Source code issues", sourceIssues,
            getColorForMetric(sourceIssues, new int[]{0, 5, 15}, false));
        updateBulletLabel(riskConfigIssuesLabel, "Config/non-source issues", configIssues,
            getColorForMetric(configIssues, new int[]{0, 3, 8}, false));
    }

    /**
     * Updates the effort explanation panel with current data and severity colors.
     */
    private void updateEffortExplanation() {
        if (dashboard == null) return;

        DependencySummary depSummary = dashboard.getDependencySummary();

        // Get values for effort factors
        int recipesWithMatches = getRecipesWithMatchesCount();
        int projectFiles = getTotalFileCount();
        int orgDeps = depSummary != null && depSummary.getOrganisationalDependencies() != null
            ? depSummary.getOrganisationalDependencies() : 0;

        // Update labels with color coding
        updateBulletLabel(effortRecipesLabel, "Refactors with recipes", recipesWithMatches,
            getColorForMetric(recipesWithMatches, new int[]{0, 5, 10}, false));
        updateBulletLabel(effortProjectSizeLabel, "Project complexity (files)", projectFiles,
            getColorForMetric(projectFiles, new int[]{100, 1000, 5000}, true));
        updateBulletLabel(effortOrgDepsLabel, "Organisational dependencies", orgDeps,
            getColorForMetric(orgDeps, new int[]{0, 3, 8}, false));
    }

    /**
     * Updates the confidence explanation panel with current data and severity colors.
     */
    private void updateConfidenceExplanation() {
        if (dashboard == null) return;

        DependencySummary depSummary = dashboard.getDependencySummary();

        // Get values for confidence factors
        int totalDeps = depSummary != null && depSummary.getTotalDependencies() != null
            ? depSummary.getTotalDependencies() : 0;
        
        // Calculate dependency knowledge percentage
        int knownPercentage = 0;
        if (totalDeps > 0) {
            int knownDeps = 0;
            if (depSummary.getJakartaUpgradeCount() != null) {
                knownDeps += depSummary.getJakartaUpgradeCount();
            }
            if (depSummary.getJakartaCompatibleCount() != null) {
                knownDeps += depSummary.getJakartaCompatibleCount();
            }
            if (depSummary.getNoJakartaSupportCount() != null) {
                knownDeps += depSummary.getNoJakartaSupportCount();
            }
            if (depSummary.getBlockerDependencies() != null) {
                knownDeps += depSummary.getBlockerDependencies();
            }
            knownPercentage = (knownDeps * 100) / totalDeps;
        }

        int unknownDeps = totalDeps - (depSummary != null && depSummary.getOrganisationalDependencies() != null 
            ? depSummary.getOrganisationalDependencies() : 0);

        // Update labels with color coding
        updateBulletLabel(confidenceScansLabel, "Scans completed", knownPercentage,
            getColorForMetric(knownPercentage, new int[]{50, 75, 90}, true));
        updateBulletLabel(confidenceUnknownDepsLabel, "Unknown/review dependencies", unknownDeps,
            getColorForMetric(unknownDeps, new int[]{0, 3, 8}, false));
    }

    // Helper methods that need access to dashboard data
    // These are placeholders - the actual implementations should be in DashboardComponent
    // or we need to pass the necessary data through the interface

    private int calculateConfidenceScore() {
        if (dashboard == null || dashboard.getDependencySummary() == null) {
            return 0;
        }
        DependencySummary depSummary = dashboard.getDependencySummary();
        int totalDeps = depSummary.getTotalDependencies() != null ? depSummary.getTotalDependencies() : 0;
        if (totalDeps == 0) return 100;

        int knownDeps = 0;
        if (depSummary.getJakartaUpgradeCount() != null) {
            knownDeps += depSummary.getJakartaUpgradeCount();
        }
        if (depSummary.getJakartaCompatibleCount() != null) {
            knownDeps += depSummary.getJakartaCompatibleCount();
        }
        if (depSummary.getNoJakartaSupportCount() != null) {
            knownDeps += depSummary.getNoJakartaSupportCount();
        }
        if (depSummary.getBlockerDependencies() != null) {
            knownDeps += depSummary.getBlockerDependencies();
        }
        return (knownDeps * 100) / totalDeps;
    }

    // Helper methods for effort score calculation - defined before calculateEffortScore
    private boolean hasJavaMajorVersionChange() {
        return false;
    }

    private boolean hasAppserverPlatformChange() {
        return false;
    }

    private int calculateScanFindingsScore() {
        if (advancedScanningService == null || !advancedScanningService.hasCachedResults()) {
            return 0;
        }

        AdvancedScanningService.AdvancedScanSummary summary = advancedScanningService.getCachedSummary();
        if (summary == null) {
            return 0;
        }

        int totalFindings = summary.getJpaCount() +
                           summary.getBeanValidationCount() +
                           summary.getServletJspCount() +
                           summary.getCdiInjectionCount() +
                           summary.getBuildConfigCount() +
                           summary.getRestSoapCount() +
                           summary.getDeprecatedApiCount() +
                           summary.getSecurityApiCount() +
                           summary.getJmsMessagingCount() +
                           summary.getConfigFileCount();

        if (totalFindings <= 0) {
            return 0;
        }

        double logDivisor = 3.0;
        double logScore = Math.log10(totalFindings + 1) / logDivisor * 100.0;

        return Math.max(0, Math.min(100, (int) Math.round(logScore)));
    }

    private int calculateJakartaUpgradeScore() {
        if (dashboard == null || dashboard.getDependencySummary() == null) {
            return 0;
        }

        DependencySummary depSummary = dashboard.getDependencySummary();
        int jakartaUpgrade = depSummary.getJakartaUpgradeCount() != null
            ? depSummary.getJakartaUpgradeCount()
            : 0;

        if (jakartaUpgrade <= 0) {
            return 0;
        }

        int maxThreshold = 50;
        double ratio = Math.min(jakartaUpgrade / (double) maxThreshold, 1.0);
        return (int) Math.round(ratio * 100);
    }

    private int calculateDockerfilesScore() {
        if (advancedScanningService == null || !advancedScanningService.hasCachedResults()) {
            return 0;
        }

        try {
            ComprehensiveScanResults scanResults = advancedScanningService.getLastScanResults();
            if (scanResults == null) {
                return 0;
            }

            int dockerFileCount = 0;
            int maxThreshold = 10;
            double ratio = Math.min(dockerFileCount / (double) maxThreshold, 1.0);
            return (int) Math.round(ratio * 100);
        } catch (Exception e) {
            LOG.warn("Could not calculate Docker files score: " + e.getMessage());
            return 0;
        }
    }

    private int calculateCicdScriptsScore() {
        if (advancedScanningService == null || !advancedScanningService.hasCachedResults()) {
            return 0;
        }

        try {
            ComprehensiveScanResults scanResults = advancedScanningService.getLastScanResults();
            if (scanResults == null) {
                return 0;
            }

            int cicdScriptCount = 0;
            int maxThreshold = 5;
            double ratio = Math.min(cicdScriptCount / (double) maxThreshold, 1.0);
            return (int) Math.round(ratio * 100);
        } catch (Exception e) {
            LOG.warn("Could not calculate CI/CD scripts score: " + e.getMessage());
            return 0;
        }
    }

    private int calculateAutomationScore() {
        if (advancedScanningService == null || !advancedScanningService.hasCachedResults()) {
            return 100;
        }

        AdvancedScanningService.AdvancedScanSummary summary = advancedScanningService.getCachedSummary();
        if (summary == null) {
            return 100;
        }

        int totalIssues = summary.getTotalIssuesFound();
        if (totalIssues == 0) {
            return 0;
        }

        int issuesWithRecipes = getIssuesWithMatchingRecipes(summary);
        int issuesWithoutRecipes = totalIssues - issuesWithRecipes;
        int automationScore = (int) Math.round((issuesWithoutRecipes * 100.0) / totalIssues);

        return Math.max(0, Math.min(100, automationScore));
    }

    private int calculateProjectSizeScore(int maxThreshold) {
        if (dashboard == null || dashboard.getDependencySummary() == null) {
            return 0;
        }

        DependencySummary depSummary = dashboard.getDependencySummary();
        int totalFiles = depSummary.getTotalDependencies() != null
            ? depSummary.getTotalDependencies()
            : 0;

        if (totalFiles == 0) {
            return 0;
        }

        double ratio = Math.min(totalFiles / (double) maxThreshold, 1.0);
        return (int) Math.round(ratio * 100);
    }

    private int getIssuesWithMatchingRecipes(AdvancedScanningService.AdvancedScanSummary summary) {
        if (summary == null) {
            return 0;
        }

        int issuesWithRecipes = 0;

        Map<String, Integer> scanTypeCounts = new HashMap<>();
        scanTypeCounts.put("jpa", summary.getJpaCount());
        scanTypeCounts.put("beanValidation", summary.getBeanValidationCount());
        scanTypeCounts.put("servletJsp", summary.getServletJspCount());
        scanTypeCounts.put("cdiInjection", summary.getCdiInjectionCount());
        scanTypeCounts.put("restSoap", summary.getRestSoapCount());
        scanTypeCounts.put("securityApi", summary.getSecurityApiCount());
        scanTypeCounts.put("jmsMessaging", summary.getJmsMessagingCount());
        scanTypeCounts.put("buildConfig", summary.getBuildConfigCount());
        scanTypeCounts.put("configFiles", summary.getConfigFileCount());
        scanTypeCounts.put("deprecatedApi", summary.getDeprecatedApiCount());
        scanTypeCounts.put("transitiveDependency", summary.getTransitiveDependencyCount());

        String[] scanTypesWithRecipes = {
            "jpa", "beanValidation", "servletJsp", "cdiInjection", "restSoap",
            "securityApi", "jmsMessaging", "buildConfig", "configFiles", "deprecatedApi"
        };

        for (String scanType : scanTypesWithRecipes) {
            Integer count = scanTypeCounts.get(scanType);
            if (count != null && count > 0) {
                issuesWithRecipes += count;
            }
        }

        return issuesWithRecipes;
    }

    private int calculateEffortScore() {
        if (dashboard == null) {
            return 0;
        }

        // Check for conditional weighting based on major version changes
        boolean hasJavaMajorVersionChange = hasJavaMajorVersionChange();
        boolean hasAppserverPlatformChange = hasAppserverPlatformChange();
        
        // Base weights from YAML
        double scanFindingsWeight = 0.20;
        double jakartaUpgradeWeight = 0.20;
        double dockerfilesWeight = 0.15;
        double cicdScriptsWeight = 0.15;
        double automationWeight = 0.15;
        double projectSizeWeight = 0.15;
        
        // Apply conditional weighting if major version changes detected
        if (hasJavaMajorVersionChange || hasAppserverPlatformChange) {
            // Use conditional weights (5% each for Docker and CI/CD scripts)
            dockerfilesWeight = 0.05;
            cicdScriptsWeight = 0.05;
            // Reduce other weights to accommodate the conditional weights
            automationWeight = 0.175; // (0.20 - 0.05 - 0.05) = 0.10
            projectSizeWeight = 0.175; // (0.20 - 0.05 - 0.05) = 0.10
        } else {
            // Normal case: use 0 weight for Docker and CI/CD scripts
            dockerfilesWeight = 0.0;
            cicdScriptsWeight = 0.0;
            // Use normal weights for other factors
            automationWeight = 0.20;
            projectSizeWeight = 0.20;
        }

        // Calculate scan findings score (logarithmic scale)
        int scanFindingsScore = calculateScanFindingsScore();

        // Calculate jakarta upgrade dependencies score
        int jakartaUpgradeScore = calculateJakartaUpgradeScore();
        
        // Calculate Docker files score
        int dockerfilesScore = calculateDockerfilesScore();
        
        // Calculate CI/CD scripts score
        int cicdScriptsScore = calculateCicdScriptsScore();

        // Calculate automation score (percentage of issues WITHOUT recipe matches)
        int automationScore = calculateAutomationScore();

        // Calculate project size score (larger projects = higher effort)
        int projectSizeScore = calculateProjectSizeScore(10000); // Use fixed threshold

        // Combine scores using new weights
        int combinedScore = (int) Math.round(
            (scanFindingsScore * scanFindingsWeight) +
            (jakartaUpgradeScore * jakartaUpgradeWeight) +
            (dockerfilesScore * dockerfilesWeight) +
            (cicdScriptsScore * cicdScriptsWeight) +
            (automationScore * automationWeight) +
            (projectSizeScore * projectSizeWeight)
        );

        // Ensure score is within 0-100 range
        return Math.max(0, Math.min(100, combinedScore));
    }

    private int getTotalFileCount() {
        try {
            if (project != null && project.getBasePath() != null) {
                java.nio.file.Path projectPath = java.nio.file.Paths.get(project.getBasePath());
                return countFilesRecursively(projectPath);
            }
        } catch (Exception e) {
            LOG.warn("Could not count project files: " + e.getMessage());
        }
        return 0;
    }
    
    /**
     * Recursively counts files in the project directory.
     */
    private int countFilesRecursively(java.nio.file.Path path) {
        try {
            return java.nio.file.Files.walk(path)
                    .filter(java.nio.file.Files::isRegularFile)
                    .filter(p -> {
                        String fileName = p.getFileName().toString();
                        // Count only source files and configuration files
                        return fileName.endsWith(".java") || fileName.endsWith(".xml") ||
                               fileName.endsWith(".properties") || fileName.endsWith(".yml") ||
                               fileName.endsWith(".yaml") || fileName.endsWith(".kt") ||
                               fileName.endsWith(".scala") || fileName.endsWith(".groovy");
                    })
                    .mapToInt(p -> 1)
                    .limit(10000) // Cap at 10000 files for performance
                    .sum();
        } catch (Exception e) {
            LOG.warn("Error counting files: " + e.getMessage());
            return 0;
        }
    }

    private int getTestFileCount() {
        try {
            if (project != null && project.getBasePath() != null) {
                java.nio.file.Path projectPath = java.nio.file.Paths.get(project.getBasePath());
                return countTestFilesRecursively(projectPath);
            }
        } catch (Exception e) {
            LOG.warn("Could not count test files: " + e.getMessage());
        }
        return 0;
    }
    
    /**
     * Recursively counts test files in the project directory.
     */
    private int countTestFilesRecursively(java.nio.file.Path path) {
        try {
            return java.nio.file.Files.walk(path)
                    .filter(java.nio.file.Files::isRegularFile)
                    .filter(p -> {
                        String fileName = p.getFileName().toString();
                        String pathStr = p.toString().toLowerCase();
                        // Count test files by naming patterns and locations
                        return fileName.endsWith("Test.java") || fileName.endsWith("Tests.java") ||
                               fileName.endsWith(".test.java") || fileName.endsWith(".tests.java") ||
                               pathStr.contains("/test/") || pathStr.contains("\\test\\") ||
                               pathStr.contains("/tests/") || pathStr.contains("\\tests\\");
                    })
                    .mapToInt(p -> 1)
                    .limit(10000) // Cap at 10000 files for performance
                    .sum();
        } catch (Exception e) {
            LOG.warn("Error counting test files: " + e.getMessage());
            return 0;
        }
    }

    private double getPlatformRiskScore() {
        try {
            if (platformsTabComponent != null) {
                return platformsTabComponent.getCurrentPlatformRiskScore();
            }
            // Default low risk if no platforms tab component available
            return 1.0;
        } catch (Exception e) {
            LOG.warn("Could not calculate platform risk: " + e.getMessage());
            return 1.0; // Default low risk
        }
    }

    private int estimateIntegrationTestCount() {
        int testFiles = getTestFileCount();
        return (int) Math.round(testFiles * 0.2);
    }

    private int estimateCriticalModulesTested() {
        int totalFiles = getTotalFileCount();
        // Assume modules are roughly 50 files each
        int estimatedModules = Math.max(totalFiles / 50, 1);
        return (int) Math.round(estimatedModules * 0.3);
    }

    private int getPlatformsNeedingUpgradeCount() {
        // This would typically come from the platforms tab component
        // For now, return 0 or get from platformsTabComponent if available
        if (platformsTabComponent != null) {
            // This is a placeholder - actual implementation would depend on
            // what platformsTabComponent exposes
            return 0;
        }
        return 0;
    }

    private int getSourceCodeIssuesCount() {
        if (advancedScanningService == null || !advancedScanningService.hasCachedResults()) {
            return 0;
        }
        AdvancedScanningService.AdvancedScanSummary summary = advancedScanningService.getCachedSummary();
        if (summary == null) return 0;

        // Sum all source code related scan issues
        return summary.getJpaCount() + summary.getBeanValidationCount() + summary.getServletJspCount()
            + summary.getCdiInjectionCount() + summary.getRestSoapCount() + summary.getDeprecatedApiCount()
            + summary.getSecurityApiCount() + summary.getJmsMessagingCount();
    }

    private int getConfigIssuesCount() {
        if (advancedScanningService == null || !advancedScanningService.hasCachedResults()) {
            return 0;
        }
        AdvancedScanningService.AdvancedScanSummary summary = advancedScanningService.getCachedSummary();
        if (summary == null) return 0;

        // Sum all config related scan issues
        return summary.getBuildConfigCount() + summary.getConfigFileCount();
    }

    private int getRecipesWithMatchesCount() {
        if (advancedScanningService == null || !advancedScanningService.hasCachedResults()) {
            return 0;
        }
        AdvancedScanningService.AdvancedScanSummary summary = advancedScanningService.getCachedSummary();
        if (summary == null) return 0;

        return getIssuesWithMatchingRecipes(summary);
    }
}
