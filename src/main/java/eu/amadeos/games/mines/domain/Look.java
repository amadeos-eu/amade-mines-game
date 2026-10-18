package eu.amadeos.games.mines.domain;

/** What a cell shows; the number of an opened cell comes from {@link Board#count(int, int)}. */
public enum Look {
    /** Not opened yet. */
    HIDDEN,
    FLAG,
    QUESTION,
    /** Opened, no mine: an empty cell or a number. */
    OPEN,
    /** A mine shown after the game was lost. */
    MINE,
    /** The mine that was opened. */
    EXPLODED,
    /** A flag on a cell without a mine, shown after the game was lost. */
    WRONG_FLAG
}
