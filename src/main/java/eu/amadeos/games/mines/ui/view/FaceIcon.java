package eu.amadeos.games.mines.ui.view;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;

/** The face on the new-game button, drawn rather than taken from an emoji font the system may not have. */
public final class FaceIcon implements Icon {
    static final Color YELLOW = new Color(0xFF, 0xE0, 0x00);

    private final int size;
    private Face face = Face.SMILE;

    public FaceIcon(int size) {
        this.size = size;
    }

    public Face face() {
        return face;
    }

    public void setFace(Face face) {
        this.face = face;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.translate(x, y);
        float s = size;
        float line = Math.max(1f, s / 16f);
        g2.setStroke(new BasicStroke(line, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Ellipse2D head = new Ellipse2D.Float(line / 2, line / 2, s - line, s - line);
        g2.setColor(YELLOW);
        g2.fill(head);
        g2.setColor(Color.BLACK);
        g2.draw(head);

        float eyeY = s * 0.38f;
        float leftEye = s * 0.35f;
        float rightEye = s * 0.65f;
        float eye = s * 0.11f;
        switch (face) {
            case LOST -> {
                for (float ex : new float[]{leftEye, rightEye}) {
                    g2.draw(new Line2D.Float(ex - eye * 0.7f, eyeY - eye * 0.7f, ex + eye * 0.7f, eyeY + eye * 0.7f));
                    g2.draw(new Line2D.Float(ex - eye * 0.7f, eyeY + eye * 0.7f, ex + eye * 0.7f, eyeY - eye * 0.7f));
                }
            }
            case WON -> {
                // Sunglasses
                g2.fill(new RoundRectangle2D.Float(leftEye - eye * 1.4f, eyeY - eye, eye * 2.6f, eye * 1.9f, eye, eye));
                g2.fill(new RoundRectangle2D.Float(rightEye - eye * 1.2f, eyeY - eye, eye * 2.6f, eye * 1.9f, eye, eye));
                g2.draw(new Line2D.Float(s * 0.12f, eyeY - eye * 0.6f, s * 0.88f, eyeY - eye * 0.6f));
            }
            default -> {
                g2.fill(new Ellipse2D.Float(leftEye - eye / 2, eyeY - eye / 2, eye, eye * 1.2f));
                g2.fill(new Ellipse2D.Float(rightEye - eye / 2, eyeY - eye / 2, eye, eye * 1.2f));
            }
        }

        switch (face) {
            case SURPRISED -> g2.draw(new Ellipse2D.Float(s * 0.42f, s * 0.58f, s * 0.16f, s * 0.18f));
            case LOST -> g2.draw(new Arc2D.Float(s * 0.30f, s * 0.62f, s * 0.40f, s * 0.30f, 20, 140, Arc2D.OPEN));
            default -> g2.draw(new Arc2D.Float(s * 0.27f, s * 0.38f, s * 0.46f, s * 0.36f, 200, 140, Arc2D.OPEN));
        }
        g2.dispose();
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }
}
