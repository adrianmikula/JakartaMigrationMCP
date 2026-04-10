package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.advancedscanning.domain.*;
import adrianmikula.jakartamigration.intellij.service.AdvancedScanningService;
import adrianmikula.jakartamigration.analysis.persistence.CentralMigrationAnalysisStore;
import adrianmikula.jakartamigration.analysis.persistence.ObjectMapperService;
import adrianmikula.jakartamigration.intellij.ui.components.TruncationHelper;
import adrianmikula.jakartamigration.intellij.ui.components.TruncationNoticePanel;
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
 * Available for all users - free users are limited by advanced scan credits.
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
    private final TruncationHelper truncationHelper;

    // Truncation notice panels for each sub-tab
    private TruncationNoticePanel jpaTruncationNotice;
    private TruncationNoticePanel beanValidationTruncationNotice;
    private TruncationNoticePanel servletJspTruncationNotice;
    private TruncationNoticePanel buildConfigTruncationNotice;
    private TruncationNoticePanel configFileTruncationNotice;
    private TruncationNoticePanel deprecatedApiTruncationNotice;
    private TruncationNoticePanel cdiInjectionTruncationNotice;
    private TruncationNoticePanel restSoapTruncationNotice;
    private TruncationNoticePanel securityApiTruncationNotice;
    private TruncationNoticePanel jmsMessagingTruncationNotice;
    private TruncationNoticePanel classloaderModuleTruncationNotice;
    private TruncationNoticePanel loggingMetricsTruncationNotice;
    private TruncationNoticePanel serializationCacheTruncationNotice;
    private TruncationNoticePanel thirdPartyLibTruncationNotice;

    public AdvancedScansComponent(Project project, AdvancedScanningService scanningService) {
        this.project = project;
        this.scanningService = scanningService;
        this.store = new CentralMigrationAnalysisStore();
        this.objectMapper = new ObjectMapperService();
        this.truncationHelper = new TruncationHelper();
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

        progressBar = new JProgressBar(0, 14);
        progressBar.setValue(0);
        progressBar.setVisible(false);
        progressBar.setPreferredSize(new Dimension(150, 20));
        progressBar.setStringPainted(true);
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

        // Truncation notice
        jpaTruncationNotice = new TruncationNoticePanel();
        jpaTruncationNotice.setVisible(false);

        panel.add(jpaStatusLabel, BorderLayout.NORTH);
        panel.add(new JScrollPane(jpaTable), BorderLayout.CENTER);
        panel.add(jpaTruncationNotice, BorderLayout.SOUTH);

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

        // Truncation notice
        beanValidationTruncationNotice = new TruncationNoticePanel();
        beanValidationTruncationNotice.setVisible(false);

        panel.add(beanValidationStatusLabel, BorderLayout.NORTH);
        panel.add(new JScrollPane(beanValidationTable), BorderLayout.CENTER);
        panel.add(beanValidationTruncationNotice, BorderLayout.SOUTH);

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

        // Truncation notice
        servletJspTruncationNotice = new TruncationNoticePanel();
        servletJspTruncationNotice.setVisible(false);

        panel.add(servletJspStatusLabel, BorderLayout.NORTH);
        panel.add(new JScrollPane(servletJspTable), BorderLayout.CENTER);
        panel.add(servletJspTruncationNotice, BorderLayout.SOUTH);

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

        // Truncation notice
        buildConfigTruncationNotice = new TruncationNoticePanel();
        buildConfigTruncationNotice.setVisible(false);

        panel.add(buildConfigStatusLabel, BorderLayout.NORTH);
        panel.add(new JScrollPane(buildConfigTable), BorderLayout.CENTER);
        panel.add(buildConfigTruncationNotice, BorderLayout.SOUTH);

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

        // Truncation notice
        configFileTruncationNotice = new TruncationNoticePanel();
        configFileTruncationNotice.setVisible(false);

        panel.add(configFileStatusLabel, BorderLayout.NORTH);
        panel.add(new JScrollPane(configFileTable), BorderLayout.CENTER);
        panel.add(configFileTruncationNotice, BorderLayout.SOUTH);

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

        // Truncation notice
        deprecatedApiTruncationNotice = new TruncationNoticePanel();
        deprecatedApiTruncationNotice.setVisible(false);

        panel.add(deprecatedApiStatusLabel, BorderLayout.NORTH);
        panel.add(new JScrollPane(deprecatedApiTable), BorderLayout.CENTER);
        panel.add(deprecatedApiTruncationNotice, BorderLayout.SOUTH);

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

        // Truncation notice
        cdiInjectionTruncationNotice = new TruncationNoticePanel();
        cdiInjectionTruncationNotice.setVisible(false);

        panel.add(cdiInjectionStatusLabel, BorderLayout.NORTH);
        panel.add(new JScrollPane(cdiInjectionTable), BorderLayout.CENTER);
        panel.add(cdiInjectionTruncationNotice, BorderLayout.SOUTH);

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

        // Truncation notice
        restSoapTruncationNotice = new TruncationNoticePanel();
        restSoapTruncationNotice.setVisible(false);

        panel.add(restSoapStatusLabel, BorderLayout.NORTH);
        panel.add(new JScrollPane(restSoapTable), BorderLayout.CENTER);
        panel.add(restSoapTruncationNotice, BorderLayout.SOUTH);

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

        // Truncation notice
        securityApiTruncationNotice = new TruncationNoticePanel();
        securityApiTruncationNotice.setVisible(false);

        panel.add(securityApiStatusLabel, BorderLayout.NORTH);
        panel.add(new JScrollPane(securityApiTable), BorderLayout.CENTER);
        panel.add(securityApiTruncationNotice, BorderLayout.SOUTH);

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

        // Truncation notice
        jmsMessagingTruncationNotice = new TruncationNoticePanel();
        jmsMessagingTruncationNotice.setVisible(false);

        panel.add(jmsMessagingStatusLabel, BorderLayout.NORTH);
        panel.add(new JScrollPane(jmsMessagingTable), BorderLayout.CENTER);
        panel.add(jmsMessagingTruncationNotice, BorderLayout.SOUTH);

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

        // Truncation notice
        classloaderModuleTruncationNotice = new TruncationNoticePanel();
        classloaderModuleTruncationNotice.setVisible(false);

        panel.add(classloaderModuleStatusLabel, BorderLayout.NORTH);
        panel.add(new JScrollPane(classloaderModuleTable), BorderLayout.CENTER);
        panel.add(classloaderModuleTruncationNotice, BorderLayout.SOUTH);

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

        // Truncation notice
        loggingMetricsTruncationNotice = new TruncationNoticePanel();
        loggingMetricsTruncationNotice.setVisible(false);

        panel.add(loggingMetricsStatusLabel, BorderLayout.NORTH);
        panel.add(new JScrollPane(loggingMetricsTable), BorderLayout.CENTER);
        panel.add(loggingMetricsTruncationNotice, BorderLayout.SOUTH);

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

        // Truncation notice
        serializationCacheTruncationNotice = new TruncationNoticePanel();
        serializationCacheTruncationNotice.setVisible(false);

        panel.add(serializationCacheStatusLabel, BorderLayout.NORTH);
        panel.add(new JScrollPane(serializationCacheTable), BorderLayout.CENTER);
        panel.add(serializationCacheTruncationNotice, BorderLayout.SOUTH);

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

        // Truncation notice
        thirdPartyLibTruncationNotice = new TruncationNoticePanel();
        thirdPartyLibTruncationNotice.setVisible(false);

        panel.add(thirdPartyLibStatusLabel, BorderLayout.NORTH);
        panel.add(new JScrollPane(thirdPartyLibTable), BorderLayout.CENTER);
        panel.add(thirdPartyLibTruncationNotice, BorderLayout.SOUTH);

        return panel;
    }

    private void runScans() {
        // Advanced scans are now 100% free - truncation mode applies when credits exhausted
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
        progressBar.setValue(0);
        progressBar.setVisible(true);
        scanButton.setEnabled(false);

        // Progress simulation timer
        final int totalScans = 14;
        final int[] currentScan = {0};
        Timer progressTimer = new Timer(300, null);
        progressTimer.addActionListener(e -> {
            currentScan[0]++;
            if (currentScan[0] >= totalScans) {
                currentScan[0] = totalScans - 1;
            }
            progressBar.setValue(currentScan[0]);
            progressBar.setString("Scanning... " + currentScan[0] + "/" + totalScans);
        });
        progressTimer.setInitialDelay(0);
        progressTimer.start();

        // Run scans in background
        SwingWorker<AdvancedScanningService.AdvancedScanSummary, Void> worker = new SwingWorker<>() {
            @Override
            protected AdvancedScanningService.AdvancedScanSummary doInBackground() {
                return scanningService.scanAll(projectPath);
            }

            @Override
            protected void done() {
                progressTimer.stop();
                try {
                    AdvancedScanningService.AdvancedScanSummary summary = get();

                    // Save to database
                    String stateJson = objectMapper.toJson(summary);
                    store.savePluginState(projectPath, "advancedScansSummary", stateJson);

                    progressBar.setValue(totalScans);
                    progressBar.setString("Complete!");
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
        ProjectScanResult<FileScanResult<JpaAnnotationUsage>> jpaResult = summary.jpaResult();
        int jpaIssues = 0;
        if (jpaResult != null) {
            displayJpaResults(jpaResult);
            jpaIssues = jpaResult.hasIssues() ? jpaResult.totalIssuesFound() : 0;
        }
        updateTabTitle(0, "JPA Annotations", jpaIssues);

        // Display Bean Validation results
        ProjectScanResult<FileScanResult<JavaxUsage>> beanValidationResult = summary.beanValidationResult();
        int bvIssues = 0;
        if (beanValidationResult != null) {
            displayBeanValidationResults(beanValidationResult);
            bvIssues = beanValidationResult.hasIssues() ? beanValidationResult.totalIssuesFound() : 0;
        }
        updateTabTitle(1, "Bean Validation", bvIssues);

        // Display Servlet/JSP results
        ProjectScanResult<FileScanResult<ServletJspUsage>> servletJspResult = summary.servletJspResult();
        int servletJspIssues = 0;
        if (servletJspResult != null) {
            displayServletJspResults(servletJspResult);
            servletJspIssues = servletJspResult.hasIssues() ? servletJspResult.totalIssuesFound() : 0;
        }
        updateTabTitle(2, "Servlet/JSP", servletJspIssues);

        // Display Build Config results
        ProjectScanResult<FileScanResult<BuildConfigUsage>> buildConfigResult = summary.buildConfigResult();
        int buildConfigIssues = 0;
        if (buildConfigResult != null) {
            displayBuildConfigResults(buildConfigResult);
            buildConfigIssues = buildConfigResult.hasIssues() ? buildConfigResult.totalIssuesFound() : 0;
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
        ProjectScanResult<FileScanResult<JavaxUsage>> cdiInjectionResult = summary.cdiInjectionResult();
        int cdiIssues = 0;
        if (cdiInjectionResult != null) {
            displayCdiInjectionResults(cdiInjectionResult);
            cdiIssues = cdiInjectionResult.hasIssues() ? cdiInjectionResult.totalIssuesFound() : 0;
        }
        updateTabTitle(6, "CDI Injection", cdiIssues);

        // Display REST/SOAP results
        ProjectScanResult<FileScanResult<JavaxUsage>> restSoapResult = summary.restSoapResult();
        int restSoapIssues = 0;
        if (restSoapResult != null) {
            displayRestSoapResults(restSoapResult);
            restSoapIssues = restSoapResult.hasIssues() ? restSoapResult.totalIssuesFound() : 0;
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

    private void displayJpaResults(ProjectScanResult<FileScanResult<JpaAnnotationUsage>> result) {
        DefaultTableModel model = (DefaultTableModel) jpaTable.getModel();
        model.setRowCount(0);

        if (result.hasIssues()) {
            boolean shouldTruncate = truncationHelper.shouldTruncateResults();
            int truncationLimit = shouldTruncate ? truncationHelper.getAdvancedScanTruncationLimit() : Integer.MAX_VALUE;
            int addedCount = 0;

            for (var fileResult : result.fileResults()) {
                for (var annotation : fileResult.usages()) {
                    if (addedCount < truncationLimit) {
                        model.addRow(new Object[] {
                                fileResult.filePath().getFileName(),
                                annotation.lineNumber(),
                                annotation.annotationName(),
                                annotation.jakartaEquivalent(),
                                fileResult.filePath().toString()
                        });
                        addedCount++;
                    }
                }
            }

            // Update status label with truncation info
            int totalCount = result.totalIssuesFound();
            if (shouldTruncate && totalCount > truncationLimit) {
                jpaStatusLabel.setText(String.format("Found %d of %d annotations in %d files",
                        addedCount, totalCount, result.filesWithIssues()));
                jpaTruncationNotice.updateMessage(addedCount, totalCount, "annotations");
            } else {
                jpaStatusLabel.setText(String.format("Found %d annotations in %d files",
                        totalCount, result.filesWithIssues()));
                jpaTruncationNotice.setVisible(false);
            }
            jpaStatusLabel.setForeground(new Color(200, 100, 0));
        } else {
            jpaStatusLabel.setText("No javax.persistence.* usage found");
            jpaStatusLabel.setForeground(new Color(0, 150, 0));
            jpaTruncationNotice.setVisible(false);
        }
    }

    private void displayBeanValidationResults(ProjectScanResult<FileScanResult<JavaxUsage>> result) {
        DefaultTableModel model = (DefaultTableModel) beanValidationTable.getModel();
        model.setRowCount(0);

        if (result.hasIssues()) {
            boolean shouldTruncate = truncationHelper.shouldTruncateResults();
            int truncationLimit = shouldTruncate ? truncationHelper.getAdvancedScanTruncationLimit() : Integer.MAX_VALUE;
            int addedCount = 0;

            for (var fileResult : result.fileResults()) {
                for (var usage : fileResult.usages()) {
                    if (addedCount < truncationLimit) {
                        model.addRow(new Object[] {
                                fileResult.filePath().getFileName(),
                                usage.lineNumber(),
                                usage.className(),
                                usage.jakartaEquivalent(),
                                fileResult.filePath().toString()
                        });
                        addedCount++;
                    }
                }
            }

            // Update status label with truncation info
            int totalCount = result.totalIssuesFound();
            if (shouldTruncate && totalCount > truncationLimit) {
                beanValidationStatusLabel.setText(String.format("Found %d of %d constraints in %d files",
                        addedCount, totalCount, result.filesWithIssues()));
                beanValidationTruncationNotice.updateMessage(addedCount, totalCount, "constraints");
            } else {
                beanValidationStatusLabel.setText(String.format("Found %d constraints in %d files",
                        totalCount, result.filesWithIssues()));
                beanValidationTruncationNotice.setVisible(false);
            }
            beanValidationStatusLabel.setForeground(new Color(200, 100, 0));
        } else {
            beanValidationStatusLabel.setText("No javax.validation.* usage found");
            beanValidationStatusLabel.setForeground(new Color(0, 150, 0));
            beanValidationTruncationNotice.setVisible(false);
        }
    }

    private void displayServletJspResults(ProjectScanResult<FileScanResult<ServletJspUsage>> result) {
        DefaultTableModel model = (DefaultTableModel) servletJspTable.getModel();
        model.setRowCount(0);

        if (result.hasIssues()) {
            boolean shouldTruncate = truncationHelper.shouldTruncateResults();
            int truncationLimit = shouldTruncate ? truncationHelper.getAdvancedScanTruncationLimit() : Integer.MAX_VALUE;
            int addedCount = 0;

            for (var fileResult : result.fileResults()) {
                for (var usage : fileResult.usages()) {
                    if (addedCount < truncationLimit) {
                        model.addRow(new Object[] {
                                fileResult.filePath().getFileName(),
                                usage.lineNumber(),
                                usage.className(),
                                usage.usageType(),
                                usage.jakartaEquivalent(),
                                fileResult.filePath().toString()
                        });
                        addedCount++;
                    }
                }
            }

            // Update status label with truncation info
            int totalCount = result.totalIssuesFound();
            if (shouldTruncate && totalCount > truncationLimit) {
                servletJspStatusLabel.setText(String.format("Found %d of %d usages in %d files",
                        addedCount, totalCount, result.filesWithIssues()));
                servletJspTruncationNotice.updateMessage(addedCount, totalCount, "usages");
            } else {
                servletJspStatusLabel.setText(String.format("Found %d usages in %d files",
                        totalCount, result.filesWithIssues()));
                servletJspTruncationNotice.setVisible(false);
            }
            servletJspStatusLabel.setForeground(new Color(200, 100, 0));
        } else {
            servletJspStatusLabel.setText("No javax.servlet.* usage found");
            servletJspStatusLabel.setForeground(new Color(0, 150, 0));
            servletJspTruncationNotice.setVisible(false);
        }
    }

    private void displayBuildConfigResults(ProjectScanResult<FileScanResult<BuildConfigUsage>> result) {
        DefaultTableModel model = (DefaultTableModel) buildConfigTable.getModel();
        model.setRowCount(0);

        if (result.hasIssues()) {
            boolean shouldTruncate = truncationHelper.shouldTruncateResults();
            int truncationLimit = shouldTruncate ? truncationHelper.getAdvancedScanTruncationLimit() : Integer.MAX_VALUE;
            int addedCount = 0;

            for (var fileResult : result.fileResults()) {
                for (var usage : fileResult.usages()) {
                    if (addedCount < truncationLimit) {
                        model.addRow(new Object[] {
                                fileResult.filePath().getFileName(),
                                usage.lineNumber(),
                                usage.groupId() + ":" + usage.artifactId(),
                                usage.jakartaGroupId() + ":" + usage.jakartaArtifactId(),
                                fileResult.filePath().toString()
                        });
                        addedCount++;
                    }
                }
            }

            // Update status label with truncation info
            int totalCount = result.totalIssuesFound();
            if (shouldTruncate && totalCount > truncationLimit) {
                buildConfigStatusLabel.setText(String.format("Found %d of %d dependencies in %d files",
                        addedCount, totalCount, result.filesWithIssues()));
                buildConfigTruncationNotice.updateMessage(addedCount, totalCount, "dependencies");
            } else {
                buildConfigStatusLabel.setText(String.format("Found %d dependencies in %d files",
                        totalCount, result.filesWithIssues()));
                buildConfigTruncationNotice.setVisible(false);
            }
            buildConfigStatusLabel.setForeground(new Color(200, 100, 0));
        } else {
            buildConfigStatusLabel.setText("No javax.* build configuration found");
            buildConfigStatusLabel.setForeground(new Color(0, 150, 0));
            buildConfigTruncationNotice.setVisible(false);
        }
    }

    private void displayConfigFileResults(ConfigFileProjectScanResult result) {
        DefaultTableModel model = (DefaultTableModel) configFileTable.getModel();
        model.setRowCount(0);

        if (result.hasJavaxUsage()) {
            boolean shouldTruncate = truncationHelper.shouldTruncateResults();
            int truncationLimit = shouldTruncate ? truncationHelper.getAdvancedScanTruncationLimit() : Integer.MAX_VALUE;
            int addedCount = 0;

            for (var fileResult : result.getFileResults()) {
                for (var usage : fileResult.getUsages()) {
                    if (addedCount < truncationLimit) {
                        model.addRow(new Object[] {
                                fileResult.getFilePath().getFileName(),
                                usage.getLineNumber(),
                                usage.getJavaxReference(),
                                usage.getReplacement(),
                                fileResult.getFilePath().toString()
                        });
                        addedCount++;
                    }
                }
            }

            // Update status label with truncation info
            int totalCount = result.getTotalJavaxUsages();
            if (shouldTruncate && totalCount > truncationLimit) {
                configFileStatusLabel.setText(String.format("Found %d of %d usages in %d files",
                        addedCount, totalCount, result.getFilesWithJavaxUsage()));
                configFileTruncationNotice.updateMessage(addedCount, totalCount, "usages");
            } else {
                configFileStatusLabel.setText(String.format("Found %d usages in %d files",
                        totalCount, result.getFilesWithJavaxUsage()));
                configFileTruncationNotice.setVisible(false);
            }
            configFileStatusLabel.setForeground(new Color(200, 100, 0));
        } else {
            configFileStatusLabel.setText("No javax.* configuration found");
            configFileStatusLabel.setForeground(new Color(0, 150, 0));
            configFileTruncationNotice.setVisible(false);
        }
    }

    private void displayDeprecatedApiResults(DeprecatedApiProjectScanResult result) {
        DefaultTableModel model = (DefaultTableModel) deprecatedApiTable.getModel();
        model.setRowCount(0);

        if (result.hasDeprecatedApiUsage()) {
            boolean shouldTruncate = truncationHelper.shouldTruncateResults();
            int truncationLimit = shouldTruncate ? truncationHelper.getAdvancedScanTruncationLimit() : Integer.MAX_VALUE;
            int addedCount = 0;

            for (var fileResult : result.fileResults()) {
                for (var usage : fileResult.usages()) {
                    if (addedCount < truncationLimit) {
                        model.addRow(new Object[] {
                                fileResult.filePath().getFileName(),
                                usage.lineNumber(),
                                usage.className() + (usage.methodName() != null && !usage.methodName().isEmpty()
                                        ? "." + usage.methodName()
                                        : ""),
                                usage.jakartaEquivalent(),
                                fileResult.filePath().toString()
                        });
                        addedCount++;
                    }
                }
            }

            // Update status label with truncation info
            int totalCount = result.totalUsagesFound();
            if (shouldTruncate && totalCount > truncationLimit) {
                deprecatedApiStatusLabel.setText(String.format("Found %d of %d usages in %d files",
                        addedCount, totalCount, result.totalFilesWithDeprecatedApi()));
                deprecatedApiTruncationNotice.updateMessage(addedCount, totalCount, "usages");
            } else {
                deprecatedApiStatusLabel.setText(String.format("Found %d usages in %d files",
                        totalCount, result.totalFilesWithDeprecatedApi()));
                deprecatedApiTruncationNotice.setVisible(false);
            }
            deprecatedApiStatusLabel.setForeground(new Color(200, 100, 0));
        } else {
            deprecatedApiStatusLabel.setText("No deprecated javax.* API found");
            deprecatedApiStatusLabel.setForeground(new Color(0, 150, 0));
            deprecatedApiTruncationNotice.setVisible(false);
        }
    }

    private void displayCdiInjectionResults(ProjectScanResult<FileScanResult<JavaxUsage>> result) {
        DefaultTableModel model = (DefaultTableModel) cdiInjectionTable.getModel();
        model.setRowCount(0);

        if (result.hasIssues()) {
            boolean shouldTruncate = truncationHelper.shouldTruncateResults();
            int truncationLimit = shouldTruncate ? truncationHelper.getAdvancedScanTruncationLimit() : Integer.MAX_VALUE;
            int addedCount = 0;

            for (var fileResult : result.fileResults()) {
                for (var usage : fileResult.usages()) {
                    if (addedCount < truncationLimit) {
                        model.addRow(new Object[] {
                                fileResult.filePath().getFileName(),
                                usage.lineNumber(),
                                usage.className(),
                                usage.jakartaEquivalent(),
                                fileResult.filePath().toString()
                        });
                        addedCount++;
                    }
                }
            }

            // Update status label with truncation info
            int totalCount = result.totalIssuesFound();
            if (shouldTruncate && totalCount > truncationLimit) {
                cdiInjectionStatusLabel.setText(String.format("Found %d of %d usages in %d files",
                        addedCount, totalCount, result.filesWithIssues()));
                cdiInjectionTruncationNotice.updateMessage(addedCount, totalCount, "usages");
            } else {
                cdiInjectionStatusLabel.setText(String.format("Found %d usages in %d files",
                        totalCount, result.filesWithIssues()));
                cdiInjectionTruncationNotice.setVisible(false);
            }
            cdiInjectionStatusLabel.setForeground(new Color(200, 100, 0));
        } else {
            cdiInjectionStatusLabel.setText("No javax.inject.* usage found");
            cdiInjectionStatusLabel.setForeground(new Color(0, 150, 0));
            cdiInjectionTruncationNotice.setVisible(false);
        }
    }

    private void displayRestSoapResults(ProjectScanResult<FileScanResult<JavaxUsage>> result) {
        DefaultTableModel model = (DefaultTableModel) restSoapTable.getModel();
        model.setRowCount(0);

        if (result.hasIssues()) {
            boolean shouldTruncate = truncationHelper.shouldTruncateResults();
            int truncationLimit = shouldTruncate ? truncationHelper.getAdvancedScanTruncationLimit() : Integer.MAX_VALUE;
            int addedCount = 0;

            for (var fileResult : result.fileResults()) {
                for (var usage : fileResult.usages()) {
                    if (addedCount < truncationLimit) {
                        model.addRow(new Object[] {
                                fileResult.filePath().getFileName(),
                                usage.lineNumber(),
                                usage.className(),
                                usage.jakartaEquivalent(),
                                fileResult.filePath().toString()
                        });
                        addedCount++;
                    }
                }
            }

            // Update status label with truncation info
            int totalCount = result.totalIssuesFound();
            if (shouldTruncate && totalCount > truncationLimit) {
                restSoapStatusLabel.setText(String.format("Found %d of %d usages in %d files",
                        addedCount, totalCount, result.filesWithIssues()));
                restSoapTruncationNotice.updateMessage(addedCount, totalCount, "usages");
            } else {
                restSoapStatusLabel.setText(String.format("Found %d usages in %d files",
                        totalCount, result.filesWithIssues()));
                restSoapTruncationNotice.setVisible(false);
            }
            restSoapStatusLabel.setForeground(new Color(200, 100, 0));
        } else {
            restSoapStatusLabel.setText("No javax.jws.* or javax.ws.* usage found");
            restSoapStatusLabel.setForeground(new Color(0, 150, 0));
            restSoapTruncationNotice.setVisible(false);
        }
    }

    private void displaySecurityApiResults(SecurityApiProjectScanResult result) {
        DefaultTableModel model = (DefaultTableModel) securityApiTable.getModel();
        model.setRowCount(0);

        if (result.hasJavaxUsage()) {
            boolean shouldTruncate = truncationHelper.shouldTruncateResults();
            int truncationLimit = shouldTruncate ? truncationHelper.getAdvancedScanTruncationLimit() : Integer.MAX_VALUE;
            int addedCount = 0;

            for (var fileResult : result.getFileResults()) {
                for (var usage : fileResult.getUsages()) {
                    if (addedCount < truncationLimit) {
                        model.addRow(new Object[] {
                                fileResult.getFilePath().getFileName(),
                                usage.getLineNumber(),
                                usage.getJavaxClass(),
                                usage.getJakartaEquivalent(),
                                fileResult.getFilePath().toString()
                        });
                        addedCount++;
                    }
                }
            }

            // Update status label with truncation info
            int totalCount = result.getTotalJavaxUsages();
            if (shouldTruncate && totalCount > truncationLimit) {
                securityApiStatusLabel.setText(String.format("Found %d of %d usages in %d files",
                        addedCount, totalCount, result.getFilesWithJavaxUsage()));
                securityApiTruncationNotice.updateMessage(addedCount, totalCount, "usages");
            } else {
                securityApiStatusLabel.setText(String.format("Found %d usages in %d files",
                        totalCount, result.getFilesWithJavaxUsage()));
                securityApiTruncationNotice.setVisible(false);
            }
            securityApiStatusLabel.setForeground(new Color(200, 100, 0));
        } else {
            securityApiStatusLabel.setText("No javax.security.* usage found");
            securityApiStatusLabel.setForeground(new Color(0, 150, 0));
            securityApiTruncationNotice.setVisible(false);
        }
    }

    private void displayJmsMessagingResults(JmsMessagingProjectScanResult result) {
        DefaultTableModel model = (DefaultTableModel) jmsMessagingTable.getModel();
        model.setRowCount(0);

        if (result.hasJavaxUsage()) {
            boolean shouldTruncate = truncationHelper.shouldTruncateResults();
            int truncationLimit = shouldTruncate ? truncationHelper.getAdvancedScanTruncationLimit() : Integer.MAX_VALUE;
            int addedCount = 0;

            for (var fileResult : result.getFileResults()) {
                for (var usage : fileResult.getUsages()) {
                    if (addedCount < truncationLimit) {
                        model.addRow(new Object[] {
                                fileResult.getFilePath().getFileName(),
                                usage.getLineNumber(),
                                usage.getJavaxClass(),
                                usage.getJakartaEquivalent(),
                                fileResult.getFilePath().toString()
                        });
                        addedCount++;
                    }
                }
            }

            // Update status label with truncation info
            int totalCount = result.getTotalJavaxUsages();
            if (shouldTruncate && totalCount > truncationLimit) {
                jmsMessagingStatusLabel.setText(String.format("Found %d of %d usages in %d files",
                        addedCount, totalCount, result.getFilesWithJavaxUsage()));
                jmsMessagingTruncationNotice.updateMessage(addedCount, totalCount, "usages");
            } else {
                jmsMessagingStatusLabel.setText(String.format("Found %d usages in %d files",
                        totalCount, result.getFilesWithJavaxUsage()));
                jmsMessagingTruncationNotice.setVisible(false);
            }
            jmsMessagingStatusLabel.setForeground(new Color(200, 100, 0));
        } else {
            jmsMessagingStatusLabel.setText("No javax.jms.* usage found");
            jmsMessagingStatusLabel.setForeground(new Color(0, 150, 0));
            jmsMessagingTruncationNotice.setVisible(false);
        }
    }

    private void displayClassloaderModuleResults(ClassloaderModuleProjectScanResult result) {
        DefaultTableModel model = (DefaultTableModel) classloaderModuleTable.getModel();
        model.setRowCount(0);

        if (result.hasJavaxUsage()) {
            boolean shouldTruncate = truncationHelper.shouldTruncateResults();
            int truncationLimit = shouldTruncate ? truncationHelper.getAdvancedScanTruncationLimit() : Integer.MAX_VALUE;
            int addedCount = 0;

            for (var fileResult : result.getFileResults()) {
                for (var usage : fileResult.getUsages()) {
                    if (addedCount < truncationLimit) {
                        model.addRow(new Object[] {
                                fileResult.getFilePath().getFileName(),
                                usage.getLineNumber(),
                                usage.getJavaxClass(),
                                usage.getReplacement(),
                                fileResult.getFilePath().toString()
                        });
                        addedCount++;
                    }
                }
            }

            // Update status label with truncation info
            int totalCount = result.getTotalJavaxUsages();
            if (shouldTruncate && totalCount > truncationLimit) {
                classloaderModuleStatusLabel.setText(String.format("Found %d of %d usages in %d files",
                        addedCount, totalCount, result.getFilesWithJavaxUsage()));
                classloaderModuleTruncationNotice.updateMessage(addedCount, totalCount, "usages");
            } else {
                classloaderModuleStatusLabel.setText(String.format("Found %d usages in %d files",
                        totalCount, result.getFilesWithJavaxUsage()));
                classloaderModuleTruncationNotice.setVisible(false);
            }
            classloaderModuleStatusLabel.setForeground(new Color(200, 100, 0));
        } else {
            classloaderModuleStatusLabel.setText("No javax.* classloader/module usage found");
            classloaderModuleStatusLabel.setForeground(new Color(0, 150, 0));
            classloaderModuleTruncationNotice.setVisible(false);
        }
    }

    private void displayLoggingMetricsResults(LoggingMetricsProjectScanResult result) {
        DefaultTableModel model = (DefaultTableModel) loggingMetricsTable.getModel();
        model.setRowCount(0);

        if (result.hasFindings()) {
            boolean shouldTruncate = truncationHelper.shouldTruncateResults();
            int truncationLimit = shouldTruncate ? truncationHelper.getAdvancedScanTruncationLimit() : Integer.MAX_VALUE;
            int addedCount = 0;

            for (var fileResult : result.getFileResults()) {
                for (var usage : fileResult.getUsages()) {
                    if (addedCount < truncationLimit) {
                        model.addRow(new Object[] {
                                Path.of(fileResult.getFilePath()).getFileName(),
                                usage.getLineNumber(),
                                usage.getUsageType(),
                                usage.getReplacement(),
                                fileResult.getFilePath()
                        });
                        addedCount++;
                    }
                }
            }

            // Update status label with truncation info
            int totalCount = result.getTotalFindings();
            if (shouldTruncate && totalCount > truncationLimit) {
                loggingMetricsStatusLabel.setText(String.format("Found %d of %d usages in %d files",
                        addedCount, totalCount, result.getTotalFilesScanned()));
                loggingMetricsTruncationNotice.updateMessage(addedCount, totalCount, "usages");
            } else {
                loggingMetricsStatusLabel.setText(String.format("Found %d usages in %d files",
                        totalCount, result.getTotalFilesScanned()));
                loggingMetricsTruncationNotice.setVisible(false);
            }
            loggingMetricsStatusLabel.setForeground(new Color(200, 100, 0));
        } else {
            loggingMetricsStatusLabel.setText("No javax.* logging/metrics usage found");
            loggingMetricsStatusLabel.setForeground(new Color(0, 150, 0));
            loggingMetricsTruncationNotice.setVisible(false);
        }
    }

    private void displaySerializationCacheResults(SerializationCacheProjectScanResult result) {
        DefaultTableModel model = (DefaultTableModel) serializationCacheTable.getModel();
        model.setRowCount(0);

        if (result.hasFindings()) {
            boolean shouldTruncate = truncationHelper.shouldTruncateResults();
            int truncationLimit = shouldTruncate ? truncationHelper.getAdvancedScanTruncationLimit() : Integer.MAX_VALUE;
            int addedCount = 0;

            for (var fileResult : result.getFileResults()) {
                boolean firstRow = true;
                for (var usage : fileResult.getUsages()) {
                    if (addedCount < truncationLimit) {
                        model.addRow(new Object[] {
                                firstRow ? Path.of(fileResult.getFilePath()).getFileName() : "",
                                usage.getLineNumber(),
                                usage.getUsageType(),
                                usage.getRiskAssessment(),
                                fileResult.getFilePath()
                        });
                        addedCount++;
                    }
                    firstRow = false;
                }
            }

            // Update status label with truncation info
            int totalCount = result.getTotalFindings();
            if (shouldTruncate && totalCount > truncationLimit) {
                serializationCacheStatusLabel.setText(String.format("Found %d of %d usages in %d files",
                        addedCount, totalCount, result.getTotalFilesScanned()));
                serializationCacheTruncationNotice.updateMessage(addedCount, totalCount, "usages");
            } else {
                serializationCacheStatusLabel.setText(String.format("Found %d usages in %d files",
                        totalCount, result.getTotalFilesScanned()));
                serializationCacheTruncationNotice.setVisible(false);
            }
            serializationCacheStatusLabel.setForeground(new Color(200, 100, 0));
        } else {
            serializationCacheStatusLabel.setText("No javax.* serialization/cache usage found");
            serializationCacheStatusLabel.setForeground(new Color(0, 150, 0));
            serializationCacheTruncationNotice.setVisible(false);
        }
    }

    /**
     * Generic table setup method to reduce duplication
     */
    private void setupTable(JBTable table) {
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        
        // Hide the last column (Path column)
        TableColumnModel columnModel = table.getColumnModel();
        int pathColumnIndex = columnModel.getColumnCount() - 1;
        columnModel.removeColumn(columnModel.getColumn(pathColumnIndex));
        
        // Add double-click listener to open file
        addFileOpenListener(table, pathColumnIndex);
    }
    
    /**
     * Adds file open listener to table
     */
    private void addFileOpenListener(JBTable table, int pathColumnIndex) {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.getSelectedRow();
                    if (row >= 0) {
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
            boolean shouldTruncate = truncationHelper.shouldTruncateResults();
            int truncationLimit = shouldTruncate ? truncationHelper.getAdvancedScanTruncationLimit() : Integer.MAX_VALUE;
            int addedCount = 0;

            for (var usage : result.getLibraries()) {
                if (addedCount < truncationLimit) {
                    model.addRow(new Object[] {
                            usage.getLibraryName(),
                            usage.getGroupId() + ":" + usage.getArtifactId(),
                            usage.getIssueType(),
                            usage.getSuggestedReplacement(),
                            "" // No file path for libraries
                    });
                    addedCount++;
                }
            }

            // Update status label with truncation info
            int totalCount = result.getTotalLibraries();
            if (shouldTruncate && totalCount > truncationLimit) {
                thirdPartyLibStatusLabel.setText(String.format("Found %d of %d incompatible libraries",
                        addedCount, totalCount));
                thirdPartyLibTruncationNotice.updateMessage(addedCount, totalCount, "libraries");
            } else {
                thirdPartyLibStatusLabel.setText(String.format("Found %d incompatible libraries",
                        totalCount));
                thirdPartyLibTruncationNotice.setVisible(false);
            }
            thirdPartyLibStatusLabel.setForeground(new Color(200, 100, 0));
        } else {
            thirdPartyLibStatusLabel.setText("No incompatible third-party libraries found");
            thirdPartyLibStatusLabel.setForeground(new Color(0, 150, 0));
            thirdPartyLibTruncationNotice.setVisible(false);
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
                    LOG.warn("Failed to load initial state for AdvancedScansComponent (may be due to schema changes): " + e.getMessage());
                }
            }
        }
    }
}
