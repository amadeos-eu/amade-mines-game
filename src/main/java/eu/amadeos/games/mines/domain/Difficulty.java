package eu.amadeos.games.mines.domain;

import java.util.Locale;
import java.util.Optional;

/** The three classic field sizes. */
public enum Difficulty {
    BEGINNER(9, 9, 10),
    INTERMEDIATE(16, 16, 40),
    EXPERT(30, 16, 99);

    private final int columns;
    private final int rows;
    private final int mines;

    Difficulty(int columns, int rows, int mines) {
        this.columns = columns;
        this.rows = rows;
        this.mines = mines;
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

    /** Name in settings files and of the high-score table, e.g. {@code beginner}. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<Difficulty> ofId(String id) {
        for (Difficulty d : values()) {
            if (d.id().equals(id)) return Optional.of(d);
        }
        return Optional.empty();
    }
}
