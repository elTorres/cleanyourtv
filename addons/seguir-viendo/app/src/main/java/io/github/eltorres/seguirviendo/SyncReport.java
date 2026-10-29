package io.github.eltorres.seguirviendo;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

final class SyncReport {
    enum Outcome { UPDATED, NETWORK_ERROR, RECONNECT_NEEDED, SKIPPED }

    private final Map<String, Outcome> outcomes;
    private final long finishedAtMs;

    SyncReport(Map<String, Outcome> outcomes, long finishedAtMs) {
        this.outcomes = Collections.unmodifiableMap(new LinkedHashMap<>(outcomes));
        this.finishedAtMs = finishedAtMs;
    }

    Outcome outcome(String sourceId) {
        return outcomes.get(sourceId);
    }

    Map<String, Outcome> outcomes() {
        return outcomes;
    }

    long finishedAtMs() {
        return finishedAtMs;
    }
}
