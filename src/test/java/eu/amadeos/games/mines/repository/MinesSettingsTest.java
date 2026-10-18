package eu.amadeos.games.mines.repository;

import eu.amadeos.games.mines.domain.Difficulty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinesSettingsTest {
    @TempDir
    Path dir;

    @Test
    void beginnerWithQuestionMarksAtFirst() {
        MinesSettings settings = new MinesSettings(dir.resolve("mines.properties"));
        assertEquals(Difficulty.BEGINNER, settings.difficulty());
        assertTrue(settings.questionMarks());
    }

    @Test
    void remembersTheChoices() {
        Path file = dir.resolve("mines.properties");
        MinesSettings settings = new MinesSettings(file);
        settings.setDifficulty(Difficulty.EXPERT);
        settings.setQuestionMarks(false);
        MinesSettings again = new MinesSettings(file);
        assertEquals(Difficulty.EXPERT, again.difficulty());
        assertFalse(again.questionMarks());
    }

    @Test
    void anUnknownSizeFallsBackToBeginner() throws Exception {
        Path file = dir.resolve("mines.properties");
        Files.writeString(file, "difficulty=huge\n");
        assertEquals(Difficulty.BEGINNER, new MinesSettings(file).difficulty());
    }
}
