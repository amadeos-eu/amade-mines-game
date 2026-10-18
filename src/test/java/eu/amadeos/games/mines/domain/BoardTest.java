package eu.amadeos.games.mines.domain;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardTest {
    private static int countMines(Board board) {
        // Only after losing do the mines show.
        int mines = 0;
        for (int r = 0; r < board.rows(); r++) {
            for (int c = 0; c < board.columns(); c++) {
                Look look = board.look(c, r);
                if (look == Look.MINE || look == Look.EXPLODED) mines++;
            }
        }
        return mines;
    }

    // --8<-- [start:first-click]
    @RepeatedTest(20)
    void theFirstClickIsSafeAndOpensAnArea() {
        RandomGenerator random = new SplittableRandom();
        Board board = new Board(Difficulty.EXPERT, random);
        assertEquals(Board.State.READY, board.state());
        int c = random.nextInt(board.columns());
        int r = random.nextInt(board.rows());
        assertTrue(board.open(c, r));
        assertEquals(Board.State.PLAYING, board.state());
        assertEquals(0, board.count(c, r), "no mine around the first cell");
        assertTrue(board.isOpen(c, r));
    }
    // --8<-- [end:first-click]

    @Test
    void laysExactlyTheNumberOfMines() {
        Board board = new Board(Difficulty.INTERMEDIATE, new SplittableRandom(7));
        board.open(0, 0);
        // Step on a mine to see them all.
        outer:
        for (int r = 0; r < board.rows(); r++) {
            for (int c = 0; c < board.columns(); c++) {
                if (!board.isOpen(c, r)) {
                    board.open(c, r);
                    if (board.state() == Board.State.LOST) break outer;
                }
            }
        }
        assertEquals(Board.State.LOST, board.state());
        assertEquals(40, countMines(board));
    }

    @Test
    void aFullFieldStillKeepsTheFirstCellFree() {
        Board board = new Board(3, 3, 8, new SplittableRandom(1));
        board.open(1, 1);
        assertEquals(8, board.count(1, 1));
        assertEquals(Board.State.WON, board.state());
    }

    // --8<-- [start:empty-area]
    @Test
    void anEmptyCellOpensItsArea() {
        Board board = Board.of(
                "....",
                "....",
                "...*",
                "....");
        board.open(0, 0);
        assertTrue(board.isOpen(3, 0));
        assertTrue(board.isOpen(2, 2));
        assertEquals(1, board.count(2, 2));
        assertFalse(board.isOpen(3, 3), "behind the numbers");
        assertEquals(Board.State.PLAYING, board.state());
        board.open(3, 3);
        assertEquals(Board.State.WON, board.state());
        assertEquals(Look.FLAG, board.look(3, 2), "the mines get their flags");
        assertEquals(0, board.minesLeft());
    }
    // --8<-- [end:empty-area]

    @Test
    void steppingOnAMineLosesAndShowsTheField() {
        Board board = Board.of(
                "*..",
                "...",
                "..*");
        board.cycleMark(1, 1, false);
        board.cycleMark(2, 2, false);
        assertTrue(board.open(0, 0));
        assertEquals(Board.State.LOST, board.state());
        assertEquals(Look.EXPLODED, board.look(0, 0));
        assertEquals(Look.FLAG, board.look(2, 2), "a correct flag stays");
        assertEquals(Look.WRONG_FLAG, board.look(1, 1));
        assertFalse(board.open(1, 0), "nothing happens after the end");
        assertFalse(board.cycleMark(1, 0, true));
    }

    @Test
    void flaggedCellsCannotBeOpened() {
        Board board = Board.of("*..", "...");
        board.cycleMark(0, 0, true);
        assertFalse(board.open(0, 0));
        assertEquals(Board.State.PLAYING, board.state());
    }

    // --8<-- [start:marks]
    @Test
    void marksCycleAndCountFlags() {
        Board board = Board.of("*..", "...");
        assertEquals(1, board.minesLeft());
        board.cycleMark(1, 0, true);
        assertEquals(Look.FLAG, board.look(1, 0));
        assertEquals(0, board.minesLeft());
        board.cycleMark(2, 0, true);
        assertEquals(-1, board.minesLeft(), "more flags than mines");
        board.cycleMark(1, 0, true);
        assertEquals(Look.QUESTION, board.look(1, 0));
        assertEquals(0, board.minesLeft());
        board.cycleMark(1, 0, true);
        assertEquals(Look.HIDDEN, board.look(1, 0));
    }
    // --8<-- [end:marks]

    @Test
    void withoutQuestionMarksAFlagGoesStraightBack() {
        Board board = Board.of("*..", "...");
        board.cycleMark(1, 0, false);
        board.cycleMark(1, 0, false);
        assertEquals(Look.HIDDEN, board.look(1, 0));
        assertEquals(1, board.minesLeft());
        // A question mark set before switching them off goes away without touching the counter.
        board.cycleMark(2, 0, true);
        board.cycleMark(2, 0, true);
        assertEquals(Look.QUESTION, board.look(2, 0));
        board.cycleMark(2, 0, false);
        assertEquals(Look.HIDDEN, board.look(2, 0));
        assertEquals(1, board.minesLeft());
    }

    @Test
    void aChordOpensTheNeighboursOnceTheFlagsMatch() {
        Board board = Board.of(
                "*..",
                "...",
                "...");
        board.open(1, 1);
        assertEquals(1, board.count(1, 1));
        assertFalse(board.chord(1, 1), "no flag yet");
        board.cycleMark(0, 0, true);
        assertTrue(board.chord(1, 1));
        assertEquals(Board.State.WON, board.state());
    }

    @Test
    void aChordWithAWrongFlagLoses() {
        Board board = Board.of(
                "*..",
                "...",
                "...");
        board.open(1, 1);
        board.cycleMark(2, 2, true);
        assertEquals(7, board.chordCells(1, 1).length, "eight closed neighbours, one of them flagged");
        assertTrue(board.chord(1, 1));
        assertEquals(Board.State.LOST, board.state());
        assertEquals(Look.EXPLODED, board.look(0, 0));
    }

    @Test
    void refusesImpossibleFields() {
        assertThrows(IllegalArgumentException.class, () -> new Board(3, 3, 9, new SplittableRandom()));
        assertThrows(IllegalArgumentException.class, () -> new Board(0, 3, 1, new SplittableRandom()));
    }

    @Test
    void theClockCountsStartedSeconds() {
        long[] now = {5_000_000_000L};
        GameClock clock = new GameClock(() -> now[0]);
        assertEquals(0, clock.seconds());
        clock.start();
        assertEquals(1, clock.seconds());
        now[0] += 2_500_000_000L;
        assertEquals(3, clock.seconds());
        clock.stop();
        now[0] += 10_000_000_000L;
        assertEquals(3, clock.seconds());
        assertFalse(clock.running());
        clock.reset();
        clock.start();
        now[0] += 5_000_000_000_000L;
        assertEquals(GameClock.MAX_SECONDS, clock.seconds());
    }
}
