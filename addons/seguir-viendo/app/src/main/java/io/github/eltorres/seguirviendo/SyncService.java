package io.github.eltorres.seguirviendo;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;

final class SyncService {
    private final List<ViewingSource> sources;
    private final WatchNextStore store;
    private final LongSupplier clock;

    SyncService(List<ViewingSource> sources, WatchNextStore store, LongSupplier clock) {
        this.sources = sources;
        this.store = store;
        this.clock = clock;
    }

    SyncReport syncAll() {
        Map<String, SyncReport.Outcome> outcomes = new LinkedHashMap<>();
        for (ViewingSource source : sources) outcomes.put(source.id(), syncOne(source));
        return new SyncReport(outcomes, clock.getAsLong());
    }

    SyncReport sync(String sourceId) {
        Map<String, SyncReport.Outcome> outcomes = new LinkedHashMap<>();
        for (ViewingSource source : sources) {
            if (source.id().equals(sourceId)) outcomes.put(source.id(), syncOne(source));
        }
        return new SyncReport(outcomes, clock.getAsLong());
    }

    private SyncReport.Outcome syncOne(ViewingSource source) {
        if (source.state() != SourceState.CONNECTED) return SyncReport.Outcome.SKIPPED;
        try {
            store.replaceSource(source.id(), source.fetch());
            return SyncReport.Outcome.UPDATED;
        } catch (AuthException e) {
            return SyncReport.Outcome.RECONNECT_NEEDED;
        } catch (IOException e) {
            return SyncReport.Outcome.NETWORK_ERROR;
        }
    }
}
