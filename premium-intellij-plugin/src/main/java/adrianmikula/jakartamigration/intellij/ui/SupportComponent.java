package adrianmikula.jakartamigration.intellij.ui;

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
 * Support tab component providing premium features,
 * trial activation, and help resources.
 */
public class SupportComponent {
    
    private static final Logger LOG = Logger.getInstance(SupportComponent.class);
    private final Project project;
    private final Consumer<Void> onPremiumActivated;
    private final Runnable onExperimentalFeaturesChanged;
    
    // UI Components
    private JPanel mainPanel;
    private JButton startTrialButton;
    JCheckBox experimentalFeaturesCheckbox;
    private JBTextArea outputArea;
    private JBScrollPane scrollPane;
    private JPanel compatibilityPanel;
    Properties supportUrls;
    
    // Header components
    private JBLabel titleLabel;
    private JBLabel descLabel;
    
    // Static state for premium status
    private static boolean isPremiumActive = false;
    private static String licenseStatus = "Unknown";
    
    public SupportComponent(@NotNull Project project, Consumer<Void> onPremiumActivated, Runnable onExperimentalFeaturesChanged) {
        this.project = project;
        this.onPremiumActivated = onPremiumActivated;
        this.onExperimentalFeaturesChanged = onExperimentalFeaturesChanged;
        
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
                "Get help with Jakarta EE migration, start premium trial, " +
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
        
        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        startTrialButton = new JButton("Start Free Trial");
        startTrialButton.setToolTipText("Activate 7-day premium trial");
        
        buttonPanel.add(startTrialButton);
        
        controlPanel.add(experimentalPanel);
        controlPanel.add(buttonPanel);
        
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
     * Starts the premium trial
     */
    private void startTrial() {
        int result = Messages.showYesNoDialog(
                project,
                "Start a 7-day free trial of Premium features?",
                "Start Free Trial",
                Messages.getQuestionIcon());
        
        if (result == Messages.YES) {
            LOG.info("User accepted free trial");
            
            // Set system properties for trial
            System.setProperty("jakarta.migration.premium", "true");
            System.setProperty("jakarta.migration.trial.end", 
                    String.valueOf(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000));
            
            // Clear license cache to force fresh check
            adrianmikula.jakartamigration.intellij.license.CheckLicense.clearCache();
            
            // Enable premium features
            adrianmikula.jakartamigration.intellij.config.FeatureFlags.getInstance().setPlatformsEnabled(true);
            adrianmikula.jakartamigration.intellij.config.FeatureFlags.getInstance().setAdvancedScansEnabled(true);
            
            // Update static state
            setPremiumActive(true);
            setLicenseStatus("Trial Active (7 days remaining)");
            
            // Show success message
            Messages.showInfoMessage(project, 
                    "Trial started! You now have 7 days of Premium access.", 
                    "Trial Activated");
            
            LOG.info("Trial started - premium=true, trial.end=" + 
                    System.getProperty("jakarta.migration.trial.end"));
            
            // Refresh premium tabs to enable buttons
            if (onPremiumActivated != null) {
                onPremiumActivated.accept(null);
            }
        }
    }
    
    /**
     * Refreshes UI based on current license status
     */
    public void refreshUI() {
        updatePremiumControls();
    }
    
    /**
     * Updates premium controls based on current license status
     */
    private void updatePremiumControls() {
        if (startTrialButton != null) {
            if (isPremiumActive) {
                startTrialButton.setText("Trial Active");
                startTrialButton.setEnabled(false);
                startTrialButton.setToolTipText("Premium trial is currently active");
            } else {
                startTrialButton.setText("Start Free Trial");
                startTrialButton.setEnabled(true);
                startTrialButton.setToolTipText("Activate 7-day premium trial");
            }
        }
        
        // Update status display
        String statusText = "License Status: " + licenseStatus;
        if (outputArea != null) {
            outputArea.setText(statusText);
        }
    }
    
    /**
     * Static method to set premium active status
     */
    public static void setPremiumActive(boolean premium) {
        isPremiumActive = premium;
        LOG.info("Premium status set to: " + premium);
    }

    /**
     * Static method to get premium active status
     */
    public static boolean isPremiumActive() {
        return isPremiumActive;
    }
    
    /**
     * Static method to set license status
     */
    public static void setLicenseStatus(String status) {
        licenseStatus = status;
        LOG.info("License status set to: " + status);
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
