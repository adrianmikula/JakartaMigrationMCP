package adrianmikula.jakartamigration.intellij.ui.components;

import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.util.ArrayList;
import java.util.List;

/**
 * A compact colour-coded pie chart with a legend.
 * Intended for summary dashboards where a cleaner visual overview is needed.
 */
public class PieChartPanel extends JBPanel<PieChartPanel> {

    private final JBLabel titleLabel;
    private final PieCanvas canvas;
    private final JBPanel<?> legendPanel;
    private List<Slice> slices = new ArrayList<>();

    public PieChartPanel(String title) {
        setLayout(new BorderLayout(8, 8));
        setOpaque(false);
        setPreferredSize(new Dimension(360, 240));
        setMinimumSize(new Dimension(260, 180));

        titleLabel = new JBLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 12f));
        add(titleLabel, BorderLayout.NORTH);

        canvas = new PieCanvas();
        canvas.setPreferredSize(new Dimension(120, 120));
        canvas.setMinimumSize(new Dimension(80, 80));
        add(canvas, BorderLayout.CENTER);

        legendPanel = new JBPanel<>();
        legendPanel.setLayout(new BoxLayout(legendPanel, BoxLayout.Y_AXIS));
        legendPanel.setOpaque(false);
        legendPanel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 4));
        legendPanel.setPreferredSize(new Dimension(150, 220));
        legendPanel.setMinimumSize(new Dimension(120, 120));
        add(legendPanel, BorderLayout.EAST);
    }

    /**
     * One segment of the pie.
     */
    public static class Slice {
        public final String label;
        public final int value;
        public final Color color;

        public Slice(String label, int value, Color color) {
            this.label = label;
            this.value = value;
            this.color = color;
        }
    }

    public void setSlices(List<Slice> slices) {
        this.slices = slices != null ? new ArrayList<>(slices) : new ArrayList<>();
        buildLegend();
        canvas.setSlices(this.slices);
    }

    private void buildLegend() {
        legendPanel.removeAll();

        int total = slices.stream().mapToInt(s -> s.value).sum();
        if (total <= 0) {
            JBLabel noneLabel = new JBLabel("No data");
            noneLabel.setFont(noneLabel.getFont().deriveFont(Font.ITALIC, 11f));
            noneLabel.setForeground(Color.GRAY);
            legendPanel.add(noneLabel);
            legendPanel.revalidate();
            legendPanel.repaint();
            return;
        }

        for (Slice slice : slices) {
            if (slice.value <= 0) {
                continue;
            }
            String text = slice.label + " (" + slice.value + ")";

            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
            row.setOpaque(false);

            JPanel swatch = new JPanel();
            swatch.setPreferredSize(new Dimension(10, 10));
            swatch.setMinimumSize(new Dimension(10, 10));
            swatch.setBackground(slice.color);
            swatch.setOpaque(true);
            swatch.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));

            JBLabel valueLabel = new JBLabel("<html><div style='width:130px'>" + text + "</div></html>");
            valueLabel.setFont(valueLabel.getFont().deriveFont(Font.PLAIN, 10f));
            valueLabel.setOpaque(false);

            row.add(swatch);
            row.add(valueLabel);
            legendPanel.add(row);
        }

        legendPanel.revalidate();
        legendPanel.repaint();
    }

    private static final class PieCanvas extends JPanel {
        private List<Slice> slices = new ArrayList<>();
        private int total = 0;

        PieCanvas() {
            setOpaque(false);
        }

        void setSlices(List<Slice> slices) {
            this.slices = slices != null ? new ArrayList<>(slices) : new ArrayList<>();
            this.total = this.slices.stream().mapToInt(s -> s.value).sum();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int diameter = Math.min(width, height) - 20;
            if (diameter <= 0) {
                g2.dispose();
                return;
            }

            int x = (width - diameter) / 2;
            int y = (height - diameter) / 2;

            if (total <= 0) {
                g2.setColor(Color.LIGHT_GRAY);
                g2.fillOval(x, y, diameter, diameter);
                g2.setColor(Color.DARK_GRAY);
                g2.drawString("No data", x + 10, y + diameter / 2);
                g2.dispose();
                return;
            }

            int sliceCount = slices.size();
            double[] extents = new double[sliceCount];
            double sum = 0.0;
            int lastNonZero = -1;
            for (int i = 0; i < sliceCount; i++) {
                Slice slice = slices.get(i);
                if (slice.value > 0) {
                    double extent = (slice.value * 360.0) / total;
                    extents[i] = extent;
                    sum += extent;
                    lastNonZero = i;
                }
            }

            if (lastNonZero >= 0 && Math.abs(360.0 - sum) > 0.01) {
                extents[lastNonZero] += (360.0 - sum);
            }

            double current = -90.0;
            for (int i = 0; i < sliceCount; i++) {
                Slice slice = slices.get(i);
                if (slice.value <= 0) {
                    continue;
                }
                g2.setColor(slice.color);
                g2.fill(new Arc2D.Double(x, y, diameter, diameter, current, extents[i], Arc2D.PIE));
                current += extents[i];
            }

            g2.setColor(Color.WHITE);
            Stroke oldStroke = g2.getStroke();
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(x, y, diameter, diameter);
            g2.setStroke(oldStroke);
            g2.dispose();
        }
    }
}
