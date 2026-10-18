package eu.amadeos.games.mines.ui.main;

import com.google.inject.Inject;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.commons.i18n.Mnemonics;
import eu.amadeos.commons.swing.AmadeUI;
import eu.amadeos.commons.swing.EdtExecutor;
import eu.amadeos.gameengine.score.HighScores;
import eu.amadeos.gameengine.score.PlayerName;
import eu.amadeos.gameengine.swing.GameMenus;
import eu.amadeos.gameengine.swing.GameTexts;
import eu.amadeos.gameengine.swing.HighScoreDialog;
import eu.amadeos.games.mines.bootstrap.AmadeMinesApplication;
import eu.amadeos.games.mines.domain.Board;
import eu.amadeos.games.mines.domain.Difficulty;
import eu.amadeos.games.mines.ui.view.BoardView;
import eu.amadeos.games.mines.ui.view.Counter;
import eu.amadeos.games.mines.ui.view.Face;
import eu.amadeos.games.mines.ui.view.FaceIcon;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The game window: menu, mine counter, face button and clock above the field. It only shows what
 * {@link MinesPresenter} tells it and passes menu commands and the gestures of the field on.
 */
public final class MinesWindow extends JFrame implements MinesView {
    /** How often the clock is redrawn while a game runs. */
    private static final int TICK = 200;

    private final Messages msg;
    private final GameMenus menus = new GameMenus(GameTexts.load());
    private final HighScores scores;
    private final PlayerName playerName;
    private final Counter minesCounter = new Counter("mines-left");
    private final Counter timeCounter = new Counter("time");
    private final FaceIcon face;
    private final JButton faceButton;
    private final BoardView boardView;
    private final Timer ticker;
    private final Map<Difficulty, JRadioButtonMenuItem> difficultyItems = new EnumMap<>(Difficulty.class);
    private final JCheckBoxMenuItem questionMarksItem;
    private MinesPresenter presenter;

    @Inject
    public MinesWindow(Messages msg, HighScores scores, PlayerName playerName) {
        super(msg.get("window.title"));
        EdtExecutor.requireEdt();
        this.msg = msg;
        this.scores = scores;
        this.playerName = playerName;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        java.net.URL icon = getClass().getResource("/icons/amade-mines-game.png");
        if (icon != null) setIconImage(new ImageIcon(icon).getImage());

        int cellSize = cellSize();
        // --8<-- [start:forward]
        boardView = new BoardView(null, cellSize, new BoardView.Listener() {
            @Override
            public void open(int column, int row) {
                presenter.open(column, row);
            }

            @Override
            public void chord(int column, int row) {
                presenter.chord(column, row);
            }

            @Override
            public void mark(int column, int row) {
                presenter.mark(column, row);
            }

            @Override
            public void pressing(boolean down) {
                presenter.pressing(down);
            }
        });
        // --8<-- [end:forward]
        face = new FaceIcon(Math.round(cellSize * 1.15f));
        faceButton = new JButton(face);
        faceButton.setName("face");
        faceButton.setToolTipText(msg.get("face.tooltip"));
        faceButton.setFocusable(false);
        faceButton.setMargin(new Insets(2, 2, 2, 2));
        faceButton.addActionListener(e -> presenter.newGame());
        int counterHeight = Math.round(cellSize * 1.5f);
        for (Counter counter : List.of(minesCounter, timeCounter)) {
            counter.setPreferredSize(new Dimension(Math.round(counterHeight * Counter.ASPECT), counterHeight));
        }
        minesCounter.setToolTipText(msg.get("counter.mines"));
        timeCounter.setToolTipText(msg.get("counter.time"));

        JPanel faceHolder = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        faceHolder.add(faceButton);
        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setBorder(BorderFactory.createCompoundBorder(AmadeUI.sunken(), BorderFactory.createEmptyBorder(4, 6, 4, 6)));
        top.add(minesCounter, BorderLayout.WEST);
        top.add(faceHolder, BorderLayout.CENTER);
        top.add(timeCounter, BorderLayout.EAST);
        JPanel field = new JPanel(new BorderLayout());
        field.setBorder(AmadeUI.sunken());
        field.add(boardView, BorderLayout.CENTER);
        JPanel content = new JPanel(new BorderLayout(0, 6));
        content.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        content.add(top, BorderLayout.NORTH);
        content.add(field, BorderLayout.CENTER);
        setContentPane(content);

        questionMarksItem = Mnemonics.apply(new JCheckBoxMenuItem(), msg.get("menu.game.marks"));
        questionMarksItem.setName("game-marks");
        questionMarksItem.addActionListener(e -> presenter.setQuestionMarks(questionMarksItem.isSelected()));
        setJMenuBar(menuBar());

        ticker = new Timer(TICK, e -> presenter.tick());
    }

    /** Connects the window with the presenter; call once before {@link MinesPresenter#start()}. */
    public void bind(MinesPresenter presenter) {
        this.presenter = presenter;
    }

    /** About 1.7 times the text size, so the field grows with the font on large screens. */
    static int cellSize() {
        java.awt.Font font = UIManager.getFont("Label.font");
        int size = font == null ? 12 : font.getSize();
        return Math.max(16, Math.round(size * 1.7f));
    }

    private JMenuBar menuBar() {
        JMenu game = menus.game();
        game.add(menus.newGame(() -> presenter.newGame()));
        game.addSeparator();
        ButtonGroup group = new ButtonGroup();
        for (Difficulty d : Difficulty.values()) {
            JRadioButtonMenuItem item = Mnemonics.apply(new JRadioButtonMenuItem(),
                    msg.get(MinesPresenter.difficultyKey(d)));
            item.setName("game-" + d.id());
            item.addActionListener(e -> presenter.setDifficulty(d));
            group.add(item);
            game.add(item);
            difficultyItems.put(d, item);
        }
        game.addSeparator();
        game.add(questionMarksItem);
        game.addSeparator();
        game.add(menus.highScores(() -> presenter.showScores()));
        game.addSeparator();
        game.add(menus.exit(this::dispose));
        JMenu help = menus.help();
        help.add(menus.about(msg.get("window.title"), () -> presenter.showAbout()));
        JMenuBar bar = new JMenuBar();
        bar.add(game);
        bar.add(help);
        return bar;
    }

    // ---- MinesView -------------------------------------------------------------------------------

    @Override
    public void showBoard(Board board) {
        boardView.setBoard(board);
        pack();
    }

    @Override
    public void refreshBoard() {
        boardView.repaint();
    }

    @Override
    public void showMinesLeft(int mines) {
        minesCounter.setValue(mines);
    }

    @Override
    public void showTime(int seconds) {
        timeCounter.setValue(seconds);
    }

    @Override
    public void showFace(Face newFace) {
        face.setFace(newFace);
        faceButton.repaint();
    }

    @Override
    public void showOptions(Difficulty difficulty, boolean questionMarks) {
        difficultyItems.get(difficulty).setSelected(true);
        questionMarksItem.setSelected(questionMarks);
    }

    @Override
    public void setTicking(boolean ticking) {
        if (ticking) ticker.start();
        else ticker.stop();
    }

    @Override
    public Optional<String> askName(String message) {
        return HighScoreDialog.askName(this, message, playerName);
    }

    @Override
    public void showScores(Difficulty selected, int highlight) {
        List<HighScoreDialog.Table> tables = new ArrayList<>();
        for (Difficulty d : Difficulty.values()) {
            tables.add(new HighScoreDialog.Table(d.id(), Mnemonics.strip(msg.get(MinesPresenter.difficultyKey(d)))));
        }
        HighScoreDialog.show(this, scores, tables, seconds -> msg.format("time.seconds", seconds), selected.id(),
                highlight);
    }

    @Override
    public void showAbout() {
        // Swing's HTML only wraps lines with a fixed body width; it grows with the font like the cells.
        int width = cellSize() * 16;
        JOptionPane.showMessageDialog(this,
                "<html><body style='width: " + width + "px'><center><h2>"
                        + msg.get("window.title") + " " + AmadeMinesApplication.version() + "</h2>"
                        + "<p>" + msg.get("dialog.about.text") + "</p><br>"
                        + "<p>" + msg.get("dialog.about.rules") + "</p><br>"
                        + "<small>amade-mines-game &bull; &copy; 2026 datazuul.com &bull; EUPL-1.2</small></center></html>",
                Mnemonics.strip(menus.texts().format("menu.help.about", msg.get("window.title"))),
                JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void dispose() {
        ticker.stop();
        super.dispose();
    }

    // ---- for tests -------------------------------------------------------------------------------

    BoardView boardView() {
        return boardView;
    }

    Counter minesCounter() {
        return minesCounter;
    }

    Counter timeCounter() {
        return timeCounter;
    }

    Face face() {
        return face.face();
    }

    boolean ticking() {
        return ticker.isRunning();
    }
}
