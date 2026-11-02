package io.github.eltorres.seguirviendo;

import java.util.List;
import java.util.Set;

final class HiddenItems {
    private static final String SEPARATOR = "|";

    private HiddenItems() {
    }

    static List<HideScope> scopesFor(Integer season, Integer episode) {
        if (episode == null) return List.of(HideScope.SHOW);
        if (season == null) return List.of(HideScope.EPISODE, HideScope.SHOW);
        return List.of(HideScope.EPISODE, HideScope.SEASON, HideScope.SHOW);
    }

    static String key(String sourceId, String itemKey, String showKey, Integer season, HideScope scope) {
        switch (scope) {
            case EPISODE:
                return sourceId + SEPARATOR + itemKey;
            case SEASON:
                return sourceId + SEPARATOR + showKey + SEPARATOR + season;
            default:
                return sourceId + SEPARATOR + showKey;
        }
    }

    static boolean hides(Set<String> keys, ViewingItem item) {
        return hides(keys, item.sourceId(), item.itemKey(), item.showKey(), item.season());
    }

    static boolean hides(Set<String> keys, PublishedRow row) {
        return hides(keys, row.sourceId(), row.itemKey(), row.showKey(), row.season());
    }

    private static boolean hides(Set<String> keys, String sourceId, String itemKey, String showKey, Integer season) {
        if (keys.isEmpty()) return false;
        if (keys.contains(key(sourceId, itemKey, showKey, season, HideScope.EPISODE))) return true;
        if (showKey != null && keys.contains(key(sourceId, itemKey, showKey, season, HideScope.SHOW))) return true;
        return showKey != null && season != null
                && keys.contains(key(sourceId, itemKey, showKey, season, HideScope.SEASON));
    }
}
