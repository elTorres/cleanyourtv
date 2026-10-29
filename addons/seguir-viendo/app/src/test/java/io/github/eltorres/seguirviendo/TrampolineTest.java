package io.github.eltorres.seguirviendo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TrampolineTest {

    @Test
    public void opensStremioByDefault() {
        assertEquals("com.stremio.one", Trampoline.targetOrDefault(null));
        assertEquals("com.stremio.one", Trampoline.targetOrDefault(""));
        assertEquals(Trampoline.Action.OPEN_APP, Trampoline.choose(false, true));
    }

    @Test
    public void deepLinkWinsWhenItResolves() {
        assertEquals(Trampoline.Action.OPEN_LINK, Trampoline.choose(true, true));
    }

    @Test
    public void reportsMissingTargetApp() {
        assertEquals(Trampoline.Action.MISSING, Trampoline.choose(false, false));
    }

    @Test
    public void cyclesThroughInstalledTargets() {
        assertEquals(Trampoline.KODI, Trampoline.nextTarget(Trampoline.STREMIO, pkg -> true));
        assertEquals(Trampoline.STREMIO, Trampoline.nextTarget(Trampoline.KODI, pkg -> true));
        assertEquals(Trampoline.STREMIO, Trampoline.nextTarget(Trampoline.STREMIO, Trampoline.STREMIO::equals));
    }
}
