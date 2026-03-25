package adrianmikula.jakartamigration.intellij.ui.components;

import adrianmikula.jakartamigration.intellij.service.RiskScoringService;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;

/**
 * A speedometer-style gauge component for displaying risk scores.
 * Shows a needle that points left/green for low risk, up/yellow for medium risk, 
 * and right/red for high risk.
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
        this.score = Math.max(0, Math.min(100, score)); // Clamp between 0-100
        this.category = riskScoringService.getCategoryForScore(this.score);
        repaint();
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
        // Draw green arc (0-60 degrees)
        g2d.setColor(new Color(40, 167, 69)); // Green
        g2d.fill(new Arc2D.Double(
            centerX - GAUGE_SIZE/2, centerY - GAUGE_SIZE/2, 
            GAUGE_SIZE, GAUGE_SIZE, 
            120, 60, Arc2D.PIE
        ));
        
        // Draw yellow arc (60-120 degrees)
        g2d.setColor(new Color(255, 193, 7)); // Yellow
        g2d.fill(new Arc2D.Double(
            centerX - GAUGE_SIZE/2, centerY - GAUGE_SIZE/2, 
            GAUGE_SIZE, GAUGE_SIZE, 
            180, 60, Arc2D.PIE
        ));
        
        // Draw red arc (120-180 degrees)
        g2d.setColor(new Color(220, 53, 69)); // Red
        g2d.fill(new Arc2D.Double(
            centerX - GAUGE_SIZE/2, centerY - GAUGE_SIZE/2, 
            GAUGE_SIZE, GAUGE_SIZE, 
            240, 60, Arc2D.PIE
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
        
        // Draw tick marks for 0, 50, 100
        int[] angles = {120, 180, 240}; // Corresponding to 0, 50, 100
        
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

    private void drawNeedle(Graphics2D g2d, int centerX, int centerY) {
        // Calculate needle angle based on score (0-100 maps to 120-240 degrees)
        double angle = 120 + (score * 1.2); // 120 degrees + (score * 1.2 degrees per point)
        double radians = Math.toRadians(angle);
        
        // Draw needle shadow
        g2d.setColor(new Color(0, 0, 0, 50));
        int shadowOffset = 2;
        int needleX = centerX + (int) (NEEDLE_LENGTH * Math.cos(radians)) + shadowOffset;
        int needleY = centerY + (int) (NEEDLE_LENGTH * Math.sin(radians)) + shadowOffset;
        g2d.fillRoundRect(centerX + shadowOffset - NEEDLE_WIDTH/2, centerY + shadowOffset - NEEDLE_WIDTH/2, 
                          needleX - centerX, needleY - centerY, NEEDLE_WIDTH, NEEDLE_WIDTH);
        
        // Draw main needle
        g2d.setColor(Color.BLACK);
        needleX = centerX + (int) (NEEDLE_LENGTH * Math.cos(radians));
        needleY = centerY + (int) (NEEDLE_LENGTH * Math.sin(radians));
        g2d.fillRoundRect(centerX - NEEDLE_WIDTH/2, centerY - NEEDLE_WIDTH/2, 
                          needleX - centerX, needleY - centerY, NEEDLE_WIDTH, NEEDLE_WIDTH);
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
