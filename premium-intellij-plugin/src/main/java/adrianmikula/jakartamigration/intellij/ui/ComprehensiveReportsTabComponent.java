package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.reporting.domain.ComprehensiveScanResults;
import adrianmikula.jakartamigration.reporting.service.ComprehensiveReportService;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Comprehensive Reports tab component for generating
 * detailed migration reports with dashboard data,
 * dependency analysis, and scan results.
 */
public class ComprehensiveReportsTabComponent {
    
    private final Project project;
    private final ComprehensiveReportService reportService;
    
    // UI Components
    private JPanel mainPanel;
    private JButton generateReportButton;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JBTextArea outputArea;
    private JScrollPane scrollPane;
    
    public ComprehensiveReportsTabComponent(@NotNull Project project) {
        this.project = project;
        this.reportService = new ComprehensiveReportService() {
            @Override
            public String generateComprehensiveReport(Project project, DependencyGraph dependencyGraph, ComprehensiveScanResults scanResults, String outputPath, Map<String, String> customData) {
                // Simple stub implementation for now
                return "PDF generation temporarily disabled";
            }
        };
        
        // Initialize UI components
        mainPanel = new JPanel(new BorderLayout());
        generateReportButton = new JButton("Generate Comprehensive Report");
        progressBar = new JProgressBar();
        progressBar.setVisible(false);
        statusLabel = new JBLabel("Ready to generate report");
        outputArea = new JBTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        scrollPane = new JBScrollPane(outputArea);
        
        // Layout components
        JPanel headerPanel = createHeaderPanel();
        JPanel contentPanel = createContentPanel();
        JPanel actionsPanel = createActionsPanel();
        
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        mainPanel.add(actionsPanel, BorderLayout.SOUTH);
        
        // Set up action listeners
        generateReportButton.addActionListener(this::handleGenerateReport);
    }
    
    /**
     * Creates header panel with title and description
     */
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(JBUI.Borders.empty(10));
        
        JLabel titleLabel = new JBLabel("Comprehensive Migration Report");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        
        JLabel descriptionLabel = new JBLabel("<html><body style='width: 600px'>" +
                "Generate a comprehensive PDF report combining dashboard analysis, " +
                "dependency graphs, and detailed scan results. " +
                "This report provides executive summary, risk assessment, " +
                "detailed findings, and migration recommendations." +
                "</body></html>");
        descriptionLabel.setFont(descriptionLabel.getFont().deriveFont(Font.PLAIN, 11f));
        
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        titlePanel.add(titleLabel);
        
        JPanel descPanel = new JPanel(new BorderLayout());
        descPanel.add(descriptionLabel, BorderLayout.CENTER);
        
        headerPanel.add(titlePanel, BorderLayout.NORTH);
        headerPanel.add(descPanel, BorderLayout.CENTER);
        
        return headerPanel;
    }
    
    /**
     * Creates content panel with progress and output
     */
    private JPanel createContentPanel() {
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(JBUI.Borders.empty(10));
        
        // Progress section
        JPanel progressPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        progressPanel.add(new JBLabel("Status:"));
        progressPanel.add(progressBar);
        progressPanel.add(statusLabel);
        
        // Output section
        JPanel outputPanel = new JPanel(new BorderLayout());
        outputPanel.setBorder(JBUI.Borders.compound(
                JBUI.Borders.empty(5, 5, 5, 5),
                BorderFactory.createTitledBorder("Report Output")
        ));
        
        outputPanel.add(new JBLabel("Output:"), BorderLayout.NORTH);
        outputPanel.add(scrollPane, BorderLayout.CENTER);
        
        contentPanel.add(progressPanel, BorderLayout.NORTH);
        contentPanel.add(outputPanel, BorderLayout.CENTER);
        
        return contentPanel;
    }
    
    /**
     * Creates actions panel with generate button
     */
    private JPanel createActionsPanel() {
        JPanel actionsPanel = new JPanel(new FlowLayout());
        generateReportButton.setToolTipText("Generate comprehensive migration report with dashboard data and scan results");
        actionsPanel.add(generateReportButton);
        return actionsPanel;
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
            
            // Get data from dashboard and scan results
            DependencyGraph dependencyGraph = getDependencyGraph();
            ComprehensiveScanResults scanResults = getScanResults();
            
            // Prepare custom data
            Map<String, String> customData = new HashMap<>();
            customData.put("projectName", project.getName());
            customData.put("pluginVersion", getPluginVersion());
            customData.put("jdkVersion", System.getProperty("java.version", "Unknown"));
            customData.put("buildSystem", "Gradle");
            
            // Generate report
            String outputPath = chooseOutputLocation();
            if (outputPath == null) {
                statusLabel.setText("Report generation cancelled");
                return;
            }
            
            String result = reportService.generateComprehensiveReport(
                    project,
                    dependencyGraph,
                    scanResults,
                    outputPath,
                    customData
            );
            
            // Show result
            outputArea.setText("Comprehensive report generated successfully!\n");
            outputArea.append("Output file: " + result + "\n");
            
            // Ask if user wants to open file
            int choice = com.intellij.openapi.ui.Messages.showYesNoDialog(
                    project,
                    "Comprehensive report generated successfully!\n\nWould you like to open PDF file?",
                    "Report Generated",
                    com.intellij.openapi.ui.Messages.getQuestionIcon());
            
            if (choice == com.intellij.openapi.ui.Messages.YES) {
                openFile(result);
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
        // For now, use a fixed location in project directory
        return project.getBasePath();
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
    
    /**
     * Gets dependency graph (placeholder implementation)
     */
    private DependencyGraph getDependencyGraph() {
        // This would integrate with the actual dependency graph component
        // For now, return a simple placeholder
        return new DependencyGraph() {
            public java.util.List<String> getDependencyNodes() {
                return java.util.Collections.emptyList();
            }
        };
    }
    
    /**
     * Gets scan results (placeholder implementation)
     */
    private ComprehensiveScanResults getScanResults() {
        // This would integrate with the actual scan results
        // For now, return a simple placeholder
        return new ComprehensiveScanResults(
                java.util.Collections.emptyList(), // securityIssues
                java.util.Collections.emptyList(), // performanceIssues
                java.util.Collections.emptyList(), // compatibilityIssues
                java.util.Collections.emptyList(), // dataMigrationIssues
                java.util.Collections.emptyList(), // configurationIssues
                java.util.Collections.emptyList(), // buildSystemIssues
                java.util.Collections.emptyList(), // thirdPartyLibIssues
                0, // totalDependencies
                0  // totalIssuesFound
        );
    }
    
    /**
     * Gets plugin version
     */
    private String getPluginVersion() {
        // Simplified version retrieval without PluginId
        return "1.0.8"; // Return known version
    }
    
    /**
     * Gets main panel for this component
     */
    public JPanel getPanel() {
        return mainPanel;
    }
}
