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
 * Phase data is loaded from property files via UiTextLoader.
 */
public class MigrationPhasesComponent {
    private final JPanel panel;
    private final Project project;
    private final MigrationStrategyComponent strategyComponent;
    private final JTabbedPane phaseTabs;
    private MigrationStrategy selectedStrategy;
    private List<DependencyInfo> dependencies;
    private final List<PhaseListener> phaseListeners = new ArrayList<>();

    // Phase definitions - loaded dynamically from property files
    private static final Map<MigrationStrategy, PhaseDefinition[]> PHASE_DEFINITIONS = loadPhaseDefinitions();

    /**
     * Load phase definitions from property files.
     */
    private static Map<MigrationStrategy, PhaseDefinition[]> loadPhaseDefinitions() {
        Map<MigrationStrategy, PhaseDefinition[]> definitions = new HashMap<>();
        
        for (MigrationStrategy strategy : MigrationStrategy.values()) {
            String strategyKey = strategy.name().toLowerCase();
            int phaseCount = UiTextLoader.getPhaseCount(strategyKey);
            
            if (phaseCount > 0) {
                PhaseDefinition[] phases = new PhaseDefinition[phaseCount];
                for (int i = 0; i < phaseCount; i++) {
                    int phaseNum = i + 1;
                    String title = UiTextLoader.getPhaseTitle(strategyKey, phaseNum);
                    String description = UiTextLoader.getPhaseDescription(strategyKey, phaseNum);
                    String[] steps = UiTextLoader.getPhaseStepsArray(strategyKey, phaseNum);
                    phases[i] = new PhaseDefinition("phase-" + phaseNum, title, description, steps);
                }
                definitions.put(strategy, phases);
            }
        }
        
        // If no phases loaded from properties, use defaults
        if (definitions.isEmpty()) {
            definitions.put(MigrationStrategy.INCREMENTAL, new PhaseDefinition[]{
                new PhaseDefinition("phase-1", "Dependency Updates", "Run analysis to identify dependencies that need migration."),
                new PhaseDefinition("phase-2", "Import Replacement", "Replace javax.* imports with jakarta.* imports."),
                new PhaseDefinition("phase-3", "Testing & Verification", "Run tests to verify the migration."),
                new PhaseDefinition("phase-4", "Production Rollout", "Deploy the migrated application to production.")
            });
        }
        
        return definitions;
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
