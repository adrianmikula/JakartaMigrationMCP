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

    public interface MigrationStrategyListener {
        void onStrategySelected(MigrationStrategy strategy);
    }

    public enum MigrationStrategy {
        BIG_BANG("Big Bang", "Migrate everything at once",
                UiTextLoader.getWithNewlines("strategy.big_bang.benefits",
                    "• Migrate all javax dependencies to Jakarta EE at once\n• Single comprehensive change\n• Best for small, self-contained projects"),
                UiTextLoader.getWithNewlines("strategy.big_bang.risks",
                    "• Higher risk - issues affect entire codebase\n• Longer rollback time if problems occur\n• Requires comprehensive test suite"),
                new Color(220, 53, 69)), // Red

        INCREMENTAL("Incremental", "One dependency at a time",
                UiTextLoader.getWithNewlines("strategy.incremental.benefits",
                    "• Migrate dependencies incrementally\n• Update one dependency, test, then proceed\n• Lower risk per change\n• Best for large, complex projects"),
                UiTextLoader.getWithNewlines("strategy.incremental.risks",
                    "• Longer overall migration timeline\n• Must maintain compatibility during transition\n• May require temporary dual dependencies"),
                new Color(255, 193, 7)), // Yellow

        TRANSFORM("Transform", "Combined build and runtime transformation",
                UiTextLoader.getWithNewlines("strategy.transform.benefits",
                    "• Combine build-time and runtime transformation approaches\n• Use OpenRewrite for automated code changes\n• Deploy runtime adapters for edge cases"),
                UiTextLoader.getWithNewlines("strategy.transform.risks",
                    "• Most complex implementation\n• Requires both build and runtime configuration\n• Higher resource overhead"),
                new Color(23, 162, 184)), // Blue

        MICROSERVICES("Microservices", "Migrate each service independently",
                UiTextLoader.getWithNewlines("strategy.microservices.benefits",
                    "• Migrate microservices one at a time\n• Each service can use different strategy\n• Independent deployment and testing"),
                UiTextLoader.getWithNewlines("strategy.microservices.risks",
                    "• Requires coordination across services\n• Inter-service dependencies must be handled\n• May need service mesh updates"),
                new Color(108, 117, 125)), // Gray

        ADAPTER("Adapter Pattern", "Use adapter classes for javax/jakarta compatibility",
                UiTextLoader.getWithNewlines("strategy.adapter.benefits",
                    "• Maintain backward compatibility during migration\n• Gradual replacement of javax with jakarta\n• Lower risk changes\n• Easy to rollback individual adapters"),
                UiTextLoader.getWithNewlines("strategy.adapter.risks",
                    "• Additional code maintenance\n• Runtime overhead for adapter layer\n• More complex classpath management"),
                new Color(111, 66, 193)); // Purple

        private final String displayName;
        private final String description;
        private final String benefits;
        private final String risks;
        private final Color color;

        MigrationStrategy(String displayName, String description, String benefits, String risks, Color color) {
            this.displayName = displayName;
            this.description = description;
            this.benefits = benefits;
            this.risks = risks;
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
        JPanel cardsPanel = new JPanel(new GridLayout(1, 4, 10, 10));
        cardsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        for (MigrationStrategy strategy : MigrationStrategy.values()) {
            JPanel card = createStrategyCard(strategy);
            cardsPanel.add(card);
        }

        // Info panel for selected strategy - now with Benefits and Risks
        JPanel infoPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        infoPanel.setPreferredSize(new Dimension(0, 180));

        // Benefits section
        JPanel benefitsPanel = new JPanel(new BorderLayout());
        benefitsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        JLabel benefitsTitle = new JLabel("✓ Benefits");
        benefitsTitle.setFont(benefitsTitle.getFont().deriveFont(Font.BOLD, 12f));
        JTextArea benefitsText = new JTextArea("Click on a strategy card to see details here.");
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
        JTextArea risksText = new JTextArea("");
        risksText.setEditable(false);
        risksText.setWrapStyleWord(true);
        risksText.setLineWrap(true);
        risksText.setFont(risksText.getFont().deriveFont(Font.PLAIN, 11f));
        risksPanel.add(risksTitle, BorderLayout.NORTH);
        risksPanel.add(risksText, BorderLayout.CENTER);

        // Add benefits and risks to info panel (50% each)
        infoPanel.add(benefitsPanel, 0);
        infoPanel.add(risksPanel, 1);

        // Add components
        panel.add(titlePanel, BorderLayout.NORTH);
        panel.add(cardsPanel, BorderLayout.CENTER);
        panel.add(infoPanel, BorderLayout.SOUTH);
    }

    private JPanel createStrategyCard(MigrationStrategy strategy) {
        JPanel card = new JBPanel<>(new BorderLayout(5, 5));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        card.setPreferredSize(new Dimension(180, 160));

        // Header with color indicator
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        headerPanel.setOpaque(false);

        JPanel colorIndicator = new JPanel();
        colorIndicator.setBackground(strategy.getColor());
        colorIndicator.setPreferredSize(new Dimension(12, 12));
        colorIndicator.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        headerPanel.add(colorIndicator);

        JLabel nameLabel = new JLabel(strategy.getDisplayName());
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 13f));
        headerPanel.add(nameLabel);

        // Description
        JTextArea descArea = new JTextArea(strategy.getDescription());
        descArea.setEditable(false);
        descArea.setOpaque(false);
        descArea.setFont(descArea.getFont().deriveFont(Font.PLAIN, 11f));

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
                        BorderFactory.createEmptyBorder(9, 9, 9, 9)
                ));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (selectedStrategy != strategy) {
                    card.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(Color.GRAY, 1),
                            BorderFactory.createEmptyBorder(10, 10, 10, 10)
                    ));
                }
            }
        });

        card.add(headerPanel, BorderLayout.NORTH);
        card.add(descArea, BorderLayout.CENTER);

        return card;
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
                                BorderFactory.createEmptyBorder(8, 8, 8, 8)
                        ));
                        card.setBackground(new Color(245, 245, 245));
                    } else {
                        card.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                                BorderFactory.createEmptyBorder(10, 10, 10, 10)
                        ));
                        card.setBackground(null);
                    }
                }
            }
        }
    }

    private MigrationStrategy findStrategyForCard(JPanel card) {
        // Find the strategy by checking the border color of the inner panel
        for (Component comp : card.getComponents()) {
            if (comp instanceof JPanel) {
                JPanel innerPanel = (JPanel) comp;
                for (MigrationStrategy strategy : MigrationStrategy.values()) {
                    if (innerPanel.getBackground().equals(strategy.getColor())) {
                        return strategy;
                    }
                }
            }
        }
        return null;
    }

    private void updateInfoPanel(MigrationStrategy strategy) {
        JPanel infoPanel = (JPanel) panel.getComponent(2);

        // Benefits panel
        JPanel benefitsPanel = (JPanel) infoPanel.getComponent(0);
        JTextArea benefitsText = (JTextArea) benefitsPanel.getComponent(1);
        benefitsText.setText(strategy.getBenefits());

        // Risks panel
        JPanel risksPanel = (JPanel) infoPanel.getComponent(1);
        JTextArea risksText = (JTextArea) risksPanel.getComponent(1);
        risksText.setText(strategy.getRisks());
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
}
