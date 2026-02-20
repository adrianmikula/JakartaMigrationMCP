package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.advancedscanning.domain.BeanValidationProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.JpaProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.ServletJspProjectScanResult;
import adrianmikula.jakartamigration.intellij.service.AdvancedScanningService;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * UI component for displaying advanced scanning results (JPA, Bean Validation, Servlet/JSP).
 * This is a premium feature that shows detailed annotation-level analysis.
 */
public class AdvancedScansComponent {
    private static final Logger LOG = Logger.getInstance(AdvancedScansComponent.class);

    private final Project project;
    private final AdvancedScanningService scanningService;
    private final JPanel mainPanel;
    
    private JTabbedPane tabbedPane;
    private JTable jpaTable;
    private JTable beanValidationTable;
    private JTable servletJspTable;
    
    private JLabel jpaStatusLabel;
    private JLabel beanValidationStatusLabel;
    private JLabel servletJspStatusLabel;
    
    private JButton scanButton;
    private JProgressBar progressBar;

    public AdvancedScansComponent(Project project) {
        this.project = project;
        this.scanningService = new AdvancedScanningService();
        this.mainPanel = new JPanel(new BorderLayout());
        initializeUI();
    }

    private void initializeUI() {
        // Toolbar
        JPanel toolbarPanel = createToolbar();
        
        // Tabbed pane for different scan types
        tabbedPane = new JTabbedPane();
        
        // JPA Tab
        JPanel jpaPanel = createJpaPanel();
        tabbedPane.addTab("JPA Annotations", jpaPanel);
        
        // Bean Validation Tab
        JPanel beanValidationPanel = createBeanValidationPanel();
        tabbedPane.addTab("Bean Validation", beanValidationPanel);
        
        // Servlet/JSP Tab
        JPanel servletJspPanel = createServletJspPanel();
        tabbedPane.addTab("Servlet/JSP", servletJspPanel);
        
        mainPanel.add(toolbarPanel, BorderLayout.NORTH);
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createToolbar() {
        JPanel toolbarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        toolbarPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        scanButton = new JButton("🔍 Run Advanced Scans");
        scanButton.setToolTipText("Run JPA, Bean Validation, and Servlet/JSP scans");
        scanButton.addActionListener(e -> runScans());
        toolbarPanel.add(scanButton);
        
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        progressBar.setPreferredSize(new Dimension(200, 20));
        toolbarPanel.add(progressBar);
        
        toolbarPanel.add(Box.createHorizontalGlue());
        
        return toolbarPanel;
    }

    private JPanel createJpaPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Status label
        jpaStatusLabel = new JLabel("Not scanned yet");
        jpaStatusLabel.setForeground(Color.GRAY);
        
        // Table
        String[] columns = {"File", "Line", "Annotation", "Jakarta Equivalent"};
        jpaTable = new JTable(new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        jpaTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        
        panel.add(jpaStatusLabel, BorderLayout.NORTH);
        panel.add(new JScrollPane(jpaTable), BorderLayout.CENTER);
        
        return panel;
    }

    private JPanel createBeanValidationPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Status label
        beanValidationStatusLabel = new JLabel("Not scanned yet");
        beanValidationStatusLabel.setForeground(Color.GRAY);
        
        // Table
        String[] columns = {"File", "Line", "Constraint", "Jakarta Equivalent"};
        beanValidationTable = new JTable(new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        beanValidationTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        
        panel.add(beanValidationStatusLabel, BorderLayout.NORTH);
        panel.add(new JScrollPane(beanValidationTable), BorderLayout.CENTER);
        
        return panel;
    }

    private JPanel createServletJspPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Status label
        servletJspStatusLabel = new JLabel("Not scanned yet");
        servletJspStatusLabel.setForeground(Color.GRAY);
        
        // Table
        String[] columns = {"File", "Line", "Class/Usage", "Type", "Jakarta Equivalent"};
        servletJspTable = new JTable(new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        servletJspTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        
        panel.add(servletJspStatusLabel, BorderLayout.NORTH);
        panel.add(new JScrollPane(servletJspTable), BorderLayout.CENTER);
        
        return panel;
    }

    private void runScans() {
        String projectPathStr = project.getBasePath();
        if (projectPathStr == null) {
            projectPathStr = project.getProjectFilePath();
        }
        
        if (projectPathStr == null) {
            JOptionPane.showMessageDialog(mainPanel, 
                "Cannot determine project path. Please open a project first.", 
                "Scan Failed", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        final Path projectPath = Path.of(projectPathStr);
        
        // Show progress
        progressBar.setVisible(true);
        scanButton.setEnabled(false);
        
        // Run scans in background
        SwingWorker<AdvancedScanningService.AdvancedScanSummary, Void> worker = 
            new SwingWorker<>() {
                @Override
                protected AdvancedScanningService.AdvancedScanSummary doInBackground() {
                    return scanningService.scanAll(projectPath);
                }
                
                @Override
                protected void done() {
                    try {
                        AdvancedScanningService.AdvancedScanSummary summary = get();
                        displayResults(summary);
                    } catch (Exception e) {
                        LOG.error("Error running scans", e);
                        JOptionPane.showMessageDialog(mainPanel, 
                            "Error running scans: " + e.getMessage(), 
                            "Scan Error", 
                            JOptionPane.ERROR_MESSAGE);
                    } finally {
                        progressBar.setVisible(false);
                        scanButton.setEnabled(true);
                    }
                }
            };
        
        worker.execute();
    }

    private void displayResults(AdvancedScanningService.AdvancedScanSummary summary) {
        // Display JPA results
        JpaProjectScanResult jpaResult = summary.jpaResult();
        if (jpaResult != null) {
            displayJpaResults(jpaResult);
        }
        
        // Display Bean Validation results
        BeanValidationProjectScanResult beanValidationResult = summary.beanValidationResult();
        if (beanValidationResult != null) {
            displayBeanValidationResults(beanValidationResult);
        }
        
        // Display Servlet/JSP results
        ServletJspProjectScanResult servletJspResult = summary.servletJspResult();
        if (servletJspResult != null) {
            displayServletJspResults(servletJspResult);
        }
        
        // Show summary message
        int totalIssues = summary.getTotalIssuesFound();
        String message = totalIssues > 0 
            ? "Found " + totalIssues + " issues requiring attention."
            : "No advanced scan issues found. Your code looks good!";
        
        JOptionPane.showMessageDialog(mainPanel, message, "Advanced Scan Complete", 
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void displayJpaResults(JpaProjectScanResult result) {
        DefaultTableModel model = (DefaultTableModel) jpaTable.getModel();
        model.setRowCount(0);
        
        if (result.hasJavaxUsage()) {
            jpaStatusLabel.setText(String.format("Found %d annotations in %d files", 
                result.totalAnnotationsFound(), result.totalFilesWithJavaxUsage()));
            jpaStatusLabel.setForeground(new Color(200, 100, 0));
            
            for (var fileResult : result.fileResults()) {
                for (var annotation : fileResult.annotations()) {
                    model.addRow(new Object[]{
                        fileResult.filePath().getFileName(),
                        annotation.lineNumber(),
                        annotation.annotationName(),
                        annotation.jakartaEquivalent()
                    });
                }
            }
        } else {
            jpaStatusLabel.setText("No javax.persistence.* usage found");
            jpaStatusLabel.setForeground(new Color(0, 150, 0));
        }
    }

    private void displayBeanValidationResults(BeanValidationProjectScanResult result) {
        DefaultTableModel model = (DefaultTableModel) beanValidationTable.getModel();
        model.setRowCount(0);
        
        if (result.hasJavaxUsage()) {
            beanValidationStatusLabel.setText(String.format("Found %d constraints in %d files", 
                result.totalAnnotationsFound(), result.totalFilesWithJavaxUsage()));
            beanValidationStatusLabel.setForeground(new Color(200, 100, 0));
            
            for (var fileResult : result.fileResults()) {
                for (var annotation : fileResult.annotations()) {
                    model.addRow(new Object[]{
                        fileResult.filePath().getFileName(),
                        annotation.lineNumber(),
                        annotation.annotationName(),
                        annotation.jakartaEquivalent()
                    });
                }
            }
        } else {
            beanValidationStatusLabel.setText("No javax.validation.* usage found");
            beanValidationStatusLabel.setForeground(new Color(0, 150, 0));
        }
    }

    private void displayServletJspResults(ServletJspProjectScanResult result) {
        DefaultTableModel model = (DefaultTableModel) servletJspTable.getModel();
        model.setRowCount(0);
        
        if (result.hasJavaxUsage()) {
            servletJspStatusLabel.setText(String.format("Found %d usages in %d files", 
                result.totalUsagesFound(), result.totalFilesWithJavaxUsage()));
            servletJspStatusLabel.setForeground(new Color(200, 100, 0));
            
            for (var fileResult : result.fileResults()) {
                for (var usage : fileResult.usages()) {
                    model.addRow(new Object[]{
                        fileResult.filePath().getFileName(),
                        usage.lineNumber(),
                        usage.className(),
                        usage.usageType(),
                        usage.jakartaEquivalent()
                    });
                }
            }
        } else {
            servletJspStatusLabel.setText("No javax.servlet.* usage found");
            servletJspStatusLabel.setForeground(new Color(0, 150, 0));
        }
    }

    /**
     * Gets the main panel for this component.
     * 
     * @return JPanel
     */
    public JPanel getPanel() {
        return mainPanel;
    }
}
