package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import adrianmikula.jakartamigration.dependencyanalysis.service.ImprovedMavenCentralLookupService;
import adrianmikula.jakartamigration.dependencyanalysis.service.ImprovedMavenCentralLookupService.JakartaArtifactMatch;
import adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus;
import adrianmikula.jakartamigration.dependencyanalysis.config.CompatibilityConfigLoader;
import adrianmikula.jakartamigration.dependencyanalysis.config.CompatibilityConfigLoader.ArtifactClassification;
import adrianmikula.jakartamigration.platforms.config.PlatformConfigLoader;
import adrianmikula.jakartamigration.platforms.model.PlatformConfig;
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
public class DependenciesTableComponent {
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
    
    // Bottom panel for recipes
    private JPanel recipesPanel;
    private JList<String> recipeList;
    private DefaultListModel<String> recipeListModel;
    private JButton applyRecipeButton;
    private JLabel selectedDependencyLabel;
    private boolean isPremiumUser = false;
    private final PlatformConfigLoader platformConfigLoader;
    private final CompatibilityConfigLoader compatibilityConfigLoader;

    // Status colors - matches DependencyGraphComponent color schema
    private static final Color STATUS_COMPATIBLE = new Color(40, 167, 69); // Green
    private static final Color STATUS_NEEDS_UPGRADE = new Color(255, 193, 7); // Yellow
    private static final Color STATUS_REQUIRES_MANUAL = new Color(255, 193, 7); // Yellow (same as needs upgrade)
    private static final Color STATUS_NO_JAKARTA = new Color(220, 53, 69); // Red
    private static final Color STATUS_MIGRATED = new Color(23, 162, 184); // Cyan
    private static final Color STATUS_UNKNOWN = new Color(108, 117, 125); // Gray
    
    // Callback interface for notifying when analysis completes
    public interface OnAnalysisCompleteListener {
        void onAnalysisComplete(List<DependencyInfo> dependencies);
    }
    private OnAnalysisCompleteListener analysisCompleteListener;

    public DependenciesTableComponent(Project project) {
        this.project = project;
        this.allDependencies = new ArrayList<>();
        this.platformConfigLoader = new PlatformConfigLoader();
        this.compatibilityConfigLoader = new CompatibilityConfigLoader();
        this.panel = new JBPanel<>(new BorderLayout());

        // Columns with Jakarta Equivalent information and Maven Coordinates
        String[] columns = {
                "Group ID",
                "Artifact ID",
                "Current Version",
                "Jakarta Equivalent",
                "Recommended Version",
                "Maven Coordinates",
                "Status",
                "Type",
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
        };

        this.searchField = new JTextField(20);
        this.statusFilter = new JComboBox<>(new String[] {
                "All", "Compatible", "Needs Upgrade", "No Jakarta Version", "Unknown"
        });
        this.transitiveFilter = new JCheckBox("Show Transitive Only", false);
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
            // Status column is at index 6, Jakarta Equivalent at index 3, Maven Coordinates at index 5, DependencyInfo at index 8
            if ((column == 6 || column == 3) && row < table.getModel().getRowCount()) {
                Object depObj = table.getModel().getValueAt(row, 8);
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
                // Determine if this row is organizational (check hidden column at index 8)
                boolean isOrg = false;
                if (row < table.getModel().getRowCount()) {
                    Object depObj = table.getModel().getValueAt(row, 8);
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
         * Get the status color based on migration status - matches DependencyGraphComponent.
         */
        private Color getStatusColor(DependencyMigrationStatus status) {
            if (status == null) {
                return STATUS_UNKNOWN;
            }
            return switch (status) {
                case COMPATIBLE -> STATUS_COMPATIBLE;
                case NEEDS_UPGRADE, REQUIRES_MANUAL_MIGRATION -> STATUS_NEEDS_UPGRADE;
                case NO_JAKARTA_VERSION -> STATUS_NO_JAKARTA;
                case MIGRATED -> STATUS_MIGRATED;
                default -> STATUS_UNKNOWN;
            };
        }

        /**
         * Get background color for a status (lighter version for table cells).
         */
        private Color getStatusBackgroundColor(DependencyMigrationStatus status) {
            Color baseColor = getStatusColor(status);
            return new Color(
                Math.min(255, baseColor.getRed() + 50),
                Math.min(255, baseColor.getGreen() + 50),
                Math.min(255, baseColor.getBlue() + 50)
            );
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

        statusFilter.addActionListener(e -> filterDependencies());
        headerPanel.add(statusFilter);

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
        table.getColumnModel().getColumn(4).setPreferredWidth(100); // Recommended Version
        table.getColumnModel().getColumn(5).setPreferredWidth(200); // Maven Coordinates
        table.getColumnModel().getColumn(6).setPreferredWidth(120); // Status (color-coded)
        table.getColumnModel().getColumn(7).setPreferredWidth(80);  // Type
        table.getColumnModel().getColumn(8).setMinWidth(0);       // Hidden DependencyInfo
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

        // Create center panel with table and recipes
        JPanel centerPanel = new JBPanel<>(new BorderLayout());
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        centerPanel.add(recipesPanel, BorderLayout.SOUTH);

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(centerPanel, BorderLayout.CENTER);
        panel.add(actionsPanel, BorderLayout.SOUTH);
    }

    private void initializeRecipesPanel() {
        recipesPanel = new JBPanel<>(new BorderLayout());
        recipesPanel.setBorder(new TitledBorder("Refactoring Recipes"));
        recipesPanel.setPreferredSize(new Dimension(0, 150));
        
        selectedDependencyLabel = new JLabel("Select a dependency to see available recipes");
        selectedDependencyLabel.setBorder(new EmptyBorder(5, 5, 5, 5));
        recipesPanel.add(selectedDependencyLabel, BorderLayout.NORTH);
        
        recipeListModel = new DefaultListModel<>();
        recipeList = new JList<>(recipeListModel);
        recipeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        JBScrollPane recipeScrollPane = new JBScrollPane(recipeList);
        recipesPanel.add(recipeScrollPane, BorderLayout.CENTER);
        
        JPanel recipeActionsPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT));
        applyRecipeButton = new JButton("Apply Recipe");
        applyRecipeButton.setEnabled(false);
        applyRecipeButton.addActionListener(this::handleApplyRecipe);
        recipeActionsPanel.add(applyRecipeButton);
        
        JLabel premiumHint = new JLabel("(Premium feature)");
        premiumHint.setForeground(Color.GRAY);
        recipeActionsPanel.add(premiumHint);
        
        recipesPanel.add(recipeActionsPanel, BorderLayout.SOUTH);
        
        // Initially hide the recipes panel
        recipesPanel.setVisible(false);
    }

    private void updateRecipesPanel() {
        List<DependencyInfo> selected = getSelectedDependencies();
        
        if (selected.isEmpty()) {
            recipesPanel.setVisible(false);
            return;
        }
        
        DependencyInfo dep = selected.get(0);
        
        // Show the recipes panel
        recipesPanel.setVisible(true);
        
        // Update the label
        String recommendedCoords = dep.getRecommendedArtifactCoordinates();
        if (recommendedCoords != null && !recommendedCoords.isEmpty() && !recommendedCoords.equals("-")) {
            selectedDependencyLabel.setText("Selected: " + dep.getDisplayName() + " → " + recommendedCoords);
        } else {
            selectedDependencyLabel.setText("Selected: " + dep.getDisplayName());
        }
        
        // Update recipe list
        recipeListModel.clear();
        
        String associatedRecipe = dep.getAssociatedRecipeName();
        if (associatedRecipe != null && !associatedRecipe.isEmpty()) {
            recipeListModel.addElement(associatedRecipe);
            recipeList.setSelectedIndex(0);
            applyRecipeButton.setEnabled(true);
        } else if (dep.getRecommendedVersion() != null && !dep.getRecommendedVersion().equals("-")) {
            // Add generic upgrade recipe if there's a recommended version
            String genericRecipe = "Upgrade to Jakarta: " + dep.getRecommendedArtifactCoordinates();
            recipeListModel.addElement(genericRecipe);
            recipeList.setSelectedIndex(0);
            applyRecipeButton.setEnabled(true);
        } else {
            recipeListModel.addElement("No recipes available");
            applyRecipeButton.setEnabled(false);
        }
    }

    private void handleApplyRecipe(ActionEvent e) {
        List<DependencyInfo> selected = getSelectedDependencies();
        if (selected.isEmpty()) {
            return;
        }
        
        DependencyInfo dep = selected.get(0);
        String selectedRecipe = recipeList.getSelectedValue();
        
        if (selectedRecipe == null || selectedRecipe.contains("No recipes")) {
            return;
        }
        
        if (!isPremiumUser) {
            Messages.showWarningDialog(project, 
                    "Applying recipes requires a Premium license.\nPlease upgrade to Premium to use this feature.", 
                    "Premium Feature");
            return;
        }
        
        // For premium users, apply recipe directly without confirmation dialog
        applyRecipeDirectly(dep, selectedRecipe);
    }
    
    private void applyRecipeDirectly(DependencyInfo dep, String selectedRecipe) {
        // This would trigger the recipe execution
        // Get the refactoring service and apply the recipe
        try {
            adrianmikula.jakartamigration.coderefactoring.service.RecipeService recipeService = 
                project.getService(adrianmikula.jakartamigration.coderefactoring.service.RecipeService.class);
            
            if (recipeService != null) {
                // Apply the recipe to migrate the dependency
                java.nio.file.Path projectPath = java.nio.file.Paths.get(project.getBasePath());
                adrianmikula.jakartamigration.coderefactoring.domain.RecipeExecutionResult result = recipeService.applyRecipe(selectedRecipe, projectPath);
                
                if (result != null && result.success()) {
                    Messages.showInfoMessage(project, 
                            "Successfully applied recipe '" + selectedRecipe + "' to migrate " + dep.getDisplayName() + ".\n\nThe refactoring has been completed. Please review the changes and run tests.", 
                            "Recipe Applied Successfully");
                } else {
                    String errorMessage = result != null ? result.errorMessage() : "Unknown error";
                    Messages.showErrorDialog(project, 
                            "Failed to apply recipe '" + selectedRecipe + "' to migrate " + dep.getDisplayName() + ".\n\nError: " + errorMessage, 
                            "Recipe Application Failed");
                }
            } else {
                Messages.showErrorDialog(project, 
                        "Recipe service not available. Please ensure the Jakarta Migration plugin is properly configured.", 
                        "Service Unavailable");
            }
        } catch (Exception e) {
            Messages.showErrorDialog(project, 
                    "Error applying recipe '" + selectedRecipe + "': " + e.getMessage(), 
                    "Recipe Application Error");
        }
    }

    public void setPremiumUser(boolean isPremium) {
        this.isPremiumUser = isPremium;
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

    public void setDependencies(List<DependencyInfo> dependencies) {
        this.allDependencies = dependencies != null ? dependencies : new ArrayList<>();
        filterDependencies();
        
        // Automatically trigger Maven Central lookup for new dependencies
        if (!this.allDependencies.isEmpty()) {
            queryMavenCentralForDependencies();
        }
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
        boolean showTransitiveOnly = transitiveFilter.isSelected();
        boolean showOrganizationalOnly = organizationalFilter.isSelected();

        for (DependencyInfo dep : allDependencies) {
            // Search filter
            boolean matchesSearch = searchText.isEmpty() ||
                    dep.getGroupId().toLowerCase().contains(searchText) ||
                    dep.getArtifactId().toLowerCase().contains(searchText) ||
                    dep.getCurrentVersion().toLowerCase().contains(searchText);

            // Status filter
            boolean matchesStatus = "All".equals(selectedStatus) ||
                    (selectedStatus != null && dep.getMigrationStatus() != null &&
                            dep.getMigrationStatus().getValue().equals(mapStatusToValue(selectedStatus)));

            // Transitive filter
            boolean matchesTransitive = !showTransitiveOnly || dep.isTransitive();

            // Organizational filter
            boolean matchesOrganizational = !showOrganizationalOnly || dep.isOrganizational();

            if (matchesSearch && matchesStatus && matchesTransitive && matchesOrganizational) {
                addDependencyRow(dep);
            }
        }
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
        
        // Jakarta Equivalent
        String jakartaEquivalent = dep.getRecommendedArtifactCoordinates() != null 
                ? dep.getRecommendedArtifactCoordinates() 
                : "-";
        
        // Compatibility Status - determines color coding
        String statusText;
        boolean hasJakartaEquivalent = jakartaEquivalent != null && !jakartaEquivalent.equals("-");
        
        if (dep.getMigrationStatus() == DependencyMigrationStatus.COMPATIBLE) {
            statusText = "✓ Compatible";
        } else if (hasJakartaEquivalent) {
            statusText = "↑ Upgrade Available";
        } else if (dep.getMigrationStatus() == DependencyMigrationStatus.NO_JAKARTA_VERSION) {
            statusText = "✗ No Jakarta Version";
        } else {
            statusText = "? Unknown";
        }

        // Build Maven Coordinates string (groupId:artifactId:version)
        String mavenCoordinates = "-";
        if (dep.getRecommendedGroupId() != null && dep.getRecommendedArtifactId() != null) {
            mavenCoordinates = dep.getRecommendedGroupId() + ":" + 
                              dep.getRecommendedArtifactId() + 
                              (dep.getRecommendedVersion() != null ? ":" + dep.getRecommendedVersion() : "");
        } else if (hasJakartaEquivalent) {
            // Fallback to using the Jakarta Equivalent coordinates
            mavenCoordinates = jakartaEquivalent;
        }

        // Add row with all columns - DependencyInfo at column 8 (hidden)
        tableModel.addRow(new Object[] {
                dep.getGroupId(),
                dep.getArtifactId(),
                dep.getCurrentVersion(),
                jakartaEquivalent,
                dep.getRecommendedVersion() != null ? dep.getRecommendedVersion() : "-",
                mavenCoordinates,
                statusText,
                dependencyType,
                dep // Full object for renderer (hidden column)
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
                // Use compatibility config to classify javax dependencies
                ArtifactClassification classification = compatibilityConfigLoader.classifyArtifact(
                    dep.getGroupId(), dep.getArtifactId());
                
                switch (classification) {
                    case JDK_PROVIDED:
                        // JDK-provided packages - no Jakarta migration needed
                        dep.setMigrationStatus(DependencyMigrationStatus.COMPATIBLE);
                        dep.setJakartaCompatibilityStatus("JDK Provided - No migration needed");
                        System.out.println("[DEBUG] JDK-provided (no migration): " + dep.getGroupId() + ":" + dep.getArtifactId());
                        break;
                        
                    case JAKARTA_REQUIRED:
                        // Must migrate to Jakarta EE - query Maven Central
                        javaxDependencies.add(dep);
                        System.out.println("[DEBUG] Jakarta required: " + dep.getGroupId() + ":" + dep.getArtifactId());
                        break;
                        
                    case CONTEXT_DEPENDENT:
                        // Ambiguous - requires manual review or Maven lookup
                        dep.setMigrationStatus(DependencyMigrationStatus.REQUIRES_MANUAL_MIGRATION);
                        dep.setJakartaCompatibilityStatus("Review Required - Context dependent");
                        // Also query Maven Central as a hint
                        javaxDependencies.add(dep);
                        System.out.println("[DEBUG] Context dependent (manual review): " + dep.getGroupId() + ":" + dep.getArtifactId());
                        break;
                        
                    case UNKNOWN:
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
        SwingUtilities.invokeLater(() -> {
            progressBar.setVisible(true);
            progressBar.setIndeterminate(true);
            progressBar.setString("Querying Maven Central for " + javaxDependencies.size() + " dependencies...");
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
        if (jakartaArtifacts == null || jakartaArtifacts.isEmpty()) {
            // No Jakarta artifacts found - mark as UNKNOWN
            javaxDep.setMigrationStatus(DependencyMigrationStatus.UNKNOWN);
            javaxDep.setJakartaCompatibilityStatus("Unknown - No Jakarta equivalent found");
            System.out.println("[DEBUG] No Jakarta equivalent found for: " + javaxDep.getGroupId() + ":" + javaxDep.getArtifactId());
            return;
        }
        
        // Filter to only found artifacts and get best match
        JakartaArtifactMatch bestMatch = jakartaArtifacts.stream()
                .filter(JakartaArtifactMatch::found)
                .findFirst()
                .orElse(null);
        
        if (bestMatch == null) {
            // No valid matches found - mark as UNKNOWN
            javaxDep.setMigrationStatus(DependencyMigrationStatus.UNKNOWN);
            javaxDep.setJakartaCompatibilityStatus("Unknown - No valid Jakarta match");
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
                
                allDependencies.set(i, updatedDep);
                break;
            }
        }
    }
    
    private void handleUpdate(ActionEvent e) {
        List<DependencyInfo> selected = getSelectedDependencies();
        if (selected.isEmpty()) {
            Messages.showWarningDialog(project, "Please select dependencies to update.", "No Selection");
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
            Messages.showWarningDialog(project, "Please select a dependency to view details.", "No Selection");
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
        return applyRecipeButton;
    }

    public void updateRecipesPanel(DependencyInfo dependency) {
        if (!isPremiumUser) {
            recipesPanel.setVisible(false);
            return;
        }
        
        recipesPanel.setVisible(true);
        selectedDependencyLabel.setText("Selected: " + dependency.getDisplayName());
        
        recipeListModel.clear();
        
        String associatedRecipe = dependency.getAssociatedRecipeName();
        if (associatedRecipe != null && !associatedRecipe.isEmpty()) {
            recipeListModel.addElement(associatedRecipe);
            recipeList.setSelectedIndex(0);
            applyRecipeButton.setEnabled(true);
        } else if (dependency.getRecommendedVersion() != null && !dependency.getRecommendedVersion().equals("-")) {
            // Add generic upgrade recipe if there's a recommended version
            String genericRecipe = "Upgrade to Jakarta: " + dependency.getRecommendedArtifactCoordinates();
            recipeListModel.addElement(genericRecipe);
            recipeList.setSelectedIndex(0);
            applyRecipeButton.setEnabled(true);
        } else {
            recipeListModel.addElement("No recipes available");
            applyRecipeButton.setEnabled(false);
        }
    }
}
