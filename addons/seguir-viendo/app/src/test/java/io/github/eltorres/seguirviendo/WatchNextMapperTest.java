package io.github.eltorres.seguirviendo;

import android.media.tv.TvContract.WatchNextPrograms;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WatchNextMapperTest {
    private static ViewingItem episode(String link) {
        return new ViewingItem("trakt", "episode:1", "show:1", ViewingKind.CONTINUE, "The Bear", "Fishes", 2, 6,
                960_000, 2_400_000, 1_800_000_000_000L, null, new OpenTarget("com.stremio.one", link));
    }

    @Test
    public void mapsContinueItemWithPositionAndDuration() {
        Map<String, Object> row = WatchNextMapper.columns(episode(null), null);
        assertEquals(WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE, row.get(WatchNextPrograms.COLUMN_WATCH_NEXT_TYPE));
        assertEquals(960_000L, row.get(WatchNextPrograms.COLUMN_LAST_PLAYBACK_POSITION_MILLIS));
        assertEquals(2_400_000L, row.get(WatchNextPrograms.COLUMN_DURATION_MILLIS));
        assertEquals("2", row.get(WatchNextPrograms.COLUMN_SEASON_DISPLAY_NUMBER));
        assertEquals("6", row.get(WatchNextPrograms.COLUMN_EPISODE_DISPLAY_NUMBER));
        assertEquals("trakt:episode:1", row.get(WatchNextPrograms.COLUMN_INTERNAL_PROVIDER_ID));
    }

    @Test
    public void marksRowsSearchable() {
        assertEquals(1, WatchNextMapper.columns(episode(null), null).get(WatchNextPrograms.COLUMN_SEARCHABLE));
    }

    @Test
    public void targetsOwnTrampoline() {
        String uri = (String) WatchNextMapper.columns(episode("stremio:///detail/series/tt1:2:6"), null)
                .get(WatchNextPrograms.COLUMN_INTENT_URI);
        assertTrue(uri.startsWith("intent:#Intent;component=io.github.eltorres.seguirviendo/.TrampolineActivity;"));
        assertTrue(uri.contains(";S.target=com.stremio.one;"));
        assertTrue(uri.contains(";S.link=stremio%3A%2F%2F%2Fdetail%2Fseries%2Ftt1%3A2%3A6;"));
        assertTrue(uri.endsWith(";end"));
    }

    @Test
    public void ownRowsAreRecognisedBySource() {
        assertTrue(WatchNextMapper.belongsTo("trakt:episode:1", "trakt"));
        assertFalse(WatchNextMapper.belongsTo("movistar:1", "trakt"));
        assertFalse(WatchNextMapper.belongsTo(null, "trakt"));
    }

    @Test
    public void storesShowKeyInProviderData() {
        ViewingItem item = episode(null);
        byte[] data = (byte[]) WatchNextMapper.columns(item, null).get(WatchNextPrograms.COLUMN_INTERNAL_PROVIDER_DATA);
        PublishedRow row = WatchNextMapper.published(9, WatchNextMapper.providerId(item), data, item.title());
        assertEquals(9, row.id());
        assertEquals("trakt", row.sourceId());
        assertEquals("episode:1", row.itemKey());
        assertEquals("show:1", row.showKey());
        assertEquals(Integer.valueOf(2), row.season());
        assertEquals(Integer.valueOf(6), row.episode());
    }

    @Test
    public void episodeWithoutSeasonKeepsItsNumber() {
        ViewingItem item = new ViewingItem("movistar", "content:4", "show:9", ViewingKind.NEXT, "Lupin", null, null, 4,
                0, 0, 1, null, new OpenTarget("com.movistarplus.androidtv", null));
        PublishedRow row = WatchNextMapper.published(1, WatchNextMapper.providerId(item),
                WatchNextMapper.providerData(item), item.title());
        assertEquals(null, row.season());
        assertEquals(Integer.valueOf(4), row.episode());
    }

    @Test
    public void rowsFromBeforeTheShowKeyAreSkipped() {
        assertEquals(null, WatchNextMapper.published(1, "trakt:episode:1", null, "The Bear"));
    }
}
