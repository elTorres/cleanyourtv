package io.github.eltorres.seguirviendo;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

public class MenuStyleTest {
    private static final Path ADDON = Paths.get("src/main/res");
    private static final Path LAUNCHER = Paths.get("../../../launcher/app/src/main/res/drawable");

    @Test
    public void menuDrawablesMatchTheLauncher() throws IOException {
        assertSame("glass_panel.xml", "drawable/menu_panel.xml");
        assertSame("input_row_bg.xml", "drawable/menu_row.xml");
        assertSame("pill_text.xml", "color/menu_row_text.xml");
    }

    private static void assertSame(String launcherFile, String addonFile) throws IOException {
        assertEquals(addonFile + " differs from the launcher's " + launcherFile,
                read(LAUNCHER.resolve(launcherFile)), read(ADDON.resolve(addonFile)));
    }

    private static String read(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
