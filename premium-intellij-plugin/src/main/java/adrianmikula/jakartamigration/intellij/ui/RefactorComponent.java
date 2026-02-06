package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.coderefactoring.domain.RefactoringPhase;
import adrianmikula.jakartamigration.coderefactoring.domain.ValidationIssue;
import adrianmikula.jakartamigration.coderefactoring.domain.ValidationResult;
import adrianmikula.jakartamigration.coderefactoring.domain.ValidationSeverity;
import adrianmikula.jakartamigration.coderefactoring.domain.ValidationStatus;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Refactor tab component for code refactoring operations.
 * Provides UI for applying automated refactorings and viewing refactoring results.
 */
public class RefactorComponent {
    private static final Logger LOG = Logger.getInstance(RefactorComponent.class);
    
    private final JPanel panel;
    private final Project project;
    private final DefaultListModel<String> phaseListModel;
    private final JList<String> phaseList;
    private final JTextArea previewArea;
    private final JButton applyButton;
    private final JButton validateButton;
    private final JButton rollbackButton;
    private final JLabel statusLabel;
    
    public RefactorComponent(Project project) {
        this.project = project;
        this.panel = createPanel();
        this.phaseListModel = new DefaultListModel<>();
        this.phaseList = new JList<>(phaseListModel);
        this.previewArea = new JTextArea(10, 40);
        this.applyButton = new JButton("Apply Refactoring");
        this.validateButton = new JButton("Validate Changes");
        this.rollbackButton = new JButton("Rollback");
        this.statusLabel = new JLabel("Ready");
        
        setupEventHandlers();
    }
    
    private JPanel createPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Left panel - Phases list
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setBorder(new TitledBorder("Refactoring Phases"));
        leftPanel.setPreferredSize(new Dimension(250, 0));
        
        phaseList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane phaseScrollPane = new JScrollPane(phaseList);
        leftPanel.add(phaseScrollPane, BorderLayout.CENTER);
        
        JPanel phaseButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        phaseButtonPanel.add(applyButton);
        phaseButtonPanel.add(validateButton);
        phaseButtonPanel.add(rollbackButton);
        leftPanel.add(phaseButtonPanel, BorderLayout.SOUTH);
        
        // Right panel - Preview
        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
        rightPanel.setBorder(new TitledBorder("Changes Preview"));
        
        previewArea.setEditable(false);
        previewArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane previewScrollPane = new JScrollPane(previewArea);
        rightPanel.add(previewScrollPane, BorderLayout.CENTER);
        
        // Status panel
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(new JLabel("Status:"));
        statusPanel.add(statusLabel);
        rightPanel.add(statusPanel, BorderLayout.SOUTH);
        
        // Split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setResizeWeight(0.3);
        mainPanel.add(splitPane, BorderLayout.CENTER);
        
        return mainPanel;
    }
    
    private void setupEventHandlers() {
        applyButton.addActionListener(this::handleApplyRefactoring);
        validateButton.addActionListener(this::handleValidate);
        rollbackButton.addActionListener(this::handleRollback);
        
        phaseList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedPhase = phaseList.getSelectedValue();
                if (selectedPhase != null) {
                    updatePreview(selectedPhase);
                }
            }
        });
    }
    
    private void handleApplyRefactoring(ActionEvent e) {
        String selectedPhase = phaseList.getSelectedValue();
        if (selectedPhase == null) {
            Messages.showWarningDialog(project, "Please select a refactoring phase first.", "No Phase Selected");
            return;
        }
        
        int result = Messages.showYesNoDialog(
            project,
            "Apply refactoring for phase: " + selectedPhase + "?",
            "Confirm Refactoring",
            Messages.getQuestionIcon()
        );
        
        if (result == Messages.YES) {
            applyRefactoring(selectedPhase);
        }
    }
    
    private void handleValidate(ActionEvent e) {
        String selectedPhase = phaseList.getSelectedValue();
        if (selectedPhase == null) {
            Messages.showWarningDialog(project, "Please select a refactoring phase first.", "No Phase Selected");
            return;
        }
        
        validateChanges(selectedPhase);
    }
    
    private void handleRollback(ActionEvent e) {
        String selectedPhase = phaseList.getSelectedValue();
        if (selectedPhase == null) {
            Messages.showWarningDialog(project, "Please select a refactoring phase first.", "No Phase Selected");
            return;
        }
        
        int result = Messages.showYesNoDialog(
            project,
            "Rollback refactoring for phase: " + selectedPhase + "?",
            "Confirm Rollback",
            Messages.getWarningIcon()
        );
        
        if (result == Messages.YES) {
            rollbackPhase(selectedPhase);
        }
    }
    
    private void applyRefactoring(String phase) {
        statusLabel.setText("Applying " + phase + "...");
        statusLabel.setForeground(Color.BLUE);
        
        // Simulate refactoring application
        SwingUtilities.invokeLater(() -> {
            try {
                Thread.sleep(500);
                statusLabel.setText("Applied successfully");
                statusLabel.setForeground(Color.GREEN);
                Messages.showInfoMessage(project, "Refactoring applied successfully!", "Success");
            } catch (Exception ex) {
                statusLabel.setText("Failed: " + ex.getMessage());
                statusLabel.setForeground(Color.RED);
                Messages.showErrorDialog(project, "Refactoring failed: " + ex.getMessage(), "Error");
            }
        });
    }
    
    private void validateChanges(String phase) {
        statusLabel.setText("Validating " + phase + "...");
        statusLabel.setForeground(Color.BLUE);
        
        // Simulate validation using record constructor
        SwingUtilities.invokeLater(() -> {
            try {
                Thread.sleep(300);
                ValidationResult result = new ValidationResult(
                    true,
                    new ArrayList<>(),
                    "src/main/java/example/Test.java",
                    ValidationStatus.PASSED
                );
                
                String message = result.isSuccessful() ? "All changes validated successfully" : "Validation found issues";
                statusLabel.setText("Validated: " + message);
                statusLabel.setForeground(Color.GREEN);
                Messages.showInfoMessage(project, message, "Valid");
            } catch (Exception ex) {
                statusLabel.setText("Validation failed: " + ex.getMessage());
                statusLabel.setForeground(Color.RED);
                Messages.showErrorDialog(project, "Validation failed: " + ex.getMessage(), "Error");
            }
        });
    }
    
    private void rollbackPhase(String phase) {
        statusLabel.setText("Rolling back " + phase + "...");
        statusLabel.setForeground(Color.BLUE);
        
        // Simulate rollback
        SwingUtilities.invokeLater(() -> {
            try {
                Thread.sleep(400);
                statusLabel.setText("Rolled back successfully");
                statusLabel.setForeground(Color.GREEN);
                Messages.showInfoMessage(project, "Rollback completed successfully!", "Success");
            } catch (Exception ex) {
                statusLabel.setText("Rollback failed: " + ex.getMessage());
                statusLabel.setForeground(Color.RED);
                Messages.showErrorDialog(project, "Rollback failed: " + ex.getMessage(), "Error");
            }
        });
    }
    
    private void updatePreview(String phase) {
        // Generate preview based on selected phase
        StringBuilder preview = new StringBuilder();
        preview.append("Refactoring Phase: ").append(phase).append("\n\n");
        preview.append("Changes to be applied:\n");
        preview.append("- Update imports from javax.* to jakarta.*\n");
        preview.append("- Update XML namespace declarations\n");
        preview.append("- Update descriptor files (web.xml, application.xml)\n");
        preview.append("- Update JNDI lookups\n");
        preview.append("- Update any remaining references\n\n");
        preview.append("Files affected:\n");
        preview.append("- src/main/java/com/example/MyServlet.java\n");
        preview.append("- src/main/webapp/WEB-INF/web.xml\n");
        preview.append("- pom.xml\n");
        
        previewArea.setText(preview.toString());
    }
    
    /**
     * Set the available refactoring phases
     */
    public void setPhases(List<RefactoringPhase> phases) {
        phaseListModel.clear();
        for (RefactoringPhase phase : phases) {
            // Use description() as the display name since getName() doesn't exist
            phaseListModel.addElement(phase.description());
        }
    }
    
    /**
     * Clear all phases
     */
    public void clearPhases() {
        phaseListModel.clear();
        previewArea.setText("");
    }
    
    public JPanel getPanel() {
        return panel;
    }
}
