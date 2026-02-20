package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.advancedscanning.domain.*;
import adrianmikula.jakartamigration.intellij.service.AdvancedScanningService;
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
private JTable buildConfigTable;
private JTable configFileTable;
private JTable deprecatedApiTable;
private JTable cdiInjectionTable;
private JTable restSoapTable;
private JTable securityApiTable;
private JTable jmsMessagingTable;
private JTable transitiveDependencyTable;
private JTable classloaderModuleTable;
private JLabel jpaStatusLabel;
private JLabel beanValidationStatusLabel;
private JLabel servletJspStatusLabel;
private JLabel buildConfigStatusLabel;
private JLabel configFileStatusLabel;
private JLabel deprecatedApiStatusLabel;
private JLabel cdiInjectionStatusLabel;
private JLabel restSoapStatusLabel;
private JLabel securityApiStatusLabel;
private JLabel jmsMessagingStatusLabel;
private JLabel transitiveDependencyStatusLabel;
private JLabel classloaderModuleStatusLabel;
    
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
        
        // Build Config Tab
        JPanel buildConfigPanel = createBuildConfigPanel();
        tabbedPane.addTab("Build Config", buildConfigPanel);
        
        // Config File Tab
        JPanel configFilePanel = createConfigFilePanel();
        tabbedPane.addTab("Config Files", configFilePanel);
        
        // Deprecated API Tab
        JPanel deprecatedApiPanel = createDeprecatedApiPanel();
        tabbedPane.addTab("Deprecated API", deprecatedApiPanel);
        
        // CDI Injection Tab
        JPanel cdiInjectionPanel = createCdiInjectionPanel();
        tabbedPane.addTab("CDI Injection", cdiInjectionPanel);
        
        // REST/SOAP Tab
        JPanel restSoapPanel = createRestSoapPanel();
        tabbedPane.addTab("REST/SOAP", restSoapPanel);
        
        // Security API Tab
        JPanel securityApiPanel = createSecurityApiPanel();
        tabbedPane.addTab("Security API", securityApiPanel);
        
        // JMS Messaging Tab
        JPanel jmsMessagingPanel = createJmsMessagingPanel();
        tabbedPane.addTab("JMS Messaging", jmsMessagingPanel);
        
        // Classloader/Module Tab
        JPanel classloaderModulePanel = createClassloaderModulePanel();
        tabbedPane.addTab("Classloader", classloaderModulePanel);
        
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

 // New panels for additional scan types
 private JPanel createBuildConfigPanel() {
 JPanel panel = new JPanel(new BorderLayout());
 panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
 
 // Status label
 buildConfigStatusLabel = new JLabel("Not scanned yet");
 buildConfigStatusLabel.setForeground(Color.GRAY);
 
 // Table
 String[] columns = {"File", "Line", "Configuration", "Jakarta Equivalent"};
 buildConfigTable = new JTable(new DefaultTableModel(columns, 0) {
 @Override
 public boolean isCellEditable(int row, int column) {
 return false;
 }
 });
 buildConfigTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
 
 panel.add(buildConfigStatusLabel, BorderLayout.NORTH);
 panel.add(new JScrollPane(buildConfigTable), BorderLayout.CENTER);
 
 return panel;
 }

 private JPanel createConfigFilePanel() {
 JPanel panel = new JPanel(new BorderLayout());
 panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
 
 // Status label
 configFileStatusLabel = new JLabel("Not scanned yet");
 configFileStatusLabel.setForeground(Color.GRAY);
 
 // Table
 String[] columns = {"File", "Line", "Configuration", "Jakarta Equivalent"};
 configFileTable = new JTable(new DefaultTableModel(columns, 0) {
 @Override
 public boolean isCellEditable(int row, int column) {
 return false;
 }
 });
 configFileTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
 
 panel.add(configFileStatusLabel, BorderLayout.NORTH);
 panel.add(new JScrollPane(configFileTable), BorderLayout.CENTER);
 
 return panel;
 }

 private JPanel createDeprecatedApiPanel() {
 JPanel panel = new JPanel(new BorderLayout());
 panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
 
 // Status label
 deprecatedApiStatusLabel = new JLabel("Not scanned yet");
 deprecatedApiStatusLabel.setForeground(Color.GRAY);
 
 // Table
 String[] columns = {"File", "Line", "Deprecated API", "Jakarta Equivalent"};
 deprecatedApiTable = new JTable(new DefaultTableModel(columns, 0) {
 @Override
 public boolean isCellEditable(int row, int column) {
 return false;
 }
 });
 deprecatedApiTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
 
 panel.add(deprecatedApiStatusLabel, BorderLayout.NORTH);
 panel.add(new JScrollPane(deprecatedApiTable), BorderLayout.CENTER);
 
 return panel;
 }

 private JPanel createCdiInjectionPanel() {
 JPanel panel = new JPanel(new BorderLayout());
 panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
 
 // Status label
 cdiInjectionStatusLabel = new JLabel("Not scanned yet");
 cdiInjectionStatusLabel.setForeground(Color.GRAY);
 
 // Table
 String[] columns = {"File", "Line", "CDI Injection", "Jakarta Equivalent"};
 cdiInjectionTable = new JTable(new DefaultTableModel(columns, 0) {
 @Override
 public boolean isCellEditable(int row, int column) {
 return false;
 }
 });
 cdiInjectionTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
 
 panel.add(cdiInjectionStatusLabel, BorderLayout.NORTH);
 panel.add(new JScrollPane(cdiInjectionTable), BorderLayout.CENTER);
 
 return panel;
 }

 private JPanel createRestSoapPanel() {
 JPanel panel = new JPanel(new BorderLayout());
 panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
 
 // Status label
 restSoapStatusLabel = new JLabel("Not scanned yet");
 restSoapStatusLabel.setForeground(Color.GRAY);
 
 // Table
 String[] columns = {"File", "Line", "REST/SOAP", "Jakarta Equivalent"};
 restSoapTable = new JTable(new DefaultTableModel(columns, 0) {
 @Override
 public boolean isCellEditable(int row, int column) {
 return false;
 }
 });
 restSoapTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
 
 panel.add(restSoapStatusLabel, BorderLayout.NORTH);
 panel.add(new JScrollPane(restSoapTable), BorderLayout.CENTER);
 
 return panel;
 }

 private JPanel createSecurityApiPanel() {
 JPanel panel = new JPanel(new BorderLayout());
 panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
 
 // Status label
 securityApiStatusLabel = new JLabel("Not scanned yet");
 securityApiStatusLabel.setForeground(Color.GRAY);
 
 // Table
 String[] columns = {"File", "Line", "Security API", "Jakarta Equivalent"};
 securityApiTable = new JTable(new DefaultTableModel(columns, 0) {
 @Override
 public boolean isCellEditable(int row, int column) {
 return false;
 }
 });
 securityApiTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
 
 panel.add(securityApiStatusLabel, BorderLayout.NORTH);
 panel.add(new JScrollPane(securityApiTable), BorderLayout.CENTER);
 
 return panel;
 }

 private JPanel createJmsMessagingPanel() {
 JPanel panel = new JPanel(new BorderLayout());
 panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
 
 // Status label
 jmsMessagingStatusLabel = new JLabel("Not scanned yet");
 jmsMessagingStatusLabel.setForeground(Color.GRAY);
 
 // Table
 String[] columns = {"File", "Line", "JMS Messaging", "Jakarta Equivalent"};
 jmsMessagingTable = new JTable(new DefaultTableModel(columns, 0) {
 @Override
 public boolean isCellEditable(int row, int column) {
 return false;
 }
 });
 jmsMessagingTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
 
 panel.add(jmsMessagingStatusLabel, BorderLayout.NORTH);
 panel.add(new JScrollPane(jmsMessagingTable), BorderLayout.CENTER);
 
 return panel;
 }

 private JPanel createClassloaderModulePanel() {
 JPanel panel = new JPanel(new BorderLayout());
 panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
 
 // Status label
 classloaderModuleStatusLabel = new JLabel("Not scanned yet");
 classloaderModuleStatusLabel.setForeground(Color.GRAY);
 
 // Table
 String[] columns = {"File", "Line", "Classloader/Module", "Jakarta Equivalent"};
 classloaderModuleTable = new JTable(new DefaultTableModel(columns, 0) {
 @Override
 public boolean isCellEditable(int row, int column) {
 return false;
 }
 });
 classloaderModuleTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
 
 panel.add(classloaderModuleStatusLabel, BorderLayout.NORTH);
 panel.add(new JScrollPane(classloaderModuleTable), BorderLayout.CENTER);
 
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
        
        // Display Build Config results
        BuildConfigProjectScanResult buildConfigResult = summary.buildConfigResult();
        if (buildConfigResult != null) {
            displayBuildConfigResults(buildConfigResult);
        }
        
        // Display Config File results
        ConfigFileProjectScanResult configFileResult = summary.configFileResult();
        if (configFileResult != null) {
            displayConfigFileResults(configFileResult);
        }
        
        // Display Deprecated API results
        DeprecatedApiProjectScanResult deprecatedApiResult = summary.deprecatedApiResult();
        if (deprecatedApiResult != null) {
            displayDeprecatedApiResults(deprecatedApiResult);
        }
        
        // Display CDI Injection results
        CdiInjectionProjectScanResult cdiInjectionResult = summary.cdiInjectionResult();
        if (cdiInjectionResult != null) {
            displayCdiInjectionResults(cdiInjectionResult);
        }
        
        // Display REST/SOAP results
        RestSoapProjectScanResult restSoapResult = summary.restSoapResult();
        if (restSoapResult != null) {
            displayRestSoapResults(restSoapResult);
        }
        
        // Display Security API results
        SecurityApiProjectScanResult securityApiResult = summary.securityApiResult();
        if (securityApiResult != null) {
            displaySecurityApiResults(securityApiResult);
        }
        
        // Display JMS Messaging results
        JmsMessagingProjectScanResult jmsMessagingResult = summary.jmsMessagingResult();
        if (jmsMessagingResult != null) {
            displayJmsMessagingResults(jmsMessagingResult);
        }
        
        // Display Classloader/Module results
        ClassloaderModuleProjectScanResult classloaderModuleResult = summary.classloaderModuleResult();
        if (classloaderModuleResult != null) {
            displayClassloaderModuleResults(classloaderModuleResult);
        }
        
        // Show summary message
        int totalIssues = summary.getTotalIssuesFound();
        String message = totalIssues > 0 
            ? "Found " + totalIssues + " issues requiring attention across all scanners."
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

    private void displayBuildConfigResults(BuildConfigProjectScanResult result) {
        DefaultTableModel model = (DefaultTableModel) buildConfigTable.getModel();
        model.setRowCount(0);
        
        if (result.hasJavaxDependencies()) {
            buildConfigStatusLabel.setText(String.format("Found %d dependencies in %d files", 
                result.totalDependenciesFound(), result.totalFilesWithJavaxDependencies()));
            buildConfigStatusLabel.setForeground(new Color(200, 100, 0));
        } else {
            buildConfigStatusLabel.setText("No javax.* build configuration found");
            buildConfigStatusLabel.setForeground(new Color(0, 150, 0));
        }
    }

    private void displayConfigFileResults(ConfigFileProjectScanResult result) {
        DefaultTableModel model = (DefaultTableModel) configFileTable.getModel();
        model.setRowCount(0);
        
        if (result.hasJavaxUsage()) {
            configFileStatusLabel.setText(String.format("Found %d usages in %d files", 
                result.getTotalJavaxUsages(), result.getFilesWithJavaxUsage()));
            configFileStatusLabel.setForeground(new Color(200, 100, 0));
        } else {
            configFileStatusLabel.setText("No javax.* configuration found");
            configFileStatusLabel.setForeground(new Color(0, 150, 0));
        }
    }

    private void displayDeprecatedApiResults(DeprecatedApiProjectScanResult result) {
        DefaultTableModel model = (DefaultTableModel) deprecatedApiTable.getModel();
        model.setRowCount(0);
        
        if (result.hasDeprecatedApiUsage()) {
            deprecatedApiStatusLabel.setText(String.format("Found %d usages in %d files", 
                result.totalUsagesFound(), result.totalFilesWithDeprecatedApi()));
            deprecatedApiStatusLabel.setForeground(new Color(200, 100, 0));
        } else {
            deprecatedApiStatusLabel.setText("No deprecated javax.* API found");
            deprecatedApiStatusLabel.setForeground(new Color(0, 150, 0));
        }
    }

    private void displayCdiInjectionResults(CdiInjectionProjectScanResult result) {
        DefaultTableModel model = (DefaultTableModel) cdiInjectionTable.getModel();
        model.setRowCount(0);
        
        if (result.hasJavaxUsage()) {
            cdiInjectionStatusLabel.setText(String.format("Found %d usages in %d files", 
                result.totalAnnotationsFound(), result.totalFilesWithJavaxUsage()));
            cdiInjectionStatusLabel.setForeground(new Color(200, 100, 0));
        } else {
            cdiInjectionStatusLabel.setText("No javax.inject.* usage found");
            cdiInjectionStatusLabel.setForeground(new Color(0, 150, 0));
        }
    }

    private void displayRestSoapResults(RestSoapProjectScanResult result) {
        DefaultTableModel model = (DefaultTableModel) restSoapTable.getModel();
        model.setRowCount(0);
        
        if (result.hasJavaxUsage()) {
            restSoapStatusLabel.setText(String.format("Found %d usages in %d files", 
                result.totalUsagesFound(), result.totalFilesWithJavaxUsage()));
            restSoapStatusLabel.setForeground(new Color(200, 100, 0));
        } else {
            restSoapStatusLabel.setText("No javax.jws.* or javax.ws.* usage found");
            restSoapStatusLabel.setForeground(new Color(0, 150, 0));
        }
    }

    private void displaySecurityApiResults(SecurityApiProjectScanResult result) {
        DefaultTableModel model = (DefaultTableModel) securityApiTable.getModel();
        model.setRowCount(0);
        
        if (result.hasJavaxUsage()) {
            securityApiStatusLabel.setText(String.format("Found %d usages in %d files", 
                result.getTotalJavaxUsages(), result.getFilesWithJavaxUsage()));
            securityApiStatusLabel.setForeground(new Color(200, 100, 0));
        } else {
            securityApiStatusLabel.setText("No javax.security.* usage found");
            securityApiStatusLabel.setForeground(new Color(0, 150, 0));
        }
    }

    private void displayJmsMessagingResults(JmsMessagingProjectScanResult result) {
        DefaultTableModel model = (DefaultTableModel) jmsMessagingTable.getModel();
        model.setRowCount(0);
        
        if (result.hasJavaxUsage()) {
            jmsMessagingStatusLabel.setText(String.format("Found %d usages in %d files", 
                result.getTotalJavaxUsages(), result.getFilesWithJavaxUsage()));
            jmsMessagingStatusLabel.setForeground(new Color(200, 100, 0));
        } else {
            jmsMessagingStatusLabel.setText("No javax.jms.* usage found");
            jmsMessagingStatusLabel.setForeground(new Color(0, 150, 0));
        }
    }

    private void displayClassloaderModuleResults(ClassloaderModuleProjectScanResult result) {
        DefaultTableModel model = (DefaultTableModel) classloaderModuleTable.getModel();
        model.setRowCount(0);
        
        if (result.hasJavaxUsage()) {
            classloaderModuleStatusLabel.setText(String.format("Found %d usages in %d files", 
                result.getTotalJavaxUsages(), result.getFilesWithJavaxUsage()));
            classloaderModuleStatusLabel.setForeground(new Color(200, 100, 0));
        } else {
            classloaderModuleStatusLabel.setText("No javax.* classloader/module usage found");
            classloaderModuleStatusLabel.setForeground(new Color(0, 150, 0));
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
