package eu.amadeos.games.mines.bootstrap;

import eu.amadeos.commons.app.AmadeApplication;
import eu.amadeos.commons.app.AppInfo;
import eu.amadeos.games.mines.ui.main.MinesPresenter;
import eu.amadeos.games.mines.ui.main.MinesWindow;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public final class AmadeMinesApplication {
    public static final AppInfo APP = new AppInfo("amade-mines-game", "Mines", "mines");

    private AmadeMinesApplication() {
    }

    /** The version from the jar's manifest; "dev" when run from the IDE. */
    public static String version() {
        String version = AmadeMinesApplication.class.getPackage().getImplementationVersion();
        return version == null ? "dev" : version;
    }

    public static void main(String[] args) {
        AmadeApplication.launch(APP, injector -> {
            MinesWindow window = injector.getInstance(MinesWindow.class);
            MinesPresenter presenter = injector.getInstance(MinesPresenter.class);
            window.bind(presenter);
            presenter.start();
            window.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    System.exit(0);
                }
            });
            window.setLocationByPlatform(true);
            window.setVisible(true);
        }, new MinesModule());
    }
}
