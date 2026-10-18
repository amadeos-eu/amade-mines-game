package eu.amadeos.games.mines.domain;

import java.util.function.LongSupplier;

/** Time of one game: starts with the first opened cell, stops when the game ends. */
public final class GameClock {
    /** The counter has three digits. */
    public static final int MAX_SECONDS = 999;

    private final LongSupplier nanoTime;
    private long started = -1;
    private long stopped = -1;

    public GameClock(LongSupplier nanoTime) {
        this.nanoTime = nanoTime;
    }

    public GameClock() {
        this(System::nanoTime);
    }

    public void reset() {
        started = -1;
        stopped = -1;
    }

    public void start() {
        if (started < 0) started = nanoTime.getAsLong();
    }

    public void stop() {
        if (started >= 0 && stopped < 0) stopped = nanoTime.getAsLong();
    }

    public boolean running() {
        return started >= 0 && stopped < 0;
    }

    public long millis() {
        if (started < 0) return 0;
        long end = stopped >= 0 ? stopped : nanoTime.getAsLong();
        return (end - started) / 1_000_000;
    }

    /**
     * Seconds as the counter shows them and the high scores record them: a started second counts, so the
     * counter shows 1 right after the first click, as in the original.
     */
    public int seconds() {
        if (started < 0) return 0;
        return (int) Math.min(MAX_SECONDS, millis() / 1000 + 1);
    }
}
