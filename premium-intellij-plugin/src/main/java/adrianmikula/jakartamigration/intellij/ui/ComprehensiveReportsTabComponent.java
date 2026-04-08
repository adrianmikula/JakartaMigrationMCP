package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import adrianmikula.jakartamigration.pdfreporting.service.PdfReportService;
import adrianmikula.jakartamigration.pdfreporting.service.impl.PdfReportServiceImpl;
import adrianmikula.jakartamigration.pdfreporting.domain.ReportTemplate;
import adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults;
import adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults.ScanSummary;
import adrianmikula.jakartamigration.platforms.model.EnhancedPlatformScanResult;
import adrianmikula.jakartamigration.platforms.model.PlatformScanResult;
import adrianmikula.jakartamigration.platforms.service.SimplifiedPlatformDetectionService;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Map.of;

/**
 * Comprehensive Reports tab component for generating
 * detailed migration reports with dashboard data,
 * dependency analysis, and scan results.
 */
public class ComprehensiveReportsTabComponent {
    
    private final Project project;
    private PdfReportService reportService;
    
    // UI Components
    private JPanel mainPanel;
    private JButton generateReportButton;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JBTextArea outputArea;
    private JScrollPane scrollPane;
    
    public ComprehensiveReportsTabComponent(@NotNull Project project) {
        this.project = project;
        this.reportService = new PdfReportServiceImpl();
        
        createUI();
        initializeComponent();
    }
    
    private void createUI() {
        mainPanel = new JBPanel<>(new BorderLayout());
        
        // Layout components
        JPanel headerPanel = createHeaderPanel();
        JPanel contentPanel = createContentPanel();
        JPanel actionsPanel = createActionsPanel();
        
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        mainPanel.add(actionsPanel, BorderLayout.SOUTH);
    }
    
    /**
     * Creates header panel with title and description
     */
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(JBUI.Borders.empty(10));
        
        JLabel titleLabel = new JLabel("Comprehensive Migration Reports");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        
        JLabel descLabel = new JLabel("<html><center>Generate detailed migration reports with dashboard data, dependency analysis, and platform scan results.</center></html>");
        descLabel.setFont(descLabel.getFont().deriveFont(Font.PLAIN, 11f));
        
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        titlePanel.add(titleLabel);
        
        JPanel descPanel = new JPanel(new BorderLayout());
        descPanel.add(descLabel, BorderLayout.CENTER);
        
        headerPanel.add(titlePanel, BorderLayout.NORTH);
        headerPanel.add(descPanel, BorderLayout.CENTER);
        
        return headerPanel;
    }
    
    /**
     * Creates content panel with output area
     */
    private JPanel createContentPanel() {
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(JBUI.Borders.empty(10));
        
        outputArea = new JBTextArea();
        outputArea.setEditable(false);
        outputArea.setBackground(Color.WHITE);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        scrollPane = new JScrollPane(outputArea);
        scrollPane.setPreferredSize(new Dimension(600, 200));
        
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        
        return contentPanel;
    }
    
    /**
     * Creates actions panel with generate button
     */
    private JPanel createActionsPanel() {
        JPanel actionsPanel = new JPanel(new FlowLayout());
        generateReportButton = new JButton("Generate Report");
        generateReportButton.setToolTipText("Generate comprehensive migration report with dashboard data and scan results");
        actionsPanel.add(generateReportButton);
        
        return actionsPanel;
    }
    
    /**
     * Initializes the component
     */
    private void initializeComponent() {
        // Set up action listeners
        generateReportButton.addActionListener(this::handleGenerateReport);
        
        // Initialize progress bar
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        
        // Initialize status label
        statusLabel = new JLabel("Ready to generate report");
    }
    
    /**
     * Handles report generation
     */
    private void handleGenerateReport(java.awt.event.ActionEvent e) {
        try {
            // Show progress
            progressBar.setVisible(true);
            progressBar.setIndeterminate(true);
            statusLabel.setText("Generating comprehensive report...");
            generateReportButton.setEnabled(false);
            
            // Generate PDF report
            String outputPath = chooseOutputLocation();
            if (outputPath == null) {
                statusLabel.setText("Report generation cancelled");
                return;
            }
            
            Map<String, Object> customData = of(
                    "projectName", project.getName(),
                    "pluginVersion", getPluginVersion(),
                    "jdkVersion", System.getProperty("java.version", "Unknown"),
                    "buildSystem", "Gradle"
            );
            
            java.nio.file.Path resultPath = reportService.generateComprehensiveReport(
                new PdfReportService.GeneratePdfReportRequest(
                    Paths.get(outputPath),
                    getDependencyGraph(),
                    null,
                    getScanResults(),
                    getPlatformScanResults(),
                    reportService.getDefaultTemplate(),
                    customData
                )
            );
            
            String resultString = resultPath.toString();
            
            // Show success and ask to open file
            outputArea.setText("PDF report generated successfully!\n");
            outputArea.append("Output file: " + resultString + "\n");
            
            int choice = com.intellij.openapi.ui.Messages.showYesNoDialog(
                project,
                "PDF report generated successfully!\n\nWould you like to open PDF file?",
                "Report Generated",
                com.intellij.openapi.ui.Messages.getQuestionIcon()
            );
            
            if (choice == com.intellij.openapi.ui.Messages.YES) {
                openFile(resultString);
            }
            
        } catch (Exception ex) {
            outputArea.setText("Error generating report: " + ex.getMessage() + "\n");
            com.intellij.openapi.ui.Messages.showErrorDialog(project, "Failed to generate report: " + ex.getMessage(), "Error");
        } finally {
            // Reset UI state
            progressBar.setVisible(false);
            progressBar.setIndeterminate(false);
            statusLabel.setText("Ready to generate report");
            generateReportButton.setEnabled(true);
        }
    }
    
    /**
     * Chooses output location for report
     */
    private String chooseOutputLocation() {
        // Use project directory with timestamped filename
        String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        return project.getBasePath() + "/Jakarta_Migration_Report_" + timestamp + ".pdf";
    }
    
    /**
     * Gets dependency graph (placeholder implementation)
     */
    private DependencyGraph getDependencyGraph() {
        return new DependencyGraph() {
            public List<DependencyInfo> getAllDependencies() {
                return Collections.emptyList();
            }
            
            public int getTotalDependencies() {
                return 0;
            }
        };
    }
    
    /**
     * Gets scan results (placeholder implementation)
     */
    private ComprehensiveScanResults getScanResults() {
        return new ComprehensiveScanResults(
                "/test/project",
                java.time.LocalDateTime.now(),
                java.util.Collections.emptyMap(),
                java.util.Collections.emptyMap(),
                java.util.Collections.emptyMap(),
                java.util.Collections.emptyMap(),
                java.util.Collections.emptyMap(),
                java.util.Collections.emptyMap(),
                java.util.Collections.emptyList(),
                0,
                new ScanSummary(0, 0, 0, 0, 0, 0.0)
        );
    }
    
    /**
     * Gets platform scan results using actual platform detection service
     */
    private PlatformScanResult getPlatformScanResults() {
        try {
            String basePath = project.getBasePath();
            if (basePath == null) {
                return null;
            }
            SimplifiedPlatformDetectionService detectionService = new SimplifiedPlatformDetectionService();
            EnhancedPlatformScanResult result = detectionService.scanProjectWithArtifacts(java.nio.file.Path.of(basePath));
            // Convert EnhancedPlatformScanResult to PlatformScanResult format
            return new PlatformScanResult(
                result.getDetectedPlatforms().stream()
                    .map(name -> new adrianmikula.jakartamigration.platforms.model.PlatformDetection(
                        name, name, "detected", true, "", java.util.Collections.emptyMap()
                    ))
                    .toList(),
                result.getTotalDeploymentCount(),
                java.util.Collections.emptyList()
            );
        } catch (Exception e) {
            // Return null if platform detection fails
            return null;
        }
    }

    /**
     * Gets plugin version
     */
    private String getPluginVersion() {
        return "1.0.8";
    }
    
    /**
     * Opens generated PDF file
     */
    private void openFile(String filePath) {
        try {
            java.awt.Desktop.getDesktop().open(new java.io.File(filePath));
        } catch (IOException ex) {
            com.intellij.openapi.ui.Messages.showErrorDialog(project, "Failed to open PDF file: " + ex.getMessage(), "Error");
        }
    }
    
    public JComponent getPanel() {
        return mainPanel;
    }
}
