package io.github.eltorres.seguirviendo;

import java.util.List;

interface WatchNextStore {
    void replaceSource(String sourceId, List<ViewingItem> items);

    void clearSource(String sourceId);
}
