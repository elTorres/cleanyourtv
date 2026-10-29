package io.github.eltorres.seguirviendo;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PosterStoreTest {
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3};

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    private static ViewingItem item(String posterKey) {
        return new ViewingItem("trakt", "episode:1", "show:1", ViewingKind.NEXT, "The Bear", null, 2, 7, 0, 0, 1,
                posterKey, new OpenTarget(Trampoline.STREMIO, null));
    }

    @Test
    public void storedPosterIsServedAsContentUri() throws Exception {
        File dir = folder.newFolder("posters");
        FilePosterStore store = new FilePosterStore(dir, PosterProvider.AUTHORITY);
        Posters posters = new Posters(store, (id, type) -> "tv".equals(type) && "136315".equals(id) ? JPEG : null);
        String uri = posters.attach(List.of(item("tmdb:tv:136315"))).get(0).posterUri();
        String id = FilePosterStore.imageId("tmdb:tv:136315");
        assertEquals("content://io.github.eltorres.seguirviendo.posters/" + id, uri);
        assertTrue(id.matches("[0-9a-f]{32}"));
        assertArrayEquals(JPEG, Files.readAllBytes(FilePosterStore.file(dir, id).toPath()));
        assertNull(FilePosterStore.file(dir, "../settings.xml"));
        Posters offline = new Posters(store, (i, t) -> { throw new IOException("offline"); });
        assertEquals(uri, offline.attach(List.of(item("tmdb:tv:136315"))).get(0).posterUri());
    }

    @Test
    public void missingArtworkPublishesWithoutPoster() throws Exception {
        FilePosterStore store = new FilePosterStore(folder.newFolder("posters"), PosterProvider.AUTHORITY);
        Posters none = new Posters(store, (id, type) -> null);
        Posters failing = new Posters(store, (id, type) -> { throw new IOException("offline"); });
        Posters keyless = new Posters(store, null);
        for (Posters posters : List.of(none, failing, keyless)) {
            ViewingItem published = posters.attach(List.of(item("tmdb:tv:1"), item(null))).get(0);
            assertNull(published.posterUri());
            assertFalse(WatchNextMapper.columns(published, published.posterUri())
                    .containsKey(android.media.tv.TvContract.WatchNextPrograms.COLUMN_POSTER_ART_URI));
        }
    }

    @Test
    public void orphanPostersAreRemoved() throws Exception {
        File dir = folder.newFolder("posters");
        FilePosterStore store = new FilePosterStore(dir, PosterProvider.AUTHORITY);
        store.put("tmdb:tv:1", JPEG);
        store.put("tmdb:tv:2", JPEG);
        store.retainOnly(Set.of("tmdb:tv:2"));
        assertNull(store.uriFor("tmdb:tv:1"));
        assertEquals(1, dir.listFiles().length);
    }
}
