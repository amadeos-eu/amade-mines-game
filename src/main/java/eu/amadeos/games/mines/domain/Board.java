package eu.amadeos.games.mines.domain;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.random.RandomGenerator;

/**
 * The mine field and the rules, without any user interface. Mines are laid when the first cell is opened,
 * never on that cell and, if there is room, not on its neighbours either, so the first click always opens
 * an area. Cells are addressed by column and row from the top left.
 */
public final class Board {
    public enum State {
        /** No cell opened yet; the mines are not laid. */
        READY,
        PLAYING,
        WON,
        LOST
    }

    private enum Mark { NONE, FLAG, QUESTION }

    private final int columns;
    private final int rows;
    private final int mines;
    private final RandomGenerator random;
    private final boolean[] mine;
    private final boolean[] open;
    private final Mark[] marks;
    private final int[] counts;
    private State state = State.READY;
    private int opened;
    private int flags;
    private int exploded = -1;

    public Board(int columns, int rows, int mines, RandomGenerator random) {
        if (columns < 1 || rows < 1) throw new IllegalArgumentException("Empty field: " + columns + "×" + rows);
        if (mines < 1 || mines >= columns * rows) throw new IllegalArgumentException("Bad number of mines: " + mines);
        this.columns = columns;
        this.rows = rows;
        this.mines = mines;
        this.random = random;
        int cells = columns * rows;
        mine = new boolean[cells];
        open = new boolean[cells];
        marks = new Mark[cells];
        Arrays.fill(marks, Mark.NONE);
        counts = new int[cells];
    }

    public Board(Difficulty difficulty, RandomGenerator random) {
        this(difficulty.columns(), difficulty.rows(), difficulty.mines(), random);
    }

    /**
     * A field with the mines already laid, for tests: one string per row, {@code *} is a mine, any other
     * character a free cell.
     */
    public static Board of(String... rowsOfCells) {
        int columns = rowsOfCells[0].length();
        int count = 0;
        for (String row : rowsOfCells) count += (int) row.chars().filter(c -> c == '*').count();
        Board board = new Board(columns, rowsOfCells.length, count, null);
        for (int r = 0; r < rowsOfCells.length; r++) {
            for (int c = 0; c < columns; c++) board.mine[r * columns + c] = rowsOfCells[r].charAt(c) == '*';
        }
        board.countNeighbours();
        board.state = State.PLAYING;
        return board;
    }

    public int columns() {
        return columns;
    }

    public int rows() {
        return rows;
    }

    public int mines() {
        return mines;
    }

    public State state() {
        return state;
    }

    public boolean finished() {
        return state == State.WON || state == State.LOST;
    }

    /** Mines minus flags, as the counter shows it; negative if there are more flags than mines. */
    public int minesLeft() {
        return mines - flags;
    }

    public boolean contains(int column, int row) {
        return column >= 0 && column < columns && row >= 0 && row < rows;
    }

    /** Mines around an opened cell, 0 to 8. */
    public int count(int column, int row) {
        return counts[index(column, row)];
    }

    public boolean isOpen(int column, int row) {
        return open[index(column, row)];
    }

    public Look look(int column, int row) {
        int i = index(column, row);
        if (i == exploded) return Look.EXPLODED;
        if (open[i]) return Look.OPEN;
        if (state == State.LOST) {
            if (mine[i] && marks[i] != Mark.FLAG) return Look.MINE;
            if (!mine[i] && marks[i] == Mark.FLAG) return Look.WRONG_FLAG;
        }
        return switch (marks[i]) {
            case FLAG -> Look.FLAG;
            case QUESTION -> Look.QUESTION;
            case NONE -> Look.HIDDEN;
        };
    }

    /**
     * Opens a cell; an empty cell opens its neighbours as well. Flagged cells stay closed.
     *
     * @return true if the field changed
     */
    public boolean open(int column, int row) {
        if (finished() || !contains(column, row)) return false;
        int i = index(column, row);
        if (open[i] || marks[i] == Mark.FLAG) return false;
        if (state == State.READY) {
            layMines(i);
            state = State.PLAYING;
        }
        if (mine[i]) {
            exploded = i;
            state = State.LOST;
            return true;
        }
        flood(i);
        if (opened == columns * rows - mines) win();
        return true;
    }

    /**
     * Opens the closed neighbours of an opened number once as many flags surround it as the number says
     * (a "chord"). A wrong flag loses the game here, as in the original.
     *
     * @return true if the field changed
     */
    public boolean chord(int column, int row) {
        if (state != State.PLAYING || !contains(column, row)) return false;
        int i = index(column, row);
        if (!open[i] || counts[i] == 0) return false;
        int flagged = 0;
        for (int n : neighbours(i)) if (marks[n] == Mark.FLAG) flagged++;
        if (flagged != counts[i]) return false;
        boolean changed = false;
        for (int n : neighbours(i)) {
            if (state != State.PLAYING) break;
            changed |= open(n % columns, n / columns);
        }
        return changed;
    }

    /**
     * Cycles the mark of a closed cell: none, flag, question mark (if switched on), none.
     *
     * @return true if the field changed
     */
    public boolean cycleMark(int column, int row, boolean questionMarks) {
        if (finished() || !contains(column, row)) return false;
        int i = index(column, row);
        if (open[i]) return false;
        Mark before = marks[i];
        marks[i] = switch (before) {
            case NONE -> Mark.FLAG;
            case FLAG -> questionMarks ? Mark.QUESTION : Mark.NONE;
            case QUESTION -> Mark.NONE;
        };
        if (marks[i] == Mark.FLAG) flags++;
        if (before == Mark.FLAG) flags--;
        return true;
    }

    /**
     * The closed, unflagged neighbours a chord on this cell would open (for showing them pressed), as
     * indices {@code row * columns() + column}.
     */
    public int[] chordCells(int column, int row) {
        if (!contains(column, row)) return new int[0];
        int i = index(column, row);
        return Arrays.stream(neighbours(i)).filter(n -> !open[n] && marks[n] != Mark.FLAG).toArray();
    }

    private void layMines(int first) {
        int cells = columns * rows;
        boolean[] blocked = new boolean[cells];
        blocked[first] = true;
        // Keep the neighbours free as well unless the field is so full that the mines would not fit.
        int[] around = neighbours(first);
        if (mines <= cells - 1 - around.length) for (int n : around) blocked[n] = true;
        int[] candidates = new int[cells];
        int size = 0;
        for (int i = 0; i < cells; i++) if (!blocked[i]) candidates[size++] = i;
        // Partial Fisher–Yates shuffle: the first `mines` candidates become mines.
        for (int k = 0; k < mines; k++) {
            int j = k + random.nextInt(size - k);
            int swap = candidates[k];
            candidates[k] = candidates[j];
            candidates[j] = swap;
            mine[candidates[k]] = true;
        }
        countNeighbours();
    }

    private void countNeighbours() {
        for (int i = 0; i < mine.length; i++) {
            int count = 0;
            for (int n : neighbours(i)) if (mine[n]) count++;
            counts[i] = count;
        }
    }

    /** Opens the cell and, from every empty cell reached, its neighbours; without recursion. */
    private void flood(int start) {
        Deque<Integer> pending = new ArrayDeque<>();
        pending.push(start);
        while (!pending.isEmpty()) {
            int i = pending.pop();
            if (open[i] || mine[i] || marks[i] == Mark.FLAG) continue;
            open[i] = true;
            opened++;
            if (marks[i] == Mark.QUESTION) marks[i] = Mark.NONE;
            if (counts[i] == 0) for (int n : neighbours(i)) if (!open[n]) pending.push(n);
        }
    }

    /** Flags every mine, as the counter then reads 0. */
    private void win() {
        state = State.WON;
        for (int i = 0; i < mine.length; i++) if (mine[i]) marks[i] = Mark.FLAG;
        flags = mines;
    }

    private int[] neighbours(int i) {
        int c = i % columns;
        int r = i / columns;
        int[] result = new int[8];
        int size = 0;
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if ((dr != 0 || dc != 0) && contains(c + dc, r + dr)) result[size++] = (r + dr) * columns + c + dc;
            }
        }
        return Arrays.copyOf(result, size);
    }

    private int index(int column, int row) {
        if (!contains(column, row)) throw new IndexOutOfBoundsException(column + "," + row);
        return row * columns + column;
    }
}
