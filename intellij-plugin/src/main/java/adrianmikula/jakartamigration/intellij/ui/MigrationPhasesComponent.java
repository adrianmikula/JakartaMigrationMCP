package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus;
import adrianmikula.jakartamigration.intellij.ui.MigrationStrategyComponent.MigrationStrategy;
import adrianmikula.jakartamigration.intellij.ui.SubtaskTableComponent.SubtaskItem;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Migration phases component with expandable accordion UI.
 * Each phase displays a subtask table with action buttons for automatable tasks.
 */
public class MigrationPhasesComponent {
    private final JPanel panel;
    private final Project project;
    private final MigrationStrategyComponent strategyComponent;
    private final JTabbedPane phaseTabs;
    private final Map<String, SubtaskTableComponent> phaseSubtaskTables = new HashMap<>();
    private MigrationStrategy selectedStrategy;
    private List<DependencyInfo> dependencies;
    private final List<PhaseListener> phaseListeners = new ArrayList<>();

    // Phase definitions with detailed descriptions
    private static final Map<MigrationStrategy, PhaseDefinition[]> PHASE_DEFINITIONS = new HashMap<>();

    static {
        PHASE_DEFINITIONS.put(MigrationStrategy.BIG_BANG, new PhaseDefinition[]{
            new PhaseDefinition("phase-1", "Complete Migration",
                """
                Migrate all javax dependencies to Jakarta EE in a single pass.
                This approach requires comprehensive planning and testing.
                """,
                new String[]{
                    "Update dependency declarations in pom.xml/build.gradle",
                    "Replace javax imports in Java files",
                    "Update XML configuration files",
                    "Update property files and resource bundles",
                    "Run unit and integration tests"
                })
        });

        PHASE_DEFINITIONS.put(MigrationStrategy.INCREMENTAL, new PhaseDefinition[]{
            new PhaseDefinition("phase-1", "Dependency Updates",
                """
                Update dependencies incrementally, starting with the lowest-risk ones.
                Focus on transitive dependencies first, then direct dependencies.
                """,
                new String[]{
                    "Analyze current dependencies",
                    "Check for Jakarta equivalents",
                    "Update lowest-risk dependencies first",
                    "Verify no breaking changes",
                    "Repeat for remaining dependencies"
                }),
            new PhaseDefinition("phase-2", "Import Replacement",
                """
                Systematically replace javax.* imports with jakarta.* across the codebase.
                Use IDE refactoring tools for safe replacement.
                """,
                new String[]{
                    "Replace imports in transitive dependencies",
                    "Replace imports in direct dependencies",
                    "Replace imports in application code",
                    "Update any reflection ConfigFiles"
                }),
            new PhaseDefinition("phase-3", "Testing & Verification",
                """
                Comprehensive testing after import replacements.
                Ensure no compilation errors and all tests pass.
                """,
                new String[]{
                    "Run unit tests",
                    "Run integration tests",
                    "Verify interactions",
                    "Test error handling"
                }),
            new PhaseDefinition("phase-4", "Production Rollout",
                """
                Deploy the runtime transformation solution.
                Monitor for issues in production environment.
                """,
                new String[]{
                    "Deploy to staging",
                    "Monitor performance",
                    "Deploy to production"
                })
        });

        PHASE_DEFINITIONS.put(MigrationStrategy.BUILD_TRANSFORMATION, new PhaseDefinition[]{
            new PhaseDefinition("phase-1", "Build Tool Updates",
                """
                Update build configuration to use Jakarta EE dependencies.
                This involves modifying pom.xml or build.gradle files.
                """,
                new String[]{
                    "Add Jakarta EE BOM to dependencyManagement",
                    "Update artifact coordinates",
                    "Configure Jakarta EE version",
                    "Verify build compiles"
                }),
            new PhaseDefinition("phase-2", "Runtime Transformation Setup",
                """
                Configure runtime transformation for javax to jakarta conversion.
                Options include Eclipse Transformer or custom class loading.
                """,
                new String[]{
                    "Install Eclipse Transformer",
                    "Configure transformation rules",
                    "Test transformation pipeline",
                    "Verify runtime behavior"
                }),
            new PhaseDefinition("phase-3", "Gradual Migration",
                """
                Migrate code gradually while maintaining backward compatibility.
                Migrate one module at a time.
                """,
                new String[]{
                    "Migrate lowest-risk module first",
                    "Test module in isolation",
                    "Repeat for each module",
                    "Verify cross-module compatibility"
                }),
            new PhaseDefinition("phase-4", "Complete Migration",
                """
                Complete the migration by removing runtime transformation.
                All code now uses Jakarta EE directly.
                """,
                new String[]{
                    "Remove transformation configuration",
                    "Verify all tests pass",
                    "Deploy fully migrated application"
                })
        });

        PHASE_DEFINITIONS.put(MigrationStrategy.RUNTIME_TRANSFORMATION, new PhaseDefinition[]{
            new PhaseDefinition("phase-1", "Adapter Pattern Setup",
                """
                Create adapter classes for javax to jakarta compatibility.
                """,
                new String[]{
                    "Identify classes needing adapters",
                    "Create adapter classes",
                    "Configure class loading",
                    "Test adapter functionality"
                }),
            new PhaseDefinition("phase-2", "Runtime Configuration",
                """
                Configure runtime behavior for mixed javax/jakarta environment.
                """,
                new String[]{
                    "Configure class loaders",
                    "Set transformation rules",
                    "Manage dependency conflicts",
                    "Test runtime behavior"
                }),
            new PhaseDefinition("phase-3", "Incremental Migration",
                """
                Gradually migrate code while maintaining runtime compatibility.
                """,
                new String[]{
                    "Migrate lowest-risk component",
                    "Test with existing adapters",
                    "Repeat for remaining components",
                    "Verify end-to-end functionality"
                }),
            new PhaseDefinition("phase-4", "Remove Adapters",
                """
                Complete migration by removing all adapters.
                """,
                new String[]{
                    "Remove adapter classes",
                    "Update build configuration",
                    "Final testing",
                    "Deploy to production"
                })
        });
    }

    public interface PhaseListener {
        void onStrategySelected(MigrationStrategy strategy);
        void onPhaseSelected(int phaseIndex);
    }

    /**
     * Phase definition with detailed description and subtasks.
     */
    public static class PhaseDefinition {
        private final String id;
        private final String name;
        private final String description;
        private final String[] subtasks;

        public PhaseDefinition(String id, String name, String description, String[] subtasks) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.subtasks = subtasks;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String[] getSubtasks() { return subtasks; }
    }

    public MigrationPhasesComponent(Project project) {
        this.project = project;
        this.dependencies = new ArrayList<>();
        this.panel = new JBPanel(new BorderLayout());
        this.strategyComponent = new MigrationStrategyComponent(project);
        this.phaseTabs = new JTabbedPane();

        initializeComponent();
        setupStrategyListener();

        // Set default strategy and update phases
        this.selectedStrategy = MigrationStrategy.INCREMENTAL;
        updatePhasesForStrategy(selectedStrategy);
    }

    private void setupStrategyListener() {
        strategyComponent.addMigrationStrategyListener(strategy -> {
            this.selectedStrategy = strategy;
            for (PhaseListener listener : phaseListeners) {
                listener.onStrategySelected(strategy);
            }
            updatePhasesForStrategy(strategy);
        });
    }

    public void setDependencies(List<DependencyInfo> dependencies) {
        this.dependencies = dependencies != null ? dependencies : new ArrayList<>();
        if (selectedStrategy != null) {
            updatePhasesForStrategy(selectedStrategy);
        }
    }

    private void updatePhasesForStrategy(MigrationStrategy strategy) {
        phaseTabs.removeAll();
        phaseSubtaskTables.clear();

        PhaseDefinition[] phases = PHASE_DEFINITIONS.getOrDefault(strategy,
            PHASE_DEFINITIONS.get(MigrationStrategy.INCREMENTAL));

        for (int i = 0; i < phases.length; i++) {
            PhaseDefinition phase = phases[i];
            SubtaskTableComponent subtaskTable = new SubtaskTableComponent(project);
            phaseSubtaskTables.put(phase.getId(), subtaskTable);

            // Create subtasks for this phase
            List<SubtaskItem> subtasks = createSubtasksForPhase(phase, dependencies);

            // First phase gets NEEDS_UPGRADE dependencies, others get REQUIRES_MANUAL_MIGRATION
            DependencyMigrationStatus targetStatus = (i == 0) ?
                DependencyMigrationStatus.NEEDS_UPGRADE : DependencyMigrationStatus.REQUIRES_MANUAL_MIGRATION;

            subtaskTable.setSubtasks(subtasks);

            // Create the tab content panel
            JPanel tabContent = createPhaseTabContent(phase, subtaskTable);
            phaseTabs.addTab(phase.getName(), tabContent);
        }
    }

    private List<SubtaskItem> createSubtasksForPhase(PhaseDefinition phase, List<DependencyInfo> deps) {
        List<SubtaskItem> items = new ArrayList<>();

        // Add phase subtasks with automation detection
        for (String task : phase.getSubtasks()) {
            String automationType = determineAutomationType(task);
            items.add(new SubtaskItem(task, "", null, automationType));
        }

        // Add dependency-specific subtasks for first phase
        if (deps != null && !deps.isEmpty()) {
            // Add a separator subtask
            items.add(new SubtaskItem("---", "", null, null));

            List<DependencyInfo> needsUpgrade = deps.stream()
                .filter(d -> d.getMigrationStatus() == DependencyMigrationStatus.NEEDS_UPGRADE)
                .toList();

            for (DependencyInfo dep : needsUpgrade.stream().limit(5).toList()) {
                String task = String.format("Migrate %s", dep.getArtifactId());
                items.add(new SubtaskItem(task, "", dep, "dependency-update"));
            }

            if (needsUpgrade.size() > 5) {
                items.add(new SubtaskItem(String.format("... and %d more dependencies", needsUpgrade.size() - 5),
                    "", null, null));
            }
        }

        return items;
    }

    private String determineAutomationType(String task) {
        String lower = task.toLowerCase();
        if (lower.contains("import") || lower.contains("refactor") || lower.contains("rewrite")) {
            return "open-rewrite";
        } else if (lower.contains("binary") || lower.contains("scan") || lower.contains("analyze")) {
            return "binary-scan";
        } else if (lower.contains("update") || lower.contains("upgrade") || lower.contains("dependency")) {
            return "dependency-update";
        }
        return null;
    }

    private JPanel createPhaseTabContent(PhaseDefinition phase, SubtaskTableComponent subtaskTable) {
        JPanel content = new JBPanel(new BorderLayout(10, 10));
        content.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Phase description
        JTextArea descriptionArea = new JTextArea(phase.getDescription());
        descriptionArea.setEditable(false);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setFont(descriptionArea.getFont().deriveFont(Font.ITALIC));
        descriptionArea.setBackground(UIManager.getColor("Panel.background"));
        JScrollPane descScroll = new JScrollPane(descriptionArea);
        descScroll.setBorder(BorderFactory.createEmptyBorder(5, 0, 10, 0));
        descScroll.setPreferredSize(new Dimension(0, 80));

        // Subtask table
        JPanel subtaskPanel = subtaskTable.getPanel();

        // Phase actions
        JPanel actionsPanel = new JBPanel(new FlowLayout(FlowLayout.LEFT));
        JButton startButton = new JButton("Start Phase");
        startButton.addActionListener(e -> startPhase(phase));
        JButton viewDetailsButton = new JButton("View Details");
        viewDetailsButton.addActionListener(e -> viewPhaseDetails(phase));

        actionsPanel.add(startButton);
        actionsPanel.add(viewDetailsButton);

        content.add(descScroll, BorderLayout.NORTH);
        content.add(subtaskPanel, BorderLayout.CENTER);
        content.add(actionsPanel, BorderLayout.SOUTH);

        return content;
    }

    private void initializeComponent() {
        // Strategy header
        JPanel strategyHeaderPanel = new JBPanel(new BorderLayout());
        strategyHeaderPanel.add(new JLabel("Migration Strategy:"), BorderLayout.WEST);
        strategyHeaderPanel.add(strategyComponent.getPanel(), BorderLayout.CENTER);
        strategyHeaderPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        // Main content
        JPanel mainContent = new JBPanel(new BorderLayout());
        mainContent.add(strategyHeaderPanel, BorderLayout.NORTH);

        // Phase tabs
        phaseTabs.setTabLayoutPolicy(JTabbedPane.TOP);
        JScrollPane tabsScroll = new JScrollPane(phaseTabs);
        tabsScroll.setBorder(BorderFactory.createTitledBorder("Migration Phases"));
        mainContent.add(tabsScroll, BorderLayout.CENTER);

        // Execution status panel
        JPanel statusPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        statusPanel.setBorder(BorderFactory.createTitledBorder("Execution Status"));

        statusPanel.add(new JLabel("Current Phase:"));
        JLabel currentPhaseLabel = new JLabel("None");
        statusPanel.add(currentPhaseLabel);
        statusPanel.add(new JLabel("Status:"));
        JLabel statusLabel = new JLabel("Not Started");
        statusPanel.add(statusLabel);

        mainContent.add(statusPanel, BorderLayout.SOUTH);

        panel.add(mainContent, BorderLayout.CENTER);
    }

    private void startPhase(PhaseDefinition phase) {
        String message = String.format("""
            Start Migration Phase

            Phase: %s

            This will begin the execution of tasks in this phase.
            Automated tasks will run using their configured tools.
            """, phase.getName());

        int result = Messages.showYesNoDialog(project, message, "Start Phase",
                Messages.getQuestionIcon());

        if (result == Messages.YES) {
            Messages.showInfoMessage(project, "Phase '" + phase.getName() + "' started.",
                "Migration Phase");
        }
    }

    private void viewPhaseDetails(PhaseDefinition phase) {
        StringBuilder details = new StringBuilder();
        details.append("Phase: ").append(phase.getName()).append("\n\n");
        details.append("Description:\n").append(phase.getDescription()).append("\n\n");
        details.append("Subtasks:\n");
        for (String subtask : phase.getSubtasks()) {
            details.append("  • ").append(subtask).append("\n");
        }

        Messages.showInfoMessage(project, details.toString(), "Phase Details");
    }

    public JPanel getPanel() {
        return panel;
    }

    public void addPhaseListener(PhaseListener listener) {
        phaseListeners.add(listener);
    }
}
