package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.platforms.service.SimplifiedPlatformDetectionService;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Simplified platforms tab component
 * Direct UI without complex abstractions
 */
public class SimplePlatformsTabComponent {
    
    private final Project project;
    private final SimplifiedPlatformDetectionService detectionService;
    private final JPanel mainPanel;
    private final JButton scanButton;
    private final JBLabel resultsLabel;
    
    public SimplePlatformsTabComponent(Project project) {
        this.project = project;
        this.detectionService = new SimplifiedPlatformDetectionService();
        this.scanButton = createScanButton();
        this.resultsLabel = createResultsLabel();
        this.mainPanel = createMainPanel();
    }
    
    private JPanel createMainPanel() {
        JBPanel panel = new JBPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(10));
        
        // Header
        JBLabel headerLabel = new JBLabel("Platform Detection");
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 16f));
        panel.add(headerLabel, BorderLayout.NORTH);
        
        // Center area
        JBPanel centerPanel = new JBPanel(new BorderLayout());
        centerPanel.add(scanButton, BorderLayout.NORTH);
        centerPanel.add(resultsLabel, BorderLayout.CENTER);
        panel.add(centerPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JButton createScanButton() {
        JButton button = new JButton("Analyze Project");
        button.addActionListener(e -> scanProject());
        return button;
    }
    
    private JBLabel createResultsLabel() {
        JBLabel label = new JBLabel("Click 'Analyze Project' to detect application servers");
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }
    
    private void scanProject() {
        scanButton.setEnabled(false);
        scanButton.setText("Scanning...");
        resultsLabel.setText("Scanning project for application servers...");
        
        // Run scan in background thread
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                Path projectPath = Paths.get(project.getBasePath());
                List<String> detectedServers = detectionService.scanProject(projectPath);
                
                if (detectedServers.isEmpty()) {
                    resultsLabel.setText("<html><body style='text-align: center;'>" +
                        "<b>No application servers detected</b><br>" +
                        "<small>Ensure your project contains pom.xml or build.gradle with server dependencies</small>" +
                        "</body></html>");
                } else {
                    StringBuilder sb = new StringBuilder("<html><body style='text-align: center;'>");
                    sb.append("<b>Detected Application Servers:</b><br><br>");
                    for (String server : detectedServers) {
                        sb.append("• ").append(server).append("<br>");
                    }
                    sb.append("</body></html>");
                    resultsLabel.setText(sb.toString());
                }
            } catch (Exception e) {
                resultsLabel.setText("<html><body style='text-align: center; color: red;'>" +
                    "<b>Error scanning project</b><br>" +
                    "<small>" + e.getMessage() + "</small>" +
                    "</body></html>");
            } finally {
                scanButton.setEnabled(true);
                scanButton.setText("Analyze Project");
            }
        });
    }
    
    public JPanel getPanel() {
        return mainPanel;
    }
}
