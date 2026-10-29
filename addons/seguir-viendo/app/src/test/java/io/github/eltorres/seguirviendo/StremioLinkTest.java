package io.github.eltorres.seguirviendo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class StremioLinkTest {

    @Test
    public void buildsEpisodeDeepLink() {
        assertEquals("stremio:///detail/series/tt1475582/tt1475582:1:1", StremioLink.episode("tt1475582", 1, 1));
        assertEquals("stremio:///detail/movie/tt0111161", StremioLink.movie("tt0111161"));
        assertNull(StremioLink.episode(null, 1, 1));
        assertNull(StremioLink.movie("tt0111161/../x"));
    }

    @Test
    public void linksOnlyForStremio() {
        String link = StremioLink.movie("tt0111161");
        assertEquals(link, StremioLink.target(Trampoline.STREMIO, link).deepLink());
        assertNull(StremioLink.target(Trampoline.KODI, link).deepLink());
        assertEquals(Trampoline.KODI, StremioLink.target(Trampoline.KODI, link).targetPackage());
    }
}
