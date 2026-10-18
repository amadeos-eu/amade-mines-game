package eu.amadeos.games.mines.ui.view;

import eu.amadeos.commons.swing.AmadeUI;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;

/**
 * Three red seven-segment digits on black, as on the original's mine counter and clock. The segments are
 * drawn, not taken from a font, so they look the same everywhere.
 */
public final class Counter extends JComponent {
    static final Color LIT = new Color(0xFF, 0x20, 0x10);
    static final Color UNLIT = new Color(0x34, 0x06, 0x03);

    /** Segments a to g of the digits 0 to 9, bit 0 = a (top), clockwise, bit 6 = g (middle). */
    private static final int[] DIGITS = {0x3F, 0x06, 0x5B, 0x4F, 0x66, 0x6D, 0x7D, 0x07, 0x7F, 0x6F};
    private static final int MINUS = 0x40;
    private static final int BEVEL = 2;
    /** Width to height of the whole counter, so that each digit is a little more than half as wide as high. */
    public static final float ASPECT = 1.9f;

    private int value;

    public Counter(String name) {
        setName(name);
    }

    public int value() {
        return value;
    }

    /** Shows -99 to 999; values beyond are cut to these. */
    public void setValue(int value) {
        int shown = Math.clamp(value, -99, 999);
        if (shown != this.value) {
            this.value = shown;
            repaint();
        }
    }

    /** The three digit patterns for a value, e.g. {@code -7} → minus, blank-zero, 7. */
    static int[] segments(int value) {
        int shown = Math.clamp(value, -99, 999);
        int magnitude = Math.abs(shown);
        int[] result = {DIGITS[magnitude / 100 % 10], DIGITS[magnitude / 10 % 10], DIGITS[magnitude % 10]};
        if (shown < 0) result[0] = MINUS;
        return result;
    }

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) return super.getPreferredSize();
        int height = Math.round(getFont() == null ? 26 : getFont().getSize2D() * 2.1f);
        return new Dimension(Math.round(height * ASPECT), height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, w, h);
        AmadeUI.paintBevel(g2, 0, 0, w, h, false, BEVEL);
        int innerH = h - 2 * BEVEL - 4;
        int digitW = (w - 2 * BEVEL - 4) / 3;
        int[] patterns = segments(value);
        for (int d = 0; d < 3; d++) {
            paintDigit(g2, BEVEL + 2 + d * digitW, BEVEL + 2, digitW, innerH, patterns[d]);
        }
        g2.dispose();
    }

    private static void paintDigit(Graphics2D g, int x, int y, int w, int h, int pattern) {
        float t = Math.max(2f, Math.min(h / 8f, w / 4.5f));
        float pad = t * 0.8f;
        float left = x + pad;
        float right = x + w - pad;
        float top = y + pad * 0.5f;
        float bottom = y + h - pad * 0.5f;
        float middle = (top + bottom) / 2;
        float[][] bars = {
                {left + t * 0.6f, top, right - t * 0.6f, top},          // a
                {right, top + t * 0.6f, right, middle - t * 0.4f},     // b
                {right, middle + t * 0.4f, right, bottom - t * 0.6f},  // c
                {left + t * 0.6f, bottom, right - t * 0.6f, bottom},   // d
                {left, middle + t * 0.4f, left, bottom - t * 0.6f},    // e
                {left, top + t * 0.6f, left, middle - t * 0.4f},       // f
                {left + t * 0.6f, middle, right - t * 0.6f, middle},   // g
        };
        g.setStroke(new BasicStroke(t, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        for (int s = 0; s < 7; s++) {
            g.setColor((pattern & (1 << s)) != 0 ? LIT : UNLIT);
            float[] b = bars[s];
            g.draw(new Line2D.Float(b[0], b[1], b[2], b[3]));
        }
    }
}
