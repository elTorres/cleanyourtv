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
}
