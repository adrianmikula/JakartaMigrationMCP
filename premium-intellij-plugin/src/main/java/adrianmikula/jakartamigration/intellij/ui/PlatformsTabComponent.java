package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.config.FeatureFlags;
import adrianmikula.jakartamigration.platforms.model.PlatformDetection;
import adrianmikula.jakartamigration.platforms.model.PlatformScanResult;
import adrianmikula.jakartamigration.platforms.service.PlatformDetectionService;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * UI component for the Platforms tab (premium feature)
 */
public class PlatformsTabComponent {
    private final Project project;
    private final PlatformDetectionService detectionService;
    
    // Main UI components
    private JPanel mainPanel;
    private JButton scanButton;
    private JPanel resultsPanel;
    private JScrollPane resultsScrollPane;
    
    // Premium controls
    private JPanel premiumPanel;
    private JLabel lockIcon;
    private JButton upgradeButton;
    private JButton trialButton;
    
    // Results display
    private List<PlatformDetectionPanel> platformPanels;
    
    public PlatformsTabComponent(Project project) {
        this.project = project;
        this.detectionService = new PlatformDetectionService();
        this.platformPanels = new ArrayList<>();
        initializeUI();
    }
    
    /**
     * Initializes the UI components
     */
    private void initializeUI() {
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(JBUI.Borders.empty(10));
        
        // Header with scan button
        JPanel headerPanel = createHeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        
        // Results area
        resultsPanel = new JPanel();
        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
        resultsScrollPane = new JBScrollPane(resultsPanel);
        resultsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        resultsScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        mainPanel.add(resultsScrollPane, BorderLayout.CENTER);
        
        // Premium controls
        JPanel footerPanel = createPremiumPanel();
        mainPanel.add(footerPanel, BorderLayout.SOUTH);
        
        updatePremiumControls();
    }
    
    /**
     * Creates the header panel with scan button
     */
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(JBUI.Borders.emptyBottom(10));
        
        JLabel titleLabel = new JBLabel("Application Server Detection");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        scanButton = new JButton("Analyse Project");
        scanButton.addActionListener(e -> scanProject());
        headerPanel.add(scanButton, BorderLayout.EAST);
        
        return headerPanel;
    }
    
    /**
     * Creates the premium controls panel
     */
    private JPanel createPremiumPanel() {
        premiumPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        // Lock icon for non-premium users
        lockIcon = new JBLabel("🔒 Premium Feature");
        lockIcon.setFont(lockIcon.getFont().deriveFont(Font.ITALIC, 12f));
        lockIcon.setForeground(Color.GRAY);
        
        // Action buttons
        upgradeButton = new JButton("Upgrade to Premium");
        upgradeButton.addActionListener(e -> showUpgradeDialog());
        
        trialButton = new JButton("Start Free Trial");
        trialButton.addActionListener(e -> showTrialDialog());
        
        premiumPanel.add(lockIcon);
        premiumPanel.add(upgradeButton);
        premiumPanel.add(trialButton);
        
        return premiumPanel;
    }
    
    /**
     * Scans the project for application servers
     */
    public void scanProject() {
        scanButton.setEnabled(false);
        scanButton.setText("Scanning...");
        
        // Run scan in background thread
        SwingWorker<PlatformScanResult, Void> worker = new SwingWorker<>() {
            @Override
            protected PlatformScanResult doInBackground() throws Exception {
                return detectionService.scanProject(java.nio.file.Paths.get(project.getBasePath()));
            }
            
            @Override
            protected void done() {
                try {
                    PlatformScanResult result = get();
                    displayResults(result.detectedPlatforms());
                    scanButton.setEnabled(true);
                    scanButton.setText("Analyse Project");
                } catch (Exception e) {
                    displayError("Failed to scan project: " + e.getMessage());
                    scanButton.setEnabled(true);
                    scanButton.setText("Analyse Project");
                }
            }
        };
        
        worker.execute();
    }
    
    /**
     * Displays scan results in the UI
     */
    public void displayResults(List<PlatformDetection> detections) {
        // Clear previous results
        resultsPanel.removeAll();
        platformPanels.clear();
        
        if (detections.isEmpty()) {
            JLabel noResultsLabel = new JBLabel("No application servers detected in this project.");
            noResultsLabel.setBorder(JBUI.Borders.empty(10));
            resultsPanel.add(noResultsLabel);
        } else {
            for (PlatformDetection detection : detections) {
                PlatformDetectionPanel panel = new PlatformDetectionPanel(detection);
                platformPanels.add(panel);
                resultsPanel.add(panel);
                resultsPanel.add(Box.createVerticalStrut(10));
            }
        }
        
        resultsPanel.revalidate();
        resultsPanel.repaint();
    }
    
    /**
     * Updates premium controls based on feature flag
     */
    public void updatePremiumControls() {
        boolean isPremium = FeatureFlags.getInstance().isPlatformsEnabled();
        
        lockIcon.setVisible(!isPremium);
        upgradeButton.setVisible(!isPremium);
        trialButton.setVisible(!isPremium);
        
        if (!isPremium) {
            // Disable scan functionality for non-premium users
            scanButton.setEnabled(false);
            scanButton.setToolTipText("Upgrade to Premium to enable platform scanning");
        } else {
            scanButton.setEnabled(true);
            scanButton.setToolTipText(null);
        }
    }
    
    /**
     * Shows upgrade dialog
     */
    private void showUpgradeDialog() {
        JOptionPane.showMessageDialog(mainPanel,
            "Upgrade to Premium to unlock Application Server Detection and other advanced features.",
            "Upgrade to Premium",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Shows trial dialog
     */
    private void showTrialDialog() {
        JOptionPane.showMessageDialog(mainPanel,
            "Start a free trial to test all premium features for 14 days.",
            "Free Trial",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Displays error message
     */
    private void displayError(String message) {
        JOptionPane.showMessageDialog(mainPanel,
            message,
            "Scan Error",
            JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * Gets the main panel for this component
     */
    public JPanel getPanel() {
        return mainPanel;
    }
    
    /**
     * Panel component for displaying a single platform detection
     */
    private static class PlatformDetectionPanel extends JPanel {
        public PlatformDetectionPanel(PlatformDetection detection) {
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                JBUI.Borders.empty(10)
            ));
            
            // Platform name and version
            JPanel headerPanel = new JPanel(new BorderLayout());
            JLabel nameLabel = new JLabel(detection.platformName());
            nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 13f));
            headerPanel.add(nameLabel, BorderLayout.WEST);
            
            JLabel versionLabel = new JLabel("Version: " + detection.detectedVersion());
            versionLabel.setFont(versionLabel.getFont().deriveFont(Font.ITALIC, 11f));
            headerPanel.add(versionLabel, BorderLayout.EAST);
            
            add(headerPanel, BorderLayout.NORTH);
            
            // Compatibility status
            JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            
            JLabel compatibilityLabel;
            if (detection.isJakartaCompatible()) {
                compatibilityLabel = new JLabel("✅ Jakarta EE Compatible");
                compatibilityLabel.setForeground(new Color(40, 167, 69)); // Green
            } else {
                compatibilityLabel = new JLabel("⚠️ Requires Jakarta EE Upgrade");
                compatibilityLabel.setForeground(new Color(255, 152, 0)); // Orange
            }
            
            statusPanel.add(compatibilityLabel);
            add(statusPanel, BorderLayout.CENTER);
            
            // Requirements
            if (!detection.requirements().isEmpty()) {
                JPanel requirementsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                requirementsPanel.setBorder(JBUI.Borders.emptyTop(10));
                
                JLabel requirementsLabel = new JLabel("Requirements: ");
                requirementsPanel.add(requirementsLabel);
                
                for (Map.Entry<String, String> requirement : detection.requirements().entrySet()) {
                    JLabel reqLabel = new JLabel(String.format("%s: %s, ", 
                        requirement.getKey(), requirement.getValue()));
                    requirementsPanel.add(reqLabel);
                }
                
                add(requirementsPanel, BorderLayout.SOUTH);
            }
        }
    }
}
