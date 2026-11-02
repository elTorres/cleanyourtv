package io.github.eltorres.seguirviendo;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class MemoryWatchNextStore implements WatchNextStore {
    final Map<String, List<ViewingItem>> rows = new LinkedHashMap<>();
    private final Map<ViewingItem, Long> ids = new IdentityHashMap<>();

    @Override
    public void replaceSource(String sourceId, List<ViewingItem> items) {
        rows.put(sourceId, new ArrayList<>(items));
    }

    @Override
    public void clearSource(String sourceId) {
        rows.remove(sourceId);
    }

    @Override
    public List<PublishedRow> published() {
        List<PublishedRow> published = new ArrayList<>();
        for (List<ViewingItem> items : rows.values()) {
            for (ViewingItem item : items) {
                published.add(WatchNextMapper.published(id(item), WatchNextMapper.providerId(item),
                        WatchNextMapper.providerData(item), item.title()));
            }
        }
        return published;
    }

    @Override
    public void delete(long rowId) {
        for (List<ViewingItem> items : rows.values()) items.removeIf(item -> id(item) == rowId);
    }

    long id(ViewingItem item) {
        return ids.computeIfAbsent(item, key -> (long) ids.size() + 1);
    }
}
