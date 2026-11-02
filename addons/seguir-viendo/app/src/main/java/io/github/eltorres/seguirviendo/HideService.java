package io.github.eltorres.seguirviendo;

import java.util.List;
import java.util.Set;

final class HideService {
    private final WatchNextStore store;
    private final HiddenStore hidden;

    HideService(WatchNextStore store, HiddenStore hidden) {
        this.store = store;
        this.hidden = hidden;
    }

    PublishedRow row(long rowId) {
        for (PublishedRow row : store.published()) {
            if (row.id() == rowId) return row;
        }
        return null;
    }

    List<HideScope> options(PublishedRow row) {
        return HiddenItems.scopesFor(row.season(), row.episode());
    }

    synchronized void hide(PublishedRow row, HideScope scope) {
        hidden.add(HiddenItems.key(row.sourceId(), row.itemKey(), row.showKey(), row.season(), scope));
        Set<String> keys = hidden.keys();
        for (PublishedRow published : store.published()) {
            if (HiddenItems.hides(keys, published)) store.delete(published.id());
        }
    }
}
