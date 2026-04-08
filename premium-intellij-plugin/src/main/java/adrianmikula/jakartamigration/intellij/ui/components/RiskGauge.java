package adrianmikula.jakartamigration.intellij.ui.components;

import adrianmikula.jakartamigration.risk.RiskScoringService;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.BasicStroke;

/**
 * RiskGauge - A speedometer-style gauge component for displaying risk scores.
 * 
 * CORRECT ANGLE CONFIGURATION:
 * ==========================
 * 
 * Needle Angles (Score to Angle Mapping):
 * - Score 0: Angle 180° (left, green zone)
 * - Score 25: Angle 225° (bottom-left, green-yellow boundary)
 * - Score 50: Angle 270° (bottom, yellow-orange boundary)
 * - Score 75: Angle 315° (bottom-right, orange-red boundary)
 * - Score 100: Angle 360° (right, red zone)
 * 
 * Dial Arc Colors (Angle Ranges):
 * - Green: 135° to 90° (upper-left to top, low risk)
 * - Yellow: 90° to 45° (top to upper-right, medium-low risk)
 * - Orange: 45° to 0° (upper-right to right, medium-high risk)
 * - Red: 0° to -45° (right to lower-right, high risk)
 * 
 * This creates a semi-circular gauge with needle moving clockwise
 * from left (180°) through bottom (270°) to right (360°) as risk score increases.
 */
public class RiskGauge extends JPanel {
    private static final int GAUGE_SIZE = 150;
    private static final int GAUGE_THICKNESS = 20;
    private static final int NEEDLE_LENGTH = 60;
    private static final int NEEDLE_WIDTH = 3;
    
    private int score = 0;
    private String title;
    private RiskScoringService.CategoryConfig category;
    private RiskScoringService riskScoringService;

    public RiskGauge(String title) {
        this.title = title;
        this.riskScoringService = RiskScoringService.getInstance();
        setPreferredSize(new Dimension(GAUGE_SIZE, GAUGE_SIZE + 30)); // Extra space for title
        setBackground(Color.WHITE);
    }

    public void setScore(int score) {
        // Prevent infinite recursion - only update if score actually changed
        if (this.score == score) {
            return;
        }
        
        this.score = Math.max(0, Math.min(100, score)); // Clamp between 0-100
        this.category = riskScoringService.getCategoryConfigForScore(this.score);
        repaint();
    }
    
    /**
     * Gets the current score
     */
    public int getScore() {
        return score;
    }

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
        
        // Draw the colored arc segments
        drawGaugeArcs(g2d, centerX, centerY);
        
        // Draw the needle
        drawNeedle(g2d, centerX, centerY);
        
        // Draw the center dot
        drawCenterDot(g2d, centerX, centerY);
        
        // Draw the title
        drawTitle(g2d, centerX, getHeight() - 10);
        
        // Draw the score and category
        drawScoreAndCategory(g2d, centerX, centerY);
        
        g2d.dispose();
    }

    private void drawGaugeArcs(Graphics2D g2d, int centerX, int centerY) {
        // Draw green arc (135-90 degrees) - upper-left to top for low scores (0-25)
        g2d.setColor(new Color(40, 167, 69)); // Green
        g2d.fill(new Arc2D.Double(
            centerX - GAUGE_SIZE/2, centerY - GAUGE_SIZE/2, 
            GAUGE_SIZE, GAUGE_SIZE, 
            135, 45, Arc2D.PIE
        ));
        
        // Draw yellow arc (90-45 degrees) - top to upper-right for medium-low scores (26-50)
        g2d.setColor(new Color(255, 193, 7)); // Yellow
        g2d.fill(new Arc2D.Double(
            centerX - GAUGE_SIZE/2, centerY - GAUGE_SIZE/2, 
            GAUGE_SIZE, GAUGE_SIZE, 
            90, 45, Arc2D.PIE
        ));
        
        // Draw orange arc (45-0 degrees) - upper-right to right for medium-high scores (51-75)
        g2d.setColor(new Color(255, 165, 0)); // Orange
        g2d.fill(new Arc2D.Double(
            centerX - GAUGE_SIZE/2, centerY - GAUGE_SIZE/2, 
            GAUGE_SIZE, GAUGE_SIZE, 
            45, 45, Arc2D.PIE
        ));
        
        // Draw red arc (0 to -45 degrees) - right to lower-right for high scores (76-100)
        g2d.setColor(new Color(220, 53, 69)); // Red
        g2d.fill(new Arc2D.Double(
            centerX - GAUGE_SIZE/2, centerY - GAUGE_SIZE/2, 
            GAUGE_SIZE, GAUGE_SIZE, 
            0, 45, Arc2D.PIE
        ));
        
        // Draw the inner circle to create the gauge effect
        g2d.setColor(getBackground());
        g2d.fill(new Ellipse2D.Double(
            centerX - (GAUGE_SIZE/2 - GAUGE_THICKNESS), 
            centerY - (GAUGE_SIZE/2 - GAUGE_THICKNESS), 
            GAUGE_SIZE - 2*GAUGE_THICKNESS, 
            GAUGE_SIZE - 2*GAUGE_THICKNESS
        ));
        
        // Draw tick marks
        drawTickMarks(g2d, centerX, centerY);
    }

    private void drawTickMarks(Graphics2D g2d, int centerX, int centerY) {
        g2d.setColor(Color.DARK_GRAY);
        g2d.setStroke(new BasicStroke(1));
        
        // Draw tick marks for 0, 25, 50, 75, 100 at the specified angles
        int[] angles = {180, 225, 270, 315, 360}; // Exact angles for each score
        
        for (int angle : angles) {
            double radians = Math.toRadians(angle);
            int innerRadius = GAUGE_SIZE/2 - GAUGE_THICKNESS - 5;
            int outerRadius = GAUGE_SIZE/2 - 5;
            
            int x1 = centerX + (int) (innerRadius * Math.cos(radians));
            int y1 = centerY + (int) (innerRadius * Math.sin(radians));
            int x2 = centerX + (int) (outerRadius * Math.cos(radians));
            int y2 = centerY + (int) (outerRadius * Math.sin(radians));
            
            g2d.drawLine(x1, y1, x2, y2);
        }
    }

    /**
     * Calculate needle angle based on score using correct angle mapping
     * Score 0: 180°, Score 25: 225°, Score 50: 270°, Score 75: 315°, Score 100: 360°
     * Package-private for testing.
     */
    int calculateNeedleAngle(int score) {
        // Map score 0-100 to angle 180-360 (clockwise movement)
        // Each 25 points of score = 45 degrees of needle movement
        double angle = 180 + (score * 1.8); // 180 + (score * 180/100)
        return (int) angle;
    }

    private void drawNeedle(Graphics2D g2d, int centerX, int centerY) {
        // Calculate needle angle based on score using correct angle mapping
        // Score 0: 180°, Score 25: 225°, Score 50: 270°, Score 75: 315°, Score 100: 360°
        double angle = calculateNeedleAngle(score);
        
        double radians = Math.toRadians(angle);
        
        // Calculate needle endpoint
        int needleX = centerX + (int) (NEEDLE_LENGTH * Math.cos(radians));
        int needleY = centerY + (int) (NEEDLE_LENGTH * Math.sin(radians));
        
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
        Font titleFont = new Font("Arial", Font.BOLD, 12);
        g2d.setFont(titleFont);
        FontMetrics fm = g2d.getFontMetrics();
        int titleWidth = fm.stringWidth(title);
        g2d.drawString(title, centerX - titleWidth/2, y);
    }

    private void drawScoreAndCategory(Graphics2D g2d, int centerX, int centerY) {
        // Draw score
        g2d.setColor(Color.BLACK);
        Font scoreFont = new Font("Arial", Font.BOLD, 16);
        g2d.setFont(scoreFont);
        FontMetrics fm = g2d.getFontMetrics();
        String scoreText = String.valueOf(score);
        int scoreWidth = fm.stringWidth(scoreText);
        g2d.drawString(scoreText, centerX - scoreWidth/2, centerY + 25);
        
        // Draw category label if available
        if (category != null) {
            g2d.setColor(Color.DARK_GRAY);
            Font categoryFont = new Font("Arial", Font.PLAIN, 10);
            g2d.setFont(categoryFont);
            fm = g2d.getFontMetrics();
            int categoryWidth = fm.stringWidth(category.label);
            g2d.drawString(category.label, centerX - categoryWidth/2, centerY + 40);
        }
    }
}
