package org.jabref.testutils.interactive.styletester;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.jabref.gui.preferences.JabRefGuiPreferences;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.util.DefaultFileUpdateMonitor;
import org.jabref.logic.JabRefException;
import org.jabref.logic.util.HeadlessExecutorService;

/**
 * Useful for checking the display of different controls. Not needed inside of JabRef.
 */
public class StyleTesterMain extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws JabRefException {
        StyleTesterView view = new StyleTesterView();
        DefaultFileUpdateMonitor fileUpdateMonitor = new DefaultFileUpdateMonitor();
        HeadlessExecutorService.INSTANCE.executeInterruptableTask(fileUpdateMonitor, "FileUpdateMonitor");
        ThemeManager themeManager = new ThemeManager(
                JabRefGuiPreferences.getInstance().getWorkspacePreferences(),
                fileUpdateMonitor,
                Runnable::run);

        Scene scene = new Scene(view.getContent());
        themeManager.installCss(scene);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        HeadlessExecutorService.INSTANCE.shutdownEverything();
    }
}
