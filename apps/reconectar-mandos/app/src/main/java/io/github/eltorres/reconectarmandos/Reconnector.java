package io.github.eltorres.reconectarmandos;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

final class Reconnector {
    enum Step { IGNORE, UNBOND, BOND }

    static final long FALLBACK_MS = 10_000;
    static final long BURST_GAP_MS = 30_000;
    static final long RETRY_MS = 5_000;

    private final RemoteStore store;
    private final BluetoothPort bluetooth;
    private final BooleanSupplier ready;
    private final Map<String, Long> started = new HashMap<>();
    private String reconnecting;

    Reconnector(RemoteStore store, BluetoothPort bluetooth, BooleanSupplier ready) {
        this.store = store;
        this.bluetooth = bluetooth;
        this.ready = ready;
    }

    boolean reconnecting(String address) {
        return address.equals(reconnecting);
    }

    Step onAdvert(String address, long nowMs) {
        if (!store.marked().containsKey(address)) return Step.IGNORE;
        Long previous = store.lastAdvert(address);
        store.saveAdvert(address, nowMs);
        if (!ready.getAsBoolean() || reconnecting != null) return Step.IGNORE;
        if (!Pending.of(store.marked(), store.unbondedByUs(), bluetooth).contains(address)) {
            store.setBurstStart(address, null);
            return Step.IGNORE;
        }
        if (!bluetooth.bonded(address)) return start(address, Step.BOND, nowMs);
        Long burst = store.burstStart(address);
        if (burst == null || previous == null || nowMs - previous > BURST_GAP_MS) {
            store.setBurstStart(address, nowMs);
            return Step.IGNORE;
        }
        if (nowMs - burst < FALLBACK_MS) return Step.IGNORE;
        Step step = start(address, Step.UNBOND, nowMs);
        if (step == Step.UNBOND) store.setUnbondedByUs(address, true);
        return step;
    }

    Step onUnbonded(String address) {
        return reconnecting(address) ? Step.BOND : Step.IGNORE;
    }

    void onFinished(String address, boolean connected, String detail, long nowMs) {
        if (reconnecting(address)) reconnecting = null;
        store.saveAttempt(address, new Attempt(nowMs, connected, detail));
        store.setBurstStart(address, null);
        if (bluetooth.bonded(address)) store.setUnbondedByUs(address, false);
    }

    private Step start(String address, Step step, long nowMs) {
        Long last = started.get(address);
        if (last != null && nowMs - last < RETRY_MS) return Step.IGNORE;
        started.put(address, nowMs);
        reconnecting = address;
        return step;
    }
}
