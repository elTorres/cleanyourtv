package io.github.eltorres.seguirviendo;

import java.util.Map;

final class SourcesService {
    private final Map<String, Runnable> forgetCredentials;
    private final WatchNextStore store;
    private final Targets targets;

    SourcesService(Map<String, Runnable> forgetCredentials, WatchNextStore store, Targets targets) {
        this.forgetCredentials = forgetCredentials;
        this.store = store;
        this.targets = targets;
    }

    void disconnect(String sourceId) {
        Runnable forget = forgetCredentials.get(sourceId);
        if (forget != null) forget.run();
        store.clearSource(sourceId);
    }

    void setTargetPackage(String sourceId, String packageName) {
        targets.setTargetPackage(sourceId, packageName);
    }
}
