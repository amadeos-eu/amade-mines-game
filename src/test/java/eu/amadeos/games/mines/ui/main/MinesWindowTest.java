package eu.amadeos.games.mines.ui.main;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.score.HighScores;
import eu.amadeos.gameengine.score.PlayerName;
import eu.amadeos.gameengine.score.Ranking;
import eu.amadeos.games.mines.bootstrap.MinesModule;
import eu.amadeos.games.mines.domain.Board;
import eu.amadeos.games.mines.domain.Difficulty;
import eu.amadeos.games.mines.domain.GameClock;
import eu.amadeos.games.mines.repository.MinesSettings;
import eu.amadeos.games.mines.ui.view.BoardView;
import eu.amadeos.games.mines.ui.view.Face;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/** The real window with the real presenter: the mouse reaches the presenter and its answers are shown. */
class MinesWindowTest {
    @TempDir
    Path dir;

    private MinesWindow window;
    private MinesPresenter presenter;

    @BeforeEach
    void open() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        HighScores scores = new HighScores(dir.resolve("scores"), Ranking.LOWER_WINS);
        Messages msg = Messages.load(MinesModule.class, MinesModule.MESSAGES, Locale.ENGLISH);
        onEdt(() -> {
            window = new MinesWindow(msg, scores, new PlayerName(dir, "tester"));
            presenter = new MinesPresenter(window, msg, new MinesSettings(dir.resolve("mines.properties")), scores,
                    new SplittableRandom(3), new GameClock());
            window.bind(presenter);
            presenter.start();
        });
    }

    @AfterEach
    void close() throws Exception {
        if (window != null) onEdt(window::dispose);
    }

    private static void onEdt(Runnable code) throws Exception {
        try {
            SwingUtilities.invokeAndWait(code);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Error error) throw error;
            throw (Exception) e.getCause();
        }
    }

    private void click(int button, int column, int row) throws Exception {
        onEdt(() -> {
            BoardView view = window.boardView();
            int size = view.cellSize();
            int x = column * size + size / 2;
            int y = row * size + size / 2;
            int mask = button == MouseEvent.BUTTON1 ? InputEvent.BUTTON1_DOWN_MASK
                    : button == MouseEvent.BUTTON2 ? InputEvent.BUTTON2_DOWN_MASK : InputEvent.BUTTON3_DOWN_MASK;
            long t = System.currentTimeMillis();
            view.dispatchEvent(new MouseEvent(view, MouseEvent.MOUSE_PRESSED, t, mask, x, y, 1, false, button));
            view.dispatchEvent(new MouseEvent(view, MouseEvent.MOUSE_RELEASED, t, 0, x, y, 1, false, button));
        });
    }

    @Test
    void theFieldHasTheSizeOfTheBoard() throws Exception {
        onEdt(() -> {
            int cell = window.boardView().cellSize();
            assertEquals(9 * cell, window.boardView().getWidth());
            assertEquals(10, window.minesCounter().value());
        });
    }

    @Test
    void leftClickOpensAndStartsTheTimer() throws Exception {
        click(MouseEvent.BUTTON1, 4, 4);
        onEdt(() -> {
            assertEquals(Board.State.PLAYING, presenter.board().state());
            assertTrue(window.ticking());
            assertEquals(1, window.timeCounter().value());
        });
    }

    @Test
    void rightClickFlags() throws Exception {
        onEdt(() -> presenter.play(Board.of("*..", "...", "...")));
        click(MouseEvent.BUTTON3, 0, 0);
        onEdt(() -> assertEquals(0, window.minesCounter().value()));
    }

    @Test
    void middleClickChordsAndTheFaceWins() throws Exception {
        onEdt(() -> presenter.play(Board.of("*..", "...", "...")));
        click(MouseEvent.BUTTON1, 1, 1);
        click(MouseEvent.BUTTON3, 0, 0);
        // A best time would open the name dialog; fill the table first.
        onEdt(() -> {
            for (int i = 0; i < HighScores.SIZE; i++) {
                new HighScores(dir.resolve("scores"), Ranking.LOWER_WINS).add("beginner",
                        new eu.amadeos.gameengine.score.Score("Pro", 0, java.time.Instant.EPOCH));
            }
        });
        click(MouseEvent.BUTTON2, 1, 1);
        onEdt(() -> assertEquals(Face.WON, window.face()));
    }

    @Test
    void anotherSizeResizesTheWindow() throws Exception {
        onEdt(() -> {
            int before = window.getWidth();
            presenter.setDifficulty(Difficulty.EXPERT);
            assertTrue(window.getWidth() > before);
        });
    }
}
