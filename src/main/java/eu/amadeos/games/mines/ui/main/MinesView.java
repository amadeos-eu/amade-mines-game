package eu.amadeos.games.mines.ui.main;

import eu.amadeos.games.mines.domain.Board;
import eu.amadeos.games.mines.domain.Difficulty;
import eu.amadeos.games.mines.ui.view.Face;

import java.util.Optional;

// --8<-- [start:view]
/** What {@link MinesPresenter} needs from the window; tests replace it. */
public interface MinesView {
    /** Shows a new field; the window takes the size of the field. */
    void showBoard(Board board);

    /** Draws the field again after a move. */
    void refreshBoard();

    void showMinesLeft(int mines);

    void showTime(int seconds);

    void showFace(Face face);

    /** Selects the size and the question-mark switch in the menu. */
    void showOptions(Difficulty difficulty, boolean questionMarks);

    /** Starts or stops the timer that calls {@link MinesPresenter#tick()}. */
    void setTicking(boolean ticking);

    /** Asks for the player's name after a best time; empty if cancelled. */
    Optional<String> askName(String message);

    /**
     * Shows the best times.
     *
     * @param selected  the size whose table is shown first
     * @param highlight the place of a new entry in that table, -1 for none
     */
    void showScores(Difficulty selected, int highlight);

    void showAbout();
}
// --8<-- [end:view]
