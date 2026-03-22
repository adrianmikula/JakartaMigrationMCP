package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.license.CheckLicense;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Support tab component for the IntelliJ plugin.
 * Provides links to GitHub, LinkedIn, and sponsor pages.
 * URLs are loaded from support-urls.properties to avoid hardcoding.
 */
public class SupportComponent {
    private static final Logger LOG = Logger.getInstance(SupportComponent.class);
    
    private static boolean premiumActive = false;
    private static String licenseStatus = "Free";

    private static final java.util.Properties URLS = loadUrls();

    private static java.util.Properties loadUrls() {
        java.util.Properties props = new java.util.Properties();
        // Defaults in case properties file is missing
        props.setProperty("github", "https://github.com/adrianmikula/JakartaMigrationMCP");
        props.setProperty("github.issues", "https://github.com/adrianmikula/JakartaMigrationMCP/issues");
        props.setProperty("github.sponsor", "https://github.com/sponsors/adrianmikula");
        props.setProperty("linkedin", "https://linkedin.com/in/adrianmikula");
        props.setProperty("plugin.page", "https://plugins.jetbrains.com/plugin/30093-jakarta-migration-javax--jakarta-");
        props.setProperty("plugin.updates", "https://plugins.jetbrains.com/plugin/30093-jakarta-migration-javax--jakarta-/versions");
        props.setProperty("marketplace", "https://plugins.jetbrains.com/plugin/30093-jakarta-migration-javax--jakarta-");
        try (java.io.InputStream is = SupportComponent.class.getClassLoader().getResourceAsStream("support-urls.properties")) {
            if (is != null) props.load(is);
        } catch (Exception e) {
            Logger.getInstance(SupportComponent.class).warn("Could not load support-urls.properties, using defaults", e);
        }
        return props;
    }

    private static final String GITHUB_URL = URLS.getProperty("github");
    private static final String GITHUB_ISSUES_URL = URLS.getProperty("github.issues");
    private static final String GITHUB_SPONSOR_URL = URLS.getProperty("github.sponsor");
    private static final String LINKEDIN_URL = URLS.getProperty("linkedin");
    private static final String PLUGIN_PAGE_URL = URLS.getProperty("plugin.page");
    private static final String UPDATE_URL = URLS.getProperty("plugin.updates");
    private static final String MARKETPLACE_URL = URLS.getProperty("marketplace");

    private final JPanel panel;
    private final Project project;
    private final boolean isPremium;
    private final Runnable refreshCallback;

    public SupportComponent(Project project, boolean isPremium, Runnable refreshCallback) {
        this.project = project;
        this.isPremium = isPremium;
        this.refreshCallback = refreshCallback;
        this.panel = createPanel();
    }

    public static void setPremiumActive(boolean active) {
        premiumActive = active;
    }

    public static void setLicenseStatus(String status) {
        licenseStatus = status;
    }

    public static boolean isPremiumActive() {
        return premiumActive || "true".equals(System.getProperty("jakarta.migration.premium"));
    }

    public JPanel getPanel() {
        return panel;
    }

    private JPanel createPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(20, 20));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Header
        JLabel headerLabel = new JLabel("Support & Resources");
        headerLabel.setFont(new Font(headerLabel.getFont().getName(), Font.BOLD, 18));

        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.add(headerLabel);

        // Content panel
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        // Premium upgrade banner (always visible at top)
        contentPanel.add(createPremiumUpgradeBanner());
        contentPanel.add(Box.createVerticalStrut(20));

        // Experimental Features section
        contentPanel.add(createExperimentalFeaturesSection());
        contentPanel.add(Box.createVerticalStrut(20));

        // About section
        contentPanel.add(createSectionHeader("About"));
        JTextArea aboutText = new JTextArea(
                "Jakarta Migration Plugin helps you migrate from javax to jakarta namespaces.\n" +
                        "This plugin supports various scanning types including JPA, Bean Validation,\n" +
                        "Servlet/JSP, CDI, and more.");
        aboutText.setEditable(false);
        aboutText.setBackground(null);
        aboutText.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        contentPanel.add(aboutText);
        contentPanel.add(Box.createVerticalStrut(20));

        // Links section
        contentPanel.add(createSectionHeader("Links"));

        // GitHub link
        contentPanel.add(createLinkButton(
                "GitHub Repository",
                "View source code, report issues, and contribute",
                GITHUB_URL));

        // GitHub Issues link
        contentPanel.add(createLinkButton(
                "Report Issues",
                "Open a GitHub issue for bugs or feature requests",
                GITHUB_ISSUES_URL));

        // Sponsor link
        contentPanel.add(createLinkButton(
                "❤️ Sponsor on GitHub",
                "Support the development of this plugin",
                GITHUB_SPONSOR_URL));

        // LinkedIn link
        contentPanel.add(createLinkButton(
                "LinkedIn",
                "Connect with the developer on LinkedIn",
                LINKEDIN_URL));

        // Plugin page link
        contentPanel.add(createLinkButton(
                "JetBrains Plugin Page",
                "Rate and review the plugin",
                PLUGIN_PAGE_URL));

        // Check for updates link
        contentPanel.add(createLinkButton(
                "Check for Updates",
                "View the latest versions and release notes",
                UPDATE_URL));

        contentPanel.add(Box.createVerticalStrut(20));

        // Version info
        contentPanel.add(createSectionHeader("Version Info"));
        JLabel versionLabel = new JLabel("Version: " + getPluginVersion() + " (" + CheckLicense.getLicenseStatusString() + ")");
        versionLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        versionLabel.setForeground(Color.GRAY);
        contentPanel.add(versionLabel);

        JLabel buildLabel = new JLabel("Build: " + getBuildTimestamp());
        buildLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        buildLabel.setForeground(Color.GRAY);
        contentPanel.add(buildLabel);

        // Wrap in scroll pane
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        return mainPanel;
    }

    private JLabel createSectionHeader(String title) {
        JLabel label = new JLabel(title);
        label.setFont(new Font(label.getFont().getName(), Font.BOLD, 14));
        label.setBorder(BorderFactory.createEmptyBorder(10, 0, 5, 0));
        return label;
    }

    private JPanel createPremiumUpgradeBanner() {
        String licenseStatus = CheckLicense.getLicenseStatusString();
        boolean alreadyPremium = CheckLicense.isLicensed();
        boolean isTrial = licenseStatus.startsWith("Trial");

        JPanel bannerPanel = new JPanel(new BorderLayout(15, 10));
        bannerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBackground(new Color(255, 253, 208));

        JLabel starLabel = new JLabel(alreadyPremium ? "✅" : "⭐");
        starLabel.setFont(new Font(starLabel.getFont().getName(), Font.PLAIN, 32));
        starLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(starLabel);

        JLabel titleLabel = new JLabel(alreadyPremium ? "Premium Active" : 
                (isTrial ? "Upgrade to Full Premium" : "Upgrade to Premium"));
        titleLabel.setFont(new Font(titleLabel.getFont().getName(), Font.BOLD, 16));
        titleLabel.setForeground(new Color(80, 60, 0));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(titleLabel);

        JLabel descLabel = new JLabel(alreadyPremium ? 
                "<html>Thank you for using Premium! Enjoy auto-fixes and one-click refactoring.</html>" :
                (isTrial ? 
                    "<html>Your trial is active! Upgrade to Full Premium to continue enjoying all features after your trial expires.</html>" :
                    "<html>Get auto-fixes, one-click refactoring, and binary fixes</html>"));
        descLabel.setFont(new Font(descLabel.getFont().getName(), Font.PLAIN, 12));
        descLabel.setForeground(new Color(100, 90, 60));
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(descLabel);

        // Right side - buttons (only show if not premium)
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setBackground(new Color(255, 253, 208));
        buttonPanel.setAlignmentY(Component.CENTER_ALIGNMENT);

        if (!alreadyPremium) {
            JButton upgradeButton = new JButton("⬆ Upgrade to Premium");
            upgradeButton.setFont(new Font(upgradeButton.getFont().getName(), Font.BOLD, 13));
            upgradeButton.setBackground(new Color(255, 215, 0));
            upgradeButton.setForeground(new Color(80, 60, 0));
            upgradeButton.setFocusPainted(false);
            upgradeButton.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
            upgradeButton.setAlignmentX(Component.RIGHT_ALIGNMENT);
            upgradeButton.addActionListener(e -> openUrl(MARKETPLACE_URL));
            buttonPanel.add(upgradeButton);

            buttonPanel.add(Box.createVerticalStrut(8));

            JButton trialButton = new JButton("Start Free Trial");
            trialButton.setFont(new Font(trialButton.getFont().getName(), Font.PLAIN, 12));
            trialButton.setForeground(new Color(100, 90, 60));
            trialButton.setFocusPainted(false);
            trialButton.setContentAreaFilled(false);
            trialButton.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            trialButton.setAlignmentX(Component.RIGHT_ALIGNMENT);
            trialButton.addActionListener(e -> startTrial());
            buttonPanel.add(trialButton);
        }

        bannerPanel.add(leftPanel, BorderLayout.CENTER);
        bannerPanel.add(buttonPanel, BorderLayout.EAST);

        return bannerPanel;
    }

    private JPanel createExperimentalFeaturesSection() {
        JPanel sectionPanel = new JPanel(new BorderLayout(10, 10));
        sectionPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        sectionPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBackground(null);

        JLabel titleLabel = new JLabel("Experimental Features");
        titleLabel.setFont(new Font(titleLabel.getFont().getName(), Font.BOLD, 13));
        titleLabel.setForeground(new Color(80, 60, 0));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(titleLabel);

        JLabel descLabel = new JLabel("Enable beta features like Runtime Error Diagnosis");
        descLabel.setFont(new Font(descLabel.getFont().getName(), Font.PLAIN, 11));
        descLabel.setForeground(Color.GRAY);
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(descLabel);

        JCheckBox betaCheckbox = new JCheckBox("Enable Experimental Features");
        betaCheckbox.setFont(betaCheckbox.getFont().deriveFont(Font.PLAIN, 12));
        betaCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);

        boolean betaEnabled = "true".equals(System.getProperty("jakarta.migration.beta_features", "false"));
        betaCheckbox.setSelected(betaEnabled);

        betaCheckbox.addActionListener(e -> {
            boolean selected = betaCheckbox.isSelected();
            System.setProperty("jakarta.migration.beta_features", String.valueOf(selected));
            adrianmikula.jakartamigration.intellij.config.FeatureFlags.getInstance().setBetaFeaturesEnabled(selected);

            LOG.info("SupportComponent: Beta features " + (selected ? "enabled" : "disabled"));

            Messages.showInfoMessage(project,
                    selected
                            ? "Experimental features enabled!\n\nThe Runtime tab will now be available."
                            : "Experimental features disabled.\n\nRestart the tool window to see changes.",
                    "Experimental Features");
        });

        leftPanel.add(Box.createVerticalStrut(5));
        leftPanel.add(betaCheckbox);

        sectionPanel.add(leftPanel, BorderLayout.CENTER);

        return sectionPanel;
    }

    private void startTrial() {
        LOG.info("SupportComponent: User clicked Start Free Trial button");

        int result = Messages.showYesNoDialog(project,
                "Start a 7-day free trial of Premium features?\n\n" +
                        "Premium features include:\n" +
                        "• Auto-fixes for migration issues\n" +
                        "• One-click refactoring\n" +
                        "• Binary fixes for JAR files\n" +
                        "• Advanced dependency analysis",
                "Start Free Trial",
                Messages.getQuestionIcon());

        if (result == Messages.YES) {
            System.setProperty("jakarta.migration.premium", "true");
            System.setProperty("jakarta.migration.trial.end",
                    String.valueOf(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000));

            // Clear license cache to force fresh check
            adrianmikula.jakartamigration.intellij.license.CheckLicense.clearCache();

            LOG.info("SupportComponent: Trial started - premium=true");

            Messages.showInfoMessage(project,
                    "Trial started! You now have 7 days of Premium access.\n\n" +
                            "Premium features are now available!",
                    "Trial Activated");

            if (refreshCallback != null) {
                refreshCallback.run();
            }
        }
    }

    private JPanel createLinkButton(String title, String description, String url) {
        JPanel linkPanel = new JPanel(new BorderLayout(10, 5));
        linkPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        linkPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        linkPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Title label
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font(titleLabel.getFont().getName(), Font.BOLD, 13));
        titleLabel.setForeground(new Color(0, 100, 180));

        // Description label
        JLabel descLabel = new JLabel(description);
        descLabel.setFont(new Font(descLabel.getFont().getName(), Font.PLAIN, 11));
        descLabel.setForeground(Color.GRAY);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.add(titleLabel);
        textPanel.add(descLabel);

        // Arrow icon
        JLabel arrowLabel = new JLabel("→");
        arrowLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        arrowLabel.setForeground(new Color(0, 100, 180));

        linkPanel.add(textPanel, BorderLayout.CENTER);
        linkPanel.add(arrowLabel, BorderLayout.EAST);

        // Add mouse listener for click handling
        linkPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openUrl(url);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                linkPanel.setBackground(new Color(240, 245, 255));
                linkPanel.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                linkPanel.setBackground(null);
                linkPanel.repaint();
            }
        });

        return linkPanel;
    }

    private void openUrl(String url) {
        try {
            // Use Desktop.browse for cross-platform URL opening
            java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
            java.net.URI uri = new java.net.URI(url);
            desktop.browse(uri);
            LOG.info("Opened URL: " + url);
        } catch (Exception e) {
            LOG.error("Failed to open URL: " + url, e);
            Messages.showErrorDialog(
                    project,
                    "Could not open URL: " + url + "\n\nYou can manually visit: " + url,
                    "Open Link Failed");
        }
    }

    private String getPluginVersion() {
        try {
            var plugin = PluginManagerCore.getPlugin(PluginId.getId("adrianmikula.jakartamigration"));
            if (plugin != null) {
                return plugin.getVersion();
            }
        } catch (Exception e) {
            LOG.error("Failed to get plugin version", e);
        }
        return "Unknown";
    }

    private String getBuildTimestamp() {
        // Try to get build timestamp from build-info.properties first
        try {
            java.util.Properties props = new java.util.Properties();
            try (java.io.InputStream is = SupportComponent.class.getClassLoader().getResourceAsStream("build-info.properties")) {
                if (is != null) {
                    props.load(is);
                    String timestamp = props.getProperty("build.timestamp");
                    if (timestamp != null && !timestamp.isEmpty()) {
                        return timestamp;
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("Could not get build timestamp from build-info.properties", e);
        }
        
        // Fallback: get timestamp from this class's JAR entry
        try {
            java.net.URL location = SupportComponent.class.getProtectionDomain().getCodeSource().getLocation();
            java.io.File jarFile = new java.io.File(location.getPath());
            if (jarFile.exists()) {
                // Return the file's last modified time formatted as date
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                return sdf.format(new java.util.Date(jarFile.lastModified()));
            }
        } catch (Exception e) {
            LOG.warn("Could not get build timestamp from JAR", e);
        }
        
        // Last fallback: current date
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new java.util.Date());
    }
}
