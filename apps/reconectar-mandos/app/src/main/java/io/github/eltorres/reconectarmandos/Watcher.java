package io.github.eltorres.reconectarmandos;

import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

final class Watcher {
    static final long WAIT_MS = 10 * 60_000L;

    private final RemoteStore store;
    private final BluetoothPort bluetooth;
    private final AdvertWatch watch;
    private final BooleanSupplier ready;
    private final LongSupplier clock;

    Watcher(RemoteStore store, BluetoothPort bluetooth, AdvertWatch watch, BooleanSupplier ready, LongSupplier clock) {
        this.store = store;
        this.bluetooth = bluetooth;
        this.watch = watch;
        this.ready = ready;
        this.clock = clock;
    }

    void start() {
        store.setWaitUntil(clock.getAsLong() + WAIT_MS);
        Set<String> pending = pending();
        if (!pending.isEmpty()) watch.watch(pending, store.waitUntil());
        else if (!watch.watching().isEmpty()) watch.stop();
    }

    void refresh() {
        Set<String> pending = pending();
        if (pending.equals(watch.watching())) return;
        if (pending.isEmpty()) watch.stop();
        else watch.watch(pending, store.waitUntil());
    }

    boolean waiting() {
        return clock.getAsLong() < store.waitUntil();
    }

    private Set<String> pending() {
        if (!ready.getAsBoolean() || !waiting()) return Set.of();
        return Pending.of(store.marked(), store.unbondedByUs(), bluetooth);
    }
}
