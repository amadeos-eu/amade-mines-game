package eu.amadeos.games.mines.ui.main;

import com.google.inject.Inject;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.commons.i18n.Mnemonics;
import eu.amadeos.gameengine.score.HighScores;
import eu.amadeos.gameengine.score.Score;
import eu.amadeos.gameengine.time.GameClock;
import eu.amadeos.games.mines.domain.Board;
import eu.amadeos.games.mines.domain.Difficulty;
import eu.amadeos.games.mines.repository.MinesSettings;
import eu.amadeos.games.mines.ui.view.BoardView;
import eu.amadeos.games.mines.ui.view.Face;

import java.time.Instant;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

/**
 * Runs the game, without Swing: new games and field sizes, the moves on the field, the clock, the face and
 * the best times. All methods run on the event dispatch thread; the window forwards menu commands and the
 * gestures of the field here and shows what the presenter tells it through {@link MinesView}.
 */
public final class MinesPresenter implements BoardView.Listener {
    private final MinesView view;
    private final Messages msg;
    private final MinesSettings settings;
    private final HighScores scores;
    private final RandomGenerator random;
    private final GameClock clock;
    private final Supplier<Instant> now;
    private Difficulty difficulty;
    private boolean questionMarks;
    private Board board;

    @Inject
    public MinesPresenter(MinesView view, Messages msg, MinesSettings settings, HighScores scores,
                          RandomGenerator random, GameClock clock) {
        this(view, msg, settings, scores, random, clock, Instant::now);
    }

    MinesPresenter(MinesView view, Messages msg, MinesSettings settings, HighScores scores, RandomGenerator random,
                   GameClock clock, Supplier<Instant> now) {
        this.view = view;
        this.msg = msg;
        this.settings = settings;
        this.scores = scores;
        this.random = random;
        this.clock = clock;
        this.now = now;
    }

    /** Opens the first game with the saved size. */
    public void start() {
        difficulty = settings.difficulty();
        questionMarks = settings.questionMarks();
        view.showOptions(difficulty, questionMarks);
        newGame();
    }

    // --8<-- [start:new-game]
    /** A new field of the current size (menu, F2 or the face). */
    public void newGame() {
        clock.reset();
        view.setTicking(false);
        board = new Board(difficulty, random);
        view.showBoard(board);
        update();
    }
    // --8<-- [end:new-game]

    public void setDifficulty(Difficulty d) {
        difficulty = d;
        settings.setDifficulty(d);
        view.showOptions(difficulty, questionMarks);
        newGame();
    }

    public void setQuestionMarks(boolean on) {
        questionMarks = on;
        settings.setQuestionMarks(on);
    }

    // --8<-- [start:moves]
    @Override
    public void open(int column, int row) {
        boolean first = board.state() == Board.State.READY;
        if (board.open(column, row) && first) {
            clock.start();
            view.setTicking(true);
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
        board.cycleMark(column, row, questionMarks);
        update();
    }

    @Override
    public void pressing(boolean down) {
        if (!board.finished()) view.showFace(down ? Face.SURPRISED : Face.SMILE);
    }
    // --8<-- [end:moves]

    /** Called by the view's timer while a game runs. */
    public void tick() {
        view.showTime(clock.seconds());
    }

    public void showScores() {
        view.showScores(difficulty, -1);
    }

    public void showAbout() {
        view.showAbout();
    }

    // --8<-- [start:update]
    /** Counters, face and field after a move; a game that just ended stops the clock. */
    private void update() {
        boolean justEnded = board.finished() && clock.running();
        if (board.finished()) {
            clock.stop();
            view.setTicking(false);
        }
        view.refreshBoard();
        view.showMinesLeft(board.minesLeft());
        view.showTime(clock.seconds());
        view.showFace(switch (board.state()) {
            case WON -> Face.WON;
            case LOST -> Face.LOST;
            default -> Face.SMILE;
        });
        if (justEnded && board.state() == Board.State.WON) recordWin(clock.seconds());
    }

    private void recordWin(int seconds) {
        String table = difficulty.id();
        if (!scores.qualifies(table, seconds)) return;
        String size = Mnemonics.strip(msg.get(difficultyKey(difficulty)));
        view.askName(msg.format("scores.won", size, seconds)).ifPresent(name -> {
            int place = scores.add(table, new Score(name, seconds, now.get()));
            view.showScores(difficulty, place);
        });
    }
    // --8<-- [end:update]

    /** The text key of a size in the menu, e.g. {@code menu.game.beginner}. */
    static String difficultyKey(Difficulty d) {
        return "menu.game." + d.id();
    }

    // ---- for tests -------------------------------------------------------------------------------

    Board board() {
        return board;
    }

    Difficulty difficulty() {
        return difficulty;
    }

    /** Puts a prepared field in place, as if a game on it had just started. */
    void play(Board prepared) {
        board = prepared;
        view.showBoard(prepared);
        clock.reset();
        clock.start();
        view.setTicking(true);
        update();
    }
}
