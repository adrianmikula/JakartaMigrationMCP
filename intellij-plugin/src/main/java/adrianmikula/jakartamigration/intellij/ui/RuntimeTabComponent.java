package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.runtimeverification.domain.ErrorAnalysis;
import adrianmikula.jakartamigration.runtimeverification.domain.MigrationContext;
import adrianmikula.jakartamigration.runtimeverification.domain.RuntimeError;
import adrianmikula.jakartamigration.runtimeverification.service.ErrorAnalyzer;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.JBOptionPane;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Runtime tab component for analyzing stack traces and runtime errors.
 * This is a community feature that helps diagnose Jakarta migration issues.
 */
public class RuntimeTabComponent {
    private static final Logger LOG = Logger.getLogger(RuntimeTabComponent.class);
    
    private final JPanel contentPanel;
    private final JTextArea stackTraceInput;
    private final JButton analyzeButton;
    private final JButton clearButton;
    private final JPanel resultsPanel;
    private final JLabel categoryLabel;
    private final JLabel rootCauseLabel;
    private final DefaultListModel<String> factorsListModel;
    private final JList<String> factorsList;
    private final DefaultListModel<String> fixesListModel;
    private final JList<String> fixesList;
    private final Project project;
    
    public RuntimeTabComponent(Project project) {
        this.project = project;
        this.contentPanel = new JPanel(new BorderLayout());
        this.stackTraceInput = new JTextArea(12, 60);
        this.analyzeButton = new JButton("Analyze Stack Trace");
        this.clearButton = new JButton("Clear");
        this.resultsPanel = new JPanel(new BorderLayout());
        this.categoryLabel = new JLabel("Category: -");
        this.rootCauseLabel = new JLabel("Root Cause: -");
        this.factorsListModel = new DefaultListModel<>();
        this.factorsList = new JList<>(factorsListModel);
        this.fixesListModel = new DefaultListModel<>();
        this.fixesList = new JList<>(fixesListModel);
        
        initializeUI();
    }
    
    private void initializeUI() {
        // Input section
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JLabel inputLabel = new JLabel("Paste Stack Trace:");
        inputLabel.setFont(inputLabel.getFont().deriveFont(Font.BOLD, 13f));
        inputPanel.add(inputLabel, BorderLayout.NORTH);
        
        stackTraceInput.setFont(new Font("Monospaced", Font.PLAIN, 11));
        stackTraceInput.setLineWrap(false);
        JBScrollPane scrollPane = new JBScrollPane(stackTraceInput);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        inputPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        analyzeButton.addActionListener(e -> analyzeStackTrace());
        buttonPanel.add(analyzeButton);
        clearButton.addActionListener(e -> clearInput());
        buttonPanel.add(clearButton);
        inputPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Results section
        resultsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        resultsPanel.setVisible(false);
        
        // Summary panel
        JPanel summaryPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        summaryPanel.setBorder(BorderFactory.createTitledBorder("Analysis Summary"));
        
        categoryLabel.setFont(categoryLabel.getFont().deriveFont(Font.BOLD));
        rootCauseLabel.setFont(rootCauseLabel.getFont().deriveFont(Font.PLAIN, 12f));
        
        summaryPanel.add(categoryLabel);
        summaryPanel.add(rootCauseLabel);
        
        // Factors panel
        JPanel factorsPanel = new JPanel(new BorderLayout());
        factorsPanel.setBorder(BorderFactory.createTitledBorder("Contributing Factors"));
        JBScrollPane factorsScrollPane = new JBScrollPane(factorsList);
        factorsPanel.add(factorsScrollPane, BorderLayout.CENTER);
        
        // Fixes panel
        JPanel fixesPanel = new JPanel(new BorderLayout());
        fixesPanel.setBorder(BorderFactory.createTitledBorder("Suggested Fixes"));
        JBScrollPane fixesScrollPane = new JBScrollPane(fixesList);
        fixesPanel.add(fixesScrollPane, BorderLayout.CENTER);
        
        // Bottom panel with factors and fixes
        JPanel bottomPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        bottomPanel.add(factorsPanel);
        bottomPanel.add(fixesPanel);
        
        resultsPanel.add(summaryPanel, BorderLayout.NORTH);
        resultsPanel.add(bottomPanel, BorderLayout.CENTER);
        
        // Premium indicator (for advanced fixes)
        JPanel premiumPanel = createPremiumIndicator();
        
        // Main layout
        contentPanel.add(inputPanel, BorderLayout.NORTH);
        contentPanel.add(resultsPanel, BorderLayout.CENTER);
        contentPanel.add(premiumPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createPremiumIndicator() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));
        panel.setBackground(new Color(240, 240, 240));
        
        JLabel iconLabel = new JLabel("🔒");
        JLabel textLabel = new JLabel("Auto-fix requires Premium");
        textLabel.setFont(textLabel.getFont().deriveFont(Font.PLAIN, 11f));
        
        panel.add(iconLabel);
        panel.add(Box.createHorizontalStrut(5));
        panel.add(textLabel);
        
        return panel;
    }
    
    /**
     * Analyze the stack trace and display results.
     */
    private void analyzeStackTrace() {
        String stackTrace = stackTraceInput.getText().trim();
        
        if (stackTrace.isEmpty()) {
            Messages.showWarningDialog(contentPanel,
                "Please paste a stack trace to analyze",
                "Empty Input");
            return;
        }
        
        try {
            // Parse stack trace
            List<String> lines = List.of(stackTrace.split("\\r?\\n"));
            ErrorAnalyzer errorAnalyzer = new ErrorAnalyzer();
            List<RuntimeError> errors = errorAnalyzer.parseErrorsFromOutput(lines, List.of());
            
            if (errors.isEmpty()) {
                Messages.showInfoMessage(contentPanel,
                    "No errors found in the provided stack trace. Make sure to paste the full stack trace.",
                    "No Errors");
                return;
            }
            
            // Analyze errors
            Path projectPath = getProjectPath();
            MigrationContext context = new MigrationContext(
                projectPath != null ? projectPath : Paths.get("."),
                false, // isPostMigration - assume pre-migration by default
                ""
            );
            
            ErrorAnalysis analysis = errorAnalyzer.analyzeErrors(errors, context);
            
            // Display results
            displayAnalysisResults(analysis, errors);
            
        } catch (Exception e) {
            LOG.error("Failed to analyze stack trace", e);
            Messages.showErrorDialog(contentPanel,
                "Analysis failed: " + e.getMessage(),
                "Error");
        }
    }
    
    private void displayAnalysisResults(ErrorAnalysis analysis, List<RuntimeError> errors) {
        resultsPanel.setVisible(true);
        
        categoryLabel.setText("Category: " + analysis.category());
        rootCauseLabel.setText("Root Cause: " + analysis.rootCause());
        
        // Clear and populate factors
        factorsListModel.clear();
        if (analysis.contributingFactors().isEmpty()) {
            factorsListModel.addElement("No contributing factors identified");
        } else {
            for (String factor : analysis.contributingFactors()) {
                factorsListModel.addElement("• " + factor);
            }
        }
        
        // Clear and populate fixes
        fixesListModel.clear();
        if (analysis.suggestedFixes().isEmpty()) {
            fixesListModel.addElement("No fixes suggested");
        } else {
            for (var fix : analysis.suggestedFixes()) {
                String fixText = String.format("[P%d] %s", fix.priority(), fix.description());
                fixesListModel.addElement(fixText);
            }
        }
        
        // Refresh UI
        contentPanel.revalidate();
        contentPanel.repaint();
    }
    
    private void clearInput() {
        stackTraceInput.setText("");
        resultsPanel.setVisible(false);
        factorsListModel.clear();
        fixesListModel.clear();
    }
    
    private Path getProjectPath() {
        String basePath = project.getBasePath();
        if (basePath != null) {
            return Paths.get(basePath);
        }
        return null;
    }
    
    public JPanel getPanel() {
        return contentPanel;
    }
}
