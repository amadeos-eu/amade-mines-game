package eu.amadeos.games.mines.bootstrap;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import eu.amadeos.commons.app.AppInfo;
import eu.amadeos.commons.config.AppDirectories;
import eu.amadeos.commons.config.XdgDirectories;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.score.HighScores;
import eu.amadeos.gameengine.score.PlayerName;
import eu.amadeos.gameengine.score.Ranking;
import eu.amadeos.games.mines.domain.GameClock;
import eu.amadeos.games.mines.repository.MinesSettings;
import eu.amadeos.games.mines.ui.main.MinesPresenter;
import eu.amadeos.games.mines.ui.main.MinesView;
import eu.amadeos.games.mines.ui.main.MinesWindow;

import java.util.Locale;
import java.util.random.RandomGenerator;

/**
 * amade-mines-game bindings. Theme, language and the XDG directories come from
 * {@link eu.amadeos.commons.app.CommonsModule}.
 */
public final class MinesModule extends AbstractModule {
    /** The texts, {@code messages.properties} and its translations. */
    public static final String MESSAGES = "eu.amadeos.games.mines.messages";

    @Override
    protected void configure() {
        bind(MinesSettings.class).in(Scopes.SINGLETON);
        bind(MinesWindow.class).in(Scopes.SINGLETON);
        bind(MinesView.class).to(MinesWindow.class);
        bind(MinesPresenter.class).in(Scopes.SINGLETON);
        bind(GameClock.class).toInstance(new GameClock());
    }

    // --8<-- [start:high-scores]
    /** Best times per field size in {@code ~/.local/share/amadeos/games/mines}. */
    @Provides
    @Singleton
    HighScores highScores(XdgDirectories xdg, AppInfo app) {
        return HighScores.of(xdg, app.configName(), Ranking.LOWER_WINS);
    }
    // --8<-- [end:high-scores]

    @Provides
    @Singleton
    PlayerName playerName(AppDirectories directories) {
        return new PlayerName(directories.config(), System.getProperty("user.name"));
    }

    @Provides
    RandomGenerator random() {
        return RandomGenerator.getDefault();
    }

    /** The texts in the language {@code AmadeApplication} chose. */
    @Provides
    @Singleton
    Messages messages(Locale locale) {
        return Messages.load(MinesModule.class, MESSAGES, locale);
    }
}
