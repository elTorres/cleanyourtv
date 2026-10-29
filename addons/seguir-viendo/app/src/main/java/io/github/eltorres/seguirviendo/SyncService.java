package io.github.eltorres.seguirviendo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;

final class SyncService {
    private final List<ViewingSource> sources;
    private final WatchNextStore store;
    private final Posters posters;
    private final LongSupplier clock;

    SyncService(List<ViewingSource> sources, WatchNextStore store, Posters posters, LongSupplier clock) {
        this.sources = sources;
        this.store = store;
        this.posters = posters;
        this.clock = clock;
    }

    synchronized SyncReport syncAll() {
        Map<String, SyncReport.Outcome> outcomes = new LinkedHashMap<>();
        List<ViewingItem> published = new ArrayList<>();
        boolean complete = true;
        for (ViewingSource source : sources) {
            SyncReport.Outcome outcome = syncOne(source, published);
            outcomes.put(source.id(), outcome);
            complete &= outcome == SyncReport.Outcome.UPDATED || outcome == SyncReport.Outcome.SKIPPED;
        }
        if (complete) posters.retainOnly(published);
        return new SyncReport(outcomes, clock.getAsLong());
    }

    synchronized SyncReport sync(String sourceId) {
        Map<String, SyncReport.Outcome> outcomes = new LinkedHashMap<>();
        for (ViewingSource source : sources) {
            if (source.id().equals(sourceId)) outcomes.put(source.id(), syncOne(source, new ArrayList<>()));
        }
        return new SyncReport(outcomes, clock.getAsLong());
    }

    private SyncReport.Outcome syncOne(ViewingSource source, List<ViewingItem> published) {
        if (source.state() != SourceState.CONNECTED) return SyncReport.Outcome.SKIPPED;
        try {
            List<ViewingItem> items = posters.attach(source.fetch());
            store.replaceSource(source.id(), items);
            published.addAll(items);
            return SyncReport.Outcome.UPDATED;
        } catch (AuthException e) {
            return SyncReport.Outcome.RECONNECT_NEEDED;
        } catch (IOException e) {
            return SyncReport.Outcome.NETWORK_ERROR;
        }
    }
}
