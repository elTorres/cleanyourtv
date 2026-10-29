package io.github.eltorres.seguirviendo;

import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SourcesServiceTest {

    private static final class MemoryTargets implements Targets {
        final Map<String, String> chosen = new HashMap<>();

        @Override
        public String targetPackage(String sourceId) {
            return Trampoline.targetOrDefault(chosen.get(sourceId));
        }

        @Override
        public void setTargetPackage(String sourceId, String packageName) {
            chosen.put(sourceId, packageName);
        }
    }

    @Test
    public void disconnectRemovesSecretsAndRows() {
        MemorySecretStore secrets = new MemorySecretStore();
        secrets.put(TraktAuth.ACCESS_TOKEN, "access");
        secrets.put(TraktAuth.REFRESH_TOKEN, "refresh");
        TraktAuth auth = new TraktAuth(new FakeHttp(), secrets, "client", "", "http://127.0.0.1", () -> 0L);
        MemoryWatchNextStore store = new MemoryWatchNextStore();
        ViewingItem row = new ViewingItem("trakt", "episode:1", "show:1", ViewingKind.CONTINUE, "The Bear", null,
                2, 6, 1, 2, 3, null, new OpenTarget(Trampoline.STREMIO, null));
        store.rows.put("trakt", List.of(row));
        store.rows.put("movistar", List.of(row));
        SourcesService sources = new SourcesService(Map.of("trakt", auth::disconnect), store, new MemoryTargets());
        sources.disconnect("trakt");
        assertTrue(secrets.values.isEmpty());
        assertEquals(SourceState.DISCONNECTED, auth.state());
        assertFalse(store.rows.containsKey("trakt"));
        assertTrue(store.rows.containsKey("movistar"));
    }

    @Test
    public void targetChoiceIsKeptPerSource() {
        MemoryTargets targets = new MemoryTargets();
        SourcesService sources = new SourcesService(Map.of(), new MemoryWatchNextStore(), targets);
        sources.setTargetPackage("trakt", Trampoline.KODI);
        assertEquals(Trampoline.KODI, targets.targetPackage("trakt"));
        assertEquals(Trampoline.STREMIO, targets.targetPackage("movistar"));
    }
}
