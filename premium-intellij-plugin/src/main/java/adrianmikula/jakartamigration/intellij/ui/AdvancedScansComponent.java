package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.advancedscanning.domain.*;
import adrianmikula.jakartamigration.intellij.service.AdvancedScanningService;
import adrianmikula.jakartamigration.analysis.persistence.CentralMigrationAnalysisStore;
import adrianmikula.jakartamigration.analysis.persistence.ObjectMapperService;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.table.JBTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * UI component for displaying advanced scanning results (JPA, Bean Validation,
 * Servlet/JSP).
 * This is a premium feature that shows detailed annotation-level analysis.
 */
public class AdvancedScansComponent {
    private static final Logger LOG = Logger.getInstance(AdvancedScansComponent.class);

    private final Project project;
    private final AdvancedScanningService scanningService;
    private final JPanel mainPanel;

    private JTabbedPane tabbedPane;
    private JBTable jpaTable;
    private JBTable beanValidationTable;
    private JBTable servletJspTable;
    private JBTable buildConfigTable;
    private JBTable configFileTable;
    private JBTable deprecatedApiTable;
    private JBTable cdiInjectionTable;
    private JBTable restSoapTable;
    private JBTable securityApiTable;
    private JBTable jmsMessagingTable;
    private JBTable transitiveDependencyTable;
    private JBTable classloaderModuleTable;
    private JBTable loggingMetricsTable;
    private JBTable serializationCacheTable;
    private JBTable thirdPartyLibTable;
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
    private JLabel loggingMetricsStatusLabel;
    private JLabel serializationCacheStatusLabel;
    private JLabel thirdPartyLibStatusLabel;

    private JButton scanButton;
    private JProgressBar progressBar;

    private final CentralMigrationAnalysisStore store;
    private final ObjectMapperService objectMapper;

    public AdvancedScansComponent(Project project, AdvancedScanningService scanningService) {
        this.project = project;
        this.scanningService = scanningService;
        this.store = new CentralMigrationAnalysisStore();
        this.objectMapper = new ObjectMapperService();
        this.mainPanel = new JPanel(new BorderLayout());
        initializeUI();
        loadInitialState();
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

        // Logging/Metrics Tab
        JPanel loggingMetricsPanel = createLoggingMetricsPanel();
        tabbedPane.addTab("Logging/Metrics", loggingMetricsPanel);

        // Serialization/Cache Tab
        JPanel serializationCachePanel = createSerializationCachePanel();
        tabbedPane.addTab("Serialization/Cache", serializationCachePanel);

        // Third-Party Libs Tab
        JPanel thirdPartyLibPanel = createThirdPartyLibPanel();
        tabbedPane.addTab("Third-Party Libs", thirdPartyLibPanel);

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
        String[] columns = { "File", "Line", "Annotation", "Jakarta Equivalent", "Path" };
        jpaTable = new JBTable(new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        setupTable(jpaTable);

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
        String[] columns = { "File", "Line", "Constraint", "Jakarta Equivalent", "Path" };
        beanValidationTable = new JBTable(new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        setupTable(beanValidationTable);

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
        String[] columns = { "File", "Line", "Class/Usage", "Type", "Jakarta Equivalent", "Path" };
        servletJspTable = new JBTable(new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        setupTable(servletJspTable);

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
        String[] columns = { "File", "Line", "Configuration", "Jakarta Equivalent", "Path" };
        buildConfigTable = new JBTable(new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        setupTable(buildConfigTable);

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
        String[] columns = { "File", "Line", "Configuration", "Jakarta Equivalent", "Path" };
        configFileTable = new JBTable(new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        setupTable(configFileTable);

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
        String[] columns = { "File", "Line", "Deprecated API", "Jakarta Equivalent", "Path" };
        deprecatedApiTable = new JBTable(new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        setupTable(deprecatedApiTable);

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
        String[] columns = { "File", "Line", "CDI Injection", "Jakarta Equivalent", "Path" };
        cdiInjectionTable = new JBTable(new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        setupTable(cdiInjectionTable);

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
        String[] columns = { "File", "Line", "REST/SOAP", "Jakarta Equivalent", "Path" };
        restSoapTable = new JBTable(new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        setupTable(restSoapTable);

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
        String[] columns = { "File", "Line", "Security API", "Jakarta Equivalent", "Path" };
        securityApiTable = new JBTable(new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        setupTable(securityApiTable);

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
        String[] columns = { "File", "Line", "JMS Messaging", "Jakarta Equivalent", "Path" };
        jmsMessagingTable = new JBTable(new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        setupTable(jmsMessagingTable);

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
        String[] columns = { "File", "Line", "Classloader/Module", "Jakarta Equivalent", "Path" };
        classloaderModuleTable = new JBTable(new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        setupTable(classloaderModuleTable);

        panel.add(classloaderModuleStatusLabel, BorderLayout.NORTH);
        panel.add(new JScrollPane(classloaderModuleTable), BorderLayout.CENTER);

        return panel;
    }

    private JPanel createLoggingMetricsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Status label
        loggingMetricsStatusLabel = new JLabel("Not scanned yet");
        loggingMetricsStatusLabel.setForeground(Color.GRAY);

        // Table
        String[] columns = { "File", "Line", "Usage Type", "Replacement", "Path" };
        loggingMetricsTable = new JBTable(new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        setupTable(loggingMetricsTable);

        panel.add(loggingMetricsStatusLabel, BorderLayout.NORTH);
        panel.add(new JScrollPane(loggingMetricsTable), BorderLayout.CENTER);

        return panel;
    }

    private JPanel createSerializationCachePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Status label
        serializationCacheStatusLabel = new JLabel("Not scanned yet");
        serializationCacheStatusLabel.setForeground(Color.GRAY);

        // Table
        String[] columns = { "File", "Line", "Usage Type", "Risk Assessment", "Path" };
        serializationCacheTable = new JBTable(new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        setupTable(serializationCacheTable);

        panel.add(serializationCacheStatusLabel, BorderLayout.NORTH);
        panel.add(new JScrollPane(serializationCacheTable), BorderLayout.CENTER);

        return panel;
    }

    private JPanel createThirdPartyLibPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Status label
        thirdPartyLibStatusLabel = new JLabel("Not scanned yet");
        thirdPartyLibStatusLabel.setForeground(Color.GRAY);

        // Table
        String[] columns = { "Library", "GroupId:ArtifactId", "Issue Type", "Suggested Replacement", "Path" };
        thirdPartyLibTable = new JBTable(new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        setupTable(thirdPartyLibTable);

        panel.add(thirdPartyLibStatusLabel, BorderLayout.NORTH);
        panel.add(new JScrollPane(thirdPartyLibTable), BorderLayout.CENTER);

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
        SwingWorker<AdvancedScanningService.AdvancedScanSummary, Void> worker = new SwingWorker<>() {
            @Override
            protected AdvancedScanningService.AdvancedScanSummary doInBackground() {
                return scanningService.scanAll(projectPath);
            }

            @Override
            protected void done() {
                try {
                    AdvancedScanningService.AdvancedScanSummary summary = get();

                    // Save to database
                    String stateJson = objectMapper.toJson(summary);
                    store.savePluginState(projectPath, "advancedScansSummary", stateJson);

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

    private void updateTabTitle(int index, String baseTitle, int issueCount) {
        if (index >= 0 && index < tabbedPane.getTabCount()) {
            if (issueCount > 0) {
                tabbedPane.setTitleAt(index, baseTitle + " [" + issueCount + "]");
                tabbedPane.setForegroundAt(index, new Color(200, 0, 0)); // Red for issues
            } else {
                tabbedPane.setTitleAt(index, baseTitle);
                tabbedPane.setForegroundAt(index, new Color(0, 150, 0)); // Green for none
            }
        }
    }

    private void displayResults(AdvancedScanningService.AdvancedScanSummary summary) {
        // Display JPA results
        JpaProjectScanResult jpaResult = summary.jpaResult();
        int jpaIssues = 0;
        if (jpaResult != null) {
            displayJpaResults(jpaResult);
            jpaIssues = jpaResult.hasJavaxUsage() ? jpaResult.totalAnnotationsFound() : 0;
        }
        updateTabTitle(0, "JPA Annotations", jpaIssues);

        // Display Bean Validation results
        BeanValidationProjectScanResult beanValidationResult = summary.beanValidationResult();
        int bvIssues = 0;
        if (beanValidationResult != null) {
            displayBeanValidationResults(beanValidationResult);
            bvIssues = beanValidationResult.hasJavaxUsage() ? beanValidationResult.totalAnnotationsFound() : 0;
        }
        updateTabTitle(1, "Bean Validation", bvIssues);

        // Display Servlet/JSP results
        ServletJspProjectScanResult servletJspResult = summary.servletJspResult();
        int servletJspIssues = 0;
        if (servletJspResult != null) {
            displayServletJspResults(servletJspResult);
            servletJspIssues = servletJspResult.hasJavaxUsage() ? servletJspResult.totalUsagesFound() : 0;
        }
        updateTabTitle(2, "Servlet/JSP", servletJspIssues);

        // Display Build Config results
        BuildConfigProjectScanResult buildConfigResult = summary.buildConfigResult();
        int buildConfigIssues = 0;
        if (buildConfigResult != null) {
            displayBuildConfigResults(buildConfigResult);
            buildConfigIssues = buildConfigResult.hasJavaxDependencies() ? buildConfigResult.totalDependenciesFound()
                    : 0;
        }
        updateTabTitle(3, "Build Config", buildConfigIssues);

        // Display Config File results
        ConfigFileProjectScanResult configFileResult = summary.configFileResult();
        int configFileIssues = 0;
        if (configFileResult != null) {
            displayConfigFileResults(configFileResult);
            configFileIssues = configFileResult.hasJavaxUsage() ? configFileResult.getTotalJavaxUsages() : 0;
        }
        updateTabTitle(4, "Config Files", configFileIssues);

        // Display Deprecated API results
        DeprecatedApiProjectScanResult deprecatedApiResult = summary.deprecatedApiResult();
        int deprecatedApiIssues = 0;
        if (deprecatedApiResult != null) {
            displayDeprecatedApiResults(deprecatedApiResult);
            deprecatedApiIssues = deprecatedApiResult.hasDeprecatedApiUsage() ? deprecatedApiResult.totalUsagesFound()
                    : 0;
        }
        updateTabTitle(5, "Deprecated API", deprecatedApiIssues);

        // Display CDI Injection results
        CdiInjectionProjectScanResult cdiInjectionResult = summary.cdiInjectionResult();
        int cdiIssues = 0;
        if (cdiInjectionResult != null) {
            displayCdiInjectionResults(cdiInjectionResult);
            cdiIssues = cdiInjectionResult.hasJavaxUsage() ? cdiInjectionResult.totalAnnotationsFound() : 0;
        }
        updateTabTitle(6, "CDI Injection", cdiIssues);

        // Display REST/SOAP results
        RestSoapProjectScanResult restSoapResult = summary.restSoapResult();
        int restSoapIssues = 0;
        if (restSoapResult != null) {
            displayRestSoapResults(restSoapResult);
            restSoapIssues = restSoapResult.hasJavaxUsage() ? restSoapResult.totalUsagesFound() : 0;
        }
        updateTabTitle(7, "REST/SOAP", restSoapIssues);

        // Display Security API results
        SecurityApiProjectScanResult securityApiResult = summary.securityApiResult();
        int securityApiIssues = 0;
        if (securityApiResult != null) {
            displaySecurityApiResults(securityApiResult);
            securityApiIssues = securityApiResult.hasJavaxUsage() ? securityApiResult.getTotalJavaxUsages() : 0;
        }
        updateTabTitle(8, "Security API", securityApiIssues);

        // Display JMS Messaging results
        JmsMessagingProjectScanResult jmsMessagingResult = summary.jmsMessagingResult();
        int jmsMessagingIssues = 0;
        if (jmsMessagingResult != null) {
            displayJmsMessagingResults(jmsMessagingResult);
            jmsMessagingIssues = jmsMessagingResult.hasJavaxUsage() ? jmsMessagingResult.getTotalJavaxUsages() : 0;
        }
        updateTabTitle(9, "JMS Messaging", jmsMessagingIssues);

        // Display Classloader/Module results
        ClassloaderModuleProjectScanResult classloaderModuleResult = summary.classloaderModuleResult();
        int classloaderModuleIssues = 0;
        if (classloaderModuleResult != null) {
            displayClassloaderModuleResults(classloaderModuleResult);
            classloaderModuleIssues = classloaderModuleResult.hasJavaxUsage()
                    ? classloaderModuleResult.getTotalJavaxUsages()
                    : 0;
        }
        updateTabTitle(10, "Classloader", classloaderModuleIssues);

        // Display Logging/Metrics results
        LoggingMetricsProjectScanResult loggingMetricsResult = summary.loggingMetricsResult();
        int loggingMetricsIssues = 0;
        if (loggingMetricsResult != null) {
            displayLoggingMetricsResults(loggingMetricsResult);
            loggingMetricsIssues = loggingMetricsResult.hasFindings() ? loggingMetricsResult.getTotalFindings() : 0;
        }
        updateTabTitle(11, "Logging/Metrics", loggingMetricsIssues);

        // Display Serialization/Cache results
        SerializationCacheProjectScanResult serializationCacheResult = summary.serializationCacheResult();
        int serializationCacheIssues = 0;
        if (serializationCacheResult != null) {
            displaySerializationCacheResults(serializationCacheResult);
            serializationCacheIssues = serializationCacheResult.hasFindings()
                    ? serializationCacheResult.getTotalFindings()
                    : 0;
        }
        updateTabTitle(12, "Serialization/Cache", serializationCacheIssues);

        // Display Third-Party Libs results
        ThirdPartyLibProjectScanResult thirdPartyLibResult = summary.thirdPartyLibResult();
        int thirdPartyLibIssues = 0;
        if (thirdPartyLibResult != null) {
            displayThirdPartyLibResults(thirdPartyLibResult);
            thirdPartyLibIssues = thirdPartyLibResult.hasFindings() ? thirdPartyLibResult.getTotalLibraries() : 0;
        }
        updateTabTitle(13, "Third-Party Libs", thirdPartyLibIssues);

        // Notify listener if present
        notifyScanComplete();
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
                    model.addRow(new Object[] {
                            fileResult.filePath().getFileName(),
                            annotation.lineNumber(),
                            annotation.annotationName(),
                            annotation.jakartaEquivalent(),
                            fileResult.filePath().toString()
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
                    model.addRow(new Object[] {
                            fileResult.filePath().getFileName(),
                            annotation.lineNumber(),
                            annotation.annotationName(),
                            annotation.jakartaEquivalent(),
                            fileResult.filePath().toString()
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
                    model.addRow(new Object[] {
                            fileResult.filePath().getFileName(),
                            usage.lineNumber(),
                            usage.className(),
                            usage.usageType(),
                            usage.jakartaEquivalent(),
                            fileResult.filePath().toString()
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

            for (var fileResult : result.fileResults()) {
                for (var usage : fileResult.usages()) {
                    model.addRow(new Object[] {
                            fileResult.filePath().getFileName(),
                            usage.lineNumber(),
                            usage.groupId() + ":" + usage.artifactId(),
                            usage.jakartaGroupId() + ":" + usage.jakartaArtifactId(),
                            fileResult.filePath().toString()
                    });
                }
            }
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

            for (var fileResult : result.getFileResults()) {
                for (var usage : fileResult.getUsages()) {
                    model.addRow(new Object[] {
                            fileResult.getFilePath().getFileName(),
                            usage.getLineNumber(),
                            usage.getJavaxReference(),
                            usage.getReplacement(),
                            fileResult.getFilePath().toString()
                    });
                }
            }
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

            for (var fileResult : result.fileResults()) {
                for (var usage : fileResult.usages()) {
                    model.addRow(new Object[] {
                            fileResult.filePath().getFileName(),
                            usage.lineNumber(),
                            usage.className() + (usage.methodName() != null && !usage.methodName().isEmpty()
                                    ? "." + usage.methodName()
                                    : ""),
                            usage.jakartaEquivalent(),
                            fileResult.filePath().toString()
                    });
                }
            }
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

            for (var fileResult : result.fileResults()) {
                for (var usage : fileResult.usages()) {
                    model.addRow(new Object[] {
                            fileResult.filePath().getFileName(),
                            usage.lineNumber(),
                            usage.className(),
                            usage.jakartaEquivalent(),
                            fileResult.filePath().toString()
                    });
                }
            }
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

            for (var fileResult : result.fileResults()) {
                for (var usage : fileResult.usages()) {
                    model.addRow(new Object[] {
                            fileResult.filePath().getFileName(),
                            usage.lineNumber(),
                            usage.className(),
                            usage.jakartaEquivalent(),
                            fileResult.filePath().toString()
                    });
                }
            }
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

            for (var fileResult : result.getFileResults()) {
                for (var usage : fileResult.getUsages()) {
                    model.addRow(new Object[] {
                            fileResult.getFilePath().getFileName(),
                            usage.getLineNumber(),
                            usage.getJavaxClass(),
                            usage.getJakartaEquivalent(),
                            fileResult.getFilePath().toString()
                    });
                }
            }
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

            for (var fileResult : result.getFileResults()) {
                for (var usage : fileResult.getUsages()) {
                    model.addRow(new Object[] {
                            fileResult.getFilePath().getFileName(),
                            usage.getLineNumber(),
                            usage.getJavaxClass(),
                            usage.getJakartaEquivalent(),
                            fileResult.getFilePath().toString()
                    });
                }
            }
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

            for (var fileResult : result.getFileResults()) {
                for (var usage : fileResult.getUsages()) {
                    model.addRow(new Object[] {
                            fileResult.getFilePath().getFileName(),
                            usage.getLineNumber(),
                            usage.getJavaxClass(),
                            usage.getReplacement(),
                            fileResult.getFilePath().toString()
                    });
                }
            }
        } else {
            classloaderModuleStatusLabel.setText("No javax.* classloader/module usage found");
            classloaderModuleStatusLabel.setForeground(new Color(0, 150, 0));
        }
    }

    private void displayLoggingMetricsResults(LoggingMetricsProjectScanResult result) {
        DefaultTableModel model = (DefaultTableModel) loggingMetricsTable.getModel();
        model.setRowCount(0);

        if (result.hasFindings()) {
            loggingMetricsStatusLabel.setText(String.format("Found %d usages in %d files",
                    result.getTotalFindings(), result.getTotalFilesScanned()));
            loggingMetricsStatusLabel.setForeground(new Color(200, 100, 0));

            for (var fileResult : result.getFileResults()) {
                for (var usage : fileResult.getUsages()) {
                    model.addRow(new Object[] {
                            Path.of(fileResult.getFilePath()).getFileName(),
                            usage.getLineNumber(),
                            usage.getUsageType(),
                            usage.getReplacement(),
                            fileResult.getFilePath()
                    });
                }
            }
        } else {
            loggingMetricsStatusLabel.setText("No javax.* logging/metrics usage found");
            loggingMetricsStatusLabel.setForeground(new Color(0, 150, 0));
        }
    }

    private void displaySerializationCacheResults(SerializationCacheProjectScanResult result) {
        DefaultTableModel model = (DefaultTableModel) serializationCacheTable.getModel();
        model.setRowCount(0);

        if (result.hasFindings()) {
            serializationCacheStatusLabel.setText(String.format("Found %d usages in %d files",
                    result.getTotalFindings(), result.getTotalFilesScanned()));
            serializationCacheStatusLabel.setForeground(new Color(200, 100, 0));

            for (var fileResult : result.getFileResults()) {
                boolean firstRow = true;
                for (var usage : fileResult.getUsages()) {
                    model.addRow(new Object[] {
                            firstRow ? Path.of(fileResult.getFilePath()).getFileName() : "",
                            usage.getLineNumber(),
                            usage.getUsageType(),
                            usage.getRiskAssessment(),
                            fileResult.getFilePath()
                    });
                    firstRow = false;
                }
            }
        } else {
            serializationCacheStatusLabel.setText("No javax.* serialization/cache usage found");
            serializationCacheStatusLabel.setForeground(new Color(0, 150, 0));
        }
    }

    private void setupTable(JBTable table) {
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        // Hide the Path column (last column)
        TableColumnModel columnModel = table.getColumnModel();
        int pathColumnIndex = columnModel.getColumnCount() - 1;
        columnModel.removeColumn(columnModel.getColumn(pathColumnIndex));

        // Add double-click listener to open file
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.getSelectedRow();
                    if (row >= 0) {
                        // Get path from model (even if column is hidden in view)
                        String pathStr = (String) table.getModel().getValueAt(row, pathColumnIndex);
                        if (pathStr != null && !pathStr.isEmpty()) {
                            openFileInEditor(pathStr);
                        }
                    }
                }
            }
        });
    }

    private void openFileInEditor(String pathStr) {
        VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(pathStr);
        if (virtualFile != null) {
            FileEditorManager.getInstance(project).openFile(virtualFile, true);
        }
    }

    private void displayThirdPartyLibResults(ThirdPartyLibProjectScanResult result) {
        DefaultTableModel model = (DefaultTableModel) thirdPartyLibTable.getModel();
        model.setRowCount(0);

        if (result.hasFindings()) {
            thirdPartyLibStatusLabel.setText(String.format("Found %d incompatible libraries",
                    result.getTotalLibraries()));
            thirdPartyLibStatusLabel.setForeground(new Color(200, 100, 0));

            for (var usage : result.getLibraries()) {
                model.addRow(new Object[] {
                        usage.getLibraryName(),
                        usage.getGroupId() + ":" + usage.getArtifactId(),
                        usage.getIssueType(),
                        usage.getSuggestedReplacement(),
                        "" // No file path for libraries
                });
            }
        } else {
            thirdPartyLibStatusLabel.setText("No incompatible third-party libraries found");
            thirdPartyLibStatusLabel.setForeground(new Color(0, 150, 0));
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

    public JTabbedPane getTabbedPane() {
        return tabbedPane;
    }

    public JTable getJpaTable() {
        return jpaTable;
    }

    public JLabel getJpaStatusLabel() {
        return jpaStatusLabel;
    }

    public JTable getBeanValidationTable() {
        return beanValidationTable;
    }

    public JLabel getBeanValidationStatusLabel() {
        return beanValidationStatusLabel;
    }

    public JTable getServletJspTable() {
        return servletJspTable;
    }

    public JLabel getServletJspStatusLabel() {
        return servletJspStatusLabel;
    }

    public JButton getScanButton() {
        return scanButton;
    }

    public JProgressBar getProgressBar() {
        return progressBar;
    }

    public interface ScanCompletionListener {
        void onScanComplete();
    }

    private final List<ScanCompletionListener> listeners = new ArrayList<>();

    public void addScanCompletionListener(ScanCompletionListener listener) {
        listeners.add(listener);
    }

    private void notifyScanComplete() {
        for (ScanCompletionListener listener : listeners) {
            listener.onScanComplete();
        }
    }

    private void loadInitialState() {
        String projectPathStr = project.getBasePath();
        if (projectPathStr == null) {
            projectPathStr = project.getProjectFilePath();
        }
        if (projectPathStr != null) {
            Path projectPath = Path.of(projectPathStr);
            String stateJson = store.getPluginState(projectPath, "advancedScansSummary");
            if (stateJson != null && !stateJson.isEmpty()) {
                try {
                    AdvancedScanningService.AdvancedScanSummary summary = objectMapper.fromJson(stateJson,
                            AdvancedScanningService.AdvancedScanSummary.class);
                    if (summary != null) {
                        displayResults(summary);
                    }
                } catch (Exception e) {
                    LOG.error("Failed to load initial state for AdvancedScansComponent", e);
                }
            }
        }
    }
}
