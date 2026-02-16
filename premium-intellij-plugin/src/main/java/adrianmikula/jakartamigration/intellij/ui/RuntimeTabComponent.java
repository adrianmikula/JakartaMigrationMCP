package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.runtimeverification.domain.ErrorAnalysis;
import adrianmikula.jakartamigration.runtimeverification.domain.ErrorCategory;
import adrianmikula.jakartamigration.runtimeverification.domain.MigrationContext;
import adrianmikula.jakartamigration.runtimeverification.domain.RemediationStep;
import adrianmikula.jakartamigration.runtimeverification.domain.RuntimeError;
import adrianmikula.jakartamigration.runtimeverification.domain.SimilarPastFailure;
import adrianmikula.jakartamigration.runtimeverification.service.ErrorAnalyzer;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Runtime Tab Component - Diagnose runtime errors with AI-powered analysis.
 */
public class RuntimeTabComponent {
    private static final Logger LOG = Logger.getInstance(RuntimeTabComponent.class);
    
    private final Project project;
    private final ErrorAnalyzer errorAnalyzer;
    private final JPanel panel;
    private final JTextArea errorInputArea;
    private final JTextArea outputArea;
    private final JButton diagnoseButton;
    private final JLabel statusLabel;
    
    public RuntimeTabComponent(Project project) {
        this.project = project;
        this.errorAnalyzer = new ErrorAnalyzer();
        this.panel = createPanel();
        this.errorInputArea = createErrorInputArea();
        this.outputArea = createOutputArea();
        this.diagnoseButton = createDiagnoseButton();
        this.statusLabel = createStatusLabel();
    }
    
    private JPanel createPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Title
        JLabel titleLabel = new JLabel("Runtime Error Diagnosis");
        titleLabel.setFont(new Font(titleLabel.getFont().getName(), Font.BOLD, 16));
        
        // Description
        JLabel descLabel = new JLabel("Paste runtime errors or stack traces below to get diagnosis and recommendations.");
        descLabel.setForeground(Color.GRAY);
        
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.add(titleLabel);
        titlePanel.add(Box.createVerticalStrut(5));
        titlePanel.add(descLabel);
        
        // Input section
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBorder(BorderFactory.createTitledBorder("Error Output / Stack Trace"));
        inputPanel.add(new JScrollPane(errorInputArea), BorderLayout.CENTER);
        
        // Output section
        JPanel outputPanel = new JPanel(new BorderLayout());
        outputPanel.setBorder(BorderFactory.createTitledBorder("Diagnosis Results"));
        outputPanel.add(new JScrollPane(outputArea), BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(diagnoseButton);
        buttonPanel.add(statusLabel);
        
        // Info text
        JLabel infoLabel = new JLabel("💡 Tip: Paste the full error output including stack traces for best results.");
        infoLabel.setForeground(new Color(100, 100, 100));
        infoLabel.setFont(infoLabel.getFont().deriveFont(Font.ITALIC));
        
        // Split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setTopComponent(inputPanel);
        splitPane.setBottomComponent(outputPanel);
        splitPane.setDividerLocation(200);
        
        mainPanel.add(titlePanel, BorderLayout.NORTH);
        mainPanel.add(splitPane, BorderLayout.CENTER);
        
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.add(buttonPanel);
        bottomPanel.add(infoLabel);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        return mainPanel;
    }
    
    private JTextArea createErrorInputArea() {
        JTextArea area = new JTextArea(10, 50);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setText("Paste your runtime error/stack trace here...\n\nExample:\njava.lang.NoClassDefFoundError: javax/servlet/Servlet\n\tat com.example.MyServlet.doGet(MyServlet.java:25)\n\tat javax.servlet.http.HttpServlet.service(HttpServlet.java:634)");
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        return area;
    }
    
    private JTextArea createOutputArea() {
        JTextArea area = new JTextArea(15, 50);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setEditable(false);
        area.setBackground(new Color(245, 245, 245));
        area.setText("Diagnosis results will appear here...\n\n" +
            "The analysis will include:\n" +
            "• Error category (NAMESPACE_MIGRATION, CLASSPATH_ISSUE, etc.)\n" +
            "• Root cause explanation\n" +
            "• Contributing factors\n" +
            "• Confidence score\n" +
            "• Recommended remediation steps");
        return area;
    }
    
    private JButton createDiagnoseButton() {
        JButton button = new JButton("🔍 Diagnose Errors");
        button.setFont(button.getFont().deriveFont(Font.BOLD));
        button.addActionListener(this::handleDiagnose);
        return button;
    }
    
    private JLabel createStatusLabel() {
        JLabel label = new JLabel("Ready");
        label.setForeground(Color.GRAY);
        return label;
    }
    
    private void handleDiagnose(ActionEvent e) {
        String errorText = errorInputArea.getText();
        
        if (errorText == null || errorText.trim().isEmpty()) {
            Messages.showWarningDialog(project, "Please paste error output first.", "No Input");
            return;
        }
        
        statusLabel.setText("Analyzing...");
        diagnoseButton.setEnabled(false);
        
        try {
            // Parse input - treat all as stderr for now
            List<String> lines = List.of(errorText.split("\n"));
            List<String> stderr = new ArrayList<>(lines);
            List<String> stdout = new ArrayList<>();
            
            // Parse errors from input
            List<RuntimeError> errors = errorAnalyzer.parseErrorsFromOutput(stderr, stdout);
            
            if (errors.isEmpty()) {
                outputArea.setText("No recognizable errors found in the input.\n\n" +
                    "Please paste a valid error message or stack trace.\n" +
                    "Example:\n" +
                    "java.lang.NoClassDefFoundError: javax/servlet/Servlet\n" +
                    "\tat com.example.MyServlet.doGet(MyServlet.java:25)");
                statusLabel.setText("No errors found");
                return;
            }
            
            // Create migration context
            MigrationContext context = new MigrationContext(
                new DependencyGraph(),
                "POST_MIGRATION",
                true
            );
            
            // Analyze errors
            ErrorAnalysis analysis = errorAnalyzer.analyzeErrors(errors, context);
            
            // Display results
            displayResults(analysis);
            
            statusLabel.setText("Analysis complete");
            
        } catch (Exception ex) {
            LOG.error("Error during diagnosis", ex);
            outputArea.setText("Error during analysis: " + ex.getMessage());
            statusLabel.setText("Error");
        } finally {
            diagnoseButton.setEnabled(true);
        }
    }
    
    private void displayResults(ErrorAnalysis analysis) {
        StringBuilder sb = new StringBuilder();
        
        // Error Category
        sb.append("═══════════════════════════════════════════════════════════\n");
        sb.append("DIAGNOSIS RESULTS\n");
        sb.append("═══════════════════════════════════════════════════════════\n\n");
        
        // Category with icon
        String categoryIcon = getCategoryIcon(analysis.category());
        sb.append(String.format("%s Error Category: %s\n\n", 
            categoryIcon, analysis.category()));
        
        // Root Cause
        sb.append("─── Root Cause ────────────────────────────────────────────\n");
        sb.append(analysis.rootCause()).append("\n\n");
        
        // Confidence
        sb.append("─── Confidence ────────────────────────────────────────────\n");
        int confidencePercent = (int) (analysis.confidence() * 100);
        String confidenceBar = createProgressBar(confidencePercent);
        sb.append(String.format("Confidence: %d%% %s\n\n", confidencePercent, confidenceBar));
        
        // Contributing Factors
        if (!analysis.contributingFactors().isEmpty()) {
            sb.append("─── Contributing Factors ────────────────────────────────\n");
            for (String factor : analysis.contributingFactors()) {
                sb.append("• ").append(factor).append("\n");
            }
            sb.append("\n");
        }
        
        // Remediation Steps
        if (!analysis.suggestedFixes().isEmpty()) {
            sb.append("─── Recommended Remediation Steps ────────────────────────\n");
            // Sort by priority (lower is better)
            List<RemediationStep> sortedSteps = new ArrayList<>(analysis.suggestedFixes());
            sortedSteps.sort((a, b) -> Integer.compare(a.priority(), b.priority()));
            
            for (int i = 0; i < sortedSteps.size(); i++) {
                RemediationStep step = sortedSteps.get(i);
                sb.append(String.format("\n[%d] %s\n", i + 1, step.action()));
                sb.append("    Description: ").append(step.description()).append("\n");
                if (!step.details().isEmpty()) {
                    sb.append("    Details:\n");
                    for (String detail : step.details()) {
                        sb.append("    • ").append(detail).append("\n");
                    }
                }
            }
        }
        
        // Similar Past Failures
        if (!analysis.similarFailures().isEmpty()) {
            sb.append("\n─── Similar Past Failures ───────────────────────────────\n");
            for (SimilarPastFailure failure : analysis.similarFailures()) {
                sb.append(String.format("• %s - %s\n", 
                    failure.errorPattern(), failure.resolution()));
            }
        }
        
        sb.append("\n═══════════════════════════════════════════════════════════\n");
        
        outputArea.setText(sb.toString());
    }
    
    private String getCategoryIcon(ErrorCategory category) {
        return switch (category) {
            case NAMESPACE_MIGRATION -> "🔄";
            case CLASSPATH_ISSUE -> "📦";
            case BINARY_INCOMPATIBILITY -> "⚙️";
            case CONFIGURATION_ERROR -> "⚙️";
            case UNKNOWN -> "❓";
        };
    }
    
    private String createProgressBar(int percent) {
        int filled = percent / 10;
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < 10; i++) {
            bar.append(i < filled ? "█" : "░");
        }
        bar.append("]");
        return bar.toString();
    }
    
    public JPanel getPanel() {
        return panel;
    }
}
