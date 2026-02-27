package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Migration Steps Component - Clear and concise migration implementation steps.
 * Replaces the complex phases UI with a straightforward step-by-step guide.
 */
public class MigrationStepsComponent {
    private final JPanel panel;
    private final Project project;
    private final JList<String> stepsList;
    private final DefaultListModel<String> stepsModel;
    private List<DependencyInfo> dependencies;
    
    public MigrationStepsComponent(Project project) {
        this.project = project;
        this.dependencies = new ArrayList<>();
        this.stepsModel = new DefaultListModel<>();
        this.stepsList = createStepsList();
        this.panel = createPanel();
        updateSteps();
    }
    
    private JList<String> createStepsList() {
        JList<String> list = new JList<>(stepsModel);
        list.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        return list;
    }
    
    private JPanel createPanel() {
        JPanel mainPanel = new JBPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Header
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        
        JLabel titleLabel = new JLabel("Migration Steps");
        titleLabel.setFont(new Font(titleLabel.getFont().getName(), Font.BOLD, 16));
        
        JLabel descLabel = new JLabel("Follow these steps to migrate your project from javax to jakarta");
        descLabel.setForeground(Color.GRAY);
        
        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(5));
        headerPanel.add(descLabel);
        
        // Steps list in scroll pane
        JScrollPane listScrollPane = new JBScrollPane(stepsList);
        listScrollPane.setBorder(BorderFactory.createTitledBorder("Steps"));
        
        // Action buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        
        JButton startButton = new JButton("▶ Start Migration");
        startButton.setFont(startButton.getFont().deriveFont(Font.BOLD));
        startButton.addActionListener(this::handleStartMigration);
        
        JButton refreshButton = new JButton("↻ Refresh Steps");
        refreshButton.addActionListener(e -> updateSteps());
        
        buttonPanel.add(startButton);
        buttonPanel.add(refreshButton);
        
        // Info panel
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        
        JLabel tipLabel = new JLabel("💡 Tip: Run analysis first to see dependency-specific steps");
        tipLabel.setForeground(new Color(100, 100, 100));
        tipLabel.setFont(tipLabel.getFont().deriveFont(Font.ITALIC));
        
        infoPanel.add(tipLabel);
        
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(listScrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        return mainPanel;
    }
    
    private void handleStartMigration(ActionEvent e) {
        int selectedIndex = stepsList.getSelectedIndex();
        if (selectedIndex >= 0) {
            String step = stepsModel.getElementAt(selectedIndex);
            Messages.showInfoMessage(project,
                "Starting: " + step + "\n\nThis will launch the appropriate migration tool.",
                "Start Migration");
        } else {
            Messages.showWarningDialog(project,
                "Please select a step to start.",
                "No Step Selected");
        }
    }
    
    public void setDependencies(List<DependencyInfo> dependencies) {
        this.dependencies = dependencies != null ? dependencies : new ArrayList<>();
        updateSteps();
    }
    
    public void updateSteps() {
        stepsModel.clear();
        
        // Add header info
        int totalDeps = dependencies.size();
        long needsUpgrade = dependencies.stream()
            .filter(d -> d.getMigrationStatus() != null && 
                d.getMigrationStatus().name().equals("NEEDS_UPGRADE"))
            .count();
        long compatible = dependencies.stream()
            .filter(d -> d.getMigrationStatus() != null && 
                d.getMigrationStatus().name().equals("COMPATIBLE"))
            .count();
        
        // Base steps for any migration
        addStep("═══ PRE-MIGRATION ═══", "");
        
        if (totalDeps == 0) {
            addStep("1. Analyze Project", "Run the analyzer to detect migration requirements");
            addStep("2. Review Dependencies", "Check which dependencies need updates");
        } else {
            addStep("1. Review Analysis Results", String.format("Found %d dependencies: %d need upgrade, %d compatible", 
                totalDeps, needsUpgrade, compatible));
            addStep("2. Update Dependencies", "Update javax dependencies to Jakarta equivalents");
            addStep("3. Run OpenRewrite Recipes", "Apply automated code transformations");
            addStep("4. Fix Compilation Errors", "Manually resolve any remaining issues");
            addStep("5. Run Tests", "Verify all tests pass");
            addStep("6. Deploy & Monitor", "Deploy and watch for runtime errors");
        }
        
        // Add dependency-specific steps
        if (!dependencies.isEmpty()) {
            addStep("", "");
            addStep("═══ DEPENDENCY STEPS ═══", "");
            
            // Show first few dependencies that need upgrading
            List<DependencyInfo> needsUpgradeDeps = dependencies.stream()
                .filter(d -> d.getMigrationStatus() != null && 
                    d.getMigrationStatus().name().equals("NEEDS_UPGRADE"))
                .limit(10)
                .toList();
            
            int remaining = (int)(needsUpgrade - needsUpgradeDeps.size());
            
            for (int i = 0; i < needsUpgradeDeps.size(); i++) {
                DependencyInfo dep = needsUpgradeDeps.get(i);
                String version = dep.getRecommendedVersion() != null ? 
                    dep.getRecommendedVersion() : "(version TBD)";
                addStep(String.format("%d. Update %s:%s → %s", 
                    i + 7, 
                    dep.getGroupId(), 
                    dep.getArtifactId(),
                    version),
                    String.format("Current: %s", dep.getCurrentVersion()));
            }
            
            if (remaining > 0) {
                addStep(String.format("   ... and %d more", remaining), "");
            }
        }
        
        addStep("", "");
        addStep("═══ POST-MIGRATION ═══", "");
        addStep("Run Runtime Verification", "Use the Runtime tab to diagnose any errors");
        addStep("Fix Runtime Issues", "Apply recommended fixes for any runtime errors");
        addStep("Final Testing", "Run full test suite");
        addStep("Deploy", "Deploy to staging/production");
    }
    
    private void addStep(String mainText, String detailText) {
        String step = detailText.isEmpty() ? mainText : mainText + "\n   └─ " + detailText;
        stepsModel.addElement(step);
    }
    
    public JPanel getPanel() {
        return panel;
    }
}
