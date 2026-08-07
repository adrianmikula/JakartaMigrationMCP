package adrianmikula.jakartamigration.intellij.service;

import adrianmikula.jakartamigration.intellij.ui.UIColors;
import adrianmikula.jakartamigration.intellij.util.NotificationHelper;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Central service for the $99 48-hour Migration Risk Audit upsell CTA.
 * <p>
 * - Detects complex patterns after scans that the plugin cannot fully migrate automatically.
 * - Shows a non-intrusive balloon with the in-scan CTA.
 * - Provides a reusable "Get Help" button that opens the audit landing page.
 * - Tracks shown/click events via simple Logger lines (no new external analytics dependencies).
 */
public class AuditUpsellService {

    private static final Logger LOG = Logger.getInstance(AuditUpsellService.class);
    private static final String CONFIG_FILE = "/audit-upsell-config.properties";
    private static final Properties CONFIG = loadConfig();

    private static final String NOTIFICATION_GROUP_ID = "JakartaMigration.Notifications";

    private static final Set<String> DISMISSED_PROJECTS = ConcurrentHashMap.newKeySet();
    private static final AtomicInteger SHOWN_COUNT = new AtomicInteger(0);
    private static final AtomicInteger CLICKED_COUNT = new AtomicInteger(0);

    private AuditUpsellService() {
        // Utility/service class
    }

    /**
     * Loads the audit upsell configuration from {@code /audit-upsell-config.properties}.
     * The source of truth for all upsell messaging and thresholds is the properties file.
     */
    private static Properties loadConfig() {
        Properties props = new Properties();
        try (InputStream is = AuditUpsellService.class.getResourceAsStream(CONFIG_FILE)) {
            if (is != null) {
                props.load(is);
                LOG.info("Loaded audit upsell configuration from " + CONFIG_FILE);
            } else {
                LOG.warn("Audit upsell configuration not found at " + CONFIG_FILE);
            }
        } catch (IOException e) {
            LOG.warn("Failed to load audit upsell configuration", e);
        }

        // Allow system properties to override individual settings at runtime
        for (String key : props.stringPropertyNames()) {
            String systemValue = System.getProperty(key);
            if (systemValue != null) {
                props.setProperty(key, systemValue);
            }
        }

        return props;
    }

    public static String getLandingPageUrl() {
        return getStringProperty("audit.landing.page.url");
    }

    public static String getCtaTitle() {
        return getStringProperty("audit.cta.title");
    }

    public static String getCtaSubtitle() {
        return getStringProperty("audit.cta.subtitle");
    }

    public static String getCtaButtonText() {
        return getStringProperty("audit.cta.button.text");
    }

    private static String getInscanTitle() {
        return getStringProperty("audit.inscan.title");
    }

    private static String getInscanMessage() {
        return getStringProperty("audit.inscan.message");
    }

    private static String getStringProperty(String key) {
        return CONFIG.getProperty(key, "");
    }

    private static int getIntProperty(String key, int defaultValue) {
        String value = CONFIG.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                LOG.warn("Invalid integer for " + key + ": " + value + ", using default " + defaultValue);
            }
        }
        return defaultValue;
    }

    /**
     * Determines whether the CTA should be shown based on the dashboard dial values
     * and the thresholds defined in {@code audit-upsell-config.properties}.
     */
    public static boolean shouldShowCta(int complexity, int risk, int automation) {
        int complexityThreshold = getIntProperty("audit.dial.complexity.high.threshold", 70);
        int riskThreshold = getIntProperty("audit.dial.risk.high.threshold", 70);
        int automationLowThreshold = getIntProperty("audit.dial.automation.low.threshold", 30);

        return complexity >= complexityThreshold
                || risk >= riskThreshold
                || automation <= automationLowThreshold;
    }

    /**
     * Shows the in-scan CTA balloon if any dashboard dial exceeds the configured threshold.
     */
    public static void showCtaIfNeeded(@NotNull Project project,
                                       int complexity, int risk, int automation) {
        if (!shouldShowCta(complexity, risk, automation)) {
            return;
        }

        String projectKey = getProjectKey(project);
        if (projectKey != null && DISMISSED_PROJECTS.contains(projectKey)) {
            return;
        }

        Notification notification = NotificationGroupManager.getInstance()
                .getNotificationGroup(NOTIFICATION_GROUP_ID)
                .createNotification(getInscanTitle(), getInscanMessage(), NotificationType.INFORMATION);

        notification.addAction(new NotificationAction(getCtaButtonText()) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification n) {
                recordClick("in_scan_notification");
                openAuditLandingPage(project);
                n.expire();
            }
        });

        notification.addAction(new NotificationAction("Don't show again for this project") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification n) {
                if (projectKey != null) {
                    DISMISSED_PROJECTS.add(projectKey);
                    LOG.info("User dismissed audit CTA for project: " + projectKey);
                }
                n.expire();
            }
        });

        Notifications.Bus.notify(notification, project);
        recordShown("in_scan_notification");
    }

    /**
     * Opens the audit landing page (from config) in the user's default browser.
     */
    public static void openAuditLandingPage(Project project) {
        String url = getLandingPageUrl();
        if (url == null || url.isEmpty()) {
            LOG.error("No audit landing page configured");
            NotificationHelper.showError(project, "Error",
                    "No audit landing page is configured.");
            return;
        }
        try {
            Desktop.getDesktop().browse(new URI(url));
            LOG.info("Opened audit landing page: " + url);
        } catch (Exception ex) {
            LOG.warn("Failed to open audit landing page: " + url, ex);
            NotificationHelper.showError(project, "Error",
                    "Could not open the audit page. URL: " + url);
        }
    }

    /**
     * Creates a prominent bottom bar with large text and a CTA button.
     * The bar is intended to be docked at the bottom of the main tool window.
     */
    public static JPanel createProminentGetHelpBar(Project project, String source) {
        JPanel bar = new JPanel(new BorderLayout(16, 0));
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIColors.BORDER),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));
        bar.setBackground(UIColors.PANEL_BACKGROUND);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel title = new JLabel(getCtaTitle());
        title.setFont(title.getFont().deriveFont(Font.BOLD, 15f));
        title.setForeground(UIColors.TEXT_PRIMARY);
        textPanel.add(title);

        textPanel.add(Box.createVerticalStrut(3));

        JLabel subtitle = new JLabel(getCtaSubtitle());
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 12f));
        subtitle.setForeground(UIColors.TEXT_SECONDARY);
        textPanel.add(subtitle);

        bar.add(textPanel, BorderLayout.CENTER);

        JButton button = new JButton(getCtaButtonText());
        button.setFont(button.getFont().deriveFont(Font.BOLD, 13f));
        button.setToolTipText(getCtaSubtitle());
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(e -> {
            recordClick(source);
            openAuditLandingPage(project);
        });

        bar.add(button, BorderLayout.EAST);
        return bar;
    }

    /**
     * Creates a prominent warning box for the Risk tab that explains why the migration is
     * considered too complex for the plugin to fully automate.
     *
     * @param project    the project context
     * @param complexity the current Complexity dial value
     * @param risk       the current Risk dial value
     * @param automation the current Automation dial value
     * @return a warning panel, or {@code null} when no dial exceeds its threshold
     */
    public static JPanel createCtaWarningPanel(Project project,
                                               int complexity, int risk, int automation) {
        if (!shouldShowCta(complexity, risk, automation)) {
            return null;
        }

        int complexityThreshold = getIntProperty("audit.dial.complexity.high.threshold", 70);
        int riskThreshold = getIntProperty("audit.dial.risk.high.threshold", 70);
        int automationLowThreshold = getIntProperty("audit.dial.automation.low.threshold", 30);

        StringBuilder html = new StringBuilder();
        html.append("<html>");
        html.append("<b>").append(getStringProperty("audit.warning.title")).append("</b><br/>");
        html.append(getStringProperty("audit.warning.intro")).append("<ul>");
        if (complexity >= complexityThreshold) {
            html.append("<li>")
                .append(String.format(getStringProperty("audit.warning.complexity.text"), complexity));
        }
        if (risk >= riskThreshold) {
            html.append("<li>")
                .append(String.format(getStringProperty("audit.warning.risk.text"), risk));
        }
        if (automation <= automationLowThreshold) {
            html.append("<li>")
                .append(String.format(getStringProperty("audit.warning.automation.text"), automation));
        }
        html.append("</ul></html>");

        JPanel warning = new JPanel(new BorderLayout(12, 0));
        warning.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, UIColors.LINK),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));
        warning.setBackground(UIColors.PANEL_BACKGROUND_ALT);

        JLabel explanation = new JLabel(html.toString());
        explanation.setFont(explanation.getFont().deriveFont(Font.PLAIN, 12f));
        explanation.setForeground(UIColors.TEXT_PRIMARY);

        JButton button = new JButton(getCtaButtonText());
        button.setFont(button.getFont().deriveFont(Font.BOLD, 12f));
        button.setToolTipText(getCtaSubtitle());
        button.addActionListener(e -> {
            recordClick("risk_warning_box");
            openAuditLandingPage(project);
        });

        warning.add(explanation, BorderLayout.CENTER);
        warning.add(button, BorderLayout.EAST);
        return warning;
    }

    /**
     * Records an audit CTA shown event. Simple Logger-based analytics to avoid new dependencies.
     */
    public static void recordShown(String source) {
        LOG.info("audit_upsell_cta_shown source=" + source + " totalShown=" + SHOWN_COUNT.incrementAndGet());
    }

    /**
     * Records an audit CTA click event. Simple Logger-based analytics to avoid new dependencies.
     */
    public static void recordClick(String source) {
        LOG.info("audit_upsell_cta_clicked source=" + source + " totalClicked=" + CLICKED_COUNT.incrementAndGet());
    }

    private static String getProjectKey(Project project) {
        if (project == null) {
            return null;
        }
        String base = project.getBasePath();
        return base != null ? base : project.getName();
    }
}
