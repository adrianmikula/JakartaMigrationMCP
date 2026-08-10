package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import adrianmikula.jakartamigration.intellij.ui.DependencyStatusColors;
import adrianmikula.jakartamigration.intellij.ui.components.TruncationHelper;
import adrianmikula.jakartamigration.intellij.ui.components.TruncationNoticePanel;
import adrianmikula.jakartamigration.intellij.util.NotificationHelper;
import adrianmikula.jakartamigration.dependencyanalysis.service.ImprovedMavenCentralLookupService;
import adrianmikula.jakartamigration.dependencyanalysis.service.ImprovedMavenCentralLookupService.JakartaArtifactMatch;
import adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Namespace;
import adrianmikula.jakartamigration.dependencyanalysis.service.NamespaceClassifier;
import adrianmikula.jakartamigration.scanning.RecipeBasedClassifier;
import adrianmikula.jakartamigration.platforms.config.PlatformConfigLoader;
import adrianmikula.jakartamigration.platforms.model.PlatformConfig;
import adrianmikula.jakartamigration.credits.CreditsService;
import adrianmikula.jakartamigration.credits.CreditType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.table.DefaultTableModel;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;

/**
 * Dependencies table component with colored status indicators and dependency
 * type column.
 * Updated to show:
 * - Colored status dot (green/yellow/red)
 * - Dependency type (Direct/Transitive)
 * - Jakarta Equivalent columns
 * - Bottom panel for refactoring recipes (premium feature)
 */
public class DependenciesTableComponent extends AbstractDependencyUIComponent {
    private final JPanel panel;
    private final Project project;
    private final JBTable table;
    private final DefaultTableModel tableModel;
    private final JTextField searchField;
    private final JComboBox<String> statusFilter;
    private final JCheckBox transitiveFilter;
    private final JCheckBox organizationalFilter;
    private JProgressBar progressBar;
    private boolean isQueryingMaven = false;
    private static final Logger LOGGER = Logger.getLogger(DependenciesTableComponent.class.getName());
    private List<DependencyInfo> allDependencies;
    
    // Truncation notice panel (shown for free users)
    private TruncationNoticePanel truncationNoticePanel;

    // Error banner for build tool failures
    private JLabel errorBannerLabel;

    // Recipes panel component (shared with tree view)
    private RecipesPanelComponent recipesPanel;
    private boolean isPremiumUser = false;
    private final PlatformConfigLoader platformConfigLoader;
    private final NamespaceClassifier namespaceClassifier;
    private final CreditsService creditsService;
    private final TruncationHelper truncationHelper;

    // Callback interface for notifying when analysis completes
    public interface OnAnalysisCompleteListener {
        void onAnalysisComplete(List<DependencyInfo> dependencies);
    }
    private OnAnalysisCompleteListener analysisCompleteListener;

    public DependenciesTableComponent(Project project) {
        this.project = project;
        this.allDependencies = new ArrayList<>();
        this.platformConfigLoader = new PlatformConfigLoader();
        this.namespaceClassifier = new RecipeBasedClassifier();
        this.creditsService = new CreditsService();
        this.truncationHelper = new TruncationHelper();
        this.panel = new JBPanel<>(new BorderLayout());

        // Columns with Jakarta Equivalent information and Confidence
        String[] columns = {
                "Group ID",
                "Artifact ID",
                "Current Version",
                "Jakarta Equivalent",
                "Compatibility Status",
                "Reason",
                "Type",
                "Confidence",
                "" // Hidden column for DependencyInfo object
        };

        this.tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        this.table = new JBTable(tableModel) {
            @Override
            public TableCellRenderer getCellRenderer(int row, int column) {
                return new StatusCellRenderer();
            }

            @Override
            public String getToolTipText(MouseEvent e) {
                Point p = e.getPoint();
                int row = rowAtPoint(p);
                if (row >= 0) {
                    int modelRow = convertRowIndexToModel(row);
                    int depColumn = getModel().getColumnCount() - 1;
                    Object depObj = getModel().getValueAt(modelRow, depColumn);
                    if (depObj instanceof DependencyInfo) {
                        String detail = ((DependencyInfo) depObj).getDetailMessage();
                        if (detail != null && !detail.isBlank()) {
                            return detail;
                        }
                    }
                }
                return null;
            }
        };

        this.searchField = new JTextField(20);
        this.statusFilter = new JComboBox<>(new String[] {
                "All", "Compatible", "Needs Upgrade", "No Jakarta Version", "Unknown"
        });
        this.transitiveFilter = new JCheckBox("Hide Transitive Dependencies", false);
        this.organizationalFilter = new JCheckBox("Show All Organisational Artifacts", false);

        initializeComponent();
    }

    /**
     * Custom cell renderer for status column with colored dot indicator.
     */
    private static class StatusCellRenderer implements TableCellRenderer {
        private boolean isRendering = false; // Prevent infinite recursion
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus,
                int row, int column) {
            // Prevent infinite recursion during rendering
            if (isRendering) {
                return new JLabel("Rendering...");
            }
            
            isRendering = true;
            try {
            // Status column is at index 4, Jakarta Equivalent at index 3, DependencyInfo at the last (hidden) column
            if (column == 4 && row < table.getModel().getRowCount()) {
                int depColumn = table.getModel().getColumnCount() - 1;
                Object depObj = table.getModel().getValueAt(row, depColumn);
                if (depObj instanceof DependencyInfo) {
                    DependencyInfo dep = (DependencyInfo) depObj;
                    JPanel panel = new JPanel(new BorderLayout());
                    panel.setOpaque(true);

                    // Set background for selection
                    if (isSelected) {
                        panel.setBackground(table.getSelectionBackground());
                    } else {
                        if (dep.isOrganizational()) {
                            panel.setBackground(new Color(230, 240, 255)); // Light blue tint
                        } else {
                            panel.setBackground(table.getBackground());
                        }
                    }

                    // Create status label with color coding based on Jakarta equivalent availability
                    String statusText = (String) value;
                    JLabel statusLabel = new JLabel(statusText);
                    statusLabel.setFont(table.getFont().deriveFont(Font.BOLD));
                    statusLabel.setOpaque(true);
                    statusLabel.setBorder(new EmptyBorder(2, 5, 2, 5));

                    // Apply color coding based on migration status - matches DependencyGraphComponent
                    DependencyMigrationStatus status = dep.getMigrationStatus();
                    Color baseColor = getStatusColor(status);
                    Color bgColor = getStatusBackgroundColor(status);
                    
                    statusLabel.setBackground(bgColor);
                    statusLabel.setForeground(baseColor.darker());

                    // Add dotted border for transitive dependencies on the panel
                    if (dep.isTransitive()) {
                        panel.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createDashedBorder(Color.GRAY),
                                new EmptyBorder(2, 2, 2, 2)));
                    } else {
                        panel.setBorder(new EmptyBorder(2, 2, 2, 2));
                    }

                    panel.add(statusLabel, BorderLayout.CENTER);
                    return panel;
                }
            }

            // Default rendering for other columns - check organizational status from hidden column
            JLabel label = new JLabel(value != null ? value.toString() : "");
            label.setOpaque(true);
            if (isSelected) {
                label.setBackground(table.getSelectionBackground());
            } else {
                // Determine if this row is organizational (check the last hidden column)
                boolean isOrg = false;
                int depColumn = table.getModel().getColumnCount() - 1;
                if (row < table.getModel().getRowCount()) {
                    Object depObj = table.getModel().getValueAt(row, depColumn);
                    if (depObj instanceof DependencyInfo) {
                        isOrg = ((DependencyInfo) depObj).isOrganizational();
                    }
                }

                if (isOrg) {
                    label.setBackground(new Color(230, 240, 255)); // Light blue for organizational
                } else {
                    label.setBackground(table.getBackground());
                }
            }
            label.setHorizontalAlignment(SwingConstants.LEFT);
            
            return label;
            } finally {
                isRendering = false;
            }
        }

        /**
         * Get the status color based on migration status - uses shared DependencyStatusColors utility.
         */
        private Color getStatusColor(DependencyMigrationStatus status) {
            return DependencyStatusColors.getStatusColor(status);
        }

        /**
         * Get background color for a status (lighter version for table cells) - uses shared DependencyStatusColors utility.
         */
        private Color getStatusBackgroundColor(DependencyMigrationStatus status) {
            return DependencyStatusColors.getStatusBackgroundColor(status);
        }
    }

    private void initializeComponent() {
        // Header with filters
        JPanel headerPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT));
        headerPanel.add(new JLabel("Dependencies Analysis"));

        searchField.setToolTipText("Search dependencies...");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterDependencies();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterDependencies();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterDependencies();
            }
        });
        headerPanel.add(searchField);

        // Status filter dropdown removed from the visible UI - not needed there,
        // but keep the listener so programmatic/test selection still triggers filtering.
        statusFilter.addActionListener(e -> filterDependencies());

        transitiveFilter.addActionListener(e -> filterDependencies());
        headerPanel.add(transitiveFilter);

        organizationalFilter.addActionListener(e -> filterDependencies());
        headerPanel.add(organizationalFilter);

        // Table
        JBScrollPane scrollPane = new JBScrollPane(table);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // Set column widths (8 columns + 1 hidden)
        table.getColumnModel().getColumn(0).setPreferredWidth(150); // Group ID
        table.getColumnModel().getColumn(1).setPreferredWidth(150); // Artifact ID
        table.getColumnModel().getColumn(2).setPreferredWidth(90);  // Current Version
        table.getColumnModel().getColumn(3).setPreferredWidth(150); // Jakarta Equivalent
        table.getColumnModel().getColumn(4).setPreferredWidth(120); // Status (color-coded)
        table.getColumnModel().getColumn(5).setPreferredWidth(200); // Reason
        table.getColumnModel().getColumn(6).setPreferredWidth(80);  // Type
        table.getColumnModel().getColumn(7).setPreferredWidth(80);  // Confidence
        table.getColumnModel().getColumn(8).setMinWidth(0);         // Hidden DependencyInfo
        table.getColumnModel().getColumn(8).setMaxWidth(0);
        table.getColumnModel().getColumn(8).setWidth(0);

        // Add mouse listener for double-click navigation
        table.addMouseListener(new MouseInputAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    handleDoubleClick();
                }
            }
        });

        // Add selection listener to update recipes panel
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateRecipesPanel();
            }
        });

        // Initialize bottom panel for recipes
        initializeRecipesPanel();

        // Actions panel
        JPanel actionsPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT));
        JButton updateButton = new JButton("Update Selected");
        updateButton.addActionListener(this::handleUpdate);
        JButton viewDetailsButton = new JButton("View Details");
        viewDetailsButton.addActionListener(this::handleViewDetails);

        actionsPanel.add(updateButton);
        actionsPanel.add(viewDetailsButton);
        
        // Add progress bar to actions panel
        progressBar = new JProgressBar();
        progressBar.setVisible(false);
        progressBar.setStringPainted(true);
        actionsPanel.add(progressBar);

        // Create truncation notice panel (hidden by default)
        truncationNoticePanel = new TruncationNoticePanel();
        truncationNoticePanel.setVisible(false);

        // Create error banner label (hidden by default)
        errorBannerLabel = new JLabel();
        errorBannerLabel.setOpaque(true);
        errorBannerLabel.setBackground(new Color(255, 243, 224)); // Light orange
        errorBannerLabel.setForeground(new Color(180, 80, 0));
        errorBannerLabel.setBorder(new EmptyBorder(6, 10, 6, 10));
        errorBannerLabel.setFont(errorBannerLabel.getFont().deriveFont(Font.BOLD));
        errorBannerLabel.setVisible(false);

        // Create center panel with table, truncation notice, and recipes
        JPanel centerPanel = new JBPanel<>(new BorderLayout());
        centerPanel.add(errorBannerLabel, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        // South panel containing truncation notice (above recipes)
        JPanel southPanel = new JBPanel<>(new BorderLayout());
        southPanel.add(truncationNoticePanel, BorderLayout.NORTH);
        southPanel.add(recipesPanel.getPanel(), BorderLayout.SOUTH);
        centerPanel.add(southPanel, BorderLayout.SOUTH);

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(centerPanel, BorderLayout.CENTER);
        panel.add(actionsPanel, BorderLayout.SOUTH);
    }

    private void initializeRecipesPanel() {
        recipesPanel = new RecipesPanelComponent(project);
        recipesPanel.setPremiumUser(isPremiumUser);
    }

    private void updateRecipesPanel() {
        List<DependencyInfo> selected = getSelectedDependencies();
        
        if (selected.isEmpty()) {
            recipesPanel.hide();
            return;
        }
        
        DependencyInfo dep = selected.get(0);
        recipesPanel.updateForDependency(dep);
    }

    public void setPremiumUser(boolean isPremium) {
        this.isPremiumUser = isPremium;
        recipesPanel.setPremiumUser(isPremium);
    }

    public void setOnAnalysisCompleteListener(OnAnalysisCompleteListener listener) {
        this.analysisCompleteListener = listener;
    }

    public boolean isPremiumUser() {
        return isPremiumUser;
    }

    public JPanel getPanel() {
        return panel;
    }

    /**
     * Shows or hides the error banner above the dependency table.
     * Used to inform users when build tool commands failed and results are partial.
     *
     * @param message the banner text, or null to hide the banner
     */
    public void setErrorBanner(String message) {
        if (message == null || message.isEmpty()) {
            errorBannerLabel.setVisible(false);
        } else {
            errorBannerLabel.setText(message);
            errorBannerLabel.setVisible(true);
        }
    }

    public void setDependencies(List<DependencyInfo> dependencies) {
        this.allDependencies = dependencies != null ? dependencies : new ArrayList<>();
        filterDependencies();
        
        // Automatically trigger Maven Central lookup for new dependencies
        if (!this.allDependencies.isEmpty()) {
            queryMavenCentralForDependencies();
        }
    }

    @Override
    public void clearDependencies() {
        setDependencies(new ArrayList<>());
    }

    /**
     * Resets the table to show "Analysis Pending" for all currently displayed dependencies.
     * Called when a new scan starts, so the user immediately sees that previous results are stale.
     */
    public void resetToPending() {
        if (allDependencies.isEmpty()) {
            return;
        }
        for (DependencyInfo dep : allDependencies) {
            dep.setMavenLookupInProgress(false);
            dep.setMigrationStatus(null);
            dep.setScanReason("PENDING_SCAN");
            dep.setRecommendedArtifactCoordinates(null);
            dep.setJakartaCompatibilityStatus(null);
            dep.setDetailMessage(null);
        }
        filterDependencies();
    }

    public List<DependencyInfo> getSelectedDependencies() {
        int[] selectedRows = table.getSelectedRows();
        List<DependencyInfo> selected = new ArrayList<>();
        for (int row : selectedRows) {
            int modelRow = table.convertRowIndexToModel(row);
            if (modelRow < allDependencies.size()) {
                selected.add(allDependencies.get(modelRow));
            }
        }
        return selected;
    }

    private void filterDependencies() {
        // Clear and rebuild table with filtered data
        tableModel.setRowCount(0);

        String searchText = searchField.getText().toLowerCase();
        String selectedStatus = (String) statusFilter.getSelectedItem();
        boolean hideTransitive = transitiveFilter.isSelected();
        boolean showOrganizationalOnly = organizationalFilter.isSelected();

        // Check if truncation should be applied for free users
        boolean shouldTruncate = truncationHelper.shouldTruncateResults();
        int truncationLimit = shouldTruncate ? truncationHelper.getDependenciesTruncationLimit() : Integer.MAX_VALUE;
        int addedCount = 0;

        // Iterate over a snapshot so async Maven Central updates don't alter the list mid-filter
        for (DependencyInfo dep : new ArrayList<>(allDependencies)) {
            // Search filter
            boolean matchesSearch = searchText.isEmpty() ||
                    dep.getGroupId().toLowerCase().contains(searchText) ||
                    dep.getArtifactId().toLowerCase().contains(searchText) ||
                    dep.getCurrentVersion().toLowerCase().contains(searchText);

            // Status filter
            boolean matchesStatus = "All".equals(selectedStatus) ||
                    (selectedStatus != null && dep.getMigrationStatus() != null &&
                            dep.getMigrationStatus().getValue().equals(mapStatusToValue(selectedStatus)));

            // Transitive filter - when checked, hide transitive dependencies
            boolean matchesTransitive = !hideTransitive || !dep.isTransitive();

            // Organizational filter
            boolean matchesOrganizational = !showOrganizationalOnly || dep.isOrganizational();

            if (matchesSearch && matchesStatus && matchesTransitive && matchesOrganizational) {
                // Apply truncation limit for free users with exhausted credits
                if (addedCount < truncationLimit) {
                    addDependencyRow(dep);
                    addedCount++;
                }
            }
        }

        // Update truncation notice visibility
        if (shouldTruncate && allDependencies.size() > truncationLimit) {
            truncationNoticePanel.updateMessage(truncationLimit, allDependencies.size(), "dependencies");
        } else {
            truncationNoticePanel.setVisible(false);
        }

        // Force the table to refresh after the model is rebuilt so hidden rows reappear
        table.revalidate();
        table.repaint();
    }



    private String mapStatusToValue(String status) {
        if (status == null)
            return null;
        switch (status) {
            case "Compatible":
                return "COMPATIBLE";
            case "Needs Upgrade":
                return "NEEDS_UPGRADE";
            case "No Jakarta Version":
                return "NO_JAKARTA_VERSION";
            default:
                return null;
        }
    }

    private void addDependencyRow(DependencyInfo dep) {
        // Determine dependency type
        String dependencyType = dep.isTransitive() ? "Transitive" : "Direct";

        // Jakarta Equivalent - prefer parsed coordinates, fall back to the raw recommendation string
        String coordinates = dep.getRecommendedArtifactCoordinates();
        String jakartaEquivalent = coordinates != null && !coordinates.isBlank() ? coordinates : "";
        if (jakartaEquivalent.isBlank() && dep.getRecommendation() != null && !dep.getRecommendation().isBlank()) {
            jakartaEquivalent = dep.getRecommendation();
        }
        if (jakartaEquivalent.isBlank()) {
            jakartaEquivalent = "-";
        }

        // Compatibility Status - determines color coding
        String statusText;
        boolean hasJakartaEquivalent = !"-".equals(jakartaEquivalent);
        String scanReason = dep.getScanReason();

        // Enhanced status logic - differentiate pending vs final states
        if (dep.isMavenLookupInProgress()) {
            // Currently being analyzed
            statusText = "? Analysis in Progress";
        } else if ("PENDING_SCAN".equals(scanReason)) {
            // Waiting for scan to complete
            statusText = "? Scan Pending";
        } else if ("UNKNOWN".equals(scanReason) || "BYTECODE_SCAN_UNKNOWN".equals(scanReason)) {
            // Pending analysis
            statusText = "? Analysis Pending";
        } else if ("BLACKLISTED".equals(scanReason) && !hasJakartaEquivalent) {
            // Known javax dependency with no known Jakarta equivalent
            statusText = "⚠ Possible Blocker";
        } else {
            // Infer a concrete status from the available recommendation when still unknown
            if (dep.getMigrationStatus() == null || dep.getMigrationStatus() == DependencyMigrationStatus.UNKNOWN) {
                dep.setMigrationStatus(hasJakartaEquivalent
                        ? DependencyMigrationStatus.NEEDS_UPGRADE
                        : DependencyMigrationStatus.NO_JAKARTA_VERSION);
            }
            statusText = DependencyStatusColors.getStatusText(dep.getMigrationStatus());
        }

        // Reason (scan reason) - provide specific pending states
        String reason = scanReason != null ? scanReason : "-";

        // Detailed reason/explanation text is shown on hover via the table tooltip

        // Enhanced reason mapping for better clarity on pending states
        if (dep.isMavenLookupInProgress()) {
            reason = "Maven lookup in progress";
        } else if ("PENDING_SCAN".equals(reason)) {
            reason = "Waiting for scan to start";
        } else if ("UNKNOWN".equals(reason)) {
            reason = "Analysis pending";
        } else if ("BYTECODE_SCAN_UNKNOWN".equals(reason)) {
            reason = "Bytecode scan pending";
        } else if ("MAVEN_LOOKUP_NONE".equals(reason)) {
            reason = "Jakarta version not found - check naming patterns";
        } else if ("BLACKLISTED".equals(reason) && !dep.isMavenLookupInProgress()) {
            reason = "Requires Jakarta upgrade";
        } else if ("WHITELISTED".equals(reason)) {
            reason = "JDK provided - compatible";
        } else if ("BYTECODE_SCAN_JAKARTA".equals(reason)) {
            reason = "Jakarta compatible - bytecode verified";
        } else if ("BYTECODE_SCAN_JAVAX".equals(reason)) {
            reason = "javax detected - upgrade required";
        } else if ("BYTECODE_SCAN_MIXED".equals(reason)) {
            reason = "Mixed javax/Jakarta - requires migration";
        } else if ("BUILD_TOOL_ERROR".equals(reason)) {
            reason = "Build tool error - regex fallback (deep scan unavailable)";
        }

        String confidence = dep.getConfidence() > 0.0
                ? String.format("%.0f%%", dep.getConfidence() * 100)
                : "";

        // Add row with all columns - DependencyInfo at the last (hidden) column
        tableModel.addRow(new Object[] {
                dep.getGroupId(),
                dep.getArtifactId(),
                dep.getCurrentVersion(),
                jakartaEquivalent,  // Column 3: Jakarta Equivalent
                statusText,         // Column 4: Status
                reason,             // Column 5: Reason
                dependencyType,     // Column 6: Type
                confidence,         // Column 7: Confidence
                dep                 // Column 8: Full object for renderer (hidden column)
        });
    }
    
    /**
     * Queries Maven Central for Jakarta equivalents of detected javax dependencies.
     * For app server dependencies (Tomcat, WildFly, etc.), uses platforms.yaml config
     * to recommend min Jakarta version directly instead of querying Maven Central.
     */
    public void queryMavenCentralForDependencies() {
        if (allDependencies.isEmpty()) {
            return;
        }
        
        // Load platform configurations
        Map<String, PlatformConfig> platforms = platformConfigLoader.getPlatformConfigs();
        
        // Separate app server dependencies from regular javax dependencies
        List<DependencyInfo> appServerDeps = new ArrayList<>();
        List<DependencyInfo> javaxDependencies = new ArrayList<>();
        
        for (DependencyInfo dep : allDependencies) {
            if (dep.getGroupId() == null || dep.getArtifactId() == null) {
                continue;
            }
            
            // Check if this is an app server dependency using group:name format
            String depGroupId = dep.getGroupId();
            String depArtifactId = dep.getArtifactId();
            boolean isAppServer = false;
            PlatformConfig matchingPlatform = null;
            
            for (PlatformConfig platform : platforms.values()) {
                if (platform.commonArtifacts() != null) {
                    for (String commonArtifact : platform.commonArtifacts()) {
                        // Parse group:name format
                        String[] parts = commonArtifact.split(":");
                        if (parts.length == 2) {
                            String artifactGroup = parts[0];
                            String artifactName = parts[1];
                            
                            // Match both group and artifact name
                            if (depGroupId.equals(artifactGroup) && depArtifactId.equals(artifactName)) {
                                isAppServer = true;
                                matchingPlatform = platform;
                                System.out.println("[DEBUG] Matched app server artifact: " + commonArtifact);
                                break;
                            }
                        }
                    }
                }
                if (isAppServer) break;
            }
            
            if (isAppServer && matchingPlatform != null) {
                // Check if version is below Jakarta min version
                String currentVersion = dep.getCurrentVersion();
                String minJakartaVersion = matchingPlatform.jakartaCompatibility() != null ? 
                    matchingPlatform.jakartaCompatibility().minVersion() : null;
                
                if (minJakartaVersion != null && currentVersion != null) {
                    // Simple version comparison (assumes semantic versioning)
                    if (isVersionBelow(currentVersion, minJakartaVersion)) {
                        // This is a javax-era app server - recommend Jakarta upgrade
                        dep.setRecommendedArtifactCoordinates(
                            matchingPlatform.name() + " " + minJakartaVersion + "+ (Jakarta EE)");
                        dep.setRecommendedVersion(minJakartaVersion + "+");
                        dep.setJakartaCompatibilityStatus("Upgrade to " + minJakartaVersion + "+");
                        dep.setMigrationStatus(DependencyMigrationStatus.NEEDS_UPGRADE);
                        appServerDeps.add(dep);
                        System.out.println("[DEBUG] App server using javax: " + dep.getGroupId() + ":" + 
                            dep.getArtifactId() + " v" + currentVersion + " -> recommend " + minJakartaVersion + "+");
                    } else {
                        // Already Jakarta-compatible
                        dep.setMigrationStatus(DependencyMigrationStatus.COMPATIBLE);
                    }
                }
            } else if (dep.getGroupId().startsWith("javax.")) {
                // Use RecipeBasedClassifier to classify javax dependencies
                Namespace ns = namespaceClassifier.classify(new Artifact(
                    dep.getGroupId(), dep.getArtifactId(), dep.getCurrentVersion(), "compile", false));

                switch (ns) {
                    case JAKARTA:
                        // Known Jakarta artifact
                        dep.setMigrationStatus(DependencyMigrationStatus.COMPATIBLE);
                        dep.setJakartaCompatibilityStatus("Jakarta-compatible");
                        break;

                    case JAVAX:
                        // Uses javax namespace, must migrate
                        javaxDependencies.add(dep);
                        System.out.println("[DEBUG] Jakarta required: " + dep.getGroupId() + ":" + dep.getArtifactId());
                        break;

                    case MIXED:
                        // Mixed namespace, requires manual review
                        dep.setMigrationStatus(DependencyMigrationStatus.REQUIRES_MANUAL_MIGRATION);
                        dep.setJakartaCompatibilityStatus("Review Required - Mixed namespace");
                        javaxDependencies.add(dep);
                        break;

                    case UNKNOWN:
                    default:
                        // Not in any list - query Maven Central as fallback
                        javaxDependencies.add(dep);
                        System.out.println("[DEBUG] Unknown (Maven lookup fallback): " + dep.getGroupId() + ":" + dep.getArtifactId());
                        break;
                }
            }
        }
        
        System.out.println("[DEBUG] App server deps to upgrade: " + appServerDeps.size());
        System.out.println("[DEBUG] Maven Central javax deps: " + javaxDependencies.size());
        
        // Refresh UI to show app server upgrades immediately
        if (!appServerDeps.isEmpty()) {
            SwingUtilities.invokeLater(() -> filterDependencies());
            // Notify listener that analysis has new results
            if (analysisCompleteListener != null) {
                analysisCompleteListener.onAnalysisComplete(new ArrayList<>(allDependencies));
            }
        }
        
        if (javaxDependencies.isEmpty()) {
            return;
        }
        
        // Continue with Maven Central lookup for remaining javax dependencies
        isQueryingMaven = true;
        
        // Set lookup state for all dependencies before starting lookup
        for (DependencyInfo dep : javaxDependencies) {
            dep.setMavenLookupInProgress(true);
        }
        
        SwingUtilities.invokeLater(() -> {
            progressBar.setVisible(true);
            progressBar.setIndeterminate(true);
            progressBar.setString("Querying Maven Central for " + javaxDependencies.size() + " dependencies...");
            // Refresh UI to show "Researching Upgrade Paths" status for BLACKLISTED dependencies
            filterDependencies();
        });
        
        ImprovedMavenCentralLookupService mavenService = new ImprovedMavenCentralLookupService();
        
        List<CompletableFuture<Void>> lookupFutures = javaxDependencies.stream()
                .map(javaxDep -> 
                    mavenService.findJakartaEquivalents(javaxDep.getGroupId(), javaxDep.getArtifactId())
                            .thenAccept(jakartaArtifacts -> {
                                updateDependencyWithJakartaInfo(javaxDep, jakartaArtifacts);
                            })
                            .exceptionally(throwable -> {
                                LOGGER.warning("Failed to query Maven Central for " + javaxDep.getGroupId() + ":" + 
                                           javaxDep.getArtifactId() + " - " + throwable.getMessage());
                                return null;
                            })
                )
                .collect(Collectors.toList());
        
        CompletableFuture.allOf(lookupFutures.toArray(new CompletableFuture[0]))
                .thenRun(() -> {
                    SwingUtilities.invokeLater(() -> {
                        isQueryingMaven = false;
                        progressBar.setVisible(false);
                        progressBar.setIndeterminate(false);
                        filterDependencies();
                        
                        // Notify listener that analysis is complete
                        if (analysisCompleteListener != null) {
                            analysisCompleteListener.onAnalysisComplete(new ArrayList<>(allDependencies));
                        }
                        
                        LOGGER.info("Maven Central query completed.");
                    });
                })
                .exceptionally(throwable -> {
                    LOGGER.log(java.util.logging.Level.SEVERE, "Unexpected error in Maven Central queries", throwable);
                    SwingUtilities.invokeLater(() -> {
                        isQueryingMaven = false;
                        progressBar.setVisible(false);
                        progressBar.setIndeterminate(false);
                    });
                    return null;
                });
    }
    
    /**
     * Simple version comparison. Assumes semantic versioning.
     * Returns true if v1 is below v2.
     */
    private boolean isVersionBelow(String v1, String v2) {
        try {
            String[] parts1 = v1.split("\\.")[0].split("-");
            String[] parts2 = v2.split("\\.")[0].split("-");
            int major1 = Integer.parseInt(parts1[0]);
            int major2 = Integer.parseInt(parts2[0]);
            return major1 < major2;
        } catch (Exception e) {
            // If parsing fails, assume it needs upgrade
            return true;
        }
    }
    
    /**
     * Updates a dependency with Jakarta artifact information from Maven Central
     * Thread-safe implementation for async updates
     * 
     * @param javaxDep original javax dependency
     * @param jakartaArtifacts list of Jakarta artifacts found
     */
    private synchronized void updateDependencyWithJakartaInfo(DependencyInfo javaxDep, List<JakartaArtifactMatch> jakartaArtifacts) {
        // Clear lookup state when lookup completes
        javaxDep.setMavenLookupInProgress(false);

        boolean alreadyHasRecommendation = hasText(javaxDep.getRecommendedArtifactCoordinates())
                || hasText(javaxDep.getRecommendation());

        if (jakartaArtifacts == null || jakartaArtifacts.isEmpty()) {
            // No Jakarta artifacts found - only mark as UNKNOWN if the scanner did not
            // already provide a recommendation. Otherwise keep the existing status.
            if (!alreadyHasRecommendation) {
                javaxDep.setMigrationStatus(DependencyMigrationStatus.UNKNOWN);
                javaxDep.setJakartaCompatibilityStatus("Unknown - No Jakarta equivalent found");
            } else {
                javaxDep.setJakartaCompatibilityStatus("Jakarta equivalent already identified");
                javaxDep.setMigrationStatus(DependencyMigrationStatus.NEEDS_UPGRADE);
            }
            System.out.println("[DEBUG] No Jakarta equivalent found for: " + javaxDep.getGroupId() + ":" + javaxDep.getArtifactId());
            return;
        }

        // Filter to only found artifacts and get best match
        JakartaArtifactMatch bestMatch = jakartaArtifacts.stream()
                .filter(JakartaArtifactMatch::found)
                .findFirst()
                .orElse(null);

        if (bestMatch == null) {
            // No valid matches found - only mark as UNKNOWN if the scanner did not
            // already provide a recommendation.
            if (!alreadyHasRecommendation) {
                javaxDep.setMigrationStatus(DependencyMigrationStatus.UNKNOWN);
                javaxDep.setJakartaCompatibilityStatus("Unknown - No valid Jakarta match");
            } else {
                javaxDep.setJakartaCompatibilityStatus("Jakarta equivalent already identified");
                javaxDep.setMigrationStatus(DependencyMigrationStatus.NEEDS_UPGRADE);
            }
            System.out.println("[DEBUG] No valid Jakarta match for: " + javaxDep.getGroupId() + ":" + javaxDep.getArtifactId());
            return;
        }

        // Update dependency with Jakarta information
        String coordinates = bestMatch.groupId() + ":" + bestMatch.artifactId() + ":" + bestMatch.version();
        javaxDep.setRecommendedArtifactCoordinates(coordinates);
        javaxDep.setJakartaCompatibilityStatus("Compatible");
        javaxDep.setMigrationStatus(DependencyMigrationStatus.NEEDS_UPGRADE);

        // Update dependency in master list
        for (int i = 0; i < allDependencies.size(); i++) {
            DependencyInfo dep = allDependencies.get(i);
            if (dep.getGroupId().equals(javaxDep.getGroupId()) &&
                dep.getArtifactId().equals(javaxDep.getArtifactId())) {

                // Create a new DependencyInfo object with updated information
                DependencyInfo updatedDep = new DependencyInfo();
                updatedDep.setGroupId(dep.getGroupId());
                updatedDep.setArtifactId(dep.getArtifactId());
                updatedDep.setCurrentVersion(dep.getCurrentVersion());
                updatedDep.setRecommendedArtifactCoordinates(coordinates);
                updatedDep.setJakartaCompatibilityStatus("Compatible");
                updatedDep.setMigrationStatus(DependencyMigrationStatus.NEEDS_UPGRADE);
                updatedDep.setTransitive(dep.isTransitive());
                updatedDep.setOrganizational(dep.isOrganizational());
                updatedDep.setScanReason(dep.getScanReason());
                updatedDep.setDetailMessage(dep.getDetailMessage());

                allDependencies.set(i, updatedDep);
                break;
            }
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
    
    private void handleUpdate(ActionEvent e) {
        List<DependencyInfo> selected = getSelectedDependencies();
        if (selected.isEmpty()) {
            NotificationHelper.showWarning(project, "No Selection", "Please select dependencies to update.");
            return;
        }

        StringBuilder message = new StringBuilder("Update selected dependencies:\n\n");
        for (DependencyInfo dep : selected) {
            message.append("- ").append(dep.getDisplayName())
                    .append(" -> ")
                    .append(dep.getRecommendedVersion() != null ? dep.getRecommendedVersion() : "No Jakarta version")
                    .append("\n");
        }

        int result = Messages.showYesNoDialog(project, message.toString(), "Confirm Updates",
                Messages.getQuestionIcon());

        if (result == Messages.YES) {
            Messages.showInfoMessage(project, "Updates would be applied here.", "Update");
        }
    }

    private void handleViewDetails(ActionEvent e) {
        List<DependencyInfo> selected = getSelectedDependencies();
        if (selected.isEmpty()) {
            NotificationHelper.showWarning(project, "No Selection", "Please select a dependency to view details.");
            return;
        }
        showDependencyDetails(selected.get(0));
    }

    public void showDependencyDetails(DependencyInfo dep) {
        String details = String.format("""
                Dependency Details
                ==================

                Group ID: %s
                Artifact ID: %s
                Current Version: %s
                Recommended Version: %s

                Migration Status: %s
                Dependency Type: %s

                Actions:
                • Update to recommended version
                • View source code
                • Exclude from analysis
                """,
                dep.getGroupId(),
                dep.getArtifactId(),
                dep.getCurrentVersion(),
                dep.getRecommendedVersion() != null ? dep.getRecommendedVersion() : "N/A",
                dep.getMigrationStatus() != null ? dep.getMigrationStatus().getValue() : "UNKNOWN",
                dep.isTransitive() ? "Transitive" : "Direct");

        Messages.showInfoMessage(project, details, "Dependency Details - " + dep.getDisplayName());
    }

    private void handleDoubleClick() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow >= 0) {
            int modelRow = table.convertRowIndexToModel(selectedRow);
            if (modelRow < allDependencies.size()) {
                DependencyInfo dep = allDependencies.get(modelRow);
                showDependencyDetails(dep);
            }
        }
    }

    /**
     * @deprecated Use setDependencies with DependencyInfo objects instead
     */
    @Deprecated
    public void addDependency(String groupId, String artifactId, String currentVersion,
            String recommendedVersion, String status, boolean isBlocker,
            String riskLevel, String impact) {
        // Convert legacy string parameters to DependencyInfo
        DependencyInfo dep = new DependencyInfo();
        dep.setGroupId(groupId);
        dep.setArtifactId(artifactId);
        dep.setCurrentVersion(currentVersion);
        dep.setRecommendedVersion(recommendedVersion);
        dep.setMigrationStatus(mapStringToStatus(status));
        dep.setTransitive(isBlocker); // Reusing isBlocker for transitive

        allDependencies.add(dep);
        addDependencyRow(dep);
    }

    private DependencyMigrationStatus mapStringToStatus(String status) {
        if (status == null)
            return null;
        switch (status.toLowerCase()) {
            case "compatible":
                return DependencyMigrationStatus.COMPATIBLE;
            case "needs upgrade":
                return DependencyMigrationStatus.NEEDS_UPGRADE;
            case "no jakarta version":
                return DependencyMigrationStatus.NO_JAKARTA_VERSION;
            case "requires manual migration":
                return DependencyMigrationStatus.REQUIRES_MANUAL_MIGRATION;
            case "migrated":
                return DependencyMigrationStatus.MIGRATED;
            case "unknown":
                return DependencyMigrationStatus.UNKNOWN;
            default:
                return DependencyMigrationStatus.UNKNOWN;
        }
    }

    public DefaultTableModel getTableModel() {
        return tableModel;
    }

    public JTextField getSearchField() {
        return searchField;
    }

    public JComboBox<String> getStatusFilter() {
        return statusFilter;
    }

    public JCheckBox getTransitiveFilter() {
        return transitiveFilter;
    }

    public JButton getApplyRecipeButton() {
        return recipesPanel.getApplyRecipeButton();
    }

    public void updateRecipesPanel(DependencyInfo dependency) {
        if (!isPremiumUser) {
            recipesPanel.hide();
            return;
        }
        
        recipesPanel.updateForDependency(dependency);
    }
}
