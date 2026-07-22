package adrianmikula.jakartamigration.intellij.ui;

import com.intellij.ui.JBColor;
import javax.swing.*;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

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

    /** Scan progress bar unfilled track background. */
    public static final Color PROGRESS_BAR_BACKGROUND = new JBColor(new Color(210, 210, 210), new Color(60, 63, 65));

    /**
     * Configures a JProgressBar so that its string-painted text is always
     * readable, regardless of the progress percentage or IDE theme.
     * <p>
     * Uses a dark track color and white text. The filled portion relies on
     * the component's foreground colour (IntelliJ LAF default by design).
     * Rounded corners are restored via custom determinate painting.
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
                return PROGRESS_BAR_BACKGROUND;
            }

            @Override
            protected void paintDeterminate(Graphics g, JComponent c) {
                JProgressBar progressBar = (JProgressBar) c;
                Insets b = progressBar.getInsets();
                int barRectHeight = progressBar.getHeight() - b.top - b.bottom;
                int barRectWidth = progressBar.getWidth() - b.left - b.right;

                Rectangle boxRect = new Rectangle();
                boxRect.x = b.left;
                boxRect.y = b.top;
                boxRect.width = barRectWidth;
                boxRect.height = barRectHeight;

                int amountFull = getBoxLength(barRectWidth, barRectHeight);
                if (progressBar.getOrientation() != JProgressBar.HORIZONTAL) {
                    amountFull = barRectHeight;
                }

                boxRect.width = amountFull;

                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(getSelectionBackground());
                g2.fill(new RoundRectangle2D.Double(boxRect.x, boxRect.y, barRectWidth, barRectHeight, barRectHeight, barRectHeight));

                if (amountFull > 0) {
                    g2.setColor(progressBar.getForeground());
                    g2.fill(new RoundRectangle2D.Double(boxRect.x, boxRect.y, amountFull, barRectHeight, barRectHeight, barRectHeight));
                }

                g2.dispose();

                if (progressBar.isStringPainted()) {
                    String progressText = progressBar.getString();
                    if (progressText != null && !progressText.isEmpty()) {
                        g.setColor(getSelectionForeground());
                        FontMetrics fm = g.getFontMetrics();
                        int textWidth = fm.stringWidth(progressText);
                        int textX = boxRect.x + (barRectWidth - textWidth) / 2;
                        int textY = boxRect.y + (barRectHeight + fm.getAscent()) / 2 - 2;
                        g.drawString(progressText, textX, textY);
                    }
                }
            }
        });
    }
}
