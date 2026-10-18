package eu.amadeos.games.mines.ui.main;

import eu.amadeos.games.mines.domain.Board;
import eu.amadeos.games.mines.domain.Difficulty;
import eu.amadeos.games.mines.ui.view.Face;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

// --8<-- [start:fake]
/** Records what the presenter shows and answers its questions from a queue. */
final class FakeView implements MinesView {
    Board board;
    int refreshes;
    int minesLeft;
    int seconds;
    Face face;
    Difficulty difficulty;
    boolean questionMarks;
    boolean ticking;
    final Deque<Optional<String>> names = new ArrayDeque<>();
    String askedWith;
    Difficulty scoresShown;
    int highlight = -2;
    boolean about;

    @Override
    public void showBoard(Board newBoard) {
        board = newBoard;
    }

    @Override
    public void refreshBoard() {
        refreshes++;
    }

    @Override
    public void showMinesLeft(int mines) {
        minesLeft = mines;
    }

    @Override
    public void showTime(int newSeconds) {
        seconds = newSeconds;
    }

    @Override
    public void showFace(Face newFace) {
        face = newFace;
    }

    @Override
    public void showOptions(Difficulty newDifficulty, boolean newQuestionMarks) {
        difficulty = newDifficulty;
        questionMarks = newQuestionMarks;
    }

    @Override
    public void setTicking(boolean on) {
        ticking = on;
    }

    @Override
    public Optional<String> askName(String message) {
        askedWith = message;
        return names.isEmpty() ? Optional.empty() : names.pop();
    }

    @Override
    public void showScores(Difficulty selected, int place) {
        scoresShown = selected;
        highlight = place;
    }

    @Override
    public void showAbout() {
        about = true;
    }
}
// --8<-- [end:fake]
