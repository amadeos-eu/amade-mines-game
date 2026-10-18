package eu.amadeos.games.mines.ui;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.score.HighScores;
import eu.amadeos.gameengine.score.PlayerName;
import eu.amadeos.gameengine.score.Ranking;
import eu.amadeos.gameengine.score.Score;
import eu.amadeos.games.mines.bootstrap.MinesModule;
import eu.amadeos.games.mines.domain.Board;
import eu.amadeos.games.mines.domain.Difficulty;
import eu.amadeos.games.mines.domain.GameClock;
import eu.amadeos.games.mines.repository.MinesSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Locale;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class MinesWindowTest {
    @TempDir
    Path dir;

    private final long[] now = {1_000_000_000L};
    private MinesWindow window;
    private MinesSettings settings;

    @BeforeEach
    void open() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        settings = new MinesSettings(dir.resolve("config/mines.properties"));
        HighScores scores = new HighScores(dir.resolve("scores"), Ranking.LOWER_WINS);
        // Full tables of one-second games, so that no test win opens the name dialog.
        for (Difficulty d : Difficulty.values()) {
            for (int i = 0; i < HighScores.SIZE; i++) scores.add(d.id(), new Score("Pro", 1, Instant.EPOCH));
        }
        Messages msg = Messages.load(MinesModule.class, MinesModule.MESSAGES, Locale.ENGLISH);
        onEdt(() -> window = new MinesWindow(msg, settings, scores, new PlayerName(dir, "tester"),
                new SplittableRandom(3), new GameClock(() -> now[0])));
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
            BoardView view = window.view();
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
    void startsWithTheSavedSize() throws Exception {
        onEdt(() -> {
            assertEquals(Difficulty.BEGINNER, window.difficulty());
            assertEquals(10, window.minesCounter().value());
            assertEquals(0, window.timeCounter().value());
            assertEquals(new Point(9, 9), window.view().cellAt(new Point(window.view().getPreferredSize().width,
                    window.view().getPreferredSize().height)));
        });
    }

    @Test
    void theFirstClickStartsTheClock() throws Exception {
        click(MouseEvent.BUTTON1, 4, 4);
        onEdt(() -> {
            assertEquals(Board.State.PLAYING, window.board().state());
            assertEquals(1, window.timeCounter().value());
            assertEquals(FaceIcon.Face.SMILE, window.face());
        });
    }

    @Test
    void rightClickFlagsAndCounts() throws Exception {
        onEdt(() -> window.play(Board.of("*..", "...", "...")));
        click(MouseEvent.BUTTON3, 0, 0);
        onEdt(() -> assertEquals(0, window.minesCounter().value()));
    }

    @Test
    void winningShowsSunglassesAndStopsTheClock() throws Exception {
        onEdt(() -> window.play(Board.of("*..", "...", "...")));
        now[0] += 4_200_000_000L;
        click(MouseEvent.BUTTON1, 2, 2);
        onEdt(() -> {
            assertEquals(Board.State.WON, window.board().state());
            assertEquals(FaceIcon.Face.WON, window.face());
            assertEquals(5, window.timeCounter().value());
        });
        now[0] += 10_000_000_000L;
        onEdt(() -> assertEquals(5, window.timeCounter().value()));
    }

    @Test
    void middleClickChords() throws Exception {
        onEdt(() -> window.play(Board.of("*..", "...", "...")));
        click(MouseEvent.BUTTON1, 1, 1);
        click(MouseEvent.BUTTON3, 0, 0);
        click(MouseEvent.BUTTON2, 1, 1);
        onEdt(() -> assertEquals(Board.State.WON, window.board().state()));
    }

    @Test
    void losingShowsTheFace() throws Exception {
        onEdt(() -> window.play(Board.of("*..", "...", "...")));
        click(MouseEvent.BUTTON1, 0, 0);
        onEdt(() -> {
            assertEquals(FaceIcon.Face.LOST, window.face());
            assertFalse(window.board().isOpen(1, 1));
        });
    }

    @Test
    void anotherSizeIsRememberedAndResizesTheField() throws Exception {
        onEdt(() -> {
            int before = window.getWidth();
            window.setDifficulty(Difficulty.EXPERT);
            assertEquals(30, window.board().columns());
            assertEquals(99, window.minesCounter().value());
            assertEquals(true, window.getWidth() > before);
        });
        assertEquals(Difficulty.EXPERT, settings.difficulty());
    }

    @Test
    void counterDigits() {
        assertEquals(3, Counter.segments(-7).length);
        assertEquals(0x40, Counter.segments(-7)[0]);
        assertEquals(Counter.segments(999)[0], Counter.segments(5000)[0]);
    }
}
