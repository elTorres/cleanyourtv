package com.gathin.tvhome;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** The Continue watching row: off in config.xml, switched on or off on the TV from Launcher settings. */
public class ContinueWatchingConfigTest {

    private static String read(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }

    @Test
    public void offByDefault() throws Exception {
        assertTrue(read("src/main/res/values/config.xml").contains("<bool name=\"continue_watching\">false</bool>"));
        assertFalse(ContinueWatching.enabled(false, null));
    }

    @Test
    public void choiceOnTvWinsOverConfig() {
        assertTrue(ContinueWatching.enabled(false, Boolean.TRUE));
        assertFalse(ContinueWatching.enabled(true, Boolean.FALSE));
        assertTrue(ContinueWatching.enabled(true, null));
    }

    @Test
    public void themeSyncKeepsTheChoice() throws Exception {
        String config = read("src/main/java/com/gathin/tvhome/Config.java");
        String sync = config.substring(config.indexOf("private static void syncThemeChoices"));
        sync = sync.substring(0, sync.indexOf("\n    }\n"));
        assertFalse(sync.contains(ContinueWatching.CHOICE_KEY));
        assertFalse(sync.contains("clear()"));
    }
}
