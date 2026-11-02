package io.github.eltorres.seguirviendo;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MovistarViewingsParserTest {
    private static final String VIEWINGS = Fixtures.read("movistar-viewings.json");
    private static final String SESSION = "{\"token\":\"hz-1\",\"demarcation\":\"0\"}";

    @Test
    public void partlyWatchedEpisodeBecomesContinueItem() throws Exception {
        ViewingItem item = MovistarViewings.parse(VIEWINGS, Fixtures.NOW).get(0);
        assertEquals("movistar", item.sourceId());
        assertEquals("content:9000102", item.itemKey());
        assertEquals("show:9000100", item.showKey());
        assertEquals(ViewingKind.CONTINUE, item.kind());
        assertEquals("Serie de prueba", item.title());
        assertEquals("Segundo episodio", item.episodeTitle());
        assertEquals(Integer.valueOf(3), item.season());
        assertEquals(Integer.valueOf(2), item.episode());
        assertEquals(1_200_000, item.positionMs());
        assertEquals(2_400_000, item.durationMs());
        assertEquals(Fixtures.NOW, item.lastEngagedMs());
    }

    @Test
    public void trackedSeriesWithoutPositionIsTheNextEpisode() throws Exception {
        ViewingItem item = MovistarViewings.parse(VIEWINGS, Fixtures.NOW).get(1);
        assertEquals(ViewingKind.NEXT, item.kind());
        assertEquals(0, item.positionMs());
        assertNull(item.season());
        assertEquals(Integer.valueOf(5), item.episode());
        assertEquals(Fixtures.NOW - MovistarViewings.ORDER_STEP_MS, item.lastEngagedMs());
    }

    @Test
    public void seenEpisodesAreLeftOut() throws Exception {
        List<ViewingItem> items = MovistarViewings.parse(VIEWINGS, Fixtures.NOW);
        assertEquals(2, items.size());
        assertTrue(items.stream().noneMatch(item -> item.itemKey().equals("content:9000302")));
    }

    @Test
    public void opensTheFichaInTheOfficialApp() throws Exception {
        ViewingItem item = MovistarViewings.parse(VIEWINGS, Fixtures.NOW).get(0);
        assertEquals("com.movistarplus.androidtv", item.target().targetPackage());
        assertEquals("appmovistar://movistarplus.es/ficha?id=9000102", item.target().deepLink());
    }

    @Test
    public void wideImageFromTheMovistarCdnIsThePoster() throws Exception {
        ViewingItem item = MovistarViewings.parse(VIEWINGS, Fixtures.NOW).get(0);
        assertEquals("movistar:image:https://estatico.emisiondof6.com/recorte/n/bigtv_horizontal/F9000001.jpg",
                item.posterKey());
        assertNull(MovistarViewings.posterRef("https://example.com/F1.jpg"));
        FakeHttp http = new FakeHttp().image(new byte[]{1});
        MovistarArtwork artwork = new MovistarArtwork(http);
        assertNull(artwork.poster("https://example.com/F1.jpg", MovistarViewings.IMAGE));
        assertEquals(1, artwork.poster("https://estatico.emisiondof6.com/recorte/n/x/F1.jpg", MovistarViewings.IMAGE).length);
    }

    @Test
    public void readsSelectedProfileOnly() throws Exception {
        MemorySecretStore secrets = connected();
        secrets.put(MovistarAuth.PROFILE_ID, "2");
        FakeHttp http = new FakeHttp().answer(200, SESSION).answer(200, VIEWINGS);
        MovistarAuth auth = new MovistarAuth(http, secrets, () -> Fixtures.NOW);
        List<ViewingItem> items = new MovistarSource(auth, http, () -> Fixtures.NOW).fetch();
        assertEquals(2, items.size());
        assertTrue(http.bodies.get(0).contains("\"userProfile\":\"2\""));
        assertEquals("hz-1", http.headers.get(1).get("X-Hzid"));
        assertEquals("Bearer jwt", http.headers.get(1).get("Authorization"));
        String url = http.requests.get(1);
        assertTrue(url.startsWith("GET https://perso.dof6.com/movistarplus/amazon.tv/users/enc-user/viewings?profile=OTT"));
        assertTrue(url.contains("container=trackedseries"));
        assertTrue(url.contains("accountNumber=00000001"));
    }

    @Test
    public void showsAtMostThreeEntries() throws Exception {
        org.json.JSONObject viewings = new org.json.JSONObject(VIEWINGS);
        org.json.JSONArray contents = viewings.getJSONArray("Contenidos");
        org.json.JSONObject template = contents.getJSONObject(1);
        for (int i = 0; i < 4; i++) {
            org.json.JSONObject copy = new org.json.JSONObject(template.toString());
            copy.getJSONObject("DatosEditoriales").put("Id", 9100000 + i);
            contents.put(copy);
        }
        FakeHttp http = new FakeHttp().answer(200, SESSION).answer(200, viewings.toString());
        MovistarAuth auth = new MovistarAuth(http, connected(), () -> Fixtures.NOW);
        List<ViewingItem> items = new MovistarSource(auth, http, () -> Fixtures.NOW).fetch();
        assertEquals(MovistarSource.MAX_ITEMS, items.size());
        assertEquals("content:9000102", items.get(0).itemKey());
    }

    @Test
    public void refusedViewingsKeepTheSourceConnected() throws Exception {
        MemorySecretStore secrets = connected();
        FakeHttp http = new FakeHttp().answer(200, SESSION).answer(401, "{\"Message\":\"Unauthorized\"}");
        MovistarAuth auth = new MovistarAuth(http, secrets, () -> Fixtures.NOW);
        try {
            new MovistarSource(auth, http, () -> Fixtures.NOW).fetch();
            throw new AssertionError("a refused answer must throw");
        } catch (AuthException expected) {
            assertEquals(SourceState.CONNECTED, auth.state());
            assertNull(secrets.get(MovistarAuth.EXPIRES_AT));
        }
    }

    private static MemorySecretStore connected() {
        MemorySecretStore secrets = new MemorySecretStore();
        secrets.put(MovistarAuth.USERNAME, "persona@example.com");
        secrets.put(MovistarAuth.PASSWORD, "clave");
        secrets.put(MovistarAuth.ACCESS_TOKEN, "jwt");
        secrets.put(MovistarAuth.EXPIRES_AT, Long.toString(Long.MAX_VALUE));
        secrets.put(MovistarAuth.ACCOUNT, "00000001");
        secrets.put(MovistarAuth.PLATFORM, "OTT");
        secrets.put(MovistarAuth.USER, "enc-user");
        secrets.put(MovistarAuth.DEVICE_ID, "dev-1");
        return secrets;
    }
}
