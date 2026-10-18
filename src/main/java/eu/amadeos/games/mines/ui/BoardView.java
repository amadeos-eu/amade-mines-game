package eu.amadeos.games.mines.ui;

import eu.amadeos.commons.swing.AmadeUI;
import eu.amadeos.commons.theme.DesktopTheme;
import eu.amadeos.games.mines.domain.Board;
import eu.amadeos.games.mines.domain.Look;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.HashSet;
import java.util.Set;

/**
 * The mine field. Left button opens (on release, so a press can be dragged away), right button cycles the
 * marks, the middle button or both buttons together open the neighbours of a number (chord). While a button
 * is held, the cells it would open are shown pressed. Colours come from the desktop theme at paint time,
 * so the field follows the Style Manager.
 */
final class BoardView extends JComponent {
    /** What the window does with the gestures. */
    interface Listener {
        void open(int column, int row);

        void chord(int column, int row);

        void mark(int column, int row);

        /** A button went down on the field ({@code true}) or all are up again ({@code false}). */
        void pressing(boolean down);
    }

    /** Number colours of the original, for light cells. */
    private static final Color[] NUMBERS = {null, new Color(0x00, 0x00, 0xFF), new Color(0x00, 0x80, 0x00),
            new Color(0xFF, 0x00, 0x00), new Color(0x00, 0x00, 0x80), new Color(0x80, 0x00, 0x00),
            new Color(0x00, 0x80, 0x80), Color.BLACK, new Color(0x80, 0x80, 0x80)};
    /** The same numbers readable on the dark cells of dark themes. */
    private static final Color[] NUMBERS_ON_DARK = {null, new Color(0x7C, 0xA8, 0xFF), new Color(0x6C, 0xD0, 0x6C),
            new Color(0xFF, 0x70, 0x70), new Color(0xB0, 0x9C, 0xFF), new Color(0xFF, 0xA0, 0x60),
            new Color(0x60, 0xD8, 0xD8), new Color(0xF0, 0xF0, 0xF0), new Color(0xB0, 0xB0, 0xB0)};
    static final Color EXPLODED = new Color(0xFF, 0x30, 0x30);
    private static final Color FLAG = new Color(0xE0, 0x00, 0x00);

    private final int cellSize;
    private final Listener listener;
    private Board board;
    private Point pressed;
    private boolean chording;
    /** After a chord, the other button's release must not open a cell. */
    private boolean swallowRelease;

    BoardView(Board board, int cellSize, Listener listener) {
        this.board = board;
        this.cellSize = cellSize;
        this.listener = listener;
        setName("board");
        setOpaque(true);
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                pressedAt(e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (pressed != null) {
                    pressed = cellAt(e.getPoint());
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                releasedAt(e);
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    void setBoard(Board board) {
        this.board = board;
        pressed = null;
        chording = false;
        swallowRelease = false;
        revalidate();
        repaint();
    }

    int cellSize() {
        return cellSize;
    }

    /** The cell under a point; a point outside the field gives a cell outside the board. */
    Point cellAt(Point p) {
        return new Point(Math.floorDiv(p.x, cellSize), Math.floorDiv(p.y, cellSize));
    }

    private void pressedAt(MouseEvent e) {
        if (board.finished()) return;
        Point cell = cellAt(e.getPoint());
        int down = e.getModifiersEx();
        boolean left = (down & InputEvent.BUTTON1_DOWN_MASK) != 0;
        boolean right = (down & InputEvent.BUTTON3_DOWN_MASK) != 0;
        if (SwingUtilities.isMiddleMouseButton(e) || (left && right)) {
            chording = true;
            pressed = cell;
            listener.pressing(true);
        } else if (SwingUtilities.isLeftMouseButton(e)) {
            pressed = cell;
            listener.pressing(true);
        } else if (SwingUtilities.isRightMouseButton(e) && board.contains(cell.x, cell.y)) {
            listener.mark(cell.x, cell.y);
        }
        repaint();
    }

    private void releasedAt(MouseEvent e) {
        int stillDown = e.getModifiersEx() & (InputEvent.BUTTON1_DOWN_MASK | InputEvent.BUTTON2_DOWN_MASK
                | InputEvent.BUTTON3_DOWN_MASK);
        Point cell = cellAt(e.getPoint());
        if (chording) {
            chording = false;
            pressed = null;
            swallowRelease = stillDown != 0;
            if (board.contains(cell.x, cell.y)) listener.chord(cell.x, cell.y);
        } else if (swallowRelease) {
            swallowRelease = stillDown != 0;
        } else if (SwingUtilities.isLeftMouseButton(e) && pressed != null) {
            pressed = null;
            if (board.contains(cell.x, cell.y)) listener.open(cell.x, cell.y);
        }
        if (stillDown == 0) listener.pressing(false);
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(board.columns() * cellSize, board.rows() * cellSize);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        DesktopTheme theme = AmadeUI.theme();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(theme.base());
        g2.fillRect(0, 0, getWidth(), getHeight());
        Set<Integer> down = pressedCells();
        Font numberFont = getFont().deriveFont(Font.BOLD, cellSize * 0.62f);
        for (int r = 0; r < board.rows(); r++) {
            for (int c = 0; c < board.columns(); c++) {
                paintCell(g2, theme, c, r, down.contains(r * board.columns() + c), numberFont);
            }
        }
        g2.dispose();
    }

    /** The closed cells drawn pressed while a button is held. */
    private Set<Integer> pressedCells() {
        Set<Integer> cells = new HashSet<>();
        if (pressed == null || board.finished()) return cells;
        if (chording) {
            for (int i : board.chordCells(pressed.x, pressed.y)) cells.add(i);
        }
        if (board.contains(pressed.x, pressed.y)) {
            Look look = board.look(pressed.x, pressed.y);
            if (look == Look.HIDDEN || look == Look.QUESTION) cells.add(pressed.y * board.columns() + pressed.x);
        }
        return cells;
    }

    private void paintCell(Graphics2D g, DesktopTheme theme, int c, int r, boolean down, Font numberFont) {
        int x = c * cellSize;
        int y = r * cellSize;
        Look look = board.look(c, r);
        boolean flat = down || look == Look.OPEN || look == Look.MINE || look == Look.EXPLODED;
        if (flat) {
            g.setColor(look == Look.EXPLODED ? EXPLODED : theme.sunkenBg());
            g.fillRect(x, y, cellSize, cellSize);
            g.setColor(theme.shadow());
            g.drawLine(x, y, x + cellSize - 1, y);
            g.drawLine(x, y, x, y + cellSize - 1);
        } else {
            g.setColor(theme.base());
            g.fillRect(x, y, cellSize, cellSize);
            AmadeUI.paintBevel(g, x, y, cellSize, cellSize, true, cellSize >= 24 ? 3 : 2);
        }
        Graphics2D s = (Graphics2D) g.create();
        s.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        s.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        switch (look) {
            case OPEN -> {
                int n = board.count(c, r);
                if (n > 0) paintNumber(s, theme, x, y, n, numberFont);
            }
            case MINE, EXPLODED -> paintMine(s, x, y);
            case FLAG -> paintFlag(s, x, y);
            case WRONG_FLAG -> {
                paintMine(s, x, y);
                s.setColor(FLAG);
                s.setStroke(new BasicStroke(Math.max(2f, cellSize / 10f)));
                float m = cellSize * 0.2f;
                s.draw(new Line2D.Float(x + m, y + m, x + cellSize - m, y + cellSize - m));
                s.draw(new Line2D.Float(x + m, y + cellSize - m, x + cellSize - m, y + m));
            }
            case QUESTION -> paintText(s, x, y, "?", theme.textActive(), numberFont);
            case HIDDEN -> {
            }
        }
        s.dispose();
    }

    private void paintNumber(Graphics2D g, DesktopTheme theme, int x, int y, int n, Font font) {
        Color[] colors = DesktopTheme.isDark(theme.sunkenBg()) ? NUMBERS_ON_DARK : NUMBERS;
        paintText(g, x, y, Integer.toString(n), colors[n], font);
    }

    private void paintText(Graphics2D g, int x, int y, String text, Color color, Font font) {
        g.setFont(font);
        g.setColor(color);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, x + (cellSize - fm.stringWidth(text)) / 2f + 0.5f,
                y + (cellSize + fm.getAscent() - fm.getDescent()) / 2f + 0.5f);
    }

    private void paintMine(Graphics2D g, int x, int y) {
        float cx = x + cellSize / 2f + 0.5f;
        float cy = y + cellSize / 2f + 0.5f;
        float radius = cellSize * 0.24f;
        float spike = cellSize * 0.36f;
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(Math.max(1.5f, cellSize / 12f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Float(cx - spike, cy, cx + spike, cy));
        g.draw(new Line2D.Float(cx, cy - spike, cx, cy + spike));
        float d = spike * 0.72f;
        g.draw(new Line2D.Float(cx - d, cy - d, cx + d, cy + d));
        g.draw(new Line2D.Float(cx - d, cy + d, cx + d, cy - d));
        g.fill(new Ellipse2D.Float(cx - radius, cy - radius, radius * 2, radius * 2));
        g.setColor(Color.WHITE);
        float glare = radius * 0.55f;
        g.fill(new Ellipse2D.Float(cx - radius * 0.6f, cy - radius * 0.6f, glare, glare));
    }

    private void paintFlag(Graphics2D g, int x, int y) {
        float s = cellSize;
        float pole = x + s * 0.55f;
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(Math.max(1.5f, s / 14f)));
        g.draw(new Line2D.Float(pole, y + s * 0.22f, pole, y + s * 0.72f));
        g.fill(new Rectangle2D.Float(x + s * 0.30f, y + s * 0.70f, s * 0.42f, s * 0.08f));
        Path2D cloth = new Path2D.Float();
        cloth.moveTo(pole, y + s * 0.20f);
        cloth.lineTo(x + s * 0.26f, y + s * 0.34f);
        cloth.lineTo(pole, y + s * 0.48f);
        cloth.closePath();
        g.setColor(FLAG);
        g.fill(cloth);
    }
}
