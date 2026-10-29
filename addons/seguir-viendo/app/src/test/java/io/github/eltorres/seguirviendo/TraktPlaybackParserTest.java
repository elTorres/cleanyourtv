package io.github.eltorres.seguirviendo;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TraktPlaybackParserTest {
    private static final OpenTarget STREMIO = new OpenTarget("com.stremio.one", null);

    private static String fixture() throws Exception {
        return new String(Files.readAllBytes(Paths.get("src/test/resources/trakt-playback.json")),
                StandardCharsets.UTF_8);
    }

    @Test
    public void pausedEpisodeBecomesContinueItem() throws Exception {
        List<ViewingItem> items = TraktPlayback.parse(fixture(), STREMIO);
        ViewingItem episode = items.get(0);
        assertEquals(ViewingKind.CONTINUE, episode.kind());
        assertEquals("The Bear", episode.title());
        assertEquals("Fishes", episode.episodeTitle());
        assertEquals(Integer.valueOf(2), episode.season());
        assertEquals(Integer.valueOf(6), episode.episode());
        assertEquals(40 * 60_000L, episode.durationMs());
        assertEquals(16 * 60_000L, episode.positionMs());
        assertEquals("episode:900001", episode.itemKey());
        assertEquals("show:800001", episode.showKey());
        assertEquals(1_800_000_000_000L, episode.lastEngagedMs());
    }

    @Test
    public void movieWithoutRuntimeKeepsItsProgress() throws Exception {
        ViewingItem movie = TraktPlayback.parse(fixture(), STREMIO).get(1);
        assertNull(movie.season());
        assertEquals("Dune", movie.title());
        assertEquals(TraktPlayback.UNKNOWN_RUNTIME_MS / 4, movie.positionMs());
    }

    @Test
    public void unknownEntriesAreSkipped() throws Exception {
        assertEquals(2, TraktPlayback.parse(fixture(), STREMIO).size());
    }
}
