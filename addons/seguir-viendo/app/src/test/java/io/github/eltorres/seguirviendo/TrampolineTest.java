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

    @Test
    public void opensOfficialMovistarApp() throws Exception {
        OpenTarget target = MovistarViewings.parse(Fixtures.read("movistar-viewings.json"), Fixtures.NOW).get(0).target();
        assertEquals(MovistarViewings.APP, target.targetPackage());
        String intent = WatchNextMapper.intentUri(target);
        assertEquals("intent:#Intent;component=io.github.eltorres.seguirviendo/.TrampolineActivity"
                + ";S.target=com.movistarplus.androidtv"
                + ";S.link=appmovistar%3A%2F%2Fmovistarplus.es%2Fficha%3Fid%3D9000102;end", intent);
        assertEquals(Trampoline.Action.OPEN_LINK, Trampoline.choose(true, true));
    }
}
