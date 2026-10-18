package eu.amadeos.games.mines.ui;

import com.google.inject.Inject;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.commons.i18n.Mnemonics;
import eu.amadeos.commons.swing.AmadeUI;
import eu.amadeos.commons.swing.EdtExecutor;
import eu.amadeos.gameengine.score.HighScores;
import eu.amadeos.gameengine.score.PlayerName;
import eu.amadeos.gameengine.score.Score;
import eu.amadeos.gameengine.swing.GameMenus;
import eu.amadeos.gameengine.swing.GameTexts;
import eu.amadeos.gameengine.swing.HighScoreDialog;
import eu.amadeos.games.mines.bootstrap.AmadeMinesApplication;
import eu.amadeos.games.mines.domain.Board;
import eu.amadeos.games.mines.domain.Difficulty;
import eu.amadeos.games.mines.domain.GameClock;
import eu.amadeos.games.mines.repository.MinesSettings;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

/**
 * The game window: menu, mine counter, face button and clock above the field. It also runs the game: it
 * starts and stops the clock and records a won game in the high scores.
 */
public final class MinesWindow extends JFrame implements BoardView.Listener {
    /** How often the clock is redrawn while a game runs. */
    private static final int TICK = 200;

    private final Messages msg;
    private final GameMenus menus = new GameMenus(GameTexts.load());
    private final MinesSettings settings;
    private final HighScores scores;
    private final PlayerName playerName;
    private final RandomGenerator random;
    private final GameClock clock;
    private final Counter minesCounter = new Counter("mines-left");
    private final Counter timeCounter = new Counter("time");
    private final FaceIcon face;
    private final JButton faceButton;
    private final BoardView view;
    private final Timer ticker;
    private final Map<Difficulty, JRadioButtonMenuItem> difficultyItems = new EnumMap<>(Difficulty.class);
    private final JCheckBoxMenuItem questionMarksItem;
    private Difficulty difficulty;
    private Board board;

    @Inject
    public MinesWindow(Messages msg, MinesSettings settings, HighScores scores, PlayerName playerName,
                       RandomGenerator random, GameClock clock) {
        super(msg.get("window.title"));
        EdtExecutor.requireEdt();
        this.msg = msg;
        this.settings = settings;
        this.scores = scores;
        this.playerName = playerName;
        this.random = random;
        this.clock = clock;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        java.net.URL icon = getClass().getResource("/icons/amade-mines-game.png");
        if (icon != null) setIconImage(new ImageIcon(icon).getImage());

        difficulty = settings.difficulty();
        board = new Board(difficulty, random);
        int cellSize = cellSize();
        view = new BoardView(board, cellSize, this);
        face = new FaceIcon(Math.round(cellSize * 1.15f));
        faceButton = new JButton(face);
        faceButton.setName("face");
        faceButton.setToolTipText(msg.get("face.tooltip"));
        faceButton.setFocusable(false);
        faceButton.setMargin(new Insets(2, 2, 2, 2));
        faceButton.addActionListener(e -> newGame());
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
        field.add(view, BorderLayout.CENTER);
        JPanel content = new JPanel(new BorderLayout(0, 6));
        content.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        content.add(top, BorderLayout.NORTH);
        content.add(field, BorderLayout.CENTER);
        setContentPane(content);

        questionMarksItem = Mnemonics.apply(new JCheckBoxMenuItem(), msg.get("menu.game.marks"));
        questionMarksItem.setName("game-marks");
        questionMarksItem.setSelected(settings.questionMarks());
        questionMarksItem.addActionListener(e -> settings.setQuestionMarks(questionMarksItem.isSelected()));
        setJMenuBar(menuBar());

        ticker = new Timer(TICK, e -> timeCounter.setValue(clock.seconds()));
        newGame();
        pack();
    }

    /** About 1.7 times the text size, so the field grows with the font on large screens. */
    static int cellSize() {
        java.awt.Font font = UIManager.getFont("Label.font");
        int size = font == null ? 12 : font.getSize();
        return Math.max(16, Math.round(size * 1.7f));
    }

    private JMenuBar menuBar() {
        JMenu game = menus.game();
        game.add(menus.newGame(this::newGame));
        game.addSeparator();
        ButtonGroup group = new ButtonGroup();
        for (Difficulty d : Difficulty.values()) {
            JRadioButtonMenuItem item = Mnemonics.apply(new JRadioButtonMenuItem(), msg.get(difficultyKey(d)));
            item.setName("game-" + d.id());
            item.setSelected(d == difficulty);
            item.addActionListener(e -> setDifficulty(d));
            group.add(item);
            game.add(item);
            difficultyItems.put(d, item);
        }
        game.addSeparator();
        game.add(questionMarksItem);
        game.addSeparator();
        game.add(menus.highScores(() -> showScores(difficulty.id(), -1)));
        game.addSeparator();
        game.add(menus.exit(this::dispose));
        JMenu help = menus.help();
        help.add(menus.about(msg.get("window.title"), this::showAbout));
        JMenuBar bar = new JMenuBar();
        bar.add(game);
        bar.add(help);
        return bar;
    }

    static String difficultyKey(Difficulty d) {
        return "menu.game." + d.id();
    }

    /** A new field of the current size. */
    public void newGame() {
        EdtExecutor.requireEdt();
        ticker.stop();
        clock.reset();
        board = new Board(difficulty, random);
        view.setBoard(board);
        face.setFace(FaceIcon.Face.SMILE);
        update();
    }

    void setDifficulty(Difficulty d) {
        difficulty = d;
        difficultyItems.get(d).setSelected(true);
        settings.setDifficulty(d);
        newGame();
        pack();
    }

    @Override
    public void open(int column, int row) {
        if (board.state() == Board.State.READY && board.open(column, row)) {
            clock.start();
            ticker.start();
        } else {
            board.open(column, row);
        }
        update();
    }

    @Override
    public void chord(int column, int row) {
        board.chord(column, row);
        update();
    }

    @Override
    public void mark(int column, int row) {
        board.cycleMark(column, row, questionMarksItem.isSelected());
        update();
    }

    @Override
    public void pressing(boolean down) {
        if (board.finished()) return;
        face.setFace(down ? FaceIcon.Face.SURPRISED : FaceIcon.Face.SMILE);
        faceButton.repaint();
    }

    /** Counters, face and field after a move; a game that just ended stops the clock. */
    private void update() {
        boolean ended = board.finished() && clock.running();
        if (board.finished()) {
            clock.stop();
            ticker.stop();
        }
        minesCounter.setValue(board.minesLeft());
        timeCounter.setValue(clock.seconds());
        face.setFace(switch (board.state()) {
            case WON -> FaceIcon.Face.WON;
            case LOST -> FaceIcon.Face.LOST;
            default -> FaceIcon.Face.SMILE;
        });
        faceButton.repaint();
        view.repaint();
        if (ended && board.state() == Board.State.WON) {
            // After the field shows the last cells and the face its sunglasses.
            SwingUtilities.invokeLater(() -> recordWin(clock.seconds()));
        }
    }

    private void recordWin(int seconds) {
        String table = difficulty.id();
        if (!scores.qualifies(table, seconds)) return;
        String title = Mnemonics.strip(msg.get(difficultyKey(difficulty)));
        HighScoreDialog.askName(this, msg.format("scores.won", title, seconds), playerName).ifPresent(name -> {
            int place = scores.add(table, new Score(name, seconds, Instant.now()));
            showScores(table, place);
        });
    }

    private void showScores(String selected, int place) {
        List<HighScoreDialog.Table> tables = new ArrayList<>();
        for (Difficulty d : Difficulty.values()) {
            tables.add(new HighScoreDialog.Table(d.id(), Mnemonics.strip(msg.get(difficultyKey(d)))));
        }
        HighScoreDialog.show(this, scores, tables, seconds -> msg.format("time.seconds", seconds), selected, place);
    }

    private void showAbout() {
        // Swing's HTML only wraps lines with a fixed body width; it grows with the font like the cells.
        int width = cellSize() * 16;
        JOptionPane.showMessageDialog(this,
                "<html><body style='width: " + width + "px'><center><h2>" + msg.get("window.title") + " " + AmadeMinesApplication.version() + "</h2>"
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

    Board board() {
        return board;
    }

    BoardView view() {
        return view;
    }

    Counter minesCounter() {
        return minesCounter;
    }

    Counter timeCounter() {
        return timeCounter;
    }

    FaceIcon.Face face() {
        return face.face();
    }

    Difficulty difficulty() {
        return difficulty;
    }

    /** Puts a prepared field in place, as if a game on it had just started. */
    void play(Board prepared) {
        board = prepared;
        view.setBoard(prepared);
        clock.reset();
        clock.start();
        update();
    }
}
