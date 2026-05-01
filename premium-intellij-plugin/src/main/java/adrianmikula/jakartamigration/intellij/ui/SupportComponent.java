package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.analytics.service.UserIdentificationService;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.function.Consumer;

/**
 * Support tab component providing help resources
 * and upgrade information.
 */
public class SupportComponent {
    
    private static final Logger LOG = Logger.getInstance(SupportComponent.class);
    private final Project project;
    private final Consumer<Void> onPremiumActivated;
    private final Runnable onExperimentalFeaturesChanged;
    private final UserIdentificationService userIdentificationService;
    
    // UI Components
    private JPanel mainPanel;
    JCheckBox experimentalFeaturesCheckbox;
    JCheckBox usageMetricsCheckbox;
    JCheckBox errorReportingCheckbox;
    private JBTextArea outputArea;
    private JBScrollPane scrollPane;
    private JPanel compatibilityPanel;
    Properties supportUrls;
    
    // Header components
    private JBLabel titleLabel;
    private JBLabel descLabel;
    
    // Removed trial-related static state - credits system handles free tier
    
    public SupportComponent(@NotNull Project project, Consumer<Void> onPremiumActivated, Runnable onExperimentalFeaturesChanged) {
        this(project, onPremiumActivated, onExperimentalFeaturesChanged, new UserIdentificationService());
    }

    public SupportComponent(@NotNull Project project, Consumer<Void> onPremiumActivated, Runnable onExperimentalFeaturesChanged, UserIdentificationService userIdentificationService) {
        this.project = project;
        this.onPremiumActivated = onPremiumActivated;
        this.onExperimentalFeaturesChanged = onExperimentalFeaturesChanged;
        this.userIdentificationService = userIdentificationService;
        
        // Load support URLs from properties file
        loadSupportUrls();
        
        initializeUI();
    }
    
    /**
     * Loads support URLs from properties file
     */
    private void loadSupportUrls() {
        supportUrls = new Properties();
        try (InputStream input = getClass().getResourceAsStream("/support-urls.properties")) {
            supportUrls.load(input);
            LOG.info("Loaded support URLs from properties file");
        } catch (IOException e) {
            LOG.error("Failed to load support URLs: " + e.getMessage());
            // Fallback to default values
            supportUrls.setProperty("plugin.page.url", "https://plugins.jetbrains.com/plugin/30093-jakarta-migration");
            supportUrls.setProperty("plugin.page.title", "IntelliJ Plugin Page");
            supportUrls.setProperty("plugin.page.description", "Download plugin and view documentation");
        }
    }
    
    void initializeUI() {
        // Initialize UI components
        mainPanel = new JPanel(new BorderLayout());
        JBTextArea outputArea = new JBTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JBScrollPane scrollPane = new JBScrollPane(outputArea);
        
        // Create title and description
        titleLabel = new JBLabel("Jakarta EE Migration Support");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 13));
        titleLabel.setForeground(new Color(0, 100, 180));
        
        descLabel = new JBLabel("<html><body style='width: 600px'>" +
                "Get help with Jakarta EE migration, upgrade to premium, " +
                "and access support resources for seamless migration " +
                "from Java EE to Jakarta EE." +
                "</body></html>");
        descLabel.setFont(descLabel.getFont().deriveFont(Font.PLAIN, 11));
        descLabel.setForeground(Color.GRAY);
        
        // Create support links panel
        JPanel linksPanel = new JPanel();
        linksPanel.setLayout(new BoxLayout(linksPanel, BoxLayout.Y_AXIS));
        linksPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Jakarta EE Documentation link
        JPanel jakartaPanel = createLinkPanel(
            supportUrls.getProperty("jakarta.ee.documentation.title", "Jakarta EE Documentation"),
            supportUrls.getProperty("jakarta.ee.documentation.description", "Official Jakarta EE specifications and documentation"),
            supportUrls.getProperty("jakarta.ee.documentation.url", "https://jakarta.ee/learn/documentation/")
        );
        
        // Migration Guide link  
        JPanel migrationPanel = createLinkPanel(
            supportUrls.getProperty("jakarta.ee.migration.guide.title", "Migration Guide"),
            supportUrls.getProperty("jakarta.ee.migration.guide.description", "Step-by-step migration guide from Java EE to Jakarta EE"), 
            supportUrls.getProperty("jakarta.ee.migration.guide.url", "https://jakarta.ee/learn/migration/")
        );
        
        // Compatibility Guide link
        JPanel compatibilityPanel = createLinkPanel(
            supportUrls.getProperty("compatibility.guide.title", "Compatibility Guide"),
            supportUrls.getProperty("compatibility.guide.description", "Jakarta EE compatibility matrix and version guidelines"), 
            supportUrls.getProperty("compatibility.guide.url", "https://jakarta.ee/learn/compatibility/")
        );
        
        // GitHub Repository link
        JPanel githubPanel = createLinkPanel(
            supportUrls.getProperty("github.repository.title", "GitHub Repository"),
            supportUrls.getProperty("github.repository.description", "Source code, issues, and community discussions"),
            supportUrls.getProperty("github.repository.url", "https://github.com/adrianmikula/jakarta-migration")
        );
        
        // Plugin Page link
        JPanel pluginPanel = createLinkPanel(
            supportUrls.getProperty("plugin.page.title", "IntelliJ Plugin Page"),
            supportUrls.getProperty("plugin.page.description", "Download plugin and view documentation"),
            supportUrls.getProperty("plugin.page.url", "https://plugins.jetbrains.com/plugin/30093-jakarta-migration")
        );
        
        // LinkedIn Profile link
        JPanel linkedinPanel = createLinkPanel(
            supportUrls.getProperty("linkedin.profile.title", "LinkedIn Profile"),
            supportUrls.getProperty("linkedin.profile.description", "Connect with the plugin author"),
            supportUrls.getProperty("linkedin.profile.url", "https://www.linkedin.com/in/adrianmikula/")
        );
        
        // Sponsor link
        JPanel sponsorPanel = createLinkPanel(
            supportUrls.getProperty("sponsor.project.title", "Sponsor This Project"),
            supportUrls.getProperty("sponsor.project.description", "Support development of Jakarta EE migration tools"),
            supportUrls.getProperty("sponsor.project.url", "https://github.com/sponsors/adrianmikula")
        );
        
        linksPanel.add(jakartaPanel);
        linksPanel.add(migrationPanel);
        linksPanel.add(compatibilityPanel);
        linksPanel.add(githubPanel);
        linksPanel.add(pluginPanel);
        linksPanel.add(linkedinPanel);
        linksPanel.add(sponsorPanel);
        
        // Create buttons and experimental features panel
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        
        // Experimental features checkbox
        JPanel experimentalPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        experimentalFeaturesCheckbox = new JCheckBox("Enable Experimental Features");
        experimentalFeaturesCheckbox.setToolTipText("Enable cutting-edge features under development");
        
        // Initialize checkbox state based on current feature flag
        boolean currentState = adrianmikula.jakartamigration.intellij.config.FeatureFlags.getInstance().isExperimentalFeaturesEnabled();
        experimentalFeaturesCheckbox.setSelected(currentState);
        System.out.println("DEBUG: Experimental features checkbox initialized to: " + currentState);
        
        experimentalFeaturesCheckbox.addActionListener(e -> {
            boolean enabled = experimentalFeaturesCheckbox.isSelected();
            System.out.println("DEBUG: Experimental features checkbox changed to: " + enabled);
            // Update the feature flag
            adrianmikula.jakartamigration.intellij.config.FeatureFlags.getInstance().setExperimentalFeaturesEnabled(enabled);
            // Refresh the UI to show/hide experimental tabs
            if (onPremiumActivated != null) {
                onPremiumActivated.accept(null);
            }
            // Additionally, refresh experimental tabs specifically
            if (onExperimentalFeaturesChanged != null) {
                onExperimentalFeaturesChanged.run();
                System.out.println("DEBUG: Experimental tabs refreshed after checkbox change");
            }
        });
        experimentalPanel.add(experimentalFeaturesCheckbox);
        
        // Usage metrics checkbox
        JPanel usageMetricsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        usageMetricsCheckbox = new JCheckBox("Enable Usage Metrics");
        usageMetricsCheckbox.setToolTipText("Allow collection of anonymous usage statistics to improve the plugin");
        
        // Initialize checkbox state based on user preference (default to enabled)
        boolean usageMetricsEnabled = !userIdentificationService.isUsageMetricsOptedOut();
        usageMetricsCheckbox.setSelected(usageMetricsEnabled);
        System.out.println("DEBUG: Usage metrics checkbox initialized to: " + usageMetricsEnabled);
        
        usageMetricsCheckbox.addActionListener(e -> {
            boolean enabled = usageMetricsCheckbox.isSelected();
            System.out.println("DEBUG: Usage metrics checkbox changed to: " + enabled);
            // Update the user preference
            userIdentificationService.setUsageMetricsEnabled(enabled);
        });
        usageMetricsPanel.add(usageMetricsCheckbox);
        
        // Error reporting checkbox
        JPanel errorReportingPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        errorReportingCheckbox = new JCheckBox("Enable Error Reporting");
        errorReportingCheckbox.setToolTipText("Allow automatic error reporting to help fix bugs and improve stability");
        
        // Initialize checkbox state based on user preference (default to enabled)
        boolean errorReportingEnabled = !userIdentificationService.isErrorReportingOptedOut();
        errorReportingCheckbox.setSelected(errorReportingEnabled);
        System.out.println("DEBUG: Error reporting checkbox initialized to: " + errorReportingEnabled);
        
        errorReportingCheckbox.addActionListener(e -> {
            boolean enabled = errorReportingCheckbox.isSelected();
            System.out.println("DEBUG: Error reporting checkbox changed to: " + enabled);
            // Update the user preference
            userIdentificationService.setErrorReportingEnabled(enabled);
        });
        errorReportingPanel.add(errorReportingCheckbox);
        
        // Buttons panel - now includes experimental features, usage metrics, and error reporting
        controlPanel.add(experimentalPanel);
        controlPanel.add(usageMetricsPanel);
        controlPanel.add(errorReportingPanel);
        
        // Layout components
        mainPanel.add(linksPanel, BorderLayout.CENTER);
        mainPanel.add(controlPanel, BorderLayout.SOUTH);
        
        // Update UI based on current license status
        refreshUI();
    }

    /**
     * Creates a clickable link panel with title and description
     */
    private JPanel createLinkPanel(String title, String description, String url) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        panel.setBackground(new Color(245, 245, 250));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        // Add title and description
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 12));
        titleLabel.setForeground(new Color(0, 100, 180));
        
        JLabel descLabel = new JLabel("<html><body style='width: 500px'>" + description + "</body></html>");
        descLabel.setFont(descLabel.getFont().deriveFont(Font.PLAIN, 11));
        descLabel.setForeground(Color.GRAY);
        
        textPanel.add(titleLabel);
        textPanel.add(descLabel);
        
        // Create arrow icon
        JLabel arrowLabel = new JLabel("→");
        arrowLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        arrowLabel.setForeground(new Color(0, 100, 180));
        
        panel.add(textPanel, BorderLayout.CENTER);
        panel.add(arrowLabel, BorderLayout.EAST);
        
        // Add mouse listener for click handling
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openUrl(url);
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                panel.setBackground(new Color(240, 245, 255));
                panel.repaint();
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                panel.setBackground(new Color(245, 245, 250));
                panel.repaint();
            }
        });
        
        return panel;
    }
    
    /**
     * Opens a URL in the default browser
     */
    private void openUrl(String url) {
        try {
            Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception ex) {
            LOG.error("Failed to open URL: " + url, ex);
            Messages.showErrorDialog(project, "Failed to open URL: " + url, "Error");
        }
    }
    
    /**
     * Refreshes UI based on current license status
     */
    public void refreshUI() {
        // No trial controls to update - credits system handles free tier
    }

    /**
     * Gets the main panel for this component
     */
    public JPanel getPanel() {
        return mainPanel;
    }
    
    /**
     * Gets the plugin version
     */
    private String getPluginVersion() {
        try {
            // Simplified version retrieval without PluginId
            return "1.0.8"; // Return known version
        } catch (Exception e) {
            LOG.error("Failed to get plugin version", e);
        }
        return "Unknown";
    }
}
