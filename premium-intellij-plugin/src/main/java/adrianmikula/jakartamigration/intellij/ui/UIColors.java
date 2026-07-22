package adrianmikula.jakartamigration.intellij.ui;

import com.intellij.ui.JBColor;
import javax.swing.*;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.Color;

/**
 * Centralised text and background color constants for the plugin UI.
 * Uses JBColor so values automatically adapt to light (e.g. IntelliJ Default)
 * and dark (e.g. Darcula) IDE themes.
 *
 * Semantic status colors (green/yellow/red/cyan) live in DependencyStatusColors.
 */
public final class UIColors {

    private UIColors() {
        // Utility class - prevent instantiation
    }

    /** Primary text - headings, titles, gauge labels. */
    public static final Color TEXT_PRIMARY = new JBColor(Color.BLACK, Color.WHITE);

    /** Secondary / muted text - descriptions, hints, dates, sub-labels. */
    public static final Color TEXT_SECONDARY = new JBColor(new Color(100, 100, 100), new Color(180, 180, 180));

    /** Panel / card background - used where hardcoded light-gray was used. */
    public static final Color PANEL_BACKGROUND = new JBColor(new Color(248, 249, 250), new Color(50, 51, 53));

    /** Subtle panel background - slightly lighter variant used in some cards. */
    public static final Color PANEL_BACKGROUND_ALT = new JBColor(new Color(245, 245, 245), new Color(55, 56, 58));

    /** Border / separator lines between panels. */
    public static final Color BORDER = new JBColor(new Color(200, 200, 200), new Color(80, 80, 80));

    /** Hyperlink / action link text. */
    public static final Color LINK = new JBColor(new Color(0, 100, 180), new Color(75, 160, 255));

    /** Hyperlink text on hover. */
    public static final Color LINK_HOVER = new JBColor(new Color(0, 80, 160), new Color(100, 180, 255));

    /**
     * Configures a JProgressBar so that its string-painted text is always
     * readable on both the filled and unfilled portions of the bar.
     * <p>
     * Without this, IntelliJ's Darcula theme renders the text in a dark
     * colour that is nearly invisible on the dark-grey unfilled area when
     * the progress percentage is low.
     *
     * @param bar the progress bar to configure
     */
    public static void configureProgressBarText(JProgressBar bar) {
        bar.setUI(new BasicProgressBarUI() {
            @Override
            protected Color getSelectionForeground() {
                return Color.WHITE;
            }

            @Override
            protected Color getSelectionBackground() {
                return Color.WHITE;
            }
        });
    }
}
