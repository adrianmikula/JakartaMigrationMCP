package adrianmikula.jakartamigration.intellij.ui;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Migration strategy selection component.
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

    public interface MigrationStrategyListener {
        void onStrategySelected(MigrationStrategy strategy);
    }

    public enum MigrationStrategy {
        BIG_BANG("Big Bang", "Migrate everything at once",
                "• Fastest migration time\n• All-or-nothing approach\n• Best for small projects\n• Higher risk",
                new Color(220, 53, 69)), // Red

        INCREMENTAL("Incremental", "One dependency at a time",
                "• Lower risk per change\n• Easy to rollback\n• Best for large projects\n• Takes longer",
                new Color(255, 193, 7)), // Yellow

        BUILD_TRANSFORMATION("Build Transformation", "Transform during build process",
                "• Automated transformation\n• Uses OpenRewrite recipes\n• CI/CD integration\n• Requires build access",
                new Color(23, 162, 184)), // Blue

        RUNTIME_TRANSFORMATION("Runtime Transformation", "Transform at runtime",
                "• No code changes needed\n• Uses adapter pattern\n• Performance overhead\n• Temporary solution",
                new Color(40, 167, 69)); // Green

        private final String displayName;
        private final String description;
        private final String benefits;
        private final Color color;

        MigrationStrategy(String displayName, String description, String benefits, Color color) {
            this.displayName = displayName;
            this.description = description;
            this.benefits = benefits;
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
        JPanel cardsPanel = new JBPanel<>(new GridLayout(1, 4, 10, 10));
        cardsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        for (MigrationStrategy strategy : MigrationStrategy.values()) {
            JPanel card = createStrategyCard(strategy);
            cardsPanel.add(card);
        }

        // Info panel for selected strategy
        JPanel infoPanel = new JBPanel<>(new BorderLayout());
        infoPanel.setBorder(BorderFactory.createTitledBorder("Strategy Details"));
        infoPanel.setPreferredSize(new Dimension(0, 120));

        JTextArea infoText = new JTextArea("Click on a strategy card to see details here.");
        infoText.setEditable(false);
        infoText.setWrapStyleWord(true);
        infoText.setLineWrap(true);
        infoText.setFont(infoText.getFont().deriveFont(Font.PLAIN, 12f));
        infoPanel.add(infoText, BorderLayout.CENTER);

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
        card.setPreferredSize(new Dimension(180, 150));

        // Header with color indicator
        JPanel headerPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 5, 0));
        headerPanel.setOpaque(false);

        JPanel colorIndicator = new JBPanel<>();
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
        JTextArea infoText = (JTextArea) infoPanel.getComponent(0);

        StringBuilder sb = new StringBuilder();
        sb.append(strategy.getDisplayName()).append("\n\n");
        sb.append(strategy.getBenefits());

        infoText.setText(sb.toString());
        infoText.setCaretPosition(0);
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
