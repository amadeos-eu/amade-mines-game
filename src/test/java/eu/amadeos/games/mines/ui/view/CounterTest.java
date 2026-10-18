package eu.amadeos.games.mines.ui.view;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CounterTest {
    @Test
    void digitsAreSevenSegmentPatterns() {
        assertArrayEquals(new int[]{0x3F, 0x06, 0x5B}, Counter.segments(12));
    }

    @Test
    void negativeValuesStartWithAMinus() {
        assertEquals(0x40, Counter.segments(-7)[0]);
    }

    @Test
    void valuesAreCutToThreeDigits() {
        assertArrayEquals(Counter.segments(999), Counter.segments(5000));
        assertArrayEquals(Counter.segments(-99), Counter.segments(-500));
    }
}
