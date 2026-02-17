package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus;
import adrianmikula.jakartamigration.intellij.ui.MigrationStrategyComponent.MigrationStrategy;
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
 * Migration phases component showing detailed phase descriptions.
 * Each phase displays a longer text description without subtask tables.
 */
public class MigrationPhasesComponent {
    private final JPanel panel;
    private final Project project;
    private final MigrationStrategyComponent strategyComponent;
    private final JTabbedPane phaseTabs;
    private MigrationStrategy selectedStrategy;
    private List<DependencyInfo> dependencies;
    private final List<PhaseListener> phaseListeners = new ArrayList<>();

    // Phase definitions with detailed descriptions
    private static final Map<MigrationStrategy, PhaseDefinition[]> PHASE_DEFINITIONS = new HashMap<>();

    static {
        PHASE_DEFINITIONS.put(MigrationStrategy.BIG_BANG, new PhaseDefinition[]{
            new PhaseDefinition("phase-1", "Complete Migration",
                """
                The Big Bang migration approach involves migrating all javax dependencies to Jakarta EE in a single, comprehensive pass. 
                This strategy is best suited for small to medium-sized projects that are relatively self-contained and have comprehensive 
                test coverage. The main advantage is speed - you complete the migration in one go, but this also means all your 
                eggs are in one basket. You'll need to thoroughly plan the migration, ensure you have a complete backup of your codebase, 
                and be prepared for a potentially longer rollback time if issues arise. This approach requires comprehensive testing 
                before starting and may cause extended downtime during the migration process.
                
                Key activities include updating all dependency declarations in your build files (pom.xml, build.gradle, etc.), 
                systematically replacing all javax.* imports with jakarta.* across every Java source file, updating XML configuration 
                files that reference javax, updating property files and resource bundles, and running your complete suite of 
                unit and integration tests to verify everything works correctly.
                """)
        });

        PHASE_DEFINITIONS.put(MigrationStrategy.INCREMENTAL, new PhaseDefinition[]{
            new PhaseDefinition("phase-1", "Dependency Updates",
                """
                The Incremental migration approach updates dependencies one at a time, starting with the lowest-risk ones and 
                progressing to higher-risk dependencies. This strategy is ideal for large, complex projects where a big-bang 
                approach would be too risky. By migrating incrementally, you can identify and fix issues as they arise, 
                rather than dealing with a massive cascade of failures.
                
                Start by analyzing your current dependency tree to understand which dependencies need to be migrated. 
                Focus on transitive dependencies first (those that are dependencies of your dependencies), then move 
                to direct dependencies. For each dependency, check if there's a Jakarta EE equivalent available. Update 
                the lowest-risk dependencies first - typically those with few dependents and good test coverage. After each 
                update, verify there are no breaking changes before proceeding to the next dependency.
                """),
            new PhaseDefinition("phase-2", "Import Replacement",
                """
                Once your dependencies have been updated to use Jakarta EE, the next step is to systematically replace 
                all javax.* imports with jakarta.* imports across your entire codebase. This is where the bulk of the 
                work happens. Use your IDE's refactoring tools to perform safe, global replacements.
                
                Start with transitive dependencies (libraries you've pulled in), then direct dependencies, then your 
                application code. Be careful with reflection-based code and configuration files that may contain 
                class names or package references. You'll also need to update any XML configuration files, deployment 
                descriptors, and property files that reference the old javax namespaces. Consider using OpenRewrite 
                recipes to automate much of this work.
                """),
            new PhaseDefinition("phase-3", "Testing & Verification",
                """
                After completing the import replacements, comprehensive testing is essential to ensure everything works 
                correctly. This phase involves running your full test suite to catch any issues that may have been 
                introduced during the migration.
                
                Run all unit tests first to verify individual components work correctly. Then run integration tests 
                to ensure different parts of your application work together properly. Pay special attention to testing 
                code that interacts with external systems, databases, or uses reflection. Verify that error handling 
                still works correctly and that any custom exception handling is still functional. Don't forget to 
                test any scheduled jobs, message consumers, and event handlers.
                """),
            new PhaseDefinition("phase-4", "Production Rollout",
                """
                The final phase involves deploying your migrated application to production. This requires careful 
                planning and monitoring to ensure a smooth transition.
                
                Start by deploying to a staging environment that mirrors production as closely as possible. Monitor 
                performance metrics closely during this phase - look for any degradation in response times, increased 
                error rates, or unusual behavior. Once you're confident the application is working correctly in staging, 
                deploy to production. Continue monitoring closely after the production deployment and have a rollback 
                plan ready in case critical issues arise.
                """)
        });

        PHASE_DEFINITIONS.put(MigrationStrategy.TRANSFORM, new PhaseDefinition[]{
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

        PHASE_DEFINITIONS.put(MigrationStrategy.MICROSERVICES, new PhaseDefinition[]{
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
     * Phase definition with detailed description.
     */
    public static class PhaseDefinition {
        private final String id;
        private final String name;
        private final String description;
        private final String[] steps;

        public PhaseDefinition(String id, String name, String description, String[] steps) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.steps = steps;
        }

        // Overloaded constructor for backward compatibility
        public PhaseDefinition(String id, String name, String description) {
            this(id, name, description, new String[0]);
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String[] getSteps() { return steps; }
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

        PhaseDefinition[] phases = PHASE_DEFINITIONS.getOrDefault(strategy,
            PHASE_DEFINITIONS.get(MigrationStrategy.INCREMENTAL));

        for (int i = 0; i < phases.length; i++) {
            PhaseDefinition phase = phases[i];
            
            // Create the tab content panel with just the description
            JPanel tabContent = createPhaseTabContent(phase);
            phaseTabs.addTab(phase.getName(), tabContent);
        }
    }

    private JPanel createPhaseTabContent(PhaseDefinition phase) {
        JPanel content = new JBPanel(new BorderLayout(10, 10));
        content.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Phase title
        JLabel titleLabel = new JLabel(phase.getName());
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        titleLabel.setBorder(new EmptyBorder(0, 0, 10, 0));

        // Phase description - longer text
        JTextArea descriptionArea = new JTextArea(phase.getDescription());
        descriptionArea.setEditable(false);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setFont(descriptionArea.getFont().deriveFont(Font.PLAIN, 13f));
        descriptionArea.setBackground(UIManager.getColor("Panel.background"));
        
        JScrollPane descScroll = new JScrollPane(descriptionArea);
        descScroll.setBorder(BorderFactory.createEmptyBorder(5, 0, 10, 0));

        content.add(titleLabel, BorderLayout.NORTH);
        content.add(descScroll, BorderLayout.CENTER);

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

        panel.add(mainContent, BorderLayout.CENTER);
    }

    public void addPhaseListener(PhaseListener listener) {
        phaseListeners.add(listener);
    }

    public MigrationStrategy getSelectedStrategy() {
        return selectedStrategy;
    }

    public JPanel getPanel() {
        return panel;
    }
}
