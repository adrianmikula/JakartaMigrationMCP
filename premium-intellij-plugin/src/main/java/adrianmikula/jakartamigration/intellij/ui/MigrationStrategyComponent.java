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
 * - Transform (build + runtime transformation)
 * - Microservices (migrate each service independently)
 * - Adapter Pattern (use adapter classes for javax/jakarta compatibility)
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
                UiTextLoader.getWithNewlines("strategy.big_bang.benefits",
                    "• Migrate all javax dependencies to Jakarta EE at once\n• Single comprehensive change\n• Best for small, self-contained projects"),
                UiTextLoader.getWithNewlines("strategy.big_bang.risks",
                    "• Higher risk - issues affect entire codebase\n• Longer rollback time if problems occur\n• Requires comprehensive test suite"),
                "Phase 1: Migrate dependencies\nPhase 2: Update code\nPhase 3: Test",
                new Color(220, 53, 69)), // Red

        INCREMENTAL("Incremental", "One dependency at a time",
                UiTextLoader.getWithNewlines("strategy.incremental.benefits",
                    "• Migrate dependencies incrementally\n• Update one dependency, test, then proceed\n• Lower risk per change\n• Best for large, complex projects"),
                UiTextLoader.getWithNewlines("strategy.incremental.risks",
                    "• Longer overall migration timeline\n• Must maintain compatibility during transition\n• May require temporary dual dependencies"),
                "Phase 1: Identify dependencies\nPhase 2: Prioritize\nPhase 3: Migrate one by one\nPhase 4: Test each",
                new Color(255, 193, 7)), // Yellow

        TRANSFORM("Transform", "Combined build and runtime transformation",
                UiTextLoader.getWithNewlines("strategy.transform.benefits",
                    "• Combine build-time and runtime transformation approaches\n• Use OpenRewrite for automated code changes\n• Deploy runtime adapters for edge cases"),
                UiTextLoader.getWithNewlines("strategy.transform.risks",
                    "• Most complex implementation\n• Requires both build and runtime configuration\n• Higher resource overhead"),
                "Phase 1: Setup transformation\nPhase 2: Run OpenRewrite\nPhase 3: Deploy adapters",
                new Color(23, 162, 184)), // Blue

        MICROSERVICES("Microservices", "Migrate each service independently",
                UiTextLoader.getWithNewlines("strategy.microservices.benefits",
                    "• Migrate microservices one at a time\n• Each service can use different strategy\n• Independent deployment and testing"),
                UiTextLoader.getWithNewlines("strategy.microservices.risks",
                    "• Requires coordination across services\n• Inter-service dependencies must be handled\n• May need service mesh updates"),
                "Phase 1: Identify services\nPhase 2: Migrate service by service\nPhase 3: Update dependencies",
                new Color(108, 117, 125)), // Gray

        ADAPTER("Adapter Pattern", "Use adapter classes for javax/jakarta compatibility",
                UiTextLoader.getWithNewlines("strategy.adapter.benefits",
                    "• Maintain backward compatibility during migration\n• Gradual replacement of javax with jakarta\n• Lower risk changes\n• Easy to rollback individual adapters"),
                UiTextLoader.getWithNewlines("strategy.adapter.risks",
                    "• Additional code maintenance\n• Runtime overhead for adapter layer\n• More complex classpath management"),
                "Phase 1: Create adapters\nPhase 2: Add adapter dependencies\nPhase 3: Replace javax with adapters",
                new Color(111, 66, 193)); // Purple

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
        card.setPreferredSize(new Dimension(150, 90));

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
