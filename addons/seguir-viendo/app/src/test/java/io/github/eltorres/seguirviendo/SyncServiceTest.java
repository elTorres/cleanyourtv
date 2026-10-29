package io.github.eltorres.seguirviendo;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SyncServiceTest {

    private static final class MemoryStore implements WatchNextStore {
        final Map<String, List<ViewingItem>> rows = new LinkedHashMap<>();

        @Override
        public void replaceSource(String sourceId, List<ViewingItem> items) {
            rows.put(sourceId, new ArrayList<>(items));
        }

        @Override
        public void clearSource(String sourceId) {
            rows.remove(sourceId);
        }
    }

    private interface Fetch {
        List<ViewingItem> run() throws IOException, AuthException;
    }

    private static ViewingSource source(String id, Fetch fetch) {
        return new ViewingSource() {
            @Override public String id() { return id; }
            @Override public SourceState state() { return SourceState.CONNECTED; }
            @Override public List<ViewingItem> fetch() throws IOException, AuthException { return fetch.run(); }
        };
    }

    private static ViewingItem item(String source, String key) {
        return new ViewingItem(source, key, key, ViewingKind.CONTINUE, key, null, null, null, 1, 2, 3, null,
                new OpenTarget("com.stremio.one", null));
    }

    @Test
    public void replacesOnlyTheSyncedSource() {
        MemoryStore store = new MemoryStore();
        store.rows.put("trakt", List.of(item("trakt", "old")));
        store.rows.put("movistar", List.of(item("movistar", "kept")));
        SyncService sync = new SyncService(List.of(source("trakt", () -> List.of(item("trakt", "new")))),
                store, () -> 42L);
        SyncReport report = sync.sync("trakt");
        assertEquals(SyncReport.Outcome.UPDATED, report.outcome("trakt"));
        assertEquals("new", store.rows.get("trakt").get(0).itemKey());
        assertEquals("kept", store.rows.get("movistar").get(0).itemKey());
        assertEquals(42L, report.finishedAtMs());
    }

    @Test
    public void keepsPreviousRowsWhenSourceFails() {
        MemoryStore store = new MemoryStore();
        store.rows.put("trakt", List.of(item("trakt", "previous")));
        SyncService sync = new SyncService(List.of(
                source("trakt", () -> { throw new IOException("offline"); }),
                source("movistar", () -> List.of(item("movistar", "fresh")))), store, () -> 1L);
        SyncReport report = sync.syncAll();
        assertEquals(SyncReport.Outcome.NETWORK_ERROR, report.outcome("trakt"));
        assertEquals("previous", store.rows.get("trakt").get(0).itemKey());
        assertEquals("fresh", store.rows.get("movistar").get(0).itemKey());
    }

    @Test
    public void authFailureMarksReconnectNeededAndKeepsRows() {
        MemorySecretStore secrets = new MemorySecretStore();
        secrets.put(TraktAuth.ACCESS_TOKEN, "revoked");
        secrets.put(TraktAuth.REFRESH_TOKEN, "refresh");
        secrets.put(TraktAuth.EXPIRES_AT, Long.toString(Long.MAX_VALUE));
        TraktAuth auth = new TraktAuth(new FakeHttp(), secrets, "client", "secret", "http://127.0.0.1", () -> 0L);
        FakeHttp http = new FakeHttp().answer(401, "");
        MemoryStore store = new MemoryStore();
        store.rows.put("trakt", List.of(item("trakt", "previous")));
        SyncService sync = new SyncService(List.of(new TraktSource(auth, http, () -> "com.stremio.one")),
                store, () -> 1L);
        assertEquals(SyncReport.Outcome.RECONNECT_NEEDED, sync.syncAll().outcome("trakt"));
        assertEquals(SourceState.RECONNECT_NEEDED, auth.state());
        assertEquals("previous", store.rows.get("trakt").get(0).itemKey());
        assertTrue(http.requests.get(0).endsWith("/sync/playback?extended=full"));
    }
}
