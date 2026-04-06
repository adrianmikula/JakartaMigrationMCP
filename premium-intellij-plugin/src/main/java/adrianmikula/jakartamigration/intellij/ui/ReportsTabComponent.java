package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.pdfreporting.service.PdfReportService;
import adrianmikula.jakartamigration.pdfreporting.service.impl.PdfReportServiceImpl;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults;
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
    
    // UI Components
    private final JButton generateDependencyReportButton;
    private final JButton generateScanResultsReportButton;
    private final JButton generateComprehensiveReportButton;
    private final JLabel statusLabel;
    private final JTextArea outputArea;
    
    public ReportsTabComponent(Project project) {
        this.project = project;
        this.pdfReportService = new PdfReportServiceImpl();
        
        // Initialize UI components
        generateDependencyReportButton = new JButton("Generate Dependency Report");
        generateScanResultsReportButton = new JButton("Generate Scan Results Report");
        generateComprehensiveReportButton = new JButton("Generate Comprehensive Report");
        statusLabel = new JBLabel("Ready to generate reports");
        outputArea = new JTextArea(10, 40);
        outputArea.setEditable(false);
        outputArea.setFont(JBUI.Fonts.smallFont());
        
        // Setup main panel
        mainPanel = createMainPanel();
        setupEventHandlers();
    }
    
    private JPanel createMainPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(JBUI.Borders.empty(10));
        
        JLabel titleLabel = new JBLabel("PDF Reports (Experimental)");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        headerPanel.add(titleLabel, BorderLayout.NORTH);
        
        JLabel descriptionLabel = new JBLabel("<html><body style='width: 600px'>" +
            "Generate comprehensive PDF reports based on your migration analysis. " +
            "Reports include dependency analysis, scan results, and migration recommendations. " +
            "This feature requires experimental features to be enabled." +
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
        
        // Create form layout
        FormBuilder formBuilder = new FormBuilder();
        formBuilder.addComponent(panel);
        formBuilder.addVerticalGap(10);
        formBuilder.addComponent(scrollPane);
        
        return formBuilder.getPanel();
    }
    
    private void setupEventHandlers() {
        generateDependencyReportButton.addActionListener(e -> generateDependencyReport());
        generateScanResultsReportButton.addActionListener(e -> generateScanResultsReport());
        generateComprehensiveReportButton.addActionListener(e -> generateComprehensiveReport());
    }
    
    private void generateDependencyReport() {
        try {
            statusLabel.setText("Generating dependency report...");
            outputArea.setText("Starting dependency report generation...\n");
            
            // Create sample dependency graph (in real implementation, this would come from actual analysis)
            DependencyGraph dependencyGraph = createSampleDependencyGraph();
            
            // Choose output location
            Path outputPath = chooseOutputLocation("dependency-report.pdf");
            if (outputPath == null) {
                statusLabel.setText("Report generation cancelled");
                return;
            }
            
            // Generate report
            Path result = pdfReportService.generateDependencyReport(dependencyGraph, outputPath);
            
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
            statusLabel.setText("Generating scan results report...");
            outputArea.setText("Starting scan results report generation...\n");
            
            // Create sample scan results (in real implementation, this would come from actual scans)
            ComprehensiveScanResults scanResults = createSampleScanResults();
            
            // Choose output location
            Path outputPath = chooseOutputLocation("scan-results-report.pdf");
            if (outputPath == null) {
                statusLabel.setText("Report generation cancelled");
                return;
            }
            
            // Generate report
            Path result = pdfReportService.generateScanResultsReport(scanResults, outputPath);
            
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
            statusLabel.setText("Generating comprehensive report...");
            outputArea.setText("Starting comprehensive report generation...\n");
            
            // Create sample data (in real implementation, this would come from actual analysis)
            DependencyGraph dependencyGraph = createSampleDependencyGraph();
            ComprehensiveScanResults scanResults = createSampleScanResults();
            
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
            customData.put("organization", "Sample Organization");
            
            // Generate report
            PdfReportService.GeneratePdfReportRequest request = new PdfReportService.GeneratePdfReportRequest(
                outputPath,
                dependencyGraph,
                null,
                scanResults,
                pdfReportService.getDefaultTemplate(),
                customData
            );
            
            Path result = pdfReportService.generateComprehensiveReport(request);
            
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
    
    // Sample data methods (in real implementation, these would get actual data)
    private DependencyGraph createSampleDependencyGraph() {
        // This would get actual dependency data from the project analysis
        DependencyGraph graph = new DependencyGraph();
        // Add sample dependencies...
        return graph;
    }
    
    private ComprehensiveScanResults createSampleScanResults() {
        // This would get actual scan results from the advanced scanning service
        ComprehensiveScanResults.ScanSummary summary = new ComprehensiveScanResults.ScanSummary(
            100, 10, 2, 5, 3, 85.5
        );
        
        return new ComprehensiveScanResults(
            project.getBasePath().toString(),
            java.time.LocalDateTime.now(),
            Map.of("jpa", "sample results"),
            Map.of("validation", "sample results"),
            Map.of("servlet", "sample results"),
            Map.of("thirdParty", "sample results"),
            Map.of("transitive", "sample results"),
            Map.of("build", "sample results"),
            java.util.List.of("Update dependencies", "Apply recipes"),
            10,
            summary
        );
    }
    
    public JPanel getPanel() {
        return mainPanel;
    }
    
    public void refresh() {
        statusLabel.setText("Ready to generate reports");
        outputArea.setText("");
    }
}
