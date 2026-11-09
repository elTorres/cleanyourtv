package io.github.eltorres.reconectarmandos;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.List;

public class WatcherTest {
    private static final String RC902 = "00:00:00:70:A2:7D";
    private static final String RC833 = "00:00:00:73:01:EE";
    private static final long T = 1_000_000;
    private static final String WATCHING = "watch [" + RC902 + ", " + RC833 + "] until " + (T + Watcher.WAIT_MS);

    private final FakeBluetooth bluetooth = new FakeBluetooth().bond(RC902, "TCL_RC902V_M1").bond(RC833, "BT_RC833_U1");
    private final FakeRemoteStore store = new FakeRemoteStore().with(RC902).with(RC833);
    private final FakeAdvertWatch watch = new FakeAdvertWatch();
    private boolean ready = true;
    private long now = T;
    private final Watcher watcher = new Watcher(store, bluetooth, watch, () -> ready, () -> now);

    @Test
    public void watchStartsWithPendingRemotes() {
        watcher.start();
        watcher.refresh();

        assertEquals(List.of(WATCHING), watch.calls);
    }

    @Test
    public void watchStopsWhenAnyMarkedConnected() {
        watcher.start();
        bluetooth.connect(RC833);
        watcher.refresh();

        assertEquals(List.of(WATCHING, "stop"), watch.calls);
    }

    @Test
    public void watchStopsWhileRequirementMissing() {
        watcher.start();
        ready = false;
        watcher.refresh();

        assertEquals(List.of(WATCHING, "stop"), watch.calls);
    }

    @Test
    public void watchStopsWhenWaitEnds() {
        watcher.start();
        now = T + Watcher.WAIT_MS - 1;
        watcher.refresh();
        now = T + Watcher.WAIT_MS;
        watcher.refresh();

        assertEquals(List.of(WATCHING, "stop"), watch.calls);
    }

    @Test
    public void startBeginsANewWait() {
        watcher.start();
        now = T + Watcher.WAIT_MS;
        watcher.refresh();
        now = T + 3 * Watcher.WAIT_MS;
        watcher.start();

        String again = "watch [" + RC902 + ", " + RC833 + "] until " + (T + 4 * Watcher.WAIT_MS);
        assertEquals(List.of(WATCHING, "stop", again), watch.calls);
    }
}
