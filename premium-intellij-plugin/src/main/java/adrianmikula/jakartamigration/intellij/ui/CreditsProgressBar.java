package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.credits.CreditType;
import adrianmikula.jakartamigration.credits.CreditsService;
import adrianmikula.jakartamigration.intellij.license.CheckLicense;
import adrianmikula.jakartamigration.analytics.service.UserIdentificationService;
import adrianmikula.jakartamigration.analytics.service.UsageService;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Credits progress bar component for the freemium model.
 * Shows remaining credits for free users at the top of the plugin UI.
 * Premium users do not see this component (unlimited credits).
 */
public class CreditsProgressBar extends JBPanel<CreditsProgressBar> {
    private static final Logger LOG = Logger.getInstance(CreditsProgressBar.class);

    // Marketplace URL for upgrades
    private static final String MARKETPLACE_URL = "https://plugins.jetbrains.com/plugin/30093-jakarta-migration";

    // Warning threshold (show orange when below this percentage)
    private static final double WARNING_THRESHOLD = 0.20;

    // Critical threshold (show red when below this percentage)
    private static final double CRITICAL_THRESHOLD = 0.10;

    private final CreditsService creditsService;
    private final Project project;

    // UI Components
    private JPanel creditsPanel;
    private JProgressBar progressBar;
    private JBLabel countLabel;
    private JBLabel statusLabel;

    // Track if user is premium
    private Boolean isPremium = null;

    public CreditsProgressBar(Project project) {
        super(new BorderLayout());
        this.project = project;
        this.creditsService = new CreditsService();

        initializeUI();
        refreshCredits();
    }

    private void initializeUI() {
        setBorder(JBUI.Borders.empty(5, 10, 5, 10));

        // Main panel with vertical layout
        creditsPanel = new JPanel();
        creditsPanel.setLayout(new BoxLayout(creditsPanel, BoxLayout.Y_AXIS));

        // Title row with upgrade link
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JBLabel titleLabel = new JBLabel("Free Credits:");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        titlePanel.add(titleLabel);

        // Upgrade button (small link-style)
        JBLabel upgradeLink = new JBLabel("<html><a href='#'>Upgrade to Premium</a></html>");
        upgradeLink.setForeground(JBColor.BLUE);
        upgradeLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        upgradeLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Track upgrade click before opening marketplace
                try {
                    UserIdentificationService userIdentificationService = new UserIdentificationService();
                    UsageService usageService = new UsageService(userIdentificationService);
                    usageService.trackUpgradeClick("credits_progress_bar", "CreditsProgressBar");
                } catch (Exception ex) {
                    // Log error but don't prevent upgrade
                    System.err.println("Failed to track upgrade click analytics: " + ex.getMessage());
                }
                openMarketplace();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                upgradeLink.setText("<html><a href='#'><b>Upgrade to Premium</b></a></html>");
            }

            @Override
            public void mouseExited(MouseEvent e) {
                upgradeLink.setText("<html><a href='#'>Upgrade to Premium</a></html>");
            }
        });
        titlePanel.add(upgradeLink);

        creditsPanel.add(titlePanel);
        creditsPanel.add(Box.createVerticalStrut(5));

        // Single progress bar for actions credit
        JPanel creditRow = createCreditRow();
        creditsPanel.add(creditRow);
        creditsPanel.add(Box.createVerticalStrut(3));

        // Status label for low credit warnings
        statusLabel = new JBLabel(" ");
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.ITALIC));
        statusLabel.setForeground(JBColor.ORANGE);
        creditsPanel.add(statusLabel);

        add(creditsPanel, BorderLayout.CENTER);

        // Add border to make it distinct
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.GRAY),
            JBUI.Borders.empty(8, 12)
        ));
    }

    private JPanel createCreditRow() {
        JPanel row = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // Progress bar
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(0, 0, 0, 5);
        int creditLimit = creditsService.getCreditLimit();
        progressBar = new JProgressBar(0, creditLimit);
        progressBar.setStringPainted(false);
        progressBar.setPreferredSize(new Dimension(150, 18));
        row.add(progressBar, gbc);

        // Count label (e.g., "5/10")
        gbc.gridx = 1;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = JBUI.insets(0);
        countLabel = new JBLabel("0/" + creditLimit);
        countLabel.setPreferredSize(new Dimension(50, 20));
        row.add(countLabel, gbc);

        return row;
    }

    /**
     * Refreshes the credit display based on current values.
     * Should be called after credit usage or periodically.
     */
    public void refreshCredits() {
        // Check premium status
        Boolean currentPremiumStatus = CheckLicense.isLicensed();
        if (currentPremiumStatus != null && currentPremiumStatus) {
            // Premium user - hide this component
            setVisible(false);
            isPremium = true;
            return;
        }

        isPremium = false;
        setVisible(true);

        // Refresh credits from service
        creditsService.refreshCache();

        // Get single credit type (ACTIONS)
        CreditType type = CreditType.ACTIONS;
        int remaining = creditsService.getRemainingCredits(type);
        int limit = creditsService.getCreditLimit(type);
        int used = creditsService.getUsedCredits(type);

        // Update single progress bar
        if (progressBar != null && countLabel != null) {
            progressBar.setMaximum(limit);
            progressBar.setValue(remaining);
            countLabel.setText(String.format("%d/%d", remaining, limit));

            // Set color based on remaining percentage
            double percentage = limit > 0 ? (double) remaining / limit : 0;
            if (percentage <= CRITICAL_THRESHOLD) {
                progressBar.setForeground(JBColor.RED);
                statusLabel.setText("⚠ No credits remaining. Upgrade for unlimited actions.");
                statusLabel.setForeground(JBColor.RED);
            } else if (percentage <= WARNING_THRESHOLD) {
                progressBar.setForeground(JBColor.ORANGE);
                statusLabel.setText("⚠ Running low on credits. Consider upgrading.");
                statusLabel.setForeground(JBColor.ORANGE);
            } else {
                progressBar.setForeground(JBColor.GREEN);
                statusLabel.setText(" ");
            }
        }

        LOG.debug("Credits: {}/{} used", used, limit);

        revalidate();
        repaint();
    }

    /**
     * Uses an action credit and refreshes the display.
     *
     * @return true if credit was successfully used
     */
    public boolean useCredit() {
        boolean success = creditsService.useCredit(CreditType.ACTIONS, "CreditsProgressBar", "credit_consumption");
        if (success) {
            refreshCredits();
        }
        return success;
    }

    /**
     * Uses a credit of the specified type and refreshes the display.
     * Deprecated: Use useCredit() instead for the single ACTIONS credit type.
     *
     * @param type the credit type to use
     * @return true if credit was successfully used
     */
    @Deprecated
    public boolean useCredit(CreditType type) {
        boolean success = creditsService.useCredit(type, "CreditsProgressBar", "deprecated_credit_consumption");
        if (success) {
            refreshCredits();
        }
        return success;
    }

    /**
     * Checks if there are action credits remaining.
     *
     * @return true if credits are available
     */
    public boolean hasCredits() {
        return creditsService.hasCredits(CreditType.ACTIONS);
    }

    /**
     * Checks if there are credits remaining for a specific type.
     * Deprecated: Use hasCredits() instead.
     *
     * @param type the credit type
     * @return true if credits are available
     */
    @Deprecated
    public boolean hasCredits(CreditType type) {
        return creditsService.hasCredits(type);
    }

    /**
     * Gets the number of remaining action credits.
     *
     * @return the number of remaining credits
     */
    public int getRemainingCredits() {
        return creditsService.getRemainingCredits(CreditType.ACTIONS);
    }

    /**
     * Gets the number of remaining credits for a specific type.
     * Deprecated: Use getRemainingCredits() instead.
     *
     * @param type the credit type
     * @return the number of remaining credits
     */
    @Deprecated
    public int getRemainingCredits(CreditType type) {
        return creditsService.getRemainingCredits(type);
    }

    /**
     * Returns true if the user is premium (no credit limits).
     *
     * @return true if premium user
     */
    public boolean isPremium() {
        if (isPremium == null) {
            isPremium = CheckLicense.isLicensed();
        }
        return isPremium != null && isPremium;
    }

    /**
     * Opens the JetBrains Marketplace to upgrade.
     */
    private void openMarketplace() {
        try {
            Desktop.getDesktop().browse(new URI(MARKETPLACE_URL));
        } catch (Exception ex) {
            LOG.warn("Failed to open marketplace URL", ex);
            com.intellij.openapi.ui.Messages.showInfoMessage(
                project,
                "Please visit: " + MARKETPLACE_URL,
                "Upgrade to Premium"
            );
        }
    }

    /**
     * Cleans up resources. Should be called when the component is no longer needed.
     */
    public void dispose() {
        creditsService.close();
    }
}
