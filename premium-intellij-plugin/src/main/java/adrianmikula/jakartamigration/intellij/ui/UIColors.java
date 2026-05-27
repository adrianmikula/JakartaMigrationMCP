package adrianmikula.jakartamigration.intellij.ui;

import com.intellij.ui.JBColor;

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
}
