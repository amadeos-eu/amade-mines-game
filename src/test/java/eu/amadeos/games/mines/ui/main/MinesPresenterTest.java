package eu.amadeos.games.mines.ui.main;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.score.HighScores;
import eu.amadeos.gameengine.score.Ranking;
import eu.amadeos.gameengine.score.Score;
import eu.amadeos.gameengine.time.GameClock;
import eu.amadeos.games.mines.bootstrap.MinesModule;
import eu.amadeos.games.mines.domain.Board;
import eu.amadeos.games.mines.domain.Difficulty;
import eu.amadeos.games.mines.repository.MinesSettings;
import eu.amadeos.games.mines.ui.view.Face;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The game without a window: the presenter talks to a {@link FakeView}. */
class MinesPresenterTest {
    private static final Instant WON_AT = Instant.parse("2026-10-18T20:00:00Z");

    @TempDir
    Path dir;

    private final long[] now = {1_000_000_000L};
    private final FakeView view = new FakeView();
    private MinesSettings settings;
    private HighScores scores;
    private MinesPresenter presenter;

    @BeforeEach
    void start() {
        settings = new MinesSettings(dir.resolve("mines.properties"));
        scores = new HighScores(dir.resolve("scores"), Ranking.LOWER_WINS);
        Messages msg = Messages.load(MinesModule.class, MinesModule.MESSAGES, Locale.ENGLISH);
        presenter = new MinesPresenter(view, msg, settings, scores, new SplittableRandom(3),
                new GameClock(() -> now[0]), () -> WON_AT);
        presenter.start();
    }

    @Test
    void startsWithTheSavedSize() {
        assertEquals(Difficulty.BEGINNER, view.difficulty);
        assertTrue(view.questionMarks);
        assertEquals(9, view.board.columns());
        assertEquals(10, view.minesLeft);
        assertEquals(0, view.seconds);
        assertEquals(Face.SMILE, view.face);
        assertFalse(view.ticking);
    }

    // --8<-- [start:test-first-click]
    @Test
    void theFirstClickStartsTheClock() {
        presenter.open(4, 4);
        assertEquals(Board.State.PLAYING, view.board.state());
        assertTrue(view.ticking);
        assertEquals(1, view.seconds);
        now[0] += 2_000_000_000L;
        presenter.tick();
        assertEquals(3, view.seconds);
    }
    // --8<-- [end:test-first-click]

    @Test
    void pressingShowsSurprise() {
        presenter.pressing(true);
        assertEquals(Face.SURPRISED, view.face);
        presenter.pressing(false);
        assertEquals(Face.SMILE, view.face);
    }

    @Test
    void flagsCountDown() {
        presenter.play(Board.of("*..", "...", "..."));
        presenter.mark(0, 0);
        assertEquals(0, view.minesLeft);
    }

    @Test
    void questionMarksCanBeSwitchedOff() {
        presenter.setQuestionMarks(false);
        presenter.play(Board.of("*..", "...", "..."));
        presenter.mark(1, 0);
        presenter.mark(1, 0);
        assertEquals(1, view.minesLeft);
        assertFalse(settings.questionMarks());
    }

    @Test
    void losingStopsTheClock() {
        presenter.play(Board.of("*..", "...", "..."));
        presenter.open(0, 0);
        assertEquals(Face.LOST, view.face);
        assertFalse(view.ticking);
        presenter.pressing(true);
        assertEquals(Face.LOST, view.face, "the face stays after the end");
    }

    // --8<-- [start:test-win]
    @Test
    void aBestTimeAsksForTheNameAndShowsTheTable() {
        view.names.push(Optional.of("Anna"));
        presenter.play(Board.of("*..", "...", "..."));
        now[0] += 4_200_000_000L;
        presenter.open(2, 2);
        assertEquals(Face.WON, view.face);
        assertEquals("You cleared Beginner in 5 seconds.", view.askedWith);
        assertEquals(List.of(new Score("Anna", 5, WON_AT)), scores.list("beginner"));
        assertEquals(Difficulty.BEGINNER, view.scoresShown);
        assertEquals(0, view.highlight);
    }
    // --8<-- [end:test-win]

    @Test
    void aSlowWinDoesNotAsk() {
        for (int i = 0; i < HighScores.SIZE; i++) scores.add("beginner", new Score("Pro", 1, Instant.EPOCH));
        presenter.play(Board.of("*..", "...", "..."));
        presenter.open(2, 2);
        assertEquals(Face.WON, view.face);
        assertNull(view.askedWith);
    }

    @Test
    void cancellingTheNameKeepsTheTable() {
        presenter.play(Board.of("*..", "...", "..."));
        presenter.open(2, 2);
        assertEquals(List.of(), scores.list("beginner"));
        assertNull(view.scoresShown);
    }

    @Test
    void middleClickChords() {
        presenter.play(Board.of("*..", "...", "..."));
        presenter.open(1, 1);
        presenter.mark(0, 0);
        presenter.chord(1, 1);
        assertEquals(Board.State.WON, view.board.state());
    }

    @Test
    void anotherSizeIsRemembered() {
        presenter.setDifficulty(Difficulty.EXPERT);
        assertEquals(30, view.board.columns());
        assertEquals(99, view.minesLeft);
        assertEquals(Difficulty.EXPERT, view.difficulty);
        assertEquals(Difficulty.EXPERT, settings.difficulty());
        presenter.showScores();
        assertEquals(Difficulty.EXPERT, view.scoresShown);
        assertEquals(-1, view.highlight);
    }

    @Test
    void aNewGameResetsTheClock() {
        presenter.open(4, 4);
        presenter.newGame();
        assertFalse(view.ticking);
        assertEquals(0, view.seconds);
        assertEquals(Board.State.READY, view.board.state());
    }
}
