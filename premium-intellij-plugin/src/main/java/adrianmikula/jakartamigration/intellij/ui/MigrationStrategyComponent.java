package adrianmikula.jakartamigration.intellij.ui;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Migration strategy selection component with strategy details and risks.
 * Displays clickable cards for different migration strategies:
 * - Big Bang (all at once)
 * - Incremental/One by One
 * - Build Transformation
 * - Runtime Transformation
 */
public class MigrationStrategyComponent {
    private final JPanel panel;
    private final Project project;
    private final List<MigrationStrategyListener> listeners = new ArrayList<>();
    private MigrationStrategy selectedStrategy;
    private JTextArea benefitsText;
    private JTextArea risksText;
    private JTextArea phasesText;

    public interface MigrationStrategyListener {
        void onStrategySelected(MigrationStrategy strategy);
    }

    public enum MigrationStrategy {
        BIG_BANG("Big Bang", "Migrate everything at once",
                """
                        • Migrate all javax dependencies to Jakarta EE at once
                        • Single comprehensive change
                        • Best for small, self-contained projects
                        • Requires thorough testing before starting
                        """,
                """
                        • Higher risk - issues affect entire codebase
                        • Longer rollback time if problems occur
                        • Requires comprehensive test suite
                        • May cause extended downtime during migration
                        """,
                """
                        1. Dependency Upgrade: Update all pom.xml/build.gradle files
                        2. Code Refactor: Replace all javax.* imports with jakarta.*
                        3. XML/Config Update: Update persistence.xml, web.xml, etc.
                        4. Global Testing: Comprehensive unit and integration testing
                        """,
                new Color(220, 53, 69)), // Red

        INCREMENTAL("Incremental", "One dependency at a time",
                """
                        • Migrate dependencies incrementally
                        • Update one dependency, test, then proceed
                        • Lower risk per change
                        • Best for large, complex projects
                        """,
                """
                        • Longer overall migration timeline
                        • Must maintain compatibility during transition
                        • May require temporary dual dependencies
                        • Need careful dependency ordering
                        """,
                """
                        1. Dependency Scan: Identify all javax dependencies
                        2. Priority Ranking: Order by risk and dependency level
                        3. Step-by-Step Upgrade: One artifact at a time
                        4. Continuous Integration: Test after every single change
                        """,
                new Color(255, 193, 7)), // Yellow

        STRANGLER("Strangler", "Migrate module by module",
                """
                        • Migrate one functional module or service at a time
                        • New features built in Jakarta EE
                        • Existing features gradually migrated
                        • Good for monolithic applications
                        """,
                """
                        • Requires inter-module compatibility layers
                        • Can create duplicate logic during transition
                        • Managing two different EE environments simultaneously
                        """,
                """
                        1. Interface Definition: Define boundaries between modules
                        2. Bridge Setup: Create compatibility layer for cross-module calls
                        3. Vertical Slices: Migrate one full functional slice at a time
                        4. Decommission: Remove legacy modules once fully replaced
                        """,
                new Color(111, 66, 193)), // Purple

        DUAL_BUILDS("Dual Builds", "Support both javax and jakarta simultaneously",
                """
                        • Build two versions of the app from one codebase
                        • Use build-time shading or source transformation
                        • High flexibility for library developers
                        """,
                """
                        • Increased CI/CD complexity
                        • Harder to debug build-time transformations
                        • Source code remains in one format (usually javax)
                        """,
                """
                        1. Multi-Release Setup: Configure build tool for shadow JARs
                        2. Transformer Integration: Setup Eclipse Transformer in build
                        3. Automated CI: Running two sets of tests (javax/jakarta)
                        4. Deployment Choice: Select version based on target runtime
                        """,
                new Color(253, 126, 20)), // Orange

        TRANSFORM("Transform", "Automatic bytecode/source transformation",
                """
                        • Use OpenRewrite/Eclipse Transformer automatically
                        • Large scale changes handled by machine
                        • Consistent application of rules
                        """,
                """
                        • Hard to handle complex custom logic
                        • Machine changes still require human review
                        • Risk of unexpected transformation errors
                        """,
                """
                        1. Recipe Selection: Choose standard and custom Rewrite recipes
                        2. Batch Execution: Run transformation across the whole codebase
                        3. Diff Review: Manual inspection of critical logic changes
                        4. Final Validation: Automated test suite verification
                        """,
                new Color(23, 162, 184)), // Blue

        ADAPTER("Adapter", "Runtime compatibility layer",
                """
                        • Use adapter pattern for runtime compatibility
                        • No code changes required
                        • Quick to implement
                        • Good for legacy systems
                        """,
                """
                        • Performance overhead at runtime
                        • Limited to supported patterns
                        • Not a permanent solution
                        • May have edge case issues
                        """,
                """
                        1. Adapter Config: Setup runtime bytecode instrumentation
                        2. Runtime Proxy: Intercept javax calls and redirect to jakarta
                        3. Legacy Support: Link old libraries to new EE runtime
                        4. Monitor: Aggressive monitoring of performance/errors
                        """,
                new Color(40, 167, 69)); // Green

        private final String displayName;
        private final String description;
        private final String benefits;
        private final String risks;
        private final String phases;
        private final Color color;

        MigrationStrategy(String displayName, String description, String benefits, String risks, String phases,
                Color color) {
            this.displayName = displayName;
            this.description = description;
            this.benefits = benefits;
            this.risks = risks;
            this.phases = phases;
            this.color = color;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }

        public String getBenefits() {
            return benefits;
        }

        public String getRisks() {
            return risks;
        }

        public String getPhases() {
            return phases;
        }

        public Color getColor() {
            return color;
        }
    }

    public MigrationStrategyComponent(Project project) {
        this.project = project;
        this.panel = new JBPanel<>(new BorderLayout());
        initializeComponent();
    }

    private void initializeComponent() {
        // Title
        JLabel titleLabel = new JLabel("Select Migration Strategy");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel titlePanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT));
        titlePanel.add(titleLabel);

        // Strategy cards panel
        JPanel cardsPanel = new JPanel(new GridLayout(2, 3, 10, 10));
        cardsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        for (MigrationStrategy strategy : MigrationStrategy.values()) {
            JPanel card = createStrategyCard(strategy);
            cardsPanel.add(card);
        }

        // Info panel for selected strategy
        JPanel infoPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        infoPanel.setPreferredSize(new Dimension(0, 220));

        // Benefits section
        JPanel benefitsPanel = new JPanel(new BorderLayout());
        benefitsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        JLabel benefitsTitle = new JLabel("✓ Benefits");
        benefitsTitle.setFont(benefitsTitle.getFont().deriveFont(Font.BOLD, 13f));
        this.benefitsText = new JTextArea("Click on a strategy card to see details here.");
        benefitsText.setEditable(false);
        benefitsText.setWrapStyleWord(true);
        benefitsText.setLineWrap(true);
        benefitsText.setFont(benefitsText.getFont().deriveFont(Font.PLAIN, 11f));
        benefitsPanel.add(benefitsTitle, BorderLayout.NORTH);
        benefitsPanel.add(benefitsText, BorderLayout.CENTER);

        // Risks section
        JPanel risksPanel = new JPanel(new BorderLayout());
        risksPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        JLabel risksTitle = new JLabel("⚠ Risks");
        risksTitle.setFont(risksTitle.getFont().deriveFont(Font.BOLD, 12f));
        this.risksText = new JTextArea("");
        risksText.setEditable(false);
        risksText.setWrapStyleWord(true);
        risksText.setLineWrap(true);
        risksText.setFont(risksText.getFont().deriveFont(Font.PLAIN, 11f));
        risksPanel.add(risksTitle, BorderLayout.NORTH);
        risksPanel.add(risksText, BorderLayout.CENTER);

        // Phases section
        JPanel phasesPanel = new JPanel(new BorderLayout());
        phasesPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        JLabel phasesTitle = new JLabel("⚑ Migration Phases");
        phasesTitle.setFont(phasesTitle.getFont().deriveFont(Font.BOLD, 12f));
        this.phasesText = new JTextArea("");
        phasesText.setEditable(false);
        phasesText.setWrapStyleWord(true);
        phasesText.setLineWrap(true);
        phasesText.setFont(phasesText.getFont().deriveFont(Font.PLAIN, 11f));
        phasesPanel.add(phasesTitle, BorderLayout.NORTH);
        phasesPanel.add(phasesText, BorderLayout.CENTER);

        // Add sections to info panel (33% each)
        infoPanel.add(benefitsPanel);
        infoPanel.add(risksPanel);
        infoPanel.add(phasesPanel);

        // Add components
        panel.add(titlePanel, BorderLayout.NORTH);
        panel.add(cardsPanel, BorderLayout.CENTER);
        panel.add(infoPanel, BorderLayout.SOUTH);
    }

    private JPanel createStrategyCard(MigrationStrategy strategy) {
        JPanel card = new JBPanel<>(new BorderLayout(5, 5));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        card.setPreferredSize(new Dimension(150, 120));

        // Header with color indicator
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        headerPanel.setOpaque(false);

        JPanel colorIndicator = new JPanel();
        colorIndicator.setBackground(strategy.getColor());
        colorIndicator.setPreferredSize(new Dimension(12, 12));
        colorIndicator.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        headerPanel.add(colorIndicator);

        JLabel nameLabel = new JLabel(strategy.getDisplayName());
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 14f));
        headerPanel.add(nameLabel);

        // Description
        JTextArea descArea = new JTextArea(strategy.getDescription());
        descArea.setEditable(false);
        descArea.setOpaque(false);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setFont(descArea.getFont().deriveFont(Font.PLAIN, 12f));

        // Make the card clickable
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectStrategy(strategy);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(strategy.getColor(), 2),
                        BorderFactory.createEmptyBorder(9, 9, 9, 9)));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (selectedStrategy != strategy) {
                    card.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(Color.GRAY, 1),
                            BorderFactory.createEmptyBorder(10, 10, 10, 10)));
                }
            }
        });

        card.add(headerPanel, BorderLayout.NORTH);
        card.add(descArea, BorderLayout.CENTER);

        return card;
    }

    public void setSelectedStrategy(MigrationStrategy strategy) {
        selectStrategy(strategy);
    }

    private void selectStrategy(MigrationStrategy strategy) {
        this.selectedStrategy = strategy;

        // Notify listeners
        for (MigrationStrategyListener listener : listeners) {
            listener.onStrategySelected(strategy);
        }

        // Update visual state of all cards
        updateCardSelection();

        // Update info panel
        updateInfoPanel(strategy);
    }

    private void updateCardSelection() {
        JPanel cardsPanel = (JPanel) ((JPanel) panel.getComponent(1));
        for (Component comp : cardsPanel.getComponents()) {
            if (comp instanceof JPanel) {
                JPanel card = (JPanel) comp;
                if (selectedStrategy != null) {
                    // Find the strategy for this card
                    MigrationStrategy cardStrategy = findStrategyForCard(card);
                    if (cardStrategy == selectedStrategy) {
                        card.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(cardStrategy.getColor(), 3),
                                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
                        card.setBackground(new Color(245, 245, 245));
                    } else {
                        card.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
                        card.setBackground(null);
                    }
                }
            }
        }
    }

    private MigrationStrategy findStrategyForCard(JPanel card) {
        // Find the strategy by checking the color indicator inside the header
        Component headerComp = card.getComponent(0);
        if (headerComp instanceof JPanel headerPanel) {
            for (Component subComp : headerPanel.getComponents()) {
                if (subComp instanceof JPanel colorIndicator) {
                    for (MigrationStrategy strategy : MigrationStrategy.values()) {
                        if (colorIndicator.getBackground().equals(strategy.getColor())) {
                            return strategy;
                        }
                    }
                }
            }
        }
        return null;
    }

    private void updateInfoPanel(MigrationStrategy strategy) {
        benefitsText.setText(strategy.getBenefits());
        risksText.setText(strategy.getRisks());
        phasesText.setText(strategy.getPhases());
    }

    public void addMigrationStrategyListener(MigrationStrategyListener listener) {
        listeners.add(listener);
    }

    public MigrationStrategy getSelectedStrategy() {
        return selectedStrategy;
    }

    public JPanel getPanel() {
        return panel;
    }

    public JTextArea getBenefitsText() {
        return benefitsText;
    }

    public JTextArea getRisksText() {
        return risksText;
    }
}
