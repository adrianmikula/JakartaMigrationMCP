package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.license.CheckLicense;
import adrianmikula.jakartamigration.analytics.service.ErrorReportingService;
import adrianmikula.jakartamigration.analytics.service.UserIdentificationService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Pair;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Development settings tab component.
 * Only visible in development mode and provides settings for testing premium features.
 */
public class DevTabComponent {
    private static final Logger LOG = Logger.getInstance(DevTabComponent.class);
    
    private final Project project;
    private final JPanel mainPanel;
    private final JBCheckBox simulatePremiumCheckBox;
    private final Consumer<Boolean> onPremiumSimulationChanged;
    private final ErrorReportingService errorReportingService;
    
    // Settings persistence
    private static final String SIMULATE_PREMIUM_KEY = "jakarta.migration.dev.simulate_premium";
    
    public DevTabComponent(Project project, Consumer<Boolean> onPremiumSimulationChanged) {
        this.project = project;
        this.onPremiumSimulationChanged = onPremiumSimulationChanged;
        this.errorReportingService = new ErrorReportingService(new UserIdentificationService());
        
        // Initialize UI components
        simulatePremiumCheckBox = new JBCheckBox("Simulate Premium License");
        simulatePremiumCheckBox.setToolTipText("When enabled, simulates having a premium license for testing purposes");
        
        // Load current setting
        boolean currentSetting = getCurrentSimulationSetting();
        simulatePremiumCheckBox.setSelected(currentSetting);
        
        // Add change listener
        simulatePremiumCheckBox.addActionListener(e -> {
            boolean newValue = simulatePremiumCheckBox.isSelected();
            setSimulationSetting(newValue);
            LOG.info("DevTabComponent: Premium simulation changed to: " + newValue);
            
            // Notify parent to refresh UI
            if (onPremiumSimulationChanged != null) {
                onPremiumSimulationChanged.accept(newValue);
            }
        });
        
        // Create the main panel
        mainPanel = createMainPanel();
        
        LOG.info("DevTabComponent: Initialized with premium simulation: " + currentSetting);
    }
    
    private JPanel createMainPanel() {
        // Create header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(JBUI.Borders.empty(10, 10, 5, 10));
        
        JLabel titleLabel = new JLabel("Development Settings");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        headerPanel.add(titleLabel, BorderLayout.NORTH);
        
        JLabel subtitleLabel = new JBLabel("Settings for development and testing (only visible in dev mode)");
        subtitleLabel.setForeground(Color.GRAY);
        headerPanel.add(subtitleLabel, BorderLayout.CENTER);
        
        // Create settings form
        JPanel settingsPanel = FormBuilder.createFormBuilder()
                .addComponent(simulatePremiumCheckBox, 1)
                .addComponent(createPremiumSimulationInfoPanel(), 1)
                .addComponent(createAdditionalSettingsPanel(), 1)
                .getPanel();
        settingsPanel.setBorder(JBUI.Borders.empty(10));
        
        // Combine header and settings
        JPanel combinedPanel = new JPanel(new BorderLayout());
        combinedPanel.add(headerPanel, BorderLayout.NORTH);
        combinedPanel.add(settingsPanel, BorderLayout.CENTER);
        
        // Add warning panel at bottom
        combinedPanel.add(createWarningPanel(), BorderLayout.SOUTH);
        
        return combinedPanel;
    }
    
    private JPanel createPremiumSimulationInfoPanel() {
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBorder(JBUI.Borders.empty(5, 20, 10, 0));
        
        JLabel infoLabel = new JBLabel("<html>" +
                "<i>When enabled, this setting overrides all license checks to simulate a premium license.<br>" +
                "This affects all premium features including tabs, toolbar badges, and credit limits.<br>" +
                "The simulation only applies in development mode and is reset on plugin restart.</i>" +
                "</html>");
        infoLabel.setForeground(Color.GRAY);
        infoPanel.add(infoLabel, BorderLayout.CENTER);
        
        return infoPanel;
    }
    
    private JPanel createAdditionalSettingsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(JBUI.Borders.empty(10, 20, 10, 0));
        
        // Add test error reporting button
        JButton testErrorButton = new JButton("Test Error Reporting");
        testErrorButton.setToolTipText("Click to test error reporting by simulating an exception");
        testErrorButton.addActionListener(e -> testErrorReporting());
        panel.add(testErrorButton);
        panel.add(Box.createVerticalStrut(10));
        
        JLabel futureLabel = new JBLabel("Additional development settings coming soon...");
        futureLabel.setForeground(Color.GRAY);
        panel.add(futureLabel);
        
        return panel;
    }
    
    private JPanel createWarningPanel() {
        JPanel warningPanel = new JPanel(new BorderLayout());
        warningPanel.setBorder(JBUI.Borders.empty(10));
        warningPanel.setBackground(new Color(255, 248, 220)); // Light yellow background
        
        JLabel warningLabel = new JBLabel("<html>" +
                "<b>⚠ Development Mode Only</b><br>" +
                "These settings are only available in development mode and will not affect production or demo builds.<br>" +
                "Premium simulation is reset each time the plugin starts for safety." +
                "</html>");
        warningLabel.setForeground(new Color(139, 69, 19)); // Brown color
        warningPanel.add(warningLabel, BorderLayout.CENTER);
        warningPanel.setBorder(JBUI.Borders.compound(
                JBUI.Borders.empty(10),
                JBUI.Borders.customLine(warningLabel.getForeground(), 1)
        ));
        
        return warningPanel;
    }
    
    /**
     * Gets the current premium simulation setting from system properties.
     */
    private boolean getCurrentSimulationSetting() {
        return Boolean.getBoolean(SIMULATE_PREMIUM_KEY);
    }
    
    /**
     * Sets the premium simulation setting in system properties.
     */
    private void setSimulationSetting(boolean enabled) {
        System.setProperty(SIMULATE_PREMIUM_KEY, String.valueOf(enabled));
        LOG.info("DevTabComponent: Set premium simulation to: " + enabled);
    }
    
    /**
     * Gets the main panel for this component.
     */
    public JPanel getPanel() {
        return mainPanel;
    }
    
    /**
     * Checks if premium simulation is currently enabled.
     */
    public boolean isPremiumSimulationEnabled() {
        return simulatePremiumCheckBox.isSelected();
    }
    
    /**
     * Programmatically sets the premium simulation state.
     */
    public void setPremiumSimulationEnabled(boolean enabled) {
        simulatePremiumCheckBox.setSelected(enabled);
        setSimulationSetting(enabled);
    }
    
    /**
     * Resets all development settings to defaults.
     */
    public void resetToDefaults() {
        simulatePremiumCheckBox.setSelected(false);
        System.clearProperty(SIMULATE_PREMIUM_KEY);
        LOG.info("DevTabComponent: Reset all settings to defaults");
    }
    
    /**
     * Gets current settings as a string for debugging.
     */
    public String getSettingsSummary() {
        return String.format("Premium Simulation: %s", isPremiumSimulationEnabled());
    }
    
    /**
     * Tests error reporting by simulating an exception and reporting it to Supabase.
     */
    private void testErrorReporting() {
        // Show confirmation dialog
        int result = Messages.showOkCancelDialog(
            project,
            "Are you sure you want to test error reporting? This will simulate an exception and send a test error report to Supabase.",
            "Test Error Reporting",
            "Test Error",
            "Cancel",
            Messages.getQuestionIcon()
        );
        
        if (result != Messages.OK) {
            return;
        }
        
        try {
            // Simulate an exception
            throw new RuntimeException("Simulated exception for testing error reporting");
        } catch (RuntimeException e) {
            // Report the exception through the normal error reporting pipeline
            errorReportingService.reportError(e, "Dev tab test error reporting");
            
            // Show success dialog
            Messages.showInfoMessage(
                project,
                "Test error report sent successfully to Supabase for testing purposes.",
                "Error Report Sent"
            );
        }
    }
}
