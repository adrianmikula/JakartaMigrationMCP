package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.runtimeverification.domain.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Runtime tab component for runtime verification and health checks.
 * Provides UI for verifying migrated applications at runtime.
 */
public class RuntimeComponent {
    private static final Logger LOG = Logger.getInstance(RuntimeComponent.class);
    
    private final JPanel panel;
    private final Project project;
    private final JTable errorTable;
    private final DefaultTableModel errorTableModel;
    private final JButton runHealthCheckButton;
    private final JButton analyzeErrorsButton;
    private final JButton remediateButton;
    private final JProgressBar progressBar;
    private final JLabel statusLabel;
    private final JTextArea detailsArea;
    
    public RuntimeComponent(Project project) {
        this.project = project;
        this.panel = createPanel();
        this.errorTableModel = new DefaultTableModel();
        this.errorTable = new JTable(errorTableModel);
        this.runHealthCheckButton = new JButton("Run Health Check");
        this.analyzeErrorsButton = new JButton("Analyze Errors");
        this.remediateButton = new JButton("Get Recommendations");
        this.progressBar = new JProgressBar(0, 100);
        this.statusLabel = new JLabel("Ready");
        this.detailsArea = new JTextArea(8, 40);
        
        setupTableColumns();
        setupEventHandlers();
    }
    
    private void setupTableColumns() {
        errorTableModel.addColumn("Error Type");
        errorTableModel.addColumn("Class/Location");
        errorTableModel.addColumn("Message");
        errorTableModel.addColumn("Confidence");
    }
    
    private JPanel createPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Top panel - Controls and progress
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.setBorder(new TitledBorder("Runtime Verification"));
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        buttonPanel.add(runHealthCheckButton);
        buttonPanel.add(analyzeErrorsButton);
        buttonPanel.add(remediateButton);
        
        JPanel statusPanel = new JPanel(new BorderLayout(5, 5));
        statusPanel.add(buttonPanel, BorderLayout.WEST);
        
        JPanel progressPanel = new JPanel(new BorderLayout(5, 5));
        progressPanel.add(new JLabel("Progress:"), BorderLayout.WEST);
        progressPanel.add(progressBar, BorderLayout.CENTER);
        progressPanel.add(statusLabel, BorderLayout.EAST);
        statusPanel.add(progressPanel, BorderLayout.SOUTH);
        
        topPanel.add(statusPanel, BorderLayout.CENTER);
        
        // Center panel - Error table and details
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        
        // Error table
        JScrollPane tableScrollPane = new JScrollPane(errorTable);
        tableScrollPane.setBorder(new TitledBorder("Detected Runtime Errors"));
        splitPane.setTopComponent(tableScrollPane);
        
        // Details area
        detailsArea.setEditable(false);
        detailsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        JScrollPane detailsScrollPane = new JScrollPane(detailsArea);
        detailsScrollPane.setBorder(new TitledBorder("Error Details"));
        splitPane.setBottomComponent(detailsScrollPane);
        splitPane.setResizeWeight(0.6);
        
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(splitPane, BorderLayout.CENTER);
        
        return mainPanel;
    }
    
    private void setupEventHandlers() {
        runHealthCheckButton.addActionListener(this::handleRunHealthCheck);
        analyzeErrorsButton.addActionListener(this::handleAnalyzeErrors);
        remediateButton.addActionListener(this::handleRemediate);
        
        errorTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = errorTable.getSelectedRow();
                if (selectedRow >= 0) {
                    updateErrorDetails(selectedRow);
                }
            }
        });
    }
    
    private void handleRunHealthCheck(ActionEvent e) {
        statusLabel.setText("Running health check...");
        statusLabel.setForeground(Color.BLUE);
        progressBar.setIndeterminate(true);
        runHealthCheckButton.setEnabled(false);
        
        // Simulate health check execution
        SwingUtilities.invokeLater(() -> {
            try {
                Thread.sleep(1500);
                
                // Generate sample errors
                List<RuntimeError> errors = generateSampleErrors();
                displayErrors(errors);
                
                statusLabel.setText("Health check complete");
                statusLabel.setForeground(errors.isEmpty() ? Color.GREEN : Color.ORANGE);
                progressBar.setIndeterminate(false);
                progressBar.setValue(100);
                runHealthCheckButton.setEnabled(true);
                
                if (errors.isEmpty()) {
                    Messages.showInfoMessage(project, "Health check passed! No runtime errors detected.", "Health Check");
                } else {
                    Messages.showWarningDialog(project, 
                        "Health check found " + errors.size() + " runtime issues.", 
                        "Issues Found");
                }
            } catch (Exception ex) {
                statusLabel.setText("Health check failed: " + ex.getMessage());
                statusLabel.setForeground(Color.RED);
                progressBar.setIndeterminate(false);
                runHealthCheckButton.setEnabled(true);
                Messages.showErrorDialog(project, "Health check failed: " + ex.getMessage(), "Error");
            }
        });
    }
    
    private void handleAnalyzeErrors(ActionEvent e) {
        int rowCount = errorTableModel.getRowCount();
        if (rowCount == 0) {
            Messages.showWarningDialog(project, "No errors to analyze. Run a health check first.", "No Errors");
            return;
        }
        
        statusLabel.setText("Analyzing errors...");
        statusLabel.setForeground(Color.BLUE);
        progressBar.setIndeterminate(true);
        
        // Simulate error analysis
        SwingUtilities.invokeLater(() -> {
            try {
                Thread.sleep(1000);
                
                // Update details with analysis
                StringBuilder analysis = new StringBuilder();
                analysis.append("Error Analysis Report\n");
                analysis.append("=====================\n\n");
                analysis.append("Total Errors: ").append(rowCount).append("\n");
                analysis.append("Class Not Found: ").append(countErrorsByType(ErrorType.CLASS_NOT_FOUND)).append("\n");
                analysis.append("Linkage Errors: ").append(countErrorsByType(ErrorType.LINKAGE_ERROR)).append("\n");
                analysis.append("Other: ").append(countErrorsByType(ErrorType.OTHER)).append("\n\n");
                analysis.append("Common Issues:\n");
                analysis.append("- Class loading failures due to javax/jakarta namespace conflicts\n");
                analysis.append("- Missing service provider configurations\n");
                analysis.append("- Descriptor file inconsistencies\n\n");
                analysis.append("Recommended Actions:\n");
                analysis.append("1. Update all javax imports to jakarta\n");
                analysis.append("2. Check service provider configurations\n");
                analysis.append("3. Verify XML descriptors are correctly configured\n");
                
                detailsArea.setText(analysis.toString());
                
                statusLabel.setText("Analysis complete");
                statusLabel.setForeground(Color.GREEN);
                progressBar.setIndeterminate(false);
                progressBar.setValue(100);
                
                Messages.showInfoMessage(project, "Error analysis complete!", "Analysis");
            } catch (Exception ex) {
                statusLabel.setText("Analysis failed: " + ex.getMessage());
                statusLabel.setForeground(Color.RED);
                progressBar.setIndeterminate(false);
                Messages.showErrorDialog(project, "Analysis failed: " + ex.getMessage(), "Error");
            }
        });
    }
    
    private void handleRemediate(ActionEvent e) {
        int rowCount = errorTableModel.getRowCount();
        if (rowCount == 0) {
            Messages.showWarningDialog(project, "No errors to remediate. Run a health check first.", "No Errors");
            return;
        }
        
        statusLabel.setText("Generating recommendations...");
        statusLabel.setForeground(Color.BLUE);
        
        // Show recommendations
        StringBuilder recommendations = new StringBuilder();
        recommendations.append("Remediation Recommendations\n");
        recommendations.append("============================\n\n");
        recommendations.append("1. Class Loading Fixes:\n");
        recommendations.append("   - Update Class.forName() calls with new class names\n");
        recommendations.append("   - Review JNDI lookup patterns\n");
        recommendations.append("   - Class loader hierarchy issue\n\n");
        recommendations.append("2. XML Descriptor Updates:\n");
        recommendations.append("   - Update namespace URIs in web.xml\n");
        recommendations.append("   - Update schema locations\n\n");
        recommendations.append("3. Service Provider Configuration:\n");
        recommendations.append("   - Update META-INF/services files\n");
        recommendations.append("   - Update SPI declarations\n\n");
        recommendations.append("4. Recommended Tool:\n");
        recommendations.append("   - Use OpenRewrite recipe: `javaxToJakarta`\n");
        recommendations.append("   - This will automatically apply most fixes\n");
        
        detailsArea.setText(recommendations.toString());
        statusLabel.setText("Recommendations generated");
        statusLabel.setForeground(Color.GREEN);
        
        int result = Messages.showYesNoDialog(
            project,
            "Would you like to apply automated fixes using OpenRewrite?",
            "Apply Fixes",
            Messages.getQuestionIcon()
        );
        
        if (result == Messages.YES) {
            Messages.showInfoMessage(project, 
                "OpenRewrite refactoring will be applied.\n" +
                "Check the Refactor tab for progress.", 
                "Fixes Applied");
        }
    }
    
    private void updateErrorDetails(int row) {
        String errorType = (String) errorTableModel.getValueAt(row, 0);
        String className = (String) errorTableModel.getValueAt(row, 1);
        String message = (String) errorTableModel.getValueAt(row, 2);
        double confidence = (Double) errorTableModel.getValueAt(row, 3);
        
        StringBuilder details = new StringBuilder();
        details.append("Error Details\n");
        details.append("============\n\n");
        details.append("Type: ").append(errorType).append("\n");
        details.append("Class: ").append(className).append("\n");
        details.append("Message: ").append(message).append("\n");
        details.append("Confidence: ").append(String.format("%.2f", confidence)).append("\n\n");
        details.append("Possible Causes:\n");
        
        if (errorType.contains("CLASS_NOT_FOUND")) {
            details.append("- Missing JAR dependency\n");
            details.append("- Incorrect class name after migration\n");
            details.append("- Class loader hierarchy issue\n");
        } else if (errorType.contains("NO_SUCH_METHOD")) {
            details.append("- Method signature changed in Jakarta EE\n");
            details.append("- Incompatible version of dependency\n");
            details.append("- Method was removed or renamed\n");
        } else if (errorType.contains("LINKAGE_ERROR")) {
            details.append("- javax imports not fully migrated\n");
            details.append("- Old namespace referenced in XML\n");
            details.append("- Cached class files with old namespaces\n");
        } else {
            details.append("- Runtime configuration issue\n");
            details.append("- Dependency conflict\n");
        }
        
        details.append("\nSuggested Fix:\n");
        details.append("1. Verify all javax imports are updated to jakarta\n");
        details.append("2. Clean and rebuild the project\n");
        details.append("3. Restart application server\n");
        details.append("4. Check server logs for additional context\n");
        
        detailsArea.setText(details.toString());
    }
    
    private void displayErrors(List<RuntimeError> errors) {
        // Clear existing data
        while (errorTableModel.getRowCount() > 0) {
            errorTableModel.removeRow(0);
        }
        
        // Add errors to table
        for (RuntimeError error : errors) {
            Object[] row = new Object[4];
            row[0] = error.type().name();
            row[1] = error.className();
            row[2] = error.message();
            row[3] = error.confidence();
            errorTableModel.addRow(row);
        }
    }
    
    private List<RuntimeError> generateSampleErrors() {
        List<RuntimeError> errors = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        // Create empty stack trace
        StackTrace emptyStackTrace = new StackTrace(
            "RuntimeException",
            "No stack trace",
            Collections.emptyList()
        );
        
        // Generate sample errors using record constructors
        RuntimeError error1 = new RuntimeError(
            ErrorType.CLASS_NOT_FOUND,
            "Class javax.servlet.ServletConfig not found",
            emptyStackTrace,
            "com.example.MyServlet",
            "init",
            now,
            0.95
        );
        errors.add(error1);
        
        RuntimeError error2 = new RuntimeError(
            ErrorType.LINKAGE_ERROR,
            "Old namespace http://xmlns.jcp.org/ns/javaee found",
            emptyStackTrace,
            "WEB-INF/web.xml",
            null,
            now,
            0.88
        );
        errors.add(error2);
        
        RuntimeError error3 = new RuntimeError(
            ErrorType.NO_SUCH_METHOD,
            "Method getMessage() not found in jakarta.ejb.SessionBean",
            emptyStackTrace,
            "com.example.EJBBean",
            "getMessage",
            now,
            0.92
        );
        errors.add(error3);
        
        return errors;
    }
    
    private int countErrorsByType(ErrorType type) {
        int count = 0;
        for (int i = 0; i < errorTableModel.getRowCount(); i++) {
            String typeName = (String) errorTableModel.getValueAt(i, 0);
            if (typeName.contains(type.name())) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Clear all results
     */
    public void clearResults() {
        while (errorTableModel.getRowCount() > 0) {
            errorTableModel.removeRow(0);
        }
        detailsArea.setText("");
        statusLabel.setText("Ready");
        statusLabel.setForeground(Color.GRAY);
        progressBar.setValue(0);
    }
    
    public JPanel getPanel() {
        return panel;
    }
}
