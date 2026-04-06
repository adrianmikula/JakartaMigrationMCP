package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.platforms.model.PlatformDetection;
import adrianmikula.jakartamigration.platforms.model.PlatformScanResult;
import adrianmikula.jakartamigration.platforms.model.EnhancedPlatformScanResult;
import adrianmikula.jakartamigration.platforms.model.PlatformConfig;
import adrianmikula.jakartamigration.platforms.model.JakartaCompatibility;
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
        
        // Results area - inline setup from working commit
        resultsPanel = new JPanel();
        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
        resultsScrollPane = new JBScrollPane(resultsPanel);
        resultsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        resultsScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        mainPanel.add(resultsScrollPane, BorderLayout.CENTER);
        
        mainPanel.add(createPremiumPanel(), BorderLayout.SOUTH);
        updatePremiumControls();
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
        System.out.println("[DEBUG] Starting enhanced platform scan for project: " + project.getBasePath());
        log.debug("Starting enhanced platform scan for project: {}", project.getBasePath());
        scanButton.setEnabled(false);
        scanButton.setText("Scanning...");
        
        SwingWorker<EnhancedPlatformScanResult, Void> worker = new SwingWorker<>() {
            @Override
            protected EnhancedPlatformScanResult doInBackground() throws Exception {
                System.out.println("[DEBUG] Calling enhancedDetectionService.scanProjectWithArtifacts()");
                EnhancedPlatformScanResult result = enhancedDetectionService.scanProjectWithArtifacts(java.nio.file.Paths.get(project.getBasePath()));
                System.out.println("[DEBUG] Scan completed. Found platforms: " + result.getDetectedPlatforms());
                return result;
            }
            
            @Override
            protected void done() {
                try {
                    EnhancedPlatformScanResult scanResult = get();
                    System.out.println("[DEBUG] SwingWorker done. Found " + scanResult.getDetectedPlatforms().size() + " platforms");
                    log.debug("Enhanced platform scan completed. Found {} platforms, WARs: {}, EARs: {}", 
                             scanResult.getDetectedPlatforms().size(), 
                             scanResult.getWarCount(), 
                             scanResult.getEarCount());
                    displayEnhancedResults(scanResult);
                    scanButton.setEnabled(true);
                    scanButton.setText("Analyse Project");
                } catch (Exception e) {
                    System.err.println("[DEBUG] Platform scan failed: " + e.getMessage());
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
        System.out.println("[DEBUG] displayResults() called with " + scanResult.getDetectedPlatforms().size() + " platforms");
        this.currentScanResult = scanResult;
        
        clearResults();
        
        List<String> detectedPlatforms = scanResult.getDetectedPlatforms();
        System.out.println("[DEBUG] detectedPlatforms isEmpty: " + detectedPlatforms.isEmpty());
        
        if (detectedPlatforms.isEmpty() && scanResult.getTotalDeploymentCount() == 0) {
            System.out.println("[DEBUG] Adding 'no results' label");
            resultsPanel.add(createNoResultsLabel());
        } else {
            // Section 1: Detected Platforms (cards)
            JPanel platformsSection = createPlatformsSection(detectedPlatforms);
            resultsPanel.add(platformsSection);
            resultsPanel.add(Box.createVerticalStrut(15));
            
            // Section 2: Deployment Artifacts (summary badges)
            JPanel artifactsSection = createArtifactsSection(scanResult);
            resultsPanel.add(artifactsSection);
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
        if (resultsScrollPane != null) {
            resultsScrollPane.revalidate();
            resultsScrollPane.repaint();
        }
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
    private JPanel createPlatformsSection(List<String> detectedPlatforms) {
        JPanel sectionPanel = new JPanel(new BorderLayout());
        sectionPanel.setBorder(BorderFactory.createTitledBorder("Detected Platforms"));
        
        JPanel cardsPanel = new JPanel();
        cardsPanel.setLayout(new BoxLayout(cardsPanel, BoxLayout.Y_AXIS));
        
        if (detectedPlatforms.isEmpty()) {
            JLabel noPlatformsLabel = new JBLabel("No platforms detected");
            noPlatformsLabel.setBorder(JBUI.Borders.empty(10));
            cardsPanel.add(noPlatformsLabel);
        } else {
            for (String platformName : detectedPlatforms) {
                JPanel card = createPlatformCard(platformName);
                cardsPanel.add(card);
                cardsPanel.add(Box.createVerticalStrut(8));
            }
        }
        
        JScrollPane scrollPane = new JBScrollPane(cardsPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setPreferredSize(new Dimension(400, 250));
        
        sectionPanel.add(scrollPane, BorderLayout.CENTER);
        return sectionPanel;
    }
    
    private JPanel createPlatformCard(String platformName) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        PlatformConfig config = configLoader.getPlatformConfig(platformName.toLowerCase());
        
        // Header with platform name
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel nameLabel = new JBLabel(config != null ? config.name() : platformName);
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 13f));
        headerPanel.add(nameLabel, BorderLayout.WEST);
        card.add(headerPanel, BorderLayout.NORTH);
        
        // Content panel with compatibility info
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        
        if (config != null && config.jakartaCompatibility() != null) {
            JakartaCompatibility compat = config.jakartaCompatibility();
            
            // Upgrade requirement status
            boolean requiresUpgrade = config.javaxVersions() != null && !config.javaxVersions().isEmpty();
            JLabel statusLabel = new JBLabel(requiresUpgrade ? 
                "⚠️ Requires Upgrade" : "✅ Jakarta EE Compatible");
            statusLabel.setForeground(requiresUpgrade ? 
                new Color(255, 152, 0) : new Color(40, 167, 69));
            contentPanel.add(statusLabel);
            contentPanel.add(Box.createVerticalStrut(4));
            
            // Jakarta compatible versions
            JLabel versionsLabel = new JBLabel(
                "Jakarta Compatible Versions: " + String.join(", ", compat.supportedVersions()));
            versionsLabel.setFont(versionsLabel.getFont().deriveFont(Font.PLAIN, 11f));
            contentPanel.add(versionsLabel);
            contentPanel.add(Box.createVerticalStrut(4));
            
            // Requirements
            if (config.requirements() != null && !config.requirements().isEmpty()) {
                StringBuilder reqText = new StringBuilder("Requirements: ");
                config.requirements().forEach((key, value) -> 
                    reqText.append(String.format("%s=%s ", key, value)));
                JLabel reqLabel = new JBLabel(reqText.toString());
                reqLabel.setFont(reqLabel.getFont().deriveFont(Font.ITALIC, 10f));
                reqLabel.setForeground(Color.GRAY);
                contentPanel.add(reqLabel);
            }
        } else {
            JLabel unknownLabel = new JBLabel("Platform configuration not available");
            unknownLabel.setForeground(Color.GRAY);
            contentPanel.add(unknownLabel);
        }
        
        card.add(contentPanel, BorderLayout.CENTER);
        return card;
    }
    
    private JPanel createArtifactsSection(EnhancedPlatformScanResult scanResult) {
        JPanel sectionPanel = new JPanel(new BorderLayout());
        sectionPanel.setBorder(BorderFactory.createTitledBorder("Deployment Artifacts"));
        
        JPanel contentPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));
        contentPanel.setBorder(JBUI.Borders.empty(10));
        
        int warCount = scanResult.getWarCount();
        int earCount = scanResult.getEarCount();
        int jarCount = scanResult.getJarCount();
        int total = scanResult.getTotalDeploymentCount();
        
        // WAR count badge
        contentPanel.add(createArtifactBadge("WAR", warCount, new Color(70, 130, 180)));
        // EAR count badge
        contentPanel.add(createArtifactBadge("EAR", earCount, new Color(128, 0, 128)));
        // JAR count badge
        contentPanel.add(createArtifactBadge("JAR", jarCount, new Color(34, 139, 34)));
        // Total badge
        contentPanel.add(createArtifactBadge("Total", total, Color.DARK_GRAY));
        
        if (total == 0) {
            JLabel noArtifactsLabel = new JBLabel("No deployment artifacts detected");
            noArtifactsLabel.setForeground(Color.GRAY);
            noArtifactsLabel.setFont(noArtifactsLabel.getFont().deriveFont(Font.ITALIC));
            contentPanel.removeAll();
            contentPanel.add(noArtifactsLabel);
        }
        
        sectionPanel.add(contentPanel, BorderLayout.CENTER);
        return sectionPanel;
    }
    
    private JPanel createArtifactBadge(String type, int count, Color color) {
        JPanel badge = new JPanel(new BorderLayout());
        badge.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color, 2),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        badge.setBackground(new Color(250, 250, 250));
        
        // Type label at the top - prominent
        JLabel typeLabel = new JBLabel(type);
        typeLabel.setFont(typeLabel.getFont().deriveFont(Font.BOLD, 12f));
        typeLabel.setForeground(color.darker());
        typeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // Count label below - larger
        JLabel countLabel = new JBLabel(String.valueOf(count));
        countLabel.setFont(countLabel.getFont().deriveFont(Font.BOLD, 24f));
        countLabel.setForeground(color);
        countLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        textPanel.setOpaque(false);
        textPanel.add(typeLabel);
        textPanel.add(countLabel);
        
        badge.add(textPanel, BorderLayout.CENTER);
        return badge;
    }
    
    private Color getRiskColor(double risk) {
        if (risk >= 60) return Color.RED;
        if (risk >= 40) return Color.ORANGE;
        if (risk >= 20) return Color.YELLOW.darker();
        return Color.GREEN.darker();
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
