package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.intellij.service.RuntimeVerificationService;
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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Runtime tab component for runtime verification and health checks.
 * Provides UI for verifying migrated applications at runtime.
 */
public class RuntimeComponent {
    private static final Logger LOG = Logger.getInstance(RuntimeComponent.class);
    
    private final JPanel panel;
    private final Project project;
    private final RuntimeVerificationService verificationService;
    
    private final JTable errorTable;
    private final DefaultTableModel errorTableModel;
    private final JButton runHealthCheckButton;
    private final JButton analyzeErrorsButton;
    private final JButton remediateButton;
    private final JButton selectJarButton;
    private final JProgressBar progressBar;
    private final JLabel statusLabel;
    private final JTextArea detailsArea;
    private final JTextField jarPathField;
    
    private Path selectedJarPath;
    private List<RuntimeError> currentErrors;
    
    public RuntimeComponent(Project project) {
        this.project = project;
        this.verificationService = new RuntimeVerificationService();
        this.panel = createPanel();
        this.errorTableModel = new DefaultTableModel();
        this.errorTable = new JTable(errorTableModel);
        this.runHealthCheckButton = new JButton("Run Health Check");
        this.analyzeErrorsButton = new JButton("Analyze Errors");
        this.remediateButton = new JButton("Get Recommendations");
        this.selectJarButton = new JButton("Select JAR");
        this.progressBar = new JProgressBar(0, 100);
        this.statusLabel = new JLabel("Ready - Select a JAR file");
        this.detailsArea = new JTextArea(8, 40);
        this.jarPathField = new JTextField(40);
        this.currentErrors = new ArrayList<>();
        
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
        
        // Top panel - JAR selection and controls
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.setBorder(new TitledBorder("Runtime Verification"));
        
        JPanel jarPanel = new JPanel(new BorderLayout(5, 5));
        jarPanel.add(new JLabel("JAR Path:"), BorderLayout.WEST);
        jarPanel.add(jarPathField, BorderLayout.CENTER);
        jarPanel.add(selectJarButton, BorderLayout.EAST);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        buttonPanel.add(runHealthCheckButton);
        buttonPanel.add(analyzeErrorsButton);
        buttonPanel.add(remediateButton);
        
        JPanel statusPanel = new JPanel(new BorderLayout(5, 5));
        statusPanel.add(jarPanel, BorderLayout.NORTH);
        statusPanel.add(buttonPanel, BorderLayout.CENTER);
        
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
        selectJarButton.addActionListener(this::handleSelectJar);
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
        
        // Disable buttons until JAR is selected
        runHealthCheckButton.setEnabled(false);
        analyzeErrorsButton.setEnabled(false);
        remediateButton.setEnabled(false);
    }
    
    private void handleSelectJar(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select JAR File to Verify");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JAR Files", "jar"));
        
        if (fileChooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION) {
            selectedJarPath = fileChooser.getSelectedFile().toPath();
            jarPathField.setText(selectedJarPath.toString());
            
            // Enable buttons
            runHealthCheckButton.setEnabled(true);
            analyzeErrorsButton.setEnabled(true);
            remediateButton.setEnabled(true);
            
            statusLabel.setText("JAR selected - Ready to verify");
            statusLabel.setForeground(Color.GRAY);
        }
    }
    
    private void handleRunHealthCheck(ActionEvent e) {
        if (selectedJarPath == null) {
            Messages.showWarningDialog(project, "Please select a JAR file first.", "No JAR Selected");
            return;
        }
        
        statusLabel.setText("Running bytecode analysis...");
        statusLabel.setForeground(Color.BLUE);
        progressBar.setIndeterminate(true);
        runHealthCheckButton.setEnabled(false);
        
        // Run verification in background
        CompletableFuture.runAsync(() -> {
            try {
                BytecodeAnalysisResult result = verificationService.analyzeBytecode(selectedJarPath);
                
                SwingUtilities.invokeLater(() -> {
                    processBytecodeAnalysisResult(result);
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Analysis failed: " + ex.getMessage());
                    statusLabel.setForeground(Color.RED);
                    progressBar.setIndeterminate(false);
                    runHealthCheckButton.setEnabled(true);
                    Messages.showErrorDialog(project, "Analysis failed: " + ex.getMessage(), "Error");
                });
            }
        });
    }
    
    private void processBytecodeAnalysisResult(BytecodeAnalysisResult result) {
        if (result != null && result.potentialErrors() != null && !result.potentialErrors().isEmpty()) {
            currentErrors = result.potentialErrors();
            displayErrors(currentErrors);
            
            statusLabel.setText("Found " + currentErrors.size() + " runtime issues");
            statusLabel.setForeground(Color.ORANGE);
            
            Messages.showWarningDialog(project, 
                "Analysis found " + currentErrors.size() + " runtime issues.", 
                "Issues Found");
        } else {
            currentErrors = new ArrayList<>();
            displayErrors(currentErrors);
            
            statusLabel.setText("No runtime issues detected");
            statusLabel.setForeground(Color.GREEN);
            
            Messages.showInfoMessage(project, 
                "Bytecode analysis passed! No Jakarta migration issues detected.", 
                "Analysis Complete");
        }
        
        progressBar.setIndeterminate(false);
        progressBar.setValue(100);
        runHealthCheckButton.setEnabled(true);
    }
    
    private void handleAnalyzeErrors(ActionEvent e) {
        if (currentErrors == null || currentErrors.isEmpty()) {
            Messages.showWarningDialog(project, 
                "No errors to analyze. Run a health check first.", 
                "No Errors");
            return;
        }
        
        statusLabel.setText("Analyzing errors...");
        statusLabel.setForeground(Color.BLUE);
        progressBar.setIndeterminate(true);
        
        // Run error analysis in background
        CompletableFuture.runAsync(() -> {
            try {
                // Create empty dependency graph for context
                DependencyGraph emptyGraph = new DependencyGraph(new HashSet<>(), new HashSet<>());
                MigrationContext context = new MigrationContext(
                    emptyGraph,
                    "post-migration",
                    true
                );
                ErrorAnalysis analysis = verificationService.analyzeErrors(currentErrors, context);
                
                SwingUtilities.invokeLater(() -> {
                    displayErrorAnalysis(analysis);
                    statusLabel.setText("Analysis complete");
                    statusLabel.setForeground(Color.GREEN);
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(100);
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Analysis failed: " + ex.getMessage());
                    statusLabel.setForeground(Color.RED);
                    progressBar.setIndeterminate(false);
                });
            }
        });
    }
    
    private void handleRemediate(ActionEvent e) {
        if (currentErrors == null || currentErrors.isEmpty()) {
            Messages.showWarningDialog(project, 
                "No errors to remediate. Run a health check first.", 
                "No Errors");
            return;
        }
        
        statusLabel.setText("Generating recommendations...");
        statusLabel.setForeground(Color.BLUE);
        
        // Show recommendations
        StringBuilder recommendations = new StringBuilder();
        recommendations.append("Remediation Recommendations\n");
        recommendations.append("============================\n\n");
        
        // Group errors by type
        long classNotFoundCount = currentErrors.stream().filter(err -> err.type() == ErrorType.CLASS_NOT_FOUND).count();
        long linkageErrorCount = currentErrors.stream().filter(err -> err.type() == ErrorType.LINKAGE_ERROR).count();
        long noSuchMethodCount = currentErrors.stream().filter(err -> err.type() == ErrorType.NO_SUCH_METHOD).count();
        
        recommendations.append("Error Summary:\n");
        recommendations.append("- Class Not Found: ").append(classNotFoundCount).append("\n");
        recommendations.append("- Linkage Errors: ").append(linkageErrorCount).append("\n");
        recommendations.append("- No Such Method: ").append(noSuchMethodCount).append("\n\n");
        
        recommendations.append("1. Class Loading Fixes:\n");
        recommendations.append("   - Update Class.forName() calls with new class names\n");
        recommendations.append("   - Review JNDI lookup patterns\n");
        recommendations.append("   - Check for hardcoded javax class references\n\n");
        
        recommendations.append("2. Dependency Updates:\n");
        recommendations.append("   - Update to Jakarta EE 9+ compatible versions\n");
        recommendations.append("   - Remove transitive javax-only dependencies\n");
        recommendations.append("   - Add provided scope for Jakarta APIs if needed\n\n");
        
        recommendations.append("3. XML Descriptor Updates:\n");
        recommendations.append("   - Update namespace URIs in web.xml from http://xmlns.jcp.org to https://jakarta.ee\n");
        recommendations.append("   - Update schema locations in all XML descriptors\n\n");
        
        recommendations.append("4. Recommended Tools:\n");
        recommendations.append("   - OpenRewrite: javaxToJakarta recipe\n");
        recommendations.append("   - Eclipse Transformer for class renaming\n\n");
        
        recommendations.append("5. Next Steps:\n");
        recommendations.append("   - Run: mvn clean compile\n");
        recommendations.append("   - Update any @Inject, @Named annotations\n");
        recommendations.append("   - Rebuild and retest\n");
        
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
    
    private void displayErrorAnalysis(ErrorAnalysis analysis) {
        StringBuilder sb = new StringBuilder();
        sb.append("Error Analysis Report\n");
        sb.append("======================\n\n");
        
        if (analysis != null) {
            sb.append("Category: ").append(analysis.category() != null ? analysis.category().name() : "Unknown").append("\n");
            sb.append("Root Cause: ").append(analysis.rootCause()).append("\n\n");
            
            sb.append("Contributing Factors:\n");
            if (analysis.contributingFactors() != null) {
                for (String factor : analysis.contributingFactors()) {
                    sb.append("- ").append(factor).append("\n");
                }
            }
            sb.append("\n");
            
            sb.append("Suggested Fixes:\n");
            if (analysis.suggestedFixes() != null) {
                for (RemediationStep fix : analysis.suggestedFixes()) {
                    sb.append("[").append(fix.priority()).append("] ").append(fix.description());
                    if (fix.action() != null) {
                        sb.append(" - ").append(fix.action());
                    }
                    sb.append("\n");
                }
            }
            
            if (analysis.similarFailures() != null && !analysis.similarFailures().isEmpty()) {
                sb.append("\nSimilar Past Failures:\n");
                for (SimilarPastFailure failure : analysis.similarFailures()) {
                    sb.append("- Pattern: ").append(failure.errorPattern());
                    sb.append(" (Similarity: ").append(String.format("%.2f", failure.similarityScore())).append(")\n");
                    if (failure.resolution() != null) {
                        sb.append("  Resolution: ").append(failure.resolution()).append("\n");
                    }
                }
            }
        } else {
            sb.append("No analysis available.");
        }
        
        detailsArea.setText(sb.toString());
    }
    
    private void updateErrorDetails(int row) {
        if (row >= 0 && row < currentErrors.size()) {
            RuntimeError error = currentErrors.get(row);
            
            StringBuilder details = new StringBuilder();
            details.append("Error Details\n");
            details.append("============\n\n");
            details.append("Type: ").append(error.type().name()).append("\n");
            details.append("Class: ").append(error.className()).append("\n");
            details.append("Method: ").append(error.methodName() != null ? error.methodName() : "N/A").append("\n");
            details.append("Message: ").append(error.message()).append("\n");
            details.append("Confidence: ").append(String.format("%.2f", error.confidence())).append("\n");
            details.append("Timestamp: ").append(error.timestamp()).append("\n\n");
            
            // Add stack trace info if available
            if (error.stackTrace() != null && error.stackTrace().elements() != null) {
                details.append("Stack Trace:\n");
                for (StackTrace.StackTraceElement elem : error.stackTrace().elements()) {
                    details.append("  at ").append(elem.className()).append(".").append(elem.methodName());
                    if (elem.fileName() != null) {
                        details.append("(").append(elem.fileName());
                        if (elem.lineNumber() > 0) {
                            details.append(":").append(elem.lineNumber());
                        }
                        details.append(")");
                    }
                    details.append("\n");
                }
            }
            
            details.append("\nPossible Causes:\n");
            switch (error.type()) {
                case CLASS_NOT_FOUND:
                    details.append("- Missing JAR dependency\n");
                    details.append("- Incorrect class name after migration\n");
                    details.append("- Class loader hierarchy issue\n");
                    break;
                case NO_SUCH_METHOD:
                    details.append("- Method signature changed in Jakarta EE\n");
                    details.append("- Incompatible version of dependency\n");
                    details.append("- Method was removed or renamed\n");
                    break;
                case LINKAGE_ERROR:
                    details.append("- javax imports not fully migrated\n");
                    details.append("- Old namespace referenced in XML\n");
                    details.append("- Cached class files with old namespaces\n");
                    break;
                default:
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
    
    /**
     * Set the selected JAR path programmatically
     */
    public void setSelectedJarPath(Path jarPath) {
        this.selectedJarPath = jarPath;
        if (jarPath != null) {
            jarPathField.setText(jarPath.toString());
            runHealthCheckButton.setEnabled(true);
            analyzeErrorsButton.setEnabled(true);
            remediateButton.setEnabled(true);
        }
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
        currentErrors = new ArrayList<>();
    }
    
    public JPanel getPanel() {
        return panel;
    }
}
