package adrianmikula.jakartamigration.intellij.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.BasicStroke;

/**
 * ScoreGauge - Abstract base class for speedometer-style gauge components.
 * Provides common rendering logic for displaying scores from 0-100.
 *
 * Subclasses define:
 * - Color scheme (getArcColorForRange)
 * - Score interpretation (getScoreLabel, getScoreCategoryColor)
 */
public abstract class ScoreGauge extends JPanel {
    protected static final int DEFAULT_GAUGE_SIZE = 150;
    protected static final int GAUGE_THICKNESS_RATIO = 20; // Ratio relative to gauge size
    protected static final int NEEDLE_LENGTH_RATIO = 60; // Ratio relative to gauge size
    protected static final int NEEDLE_WIDTH = 3;
    protected static final int MIN_GAUGE_SIZE = 80; // Minimum size to maintain readability

    protected int score = 0;
    protected String title;

    public ScoreGauge(String title) {
        this.title = title;
        setPreferredSize(new Dimension(DEFAULT_GAUGE_SIZE, DEFAULT_GAUGE_SIZE + 30)); // Extra space for title
        setMinimumSize(new Dimension(MIN_GAUGE_SIZE, MIN_GAUGE_SIZE + 20));
        setBackground(Color.WHITE);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension parentSize = getParent() != null ? getParent().getSize() : new Dimension(DEFAULT_GAUGE_SIZE, DEFAULT_GAUGE_SIZE);
        int availableWidth = parentSize.width;
        int availableHeight = parentSize.height;
        
        // Calculate optimal size based on available space while maintaining aspect ratio
        int optimalSize = Math.min(availableWidth - 40, Math.min(availableHeight - 60, DEFAULT_GAUGE_SIZE));
        optimalSize = Math.max(optimalSize, MIN_GAUGE_SIZE);
        
        return new Dimension(optimalSize, optimalSize + 30);
    }
    
    @Override
    public Dimension getMinimumSize() {
        return new Dimension(MIN_GAUGE_SIZE, MIN_GAUGE_SIZE + 20);
    }
    
    protected int getGaugeSize() {
        return Math.max(getWidth() - 20, Math.max(getHeight() - 50, MIN_GAUGE_SIZE));
    }
    
    protected int getGaugeThickness() {
        return Math.max(getGaugeSize() * GAUGE_THICKNESS_RATIO / DEFAULT_GAUGE_SIZE, 8);
    }
    
    protected int getNeedleLength() {
        return getGaugeSize() * NEEDLE_LENGTH_RATIO / DEFAULT_GAUGE_SIZE;
    }

    public void setScore(int score) {
        if (this.score == score) {
            return;
        }
        this.score = Math.max(0, Math.min(100, score)); // Clamp between 0-100
        repaint();
    }

    public int getScore() {
        return score;
    }

    /**
     * Get the color for a specific score range arc.
     * @param rangeIndex 0-3 representing the four score ranges (0-25, 26-50, 51-75, 76-100)
     * @return Color for that arc segment
     */
    protected abstract Color getArcColorForRange(int rangeIndex);

    /**
     * Get the label to display below the score (e.g., "Low", "High", "Excellent")
     * @return Category label, or null for no label
     */
    protected abstract String getScoreLabel();

    /**
     * Get the color for the score text.
     * @return Color for the score number
     */
    protected abstract Color getScoreColor();

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        // Enable antialiasing for smooth rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Center point of the gauge
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2 - 15; // Adjust for title space
        
        // Get dynamic gauge dimensions
        int gaugeSize = getGaugeSize();
        int gaugeThickness = getGaugeThickness();

        // Draw the colored arc segments
        drawGaugeArcs(g2d, centerX, centerY);

        // Draw the needle
        drawNeedle(g2d, centerX, centerY);

        // Draw the center dot
        drawCenterDot(g2d, centerX, centerY);

        // Draw the title
        drawTitle(g2d, centerX, getHeight() - 10);

        // Draw the score and label
        drawScoreAndLabel(g2d, centerX, centerY);

        g2d.dispose();
    }

    private void drawGaugeArcs(Graphics2D g2d, int centerX, int centerY) {
        // Get dynamic dimensions
        int gaugeSize = getGaugeSize();
        int gaugeThickness = getGaugeThickness();
        
        // Draw four arc segments (0-25, 26-50, 51-75, 76-100)
        // Each segment is 45 degrees
        int[] startAngles = {135, 90, 45, 0};
        int arcExtent = 45;

        for (int i = 0; i < 4; i++) {
            g2d.setColor(getArcColorForRange(i));
            g2d.fill(new Arc2D.Double(
                centerX - gaugeSize/2, centerY - gaugeSize/2,
                gaugeSize, gaugeSize,
                startAngles[i], arcExtent, Arc2D.PIE
            ));
        }

        // Draw the inner circle to create the gauge effect
        g2d.setColor(getBackground());
        g2d.fill(new Ellipse2D.Double(
            centerX - (gaugeSize/2 - gaugeThickness),
            centerY - (gaugeSize/2 - gaugeThickness),
            gaugeSize - 2*gaugeThickness,
            gaugeSize - 2*gaugeThickness
        ));

        // Draw tick marks
        drawTickMarks(g2d, centerX, centerY);
    }

    private void drawTickMarks(Graphics2D g2d, int centerX, int centerY) {
        g2d.setColor(Color.DARK_GRAY);
        g2d.setStroke(new BasicStroke(1));
        
        // Get dynamic dimensions
        int gaugeSize = getGaugeSize();
        int gaugeThickness = getGaugeThickness();

        // Draw tick marks for 0, 25, 50, 75, 100 at the specified angles
        int[] angles = {180, 225, 270, 315, 360};

        for (int angle : angles) {
            double radians = Math.toRadians(angle);
            int innerRadius = gaugeSize/2 - gaugeThickness - 5;
            int outerRadius = gaugeSize/2 - 5;

            int x1 = centerX + (int) (innerRadius * Math.cos(radians));
            int y1 = centerY + (int) (innerRadius * Math.sin(radians));
            int x2 = centerX + (int) (outerRadius * Math.cos(radians));
            int y2 = centerY + (int) (outerRadius * Math.sin(radians));

            g2d.drawLine(x1, y1, x2, y2);
        }
    }

    /**
     * Calculate needle angle based on score.
     * Score 0: 180°, Score 25: 225°, Score 50: 270°, Score 75: 315°, Score 100: 360°
     */
    protected int calculateNeedleAngle(int score) {
        double angle = 180 + (score * 1.8); // 180 + (score * 180/100)
        return (int) angle;
    }

    private void drawNeedle(Graphics2D g2d, int centerX, int centerY) {
        double angle = calculateNeedleAngle(score);
        double radians = Math.toRadians(angle);
        
        // Get dynamic needle length
        int needleLength = getNeedleLength();

        // Calculate needle endpoint
        int needleX = centerX + (int) (needleLength * Math.cos(radians));
        int needleY = centerY + (int) (needleLength * Math.sin(radians));

        // Draw needle shadow
        g2d.setColor(new Color(0, 0, 0, 50));
        int shadowOffset = 2;
        g2d.setStroke(new BasicStroke(NEEDLE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawLine(centerX + shadowOffset, centerY + shadowOffset,
                    needleX + shadowOffset, needleY + shadowOffset);

        // Draw main needle
        g2d.setColor(Color.RED);
        g2d.setStroke(new BasicStroke(NEEDLE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawLine(centerX, centerY, needleX, needleY);
    }

    private void drawCenterDot(Graphics2D g2d, int centerX, int centerY) {
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillOval(centerX - 5, centerY - 5, 10, 10);
    }

    private void drawTitle(Graphics2D g2d, int centerX, int y) {
        g2d.setColor(Color.BLACK);
        int gaugeSize = getGaugeSize();
        int titleFontSize = Math.max(10, gaugeSize / 12); // Scale font size with gauge
        Font titleFont = new Font("Arial", Font.BOLD, titleFontSize);
        g2d.setFont(titleFont);
        FontMetrics fm = g2d.getFontMetrics();
        int titleWidth = fm.stringWidth(title);
        g2d.drawString(title, centerX - titleWidth/2, y);
    }

    private void drawScoreAndLabel(Graphics2D g2d, int centerX, int centerY) {
        int gaugeSize = getGaugeSize();
        
        // Draw score with dynamic font size
        g2d.setColor(getScoreColor());
        int scoreFontSize = Math.max(12, gaugeSize / 8); // Scale font size with gauge
        Font scoreFont = new Font("Arial", Font.BOLD, scoreFontSize);
        g2d.setFont(scoreFont);
        FontMetrics fm = g2d.getFontMetrics();
        String scoreText = String.valueOf(score);
        int scoreWidth = fm.stringWidth(scoreText);
        g2d.drawString(scoreText, centerX - scoreWidth/2, centerY + 25);

        // Draw label if available with dynamic font size
        String label = getScoreLabel();
        if (label != null && !label.isEmpty()) {
            g2d.setColor(Color.DARK_GRAY);
            int labelFontSize = Math.max(8, gaugeSize / 15); // Scale font size with gauge
            Font categoryFont = new Font("Arial", Font.PLAIN, labelFontSize);
            g2d.setFont(categoryFont);
            fm = g2d.getFontMetrics();
            int labelWidth = fm.stringWidth(label);
            g2d.drawString(label, centerX - labelWidth/2, centerY + 40);
        }
    }
}
