package eu.amadeos.games.mines.ui.main;

import eu.amadeos.commons.i18n.Language;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.commons.i18n.Mnemonics;
import eu.amadeos.commons.i18n.TranslationCheck;
import eu.amadeos.gameengine.swing.GameTexts;
import eu.amadeos.games.mines.bootstrap.MinesModule;
import eu.amadeos.games.mines.domain.Difficulty;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TranslationTest {
    @Test
    void everyLanguageHasEveryText() {
        assertEquals(Map.of(), TranslationCheck.problems(MinesModule.class, MinesModule.MESSAGES));
    }

    @Test
    void textsAreFormattedInTheLanguage() {
        Messages german = Messages.load(MinesModule.class, MinesModule.MESSAGES, Locale.GERMANY);
        assertEquals("Sie haben Profis in 345 Sekunden geräumt.", german.format("scores.won", "Profis", 345));
        Messages french = Messages.load(MinesModule.class, MinesModule.MESSAGES, Locale.FRANCE);
        assertEquals("Trouvez toutes les mines sans marcher sur aucune.", french.get("dialog.about.text"));
    }

    /** The Game menu mixes the engine's entries with the game's own; no two may share a letter. */
    @Test
    void mnemonicsAreUniqueInTheGameMenu() {
        Map<String, String> clashes = new HashMap<>();
        for (Language language : Language.values()) {
            Messages engine = GameTexts.load(language.locale());
            Messages game = Messages.load(MinesModule.class, MinesModule.MESSAGES, language.locale());
            List<String> texts = new ArrayList<>(List.of(engine.get("menu.game.new"), engine.get("menu.game.scores"),
                    engine.get("menu.game.exit"), game.get("menu.game.marks")));
            for (Difficulty d : Difficulty.values()) texts.add(game.get(MinesPresenter.difficultyKey(d)));
            Map<Character, String> seen = new HashMap<>();
            for (String text : texts) {
                Mnemonics.Text t = Mnemonics.parse(text);
                if (t.index() < 0) {
                    clashes.put(language.code() + " " + text, "no mnemonic");
                    continue;
                }
                String other = seen.put(Character.toLowerCase(t.text().charAt(t.index())), text);
                if (other != null) clashes.put(language.code() + " " + text, other);
            }
        }
        assertEquals(Map.of(), clashes);
    }
}
