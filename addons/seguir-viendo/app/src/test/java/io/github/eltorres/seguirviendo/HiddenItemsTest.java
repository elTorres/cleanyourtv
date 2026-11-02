package io.github.eltorres.seguirviendo;

import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HiddenItemsTest {

    private static ViewingItem episode(String source, String show, int season, int number) {
        String key = "episode:" + show + "-" + season + "-" + number;
        return new ViewingItem(source, key, "show:" + show, ViewingKind.NEXT, "Lupin", null, season, number, 0, 0, 1,
                null, new OpenTarget("com.stremio.one", null));
    }

    private static String hide(ViewingItem item, HideScope scope) {
        return HiddenItems.key(item.sourceId(), item.itemKey(), item.showKey(), item.season(), scope);
    }

    @Test
    public void episodeWithSeasonOffersEpisodeSeasonAndShow() {
        assertEquals(List.of(HideScope.EPISODE, HideScope.SEASON, HideScope.SHOW), HiddenItems.scopesFor(2, 5));
    }

    @Test
    public void episodeWithoutSeasonOffersEpisodeAndShow() {
        assertEquals(List.of(HideScope.EPISODE, HideScope.SHOW), HiddenItems.scopesFor(null, 5));
    }

    @Test
    public void movieOffersOnlyShow() {
        assertEquals(List.of(HideScope.SHOW), HiddenItems.scopesFor(null, null));
    }

    @Test
    public void hiddenEpisodeKeepsNextEpisode() {
        Set<String> keys = Set.of(hide(episode("trakt", "1", 1, 3), HideScope.EPISODE));
        assertTrue(HiddenItems.hides(keys, episode("trakt", "1", 1, 3)));
        assertFalse(HiddenItems.hides(keys, episode("trakt", "1", 1, 4)));
    }

    @Test
    public void hiddenSeasonKeepsOtherSeasons() {
        Set<String> keys = Set.of(hide(episode("trakt", "1", 1, 3), HideScope.SEASON));
        assertTrue(HiddenItems.hides(keys, episode("trakt", "1", 1, 8)));
        assertFalse(HiddenItems.hides(keys, episode("trakt", "1", 2, 1)));
    }

    @Test
    public void hiddenShowHidesEveryEpisode() {
        Set<String> keys = Set.of(hide(episode("trakt", "1", 1, 3), HideScope.SHOW));
        assertTrue(HiddenItems.hides(keys, episode("trakt", "1", 4, 2)));
        assertFalse(HiddenItems.hides(keys, episode("trakt", "2", 1, 3)));
    }

    @Test
    public void sameTitleInAnotherSourceStays() {
        Set<String> keys = Set.of(hide(episode("trakt", "1", 1, 3), HideScope.SHOW));
        assertFalse(HiddenItems.hides(keys, episode("movistar", "1", 1, 3)));
    }

    @Test
    public void publishedRowMatchesLikeItsItem() {
        ViewingItem item = episode("trakt", "1", 1, 3);
        PublishedRow row = new PublishedRow(7, "trakt", item.itemKey(), item.showKey(), "Lupin", 1, 3);
        assertTrue(HiddenItems.hides(Set.of(hide(item, HideScope.SEASON)), row));
    }
}
