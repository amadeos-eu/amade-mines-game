package eu.amadeos.games.mines.repository;

import com.google.inject.Inject;
import eu.amadeos.commons.config.AppDirectories;
import eu.amadeos.commons.config.PropertiesFile;
import eu.amadeos.games.mines.domain.Difficulty;

import java.nio.file.Path;
import java.util.Properties;

/** The field size and the question-mark switch, in {@code ~/.config/amadeos/mines/mines.properties}. */
public final class MinesSettings {
    private static final String DIFFICULTY = "difficulty";
    private static final String QUESTION_MARKS = "question-marks";

    private final Path file;

    @Inject
    public MinesSettings(AppDirectories directories) {
        this(directories.configFile("mines.properties"));
    }

    public MinesSettings(Path file) {
        this.file = file;
    }

    public Difficulty difficulty() {
        return Difficulty.ofId(PropertiesFile.load(file).getProperty(DIFFICULTY, "")).orElse(Difficulty.BEGINNER);
    }

    public void setDifficulty(Difficulty difficulty) {
        update(DIFFICULTY, difficulty.id());
    }

    public boolean questionMarks() {
        return Boolean.parseBoolean(PropertiesFile.load(file).getProperty(QUESTION_MARKS, "true"));
    }

    public void setQuestionMarks(boolean on) {
        update(QUESTION_MARKS, Boolean.toString(on));
    }

    private void update(String key, String value) {
        Properties properties = PropertiesFile.load(file);
        properties.setProperty(key, value);
        PropertiesFile.store(file, properties, "amade-mines-game settings");
    }
}
