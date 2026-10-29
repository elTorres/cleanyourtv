package io.github.eltorres.seguirviendo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class MemoryWatchNextStore implements WatchNextStore {
    final Map<String, List<ViewingItem>> rows = new LinkedHashMap<>();

    @Override
    public void replaceSource(String sourceId, List<ViewingItem> items) {
        rows.put(sourceId, new ArrayList<>(items));
    }

    @Override
    public void clearSource(String sourceId) {
        rows.remove(sourceId);
    }
}
