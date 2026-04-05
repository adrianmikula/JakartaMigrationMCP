package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.platforms.model.PlatformDetection;
import adrianmikula.jakartamigration.platforms.model.PlatformScanResult;
import adrianmikula.jakartamigration.platforms.model.EnhancedPlatformScanResult;
import adrianmikula.jakartamigration.platforms.service.SimplifiedPlatformDetectionService;
import adrianmikula.jakartamigration.platforms.service.EnhancedPlatformDetectionService;
import adrianmikula.jakartamigration.platforms.config.PlatformConfigLoader;
import adrianmikula.jakartamigration.platforms.config.RiskScoringConfig;
import adrianmikula.jakartamigration.intellij.util.DevModeLogger;

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
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * UI component for the Platforms tab (premium feature)
 */
public class PlatformsTabComponent {
    private static final Logger log = LoggerFactory.getLogger(PlatformsTabComponent.class);
    private final Project project;
    private final SimplifiedPlatformDetectionService detectionService;
    private final EnhancedPlatformDetectionService enhancedDetectionService;
    private final PlatformConfigLoader configLoader;
    
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
    private List<JPanel> platformPanels;
    private EnhancedPlatformScanResult currentScanResult;
    
    public PlatformsTabComponent(Project project) {
        this.project = project;
        this.configLoader = new PlatformConfigLoader();
        this.detectionService = new SimplifiedPlatformDetectionService();
        this.enhancedDetectionService = new EnhancedPlatformDetectionService(configLoader);
        this.platformPanels = new ArrayList<>();
        initializeUI();
    }
    
    /**
     * Initializes the UI components
     */
    private void initializeUI() {
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(JBUI.Borders.empty(10));
        
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(JBUI.Borders.emptyBottom(10));
        
        JLabel titleLabel = new JBLabel("Platform Detection");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        scanButton = new JButton("Analyse Project");
        scanButton.addActionListener(e -> scanProject());
        headerPanel.add(scanButton, BorderLayout.EAST);
        
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(createResultsPanel(), BorderLayout.CENTER);
        mainPanel.add(createPremiumPanel(), BorderLayout.SOUTH);
        updatePremiumControls();
    }
    
    private JComponent createResultsPanel() {
        resultsPanel = new JPanel();
        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
        resultsScrollPane = new JBScrollPane(resultsPanel);
        resultsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        resultsScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        return resultsScrollPane;
    }
    
    /**
     * Creates the premium controls panel
     */
    private JPanel createPremiumPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        lockIcon = new JBLabel("🔒 Premium Feature");
        lockIcon.setFont(lockIcon.getFont().deriveFont(Font.ITALIC, 12f));
        lockIcon.setForeground(Color.GRAY);
        panel.add(lockIcon);
        
        upgradeButton = new JButton("Upgrade to Premium");
        trialButton = new JButton("Start Free Trial");
        
        panel.add(upgradeButton);
        panel.add(trialButton);
        
        return panel;
    }
    
    /**
     * Scans the project for application servers with deployment artifact counting
     */
    public void scanProject() {
        log.debug("Starting enhanced platform scan for project: {}", project.getBasePath());
        scanButton.setEnabled(false);
        scanButton.setText("Scanning...");
        
        SwingWorker<EnhancedPlatformScanResult, Void> worker = new SwingWorker<>() {
            @Override
            protected EnhancedPlatformScanResult doInBackground() throws Exception {
                return enhancedDetectionService.scanProjectWithArtifacts(java.nio.file.Paths.get(project.getBasePath()));
            }
            
            @Override
            protected void done() {
                try {
                    EnhancedPlatformScanResult scanResult = get();
                    log.debug("Enhanced platform scan completed. Found {} platforms, WARs: {}, EARs: {}", 
                             scanResult.getDetectedPlatforms().size(), 
                             scanResult.getWarCount(), 
                             scanResult.getEarCount());
                    displayEnhancedResults(scanResult);
                    scanButton.setEnabled(true);
                    scanButton.setText("Analyse Project");
                } catch (Exception e) {
                    log.error("Platform scan failed: {}", e.getMessage(), e);
                    displayError("Failed to scan project: " + e.getMessage());
                    scanButton.setEnabled(true);
                    scanButton.setText("Analyse Project");
                }
            }
        };
        
        worker.execute();
    }
    
    /**
     * Displays scan results in the UI (unified method)
     */
    public void displayResults(List<String> detectedPlatforms) {
        displayResults(new EnhancedPlatformScanResult(detectedPlatforms, new HashMap<>(), new HashMap<>()));
    }
    
    /**
     * Displays enhanced scan results in the UI (unified method)
     */
    public void displayEnhancedResults(EnhancedPlatformScanResult scanResult) {
        displayResults(scanResult);
    }
    
    /**
     * Unified display method for both simple and enhanced results
     */
    private void displayResults(EnhancedPlatformScanResult scanResult) {
        // Store current scan result for external access
        this.currentScanResult = scanResult;
        
        clearResults();
        
        List<String> detectedPlatforms = scanResult.getDetectedPlatforms();
        if (detectedPlatforms.isEmpty()) {
            resultsPanel.add(createNoResultsLabel());
        } else {
            // Add artifact summary if available
            if (scanResult.getTotalDeploymentCount() > 0) {
                resultsPanel.add(createArtifactSummaryPanel(scanResult));
            }
            
            // Add platform panels
            detectedPlatforms.forEach(platform -> {
                JPanel panel = scanResult.getTotalDeploymentCount() > 0 ? 
                    createEnhancedPlatformPanel(platform, scanResult) : 
                    createSimplePlatformPanel(platform);
                platformPanels.add(panel);
                resultsPanel.add(panel);
                resultsPanel.add(Box.createVerticalStrut(10));
            });
        }
        
        refreshResultsPanel();
    }
    
    private void clearResults() {
        if (resultsPanel != null) {
            resultsPanel.removeAll();
            platformPanels.clear();
        }
    }
    
    private JLabel createNoResultsLabel() {
        JLabel label = new JBLabel("No application servers detected in this project.");
        label.setBorder(JBUI.Borders.empty(10));
        return label;
    }
    
    private void refreshResultsPanel() {
        resultsPanel.revalidate();
        resultsPanel.repaint();
    }
    
    /**
     * Gets the current enhanced platform risk score for dashboard integration
     */
    public double getCurrentPlatformRiskScore() {
        if (currentScanResult == null) {
            return 1.0; // Default low risk if no scan has been performed
        }
        return calculateEnhancedPlatformRiskScore(currentScanResult);
    }
    
    /**
     * Gets the current enhanced scan result
     */
    public EnhancedPlatformScanResult getCurrentScanResult() {
        return currentScanResult;
    }
    
    /**
     * Gets the detected platforms for dashboard integration
     */
    public List<String> getDetectedPlatforms() {
        return currentScanResult != null ? currentScanResult.getDetectedPlatforms() : new ArrayList<>();
    }
    
    /**
     * Calculates enhanced platform risk score based on detected platforms and deployment artifacts
     */
    private double calculateEnhancedPlatformRiskScore(EnhancedPlatformScanResult scanResult) {
        var riskConfig = configLoader.getRiskScoringConfig();
        
        double platformRisk = calculatePlatformRisk(scanResult.getDetectedPlatforms(), riskConfig);
        double artifactRisk = calculateArtifactRisk(scanResult, riskConfig);
        
        return Math.min(platformRisk + artifactRisk, riskConfig.getMaxTotalRisk());
    }
    
    private double calculatePlatformRisk(List<String> platforms, RiskScoringConfig riskConfig) {
        double baseRisk = Math.min(platforms.size() * riskConfig.getPlatformMultiplier(), riskConfig.getMaxPlatformRisk());
        double specificRisk = platforms.stream()
            .mapToDouble(riskConfig::getPlatformBaseRisk)
            .sum();
        return baseRisk + specificRisk;
    }
    
    private double calculateArtifactRisk(EnhancedPlatformScanResult scanResult, RiskScoringConfig riskConfig) {
        double artifactRisk = scanResult.getWarCount() * riskConfig.getDeploymentArtifactRisk("war") +
                             scanResult.getEarCount() * riskConfig.getDeploymentArtifactRisk("ear") +
                             scanResult.getJarCount() * riskConfig.getDeploymentArtifactRisk("jar");
        
        return artifactRisk + riskConfig.calculateArtifactComplexityRisk(scanResult.getTotalDeploymentCount());
    }
    
    /**
     * Creates a panel showing deployment artifact summary
     */
    private JPanel createArtifactSummaryPanel(EnhancedPlatformScanResult scanResult) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Deployment Artifacts"));
        
        String summaryText = buildArtifactSummaryText(scanResult);
        JLabel summaryLabel = new JBLabel(summaryText);
        panel.add(summaryLabel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private String buildArtifactSummaryText(EnhancedPlatformScanResult scanResult) {
        StringBuilder text = new StringBuilder("<html><b>Deployment Summary:</b><br>");
        
        // Basic artifact counts
        text.append(String.format("• WAR files: %d<br>", scanResult.getWarCount()));
        text.append(String.format("• EAR files: %d<br>", scanResult.getEarCount()));
        text.append(String.format("• JAR files: %d<br>", scanResult.getJarCount()));
        text.append(String.format("• Total artifacts: %d<br>", scanResult.getTotalDeploymentCount()));
        
        // Platform-specific artifacts
        scanResult.getPlatformSpecificArtifacts().entrySet().stream()
            .filter(entry -> entry.getValue() > 0)
            .forEach(entry -> text.append(String.format("• %s: %d<br>", entry.getKey(), entry.getValue())));
        
        // Risk assessment
        double riskScore = calculateEnhancedPlatformRiskScore(scanResult);
        text.append(String.format("<br><b>Migration Risk:</b> %s (%.1f/100)</html>", getRiskLevel(riskScore), riskScore));
        
        return text.toString();
    }
    
    /**
     * Gets risk level description based on score
     */
    private String getRiskLevel(double score) {
        if (score >= 80) return "Very High";
        if (score >= 60) return "High";
        if (score >= 40) return "Medium";
        if (score >= 20) return "Low";
        return "Very Low";
    }
    
    /**
     * Creates platform panel (unified method for both simple and enhanced)
     */
    private JPanel createPlatformPanel(String platformName, EnhancedPlatformScanResult scanResult, boolean showRisk) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        
        JLabel nameLabel = new JBLabel(platformName);
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 12f));
        
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.add(nameLabel, BorderLayout.NORTH);
        
        if (showRisk) {
            double platformRisk = calculateIndividualPlatformRisk(platformName, scanResult);
            JLabel riskLabel = createRiskLabel(platformRisk);
            infoPanel.add(riskLabel, BorderLayout.SOUTH);
        }
        
        panel.add(infoPanel, BorderLayout.CENTER);
        return panel;
    }
    
    private JLabel createRiskLabel(double risk) {
        String riskText = String.format("Risk: %s (%.1f/100)", getRiskLevel(risk), risk);
        JLabel riskLabel = new JBLabel(riskText);
        riskLabel.setFont(riskLabel.getFont().deriveFont(Font.ITALIC, 11f));
        riskLabel.setForeground(getRiskColor(risk));
        return riskLabel;
    }
    
    private Color getRiskColor(double risk) {
        if (risk >= 60) return Color.RED;
        if (risk >= 40) return Color.ORANGE;
        if (risk >= 20) return Color.YELLOW.darker();
        return Color.GREEN.darker();
    }
    
    private JPanel createSimplePlatformPanel(String platformName) {
        return createPlatformPanel(platformName, null, false);
    }
    
    private JPanel createEnhancedPlatformPanel(String platformName, EnhancedPlatformScanResult scanResult) {
        return createPlatformPanel(platformName, scanResult, true);
    }
    
    /**
     * Calculates individual platform risk considering artifacts
     */
    private double calculateIndividualPlatformRisk(String platformName, EnhancedPlatformScanResult scanResult) {
        var riskConfig = configLoader.getRiskScoringConfig();
        double baseRisk = riskConfig.getPlatformBaseRisk(platformName);
        
        if (scanResult != null && scanResult.getTotalDeploymentCount() > 0) {
            baseRisk += riskConfig.calculateArtifactComplexityRisk(scanResult.getTotalDeploymentCount());
        }
        
        return Math.min(baseRisk, riskConfig.getMaxTotalRisk());
    }
    
    /**
     * Updates premium controls based on premium status
     */
    public void updatePremiumControls() {
        boolean isPremium = adrianmikula.jakartamigration.intellij.ui.SupportComponent.isPremiumActive();
        System.out.println("DEBUG: PlatformsTabComponent.updatePremiumControls() - isPremiumActive: " + isPremium);
        
        lockIcon.setVisible(!isPremium);
        upgradeButton.setVisible(!isPremium);
        trialButton.setVisible(!isPremium);
        
        if (!isPremium) {
            // Disable scan functionality for non-premium users
            scanButton.setEnabled(false);
            scanButton.setToolTipText("Upgrade to Premium to enable platform scanning");
            System.out.println("DEBUG: Platforms scan button DISABLED");
        } else {
            scanButton.setEnabled(true);
            scanButton.setToolTipText(null);
            System.out.println("DEBUG: Platforms scan button ENABLED");
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
                "Error",
                JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * Refreshes the UI when trial status changes
     */
    public void refreshUI() {
        System.out.println("DEBUG: PlatformsTabComponent.refreshUI() called");
        updatePremiumControls();
        resultsPanel.revalidate();
        resultsPanel.repaint();
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
