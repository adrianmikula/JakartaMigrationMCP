package adrianmikula.jakartamigration.intellij.service;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * Canonical definition of all advanced scan categories used across the UI.
 * <p>
 * Centralising the category metadata (display label, count extractor, grouping,
 * recipe availability, chart colour) removes the duplicated lists of category names
 * and switch/mapping blocks that previously existed in multiple UI components.
 */
public enum AdvancedScanCategory {
    JPA("JPA Annotations", "JPA", true, true, false, true, new Color(54, 162, 235), AdvancedScanningService.AdvancedScanSummary::getJpaCount),
    BEAN_VALIDATION("Bean Validation", "Bean Validation", true, true, false, true, new Color(255, 99, 132), AdvancedScanningService.AdvancedScanSummary::getBeanValidationCount),
    SERVLET_JSP("Servlet/JSP", "Servlet/JSP", true, true, false, true, new Color(255, 205, 86), AdvancedScanningService.AdvancedScanSummary::getServletJspCount),
    CDI_INJECTION("CDI Injection", "CDI Injection", true, true, false, true, new Color(75, 192, 192), AdvancedScanningService.AdvancedScanSummary::getCdiInjectionCount),
    REST_SOAP("REST/SOAP", "REST/SOAP", true, true, false, true, new Color(153, 102, 255), AdvancedScanningService.AdvancedScanSummary::getRestSoapCount),
    DEPRECATED_API("Deprecated API", "Deprecated API", true, true, false, true, new Color(255, 159, 64), AdvancedScanningService.AdvancedScanSummary::getDeprecatedApiCount),
    SECURITY_API("Security API", "Security API", true, true, false, true, new Color(199, 199, 199), AdvancedScanningService.AdvancedScanSummary::getSecurityApiCount),
    JMS_MESSAGING("JMS Messaging", "JMS Messaging", true, true, false, true, new Color(83, 102, 255), AdvancedScanningService.AdvancedScanSummary::getJmsMessagingCount),
    BUILD_CONFIG("Build Config", "Build Config", true, false, true, true, new Color(40, 167, 69), AdvancedScanningService.AdvancedScanSummary::getBuildConfigCount),
    CONFIG_FILES("Config Files", "Config Files", true, false, true, true, new Color(220, 53, 69), AdvancedScanningService.AdvancedScanSummary::getConfigFileCount),
    TRANSITIVE_DEPENDENCY("Transitive Dependencies", "Transitive Deps", false, false, false, false, new Color(108, 117, 125), AdvancedScanningService.AdvancedScanSummary::getTransitiveDependencyCount),
    CLASSLOADER_MODULE("Classloader", "Classloader", true, false, false, false, new Color(255, 193, 7), AdvancedScanningService.AdvancedScanSummary::getClassloaderModuleCount),
    LOGGING_METRICS("Logging/Metrics", "Logging/Metrics", true, false, false, false, new Color(0, 123, 255), AdvancedScanningService.AdvancedScanSummary::getLoggingMetricsCount),
    SERIALIZATION_CACHE("Serialization/Cache", "Serialization/Cache", true, false, false, false, new Color(142, 68, 173), AdvancedScanningService.AdvancedScanSummary::getSerializationCacheCount),
    THIRD_PARTY_LIBS("Third-Party Libs", "Third-Party", true, false, false, false, new Color(23, 162, 184), AdvancedScanningService.AdvancedScanSummary::getThirdPartyLibCount);

    private final String tabLabel;
    private final String shortLabel;
    private final boolean uiTab;
    private final boolean sourceIssue;
    private final boolean configIssue;
    private final boolean hasRecipe;
    private final Color chartColor;
    private final Function<AdvancedScanningService.AdvancedScanSummary, Integer> countExtractor;

    AdvancedScanCategory(String tabLabel, String shortLabel, boolean uiTab, boolean sourceIssue,
                         boolean configIssue, boolean hasRecipe, Color chartColor,
                         Function<AdvancedScanningService.AdvancedScanSummary, Integer> countExtractor) {
        this.tabLabel = tabLabel;
        this.shortLabel = shortLabel;
        this.uiTab = uiTab;
        this.sourceIssue = sourceIssue;
        this.configIssue = configIssue;
        this.hasRecipe = hasRecipe;
        this.chartColor = chartColor;
        this.countExtractor = countExtractor;
    }

    public String getTabLabel() {
        return tabLabel;
    }

    public String getShortLabel() {
        return shortLabel;
    }

    public boolean isUiTab() {
        return uiTab;
    }

    public boolean isSourceIssue() {
        return sourceIssue;
    }

    public boolean isConfigIssue() {
        return configIssue;
    }

    public boolean hasRecipe() {
        return hasRecipe;
    }

    public Color getChartColor() {
        return chartColor;
    }

    public int getCount(AdvancedScanningService.AdvancedScanSummary summary) {
        if (summary == null) {
            return 0;
        }
        return countExtractor.apply(summary);
    }

    public static List<AdvancedScanCategory> uiTabCategories() {
        return Arrays.stream(values()).filter(AdvancedScanCategory::isUiTab).toList();
    }
}
