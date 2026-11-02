package io.github.eltorres.seguirviendo;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class HideServiceTest {

    private static ViewingItem episode(String source, String show, int season, int number) {
        return new ViewingItem(source, "episode:" + show + "-" + season + "-" + number, "show:" + show,
                ViewingKind.NEXT, "Lupin", null, season, number, 0, 0, 1, null,
                new OpenTarget("com.stremio.one", null));
    }

    private static List<String> keys(MemoryWatchNextStore store) {
        List<String> keys = new ArrayList<>();
        for (PublishedRow row : store.published()) keys.add(row.sourceId() + ":" + row.itemKey());
        return keys;
    }

    @Test
    public void hidingRemovesEveryMatchingRow() {
        MemoryWatchNextStore store = new MemoryWatchNextStore();
        store.replaceSource("trakt", List.of(episode("trakt", "1", 1, 3), episode("trakt", "1", 2, 1),
                episode("trakt", "2", 1, 1)));
        store.replaceSource("movistar", List.of(episode("movistar", "1", 1, 3)));
        MemoryHiddenStore hidden = new MemoryHiddenStore();
        HideService hide = new HideService(store, hidden);
        hide.hide(store.published().get(0), HideScope.SHOW);
        assertEquals(List.of("trakt:episode:2-1-1", "movistar:episode:1-1-3"), keys(store));
        assertEquals(1, hidden.keys.size());
    }

    @Test
    public void optionsFollowTheRow() {
        MemoryWatchNextStore store = new MemoryWatchNextStore();
        store.replaceSource("trakt", List.of(episode("trakt", "1", 2, 5)));
        HideService hide = new HideService(store, new MemoryHiddenStore());
        PublishedRow row = hide.row(store.published().get(0).id());
        assertEquals(List.of(HideScope.EPISODE, HideScope.SEASON, HideScope.SHOW), hide.options(row));
    }

    @Test
    public void unknownRowOffersNothing() {
        MemoryWatchNextStore store = new MemoryWatchNextStore();
        store.replaceSource("trakt", List.of(episode("trakt", "1", 1, 3)));
        assertNull(new HideService(store, new MemoryHiddenStore()).row(404));
    }

    @Test
    public void hidingNeverTouchesTheServices() throws Exception {
        for (String file : List.of("HideService.java", "OptionsActivity.java")) {
            String source = new String(Files.readAllBytes(Paths.get(
                    "src/main/java/io/github/eltorres/seguirviendo/" + file)), StandardCharsets.UTF_8);
            assertFalse(file, source.contains("Http"));
            assertFalse(file, source.contains("TraktAuth"));
            assertFalse(file, source.contains("MovistarAuth"));
        }
    }

    @Test
    public void restoreAllEmptiesTheList() {
        MemoryWatchNextStore store = new MemoryWatchNextStore();
        store.replaceSource("trakt", List.of(episode("trakt", "1", 1, 3), episode("trakt", "2", 1, 1)));
        MemoryHiddenStore hidden = new MemoryHiddenStore();
        HideService hide = new HideService(store, hidden);
        hide.hide(store.published().get(0), HideScope.EPISODE);
        hide.hide(store.published().get(0), HideScope.SHOW);
        assertEquals(2, hide.hiddenCount());
        hide.restoreAll();
        assertEquals(0, hide.hiddenCount());
        assertFalse(HiddenItems.hides(hidden.keys(), episode("trakt", "1", 1, 3)));
    }

    @Test
    public void countIsZeroWithoutHiddenItems() {
        assertEquals(0, new HideService(new MemoryWatchNextStore(), new MemoryHiddenStore()).hiddenCount());
    }
}
