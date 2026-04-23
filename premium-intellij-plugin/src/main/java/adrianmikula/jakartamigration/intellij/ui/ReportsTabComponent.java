package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.pdfreporting.service.PdfReportService;
import adrianmikula.jakartamigration.pdfreporting.service.impl.HtmlToPdfReportServiceImpl;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults;
import adrianmikula.jakartamigration.intellij.service.AdvancedScanningService;
import adrianmikula.jakartamigration.intellij.service.MigrationAnalysisService;
import adrianmikula.jakartamigration.credits.CreditType;
import adrianmikula.jakartamigration.credits.CreditsService;
import adrianmikula.jakartamigration.intellij.license.CheckLicense;
import adrianmikula.jakartamigration.intellij.ui.components.PremiumUpgradeButton;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.awt.Desktop;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Reports tab component for generating PDF reports based on scan data.
 * This is an experimental feature that requires premium + experimental features enabled.
 */
public class ReportsTabComponent {
    
    private final Project project;
    private final JPanel mainPanel;
    private final PdfReportService pdfReportService;
    private final MigrationAnalysisService migrationAnalysisService;
    private final AdvancedScanningService advancedScanningService;
    private final CreditsService creditsService;
    
    // UI Components
    private final JButton generateDependencyReportButton;
    private final JButton generateScanResultsReportButton;
    private final JButton generateComprehensiveReportButton;
    private final JLabel statusLabel;
    private final JTextArea outputArea;
    private final JPanel upgradeButtonPanel;
    
    public ReportsTabComponent(Project project, MigrationAnalysisService migrationAnalysisService, AdvancedScanningService advancedScanningService) {
        this.project = project;
        this.pdfReportService = new HtmlToPdfReportServiceImpl();
        this.migrationAnalysisService = migrationAnalysisService;
        this.advancedScanningService = advancedScanningService;
        this.creditsService = new CreditsService();
        
        // Initialize UI components
        generateDependencyReportButton = new JButton("Generate Dependency Report");
        generateScanResultsReportButton = new JButton("Generate Scan Results Report");
        generateComprehensiveReportButton = new JButton("Generate Comprehensive Report");
        statusLabel = new JBLabel("Ready to generate reports");
        outputArea = new JTextArea(10, 40);
        outputArea.setEditable(false);
        outputArea.setFont(JBUI.Fonts.smallFont());
        
        // Create upgrade button panel (conditionally shown)
        upgradeButtonPanel = createUpgradeButtonPanel();
        
        // Initially hide upgrade button for premium users
        boolean isPremium = CheckLicense.isLicensed();
        upgradeButtonPanel.setVisible(!isPremium);
        
        // Setup main panel
        mainPanel = createMainPanel();
        setupEventHandlers();
    }
    
    private JPanel createMainPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(JBUI.Borders.empty(10));
        
        JLabel titleLabel = new JBLabel("Professional PDF Reports");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        headerPanel.add(titleLabel, BorderLayout.NORTH);
        
        JLabel descriptionLabel = new JBLabel("<html><body style='width: 600px'>" +
            "Generate professional PDF reports with modern HTML-to-PDF rendering. " +
            "Reports include executive summary, risk assessment, dependency analysis, " +
            "scan results, and actionable migration recommendations. " +
            "Features professional styling and layout for enterprise-ready reports." +
            "</body></html>");
        descriptionLabel.setBorder(JBUI.Borders.emptyTop(5));
        headerPanel.add(descriptionLabel, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new GridBagLayout());
        buttonPanel.setBorder(JBUI.Borders.empty(10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        
        buttonPanel.add(generateDependencyReportButton, gbc);
        
        gbc.gridy = 1;
        buttonPanel.add(generateScanResultsReportButton, gbc);
        
        gbc.gridy = 2;
        buttonPanel.add(generateComprehensiveReportButton, gbc);
        
        // Status panel
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(JBUI.Borders.empty(10));
        statusPanel.add(statusLabel, BorderLayout.NORTH);
        
        // Output area
        JScrollPane scrollPane = new JBScrollPane(outputArea);
        scrollPane.setBorder(JBUI.Borders.empty(10));
        scrollPane.setPreferredSize(new Dimension(600, 200));
        
        // Assemble main panel
        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(buttonPanel, BorderLayout.CENTER);
        panel.add(statusPanel, BorderLayout.SOUTH);
        
        // Create form layout with upgrade button panel
        FormBuilder formBuilder = new FormBuilder();
        formBuilder.addComponent(panel);
        formBuilder.addVerticalGap(10);
        formBuilder.addComponent(scrollPane);
        formBuilder.addVerticalGap(5);
        formBuilder.addComponent(upgradeButtonPanel); // Add upgrade button panel
        
        return formBuilder.getPanel();
    }
    
    private void setupEventHandlers() {
        generateDependencyReportButton.addActionListener(e -> generateDependencyReport());
        generateScanResultsReportButton.addActionListener(e -> generateScanResultsReport());
        generateComprehensiveReportButton.addActionListener(e -> generateComprehensiveReport());
    }
    
    private JPanel createUpgradeButtonPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Create upgrade button with custom message for reports
        JButton upgradeButton = PremiumUpgradeButton.createUpgradeButton(
            project, 
            "⬆ Upgrade to Premium for Unlimited Reports",
            "Get unlimited PDF reports, advanced scanning, and professional features"
        );
        
        // Add upgrade button to center
        JPanel buttonWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonWrapper.add(upgradeButton);
        panel.add(buttonWrapper, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void showUpgradePrompt(String title, String message) {
        // Update status and output to show upgrade prompt
        statusLabel.setText(title);
        outputArea.setText(message + "\n\n");
        outputArea.append("Upgrade button is available below to unlock premium features.\n");
        
        // Make upgrade button panel visible
        upgradeButtonPanel.setVisible(true);
        
        // Scroll to show upgrade button
        SwingUtilities.invokeLater(() -> {
            JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, outputArea);
            if (scrollPane != null) {
                JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
                verticalScrollBar.setValue(verticalScrollBar.getMaximum());
            }
        });
    }
    
    private void generateDependencyReport() {
        try {
            // Check if user is premium
            boolean isPremium = CheckLicense.isLicensed();
            
            // Check if PDF reports are premium-only feature
            if (adrianmikula.jakartamigration.intellij.config.FeatureFlags.getInstance().isPdfReportsPremiumOnly()) {
                if (!isPremium) {
                    showUpgradePrompt("Premium Feature Required", 
                        "PDF reports require Premium. Upgrade to generate unlimited PDF reports with professional styling.");
                    return;
                }
            } else {
                // Check credits for free users (if not premium-only)
                if (!isPremium) {
                    int remainingCredits = creditsService.getRemainingCredits(CreditType.ACTIONS);
                    if (remainingCredits <= 0) {
                        showUpgradePrompt("Action Credits Exhausted", 
                            "You've used all your action credits. Upgrade to Premium for unlimited PDF reports.");
                        return;
                    }
                }
            }

            statusLabel.setText("Generating dependency report...");
            outputArea.setText("Starting dependency report generation...\n");
            
            // Get real dependency graph from the analysis service
            Path projectPath = Paths.get(project.getBasePath());
            DependencyGraph dependencyGraph = migrationAnalysisService.getDependencyGraph(projectPath);
            
            if (dependencyGraph == null || dependencyGraph.getNodes().isEmpty()) {
                outputArea.append("Warning: No dependencies found. Run analysis first.\n");
                statusLabel.setText("No dependency data available");
                return;
            }
            
            outputArea.append("Found " + dependencyGraph.getNodes().size() + " dependencies\n");
            
            // Choose output location
            Path outputPath = chooseOutputLocation("dependency-report.pdf");
            if (outputPath == null) {
                statusLabel.setText("Report generation cancelled");
                return;
            }
            
            // Generate report
            Path result = pdfReportService.generateDependencyReport(dependencyGraph, outputPath);
            
            // Consume 1 action credit for free users (only if not premium-only)
            if (!isPremium && !adrianmikula.jakartamigration.intellij.config.FeatureFlags.getInstance().isPdfReportsPremiumOnly()) {
                boolean creditConsumed = creditsService.useCredit(CreditType.ACTIONS);
                if (creditConsumed) {
                    outputArea.append("Action credit consumed successfully\n");
                } else {
                    outputArea.append("Warning: Failed to consume Action credit\n");
                }
            }
            
            outputArea.append("Dependency report generated successfully!\n");
            outputArea.append("Output file: " + result.toAbsolutePath() + "\n");
            outputArea.append("File size: " + result.toFile().length() + " bytes\n");
            
            statusLabel.setText("Dependency report generated successfully");
            
            // Ask if user wants to open the file
            int choice = Messages.showYesNoDialog(
                project,
                "Dependency report generated successfully!\n\nWould you like to open the PDF file?",
                "Report Generated",
                Messages.getQuestionIcon());
            
            if (choice == Messages.YES) {
                openFile(result);
            }
            
        } catch (Exception e) {
            outputArea.append("Error generating dependency report: " + e.getMessage() + "\n");
            statusLabel.setText("Error generating report");
            Messages.showErrorDialog(project, "Failed to generate dependency report: " + e.getMessage(), "Error");
        }
    }
    
    private void generateScanResultsReport() {
        try {
            // Check if user is premium
            boolean isPremium = CheckLicense.isLicensed();
            
            // Check if PDF reports are premium-only feature
            if (adrianmikula.jakartamigration.intellij.config.FeatureFlags.getInstance().isPdfReportsPremiumOnly()) {
                if (!isPremium) {
                    showUpgradePrompt("Premium Feature Required", 
                        "PDF reports require Premium. Upgrade to generate unlimited PDF reports with professional styling.");
                    return;
                }
            } else {
                // Check credits for free users (if not premium-only)
                if (!isPremium) {
                    int remainingCredits = creditsService.getRemainingCredits(CreditType.ACTIONS);
                    if (remainingCredits <= 0) {
                        showUpgradePrompt("Action Credits Exhausted", 
                            "You've used all your action credits. Upgrade to Premium for unlimited PDF reports.");
                        return;
                    }
                }
            }

            statusLabel.setText("Generating scan results report...");
            outputArea.setText("Starting scan results report generation...\n");
            
            // Get real scan results from the advanced scanning service
            ComprehensiveScanResults scanResults = getRealScanResults();
            
            if (scanResults == null) {
                outputArea.append("Warning: No scan results found. Run analysis first.\n");
                statusLabel.setText("No scan data available");
                return;
            }
            
            outputArea.append("Total issues found: " + scanResults.totalIssuesFound() + "\n");
            
            // Choose output location
            Path outputPath = chooseOutputLocation("scan-results-report.pdf");
            if (outputPath == null) {
                statusLabel.setText("Report generation cancelled");
                return;
            }
            
            // Generate report
            Path result = pdfReportService.generateScanResultsReport(scanResults, outputPath);
            
            // Consume 1 action credit for free users (only if not premium-only)
            if (!isPremium && !adrianmikula.jakartamigration.intellij.config.FeatureFlags.getInstance().isPdfReportsPremiumOnly()) {
                boolean creditConsumed = creditsService.useCredit(CreditType.ACTIONS);
                if (creditConsumed) {
                    outputArea.append("Action credit consumed successfully\n");
                } else {
                    outputArea.append("Warning: Failed to consume Action credit\n");
                }
            }
            
            outputArea.append("Scan results report generated successfully!\n");
            outputArea.append("Output file: " + result.toAbsolutePath() + "\n");
            outputArea.append("File size: " + result.toFile().length() + " bytes\n");
            
            statusLabel.setText("Scan results report generated successfully");
            
            // Ask if user wants to open the file
            int choice = Messages.showYesNoDialog(
                project,
                "Scan results report generated successfully!\n\nWould you like to open the PDF file?",
                "Report Generated",
                Messages.getQuestionIcon());
            
            if (choice == Messages.YES) {
                openFile(result);
            }
            
        } catch (Exception e) {
            outputArea.append("Error generating scan results report: " + e.getMessage() + "\n");
            statusLabel.setText("Error generating report");
            Messages.showErrorDialog(project, "Failed to generate scan results report: " + e.getMessage(), "Error");
        }
    }
    
    private void generateComprehensiveReport() {
        try {
            // Check if user is premium
            boolean isPremium = CheckLicense.isLicensed();
            
            // Check if PDF reports are premium-only feature
            if (adrianmikula.jakartamigration.intellij.config.FeatureFlags.getInstance().isPdfReportsPremiumOnly()) {
                if (!isPremium) {
                    showUpgradePrompt("Premium Feature Required", 
                        "PDF reports require Premium. Upgrade to generate unlimited PDF reports with professional styling.");
                    return;
                }
            } else {
                // Check credits for free users (if not premium-only)
                if (!isPremium) {
                    int remainingCredits = creditsService.getRemainingCredits(CreditType.ACTIONS);
                    if (remainingCredits <= 0) {
                        showUpgradePrompt("Action Credits Exhausted", 
                            "You've used all your action credits. Upgrade to Premium for unlimited PDF reports.");
                        return;
                    }
                }
            }

            statusLabel.setText("Generating comprehensive report...");
            outputArea.setText("Starting comprehensive report generation...\n");
            
            // Get real data from services
            Path projectPath = Paths.get(project.getBasePath());
            DependencyGraph dependencyGraph = migrationAnalysisService.getDependencyGraph(projectPath);
            ComprehensiveScanResults scanResults = getRealScanResults();
            
            if (dependencyGraph == null || dependencyGraph.getNodes().isEmpty()) {
                outputArea.append("Warning: No dependency data found. Run analysis first.\n");
                statusLabel.setText("No analysis data available");
                return;
            }
            
            outputArea.append("Found " + dependencyGraph.getNodes().size() + " dependencies\n");
            if (scanResults != null) {
                outputArea.append("Total scan issues: " + scanResults.totalIssuesFound() + "\n");
            }
            
            // Choose output location
            Path outputPath = chooseOutputLocation("comprehensive-report.pdf");
            if (outputPath == null) {
                statusLabel.setText("Report generation cancelled");
                return;
            }
            
            // Create custom data
            Map<String, Object> customData = new HashMap<>();
            customData.put("projectName", project.getName());
            customData.put("description", "Jakarta Migration Analysis Report");
            customData.put("generatedAt", LocalDateTime.now().toString());
            
            // Generate report
            PdfReportService.GeneratePdfReportRequest request = new PdfReportService.GeneratePdfReportRequest(
                outputPath,
                dependencyGraph,
                null,
                scanResults,
                null,
                pdfReportService.getDefaultTemplate(),
                customData
            );
            
            Path result = pdfReportService.generateComprehensiveReport(request);
            
            // Consume 1 action credit for free users (only if not premium-only)
            if (!isPremium && !adrianmikula.jakartamigration.intellij.config.FeatureFlags.getInstance().isPdfReportsPremiumOnly()) {
                boolean creditConsumed = creditsService.useCredit(CreditType.ACTIONS);
                if (creditConsumed) {
                    outputArea.append("Action credit consumed successfully\n");
                } else {
                    outputArea.append("Warning: Failed to consume Action credit\n");
                }
            }
            
            outputArea.append("Comprehensive report generated successfully!\n");
            outputArea.append("Output file: " + result.toAbsolutePath() + "\n");
            outputArea.append("File size: " + result.toFile().length() + " bytes\n");
            
            statusLabel.setText("Comprehensive report generated successfully");
            
            // Ask if user wants to open the file
            int choice = Messages.showYesNoDialog(
                project,
                "Comprehensive report generated successfully!\n\nWould you like to open the PDF file?",
                "Report Generated",
                Messages.getQuestionIcon());
            
            if (choice == Messages.YES) {
                openFile(result);
            }
            
        } catch (Exception e) {
            outputArea.append("Error generating comprehensive report: " + e.getMessage() + "\n");
            statusLabel.setText("Error generating report");
            Messages.showErrorDialog(project, "Failed to generate comprehensive report: " + e.getMessage(), "Error");
        }
    }
    
    private Path chooseOutputLocation(String defaultFileName) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Choose Report Output Location");
        fileChooser.setSelectedFile(new File(defaultFileName));
        
        int userSelection = fileChooser.showSaveDialog(mainPanel);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            return Paths.get(fileChooser.getSelectedFile().getAbsolutePath());
        }
        return null;
    }
    
    private void openFile(Path file) {
        try {
            Desktop.getDesktop().open(file.toFile());
        } catch (Exception e) {
            Messages.showErrorDialog(project, "Failed to open PDF file: " + e.getMessage(), "Error");
        }
    }
    
    // Helper method to get real scan results from the advanced scanning service
    private ComprehensiveScanResults getRealScanResults() {
        if (advancedScanningService == null) {
            return null;
        }
        
        // Check if we have cached results
        if (advancedScanningService.hasCachedResults()) {
            AdvancedScanningService.AdvancedScanSummary summary = advancedScanningService.getCachedSummary();
            if (summary != null) {
                // Convert AdvancedScanSummary to ComprehensiveScanResults
                return convertToComprehensiveScanResults(summary);
            }
        }
        
        // No cached results available - return null
        return null;
    }
    
    // Convert AdvancedScanSummary to ComprehensiveScanResults
    private ComprehensiveScanResults convertToComprehensiveScanResults(AdvancedScanningService.AdvancedScanSummary summary) {
        java.util.Map<String, Object> jpaResults = new java.util.HashMap<>();
        jpaResults.put("count", summary.getJpaCount());
        
        java.util.Map<String, Object> beanValidationResults = new java.util.HashMap<>();
        beanValidationResults.put("count", summary.getBeanValidationCount());
        
        java.util.Map<String, Object> servletJspResults = new java.util.HashMap<>();
        servletJspResults.put("count", summary.getServletJspCount());
        
        java.util.Map<String, Object> thirdPartyLibResults = new java.util.HashMap<>();
        thirdPartyLibResults.put("count", summary.getThirdPartyLibCount());
        
        java.util.Map<String, Object> buildConfigResults = new java.util.HashMap<>();
        buildConfigResults.put("count", summary.getBuildConfigCount());
        
        java.util.Map<String, Object> transitiveDependencyResults = new java.util.HashMap<>();
        transitiveDependencyResults.put("count", summary.getTransitiveDependencyCount());
        
        // Create scan summary
        ComprehensiveScanResults.ScanSummary scanSummary = new ComprehensiveScanResults.ScanSummary(
            0, // totalFilesScanned - not directly available from summary
            summary.getTotalIssuesFound(), // filesWithIssues
            0, // criticalIssues - not directly available
            0, // warningIssues - not directly available
            0, // infoIssues - not directly available
            0.0 // readinessScore - not directly available
        );
        
        return new ComprehensiveScanResults(
            project.getBasePath(),
            LocalDateTime.now(),
            jpaResults,
            beanValidationResults,
            servletJspResults,
            thirdPartyLibResults,
            transitiveDependencyResults,
            buildConfigResults,
            java.util.List.of(), // recommendations - empty for now
            summary.getTotalIssuesFound(),
            scanSummary
        );
    }
    
    // Sample data methods (deprecated - use getRealScanResults instead)
    private DependencyGraph createSampleDependencyGraph() {
        // This method is kept for backward compatibility but should not be used
        // Real implementation should call migrationAnalysisService.getDependencyGraph()
        return new DependencyGraph();
    }
    
    private ComprehensiveScanResults createSampleScanResults() {
        // Try to get real results first
        ComprehensiveScanResults realResults = getRealScanResults();
        if (realResults != null) {
            return realResults;
        }
        
        // Fallback to empty results if no real data available
        ComprehensiveScanResults.ScanSummary summary = new ComprehensiveScanResults.ScanSummary(
            0, 0, 0, 0, 0, 0.0
        );
        
        return new ComprehensiveScanResults(
            project.getBasePath(),
            LocalDateTime.now(),
            java.util.Map.of(),
            java.util.Map.of(),
            java.util.Map.of(),
            java.util.Map.of(),
            java.util.Map.of(),
            java.util.Map.of(),
            java.util.List.of(),
            0,
            summary
        );
    }
    
    public JPanel getPanel() {
        return mainPanel;
    }
    
    public void refresh() {
        statusLabel.setText("Ready to generate reports");
        outputArea.setText("");
        
        // Update upgrade button visibility based on premium status
        boolean isPremium = CheckLicense.isLicensed();
        upgradeButtonPanel.setVisible(!isPremium);
    }
}
