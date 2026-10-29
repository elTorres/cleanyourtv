package io.github.eltorres.seguirviendo;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TmdbArtworkTest {
    private static final byte[] IMAGE = {1, 2, 3};

    @Test
    public void prefersTheWideBackdropForTheRowCard() throws Exception {
        FakeHttp http = new FakeHttp()
                .answer(200, "{\"backdrop_path\":\"/wide.jpg\",\"poster_path\":\"/tall.jpg\"}")
                .image(IMAGE);
        assertArrayEquals(IMAGE, new TmdbArtwork(http, "key", "es-ES").poster("136315", Posters.TV));
        assertEquals("GET https://api.themoviedb.org/3/tv/136315?language=es-ES&api_key=key", http.requests.get(0));
        assertEquals("GET https://image.tmdb.org/t/p/w300/wide.jpg", http.requests.get(1));
    }

    @Test
    public void readAccessTokenGoesInTheHeaderNotTheUrl() throws Exception {
        FakeHttp http = new FakeHttp().answer(200, "{\"backdrop_path\":null,\"poster_path\":\"/tall.jpg\"}").image(IMAGE);
        new TmdbArtwork(http, "eyJtoken", "es-ES").poster("438631", Posters.MOVIE);
        assertEquals("GET https://api.themoviedb.org/3/movie/438631?language=es-ES", http.requests.get(0));
        assertTrue(http.requests.get(1).endsWith("/tall.jpg"));
    }

    @Test
    public void unknownShowHasNoArtwork() throws Exception {
        FakeHttp http = new FakeHttp().answer(404, "{}");
        assertNull(new TmdbArtwork(http, "key", "es-ES").poster("1", Posters.TV));
        assertNull(new TmdbArtwork(new FakeHttp(), "key", "es-ES").poster("1/../x", Posters.TV));
        assertNull(TmdbArtwork.orNull(http, "", "es-ES"));
    }
}
