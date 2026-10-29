package io.github.eltorres.seguirviendo;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TraktProgressParserTest {

    @Test
    public void finishedEpisodeYieldsNextEpisode() throws Exception {
        List<TraktProgress.WatchedShow> shows = TraktProgress.recentShows(Fixtures.read("trakt-watched-shows.json"),
                Fixtures.NOW);
        assertEquals(2, shows.size());
        TraktProgress.WatchedShow severance = shows.get(1);
        ViewingItem next = TraktProgress.next(severance, Fixtures.read("trakt-progress-next.json"), Trampoline.STREMIO);
        assertEquals(ViewingKind.NEXT, next.kind());
        assertEquals("Severance", next.title());
        assertEquals("Woe's Hollow", next.episodeTitle());
        assertEquals(Integer.valueOf(2), next.season());
        assertEquals(Integer.valueOf(4), next.episode());
        assertEquals("show:800002", next.showKey());
        assertEquals(severance.lastWatchedMs, next.lastEngagedMs());
        assertEquals("tmdb:tv:95396", next.posterKey());
        assertEquals("stremio:///detail/series/tt11280740/tt11280740:2:4", next.target().deepLink());
        assertNull(TraktProgress.next(severance, Fixtures.read("trakt-progress-done.json"), Trampoline.STREMIO));
    }

    @Test
    public void continueItemSuppressesNextForSameShow() throws Exception {
        FakeHttp http = new FakeHttp()
                .answer(200, Fixtures.read("trakt-playback.json"))
                .answer(200, Fixtures.read("trakt-watched-shows.json"))
                .answer(200, Fixtures.read("trakt-progress-next.json"));
        TraktSource source = new TraktSource(Fixtures.connectedTrakt(), http, () -> Trampoline.STREMIO,
                () -> Fixtures.NOW);
        List<ViewingItem> items = source.fetch();
        assertEquals(3, items.size());
        long bearRows = items.stream().filter(i -> i.showKey().equals("show:800001")).count();
        assertEquals(1, bearRows);
        assertEquals(ViewingKind.CONTINUE, items.get(0).kind());
        assertEquals(ViewingKind.NEXT, items.get(2).kind());
        assertEquals(3, http.requests.size());
        assertEquals("GET https://api.trakt.tv/shows/800002/progress/watched", http.requests.get(2));
    }
}
